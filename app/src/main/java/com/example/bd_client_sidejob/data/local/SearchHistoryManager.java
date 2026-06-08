package com.example.bd_client_sidejob.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.bd_client_sidejob.data.model.SearchHistory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索历史管理器
 * 使用 SharedPreferences 存储搜索历史，支持添加、删除、清空操作
 */
public class SearchHistoryManager {

    private static final String PREF_NAME = "search_history";
    private static final String KEY_HISTORY = "history_list";
    private static final int MAX_HISTORY_SIZE = 20;

    private final SharedPreferences sp;
    private final Gson gson;
    private static SearchHistoryManager instance;

    private SearchHistoryManager(Context context) {
        sp = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static synchronized SearchHistoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new SearchHistoryManager(context);
        }
        return instance;
    }

    /**
     * 添加搜索关键词到历史记录
     *
     * @param keyword 搜索关键词
     */
    public void addHistory(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }

        final String trimmedKeyword = keyword.trim();

        // 获取现有历史
        List<SearchHistory> historyList = getHistoryList();

        // 移除已存在的相同关键词（避免重复）；Collection.removeIf() 会遍历集合，删除满足条件的元素
        historyList.removeIf(h -> h.getKeyword().equals(trimmedKeyword));

        // 添加到列表开头
        historyList.add(0, new SearchHistory(trimmedKeyword));

        // 限制最大数量
        while (historyList.size() > MAX_HISTORY_SIZE) {
            historyList.remove(historyList.size() - 1);
        }

        // 保存
        saveHistoryList(historyList);
    }

    /**
     * 删除单条历史记录
     *
     * @param keyword 搜索关键词
     */
    public void removeHistory(String keyword) {
        List<SearchHistory> historyList = getHistoryList();
        historyList.removeIf(h -> h.getKeyword().equals(keyword));
        saveHistoryList(historyList);
    }

    /**
     * 清空所有历史记录
     */
    public void clearHistory() {
        sp.edit().remove(KEY_HISTORY).apply();
    }

    /**
     * 获取历史记录列表
     *
     * @return 历史记录列表（按时间倒序）
     */
    public List<SearchHistory> getHistoryList() {
        String json = sp.getString(KEY_HISTORY, "[]"); // 从SharedPreferences 获取 JSON 字符串，默认空数组
        // TypeToken 是 Gson 库提供的工具类，用于解决 Java 泛型擦除问题
        Type type = new TypeToken<ArrayList<SearchHistory>>() {}.getType(); // 获取 List<SearchHistory> 的类型信息
        List<SearchHistory> list = gson.fromJson(json, type); // 将 JSON 字符串解析为对象列表
        return list == null ? new ArrayList<>() : list; // 如果解析结果为 null，返回空列表
    }

    /**
     * 保存历史记录列表
     */
    private void saveHistoryList(List<SearchHistory> list) {
        String json = gson.toJson(list);
        sp.edit().putString(KEY_HISTORY, json).apply();
    }
}