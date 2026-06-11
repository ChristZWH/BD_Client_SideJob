package com.example.bd_client_sidejob.data.repository;

import com.example.bd_client_sidejob.data.model.ImageCard;
import com.example.bd_client_sidejob.data.model.Video;
import com.example.bd_client_sidejob.data.model.VideoList;

import java.util.List;

// 视频仓库接口
public interface VideoRepository {
    void getVideoList(int page, int pageSize, VideoListCallback callback);
    void searchVideos(String keyword, SearchVideoCallback callback);
    void getRecommendKeywords(RecommendKeywordsCallback callback);
    void getImageCards(ImageCardsCallback callback);
    void getVideoById(long videoId, VideoByIdCallback callback);

    interface VideoListCallback {
        void onSuccess(VideoList videoList);
        void onError(String message);
    }

    interface SearchVideoCallback {
        void onSuccess(List<Video> videos);
        void onError(String message);
    }

    interface RecommendKeywordsCallback {
        void onSuccess(String[] keywords);
        void onError(String message);
    }

    interface ImageCardsCallback {
        void onSuccess(List<ImageCard> cards);
        void onError(String message);
    }

    interface VideoByIdCallback {
        void onSuccess(Video video);
        void onError(String message);
    }

    /**
     * Feed 回调接口
     */
     interface FeedCallback {
        void onSuccess(List<Object> items, boolean hasMore);
        void onError(String message);
    }
}