package com.example.bd_client_sidejob.data.api;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 推荐词相关响应
 */
public class RecommendResponse {
    // /api/v1/recommend/:videoId 返回结构
    private long videoId;
    private List<KeywordItem> keywords;

    public RecommendResponse() {}

    public long getVideoId() {
        return videoId;
    }

    public void setVideoId(long videoId) {
        this.videoId = videoId;
    }

    public List<KeywordItem> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<KeywordItem> keywords) {
        this.keywords = keywords;
    }

    /**
     * 提取关键词字符串数组
     */
    public String[] toKeywordArray() {
        if (keywords == null || keywords.isEmpty()) {
            return new String[0];
        }
        String[] result = new String[keywords.size()];
        for (int i = 0; i < keywords.size(); i++) {
            result[i] = keywords.get(i).getKeyword();
        }
        return result;
    }

    public static class KeywordItem {
        private String keyword;
        private String source;
        private double score;

        public KeywordItem() {}

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }
    }

    /**
     * 流行推荐词响应（/api/v1/recommend/popular）
     * keywords 是简单字符串数组
     */
    public static class PopularKeywordsResponse {
        private List<String> keywords;

        public PopularKeywordsResponse() {}

        public List<String> getKeywords() {
            return keywords;
        }

        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }

        /** 
         * 将 List<String> 转换为 String[] 数组
        */
        public String[] toKeywordArray() {
            if (keywords == null || keywords.isEmpty()) {
                return new String[0];
            }
            // 传 new String[0] 是为了告诉toArray()方法要返回什么类型的数组
            // new String[0] 是一个空数组，只用于提供类型信息，实际大小由列表长度决定
            return keywords.toArray(new String[0]);
            // List<String> keywords = ["川菜", "火锅", "烧烤"];
            // String[] array = keywords.toArray(new String[0]);
            // 结果：array = ["川菜", "火锅", "烧烤"]
        }
    }
}
