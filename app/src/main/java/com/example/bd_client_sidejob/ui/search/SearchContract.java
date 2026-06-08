package com.example.bd_client_sidejob.ui.search;

import com.example.bd_client_sidejob.base.BaseView;
import com.example.bd_client_sidejob.data.model.SearchHistory;

import java.util.List;

/** 搜索页面 MVP 契约接口 */
public interface SearchContract {

    /** View 层接口 */
    interface View extends BaseView<Presenter> {
        /**获取 Context */
        android.content.Context getContext();

        /** 显示搜索历史 */
        void showSearchHistory(List<SearchHistory> historyList);

        /** 跳转到搜索结果页面 */
        void navigateToSearchResult(String keyword);

        /** 显示加载状态 */
        void showLoading();

        /** 隐藏加载状态 */
        void hideLoading();

        /** 显示提示信息 */
        void showToast(String message);
    }

    /** Presenter 层接口 */
    interface Presenter {
        /** 加载搜索历史和推荐关键词 */
        void loadSearchHistoryAndRecommend();

        /** 执行搜索 */
        void performSearch(String keyword);

        /** 添加搜索关键词到历史 */
        void addSearchKeyword(String keyword);

        /** 删除单条搜索历史 */
        void removeSearchHistory(String keyword);

        /** 清空所有搜索历史 */
        void clearAllSearchHistory();
    }
}