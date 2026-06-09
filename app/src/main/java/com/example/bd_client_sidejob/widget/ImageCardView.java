package com.example.bd_client_sidejob.widget;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.bd_client_sidejob.R;
import com.example.bd_client_sidejob.data.model.ImageCard;

import java.util.List;

/**
 * 图片卡片视图（仿抖音图文模式）
 *
 * 核心机制：不依赖内嵌 ViewPager2（避免与外层垂直滑动冲突），
 * 而是直接用一个 ImageView + GestureDetector 检测左右滑动，
 * 用平移动画实现图片切换。
 *
 * 交互：
 * - 左右滑动 → 切换图片（带平移过渡动画）
 * - 双击 → 飘心动画
 * - 单击中间区域 → 回调外部
 */
public class ImageCardView extends ConstraintLayout {

    // ==================== 视图组件 ====================
    /** 当前显示的图片 */
    private ImageView ivImageContent;
    /** 页码指示器（如 "1 / 3"） */
    private TextView tvPageIndicator;
    /** 页码指示器容器 */
    private View llPageIndicator;
    /** 图片标题 */
    private TextView tvImageTitle;
    /** 图片标题容器 */
    private View llImageInfo;
    /** 图片提示文字 */
    private TextView tvImageHint;
    /** 飘心动画容器 */
    private FrameLayout flFloatHearts;

    // ==================== 数据 ====================
    /** 当前图片卡片数据 */
    private ImageCard currentCard;
    /** 图片 URL 列表 */
    private List<String> imageUrls;
    /** 当前页索引 */
    private int currentPageIndex = 0;
    /** 总页数 */
    private int totalPages = 1;

    // ==================== 手势检测 ====================
    private GestureDetector gestureDetector;
    /** 滑动阈值（dp） */
    private static final int SWIPE_THRESHOLD_DP = 60;
    private int swipeThresholdPx;
    /** 上次触摸的 X 坐标（用于实时平移动画） */
    private float lastTouchX;
    /** 拖动开始时 ImageView 的 translationX */
    private float startTranslationX;
    /** 是否正在拖动 */
    private boolean isDragging = false;

    // ==================== 飘心动画颜色 ====================
    private static final int[] HEART_COLORS = {
            0xFFFF4081, 0xFFFF80AB, 0xFFFF5252, 0xFFFF1744,
            0xFFE1BEE7, 0xFFCE93D8, 0xFFF48FB1, 0xFFF8BBD9
    };

    // ==================== 监听器 ====================
    private OnImageCardClickListener clickListener;

    public interface OnImageCardClickListener {
        void onSingleClick(ImageCard card);
    }

    public void setOnImageCardClickListener(OnImageCardClickListener listener) {
        this.clickListener = listener;
    }

    public ImageCardView(Context context) {
        super(context);
        init(context);
    }

