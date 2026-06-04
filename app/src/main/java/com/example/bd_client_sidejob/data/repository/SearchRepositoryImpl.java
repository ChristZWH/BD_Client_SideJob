package com.example.bd_client_sidejob.data.repository;

import android.content.Context;

import com.example.bd_client_sidejob.data.local.SearchHistoryManager;

import java.util.List;

// 搜索仓库实现类
public class SearchRepositoryImpl implements SearchRepository {

    private final SearchHistoryManager historyManager;
    private static SearchRepositoryImpl instance;

    private SearchRepositoryImpl(Context context) {
        this.historyManager = SearchHistoryManager.getInstance(context);
    }

    public static synchronized SearchRepositoryImpl getInstance(Context context) {
        if (instance == null) {
            instance = new SearchRepositoryImpl(context);
        }
        return instance;
    }

    @Override
    public void saveSearchKeyword(String keyword) {
        historyManager.addSearchKeyword(keyword);
    }

    @Override
    public List<String> getSearchHistory() {
        return historyManager.getSearchHistory();
    }

    @Override
    public void clearSearchHistory() {
        historyManager.clearSearchHistory();
    }

    @Override
    public void removeSearchKeyword(String keyword) {
        historyManager.removeSearchKeyword(keyword);
    }
}