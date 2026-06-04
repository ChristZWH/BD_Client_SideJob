package com.example.bd_client_sidejob.ui.main;

import com.example.bd_client_sidejob.base.BasePresenter;
import com.example.bd_client_sidejob.data.model.Video;
import com.example.bd_client_sidejob.data.model.VideoList;
import com.example.bd_client_sidejob.data.repository.VideoRepository;
import com.example.bd_client_sidejob.data.repository.VideoRepositoryImpl;

import java.util.ArrayList;
import java.util.List;

public class MainPresenter extends BasePresenter<MainContract.View> implements MainContract.Presenter {

    private final VideoRepository videoRepository;
    private List<Video> videoList;
    private int currentPage = 0;
    private final int pageSize = 5;
    private boolean hasMore = true;
    private boolean isLoading = false;
    private int currentPlayingPosition = -1;

    public MainPresenter() {
        this.videoRepository = VideoRepositoryImpl.getInstance();
        this.videoList = new ArrayList<>();
    }

    @Override
    public void loadVideos(int page, int pageSize) {
        if (isLoading) {
            return;
        }

        isLoading = true;
        if (page == 0) {
            if (isViewAttached()) {
                getView().showLoading();
            }
        }

        videoRepository.getVideoList(page, pageSize, new VideoRepository.VideoListCallback() {
            @Override
            public void onSuccess(VideoList result) {
                isLoading = false;
                currentPage = result.getCurrentPage();
                hasMore = result.isHasMore();

                if (page == 0) {
                    videoList.clear();
                }
                videoList.addAll(result.getVideos());

                if (isViewAttached()) {
                    getView().hideLoading();

                    if (page == 0) {
                        getView().showVideoList(result.getVideos());
                    } else {
                        getView().onMoreVideosLoaded(result.getVideos());
                    }

                    getView().hasMoreVideos(hasMore);
                }
            }

            @Override
            public void onError(String message) {
                isLoading = false;
                if (isViewAttached()) {
                    getView().hideLoading();
                    getView().showError(message);
                }
            }
        });
    }

    @Override
    public void loadMoreVideos() {
        if (!hasMore || isLoading) {
            return;
        }
        loadVideos(currentPage + 1, pageSize);
    }

    @Override
    public void playVideo(int position) {
        if (position >= 0 && position < videoList.size()) {
            currentPlayingPosition = position;
            Video video = videoList.get(position);
            if (isViewAttached()) {
                getView().onVideoLoaded(video, position);
            }
        }
    }

    @Override
    public void pauseVideo(int position) {
        // 暂停指定位置的视频
        if (isViewAttached()) {
            // View层处理暂停逻辑
        }
    }

    @Override
    public void releaseVideo(int position) {
        // 释放指定位置的视频资源
        if (isViewAttached()) {
            // View层处理释放逻辑
        }
    }

    @Override
    public void onVideoPageChanged(int position) {
        // 页面切换时，播放当前视频，暂停其他视频
        if (position >= 0 && position < videoList.size()) {
            playVideo(position);
        }
    }

    public List<Video> getVideoList() {
        return videoList;
    }

    public int getCurrentPlayingPosition() {
        return currentPlayingPosition;
    }

    public Video getVideoAtPosition(int position) {
        if (position >= 0 && position < videoList.size()) {
            return videoList.get(position);
        }
        return null;
    }
}