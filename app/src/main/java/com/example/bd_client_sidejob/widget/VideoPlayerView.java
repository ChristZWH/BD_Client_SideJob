package com.example.bd_client_sidejob.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.example.bd_client_sidejob.R;
import com.example.bd_client_sidejob.data.model.Video;
import com.example.bd_client_sidejob.util.VideoPlayerController;

/**
 * 视频播放器视图
 * 自定义视图组件，集成视频播放功能和控制界面
 */
public class VideoPlayerView extends ConstraintLayout {

    /** 视频显示 SurfaceView就是一个硬件加速的画布，提供独立的绘制表面，不参与UI线程竞争，ExoPlayer解码后的视频帧“画”到这个Surface上 */
    private SurfaceView surfaceView;
    /** 播放/暂停按钮 */
    private ImageView ivPlayPause;
    /** 加载进度条 */
    private ProgressBar progressBar;
    /** 进度条 */
    private SeekBar seekBar;
    /** 当前时间显示 */
    private TextView tvCurrentTime;
    /** 总时长显示 */
    private TextView tvTotalTime;
    /** 视频标题 */
    private TextView tvTitle;
    /** 作者名称 */
    private TextView tvAuthor;
    /** 作者头像 */
    private ImageView ivAvatar;
    /** 点赞数 */
    private TextView tvLikeCount;
    /** 评论数 */
    private TextView tvCommentCount;
    /** 收藏数 */
    private TextView tvCollectCount;
    /** 分享数 */
    private TextView tvShareCount;
    /** 互动按钮区域 */
    private LinearLayout llInteraction;
    /** 控制容器 */
    private View controlContainer;

    /** 视频播放器控制器 */
    private VideoPlayerController playerController;
    /** 当前视频数据 */
    private Video currentVideo;
    /** 控制栏是否可见 */
    private boolean isControlsVisible = true;
    /** 控制栏自动隐藏延迟（毫秒） */
    private long hideControlsDelay = 3000;
    /** 控制栏隐藏 Handler */
    private android.os.Handler hideControlsHandler;
    /** 是否自动播放 */
    private boolean autoPlay = false;
    /** Surface 是否已准备好 */
    private boolean isSurfaceReady = false;
    /** 待播放的视频（Surface 准备好后播放） */
    private Video pendingVideo = null;

    /**
     * 构造函数（代码中创建调用）
     * @param context 上下文
     */
    public VideoPlayerView(Context context) {
        super(context);
        init(context);
    }

    /**
     * 构造函数（XML 中创建时调用，读取XML 属性）
     * @param context 上下文
     * @param attrs 属性集
     */
    public VideoPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * 构造函数（指定默认样式时调用）
     * @param context 上下文
     * @param attrs 属性集
     * @param defStyleAttr 默认样式属性
     */
    public VideoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化视图
     * @param context 上下文
     */
    private void init(Context context) {
        // 加载布局（三个参数：XML布局文件、父容器-当前的VideoPlayerView、是否将布局添加到父容器中）
        LayoutInflater.from(context).inflate(R.layout.layout_video_player, this, true);

        // 绑定视图
        surfaceView = findViewById(R.id.surfaceView);
        ivPlayPause = findViewById(R.id.ivPlayPause);
        progressBar = findViewById(R.id.progressBar);
        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);
        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        ivAvatar = findViewById(R.id.ivAvatar);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        tvCommentCount = findViewById(R.id.tvCommentCount);
        tvCollectCount = findViewById(R.id.tvCollectCount);
        tvShareCount = findViewById(R.id.tvShareCount);
        llInteraction = findViewById(R.id.llInteraction);
        controlContainer = findViewById(R.id.controlContainer);

        // 初始化 Handler。Handler 是 Android 的消息机制核心：用于线程间通信，可以延迟执行任务，这里用于 3秒后自动隐藏控制栏
        hideControlsHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        // 设置 SurfaceView 监听
        setupSurfaceViewListener();

