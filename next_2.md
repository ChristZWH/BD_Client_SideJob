# 项目后续实现方案

> **最后更新：2026-06-09**

## 一、当前项目状态总结

### ✅ 已完成功能（P0 核心功能）

#### 1. 基础视频播放流
- [x] Android 项目搭建，ExoPlayer 依赖配置完成
- [x] 视频数据模型（Video、VideoList、ImageCard）定义完成
- [x] MockVideoData 假数据（10+ 条视频数据）实现完成
- [x] ViewPager2 + RecyclerView 垂直滑动切换视频实现完成
- [x] VideoPlayerController 播放器控制（播放/暂停/Seek）实现完成
- [x] 互动元素 UI（头像、作者、标题、点赞、评论）实现完成
- [x] ~~**CrashFix**~~ 线程安全修复 — `VideoPlayerController.release()` / `detachSurface()` 自动主线程路由
- [x] ~~**CrashFix**~~ OOM 防护 — `AndroidManifest largeHeap`、内存感知播放器创建、`maxLiveDistance=1`

#### 2. 搜索功能
- [x] MainActivity 顶部搜索框实现完成
- [x] SearchActivity 搜索中间页实现完成（搜索框 + 历史记录 + 推荐词）
- [x] SearchHistoryManager 搜索历史管理（SharedPreferences，最多 10 条）实现完成
- [x] MainActivity 底部推荐词轮播实现完成
- [x] SearchResultActivity 搜索结果页实现完成（视频列表 + 相关搜索）

#### 3. MVP 架构重构
- [x] BaseActivity、BasePresenter、BaseView 基类实现完成
- [x] main、search、searchresult 三个模块已重构为 MVP 架构

---

### ✅ 已完成功能（P1 进阶功能）

#### 4. 视频预加载优化（已完成）
| 文件 | 改动说明 |
|------|----------|
| `util/PlayerManager.java` | 播放器池管理（LRU 缓存）、预加载调度、临时 SurfaceView 生命周期、线程安全（自动主线程路由）、内存检查 `hasEnoughMemory()` |
| `util/PreloadConfig.java` | Builder 模式可配置预加载参数 |
| `util/PreloadStatistics.java` | 起播延迟、缓存命中率统计 + `printReport()` |
| `util/CacheManager.java` | ExoPlayer CacheDataSource（500MB 磁盘缓存） |
| `ui/main/MainPresenter.java` | `initPlayerManager()` 低内存安全配置、`preloadUpcomingVideo()`、`schedulePreloading()` |
| `ui/main/MainActivity.java` | `playVideoAtPosition()` 优先走 PlayerManager 秒开路径、`releaseDistantControllers(maxLiveDistance=1)` |

预加载流程：
```
用户滑动 → preloadUpcomingVideo(nextPos)
  → PlayerManager.preloadVideo(videoId, url)
    → 主线程创建 ExoPlayer + 临时 SurfaceView → prepare() + playWhenReady=false
    → onPrepared → 后台等 500ms 让缓冲区填充 → 放入 playerPool
用户真正滑动到该视频 → getPlayer() 从 pool 取出 → attachSurfaceView() → 秒开
```

已解决的 crash：
- [x] **OOM 崩溃**：`maxCachedPlayers=1`、`maxLiveDistance=1`、`largeHeap=true`、64MB 内存阈值
- [x] **线程崩溃**：`release()`/`detachSurface()` 自动检测 Looper 并 post 到主线程
- [x] **临时 SurfaceView 泄漏**：`tempSurfaceMap` 追踪并在 `cleanupPreloadPlayer()` 中释放

#### 5. 清晰度切换（已完成）
| 文件 | 改动说明 |
|------|----------|
| `data/model/Video.java` | `quality360p` / `quality720p` 字段 + getter/setter |
| `data/local/MockVideoData.java` | 每个视频有 360p 和 720p URL |
| `widget/VideoPlayerView.java` | 左下角清晰度标签（`llQualityToggle`）、弹窗菜单（`llQualityMenu`）、`switchToQuality()` 委托给 controller |
| `util/VideoPlayerController.java` | `switchQuality()` 保持播放进度和播放状态，一次性监听器恢复位置后自移除 |
| `res/drawable/bg_quality_option.xml` | 选中/未选中状态背景 |

