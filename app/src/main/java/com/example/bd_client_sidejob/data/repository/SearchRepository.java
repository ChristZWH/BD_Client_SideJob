package com.example.bd_client_sidejob.data.repository;

import com.example.bd_client_sidejob.data.model.SearchHistory;

import java.util.List;

// 搜索仓库接口
public interface SearchRepository {
    /**
     * 添加搜索关键词到历史记录
     */
    void addSearchKeyword(String keyword);
    /**
     * 获取搜索历史列表
     */
    List<SearchHistory> getSearchHistory();
    /**
     * 清空所有搜索历史
     */
    void clearSearchHistory();
    /**
     * 删除指定搜索历史
     */
    void removeSearchKeyword(String keyword);
    /**
     * 获取推荐关键词
     */
    String[] getRecommendKeywords();
}