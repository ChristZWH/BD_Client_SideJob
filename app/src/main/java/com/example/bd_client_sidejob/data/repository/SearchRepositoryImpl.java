package com.example.bd_client_sidejob.data.repository;

import android.content.Context;

import com.example.bd_client_sidejob.data.local.MockVideoData;
import com.example.bd_client_sidejob.data.local.SearchHistoryManager;
import com.example.bd_client_sidejob.data.model.SearchHistory;

import java.util.List;

// 搜索仓库实现类
public class SearchRepositoryImpl implements SearchRepository {

    private final SearchHistoryManager historyManager;
    private static SearchRepositoryImpl instance;
    private static Context appContext;  // 保存全局 Context

    private SearchRepositoryImpl(Context context) {
        this.historyManager = SearchHistoryManager.getInstance(context);
    }

    /**
     * 带 Context 的初始化方法（首次调用）
     */
    public static synchronized SearchRepositoryImpl getInstance(Context context) {
        if (instance == null) {
            appContext = context.getApplicationContext();  // 保存全局 Context
            instance = new SearchRepositoryImpl(appContext);
        }
        return instance;
    }

    /**
     * 无参获取实例（后续调用）
     */
    public static synchronized SearchRepositoryImpl getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SearchRepositoryImpl 尚未初始化，请先调用 getInstance(Context)");
        }
        return instance;
    }

    @Override
    public void addSearchKeyword(String keyword) {
        historyManager.addHistory(keyword);
    }

    @Override
    public List<SearchHistory> getSearchHistory() {
        return historyManager.getHistoryList();
    }

    @Override
    public void clearSearchHistory() {
        historyManager.clearHistory();
    }

    @Override
    public void removeSearchKeyword(String keyword) {
        historyManager.removeHistory(keyword);
    }

    @Override
    public String[] getRecommendKeywords() {
        return MockVideoData.getRecommendKeywords();
    }
}
