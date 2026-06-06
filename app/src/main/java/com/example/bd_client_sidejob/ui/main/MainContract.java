package com.example.bd_client_sidejob.ui.main;

import com.example.bd_client_sidejob.base.BasePresenter;
import com.example.bd_client_sidejob.base.BaseView;
import com.example.bd_client_sidejob.data.model.Video;
import com.example.bd_client_sidejob.data.model.VideoList;

import java.util.List;

// 契约接口，定义View和Presenter之间的交互规范
public interface MainContract {

    interface View extends BaseView<Presenter> {
        void showVideoList(List<Video> videos);
        void showLoading();
        void hideLoading();
        void showError(String message);
        void onVideoLoaded(Video video, int position);
        void onMoreVideosLoaded(List<Video> videos);
        void hasMoreVideos(boolean hasMore);
        void onVideoPaused(int position);
        void onVideoReleased(int position);
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