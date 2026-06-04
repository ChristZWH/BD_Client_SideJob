package com.example.bd_client_sidejob.data.model;

import java.util.List;

// 搜索结果实体
public class SearchResult {
    private String keyword;
    private List<Video> videos;
    private int totalCount;

    public SearchResult() {}

    public SearchResult(String keyword, List<Video> videos, int totalCount) {
        this.keyword = keyword;
        this.videos = videos;
        this.totalCount = totalCount;
    }

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
}