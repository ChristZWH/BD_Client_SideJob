package com.example.bd_client_sidejob.ui.main;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.viewpager2.widget.ViewPager2;

import com.example.bd_client_sidejob.R;
import com.example.bd_client_sidejob.base.BaseActivity;
import com.example.bd_client_sidejob.data.model.ImageCard;
import com.example.bd_client_sidejob.data.model.Video;
import com.example.bd_client_sidejob.ui.main.adapter.VideoFeedAdapter;
import com.example.bd_client_sidejob.ui.searchresult.SearchResultActivity;
import com.example.bd_client_sidejob.util.PlayerManager;
import com.example.bd_client_sidejob.util.VideoPlayerController;
import com.example.bd_client_sidejob.ui.search.SearchActivity;
import com.example.bd_client_sidejob.widget.VideoPlayerView;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.ArrayList;

/**
 * 主活动类 - 视频播放应用的主入口
 * 采用 MVP 架构模式，作为 View 层负责 UI 展示和用户交互
 */
public class MainActivity extends BaseActivity<MainContract.Presenter> implements MainContract.View {
    
    private static final String TAG = "MainActivity";

    /** Intent 传递视频ID的常量 */
    public static final String EXTRA_VIDEO_ID = "extra_video_id";
    /** Intent 传递搜索关键词的常量（从搜索结果页传入） */
    public static final String EXTRA_SEARCH_KEYWORD = "extra_search_keyword";

    /** ViewPager2 组件 - 用于实现垂直滑动的视频列表 */
    private ViewPager2 viewPager;
    /** 加载进度条 - 数据加载时显示 */
    private ProgressBar progressBar;
    /** 视频列表适配器 - 管理视频数据和播放器视图 */
    private VideoFeedAdapter adapter;
    /**
     * 播放器控制器映射 - 使用 WeakHashMap 防止内存泄漏
     * key: 视频位置, value: 对应的 VideoPlayerController
     * WeakHashMap 的特性：当 key 不再被强引用时会自动被 GC 回收
     */
    private Map<Integer, VideoPlayerController> playerControllers = new WeakHashMap<>();

    // 搜索相关组件
    private LinearLayout llTopSearch;
    private ImageView ivBack;
    private TextView tvListen;
    private ImageView ivAdd;
    private ImageView ivMore;
    private LinearLayout llSearchArea;

    // 底部相关搜索组件
    private LinearLayout llRelatedSearch;
    private TextView tvRelatedSearch;
    private ImageView ivRelatedArrow;

    // ==================== 横竖屏旋转相关 ====================
    /** 屏幕方向监听器 */
    private OrientationEventListener orientationListener;
    /** 当前是否横屏全屏模式 */
    private boolean isLandscapeFullscreen = false;

    // 相关搜索数据
    private List<String> relatedKeywords = new ArrayList<>();
    private int currentRelatedIndex = 0;

    // 轮播相关
    private Handler carouselHandler;
    private Runnable carouselRunnable;
    private static final long CAROUSEL_INTERVAL = 3000; // 轮播间隔（毫秒）

    // ==================== 屏幕方向监听 ====================

