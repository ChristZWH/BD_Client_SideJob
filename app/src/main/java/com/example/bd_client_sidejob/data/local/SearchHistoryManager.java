package com.example.bd_client_sidejob.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 搜索历史管理类
public class SearchHistoryManager {

    private static final String PREF_NAME = "search_history";
    private static final String KEY_HISTORY = "history_list";
    private static final int MAX_HISTORY_SIZE = 10;

    private final SharedPreferences sp;
    private static SearchHistoryManager instance;

    private SearchHistoryManager(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SearchHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new SearchHistoryManager(context);
        }
        return instance;
    }

    public void addSearchKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        keyword = keyword.trim();

        Set<String> historySet = new HashSet<>(sp.getStringSet(KEY_HISTORY, new HashSet<>()));
        List<String> historyList = new ArrayList<>(historySet);

        historyList.remove(keyword);  // 1. 去重：先移除已存在的关键词

        historyList.add(0, keyword);  // 2. 新增关键词到列表头部（最新的关键词在最上面）

        while (historyList.size() > MAX_HISTORY_SIZE) { // 保留最多 MAX_HISTORY_SIZE 条搜索记录
            historyList.remove(historyList.size() - 1);
        }

        sp.edit().putStringSet(KEY_HISTORY, new HashSet<>(historyList)).apply();
    }

    public List<String> getSearchHistory() {
        Set<String> historySet = sp.getStringSet(KEY_HISTORY, new HashSet<>());
        List<String> historyList = new ArrayList<>(historySet);
        return historyList;
    }

    public void clearSearchHistory() {
        sp.edit().remove(KEY_HISTORY).apply();
    }

    public void removeSearchKeyword(String keyword) {
        Set<String> historySet = new HashSet<>(sp.getStringSet(KEY_HISTORY, new HashSet<>()));
        historySet.remove(keyword);
        sp.edit().putStringSet(KEY_HISTORY, historySet).apply();
    }
}