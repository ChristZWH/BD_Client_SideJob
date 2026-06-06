package com.example.bd_client_sidejob.ui.main;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.viewpager2.widget.ViewPager2;

import com.example.bd_client_sidejob.R;
import com.example.bd_client_sidejob.base.BaseActivity;
import com.example.bd_client_sidejob.data.model.Video;
import com.example.bd_client_sidejob.ui.main.adapter.VideoFeedAdapter;
import com.example.bd_client_sidejob.util.VideoPlayerController;
import com.example.bd_client_sidejob.widget.VideoPlayerView;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 主活动类 - 视频播放应用的主入口
 * 采用 MVP 架构模式，作为 View 层负责 UI 展示和用户交互
 */
public class MainActivity extends BaseActivity<MainContract.Presenter> implements MainContract.View {

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

        // 创建视频列表适配器并绑定到 ViewPager2
        adapter = new VideoFeedAdapter(this);
        viewPager.setAdapter(adapter);

        // 设置 ViewPager2 为垂直滑动模式（类似抖音）
        viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        // 注册页面切换回调 - 当用户滑动切换页面时触发
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                handlePageChange(position); // 处理页面切换逻辑（暂停所有视频，播放当前视频）
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
        });

        // 使用 AndroidX 的 OnBackPressedDispatcher 处理返回键
        // 替代传统的 onBackPressed() 方法，更灵活可控
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            // 当用户按返回键时，系统自动触发
            @Override
            public void handleOnBackPressed() {
                finish(); // 关闭当前 Activity
            }
        });
    }

    /**
     * 初始化数据
     * View 层通知 Presenter 加载初始视频数据
     */
    @Override
    protected void initData() {
        if (mPresenter != null) {
            // 加载第0页，每页5条数据
            mPresenter.loadVideos(0, 5);
        }
    }

    /**
     * 处理页面切换逻辑
     * @param position 切换到的页面位置
     */
    private void handlePageChange(int position) {
        // 1. 暂停所有正在播放的视频
        pauseAllVideos();

        // 2. 播放当前页面的视频
        playVideoAtPosition(position);

        // 3. 通知 Presenter 页面已切换
        if (mPresenter != null) {
            mPresenter.onVideoPageChanged(position);
        }
    }

    /**
     * 播放指定位置的视频（视频播放的具体位置）
     * @param position 视频在列表中的位置
     */
    private void playVideoAtPosition(int position) {
        // 1. 获取指定位置的视频数据
        Video video = adapter.getVideoAtPosition(position);
        if (video != null) {
            // 2. 获取对应位置的 VideoPlayerView
            VideoPlayerView playerView = adapter.getPlayerViewAtPosition(position);
            if (playerView != null) {
                // 3. 获取或创建 VideoPlayerController
                VideoPlayerController controller = playerControllers.get(position);
                if (controller == null) {
                    controller = new VideoPlayerController();  // 创建新控制器
                    controller.initialize(this, playerView.getSurfaceView());  // 初始化
                    playerControllers.put(position, controller); // 缓存到 Map
                }
                // 4. 设置控制器、视频数据，开始播放
                playerView.setPlayerController(controller);
                playerView.setVideo(video);
                playerView.play();
                controller.startProgressUpdate(); // 开始进度更新（每秒回调）
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
     * 显示视频列表
     * @param videos 视频列表数据
     */
    @Override
    public void showVideoList(List<Video> videos) {
        adapter.setVideoList(videos);

        // 预加载第一个视频
        if (videos != null && !videos.isEmpty()) {
            playVideoAtPosition(0);
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
     * 加载更多视频完成回调，在视频列表后append
     * @param videos 新加载的视频列表
     */
    @Override
    public void onMoreVideosLoaded(List<Video> videos) {
        adapter.addVideos(videos);
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
    }

    /**
     * Activity 销毁时调用
     * 释放所有播放器资源，防止内存泄漏
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 释放所有播放器控制器
        for (VideoPlayerController controller : playerControllers.values()) {
            if (controller != null) {
                controller.release();
            }
        }
        playerControllers.clear();

        // 释放适配器中的所有视图资源
        if (adapter != null) {
            adapter.releaseAll();
        }
    }
}