    /**
     * 初始化屏幕方向监听器 — 检测设备物理旋转角度，自动切换横竖屏
     */
    private void initOrientationListener() {
        orientationListener = new OrientationEventListener(this) {
            private int lastOrientation = -1;

            @Override
            public void onOrientationChanged(int orientation) {
                if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN) {
                    return;
                }

                // 角度区间检测（防抖：避免微小角度变化频繁切换）
                int currentOrientation;
                if (orientation > 70 && orientation < 110) {
                    currentOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                } else if (orientation > 250 && orientation < 290) {
                    currentOrientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                } else if (orientation > 340 || orientation < 20) {
                    currentOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                } else {
                    return; // 中间角度（20°~70°、110°~250°、290°~340°）不触发
                }

                if (currentOrientation != lastOrientation) {
                    lastOrientation = currentOrientation;
                    applyOrientation(currentOrientation);
                }
            }
        };
    }

    /**
     * 根据方向值应用横竖屏切换
     */
    private void applyOrientation(int screenOrientation) {
        boolean isLandscape = (screenOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                || screenOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

        if (isLandscape == isLandscapeFullscreen) {
            return; // 无需切换
        }

        if (isLandscape) {
            enterLandscapeFullscreen();
        } else {
            exitLandscapeFullscreen();
        }
    }

    /**
     * 进入横屏全屏模式
     * 隐藏系统状态栏、顶部搜索栏、底部推荐词，让视频铺满屏幕
     */
    private void enterLandscapeFullscreen() {
        isLandscapeFullscreen = true;

        // 设置 Activity 为横屏（触发屏幕旋转）
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        // 沉浸式全屏：隐藏状态栏和导航栏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        );

        // 隐藏顶栏和底部搜索
        llTopSearch.setVisibility(View.GONE);
        llRelatedSearch.setVisibility(View.GONE);

        // 通知当前 VideoPlayerView 进入全屏模式
        VideoPlayerView currentView = adapter.getPlayerViewAtPosition(viewPager.getCurrentItem());
        if (currentView != null) {
            currentView.setFullscreenMode(true);
        }

        Log.d(TAG, "Entered landscape fullscreen");
    }

    /**
     * 退出横屏全屏模式
     * 恢复竖屏布局：显示搜索栏、推荐词、右侧按钮
     */
    private void exitLandscapeFullscreen() {
        isLandscapeFullscreen = false;

        // 恢复竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // 清除全屏标志
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

        // 显示顶栏和底部搜索
        llTopSearch.setVisibility(View.VISIBLE);
        llRelatedSearch.setVisibility(View.VISIBLE);

        // 通知当前 VideoPlayerView 退出全屏模式
        VideoPlayerView currentView = adapter.getPlayerViewAtPosition(viewPager.getCurrentItem());
        if (currentView != null) {
            currentView.setFullscreenMode(false);
        }

        Log.d(TAG, "Exited landscape fullscreen");
    }

    /**
     * 获取布局资源ID
     * @return 布局文件的资源ID
     */
    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    /**
     * 创建 Presenter 实例
     * MVP 标准初始化模式：View 负责创建并管理 Presenter，确保生命周期同步
     * 该方法在 BaseActivity.onCreate() 中的 initPresenter() 调用
     * @return MainPresenter 实例
     */
    @Override
    protected MainContract.Presenter createPresenter() {
        return new MainPresenter();
    }

    /**
     * 初始化视图组件
     * 绑定布局控件、设置适配器、注册回调监听
     */
    @Override
    protected void initView() {
        // 获取布局中的控件实例
        viewPager = findViewById(R.id.viewPager);
        progressBar = findViewById(R.id.progressBar);

        // 初始化屏幕方向监听器
        initOrientationListener();

        // 初始化搜索相关组件
        llTopSearch = findViewById(R.id.llTopSearch);
        ivBack = findViewById(R.id.ivBack);
        tvListen = findViewById(R.id.tvListen);
        ivAdd = findViewById(R.id.ivAdd);
        ivMore = findViewById(R.id.ivMore);
        llSearchArea = findViewById(R.id.llSearchArea);

        // 初始化底部相关搜索组件
        llRelatedSearch = findViewById(R.id.llRelatedSearch);
        tvRelatedSearch = findViewById(R.id.tvRelatedSearch);
        ivRelatedArrow = findViewById(R.id.ivRelatedArrow);

        // 创建监听器
        setupListeners();
    }

    // 注册各种监听事件
    private void setupListeners() {
        // 设置搜索区域点击事件 - 视频播放页面跳转到搜索页面
        llSearchArea.setOnClickListener(v -> {
            Intent intent = new Intent(this, SearchActivity.class);
            startActivity(intent);
        });

        // 设置 相关搜索 点击事件 - 视频播放页面跳转到搜索页面
        llRelatedSearch.setOnClickListener(v -> {
            // 跳转到搜索结果页面
            if (!relatedKeywords.isEmpty()) {
                Intent intent = new Intent(this, SearchResultActivity.class);
                intent.putExtra(SearchResultActivity.EXTRA_KEYWORD, relatedKeywords.get(currentRelatedIndex));
                startActivity(intent);
            }
        });

        // 设置返回按钮
        ivBack.setOnClickListener(v -> finish());

        // 设置听按钮
        tvListen.setOnClickListener(v -> {
            Toast.makeText(this, "听功能待接入", Toast.LENGTH_SHORT).show();
        });

        // 设置添加按钮
        ivAdd.setOnClickListener(v -> {
            Toast.makeText(this, "添加功能待接入", Toast.LENGTH_SHORT).show();
        });

        // 设置更多按钮
        ivMore.setOnClickListener(v -> {
            Toast.makeText(this, "更多功能待接入", Toast.LENGTH_SHORT).show();
        });

        // 创建视频列表适配器并绑定到 ViewPager2
        adapter = new VideoFeedAdapter(this);
        viewPager.setAdapter(adapter);

        // 设置 ViewPager2 为垂直滑动模式（类似抖音）
        viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        // 注册页面切换回调 - 当用户滑动切换页面时触发
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            private int currentPosition = 0;
            
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                handlePageChange(position); // 处理页面切换逻辑（暂停所有视频，播放当前视频）
                currentPosition = position;
            }
            
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                // 状态 1 = START（开始拖动），状态 2 = DRAGGING（正在拖动）
                // 在用户开始滑动时就预加载下一个视频
                if (state == ViewPager2.SCROLL_STATE_DRAGGING || state == ViewPager2.SCROLL_STATE_SETTLING) {
                    int nextPosition = currentPosition + 1;
                    if (mPresenter != null && mPresenter instanceof MainPresenter && adapter != null) {
                        // 预加载下一个视频
                        ((MainPresenter) mPresenter).preloadUpcomingVideo(nextPosition);
                        // 同时预加载前一个视频
                        ((MainPresenter) mPresenter).preloadUpcomingVideo(currentPosition - 1);
                    }
                }
            }
        });

        // 设置适配器的页面变更监听器
        adapter.setPageChangeListener(new VideoFeedAdapter.OnVideoPageChangeListener() {
            @Override
            public void onPageChanged(int position) {
                // 页面切换处理（由 ViewPager2 回调处理，此处预留）
            }

            @Override
            public void onLoadMore() {
                // 加载更多视频 - 通知 Presenter
                if (mPresenter != null) {
                    mPresenter.loadMoreVideos();
                }
            }

            @Override
            public void onVideoPrepared() {
                // 视频准备完成，通知 Presenter 记录起播延迟
                if (mPresenter != null && mPresenter instanceof MainPresenter) {
                    ((MainPresenter) mPresenter).onVideoPrepared();
                }
            }

            @Override
            public void onPageWillChange(int position) {
                // 页面即将切换，提前预加载即将播放的视频（跳过图片卡片）
                if (mPresenter != null && mPresenter instanceof MainPresenter) {
                    ((MainPresenter) mPresenter).preloadUpcomingVideo(position);
                }
            }

            @Override
            public void onImageCardClicked(ImageCard card) {
                // 图片卡片被单击：显示 Toast 提示
                Toast.makeText(MainActivity.this,
                        card.getTitle() + "\n点击查看大图（待接入）", Toast.LENGTH_SHORT).show();
            }
        });

        // 使用 AndroidX 的 OnBackPressedDispatcher 处理返回键
        // 替代传统的 onBackPressed() 方法，更灵活可控
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            // 当用户按返回键时，系统自动触发
            @Override
            public void handleOnBackPressed() {
                // 如果当前是横屏模式，先退出横屏再关闭
                if (isLandscapeFullscreen) {
                    exitLandscapeFullscreen();
                } else {
                    finish(); // 关闭当前 Activity
                }
            }
        });
    }

    /**
     * 初始化数据
     * View 层通知 Presenter 加载初始视频数据
     */
    @Override
    protected void initData() {
        // 读取从搜索结果页面传入的目标视频ID和搜索关键词
        targetVideoId = getIntent().getLongExtra(EXTRA_VIDEO_ID, -1);
        String searchKeyword = getIntent().getStringExtra(EXTRA_SEARCH_KEYWORD);

        // 初始化播放器管理器（用于预加载）
        if (mPresenter != null && mPresenter instanceof MainPresenter) {
            MainPresenter mainPresenter = (MainPresenter) mPresenter;
            mainPresenter.initPlayerManager(this);
            // 有关键词 → 先展示搜索结果，搜索结果的视频播完后继续加载全量
            if (searchKeyword != null && !searchKeyword.isEmpty()) {
                mainPresenter.loadSearchResultsFirst(targetVideoId, searchKeyword);
            } else {
                mainPresenter.loadVideos(0, 5);
            }
        }

        // 加载相关搜索数据
        loadRelatedSearch();
    }

    /**
     * 处理页面切换逻辑
     * @param position 切换到的页面位置
     */
    private void handlePageChange(int position) {
        // 图片卡片项不需要播放控制
        if (!adapter.isVideoAtPosition(position)) {
            return;
        }

        // 1. 暂停所有正在播放的视频
        pauseAllVideos();

        // 2. 🔑 清理所有 controller 对旧 SurfaceView 的连接，防止解码器连接冲突
        detachAllSurfaces();

        // 3. 播放当前页面的视频
        playVideoAtPosition(position);

        // 4. 通知 Presenter 页面已切换
        if (mPresenter != null) {
            mPresenter.onVideoPageChanged(position);
        }
    }

    /**
     * 将所有 controller 从 SurfaceView 上断开
     * ViewPager2 会复用 SurfaceView，必须断开旧连接才能让新解码器绑定
     */
    private void detachAllSurfaces() {
        for (VideoPlayerController controller : playerControllers.values()) {
            if (controller != null) {
                controller.detachSurface();
            }
        }
    }

    /**
     * 播放指定位置的视频（视频播放的具体位置）
     * @param position 视频在列表中的位置
     */
    private void playVideoAtPosition(int position) {
        // 1. 释放远程位置的 controller（远离当前播放位置 > 1 格），控制内存
        releaseDistantControllers(position);

        // 2. 获取指定位置的视频数据
        Video video = adapter.getVideoAtPosition(position);
        if (video != null) {
            // 3. 获取对应位置的 VideoPlayerView
            VideoPlayerView playerView = adapter.getPlayerViewAtPosition(position);
            if (playerView != null) {
                // 4. 获取 VideoPlayerController
                VideoPlayerController controller = playerControllers.get(position);
                if (controller == null) {
                    // 统一从 PlayerManager 获取播放器（缓存命中 / 预加载中 / 降级创建，三者归一）
                    PlayerManager playerManager = null;
                    if (mPresenter != null && mPresenter instanceof MainPresenter) {
                        playerManager = ((MainPresenter) mPresenter).getPlayerManager();
                    }

                    if (playerManager != null) {
                        controller = playerManager.getPlayer(String.valueOf(video.getVideoId()), playerView.getSurfaceView()); // 让内部自己决定是复用还是创建新的ExoPlayer
                        if (controller != null) {
                            Log.d(TAG, "✓ Player obtained for: " + video.getTitle());
                        }
                    }

                    if (controller == null) {
                        Log.w(TAG, "Unable to obtain player for position " + position + ", skipping");
                        return;
                    }
                    playerControllers.put(position, controller);
                } else {
                    // 已有缓存的 controller，只换 SurfaceView（保留已缓冲的 ExoPlayer）
                    controller.attachSurfaceView(playerView.getSurfaceView());
                }
                // 5. 设置控制器、视频数据，开始播放
                playerView.setPlayerController(controller);
                playerView.setVideo(video);
                playerView.play();
                controller.startProgressUpdate();
            }
        }
    }

    /**
     * 暂停所有正在播放的视频
     */
    private void pauseAllVideos() {
        for (VideoPlayerController controller : playerControllers.values()) {
            if (controller != null && controller.isPlaying()) {
                controller.pause();           // 暂停播放
                controller.stopProgressUpdate(); // 停止进度更新
            }
        }
    }

    /**
     * 释放距离当前播放位置 >= 2 格远的 controller，控制内存峰值
     * 每个 ExoPlayer 约 40-80MB，需严格限制同时存活的实例数
     * maxLiveDistance=1: 仅保留当前播放位置和前/后各 1 格（共最多 3 个）
     * @param currentPosition 当前播放位置
     */
    private void releaseDistantControllers(int currentPosition) {
        int maxLiveDistance = 1; // 仅保留前后各 1 格内的 controller（共 ~3 个，~180MB）
        // 收集待释放的 entry，避免在遍历中修改 Map
        List<Integer> toRelease = new ArrayList<>();
        for (Map.Entry<Integer, VideoPlayerController> entry : playerControllers.entrySet()) {
            int pos = entry.getKey();
            if (Math.abs(pos - currentPosition) > maxLiveDistance) {
                toRelease.add(pos);
            }
        }
        for (int pos : toRelease) {
            VideoPlayerController controller = playerControllers.remove(pos);
            if (controller != null) {
                // 先 detachSurface 再 release，避免 Surface 竞争
                controller.detachSurface();
                controller.release();
                Log.d(TAG, "Released distant controller at position " + pos + " (current: " + currentPosition + ")");
            }
        }
        // 额外内存检查：如果堆使用率超过 80%，主动触发 GC 回收
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        if (usedMemory > maxMemory * 0.8) {
            Log.w(TAG, "High memory usage: " + (usedMemory / (1024 * 1024)) + "MB / " + (maxMemory / (1024 * 1024)) + "MB, requesting GC");
            System.gc();
        }
    }

    // 从搜索结果传入的目标视频ID（用于跳转到指定视频）
    private long targetVideoId = -1;

    /**
     * 显示视频列表（兼容旧接口，内部转为 setFeedItems）
     * @param videos 视频列表数据
     */
    @Override
    public void showVideoList(List<Video> videos) {
        adapter.setVideoList(videos);
        afterShowList(videos);
    }

    /**
     * 显示混合数据列表（视频 + 图片卡片混排）
     */
    @Override
    public void showFeedItems(List<Object> feedItems) {
        adapter.setFeedItems(feedItems);
        // 提取视频列表用于目标跳转
        List<Video> videos = new ArrayList<>();
        for (Object item : feedItems) {
            if (item instanceof Video) {
                videos.add((Video) item);
            }
        }
        afterShowList(videos);
    }

    /**
     * 列表显示后的通用处理：定位目标视频 + 预加载
     */
    private void afterShowList(List<Video> videos) {
        // 找到目标视频的位置（从搜索结果页面传入）
        int startPosition = 0;
        if (targetVideoId >= 0) {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                Video v = adapter.getVideoAtPosition(i);
                if (v != null && targetVideoId == v.getVideoId()) {
                    startPosition = i;
                    break;
                }
            }
            targetVideoId = -1; // 只跳转一次
        }

        // 跳转到目标位置并播放
        final int finalPosition = startPosition;
        viewPager.post(() -> {
            viewPager.setCurrentItem(finalPosition, false);
            // 只对视频项播放
            if (adapter.isVideoAtPosition(finalPosition)) {
                playVideoAtPosition(finalPosition);
            }
        });

        // 首次加载时预加载前后视频（提升首次体验）
        if (mPresenter != null && mPresenter instanceof MainPresenter) {
            MainPresenter mainPresenter = (MainPresenter) mPresenter;
            // 预加载紧跟后面的视频（跳过图片卡片）
            for (int offset = 1; offset <= 3; offset++) {
                int pos = startPosition + offset;
                if (pos < adapter.getItemCount() && adapter.isVideoAtPosition(pos)) {
                    mainPresenter.preloadUpcomingVideo(pos);
                    break; // 只预加载最近的一个
                }
            }
            // 预加载前面的视频
            for (int offset = 1; offset <= 3; offset++) {
                int pos = startPosition - offset;
                if (pos >= 0 && adapter.isVideoAtPosition(pos)) {
                    mainPresenter.preloadUpcomingVideo(pos);
                    break;
                }
            }
        }
    }

    /**
     * 显示加载进度条（转圈圈）
     */
    @Override
    public void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏加载进度条
     */
    @Override
    public void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    /**
     * 显示错误提示
     * @param message 错误信息
     */
    @Override
    public void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * 视频加载完成回调
     * @param video 加载完成的视频
     * @param position 视频位置
     */
    @Override
    public void onVideoLoaded(Video video, int position) {
        // 视频加载完成（预留扩展）
    }

    /**
     * 加载更多视频完成回调（兼容旧接口）
     */
    @Override
    public void onMoreVideosLoaded(List<Video> videos) {
        adapter.addVideos(videos);
    }

    /**
     * 加载更多混合数据完成回调
     */
    @Override
    public void onMoreFeedItemsLoaded(List<Object> feedItems) {
        adapter.addFeedItems(feedItems);
    }

    /**
     * 是否还有更多视频回调
     * @param hasMore 是否有更多数据
     */
    @Override
    public void hasMoreVideos(boolean hasMore) {
        // 是否还有更多视频（预留扩展）
    }

    /**
     * 视频暂停回调 —— presenter通知 View 暂停视频
     * @param position 要暂停的视频位置
     */
    @Override
    public void onVideoPaused(int position) {
        VideoPlayerController controller = playerControllers.get(position);
        if (controller != null && controller.isPlaying()) {
            controller.pause();
            controller.stopProgressUpdate();
        }
    }

    /**
     * 视频资源释放回调
     * @param position 要释放的视频位置
     */
    @Override
    public void onVideoReleased(int position) {
        VideoPlayerController controller = playerControllers.get(position);
        if (controller != null) {
            controller.release();           // 释放控制器资源
            playerControllers.remove(position); // 从 Map 中移除
        }

        VideoPlayerView playerView = adapter.getPlayerViewAtPosition(position);
        if (playerView != null) {
            playerView.release();  // 释放视图资源
        }
    }

    /**
     * Activity 暂停时调用
     * 暂停所有视频播放，节省资源
     */
    @Override
    protected void onPause() {
        super.onPause();
        pauseAllVideos();
        stopCarousel();
        // 停用方向监听器
        if (orientationListener != null) {
            orientationListener.disable();
        }
    }

    /**
     * Activity 恢复时调用
     * 继续播放当前页面的视频
     */
    @Override
    protected void onResume() {
        super.onResume();
        int currentPosition = viewPager.getCurrentItem();
        playVideoAtPosition(currentPosition);
        startCarousel();
        // 恢复方向监听器
        if (orientationListener != null && orientationListener.canDetectOrientation()) {
            orientationListener.enable();
        }
    }

    /**
     * Activity 销毁时调用
     * 释放所有播放器资源，防止内存泄漏
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 1.输出预加载统计报告
        if (mPresenter != null && mPresenter instanceof MainPresenter) {
            MainPresenter mainPresenter = (MainPresenter) mPresenter;
            mainPresenter.printPreloadReport();
            // 释放 PlayerManager 所有缓存（包括预加载池中的播放器）
            PlayerManager pm = mainPresenter.getPlayerManager();
            if (pm != null) {
                pm.releaseAll();
            }
        }

        // 2.释放所有播放器控制器
        for (VideoPlayerController controller : playerControllers.values()) {
            if (controller != null) {
                controller.detachSurface(); // 先断开 Surface 连接
                controller.release();       // 释放 ExoPlayer 实例
            }
        }
        playerControllers.clear(); // 清空缓存

        // 3.释放适配器中的所有视图资源
        if (adapter != null) {
            adapter.releaseAll(); // 释放所有 VideoPlayerView
        }

        // 4. 释放轮播定时器
        stopCarousel();

        // 5. 释放方向监听器
        if (orientationListener != null) {
            orientationListener.disable();
            orientationListener = null;
        }
    }

    // ==================== 底部相关搜索功能 ====================

    /**
     * 加载相关搜索数据（网络优先 + Mock 降级）
     */
    private void loadRelatedSearch() {
        com.example.bd_client_sidejob.data.repository.SearchRepositoryImpl repo =
                com.example.bd_client_sidejob.data.repository.SearchRepositoryImpl.getInstance(this);
        repo.loadRelatedSearchKeywords(keywords -> {
            relatedKeywords.clear();
            for (String keyword : keywords) {
                relatedKeywords.add(keyword);
            }
            // 在主线程更新UI
            runOnUiThread(this::updateRelatedSearchUI);
        });
    }

    /**
     * 更新相关搜索UI（上下轮播，一次显示一个）
     */
    private void updateRelatedSearchUI() {
        // 重置索引
        currentRelatedIndex = 0;
        
        // 显示第一个相关搜索
        if (!relatedKeywords.isEmpty()) {
            tvRelatedSearch.setText(relatedKeywords.get(currentRelatedIndex));
        }
        
        // 启动轮播
        startCarousel();
    }

    /**
     * 开始轮播（上下切换推荐词）
     */
    private void startCarousel() {
        if (carouselHandler == null) {
            carouselHandler = new Handler(Looper.getMainLooper());
        }

        stopCarousel();

        carouselRunnable = new Runnable() {
            @Override
            public void run() {
                if (relatedKeywords != null && relatedKeywords.size() > 1) {
                    // 切换到下一个推荐词
                    currentRelatedIndex = (currentRelatedIndex + 1) % relatedKeywords.size();
                    tvRelatedSearch.setText(relatedKeywords.get(currentRelatedIndex));
                }
                // this 指的是 当前匿名内部类 Runnable 的实例
                carouselHandler.postDelayed(this, CAROUSEL_INTERVAL);
            }
        };

        // 首次调用位置，确保轮播从第一个推荐词开始
        carouselHandler.postDelayed(carouselRunnable, CAROUSEL_INTERVAL);
    }

    /**
     * 停止轮播
     */
    private void stopCarousel() {
        if (carouselHandler != null && carouselRunnable != null) {
            carouselHandler.removeCallbacks(carouselRunnable);
        }
    }
}