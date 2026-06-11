package com.example.bd_client_sidejob.data.repository;

import android.content.Context;
import android.util.Log;

import com.example.bd_client_sidejob.data.api.ApiResponse;
import com.example.bd_client_sidejob.data.api.ApiService;
import com.example.bd_client_sidejob.data.api.RecommendResponse;
import com.example.bd_client_sidejob.data.api.RetrofitClient;
import com.example.bd_client_sidejob.data.local.MockVideoData;
import com.example.bd_client_sidejob.data.local.SearchHistoryManager;
import com.example.bd_client_sidejob.data.model.SearchHistory;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// 搜索仓库实现类
public class SearchRepositoryImpl implements SearchRepository {
    private static final String TAG = "SearchRepositoryImpl";

    private final SearchHistoryManager historyManager;
    private final ApiService apiService;
    private static SearchRepositoryImpl instance;
    private static Context appContext;  // 保存全局 Context

    /** 由首次初始化方法调用，后续运行中不在调用*/
    private SearchRepositoryImpl(Context context) {
        this.historyManager = SearchHistoryManager.getInstance(context);
        this.apiService = RetrofitClient.getInstance().getApiService();
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

    // 添加搜索关键词
    @Override
    public void addSearchKeyword(String keyword) {
        historyManager.addHistory(keyword);
    }

    // 获取搜索历史列表
    @Override
    public List<SearchHistory> getSearchHistory() {
        return historyManager.getHistoryList();
    }

    // 清除所有历史
    @Override
    public void clearSearchHistory() {
        historyManager.clearHistory();
    }

    // 清除单个历史
    @Override
    public void removeSearchKeyword(String keyword) {
        historyManager.removeHistory(keyword);
    }

    // 关键词获取（下面三个）
    // 同步获取 ———— 直接返回Mock数据
    @Override
    public String[] getRecommendKeywords() {
        // 同步方法无法等待网络，直接返回 Mock 数据
        // 推荐词异步加载请使用 loadRecommendKeywords()
        return MockVideoData.getRecommendKeywords();
    }

    /**
     * 异步加载推荐关键词（网络优先 + Mock 降级）
     */
    public void loadRecommendKeywords(RecommendKeywordsCallback callback) {
        apiService.getPopularKeywords().enqueue(new Callback<ApiResponse<RecommendResponse.PopularKeywordsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<RecommendResponse.PopularKeywordsResponse>> call,
                                   Response<ApiResponse<RecommendResponse.PopularKeywordsResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "✓ Popular keywords loaded from network");
                    String[] keywords = response.body().getData().toKeywordArray();
                    if (keywords.length > 0) {
                        callback.onSuccess(keywords);
                        return;
                    }
                }
                Log.w(TAG, "Network recommend failed, falling back to mock");
                callback.onSuccess(MockVideoData.getRecommendKeywords());
            }

            @Override
            public void onFailure(Call<ApiResponse<RecommendResponse.PopularKeywordsResponse>> call, Throwable t) {
                Log.e(TAG, "Network recommend request failed: " + t.getMessage() + ", falling back to mock");
                callback.onSuccess(MockVideoData.getRecommendKeywords());
            }
        });
    }

    /**
     * 获取相关搜索关键词（异步，网络优先 + Mock 降级）
     */
    public void loadRelatedSearchKeywords(RecommendKeywordsCallback callback) {
        apiService.getPopularKeywords().enqueue(new Callback<ApiResponse<RecommendResponse.PopularKeywordsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<RecommendResponse.PopularKeywordsResponse>> call,
                                   Response<ApiResponse<RecommendResponse.PopularKeywordsResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "✓ Related search keywords loaded from network");
                    String[] keywords = response.body().getData().toKeywordArray();
                    if (keywords.length > 0) {
                        callback.onSuccess(keywords);
                        return;
                    }
                }
                Log.w(TAG, "Network Related failed, falling back to mock");
                callback.onSuccess(MockVideoData.getRelatedSearchKeywords());
            }

            @Override
            public void onFailure(Call<ApiResponse<RecommendResponse.PopularKeywordsResponse>> call, Throwable t) {
                Log.e(TAG, "Network Related request failed: " + t.getMessage() + ", falling back to mock");
                callback.onSuccess(MockVideoData.getRelatedSearchKeywords());
            }
        });
    }
}
