package com.example.bd_client_sidejob.data.repository;

import java.util.List;

// 搜索仓库接口
public interface SearchRepository {
    void saveSearchKeyword(String keyword);
    List<String> getSearchHistory();
    void clearSearchHistory();
    void removeSearchKeyword(String keyword);
}