package com.example.bd_client_sidejob.data.repository;

import com.example.bd_client_sidejob.data.local.MockVideoData;
import com.example.bd_client_sidejob.data.model.ImageCard;
import com.example.bd_client_sidejob.data.model.Video;
import com.example.bd_client_sidejob.data.model.VideoList;

import java.util.List;

// 视频仓库实现类
public class VideoRepositoryImpl implements VideoRepository {

    private static VideoRepositoryImpl instance;

    private VideoRepositoryImpl() {}

    public static synchronized VideoRepositoryImpl getInstance() {
        if (instance == null) {
            instance = new VideoRepositoryImpl();
        }
        return instance;
    }

    @Override
    public void getVideoList(int page, int pageSize, VideoListCallback callback) {
        try {
            List<Video> videos = MockVideoData.getVideosByPage(page, pageSize);
            boolean hasMore = MockVideoData.hasMorePages(page, pageSize);
            VideoList videoList = new VideoList(videos, page, hasMore);
            callback.onSuccess(videoList); // 加载成功则传递视频列表给回调函数
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    @Override
    public void searchVideos(String keyword, SearchVideoCallback callback) {
        try {
            List<Video> videos = MockVideoData.searchVideos(keyword);
            callback.onSuccess(videos);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    @Override
    public void getRecommendKeywords(RecommendKeywordsCallback callback) {
        try {
            String[] keywords = MockVideoData.getRecommendKeywords();
            callback.onSuccess(keywords);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    @Override
    public void getImageCards(ImageCardsCallback callback) {
        try {
            List<ImageCard> cards = MockVideoData.getImageCards();
            callback.onSuccess(cards);
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }

    @Override
    public void getVideoById(String videoId, VideoByIdCallback callback) {
        try {
            List<Video> allVideos = MockVideoData.getMockVideos();
            for (Video video : allVideos) {
                if (video.getVideoId().equals(videoId)) {
                    callback.onSuccess(video);
                    return;
                }
            }
            callback.onError("Video not found");
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }
}