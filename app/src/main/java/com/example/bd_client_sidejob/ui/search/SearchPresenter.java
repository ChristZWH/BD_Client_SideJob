package com.example.bd_client_sidejob.ui.search;

import com.example.bd_client_sidejob.base.BasePresenter;
import com.example.bd_client_sidejob.data.model.SearchHistory;
import com.example.bd_client_sidejob.data.repository.SearchRepository;
import com.example.bd_client_sidejob.data.repository.SearchRepositoryImpl;

import java.util.Arrays;
import java.util.List;

/**
 * 搜索页面 Presenter 层
 * 负责处理搜索相关的业务逻辑，作为 View 和 Repository 之间的桥梁
 */
public class SearchPresenter extends BasePresenter<SearchContract.View> implements SearchContract.Presenter {

    /** 搜索仓库接口，用于数据访问 */
    private SearchRepository searchRepository;

    /**
     * 获取或初始化搜索仓库
     */
    private SearchRepository getRepository() {
        if (searchRepository == null && isViewAttached()) {
            searchRepository = SearchRepositoryImpl.getInstance(getView().getContext());
        }
        return searchRepository;
    }

    /**
     * 加载搜索历史和推荐关键词
     * 从 Repository 获取数据后，通过 View 层展示
     */
    @Override
    public void loadSearchHistoryAndRecommend() {
        if (!isViewAttached()) {
            return;
        }

        // 显示加载状态
        getView().showLoading();

        // 从仓库获取搜索历史列表
        List<SearchHistory> historyList = getRepository().getSearchHistory();
        
        // 从仓库获取推荐关键词
        String[] keywordsArray = getRepository().getRecommendKeywords();
        List<String> keywords = Arrays.asList(keywordsArray);

        // 更新 UI
        getView().hideLoading();
        getView().showSearchHistory(historyList);
    }

    /**
     * 执行搜索操作
     * @param keyword 搜索关键词
     */
    @Override
    public void performSearch(String keyword) {
        if (!isViewAttached() || keyword == null || keyword.trim().isEmpty()) {
            return;
        }

        // 将关键词添加到搜索历史
        getRepository().addSearchKeyword(keyword);

        // 通知 View 跳转到搜索结果页面
        getView().navigateToSearchResult(keyword);
    }

    /**
     * 添加搜索关键词到历史记录
     * @param keyword 关键词
     */
    @Override
    public void addSearchKeyword(String keyword) {
        getRepository().addSearchKeyword(keyword);
        
        // 显示提示信息
        if (isViewAttached()) {
            getView().showToast("搜索历史已保存");
        }
    }

    /**
     * 删除单条搜索历史
     * @param keyword 要删除的关键词
     */
    @Override
    public void removeSearchHistory(String keyword) {
        // 删除指定关键词
        getRepository().removeSearchKeyword(keyword);
        
        // 重新加载历史列表
        loadSearchHistoryAndRecommend();
    }

    /**
     * 清空所有搜索历史
     */
    @Override
    public void clearAllSearchHistory() {
        // 清空历史记录
        getRepository().clearSearchHistory();
        
        // 显示提示并重新加载
        if (isViewAttached()) {
            getView().showToast("搜索历史已清空");
            loadSearchHistoryAndRecommend();
        }
    }
}