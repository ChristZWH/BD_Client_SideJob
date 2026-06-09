package com.example.bd_client_sidejob.data.model;

import java.util.ArrayList;
import java.util.List;

// 图片卡片实体（支持多图左右滑动）
public class ImageCard {
    private String cardId;
    /** @deprecated 用 imageUrls 替代，保留兼容旧代码 */
    private String imageUrl;
    /** 多图列表（每张图片可左右滑动查看） */
    private List<String> imageUrls;
    private String title;
    private int position;

    public ImageCard() {}

    public ImageCard(String cardId, String imageUrl, String title, int position) {
        this.cardId = cardId;
        this.imageUrl = imageUrl;
        this.title = title;
        this.position = position;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    /**
     * 获取多图列表（如果未设置则用 imageUrl 构造单图列表）
     */
    public List<String> getImageUrls() {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls;
        }
        // 兼容旧数据：单图 → 单元素列表
        List<String> fallback = new ArrayList<>();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            fallback.add(imageUrl);
        }
        return fallback;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    /**
     * 获取图片数量
     */
    public int getImageCount() {
        List<String> urls = getImageUrls();
        return urls.size();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}