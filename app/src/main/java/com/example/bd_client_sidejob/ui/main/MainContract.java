package com.example.bd_client_sidejob.ui.main;

import com.example.bd_client_sidejob.base.BasePresenter;
import com.example.bd_client_sidejob.base.BaseView;
import com.example.bd_client_sidejob.data.model.Video;
import com.example.bd_client_sidejob.data.model.VideoList;

import java.util.List;

public interface MainContract {

    interface View extends BaseView<Presenter> {
        void showVideoList(List<Video> videos);
        void showLoading();
        void hideLoading();
        void showError(String message);
        void onVideoLoaded(Video video, int position);
        void onMoreVideosLoaded(List<Video> videos);
        void hasMoreVideos(boolean hasMore);
    }

    interface Presenter {
        void loadVideos(int page, int pageSize);
        void loadMoreVideos();
        void playVideo(int position);
        void pauseVideo(int position);
        void releaseVideo(int position);
        void onVideoPageChanged(int position);
    }
}