    public ImageCardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ImageCardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.layout_image_card, this, true);

        ivImageContent = findViewById(R.id.ivImageContent);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);
        llPageIndicator = findViewById(R.id.llPageIndicator);
        tvImageTitle = findViewById(R.id.tvImageTitle);
        tvImageHint = findViewById(R.id.tvImageHint);
        llImageInfo = findViewById(R.id.llImageInfo);
        flFloatHearts = findViewById(R.id.flImageHearts);

        swipeThresholdPx = (int) (SWIPE_THRESHOLD_DP * context.getResources().getDisplayMetrics().density);

        // 手势检测器：只处理水平滑动
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || totalPages <= 1) return false;
                float diffX = e2.getX() - e1.getX();
                // 水平滑动距离 > 阈值且水平速度 > 垂直速度（防止垂直滑动被误判）
                if (Math.abs(diffX) > swipeThresholdPx && Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (diffX < 0) {
                        showNextImage();
                    } else {
                        showPreviousImage();
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                // 单击 → 回调外部
                if (clickListener != null && currentCard != null) {
                    clickListener.onSingleClick(currentCard);
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent e) {
                createFloatHeart(e.getX(), e.getY());
                return true;
            }
        });

        // 统一触摸入口
        ivImageContent.setOnTouchListener((v, event) -> {
            // 把事件传给 GestureDetector
            gestureDetector.onTouchEvent(event);

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getX();
                    startTranslationX = ivImageContent.getTranslationX();
                    isDragging = true;
                    // 请求父 View（外层垂直 ViewPager2）不要拦截触摸事件
                    requestParentDisallowIntercept(true);
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (isDragging) {
                        float dx = event.getX() - lastTouchX;
                        float newTransX = startTranslationX + dx;
                        // Clamp: 不能拖出第一张左边或最后一张右边
                        if (currentPageIndex == 0 && newTransX > 0) {
                            newTransX = newTransX * 0.3f; // 弹性阻尼
                        } else if (currentPageIndex == totalPages - 1 && newTransX < 0) {
                            newTransX = newTransX * 0.3f;
                        }
                        // 如果水平和垂直位移差不多，让外层接管
                        if (Math.abs(event.getY() - lastTouchX) > Math.abs(dx) * 1.5f) {
                            requestParentDisallowIntercept(false);
                            isDragging = false;
                        }
                        ivImageContent.setTranslationX(newTransX);
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    isDragging = false;
                    // 释放父 View 拦截权限
                    requestParentDisallowIntercept(false);
                    // 回弹动画
                    snapToCurrentImage();
                    break;
            }
            return true;
        });

        // 如果当前是图片卡片，也监听整个 View 的触摸防止漏掉
        setOnTouchListener((v, event) -> {
            // 把事件传给 gestureDetector
            gestureDetector.onTouchEvent(event);
            return true;
        });
    }

    /**
     * 请求父 View 不允许拦截触摸（核心：解决和外层 ViewPager2 的冲突）
     */
    private void requestParentDisallowIntercept(boolean disallow) {
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
        }
    }

    /**
     * 回弹到当前图片位置（平移结束后的吸附动画）
     */
    private void snapToCurrentImage() {
        ivImageContent.animate()
                .translationX(0)
                .setDuration(200)
                .withEndAction(() -> {
                    // 动画结束后重新加载当前图
                    loadCurrentImage();
                })
                .start();
    }

    /**
     * 显示上一张图片
     */
    private void showPreviousImage() {
        if (currentPageIndex <= 0) return;

        // 动画：当前图向右滑出，上一张从左边滑入
        currentPageIndex--;
        animateSwitchImage(ivImageContent.getWidth()); // 从右边来
    }

    /**
     * 显示下一张图片
     */
    private void showNextImage() {
        if (currentPageIndex >= totalPages - 1) return;

        // 动画：当前图向左滑出，下一张从右边滑入
        currentPageIndex++;
        animateSwitchImage(-ivImageContent.getWidth()); // 从左边来
    }

    /**
     * 图片切换动画（滑动→加载新图→归位）
     * @param fromX 动画起始偏移（正=从右边, 负=从左边）
     */
    private void animateSwitchImage(float fromX) {
        // 1. 先把 ImageView 移到 fromX 并设置新图片（不可见）
        ivImageContent.setTranslationX(fromX);
        // 切换图片和更新页码
        loadCurrentImage();
        updatePageIndicator();

        // 2. 平移动画归位到 0
        ivImageContent.animate()
                .translationX(0)
                .setDuration(250)
                .setListener(new Animator.AnimatorListener() {
                    @Override public void onAnimationStart(Animator animation) {
                        updatePageIndicator();
                    }
                    @Override public void onAnimationEnd(Animator animation) {
                        updatePageIndicator();
                    }
                    @Override public void onAnimationCancel(Animator animation) {}
                    @Override public void onAnimationRepeat(Animator animation) {}
                })
                .start();
    }

    // ==================== 数据绑定 ====================

    /**
     * 绑定图片卡片数据（多图模式）
     */
    public void setImageCard(ImageCard card) {
        this.currentCard = card;
        if (card == null) return;

        this.imageUrls = card.getImageUrls();
        this.currentPageIndex = 0;
        this.totalPages = imageUrls.size();

        tvImageTitle.setText(card.getTitle());

        if (totalPages > 1) {
            llPageIndicator.setVisibility(View.VISIBLE);
            tvImageHint.setVisibility(View.VISIBLE);
            tvImageHint.setText("← 左右滑动查看更多 →");
        } else {
            llPageIndicator.setVisibility(View.GONE);
            tvImageHint.setVisibility(View.GONE);
        }

        // 加载第一张图片
        loadCurrentImage();
        updatePageIndicator();
    }

    /**
     * 加载当前页的图片
     */
    private void loadCurrentImage() {
        if (imageUrls == null || currentPageIndex < 0 || currentPageIndex >= imageUrls.size()) return;

        String url = imageUrls.get(currentPageIndex);

        Glide.with(getContext())
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(android.R.color.black)
                .error(android.R.color.darker_gray)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        return false;
                    }
                })
                .into(ivImageContent);
    }

    /**
     * 更新页码指示器
     */
    private void updatePageIndicator() {
        if (totalPages <= 1) {
            llPageIndicator.setVisibility(View.GONE);
            return;
        }
        llPageIndicator.setVisibility(View.VISIBLE);
        tvPageIndicator.setText((currentPageIndex + 1) + " / " + totalPages);
    }

    // ==================== 飘心动画 ====================

    /**
     * 创建飘心动画（双击图片时）
     */
    private void createFloatHeart(float x, float y) {
        ImageView heart = new ImageView(getContext());
        heart.setImageResource(R.drawable.ic_heart_filled);
        heart.setColorFilter(HEART_COLORS[(int) (Math.random() * HEART_COLORS.length)]);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(80, 80);
        params.leftMargin = (int) x - 40;
        params.topMargin = (int) y - 40;
        flFloatHearts.addView(heart, params);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(heart, "translationY", 0, -250),
                ObjectAnimator.ofFloat(heart, "alpha", 1f, 0f),
                ObjectAnimator.ofFloat(heart, "scaleX", 1f, 1.5f, 0.8f),
                ObjectAnimator.ofFloat(heart, "scaleY", 1f, 1.5f, 0.8f)
        );
        set.setDuration(1200);
        set.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator a) {}
            @Override public void onAnimationEnd(Animator a) { flFloatHearts.removeView(heart); }
            @Override public void onAnimationCancel(Animator a) { flFloatHearts.removeView(heart); }
            @Override public void onAnimationRepeat(Animator a) {}
        });
        set.start();
    }

    // ==================== 公共方法 ====================

    public ImageCard getImageCard() {
        return currentCard;
    }

    /**
     * 释放资源
     */
    public void release() {
        Glide.with(getContext()).clear(ivImageContent);
    }
}
