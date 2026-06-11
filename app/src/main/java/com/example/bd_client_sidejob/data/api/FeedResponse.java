package com.example.bd_client_sidejob.data.api;

import com.example.bd_client_sidejob.data.model.ImageCard;
import com.example.bd_client_sidejob.data.model.Video;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Feed 混合流响应（对应 GET /api/v1/feed）
 * 包含自定义 Gson 反序列化器处理动态 data 类型
 */
public class FeedResponse {
    private List<FeedItem> items;
    private int currentPage;
    private boolean hasMore;

    public FeedResponse() {}

    public List<FeedItem> getItems() {
        return items;
    }

    public void setItems(List<FeedItem> items) {
        this.items = items;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    // 相应体 中
    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    /**
     * 将 FeedItem 列表转换为 Object 列表（Video 或 ImageCard）
     * 供 Adapter 的 getItemViewType() 使用
     */
    public List<Object> toFlatItems() {
        List<Object> result = new ArrayList<>();
        if (items != null) {
            for (FeedItem item : items) {
                result.add(item.getTypedData());
            }
        }
        return result;
    }

    /**
     * FeedItem: 包装类，其中 dataObject 根据 type 字段是 Video 或 ImageCard
     */
    public static class FeedItem {
        private String type;
        private Object dataObject; // deserialized by custom deserializer

        public FeedItem() {}

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setDataObject(Object dataObject) {
            this.dataObject = dataObject;
        }

        public Object getTypedData() {
            return dataObject;
        }

        public boolean isVideo() {
            return "video".equals(type);
        }

        public boolean isImageCard() {
            return "image_card".equals(type);
        }

        public Video getVideoData() {
            return dataObject instanceof Video ? (Video) dataObject : null;
        }

        public ImageCard getImageCardData() {
            return dataObject instanceof ImageCard ? (ImageCard) dataObject : null;
        }
    }

    /**
     * 自定义 Gson 反序列化器 — 根据 type 字段决定 data 字段的类型
     * JsonDeserializer<T> 是 Gson 库提供的接口 ，用于自定义 JSON 反序列化逻辑
     */
    public static class FeedResponseDeserializer implements JsonDeserializer<FeedResponse> {
        private final Gson gson;

        public FeedResponseDeserializer() {
            this.gson = new Gson();
        }

        @Override
        public FeedResponse deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            JsonObject root = json.getAsJsonObject();

            FeedResponse response = new FeedResponse();
            response.currentPage = root.has("currentPage") ? root.get("currentPage").getAsInt() : 0;
            response.hasMore = root.has("hasMore") && root.get("hasMore").getAsBoolean();

            if (root.has("items")) {
                JsonArray itemsArr = root.getAsJsonArray("items");
                response.items = new ArrayList<>();
                for (JsonElement itemElem : itemsArr) {
                    JsonObject itemObj = itemElem.getAsJsonObject();
                    String type = itemObj.has("type") ? itemObj.get("type").getAsString() : "";
                    JsonElement dataElem = itemObj.get("data");

                    FeedItem feedItem = new FeedItem();
                    feedItem.type = type;

                    if (dataElem != null && !dataElem.isJsonNull()) {
                        if ("video".equals(type)) {
                            feedItem.dataObject = gson.fromJson(dataElem, Video.class);
                        } else if ("image_card".equals(type)) {
                            feedItem.dataObject = gson.fromJson(dataElem, ImageCard.class);
                        }
                    }

                    response.items.add(feedItem);
                }
            }

            return response;
        }
    }
}
