package com.example.bd_client_sidejob.ui.main;

import com.example.bd_client_sidejob.base.BasePresenter;
import com.example.bd_client_sidejob.base.BaseView;
import com.example.bd_client_sidejob.data.model.Video;

import java.util.List;

// 契约接口，定义View和Presenter之间的交互规范
public interface MainContract {

    interface View extends BaseView<Presenter> {
        void showVideoList(List<Video> videos);

        /** 显示混合数据列表（视频 + 图片卡片混排） */
        void showFeedItems(List<Object> feedItems);

        void showLoading();
        void hideLoading();
        void showError(String message);
        void onVideoLoaded(Video video, int position);

        /** 加载更多视频 + 图片卡片完成 */
        void onMoreFeedItemsLoaded(List<Object> feedItems);

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
