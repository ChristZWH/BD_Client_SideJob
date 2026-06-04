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

public class MainActivity extends BaseActivity<MainContract.Presenter> implements MainContract.View {

    private ViewPager2 viewPager;
    private ProgressBar progressBar;
    private VideoFeedAdapter adapter;
    private Map<Integer, VideoPlayerController> playerControllers = new WeakHashMap<>();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected MainContract.Presenter createPresenter() {
        return new MainPresenter();
    }

    @Override
    protected void initView() {
        viewPager = findViewById(R.id.viewPager);
        progressBar = findViewById(R.id.progressBar);

        adapter = new VideoFeedAdapter(this);
        viewPager.setAdapter(adapter);

        viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                handlePageChange(position);
            }
        });

        adapter.setPageChangeListener(new VideoFeedAdapter.OnVideoPageChangeListener() {
            @Override
            public void onPageChanged(int position) {
                // 页面切换处理
            }

            @Override
            public void onLoadMore() {
                if (mPresenter != null) {
                    mPresenter.loadMoreVideos();
                }
            }
        });

        // 使用 AndroidX 的 OnBackPressedDispatcher 处理返回键
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    @Override
    protected void initData() {
        if (mPresenter != null) {
            mPresenter.loadVideos(0, 5);
        }
    }

    private void handlePageChange(int position) {
        // 暂停所有视频
        pauseAllVideos();

        // 播放当前视频
        playVideoAtPosition(position);

        // 通知Presenter
        if (mPresenter != null) {
            mPresenter.onVideoPageChanged(position);
        }
    }

    private void playVideoAtPosition(int position) {
        Video video = adapter.getVideoAtPosition(position);
        if (video != null) {
            VideoPlayerView playerView = adapter.getPlayerViewAtPosition(position);
            if (playerView != null) {
                VideoPlayerController controller = playerControllers.get(position);
                if (controller == null) {
                    controller = new VideoPlayerController();
                    controller.initialize(this, playerView.getSurfaceView());
                    playerControllers.put(position, controller);
                }
                playerView.setPlayerController(controller);
                playerView.setVideo(video);
                playerView.play();
                controller.startProgressUpdate();
            }
        }
    }

    private void pauseAllVideos() {
        for (VideoPlayerController controller : playerControllers.values()) {
            if (controller != null && controller.isPlaying()) {
                controller.pause();
                controller.stopProgressUpdate();
            }
        }
    }

    @Override
    public void showVideoList(List<Video> videos) {
        adapter.setVideoList(videos);

        // 预加载第一个视频
        if (videos != null && !videos.isEmpty()) {
            playVideoAtPosition(0);
        }
    }

    @Override
    public void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideLoading() {
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onVideoLoaded(Video video, int position) {
        // 视频加载完成
    }

    @Override
    public void onMoreVideosLoaded(List<Video> videos) {
        adapter.addVideos(videos);
    }

    @Override
    public void hasMoreVideos(boolean hasMore) {
        // 是否还有更多视频
    }

    @Override
    protected void onPause() {
        super.onPause();
        pauseAllVideos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int currentPosition = viewPager.getCurrentItem();
        playVideoAtPosition(currentPosition);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 释放所有播放器
        for (VideoPlayerController controller : playerControllers.values()) {
            if (controller != null) {
                controller.release();
            }
        }
        playerControllers.clear();

        if (adapter != null) {
            adapter.releaseAll();
        }
    }
}