        // 设置视图监听器
        setupListeners();
    }

    /**
     * 设置 SurfaceView 监听器
     * 确保在 Surface 准备好后才初始化播放器
     */
    private void setupSurfaceViewListener() {
        surfaceView.getHolder().addCallback(new android.view.SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(android.view.SurfaceHolder holder) {
                android.util.Log.d("VideoPlayerView", "Surface created");
                isSurfaceReady = true;

                // 如果 Surface 创建前就有待播放的视频，现在 Surface 准备好了，就开始播放
                if (pendingVideo != null && playerController != null) {
                    android.util.Log.d("VideoPlayerView", "Playing pending video");
                    setVideoInternal(pendingVideo);
                    pendingVideo = null;
                }
            }

            @Override
            public void surfaceChanged(android.view.SurfaceHolder holder, int format, int width, int height) {
                // Surface 尺寸变化
            }

            @Override
            public void surfaceDestroyed(android.view.SurfaceHolder holder) {
                android.util.Log.d("VideoPlayerView", "Surface destroyed");
                isSurfaceReady = false;
                // Surface 销毁时暂停播放
                if (playerController != null) {
                    playerController.pause();
                }
            }
        });
    }

    /**
     * 设置视图监听器
     */
    private void setupListeners() {
        // 播放/暂停按钮点击事件
        ivPlayPause.setOnClickListener(v -> {
            if (playerController != null) {
                if (playerController.isPlaying()) {
                    playerController.pause();
                } else {
                    playerController.play();
                }
            }
        });

        // 进度条拖动事件
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            // progress: 进度条当前进度值，fromUser: 是否由用户拖动触发（true=用户拖动，false=程序设置）只在用户拖动时才跳转，避免循环调用
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && playerController != null) {
                    playerController.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 暂停进度更新
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 恢复进度更新
            }
        });

        // 点击视频区域切换控制栏显示/隐藏
        surfaceView.setOnClickListener(v -> {
            toggleControls();
        });
    }

    /**
     * 设置播放器控制器
     * @param controller 播放器控制器
     */
    public void setPlayerController(VideoPlayerController controller) {
        this.playerController = controller;

        if (controller != null) {
            // 设置播放状态监听器
            controller.setPlayStateListener(new VideoPlayerController.PlayStateListener() {
                @Override
                public void onPlayStateChanged(boolean isPlaying) {
                    // 更新播放/暂停图标
                    updatePlayPauseIcon(isPlaying);
                    if (isPlaying) {
                        // 播放时自动隐藏控制栏
                        scheduleHideControls();
                    } else {
                        // 暂停时显示控制栏
                        cancelHideControls();
                        showControls();
                    }
                }

                // 播放器状态——>更新UI（与前面的onProgressChanged进行对比）
                @Override
                public void onProgressChanged(long position, long duration) {
                    // 更新进度显示，position是当前播放位置
                    updateProgress(position, duration);
                }

                @Override
                public void onError(String errorMessage) {
                    // 播放错误，隐藏进度条
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onPrepared() { // 视频解析完毕后Controller会回调
                    // 视频准备完成，隐藏进度条
                    progressBar.setVisibility(View.GONE);
                    long duration = playerController.getDuration();
                    tvTotalTime.setText(formatTime(duration)); // 设置总时长
                    seekBar.setMax((int) duration);

                    // 视频准备好后，如果设置了自动播放，则开始播放
                    if (autoPlay) {
                        playerController.play();
                        autoPlay = false;
                    }
                }
            });
        }
    }

    /**
     * 设置视频数据
     * @param video 视频对象
     */
    public void setVideo(Video video) {
        this.currentVideo = video;

        if (video != null) {
            // 设置视频信息（UI部分）
            tvTitle.setText(video.getTitle());
            tvAuthor.setText(video.getAuthor());
            tvLikeCount.setText(video.getFormattedLikeCount());
            tvCommentCount.setText(video.getFormattedCommentCount());
            tvCollectCount.setText(video.getFormattedCollectCount());
            tvShareCount.setText(video.getFormattedShareCount());

            // 加载作者头像(Glide 是 Android 最流行的图片加载库，自动处理缓存、异步加载、占位图等)
            Glide.with(getContext())
                    .load(video.getAvatar()) // 图片URL
                    .circleCrop() // 圆形裁剪
                    .into(ivAvatar); // 目标

            // 如果 Surface 已准备好，直接设置视频 URL
            if (isSurfaceReady && playerController != null) {
                setVideoInternal(video);
            } else {
                // Surface 还没准备好，先保存视频，等 Surface 准备好后播放
                pendingVideo = video;
                android.util.Log.d("VideoPlayerView", "Surface not ready, video pending");
            }
        }
    }

    /**
     * 内部方法：设置视频并开始准备
     * @param video 视频对象
     */
    private void setVideoInternal(Video video) {
        if (playerController != null) {
            // 转圈动画，加载进度条
            progressBar.setVisibility(View.VISIBLE);
            playerController.setVideoUrl(video.getUrl());
        }
    }

    /**
     * 开始播放
     */
    public void play() {
        if (playerController != null) {
            autoPlay = true;
            // 如果视频已经准备好，立即播放
            if (playerController.getDuration() > 0) {
                playerController.play();
                autoPlay = false;
            }
        }
    }

    /**
     * 暂停播放
     */
    public void pause() {
        if (playerController != null) {
            playerController.pause();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        if (playerController != null) {
            playerController.release();
        }
        cancelHideControls();
    }

    /**
     * 更新播放/暂停图标
     * @param isPlaying 是否正在播放
     */
    private void updatePlayPauseIcon(boolean isPlaying) {
        ivPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    /**
     * 更新播放进度
     * @param position 当前位置（毫秒）
     * @param duration 总时长（毫秒）
     */
    private void updateProgress(long position, long duration) {
        tvCurrentTime.setText(formatTime(position));
        seekBar.setProgress((int) position);
    }

    /**
     * 格式化时间显示
     * @param timeMs 时间（毫秒）
     * @return 格式化后的时间字符串（mm:ss）
     */
    private String formatTime(long timeMs) {
        long seconds = timeMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * 切换控制栏显示状态
     */
    private void toggleControls() {
        if (isControlsVisible) {
            hideControls();
        } else {
            showControls();
            if (playerController != null && playerController.isPlaying()) {
                scheduleHideControls();
            }
        }
    }

    /**
     * 显示控制栏
     */
    private void showControls() {
        isControlsVisible = true;
        controlContainer.setVisibility(View.VISIBLE);
        llInteraction.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏控制栏
     */
    private void hideControls() {
        isControlsVisible = false;
        controlContainer.setVisibility(View.GONE);
        llInteraction.setVisibility(View.GONE);
    }

    /**
     * 调度控制栏隐藏
     */
    private void scheduleHideControls() {
        cancelHideControls();
        hideControlsHandler.postDelayed(() -> {
            if (playerController != null && playerController.isPlaying()) {
                hideControls();
            }
        }, hideControlsDelay);
    }

    /**
     * 取消控制栏隐藏调度
     */
    private void cancelHideControls() {
        hideControlsHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 获取 SurfaceView
     * @return SurfaceView 实例
     */
    public SurfaceView getSurfaceView() {
        return surfaceView;
    }
}