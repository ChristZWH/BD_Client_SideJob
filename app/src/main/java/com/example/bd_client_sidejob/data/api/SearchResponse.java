package com.example.bd_client_sidejob.data.api;

import com.example.bd_client_sidejob.data.model.Video;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 搜索响应（对应 GET /api/v1/search）
 */
public class SearchResponse {
    private String keyword;
    private List<Video> videos;
    private int totalCount;
    private int currentPage;
    private boolean hasMore;

    public SearchResponse() {}

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<Video> getVideos() {
        return videos;
    }

    public void setVideos(List<Video> videos) {
        this.videos = videos;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}
