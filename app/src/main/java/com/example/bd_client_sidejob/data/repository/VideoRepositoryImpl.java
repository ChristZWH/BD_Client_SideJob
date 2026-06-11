package com.example.bd_client_sidejob.data.repository;

import android.util.Log;

import com.example.bd_client_sidejob.data.api.ApiResponse;
import com.example.bd_client_sidejob.data.api.ApiService;
import com.example.bd_client_sidejob.data.api.FeedResponse;
import com.example.bd_client_sidejob.data.api.ImageCardResponse;
import com.example.bd_client_sidejob.data.api.RecommendResponse;
import com.example.bd_client_sidejob.data.api.RetrofitClient;
import com.example.bd_client_sidejob.data.api.SearchResponse;
import com.example.bd_client_sidejob.data.api.VideoListResponse;
import com.example.bd_client_sidejob.data.local.MockVideoData;
import com.example.bd_client_sidejob.data.model.ImageCard;
import com.example.bd_client_sidejob.data.model.Video;
import com.example.bd_client_sidejob.data.model.VideoList;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 视频仓库实现类
 * 策略：网络优先（Go 服务）→ 失败降级到本地 Mock
 */
public class VideoRepositoryImpl implements VideoRepository {
    private static final String TAG = "VideoRepositoryImpl";

    private static VideoRepositoryImpl instance;
    private final ApiService apiService;

    private VideoRepositoryImpl() {
        this.apiService = RetrofitClient.getInstance().getApiService();
    }

    public static synchronized VideoRepositoryImpl getInstance() {
        if (instance == null) {
            instance = new VideoRepositoryImpl();
        }
        return instance;
    }

