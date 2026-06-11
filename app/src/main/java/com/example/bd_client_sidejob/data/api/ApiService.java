package com.example.bd_client_sidejob.data.api;

import com.example.bd_client_sidejob.data.model.Video;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Retrofit API 接口 — 对接 Go 视频数据服务
 * Base URL: http://<host>:8080/api/v1
 */
public interface ApiService {

    // ==================== 视频相关 ====================

    /**
     * 获取视频列表（分页）
     * GET /api/v1/videos?page=0&pageSize=5
     */
    @GET("api/v1/videos")
    Call<ApiResponse<VideoListResponse>> getVideoList(
            @Query("page") int page,
            @Query("pageSize") int pageSize
    );

    /**
     * 获取单个视频详情
     * GET /api/v1/videos/16
     */
    @GET("api/v1/videos/{id}")
    Call<ApiResponse<Video>> getVideoById(
            @Path("id") long videoId
    );

    // ==================== Feed 混合流 ====================

    /**
     * 获取 Feed 混合流（视频 + 图片卡片混排）
     * GET /api/v1/feed?page=0&pageSize=5&imageInterval=3
     */
    @GET("api/v1/feed")
    Call<ApiResponse<FeedResponse>> getFeed(
            @Query("page") int page,
            @Query("pageSize") int pageSize,
            @Query("imageInterval") int imageInterval
    );

    // ==================== 搜索相关 ====================

    /**
     * 搜索视频
     * GET /api/v1/search?keyword=川菜&page=0&pageSize=20
     */
    @GET("api/v1/search")
    Call<ApiResponse<SearchResponse>> searchVideos(
            @Query("keyword") String keyword,
            @Query("page") int page,
            @Query("pageSize") int pageSize
    );

    /**
     * 获取全局热门推荐词（底部轮播用）
     * GET /api/v1/recommend/popular
     * 返回 { keywords: ["大兔子", "汽车评测", ...] }
     */
    @GET("api/v1/recommend/popular")
    Call<ApiResponse<RecommendResponse.PopularKeywordsResponse>> getPopularKeywords();

    /**
     * 获取当前视频相关的推荐词
     * GET /api/v1/recommend/16
     * 返回 { videoId: 16, keywords: [{ keyword: "...", source: "...", score: 0.95 }] }
     */
    @GET("api/v1/recommend/{videoId}")
    Call<ApiResponse<RecommendResponse>> getRecommendByVideo(
            @Path("videoId") long videoId
    );

    // ==================== 图片卡片 ====================

    /**
     * 获取所有图片卡片
     * GET /api/v1/imagecards
     */
    @GET("api/v1/imagecards")
    Call<ApiResponse<ImageCardResponse>> getImageCards();
}