切换流程：
```
点击 360p → switchToQuality(QUALITY_360P, url360p, url720p)
  → 保存 currentPosition + isPlaying
  → setMediaItem(newUrl) + prepare()
  → STATE_READY → seekTo(savedPosition) → 恢复 playWhenReady
  → removeListener(this) 避免堆积
```

---

## 二、待实现功能

### 📋 功能清单

| 序号 | 功能 | 优先级 | 预估耗时 | 状态 |
|------|------|--------|----------|------|
| 1 | ~~视频预加载优化~~ | 高 | 2h | ✅ 已完成 |
| 2 | ~~清晰度切换（360p/720p）~~ | 高 | 2h | ✅ 已完成 |
| 3 | 横屏播放支持 | 中 | 2h | ❌ 未开始 |
| 4 | 视频流混排图片卡片 | 中 | 3h | ❌ 未开始 |

---

## 三、详细实现方案

### 3.1 视频预加载优化 ✅

**状态**：已完成，并额外增加了线程安全和 OOM 防护。

**已实现核心能力**：
- `PlayerManager` — 单例 LRU 播放器池，`preloadVideo()` 后台缓冲，`getPlayer()` 秒开复用
- `PreloadConfig` — Builder 模式配置（forwardCount/backwardCount/preloadDurationMs/maxCachedPlayers）
- `PreloadStatistics` — 起播延迟统计、缓存命中率、`printReport()` 格式报告
- `CacheManager` — ExoPlayer CacheDataSource 500MB 磁盘缓存
- `MainActivity` — `playVideoAtPosition()` 优先走 PlayerManager 秒开路径
- `MainPresenter` — 每次页面切换触发 `preloadUpcomingVideo()`

**当前内存安全配置**（MainPresenter.initPlayerManager）：
```java
forwardCount = 1        // 只预加载下一个
backwardCount = 0       // 不预加载上一个
preloadDurationMs = 500 // 缓冲 0.5s
maxCachedPlayers = 1    // 极严，每 ExoPlayer ~60MB
```

**线上运行保护**：
- `largeHeap=true` → 堆上限 192MB → ~512MB
- `MAX_CACHED_PLAYERS_LIMIT=2` 硬上限
- `hasEnoughMemory()` 创建前 64MB 阈值检查
- `MainActivity.releaseDistantControllers(maxLiveDistance=1)` 最多 3 个存活
- `VideoPlayerController.release()` / `detachSurface()` 自动主线程路由

**修改文件**：

| 文件 | 修改内容 | 状态 |
|------|----------|------|
| `util/PlayerManager.java` | 新增播放器池管理、预加载配置、统计功能、线程安全、内存检查 | ✅ |
| `util/PreloadConfig.java` | 新增预加载配置类 | ✅ |
| `util/PreloadStatistics.java` | 新增统计数据类 | ✅ |
| `util/CacheManager.java` | 新增 ExoPlayer 磁盘缓存 | ✅ |
| `util/VideoPlayerController.java` | `release()`/`detachSurface()` 主线程路由 + 四步释放 | ✅ |
| `ui/main/MainPresenter.java` | 添加预加载调度逻辑 + 内存安全配置 | ✅ |
| `ui/main/MainActivity.java` | 集成预加载回调 + 内存感知播放器创建 | ✅ |
| `ui/main/adapter/VideoFeedAdapter.java` | `onViewDetachedFromWindow` 只暂停不释放 | ✅ |
| `AndroidManifest.xml` | `largeHeap=true` | ✅ |

---

### 3.2 清晰度切换 ✅

**状态**：已完成。

**修改文件**：

| 文件 | 修改内容 | 状态 |
|------|----------|------|
| `data/model/Video.java` | 添加 `quality360p`/`quality720p` 字段 | ✅ |
| `data/local/MockVideoData.java` | 补充清晰度 URL | ✅ |
| `widget/VideoPlayerView.java` | 清晰度切换按钮 + 菜单 + `switchToQuality()` | ✅ |
| `util/VideoPlayerController.java` | `switchQuality()` 保持进度+一次性监听器 | ✅ |
| `res/drawable/bg_quality_option.xml` | 选中/未选中背景 | ✅ |
| `res/drawable/bg_quality_option_unselected.xml` | 未选中背景 | ✅ |
| `res/drawable/ic_quality.xml` | 清晰度图标 | ✅ |
| `res/drawable/ic_quality_hd.xml` | HD 图标 | ✅ |

---

### 3.3 横屏播放支持 ❌

