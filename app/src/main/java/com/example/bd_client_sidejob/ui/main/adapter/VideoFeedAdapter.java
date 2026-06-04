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

public class VideoFeedAdapter extends RecyclerView.Adapter<VideoFeedAdapter.VideoViewHolder> {

    private Context context;
    private List<Video> videoList;
    private OnVideoPageChangeListener pageChangeListener;
    private Map<Integer, VideoPlayerView> playerViewMap = new HashMap<>();
    private int currentPlayingPosition = -1;
    private boolean isLoadingMore = false;

    public interface OnVideoPageChangeListener {
        void onPageChanged(int position);
        void onLoadMore();
    }

    public VideoFeedAdapter(Context context) {
        this.context = context;
        this.videoList = new ArrayList<>();
    }

    public void setVideoList(List<Video> videos) {
        this.videoList = videos;
        notifyDataSetChanged();
    }

    public void addVideos(List<Video> videos) {
        int startPosition = videoList.size();
        videoList.addAll(videos);
        notifyItemRangeInserted(startPosition, videos.size());
        isLoadingMore = false; // 重置加载状态
    }

    public void setPageChangeListener(OnVideoPageChangeListener listener) {
        this.pageChangeListener = listener;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        VideoPlayerView playerView = new VideoPlayerView(context);
        playerView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        return new VideoViewHolder(playerView);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Video video = videoList.get(position);
        holder.bindVideo(video, position);

        playerViewMap.put(position, holder.playerView);

        // 检查是否需要加载更多 - 使用 post 避免在布局计算中调用
        if (position == videoList.size() - 1 && pageChangeListener != null && !isLoadingMore) {
            isLoadingMore = true;
            holder.playerView.post(new Runnable() {
                @Override
                public void run() {
                    pageChangeListener.onLoadMore();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    @Override
    public void onViewAttachedToWindow(@NonNull VideoViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION && position != currentPlayingPosition) {
            currentPlayingPosition = position;
            // 使用 post 避免在布局计算中调用回调
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

    @Override
    public void onViewDetachedFromWindow(@NonNull VideoViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION && position == currentPlayingPosition) {
            holder.playerView.pause();
            holder.playerView.release();
        }
    }

    public VideoPlayerView getPlayerViewAtPosition(int position) {
        return playerViewMap.get(position);
    }

    public void pauseAll() {
        for (VideoPlayerView playerView : playerViewMap.values()) {
            if (playerView != null) {
                playerView.pause();
            }
        }
    }

    public void releaseAll() {
        for (VideoPlayerView playerView : playerViewMap.values()) {
            if (playerView != null) {
                playerView.release();
            }
        }
        playerViewMap.clear();
    }

    public Video getVideoAtPosition(int position) {
        if (position >= 0 && position < videoList.size()) {
            return videoList.get(position);
        }
        return null;
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        VideoPlayerView playerView;

        public VideoViewHolder(@NonNull VideoPlayerView itemView) {
            super(itemView);
            this.playerView = itemView;
        }

        public void bindVideo(Video video, int position) {
            playerView.setVideo(video);
        }
    }
}