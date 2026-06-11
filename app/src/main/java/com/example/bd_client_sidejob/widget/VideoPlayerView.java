package com.example.bd_client_sidejob.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.example.bd_client_sidejob.R;
import com.example.bd_client_sidejob.data.model.Video;
import com.example.bd_client_sidejob.util.VideoPlayerController;

/**
 * 视频播放器视图
 * 功能：单击播放/暂停、双击点赞、右侧固定操作按钮
 */
public class VideoPlayerView extends ConstraintLayout {

    // ==================== 视图组件 ====================
    /** 视频显示 SurfaceView就是一个硬件加速的画布，提供独立的绘制表面，不参与UI线程竞争，ExoPlayer解码后的视频帧“画”到这个Surface上 */
    private SurfaceView surfaceView;
    /** 加载进度条，视频缓冲时显示 */
    private ProgressBar progressBar;
    /** 飘心动画的容器，用于添加和移除飘动的爱心 */
    private FrameLayout flFloatHearts;
    /** 中央暂停/播放按钮（半透明圆形背景+三角形图标） */
    private ImageView ivPlayPause;
    /** 底部进度条，显示和拖动视频播放进度 */
    private SeekBar seekBar;
    /** 当前播放时间显示（如 00:15） */
    private TextView tvCurrentTime;
    /** 视频总时长显示（如 03:20） */
    private TextView tvTotalTime;

    // ==================== 视频信息区域（左下角） ====================
    /** 作者头像（视频信息区域） */
    private ImageView ivAvatar;
    /** 作者名称 */
    private TextView tvAuthor;
    /** 视频标题 */
    private TextView tvTitle;

    // ==================== 右侧操作按钮区域 ====================
    /** 右侧按钮区域容器（用户头像、点赞、评论、收藏、分享） */
    private LinearLayout llRightButtons;
    /** 用户头像按钮（右侧顶部） */
    private ImageView ivAvatarBtn;
    /** 点赞按钮容器（包含图标和数量） */
    private LinearLayout llLike;
    /** 点赞图标（爱心） */
    private ImageView ivLike;
    /** 点赞数量显示 */
    private TextView tvLikeCount;
    /** 评论按钮容器 */
    private LinearLayout llComment;
    /** 评论图标 */
    private ImageView ivComment;
    /** 评论数量显示 */
    private TextView tvCommentCount;
    /** 收藏按钮容器 */
    private LinearLayout llCollect;
    /** 收藏图标（星形） */
    private ImageView ivCollect;
    /** 收藏数量显示 */
    private TextView tvCollectCount;
    /** 分享按钮容器 */
    private LinearLayout llShare;
    /** 分享数量显示 */
    private TextView tvShareCount;

    // ==================== 全屏模式 ====================
    /** 当前是否处于横屏全屏模式 */
    private boolean isFullscreen = false;
    /** 全屏模式切换监听器 */
    private OnFullscreenToggleListener fullscreenToggleListener;

    /**
     * 全屏模式切换监听器接口
     */
    public interface OnFullscreenToggleListener {
        /**
         * 用户点击全屏/退出全屏按钮时回调
         * @param enterFullscreen true 表示进入全屏，false 表示退出全屏
         */
        void onToggleFullscreen(boolean enterFullscreen);
    }

    /**
     * 设置全屏模式切换监听器
     */
    public void setOnFullscreenToggleListener(OnFullscreenToggleListener listener) {
        this.fullscreenToggleListener = listener;
    }