**状态**：未开始，无任何实现代码。

**目标**：支持设备旋转自动切换横屏全屏

**实现方案**：
1. **配置 Activity**：AndroidManifest 已有 `configChanges="orientation|screenSize"`（无需修改）
2. **监听旋转**：在 MainActivity 中注册 `OrientationEventListener`，根据角度自动切换
3. **布局调整**：
   - 横屏时隐藏顶部搜索栏（`llTopSearch`）、右侧按钮（`llRightButtons`）、底部推荐词（`llRelatedSearch`）
   - 进度条下移，适配横屏安全区域
   - 播放器铺满屏幕
4. **视频信息**：横屏时标题/作者移到左下角半透明浮层

**需要新增/修改的文件**：

| 文件 | 修改内容 |
|------|----------|
| `ui/main/MainActivity.java` | 注册 `OrientationEventListener`、`onOrientationChanged()` 处理横竖屏切换逻辑 |
| `widget/VideoPlayerView.java` | 新增 `setFullscreenMode(boolean)` 方法，横屏时调整布局 |
| `res/layout/layout_video_player.xml` | 无需改（VideoPlayerView 代码中动态调整） |

**核心代码要点**：
```java
// MainActivity
OrientationEventListener orientationListener = new OrientationEventListener(this) {
    @Override
    public void onOrientationChanged(int orientation) {
        if (orientation == ORIENTATION_UNKNOWN) return;
        // 横屏：70°~110° 或 250°~290°
        boolean isLandscape = (orientation > 70 && orientation < 110)
                           || (orientation > 250 && orientation < 290);
        setFullscreenMode(isLandscape);
    }
};

private void setFullscreenMode(boolean fullscreen) {
    if (fullscreen) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        llTopSearch.setVisibility(View.GONE);
        llRelatedSearch.setVisibility(View.GONE);
    } else {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        llTopSearch.setVisibility(View.VISIBLE);
        llRelatedSearch.setVisibility(View.VISIBLE);
    }
    // 通知当前 VideoPlayerView 调整布局
    VideoPlayerView currentView = adapter.getPlayerViewAtPosition(viewPager.getCurrentItem());
    if (currentView != null) {
        currentView.setFullscreenMode(fullscreen);
    }
}
```

**注意**：`Activity.screenOrientation` 在 AndroidManifest 中已设为 `portrait`，如果要支持自动旋转，需要移除此限制。如果只支持"手动横屏按钮"（类似抖音点击全屏按钮），则保留 `portrait` 限制，在 `VideoPlayerView` 添加一个全屏按钮。

---

### 3.4 视频流混排图片卡片 ❌

**状态**：ImageCard 模型和 Mock 数据已就绪，Adapter 未实现多类型。

**已就绪的前置条件**：
- `data/model/ImageCard.java` — 图片卡片模型（cardId/imageUrl/title/position）
- `data/local/MockVideoData.java` — `getImageCards()` 5 张图片数据

**待实现**：

**目标**：在视频列表中插入图片卡片，类似抖音的图文混排

**实现方案**：

**1. 数据混合**：将 `Video` 和 `ImageCard` 统一为一个 `FeedItem` 接口或使用 `Object` 列表
```java
// 在 MainPresenter 中混合 Video 和 ImageCard
List<Object> feedItems = new ArrayList<>();
// 每 3 个视频插入 1 张图片卡片
for (int i = 0; i < videos.size(); i++) {
    feedItems.add(videos.get(i));
    if ((i + 1) % 3 == 0 && imageCardIndex < imageCards.size()) {
        feedItems.add(imageCards.get(imageCardIndex++));
    }
}
```

**2. Adapter 改造**：使用多类型 RecyclerView Adapter
```java
private static final int TYPE_VIDEO = 0;
private static final int TYPE_IMAGE = 1;

@Override
public int getItemViewType(int position) {
    Object item = feedItems.get(position);
    if (item instanceof Video) return TYPE_VIDEO;
    if (item instanceof ImageCard) return TYPE_IMAGE;
    return TYPE_VIDEO;
}
```

**3. 图片展示**：新建 `ImageCardView`，使用 Glide 加载图片

**需要新增/修改的文件**：

