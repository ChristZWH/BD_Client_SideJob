package com.example.bd_client_sidejob.data.model;

// 图片卡片实体
public class ImageCard {
    private String cardId;
    private String imageUrl;
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