    /**
     * 设置全屏模式（由 MainActivity 在横竖屏切换时调用）
     * 横屏时隐藏右侧按钮和视频信息，让视频铺满屏幕
     * 同时动态修改 ConstraintLayout 约束：解除对 llRightButtons 的依赖，改到 parent 右侧
     * @param fullscreen true 进入全屏模式，false 恢复正常模式
     */
    public void setFullscreenMode(boolean fullscreen) {
        this.isFullscreen = fullscreen;

        View llVideoInfo = findViewById(R.id.llVideoInfo);
        View llSeekBar = findViewById(R.id.llSeekBar);

        if (fullscreen) {
            // 横屏全屏：隐藏所有 UI 元素，只保留 SurfaceView
            llRightButtons.setVisibility(View.GONE);
            llVideoInfo.setVisibility(View.GONE);
            llSeekBar.setVisibility(View.GONE);
            llQualityMenu.setVisibility(View.GONE);
            llQualityToggle.setVisibility(View.GONE);

            // 🔑 动态修改约束：横屏时进度条和信息区域不再依赖右侧按钮，直接约束到 parent
            updateConstraint(llSeekBar, R.id.llRightButtons, ConstraintLayout.LayoutParams.PARENT_ID);
        } else {
            // 竖屏恢复：显示所有 UI 元素
            llRightButtons.setVisibility(View.VISIBLE);
            llVideoInfo.setVisibility(View.VISIBLE);
            llSeekBar.setVisibility(View.VISIBLE);
            llQualityToggle.setVisibility(View.VISIBLE);
            // 不恢复 llQualityMenu — 它可能之前就是隐藏的

            // 🔑 恢复约束：进度条和信息区域的右边重新依赖右侧按钮
            updateConstraint(llSeekBar, ConstraintLayout.LayoutParams.PARENT_ID, R.id.llRightButtons);
        }
    }

    /**
     * 修改 ConstraintLayout 中指定 View 的 endToStart 约束目标
     * @param view 要修改约束的 View
     * @param fromId 旧的约束目标 ID
     * @param toId 新的约束目标 ID
     */
    private void updateConstraint(View view, int fromId, int toId) {
        if (view == null) return;
        ConstraintLayout.LayoutParams lp = (ConstraintLayout.LayoutParams) view.getLayoutParams();
        if (lp != null) {
            lp.endToStart = toId;
            view.setLayoutParams(lp);
        }
    }

    /**
     * 获取当前是否全屏模式
     */
    public boolean isFullscreen() {
        return isFullscreen;
    }
    /** 清晰度快捷切换按钮（左下角，标题下方） */
    // ==================== 清晰度切换UI组件 ====================
    /** 清晰度快捷切换按钮（左下角，标题下方） */
    private LinearLayout llQualityToggle;
    /** 清晰度图标（HD 徽标） */
    private ImageView ivQualityIcon;
    /** 当前清晰度文字标签（如 720p） */
    private TextView tvQualityLabel;
    /** 清晰度选择菜单容器 */
    private LinearLayout llQualityMenu;
    /** 360p 选项 */
    private TextView tvQuality360p;
    /** 720p 选项 */
    private TextView tvQuality720p;
    /** 清晰度菜单是否可见 */
    private boolean isQualityMenuVisible = false;

    // ==================== 播放器相关 ====================
    /** 视频播放器控制器（基于 ExoPlayer 封装） */
    private VideoPlayerController playerController;
    /** 当前播放的视频数据 */
    private Video currentVideo;
    /** 是否自动播放（用于 Surface 准备好后自动开始） */
    private boolean autoPlay = false;
    /** SurfaceView 是否已准备好（surfaceCreated 回调后变为 true） */
    private boolean isSurfaceReady = false;
    /** 待播放的视频（Surface 未准备好时暂存） */
    private Video pendingVideo = null;
    /** 视频准备完成监听器 */
    private OnVideoPreparedListener preparedListener;

    /**
     * 视频准备完成监听器接口
     */
    public interface OnVideoPreparedListener {
        void onVideoPrepared();
    }

    /**
     * 设置视频准备完成监听器
     */
    public void setOnVideoPreparedListener(OnVideoPreparedListener listener) {
        this.preparedListener = listener;
    }

    // ==================== 状态标志 ====================
    /** 当前是否已点赞 */
    private boolean isLiked = false;
    /** 当前是否已收藏 */
    private boolean isCollected = false;
    /** 当前点赞数 */
    private int likeCount = 0;
    /** 当前收藏数 */
    private int collectCount = 0;

