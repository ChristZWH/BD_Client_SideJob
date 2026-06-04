package com.example.bd_client_sidejob.data.model;

import java.util.List;

// 视频列表实体
public class VideoList {
    private List<Video> videos;
    private int currentPage;
    private boolean hasMore;

    public VideoList() {}

    public VideoList(List<Video> videos, int currentPage, boolean hasMore) {
        this.videos = videos;
        this.currentPage = currentPage;
        this.hasMore = hasMore;
    }

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

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}