    @Override
    public void getVideoList(int page, int pageSize, VideoListCallback callback) {
        // 1. 尝试网络请求
        apiService.getVideoList(page, pageSize).enqueue(new Callback<ApiResponse<VideoListResponse>>() {
            @Override
            // call:发起请求的 Call 对象本身，可用于取消请求
            // response:HTTP 响应对象，包含状态码、响应头、响应体等
            public void onResponse(Call<ApiResponse<VideoListResponse>> call,
                                   Response<ApiResponse<VideoListResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "✓ Video list loaded from network, page=" + page);
                    VideoListResponse data = response.body().getData(); // data 是泛型T，在这里的T是VideoListResponse
                    VideoList videoList = new VideoList(
                            data.getVideos() != null ? data.getVideos() : new ArrayList<>(),
                            data.getCurrentPage(),
                            data.isHasMore()
                    );
                    callback.onSuccess(videoList);
                } else {
                    Log.w(TAG, "Network response failed, falling back to mock");
                    callback.onSuccess(getMockVideoList(page, pageSize));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<VideoListResponse>> call, Throwable t) {
                Log.e(TAG, "Network request failed: " + t.getMessage() + ", falling back to mock");
                callback.onSuccess(getMockVideoList(page, pageSize));
            }
        });
    }

    // 后续升级为 RAG 搜索接口
    @Override
    public void searchVideos(String keyword, SearchVideoCallback callback) {
        // 1. 尝试网络搜索
        apiService.searchVideos(keyword, 0, 80).enqueue(new Callback<ApiResponse<SearchResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<SearchResponse>> call,
                                   Response<ApiResponse<SearchResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "✓ Search results loaded from network, keyword=" + keyword);
                    List<Video> videos = response.body().getData().getVideos();
                    // 回调中只处理 videos，其他字段如分页信息等不处理
                    callback.onSuccess(videos != null ? videos : new ArrayList<>());
                } else {
                    Log.w(TAG, "Network search failed, falling back to mock");
                    callback.onSuccess(getMockSearchVideos(keyword));
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SearchResponse>> call, Throwable t) {
                Log.e(TAG, "Network search request failed: " + t.getMessage() + ", falling back to mock");
                callback.onSuccess(getMockSearchVideos(keyword));
            }
        });
    }

    @Override
    public void getRecommendKeywords(RecommendKeywordsCallback callback) {
        // 1. 尝试从网络获取热门推荐词（目前写死，直接从后端数据库中获取）
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

    @Override
    public void getImageCards(ImageCardsCallback callback) {
        // 1. 尝试从网络获取
        apiService.getImageCards().enqueue(new Callback<ApiResponse<ImageCardResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<ImageCardResponse>> call,
                                   Response<ApiResponse<ImageCardResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "✓ Image cards loaded from network");
                    List<ImageCard> cards = response.body().getData().getCards();
                    callback.onSuccess(cards != null ? cards : new ArrayList<>());
                } else {
                    Log.w(TAG, "Network imagecards failed, falling back to mock");
                    callback.onSuccess(MockVideoData.getImageCards());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<ImageCardResponse>> call, Throwable t) {
                Log.e(TAG, "Network imagecards request failed: " + t.getMessage() + ", falling back to mock");
                callback.onSuccess(MockVideoData.getImageCards());
            }
        });
    }

    @Override
    public void getVideoById(long videoId, VideoByIdCallback callback) {
        // 1. 尝试从网络获取
        apiService.getVideoById(videoId).enqueue(new Callback<ApiResponse<Video>>() {
            @Override
            public void onResponse(Call<ApiResponse<Video>> call,
                                   Response<ApiResponse<Video>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "✓ Video loaded from network, id=" + videoId);
                    callback.onSuccess(response.body().getData());
                } else {
                    Log.w(TAG, "Network videoById failed, falling back to mock");
                    getMockVideoById(videoId, callback);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Video>> call, Throwable t) {
                Log.e(TAG, "Network videoById request failed: " + t.getMessage() + ", falling back to mock");
                getMockVideoById(videoId, callback);
            }
        });
    }

    /**
     * 获取 Feed 混合流（网络优先 + Mock 降级）
     */
    public void getFeed(int page, int pageSize, int imageInterval, FeedCallback callback) {
        apiService.getFeed(page, pageSize, imageInterval).enqueue(new Callback<ApiResponse<FeedResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<FeedResponse>> call, Response<ApiResponse<FeedResponse>> response) {
                // 检查三方面；先查看HTTP状态码是否成功、是否有响应体以及业务状态码是否成功
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Log.d(TAG, "✓ Feed loaded from network, page=" + page);
                    FeedResponse data = response.body().getData();
                    callback.onSuccess(data.toFlatItems(), data.isHasMore());
                } else {
                    Log.w(TAG, "Network feed failed, falling back to mock");
                    getMockFeed(page, pageSize, imageInterval, callback);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<FeedResponse>> call, Throwable t) {
                Log.e(TAG, "Network feed request failed: " + t.getMessage() + ", falling back to mock");
                getMockFeed(page, pageSize, imageInterval, callback);
            }
        });
    }

    // ==================== 降级方法（Mock 数据） ====================

    private VideoList getMockVideoList(int page, int pageSize) {
        List<Video> videos = MockVideoData.getVideosByPage(page, pageSize);
        boolean hasMore = MockVideoData.hasMorePages(page, pageSize);
        return new VideoList(videos, page, hasMore);
    }

    private List<Video> getMockSearchVideos(String keyword) {
        return MockVideoData.searchVideos(keyword);
    }

    private void getMockVideoById(long videoId, VideoByIdCallback callback) {
        List<Video> allVideos = MockVideoData.getMockVideos();
        for (Video video : allVideos) {
            if (video.getVideoId() == videoId) {
                callback.onSuccess(video);
                return;
            }
        }
        callback.onError("Video not found");
    }

    private void getMockFeed(int page, int pageSize, int imageInterval, FeedCallback callback) {
        List<Video> videoPage = MockVideoData.getVideosByPage(page, pageSize);
        boolean hasMore = MockVideoData.hasMorePages(page, pageSize);
        List<ImageCard> allCards = MockVideoData.getImageCards();

        // 本地混排（与 Go 服务 mixItems 逻辑一致）
        List<Object> items = new ArrayList<>();
        int cardIndex = 0;
        int globalOffset = page * pageSize;

        for (int i = 0; i < videoPage.size(); i++) {
            items.add(videoPage.get(i));
            if ((globalOffset + i + 1) % imageInterval == 0 && cardIndex < allCards.size()) {
                items.add(allCards.get(cardIndex));
                cardIndex++;
            }
        }

        callback.onSuccess(items, hasMore);
    }
}
