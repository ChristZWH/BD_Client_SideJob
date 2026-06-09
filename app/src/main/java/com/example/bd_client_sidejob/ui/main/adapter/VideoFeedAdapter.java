package com.example.bd_client_sidejob.ui.main.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bd_client_sidejob.data.model.ImageCard;
import com.example.bd_client_sidejob.data.model.Video;
import com.example.bd_client_sidejob.widget.ImageCardView;
import com.example.bd_client_sidejob.widget.VideoPlayerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VideoFeedAdapter - 视频流列表适配器（支持视频 + 图片卡片混排）
 * ViewPager2（底层是 RecyclerView）通过 Adapter 管理数据和创建视图
 * 职责：
 * 1. 持有视频列表数据
 * 2. 创建和复用 VideoPlayerView / ImageCardView
 * 3. 将数据绑定到视图
 * 4. 处理加载更多逻辑
 * 5. 管理播放器视图的生命周期
 */
public class VideoFeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // ==================== 视图类型常量 ====================
    private static final int TYPE_VIDEO = 0;
    private static final int TYPE_IMAGE = 1;

    /** 上下文对象 */
    private Context context;
    /** 混合数据列表（Video 或 ImageCard） */
    private List<Object> feedItems;
    /** 页面变更监听器 - 用于回调页面切换和加载更多事件 */
    private OnVideoPageChangeListener pageChangeListener;
    /** 播放器视图映射 - 缓存已创建的 VideoPlayerView */
    private Map<Integer, VideoPlayerView> playerViewMap = new HashMap<>();
    /** 当前播放位置 */
    private int currentPlayingPosition = -1;
    /** 是否正在加载更多 - 防止重复触发加载 */
    private boolean isLoadingMore = false;

    /**
     * 页面变更监听器接口
     */
    public interface OnVideoPageChangeListener {
        /**
         * 页面切换回调
         * @param position 新页面位置
         */
        void onPageChanged(int position);

        /**
         * 加载更多回调
         */
        void onLoadMore();

        /**
         * 视频准备完成回调（用于统计起播延迟）
         */
        void onVideoPrepared();

        /**
         * 页面即将切换回调（用于提前预加载）
         * @param position 即将切换到的页面位置
         */
        void onPageWillChange(int position);

        /**
         * 图片卡片被单击回调
         * @param card 图片卡片数据
         */
        void onImageCardClicked(ImageCard card);
    }

    /**
     * 构造函数
     * @param context 上下文对象
     */
    public VideoFeedAdapter(Context context) {
        this.context = context;
        this.feedItems = new ArrayList<>();
    }

    /**
     * 设置视频列表（替换原有数据，类似刷新）
     * @param videos 新的视频列表
     */
    public void setVideoList(List<Video> videos) {
        this.feedItems = new ArrayList<>(videos);
        notifyDataSetChanged(); // 通知数据变更
    }

    /**
     * 设置混合数据列表（视频 + 图片卡片）
     * @param items 混合数据列表（Video 或 ImageCard）
     */
    public void setFeedItems(List<Object> items) {
        this.feedItems = items;
        notifyDataSetChanged();
    }

    /**
     * 添加视频列表（追加到现有数据）
     * @param videos 要添加的视频列表
     */
    public void addVideos(List<Video> videos) {
        int startPosition = feedItems.size();
        feedItems.addAll(videos);
        notifyItemRangeInserted(startPosition, videos.size());
        isLoadingMore = false; // 重置加载状态
    }

    /**
     * 添加混合数据列表（追加）
     * @param items 混合数据列表
     */
    public void addFeedItems(List<Object> items) {
        int startPosition = feedItems.size();
        feedItems.addAll(items);
        notifyItemRangeInserted(startPosition, items.size());
        isLoadingMore = false;
    }

    /**
     * 设置页面变更监听器
     * @param listener 监听器实例
     */
    public void setPageChangeListener(OnVideoPageChangeListener listener) {
        this.pageChangeListener = listener;
    }

    // ==================== RecyclerView Adapter 核心方法 ====================

    @Override
    public int getItemViewType(int position) {
        Object item = feedItems.get(position);
        if (item instanceof ImageCard) {
            return TYPE_IMAGE;
        }
        return TYPE_VIDEO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == TYPE_IMAGE) {
            ImageCardView cardView = new ImageCardView(context);
            cardView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            return new ImageCardViewHolder(cardView);
        } else {
            VideoPlayerView playerView = new VideoPlayerView(context);
            playerView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
            return new VideoViewHolder(playerView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = feedItems.get(position);

        if (holder instanceof ImageCardViewHolder && item instanceof ImageCard) {
            ImageCard card = (ImageCard) item;
            ((ImageCardViewHolder) holder).bindCard(card, position);
        } else if (holder instanceof VideoViewHolder && item instanceof Video) {
            Video video = (Video) item;
            VideoViewHolder vh = (VideoViewHolder) holder;
            vh.bindVideo(video, position);
            // 缓存播放器视图
            playerViewMap.put(position, vh.playerView);
        }

        // 检查是否需要加载更多
        if (position == feedItems.size() - 1 && pageChangeListener != null && !isLoadingMore) {
            isLoadingMore = true;
            View view = holder.itemView;
            if (view != null) {
                view.post(() -> pageChangeListener.onLoadMore());
            }
        }
    }

    @Override
    public int getItemCount() {
        return feedItems.size();
    }

    // ==================== 视图生命周期回调 ====================

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        int position = holder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION || position == currentPlayingPosition) {
            return;
        }

        // 图片卡片不需要播放逻辑
        if (holder instanceof ImageCardViewHolder) {
            return;
        }

        // 视频项：提前通知即将切换（用于预加载）
        if (pageChangeListener != null) {
            pageChangeListener.onPageWillChange(position);
        }

        currentPlayingPosition = position;
        holder.itemView.post(() -> {
            if (pageChangeListener != null) {
                pageChangeListener.onPageChanged(position);
            }
        });
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        int position = holder.getAdapterPosition();

        if (holder instanceof ImageCardViewHolder) {
            // 图片卡片：简单暂停资源
            ((ImageCardViewHolder) holder).pause();
            return;
        }

        if (position != RecyclerView.NO_POSITION && position == currentPlayingPosition) {
            ((VideoViewHolder) holder).playerView.pause();
        }
    }

    // ==================== 数据查询方法 ====================

    /**
     * 获取指定位置的播放器视图（仅对视频项有效）
     * @param position 视频位置
     * @return VideoPlayerView 实例（不存在返回null）
     */
    public VideoPlayerView getPlayerViewAtPosition(int position) {
        return playerViewMap.get(position);
    }

    /**
     * 暂停所有视频播放
     */
    public void pauseAll() {
        for (VideoPlayerView playerView : playerViewMap.values()) {
            if (playerView != null) {
                playerView.pause();
            }
        }
    }

    /**
     * 释放所有播放器资源
     */
    public void releaseAll() {
        for (VideoPlayerView playerView : playerViewMap.values()) {
            if (playerView != null) {
                playerView.release();
            }
        }
        playerViewMap.clear(); // 清空缓存
    }

    /**
     * 获取指定位置的视频数据（仅对视频项有效）
     * @param position 视频位置
     * @return Video 实例（越界或非视频返回null）
     */
    public Video getVideoAtPosition(int position) {
        if (position >= 0 && position < feedItems.size()) {
            Object item = feedItems.get(position);
            if (item instanceof Video) {
                return (Video) item;
            }
        }
        return null;
    }

    /**
     * 判断指定位置是否是视频
     */
    public boolean isVideoAtPosition(int position) {
        if (position >= 0 && position < feedItems.size()) {
            return feedItems.get(position) instanceof Video;
        }
        return false;
    }

    /**
     * 判断指定位置是否是图片卡片
     */
    public boolean isImageCardAtPosition(int position) {
        if (position >= 0 && position < feedItems.size()) {
            return feedItems.get(position) instanceof ImageCard;
        }
        return false;
    }

    /**
     * 获取混合数据列表
     */
    public List<Object> getFeedItems() {
        return feedItems;
    }

    // ==================== ViewHolder 内部类 ====================

    /**
     * 视频项 ViewHolder
     */
    class VideoViewHolder extends RecyclerView.ViewHolder {
        VideoPlayerView playerView;

        public VideoViewHolder(@NonNull VideoPlayerView itemView) {
            super(itemView);
            this.playerView = itemView;
        }

        public void bindVideo(Video video, int position) {
            playerView.setVideo(video);

            playerView.setOnVideoPreparedListener(() -> {
                if (pageChangeListener != null) {
                    pageChangeListener.onVideoPrepared();
                }
            });
        }
    }

    /**
     * 图片卡片 ViewHolder
     */
    class ImageCardViewHolder extends RecyclerView.ViewHolder {
        ImageCardView cardView;

        public ImageCardViewHolder(@NonNull ImageCardView itemView) {
            super(itemView);
            this.cardView = itemView;
        }

        public void bindCard(ImageCard card, int position) {
            cardView.setImageCard(card);

            cardView.setOnImageCardClickListener(c -> {
                if (pageChangeListener != null) {
                    pageChangeListener.onImageCardClicked(c);
                }
            });
        }

        public void pause() {
            // 图片卡片不需要暂停逻辑（没有播放器）
            // 预留：可在此处暂停 GIF 等动画
        }
    }
}
