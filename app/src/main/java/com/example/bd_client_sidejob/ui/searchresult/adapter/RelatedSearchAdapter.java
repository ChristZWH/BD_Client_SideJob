package com.example.bd_client_sidejob.ui.searchresult.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bd_client_sidejob.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 相关搜索推荐适配器
 * 标签式展示相关搜索关键词
 */
public class RelatedSearchAdapter extends RecyclerView.Adapter<RelatedSearchAdapter.ViewHolder> {

    private Context context;
    private List<String> keywordList = new ArrayList<>();
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String keyword);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setKeywords(List<String> keywords) {
        this.keywordList = keywords != null ? keywords : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_related_search, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String keyword = keywordList.get(position);
        holder.bind(keyword);
    }

    @Override
    public int getItemCount() {
        return keywordList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvKeyword;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKeyword = itemView.findViewById(R.id.tvKeyword);

            itemView.setOnClickListener(v -> {
                if (listener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    listener.onItemClick(keywordList.get(getAdapterPosition()));
                }
            });
        }

        void bind(String keyword) {
            tvKeyword.setText(keyword);
        }
    }
}