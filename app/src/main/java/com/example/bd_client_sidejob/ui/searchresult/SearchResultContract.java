package com.example.bd_client_sidejob.ui.searchresult;

import com.example.bd_client_sidejob.base.BaseView;
import com.example.bd_client_sidejob.data.model.Video;

import java.util.List;

/** 搜索结果页面 MVP 契约接口 */
public interface SearchResultContract {

    /** View 层接口 */
    interface View extends BaseView<Presenter> {
        /** 获取 Context */
        android.content.Context getContext();

        /** 显示搜索结果视频列表 */
        void showSearchResults(List<Video> videos);

        /** 显示空结果 */
        void showEmptyResult();

        /** 显示相关搜索推荐 */
        void showRelatedSearch(List<String> keywords);

        /** 显示加载状态 */
        void showLoading();

        /** 隐藏加载状态 */
        void hideLoading();

        /** 显示提示信息 */
        void showToast(String message);

        /** 跳转到视频播放页面 */
        void navigateToVideoPlay(Video video);
    }

    /** Presenter 层接口 */
    interface Presenter {
        /** 加载搜索结果 */
        void loadSearchResults(String keyword);

        /** 加载相关搜索推荐 */
        void loadRelatedSearch(String keyword);

        /** 处理视频点击 */
        void onVideoClick(Video video);

        /** 处理相关搜索点击 */
        void onRelatedSearchClick(String keyword);
    }
}
