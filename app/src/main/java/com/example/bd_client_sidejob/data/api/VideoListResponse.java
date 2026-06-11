package com.example.bd_client_sidejob.data.api;

import com.example.bd_client_sidejob.data.model.Video;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 视频列表分页响应（对应 GET /api/v1/videos）
 */
public class VideoListResponse {
    private List<Video> videos;
    private int currentPage;
    private int pageSize;
    private int totalCount;
    private boolean hasMore;

    public VideoListResponse() {}

    public List<Video> getVideos() {
        return videos;
    }

    public void setVideos(List<Video> videos) {
        this.videos = videos;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}
