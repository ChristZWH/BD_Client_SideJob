package com.example.bd_client_sidejob.ui.searchresult.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.bd_client_sidejob.R;
import com.example.bd_client_sidejob.data.model.Video;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果视频适配器
 * 视频为主，文字描述在视频上方的布局
 */
public class SearchResultVideoAdapter extends RecyclerView.Adapter<SearchResultVideoAdapter.ViewHolder> {

    private Context context;
    private List<Video> videoList = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Video video);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setVideos(List<Video> videos) {
        this.videoList = videos != null ? videos : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_search_result_video, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Video video = videoList.get(position);
        holder.bind(video);
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivCover;
        private ImageView ivAvatar;
        private TextView tvTitle;
        private TextView tvAuthor;
        private TextView tvInfo;
        private TextView tvDuration;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvInfo = itemView.findViewById(R.id.tvInfo);
            tvDuration = itemView.findViewById(R.id.tvDuration);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onItemClick(videoList.get(getAdapterPosition()));
                }
            });
        }

        void bind(Video video) {
            // 标题（视频上方）
            tvTitle.setText(video.getTitle());
            // 作者
            tvAuthor.setText(video.getAuthor());
            // 底部信息：点赞数、评论数
            String info = formatCount(video.getLikeCount()) + " 赞 · " +
                    formatCount(video.getCommentCount()) + " 评论";
            tvInfo.setText(info);

            // 加载视频封面
            if (video.getCoverUrl() != null && !video.getCoverUrl().isEmpty()) {
                Glide.with(context)
                        .load(video.getCoverUrl())
                        .placeholder(R.drawable.bg_video_cover_placeholder)
                        .error(R.drawable.bg_video_cover_placeholder)
                        .centerCrop()
                        .into(ivCover);
            } else {
                ivCover.setImageResource(R.drawable.bg_video_cover_placeholder);
            }

            // 加载作者头像
            if (video.getAvatar() != null && !video.getAvatar().isEmpty()) {
                Glide.with(context)
                        .load(video.getAvatar())
                        .placeholder(R.drawable.circle_white_border)
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.drawable.circle_white_border);
            }
        }

        private String formatCount(int count) {
            if (count >= 10000) {
                return String.format("%.1fw", count / 10000.0);
            } else if (count >= 1000) {
                return String.format("%.1fk", count / 1000.0);
            }
            return String.valueOf(count);
        }
    }
}