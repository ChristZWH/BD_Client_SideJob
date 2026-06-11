package com.example.bd_client_sidejob.data.model;

import com.google.gson.annotations.SerializedName;

// 视频实体（对接 Go 视频服务）
public class Video {
    @SerializedName("id") // （JSON中的 id <————> Java类中 videoId），实现名称映射
    private long videoId;
    private String url;
    private String title;
    private String author;
    @SerializedName("avatarColor") // （JSON中的 avatarColor <————> Java类中 avatarColor），实现名称映射
    private String avatar;
    private int likeCount;
    private int commentCount;
    private int collectCount;
    private int shareCount;
    private String coverUrl;
    private String quality360p;
    private String quality720p;
    private String category; // 视频分类
    private String tags; // 视频标签
    private String createdAt;
    private String updatedAt;

    public Video() {}

    public Video(long videoId, String url, String title, String author, String avatar,
                 int likeCount, int commentCount, int collectCount, int shareCount,
                 String coverUrl, String quality360p, String quality720p) {
        this.videoId = videoId;
        this.url = url;
        this.title = title;
        this.author = author;
        this.avatar = avatar;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.collectCount = collectCount;
        this.shareCount = shareCount;
        this.coverUrl = coverUrl;
        this.quality360p = quality360p;
        this.quality720p = quality720p;
    }

    public long getVideoId() {
        return videoId;
    }

    public void setVideoId(long videoId) {
        this.videoId = videoId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public int getCollectCount() {
        return collectCount;
    }

    public void setCollectCount(int collectCount) {
        this.collectCount = collectCount;
    }

    public int getShareCount() {
        return shareCount;
    }

    public void setShareCount(int shareCount) {
        this.shareCount = shareCount;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getQuality360p() {
        return quality360p;
    }

    public void setQuality360p(String quality360p) {
        this.quality360p = quality360p;
    }

    public String getQuality720p() {
        return quality720p;
    }

    public void setQuality720p(String quality720p) {
        this.quality720p = quality720p;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFormattedLikeCount() {
        return formatCount(likeCount);
    }

    public String getFormattedCommentCount() {
        return formatCount(commentCount);
    }

    public String getFormattedCollectCount() {
        return formatCount(collectCount);
    }

    public String getFormattedShareCount() {
        return formatCount(shareCount);
    }

    private String formatCount(int count) {
        if (count >= 100000000) {
            return String.format("%.1f亿", count / 100000000.0);
        } else if (count >= 10000) {
            return String.format("%.1f万", count / 10000.0);
        } else {
            return String.valueOf(count);
        }
    }
}