| 文件 | 修改内容 |
|------|----------|
| `ui/main/adapter/VideoFeedAdapter.java` | 改为多类型 Adapter：新增 `TYPE_VIDEO`/`TYPE_IMAGE`、`getItemViewType()`、`ImageCardViewHolder` |
| `widget/ImageCardView.java` | **新增**：图片卡片视图（ImageView + 标题），支持点击查看大图 |
| `res/layout/layout_image_card.xml` | **新增**：图片卡片布局文件 |
| `data/local/MockVideoData.java` | 已有 `getImageCards()`，无需改动 |
| `ui/main/MainPresenter.java` | `loadVideos()` 中混合 Video + ImageCard 为统一 feedItems 列表 |
| `ui/main/MainContract.java` | `showVideoList()` 签名可能需要改为 `List<Object>` 或新增 `showFeedItems()` |

---

## 四、推荐实现顺序

```
Phase 1: 视频预加载优化  ✅ 已完成
    ↓
Phase 2: 清晰度切换      ✅ 已完成
    ↓
Phase 3: 横屏播放支持    ❌ 未开始（约 2h）
    ↓
Phase 4: 视频流混排图片卡片  ❌ 未开始（约 3h）
```

### 当前优先级说明

| 阶段 | 状态 | 理由 |
|------|------|------|
| Phase 1 | ✅ 已完成 | 直接提升用户体验，减少等待时间 |
| Phase 2 | ✅ 已完成 | 满足不同网络环境下的播放需求 |
| Phase 3 | ❌ 待实现 | 提升大屏观看体验，2h 即可完成 |
| Phase 4 | ❌ 待实现 | 增加内容多样性，需 3h，改动面较大 |

---

## 五、预期效果

### 当前状态评估

| 模块 | 状态 | 说明 |
|------|------|------|
| 基础播放流 | ✅ 完成 | ViewPager2 + ExoPlayer 稳定运行 |
| 搜索功能 | ✅ 完成 | 搜索中间页 + 历史记录 + 搜索结果 |
| MVP 架构 | ✅ 完成 | 三层解耦，Base 基类封装 |
| 视频预加载 | ✅ 完成 | PlayerManager LRU 池 + 线程安全 + OOM 防护 |
| 清晰度切换 | ✅ 完成 | 360p/720p 秒切，保持播放进度 |
| 横屏播放 | ❌ 未实现 | 仅竖屏，横屏需 2h |
| 图文混排 | ❌ 未实现 | ImageCard 模型就绪，Adapter 待改造 |

### 功能完成度

```
已实现 5/7 模块 (71.4%)
  ├── P0 核心功能: 3/3 (100%)  ✅
  └── P1 进阶功能: 2/4 (50%)   🔄

待实现:
  ├── 横屏播放支持 (2h)
  └── 视频流混排图片卡片 (3h)
```

---

## 六、风险评估

| 风险 | 描述 | 应对方案 | 状态 |
|------|------|----------|------|
| 内存溢出 | 预加载过多播放器实例 | `maxCachedPlayers=1`、`maxLiveDistance=1`、`largeHeap=true`、64MB 阈值 | ✅ 已缓解 |
| 线程崩溃 | 后台线程访问 ExoPlayer | `release()`/`detachSurface()` 自动主线程路由 | ✅ 已修复 |
| 临时 SurfaceView 泄漏 | 预加载的 temp SurfaceView 未释放 | `tempSurfaceMap` 追踪 + `cleanupPreloadPlayer()` | ✅ 已修复 |
| 网络波动 | 清晰度切换时卡顿 | 添加加载状态和错误处理 | ✅ 已处理 |
| 旋转闪烁 | 横屏切换时布局闪烁 | 使用 configChanges 避免 Activity 重建 | 🔜 待实现 |
| 数据混合 | 多类型数据处理复杂 | 提前定义数据类型枚举，统一 FeedItem | 🔜 待实现 |

---

## 七、需要用户确认

1. **是否需要实现横屏播放？**（约 2h，改动 2-3 个文件）
   - 选项 A：自动旋转横屏（需要移除 AndroidManifest 的 `portrait` 限制）
   - 选项 B：手动横屏按钮（保留竖屏限制，类似抖音点全屏按钮）

2. **是否需要实现图文混排？**（约 3h，改动 5-6 个文件）
   - 需要新建 `ImageCardView`、`layout_image_card.xml`
   - 需要改造 `VideoFeedAdapter` 为多类型
   - 需要改造 `MainPresenter` 混合数据

3. **当前运行的稳定性如何？** OOM 和线程崩溃是否已解决？