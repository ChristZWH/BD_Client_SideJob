package com.example.bd_client_sidejob.data.model;

/**
 * 搜索历史数据模型
 */
public class SearchHistory {
    private String keyword;
    private long timestamp;

    public SearchHistory() {}
    
    public SearchHistory(String keyword) {
        this.keyword = keyword;
        this.timestamp = System.currentTimeMillis();
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