    // ==================== 双击检测 ====================
    /** 上次点击时间戳，用于判断双击 */
    private long lastClickTime = 0;
    /** 双击判定间隔（毫秒），300ms 内两次点击视为双击 */
    private static final long DOUBLE_CLICK_DELAY = 300;

    // ==================== 飘心动画颜色 ====================
    /** 飘心动画的随机颜色数组（粉色系） */
    private static final int[] HEART_COLORS = {
            0xFFFF4081, 0xFFFF80AB, 0xFFFF5252, 0xFFFF1744,
            0xFFE1BEE7, 0xFFCE93D8, 0xFFF48FB1, 0xFFF8BBD9
    };

    // ==================== 构造方法 ====================
    public VideoPlayerView(Context context) {
        super(context);
        init(context);
    }

    public VideoPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public VideoPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    // ==================== 初始化 ====================
    /**
     * 初始化视图：加载布局、绑定视图、设置监听器
     */
    private void init(Context context) {
        // 加载布局文件
        LayoutInflater.from(context).inflate(R.layout.layout_video_player, this, true);

        // 绑定视图组件
        bindViews();

        // 设置各种监听器（点击、触摸、进度条等）
        setupListeners();

        // 设置 SurfaceView 生命周期监听
        setupSurfaceViewListener();
    }

    /**
     * 绑定所有视图组件（通过 findViewById）
     */
    private void bindViews() {
        // 视频播放相关
        surfaceView = findViewById(R.id.surfaceView);
        progressBar = findViewById(R.id.progressBar);
        flFloatHearts = findViewById(R.id.flFloatHearts);
        ivPlayPause = findViewById(R.id.ivPlayPause);
        seekBar = findViewById(R.id.seekBar);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime = findViewById(R.id.tvTotalTime);

        // 视频信息区域
        ivAvatar = findViewById(R.id.ivAvatar);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvTitle = findViewById(R.id.tvTitle);

        // 右侧操作按钮
        ivAvatarBtn = findViewById(R.id.ivAvatarBtn);
        llLike = findViewById(R.id.llLike);
        ivLike = findViewById(R.id.ivLike);
        tvLikeCount = findViewById(R.id.tvLikeCount);
        llComment = findViewById(R.id.llComment);
        ivComment = findViewById(R.id.ivComment);
        tvCommentCount = findViewById(R.id.tvCommentCount);
        llCollect = findViewById(R.id.llCollect);
        ivCollect = findViewById(R.id.ivCollect);
        tvCollectCount = findViewById(R.id.tvCollectCount);
        llShare = findViewById(R.id.llShare);
        tvShareCount = findViewById(R.id.tvShareCount);
        llRightButtons = findViewById(R.id.llRightButtons);

        // 清晰度切换组件
        llQualityToggle = findViewById(R.id.llQualityToggle);
        ivQualityIcon = findViewById(R.id.ivQualityIcon);
        tvQualityLabel = findViewById(R.id.tvQualityLabel);
        llQualityMenu = findViewById(R.id.llQualityMenu);
        tvQuality360p = findViewById(R.id.tvQuality360p);
        tvQuality720p = findViewById(R.id.tvQuality720p);
    }

