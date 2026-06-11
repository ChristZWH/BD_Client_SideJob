package com.example.bd_client_sidejob.data.api;

import com.example.bd_client_sidejob.data.model.ImageCard;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 图片卡片列表响应（对应 GET /api/v1/imagecards）
 */
public class ImageCardResponse {
    private List<ImageCard> cards;

    public ImageCardResponse() {}

    public List<ImageCard> getCards() {
        return cards;
    }

    public void setCards(List<ImageCard> cards) {
        this.cards = cards;
    }
}
