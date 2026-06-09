# 项目后续实现方案

## 一、当前项目状态总结

### ✅ 已完成功能（P0 核心功能）

#### 1. 基础视频播放流
- [x] Android 项目搭建，ExoPlayer 依赖配置完成
- [x] 视频数据模型（Video、VideoList、ImageCard）定义完成
- [x] MockVideoData 假数据（10+ 条视频数据）实现完成
- [x] ViewPager2 + RecyclerView 垂直滑动切换视频实现完成
- [x] VideoPlayerController 播放器控制（播放/暂停/Seek）实现完成
- [x] 互动元素 UI（头像、作者、标题、点赞、评论）实现完成

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

## 二、待实现功能（P1 进阶功能）

### 📋 功能清单

| 序号 | 功能 | 优先级 | 预估耗时 | 状态 |
|------|------|--------|----------|------|
| 1 | 视频预加载优化 | 高 | 2h | 未开始 |
| 2 | 清晰度切换（360p/720p） | 高 | 2h | 未开始 |
| 3 | 横屏播放支持 | 中 | 2h | 未开始 |
| 4 | 视频流混排图片卡片 | 中 | 3h | 未开始 |

---

## 三、详细实现方案

### 3.1 视频预加载优化

**目标**：减少视频起播延迟，提升用户体验

**实现方案**：
1. **播放器预创建**：当前视频播放时，提前创建前后各 1 个位置的播放器实例
2. **数据预加载**：使用 ExoPlayer 的缓存策略，预加载下一个视频的前 5 秒数据
3. **缓存管理**：限制同时缓存的视频数量（最多 3 个），避免内存溢出

**修改文件**：
- `util/PlayerManager.java` - 新增播放器池管理
- `ui/main/MainPresenter.java` - 添加预加载逻辑
- `ui/main/adapter/VideoFeedAdapter.java` - 优化 ViewHolder 复用

### 3.2 清晰度切换

**目标**：支持 360p/720p 清晰度切换

**实现方案**：
1. **数据准备**：为每个视频添加 quality360p 和 quality720p 字段
2. **UI 实现**：在播放控制栏添加清晰度切换按钮
3. **切换逻辑**：点击按钮弹出选项，切换播放源并保持播放进度

**修改文件**：
- `data/model/Video.java` - 添加清晰度字段
- `data/local/MockVideoData.java` - 补充清晰度 URL
- `widget/PlayerControlView.java` - 添加清晰度切换按钮
- `util/VideoPlayerController.java` - 添加切换清晰度方法

### 3.3 横屏播放支持

**目标**：支持设备旋转自动切换横屏全屏

**实现方案**：
1. **配置 Activity**：设置 `android:configChanges="orientation|screenSize"`
2. **监听旋转**：在 MainActivity 中监听 OrientationEventListener
3. **布局调整**：横屏时隐藏状态栏，播放器铺满屏幕

**修改文件**：
- `ui/main/MainActivity.java` - 添加旋转监听和布局调整逻辑
- `res/layout-land/activity_main.xml` - 横屏布局文件

### 3.4 视频流混排图片卡片

**目标**：在视频列表中插入图片卡片

**实现方案**：
1. **数据混合**：在 VideoList 中混合 Video 和 ImageCard 数据
2. **Adapter 改造**：使用多类型 RecyclerView Adapter
3. **图片展示**：使用 Glide 加载图片，支持点击查看大图

**修改文件**：
- `ui/main/adapter/VideoFeedAdapter.java` - 改造为多类型 Adapter
- `data/local/MockVideoData.java` - 添加图片卡片数据

---

## 四、推荐实现顺序

```
Phase 1: 视频预加载优化
    ↓
Phase 2: 清晰度切换
    ↓
Phase 3: 横屏播放支持
    ↓
Phase 4: 视频流混排图片卡片
```

### 优先级说明

| 阶段 | 理由 |
|------|------|
| Phase 1 | 直接提升用户体验，减少等待时间 |
| Phase 2 | 满足不同网络环境下的播放需求 |
| Phase 3 | 提升大屏观看体验 |
| Phase 4 | 增加内容多样性，非核心功能 |

---

## 五、预期效果

### 优化前 vs 优化后

| 指标 | 优化前 | 优化后 |
|------|--------|--------|
| 起播延迟 | ~800ms | ~300ms |
| 清晰度选择 | 固定 | 360p/720p 可选 |
| 屏幕方向 | 仅竖屏 | 自动横竖屏切换 |
| 内容形式 | 纯视频 | 视频 + 图片混排 |

---

## 六、风险评估

| 风险 | 描述 | 应对方案 |
|------|------|----------|
| 内存溢出 | 预加载过多播放器实例 | 限制同时存在的播放器数量（最多 3 个） |
| 网络波动 | 清晰度切换时卡顿 | 添加加载状态和错误处理 |
| 旋转闪烁 | 横屏切换时布局闪烁 | 使用 configChanges 避免 Activity 重建 |
| 数据混合 | 多类型数据处理复杂 | 提前定义数据类型枚举 |

---

## 七、需要用户确认

请确认以下事项，以便我开始实施：

1. **是否需要实现全部 P1 功能？**（可选择部分实现）
2. **是否有特定的清晰度 URL 要求？**（当前使用假数据）
3. **图片卡片的样式是否有设计要求？**（如比例、点击行为）

确认后，我将按照方案开始实现。