    // ==================== SurfaceView 生命周期 ====================
    /**
     * 设置 SurfaceView 的生命周期回调
     * 确保 Surface 准备好后再开始播放视频
     */
    private void setupSurfaceViewListener() {
        // 系统在合适的时机自动回调：
        surfaceView.getHolder().addCallback(new android.view.SurfaceHolder.Callback() {
            // Surface 创建时（视图显示时）
            @Override
            public void surfaceCreated(android.view.SurfaceHolder holder) {
                isSurfaceReady = true;
                // 如果有待播放的视频，立即播放
                if (pendingVideo != null && playerController != null) {
                    setVideoInternal(pendingVideo);
                    pendingVideo = null;
                }
            }

            // Surface 尺寸变化时
            @Override
            public void surfaceChanged(android.view.SurfaceHolder holder, int format, int width, int height) {
                // Surface 尺寸变化时触发，当前无需处理
            }

            // Surface 销毁时（视图隐藏/销毁时）
            @Override
            public void surfaceDestroyed(android.view.SurfaceHolder holder) {
                isSurfaceReady = false;
                // Surface 销毁时暂停播放，避免异常
                if (playerController != null) {
                    playerController.pause();
                }
            }
        });
    }

    // ==================== 监听器设置 ====================
    /**
     * 设置所有交互监听器
     * 包括：视频画面触摸、暂停按钮、进度条、点赞、评论、收藏、分享
     */
    private void setupListeners() {
        // 视频画面触摸事件（处理单击播放/暂停、双击点赞）
        surfaceView.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    handleClick(event.getX(), event.getY());
                }
                return true; // 消费触摸事件，防止继续传递
            }
        });

        // 中央暂停按钮点击事件：点击后恢复播放
        ivPlayPause.setOnClickListener(v -> {
            if (playerController != null) {
                playerController.play();
                ivPlayPause.setVisibility(View.GONE);
            }
        });

        // 进度条拖动事件：拖动时跳转到对应位置
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // fromUser 为 true 表示是用户拖动触发的
                if (fromUser && playerController != null) {
                    playerController.seekTo(progress);
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 用户开始拖动进度条
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 用户停止拖动进度条
            }
        });

        // 点赞按钮点击事件
        llLike.setOnClickListener(v -> toggleLike());

        // 评论按钮点击事件（暂未实现）
        llComment.setOnClickListener(v -> {
            Toast.makeText(getContext(), "评论功能待接入", Toast.LENGTH_SHORT).show();
        });

        // 收藏按钮点击事件
        llCollect.setOnClickListener(v -> toggleCollect());

        // 分享按钮点击事件（暂未实现）
        llShare.setOnClickListener(v -> {
            Toast.makeText(getContext(), "分享功能待接入", Toast.LENGTH_SHORT).show();
        });

        // 头像按钮点击事件（暂未实现）
        ivAvatarBtn.setOnClickListener(v -> {
            Toast.makeText(getContext(), "用户主页待接入", Toast.LENGTH_SHORT).show();
        });

        // 清晰度快捷切换按钮点击事件（切换清晰度菜单显示/隐藏）
        llQualityToggle.setOnClickListener(v -> toggleQualityMenu());

        // 清晰度菜单选项点击事件
        tvQuality360p.setOnClickListener(v -> switchToQuality(VideoPlayerController.QUALITY_360P));
        tvQuality720p.setOnClickListener(v -> switchToQuality(VideoPlayerController.QUALITY_720P));
    }

    // ==================== 点击事件处理 ====================
    /**
     * 处理视频画面的点击事件，区分单击和双击
     * 单击：播放/暂停
     * 双击：点赞 + 飘心动画
     *
     * @param x 点击的 X 坐标
     * @param y 点击的 Y 坐标
     */
    private void handleClick(float x, float y) {
        long currentTime = System.currentTimeMillis();

        // 如果点击在右侧按钮区域，不处理（让按钮自己的点击事件处理）
        if (isClickOnRightButtons((int) x, (int) y)) {
            return;
        }

        // 判断点击位置是否在进度条区域
        if (isClickOnSeekBar((int) x, (int) y)) {
            return;
        }

        // 如果清晰度菜单可见，不处理播放区域的单击（避免冲突）
        if (isQualityMenuVisible) {
            return;
        }

        // 双击检测：两次点击间隔小于 DOUBLE_CLICK_DELAY 视为双击
        if (currentTime - lastClickTime < DOUBLE_CLICK_DELAY) {
            // 双击：执行点赞 + 飘心动画
            handleDoubleClick(x, y);
            lastClickTime = 0; // 重置，避免连续触发
        } else {
            // 可能是单击，延迟执行以等待是否会有第二次点击
            postDelayed(() -> {
                // 延迟后检查，如果点击时间没有被重置，说明是单击
                if (System.currentTimeMillis() - lastClickTime >= DOUBLE_CLICK_DELAY) {
                    handleSingleClick();
                }
            }, DOUBLE_CLICK_DELAY);
        }

        lastClickTime = currentTime;
    }

    /**
     * 判断点击位置是否在右侧按钮区域
     * 用于避免单击/双击事件与按钮点击冲突
     */
    private boolean isClickOnRightButtons(int x, int y) {
        int[] location = new int[2];
        llRightButtons.getLocationOnScreen(location);
        int btnLeft = location[0];
        int btnTop = location[1];
        int btnRight = btnLeft + llRightButtons.getWidth();
        int btnBottom = btnTop + llRightButtons.getHeight();

        return x >= btnLeft && x <= btnRight && y >= btnTop && y <= btnBottom;
    }

    /**
     * 判断点击位置是否在进度条区域
     * 用于避免单击/双击事件与进度条拖动冲突
     */
    private boolean isClickOnSeekBar(int x, int y) {
        int[] location = new int[2];
        seekBar.getLocationOnScreen(location);
        int seekLeft = location[0];
        int seekTop = location[1];
        int seekRight = seekLeft + seekBar.getWidth();
        int seekBottom = seekTop + seekBar.getHeight();

        return x >= seekLeft && x <= seekRight && y >= seekTop && y <= seekBottom;
    }

    // ==================== 单击/双击处理 ====================
    /**
     * 处理单击事件：播放/暂停切换
     * 播放时隐藏中央暂停按钮，暂停时显示
     */
    private void handleSingleClick() {
        if (playerController != null) {
            if (playerController.isPlaying()) {
                playerController.pause();
                ivPlayPause.setVisibility(View.VISIBLE); // 显示暂停按钮
            } else {
                playerController.play();
                ivPlayPause.setVisibility(View.GONE); // 隐藏暂停按钮
            }
        }
    }

    /**
     * 处理双击事件：点赞 + 飘心动画
     *
     * @param x 双击位置的 X 坐标（飘心动画起点）
     * @param y 双击位置的 Y 坐标（飘心动画起点）
     */
    private void handleDoubleClick(float x, float y) {
        // 执行点赞/取消点赞
        toggleLike();
        // 在点击位置创建飘心动画
        createFloatHeartAnimation(x, y);
    }

    // ==================== 点赞功能 ====================
    /**
     * 点赞/取消点赞切换
     * 更新点赞状态和图标显示
     */
    private void toggleLike() {
        if (!isLiked) {
            // 点赞：变为红色实心爱心，数量+1
            isLiked = true;
            likeCount++;
            ivLike.setImageResource(R.drawable.ic_heart_filled);
        } else {
            // 取消点赞：变为白色空心爱心，数量-1
            isLiked = false;
            likeCount--;
            ivLike.setImageResource(R.drawable.ic_heart_outline);
        }
        tvLikeCount.setText(formatCount(likeCount));
    }

    // ==================== 收藏功能 ====================
    /**
     * 收藏/取消收藏切换
     * 更新收藏状态和图标显示
     */
    private void toggleCollect() {
        if (!isCollected) {
            // 收藏：变为黄色实心星星，数量+1
            isCollected = true;
            collectCount++;
            ivCollect.setImageResource(R.drawable.ic_star_filled);
        } else {
            // 取消收藏：变为灰色空心星星，数量-1
            isCollected = false;
            collectCount--;
            ivCollect.setImageResource(R.drawable.ic_star_outline);
        }
        tvCollectCount.setText(formatCount(collectCount));
    }

    // ==================== 飘心动画 ====================
    /**
     * 创建飘心动画
     * 在指定位置创建一个爱心，执行向上飘动、缩放、透明度变化的动画
     *
     * @param startX 动画起始 X 坐标
     * @param startY 动画起始 Y 坐标
     */
    private void createFloatHeartAnimation(float startX, float startY) {
        // 创建爱心图片
        ImageView heart = new ImageView(getContext());
        heart.setImageResource(R.drawable.ic_heart_filled);
        // 设置随机颜色
        heart.setColorFilter(HEART_COLORS[(int) (Math.random() * HEART_COLORS.length)]);

        // 设置布局参数（60x60 大小，居中于点击位置）
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(60, 60);
        params.leftMargin = (int) startX - 30;
        params.topMargin = (int) startY - 30;
        flFloatHearts.addView(heart, params);

        // 创建动画集合
        AnimatorSet animatorSet = new AnimatorSet();

        // 缩放动画：先放大再恢复
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(heart, "scaleX", 1f, 1.5f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(heart, "scaleY", 1f, 1.5f, 1f);

        // 向上移动动画：向上飘动 300 像素
        ObjectAnimator translateY = ObjectAnimator.ofFloat(heart, "translationY", 0, -300);

        // 水平摇摆动画：随机左右偏移
        ObjectAnimator translateX = ObjectAnimator.ofFloat(heart, "translationX", 0,
                (float) (Math.random() - 0.5) * 100);

        // 透明度动画：从完全不透明到完全透明
        ObjectAnimator alpha = ObjectAnimator.ofFloat(heart, "alpha", 1f, 0f);

        // 随机动画时长（1000ms - 1500ms）
        long duration = 1000 + (long) (Math.random() * 500);

        // 组合所有动画同时执行
        animatorSet.playTogether(scaleX, scaleY, translateY, translateX, alpha);
        animatorSet.setDuration(duration);

        // 动画结束后移除爱心，避免内存泄漏
        animatorSet.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                flFloatHearts.removeView(heart);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                flFloatHearts.removeView(heart);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        animatorSet.start();
    }

    // ==================== 格式化工具方法 ====================
    /**
     * 格式化数字显示
     * 例如：12345 -> 12.3k，1234567 -> 123.5w
     *
     * @param count 数字
     * @return 格式化后的字符串
     */
    private String formatCount(int count) {
        if (count >= 10000) {
            return String.format("%.1fw", count / 10000.0);
        } else if (count >= 1000) {
            return String.format("%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }

    /**
     * 格式化时间显示
     * 将毫秒转换为 mm:ss 格式
     *
     * @param timeMs 时间（毫秒）
     * @return 格式化后的字符串（如 03:20）
     */
    private String formatTime(long timeMs) {
        long seconds = timeMs / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // ==================== 播放器控制器设置 ====================
    /**
     * 设置播放器控制器
     * 并设置播放状态监听器，用于更新 UI（暂停按钮、进度条等）
     *
     * @param controller 视频播放器控制器
     */
    public void setPlayerController(VideoPlayerController controller) {
        this.playerController = controller;

        if (controller != null) {
            controller.setPlayStateListener(new VideoPlayerController.PlayStateListener() {
                @Override
                public void onPlayStateChanged(boolean isPlaying) {
                    // 根据播放状态显示/隐藏中央暂停按钮
                    ivPlayPause.setVisibility(isPlaying ? View.GONE : View.VISIBLE);
                    // 开始播放时启动进度更新
                    if (isPlaying) {
                        playerController.startProgressUpdate();
                    }
                }

                @Override
                public void onProgressChanged(long position, long duration) {
                    // 更新当前时间和进度条位置
                    tvCurrentTime.setText(formatTime(position));
                    seekBar.setProgress((int) position);
                }

                @Override
                public void onError(String errorMessage) {
                    // 播放出错时隐藏加载进度条
                    progressBar.setVisibility(View.GONE);
                }

                @Override
                public void onPrepared() {
                    // 视频准备完成：隐藏加载条、设置总时长、开始播放
                    progressBar.setVisibility(View.GONE);  // 隐藏加载动画
                    long duration = playerController.getDuration(); // 获取总时长
                    tvTotalTime.setText(formatTime(duration)); // 显示总时长
                    seekBar.setMax((int) duration); // 设置进度条最大值

                    // 通知监听器视频准备完成（用于统计起播延迟）
                    if (preparedListener != null) {
                        preparedListener.onVideoPrepared();
                    }

                    if (autoPlay) {
                        playerController.play();
                        autoPlay = false;
                    }
                }
            });
        }
    }

    // ==================== 视频数据设置 ====================
    /**
     * 设置视频数据
     * 更新视频信息、作者信息、点赞/收藏数等 UI
     *
     * @param video 视频数据对象
     */
    public void setVideo(Video video) {
        this.currentVideo = video;

        if (video != null) {
            // 设置视频标题和作者
            tvTitle.setText(video.getTitle());
            tvAuthor.setText(video.getAuthor());

            // 初始化点赞和收藏状态（默认未点赞、未收藏）
            likeCount = video.getLikeCount();
            collectCount = video.getCollectCount();
            isLiked = false;
            isCollected = false;

            // 设置数量显示
            tvLikeCount.setText(formatCount(likeCount));
            tvCommentCount.setText(video.getFormattedCommentCount());
            tvCollectCount.setText(formatCount(collectCount));
            tvShareCount.setText(video.getFormattedShareCount());

            // ==================== 头像本地渲染 ====================
            // 不再使用 Glide 加载网络图片 —— 用纯色圆形本地渲染，0ms 展示，不依赖网络
            String avatarStr = video.getAvatar();
            int avatarColor = 0xFF4ECDC4; // 默认颜色
            try {
                avatarColor = Integer.parseInt(avatarStr);
            } catch (NumberFormatException ignored) {
                // 如果 avatar 不是颜色值（比如是 URL），使用默认颜色
            }

            // 头像用纯色圆形
            android.graphics.drawable.GradientDrawable avatarDrawable = new android.graphics.drawable.GradientDrawable();
            avatarDrawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            avatarDrawable.setColor(avatarColor);
            ivAvatar.setImageDrawable(avatarDrawable);

            android.graphics.drawable.GradientDrawable avatarBtnDrawable = new android.graphics.drawable.GradientDrawable();
            avatarBtnDrawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            avatarBtnDrawable.setColor(avatarColor);
            ivAvatarBtn.setImageDrawable(avatarBtnDrawable);

            // 设置图标为默认状态（未点赞、未收藏）
            ivLike.setImageResource(R.drawable.ic_heart_outline);
            ivCollect.setImageResource(R.drawable.ic_star_outline);

            // 如果 Surface 已准备好，直接播放；否则暂存等待
            if (isSurfaceReady && playerController != null) {
                setVideoInternal(video);
            } else {
                pendingVideo = video;
            }
        }
    }

    /**
     * 内部方法：设置视频 URL 并开始加载
     * 如果预加载播放器已经缓冲了同一个 URL，setVideoUrl 会跳过重复 prepare
     * VideoPlayerView 自己只管"拿到一个 controller → 调用 setVideoUrl → 等待 onPrepared → 播放"，它不需要知道这个 controller 是预加载过的还是刚创建的
     */
    private void setVideoInternal(Video video) {
        if (playerController != null) {
            progressBar.setVisibility(View.VISIBLE); // 显示加载进度
            // 使用单参方法，内部委托给两参方法（preloadMode=false）
            // 如果 URL 已缓冲好，内部会跳过 prepare，直接触发 onPrepared
            playerController.setVideoUrl(video.getUrl());
        }
    }

    // ==================== 清晰度切换 ====================

    /**
     * 切换清晰度菜单显示/隐藏
     * 点击左下角清晰度标签时触发
     */
    private void toggleQualityMenu() {
        if (isQualityMenuVisible) {
            // 隐藏菜单 — 使用简单的消失动画
            llQualityMenu.setVisibility(View.GONE);
            llQualityToggle.setBackgroundResource(R.drawable.bg_quality_option_unselected);
        } else {
            // 显示菜单 — 更新选中状态后展示
            updateQualityMenuSelection();
            llQualityMenu.setVisibility(View.VISIBLE);
            llQualityToggle.setBackgroundResource(R.drawable.bg_quality_option);
        }
        isQualityMenuVisible = !isQualityMenuVisible;
    }

    /**
     * 执行清晰度切换
     * 委托给 VideoPlayerController.switchQuality() 处理核心逻辑
     *
     * @param quality 目标清晰度（QUALITY_360P 或 QUALITY_720P）
     */
    private void switchToQuality(int quality) {
        if (playerController == null || currentVideo == null) {
            return;
        }

        // 同一清晰度不重复切换
        if (quality == playerController.getCurrentQuality()) {
            hideQualityMenu();
            return;
        }

        // 获取视频的清晰度 URL
        String url360p = currentVideo.getQuality360p();
        String url720p = currentVideo.getQuality720p();

        // 执行切换（控制器内部会保持播放进度）
        playerController.switchQuality(quality, url360p, url720p);

        // 更新 UI 并隐藏菜单
        updateQualityLabel(quality);
        updateQualityMenuSelection();
        hideQualityMenu();
    }

    /**
     * 隐藏清晰度菜单
     */
    private void hideQualityMenu() {
        llQualityMenu.setVisibility(View.GONE);
        llQualityToggle.setBackgroundResource(R.drawable.bg_quality_option_unselected);
        isQualityMenuVisible = false;
    }

    /**
     * 更新清晰度标签文字
     * 切换成功后，左下角标签更新为当前清晰度
     *
     * @param quality 当前清晰度
     */
    private void updateQualityLabel(int quality) {
        if (quality == VideoPlayerController.QUALITY_720P) {
            tvQualityLabel.setText("720p");
        } else if (quality == VideoPlayerController.QUALITY_360P) {
            tvQualityLabel.setText("360p");
        }
    }

    /**
     * 更新清晰度菜单中选中项的视觉状态
     * 当前清晰度 → 粉色背景，未选中 → 半透明背景
     */
    private void updateQualityMenuSelection() {
        int currentQuality = playerController != null
                ? playerController.getCurrentQuality()
                : VideoPlayerController.QUALITY_DEFAULT;

        if (currentQuality == VideoPlayerController.QUALITY_360P) {
            tvQuality360p.setBackgroundResource(R.drawable.bg_quality_option);
            tvQuality720p.setBackgroundResource(R.drawable.bg_quality_option_unselected);
        } else {
            tvQuality720p.setBackgroundResource(R.drawable.bg_quality_option);
            tvQuality360p.setBackgroundResource(R.drawable.bg_quality_option_unselected);
        }
    }

    // ==================== 播放控制 ====================
    /**
     * 开始播放视频
     */
    public void play() {
        if (playerController != null) {
            autoPlay = true;
            // 如果视频已准备好，直接播放
            if (playerController.getDuration() > 0) {
                playerController.play();
                autoPlay = false;
            }
            // 否则等待 onPrepared 回调后自动播放
        }
    }

    /**
     * 暂停播放视频
     */
    public void pause() {
        if (playerController != null) {
            playerController.pause();
        }
    }

    /**
     * 释放播放器资源
     * 页面销毁时调用，避免内存泄漏
     */
    public void release() {
        if (playerController != null) {
            playerController.release();
        }
    }

    /**
     * 获取 SurfaceView 实例
     */
    public SurfaceView getSurfaceView() {
        return surfaceView;
    }

    /**
     * 获取当前播放状态
     *
     * @return true 表示正在播放
     */
    public boolean isPlaying() {
        return playerController != null && playerController.isPlaying();
    }
}
