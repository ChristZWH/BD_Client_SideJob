package com.example.bd_client_sidejob.ui.main.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bd_client_sidejob.data.model.Video;
import com.example.bd_client_sidejob.widget.VideoPlayerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VideoFeedAdapter - 视频流列表适配器
 * ViewPager2（底层是 RecyclerView）通过 Adapter 管理数据和创建视图
 * 职责：
 * 1. 持有视频列表数据
 * 2. 创建和复用 VideoPlayerView
 * 3. 将数据绑定到视图
 * 4. 处理加载更多逻辑
 * 5. 管理播放器视图的生命周期
 */
public class VideoFeedAdapter extends RecyclerView.Adapter<VideoFeedAdapter.VideoViewHolder> {

    /** 上下文对象 */
    private Context context;
    /** 视频列表数据 */
    private List<Video> videoList;
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
    }

    /**
     * 构造函数
     * @param context 上下文对象
     */
    public VideoFeedAdapter(Context context) {
        this.context = context;
        this.videoList = new ArrayList<>();
    }

    /**
     * 设置视频列表（替换原有数据，类似刷新）
     * @param videos 新的视频列表
     */
    public void setVideoList(List<Video> videos) {
        this.videoList = videos;
        notifyDataSetChanged(); // 通知数据变更
    }

    /**
     * 添加视频列表（追加到现有数据）
     * @param videos 要添加的视频列表
     */
    public void addVideos(List<Video> videos) {
        int startPosition = videoList.size();
        videoList.addAll(videos);
        // notifyItemRangeInserted是 通知 RecyclerView 数据发生了变化 ，需要更新视图；指定变化范围 ，避免全部重绘；触发视图更新 ，RecyclerView 会自动调用 onBindViewHolder() 更新新插入的视图
        notifyItemRangeInserted(startPosition, videos.size()); // 通知部分数据插入
        isLoadingMore = false; // 重置加载状态
    }

    /**
     * 设置页面变更监听器
     * @param listener 监听器实例
     */
    public void setPageChangeListener(OnVideoPageChangeListener listener) {
        this.pageChangeListener = listener;
    }

    /**
     * 创建VideoPlayerView的 ViewHolder，RecyclerView 自动调用
     * @param parent 父容器
     * @param viewType 视图类型
     * @return VideoViewHolder 实例
     */
    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        VideoPlayerView playerView = new VideoPlayerView(context);  // 创建视频播放器视图
        playerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return new VideoViewHolder(playerView); // 包装成 ViewHolder
        // RecyclerView 的复用机制 ：
        // 1. RecyclerView 不会为每个 item 都创建新视图
        // 2. 而是通过 onCreateViewHolder() 创建有限数量的视图
        // 3. 滑动时 复用 这些视图，只调用 onBindViewHolder() 更新数据
    }

    /**
     * 绑定数据到视图 ViewHolder，RecyclerView 自动调用
     * @param holder ViewHolder 实例
     * @param position 数据位置
     */
    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Video video = videoList.get(position);
        holder.bindVideo(video, position);

        // 缓存播放器视图
        playerViewMap.put(position, holder.playerView);

        // 检查是否需要加载更多（滑动到最后一条时触发）
        // 使用 post 避免在布局计算过程中调用，防止异常
        if (position == videoList.size() - 1 && pageChangeListener != null && !isLoadingMore) {
            isLoadingMore = true;
            // 将onLoadMore() 回调 将 Runnable 添加到 消息队列末尾，延迟到下一个消息循环执行，避免onBindViewHolder() 执行过程中（布局计算阶段）触发回调；防止 RecyclerView 抛出异常；确保布局完全准备好后再触发加载更多
            // RecyclerView 正在执行 onBindViewHolder() ，处于 布局计算阶段 ，此时如果：数据发生变化（ notifyItemRangeInserted() ），那么RecyclerView 尝试重新布局，就会爆出异常
            holder.playerView.post(new Runnable() {
                @Override
                public void run() {
                    pageChangeListener.onLoadMore();
                }
            });
        }
    }

    /**
     * 获取列表项数量
     * @return 视频列表大小
     */
    @Override
    public int getItemCount() {
        return videoList.size();
    }

    /**
     * 视图进入屏幕时的回调
     * onViewAttachedToWindow是RecyclerView 的 生命周期回调 ，系统自动调用
     * @param holder ViewHolder 实例
     */
    @Override
    public void onViewAttachedToWindow(@NonNull VideoViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        int position = holder.getAdapterPosition();
        // 确保位置有效且不是当前播放位置；视图可见的时，通知外部播放视频
        if (position != RecyclerView.NO_POSITION && position != currentPlayingPosition) {
            currentPlayingPosition = position;
            // 使用 post 避免在布局计算过程中调用回调
            holder.playerView.post(new Runnable() {
                @Override
                public void run() {
                    if (pageChangeListener != null) {
                        pageChangeListener.onPageChanged(position);
                    }
                }
            });
        }
    }

    /**
     * 视图从窗口分离时调用
     * onViewDetachedFromWindow是RecyclerView 的 生命周期回调 ，系统自动调用
     * @param holder ViewHolder 实例
     */
    @Override
    public void onViewDetachedFromWindow(@NonNull VideoViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        int position = holder.getAdapterPosition();
        // 如果分离的是当前播放的视频，暂停并释放资源
        if (position != RecyclerView.NO_POSITION && position == currentPlayingPosition) {
            holder.playerView.pause();
            holder.playerView.release();
        }
    }

    /**
     * 获取指定位置的播放器视图
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
     * 获取指定位置的视频数据
     * @param position 视频位置
     * @return Video 实例（越界返回null）
     */
    public Video getVideoAtPosition(int position) {
        if (position >= 0 && position < videoList.size()) {
            return videoList.get(position);
        }
        return null;
    }

    /**
     * VideoViewHolder - 视频列表项的视图持有者
     * 内部类，用于管理视频播放器视图的生命周期和数据绑定操作，只为 VideoFeedAdapter 服务
     */
    static class VideoViewHolder extends RecyclerView.ViewHolder {
        /** 视频播放器视图 VideoPlayerView类 */
        VideoPlayerView playerView;

        /**
         * 构造函数
         * @param itemView 视图实例
         */
        public VideoViewHolder(@NonNull VideoPlayerView itemView) {
            super(itemView);
            this.playerView = itemView;
        }

        /**
         * 绑定视频数据到视图 —— 设置视频数据
         * @param video 视频数据
         * @param position 位置
         */
        public void bindVideo(Video video, int position) {
            playerView.setVideo(video);
        }
    }
}