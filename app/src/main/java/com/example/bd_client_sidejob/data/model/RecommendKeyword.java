package com.example.bd_client_sidejob.data.model;

/**
 * 推荐关键词数据模型
 */
public class RecommendKeyword {
    private String keyword;
    private int rank;

    public RecommendKeyword() {}
    
    public RecommendKeyword(String keyword, int rank) {
        this.keyword = keyword;
        this.rank = rank;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }
}
