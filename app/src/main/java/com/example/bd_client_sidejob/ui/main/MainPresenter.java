package com.example.bd_client_sidejob.ui.main;

import android.content.Context;
import android.util.Log;

import com.example.bd_client_sidejob.base.BasePresenter;
import com.example.bd_client_sidejob.data.local.MockVideoData;
import com.example.bd_client_sidejob.data.model.ImageCard;
import com.example.bd_client_sidejob.data.model.Video;
import com.example.bd_client_sidejob.data.model.VideoList;
import com.example.bd_client_sidejob.data.repository.VideoRepository;
import com.example.bd_client_sidejob.data.repository.VideoRepositoryImpl;
import com.example.bd_client_sidejob.util.PlayerManager;
import com.example.bd_client_sidejob.util.PreloadConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * MainPresenter - 主页面的业务逻辑控制器
 * 采用 MVP 架构模式，作为 Presenter 层负责业务逻辑处理和状态管理
 * 负责视频数据加载、播放控制、页面切换、预加载等核心业务
 */
public class MainPresenter extends BasePresenter<MainContract.View> implements MainContract.Presenter {
    private static final String TAG = "MainPresenter";

    /** 视频数据仓库 - 用于获取视频数据 */
    private final VideoRepositoryImpl videoRepository;
    /** 视频列表数据 - 缓存已加载的视频 */
    private List<Video> videoList;
    /** 图片卡片列表 */
    private List<ImageCard> imageCardList;
    /** 混合数据列表（Video + ImageCard 混排） */
    private List<Object> feedItems;
    /** 当前页码 - 用于分页加载 */
    private int currentPage = 0;
    /** 每页大小 - 每页加载的视频数量 */
    private final int pageSize = 5;
    /** 每次插入图片卡片的视频间隔（每 N 个视频插入 1 张图片） */
    private final int imageCardInterval = 3;
    /** 是否有更多数据 - 用于判断是否可以继续加载 */
    private boolean hasMore = true;
    /** 是否正在加载 - 防止重复请求 */
    private boolean isLoading = false;
    /** 当前播放位置 - 记录正在播放的视频位置 */
    private int currentPlayingPosition = -1;

    /** 播放器管理器 - 用于预加载管理 */
    private PlayerManager playerManager;
    /** 是否是首次播放（用于记录基准延迟） */
    private boolean isFirstPlay = true;
    /** 视频开始播放时间戳 */
    private long playStartTime;

    /**
     * 构造函数 - 初始化数据仓库和视频列表
     */
    public MainPresenter() {
        this.videoRepository = VideoRepositoryImpl.getInstance();
        this.videoList = new ArrayList<>();
        this.imageCardList = new ArrayList<>();
        this.feedItems = new ArrayList<>();
    }

    /**
     * 初始化播放器管理器 playerManager
     * 极简配置：每个 ExoPlayer ~60MB，192MB 堆最多安全承载 2 个
     */
    public void initPlayerManager(Context context) {
        this.playerManager = PlayerManager.getInstance(context);

        // 低内存安全配置：单预加载 + 极小缓存池
        PreloadConfig config = new PreloadConfig.Builder()
                .enabled(true)
                .forwardCount(1)
                .backwardCount(0)       // 只预加载下一个视频，不预加载上一个
                .preloadDurationMs(500) // 仅缓冲 500ms（快速释放 MediaCodec 临时 buffer）
                .maxCachedPlayers(1)    // 极严限制：仅缓存 1 个预加载播放器
                .enableStatistics(true)
                .build();
        playerManager.setConfig(config);

        Log.d(TAG, "PlayerManager initialized with memory-safe config: forward=" + config.forwardCount
                + ", backward=" + config.backwardCount + ", duration=" + config.preloadDurationMs
                + ", maxCached=" + config.maxCachedPlayers);
    }

    /**
     * 加载视频列表（搜索模式）
     * 先展示搜索匹配的视频，播完搜索结果后再加载全量视频，每 3 个视频插入 1 张图片卡片
     * @param targetVideoId 目标视频ID（搜索结果的第一个视频先播放）
     * @param keyword 搜索关键词
     */
    public void loadSearchResultsFirst(long targetVideoId, String keyword) {
        if (isLoading) return;
        isLoading = true;

        List<Video> searchResults = MockVideoData.searchVideos(keyword);
        List<Video> allVideos = MockVideoData.getMockVideos();

        // 去重：先放搜索结果，再补上全量中不在搜索结果里的
        videoList.clear();
        videoList.addAll(searchResults);

        Set<Long> searchIds = new HashSet<>();
        for (Video v : searchResults) {
            searchIds.add(v.getVideoId());
        }
        for (Video v : allVideos) {
            if (!searchIds.contains(v.getVideoId())) {
                videoList.add(v);
            }
        }

        // 加载图片卡片数据
        imageCardList = MockVideoData.getImageCards();

        // 混合为 feedItems（视频 + 图片卡片混排）
        mixFeedItems();

        isLoading = false;

        if (isViewAttached()) {
            getView().showFeedItems(feedItems);
        }
    }

    /**
     * 加载视频列表
     * @param page 页码（从0开始）
     * @param pageSize 每页大小
     */
    @Override
    public void loadVideos(int page, int pageSize) {
        // 防止重复加载
        if (isLoading) {
            return;
        }

        isLoading = true;

        // 第一页时显示加载进度
        if (page == 0 && isViewAttached()) {
            getView().showLoading();
        }

        // 使用 Feed 接口获取混排数据（网络优先 + Mock 降级）
        videoRepository.getFeed(page, pageSize, imageCardInterval, new VideoRepositoryImpl.FeedCallback() {
            @Override
            public void onSuccess(List<Object> items, boolean hasMoreData) {
                isLoading = false;
                currentPage = page;
                hasMore = hasMoreData;

                // 提取视频列表和图片卡片列表
                if (page == 0) {
                    videoList.clear();
                    imageCardList.clear();
                }
                for (Object item : items) {
                    if (item instanceof Video) {
                        videoList.add((Video) item);
                    } else if (item instanceof ImageCard) {
                        imageCardList.add((ImageCard) item);
                    }
                }

                // 第一页替换，后续追加
                if (page == 0) {
                    feedItems.clear();
                }
                feedItems.addAll(items);

                // 通知 View 更新 UI
                if (isViewAttached()) {
                    getView().hideLoading();

                    if (page == 0) {
                        getView().showFeedItems(feedItems);
                    } else {
                        getView().onMoreFeedItemsLoaded(items);
                    }

                    getView().hasMoreVideos(hasMore); // hasMoreVideos预留扩展
                }
            }

            @Override
            public void onError(String message) {
                isLoading = false;
                if (isViewAttached()) {
                    getView().hideLoading();
                    getView().showError(message);
                }
            }
        });
    }

    /**
     * 加载更多视频（分页加载下一页）
     */
    @Override
    public void loadMoreVideos() {
        // 没有更多数据或正在加载时不执行
        if (!hasMore || isLoading) {
            return;
        }
        // 加载下一页
        loadVideos(currentPage + 1, pageSize);
    }

    /**
     * 将视频列表和图片卡片混合为统一的 feedItems
     * 每 imageCardInterval 个视频插入 1 张图片卡片
     * 注：Feed 接口已经返回混排结果，此方法用于降级/Mock 场景
     */
    private void mixFeedItems() {
        feedItems.clear();
        int cardIndex = 0;

        for (int i = 0; i < videoList.size(); i++) {
            feedItems.add(videoList.get(i));
            // 每 3 个视频插入 1 张图片卡片
            if ((i + 1) % imageCardInterval == 0 && imageCardList != null && cardIndex < imageCardList.size()) {
                feedItems.add(imageCardList.get(cardIndex));
                cardIndex++;
            }
        }
    }

    /**
     * 播放指定位置的视频
     * @param position 视频位置（在 feedItems 中的位置）
     */
    @Override
    public void playVideo(int position) {
        if (position >= 0 && position < feedItems.size()) {
            Object item = feedItems.get(position);
            if (!(item instanceof Video)) {
                return; // 图片卡片不需要播放
            }

            Video video = (Video) item;
            // 记录播放开始时间（用于统计起播延迟）
            playStartTime = System.currentTimeMillis();

            // 更新当前播放位置
            currentPlayingPosition = position;

            // 预加载当前视频前后的视频（在播放前就开始预加载）
            schedulePreloading(position);

            // 通知 View 播放视频
            if (isViewAttached()) {
                getView().onVideoLoaded(video, position);
            }
        }
    }

    /**
     * 记录视频播放准备完成（用于统计起播延迟）
     */
    public void onVideoPrepared() {
        if (playerManager != null && playerManager.getConfig().enableStatistics) {
            long delay = System.currentTimeMillis() - playStartTime;

            if (isFirstPlay) {
                // 首次播放记录基准延迟（无预加载状态）
                playerManager.getStatistics().recordBaselineDelay(delay);
                isFirstPlay = false;
                Log.d(TAG, "Baseline delay recorded: " + delay + "ms");
            } else {
                // 记录优化后延迟
                playerManager.getStatistics().recordOptimizedDelay(delay);
                Log.d(TAG, "Optimized delay recorded: " + delay + "ms");
            }
        }
    }

    /**
     * 调度预加载
     * 预加载当前视频前后的视频（跳过图片卡片项）
     */
    private void schedulePreloading(int currentPosition) {
        if (playerManager == null || !playerManager.getConfig().enabled) {
            return;
        }

        PreloadConfig config = playerManager.getConfig();

        // 预加载前面的视频（向后预加载），跳过图片卡片
        int skipped = 0;
        for (int i = 1; i <= config.backwardCount + skipped; i++) {
            int pos = currentPosition - i;
            if (pos < 0) break;
            if (pos >= feedItems.size()) break;
            Object item = feedItems.get(pos);
            if (!(item instanceof Video)) {
                skipped++;
                continue;
            }
            Video video = (Video) item;
            if (!playerManager.isCached(String.valueOf(video.getVideoId())) && !playerManager.isPreloading(String.valueOf(video.getVideoId()))) {
                playerManager.preloadVideo(String.valueOf(video.getVideoId()), video.getUrl());
                Log.d(TAG, "Scheduled preload for previous video: " + pos + " - " + video.getTitle());
            }
        }

        // 预加载后面的视频（向前预加载），跳过图片卡片
        skipped = 0;
        for (int i = 1; i <= config.forwardCount + skipped; i++) {
            int pos = currentPosition + i;
            if (pos >= feedItems.size()) break;
            Object item = feedItems.get(pos);
            if (!(item instanceof Video)) {
                skipped++;
                continue;
            }
            Video video = (Video) item;
            if (!playerManager.isCached(String.valueOf(video.getVideoId())) && !playerManager.isPreloading(String.valueOf(video.getVideoId()))) {
                playerManager.preloadVideo(String.valueOf(video.getVideoId()), video.getUrl());
                Log.d(TAG, "Scheduled preload for next video: " + pos + " - " + video.getTitle());
            }
        }
    }

    /**
     * 获取播放器管理器
     */
    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    /**
     * 输出预加载统计报告，在MainActivity销毁阶段调用
     */
    public void printPreloadReport() {
        if (playerManager != null) {
            playerManager.getStatistics().printReport();
        }
    }

    /**
     * 暂停指定位置的视频
     * @param position 视频位置
     */
    @Override
    public void pauseVideo(int position) {
        if (position >= 0 && position < videoList.size() && isViewAttached()) {
            // 如果暂停的是当前播放的视频，清空播放位置记录
            if (currentPlayingPosition == position) {
                currentPlayingPosition = -1;
            }
            // 通知 View 暂停视频
            getView().onVideoPaused(position);
        }
    }

    /**
     * 释放指定位置的视频资源
     * @param position 视频位置
     */
    @Override
    public void releaseVideo(int position) {
        if (position >= 0 && position < videoList.size() && isViewAttached()) {
            // 如果释放的是当前播放的视频，清空播放位置记录
            if (currentPlayingPosition == position) {
                currentPlayingPosition = -1;
            }
            // 通知 View 释放视频资源
            getView().onVideoReleased(position);
        }
    }

    /**
     * 页面切换处理
     * 暂停上一个视频，播放当前视频
     * @param position 新页面位置
     */
    @Override
    public void onVideoPageChanged(int position) {
        if (position >= 0 && position < feedItems.size()) {
            // 图片卡片不需要播放控制
            Object item = feedItems.get(position);
            if (!(item instanceof Video)) {
                return;
            }
            // 如果有其他视频在播放，先暂停
            if (currentPlayingPosition != -1 && currentPlayingPosition != position) {
                pauseVideo(currentPlayingPosition);
            }
            // 播放当前视频
            playVideo(position);
        }
    }

    /**
     * 预加载即将播放的视频（用户滑动前调用）
     * 在用户滑动到新视频之前就开始预加载，提升响应速度
     * @param position 即将播放的视频位置（在 feedItems 中的位置）
     */
    public void preloadUpcomingVideo(int position) {
        Log.d(TAG, "preloadUpcomingVideo called for position: " + position);

        if (playerManager == null) {
            Log.d(TAG, "playerManager is null, skipping preload");
            return;
        }

        if (!playerManager.getConfig().enabled) {
            Log.d(TAG, "preload is disabled, skipping");
            return;
        }

        if (feedItems == null || feedItems.isEmpty()) {
            Log.d(TAG, "feedItems is empty, skipping preload");
            return;
        }

        // 预加载即将播放的视频本身（跳过图片卡片）
        int targetPos = position;
        Log.d(TAG, "targetPos: " + targetPos + ", feedItems.size(): " + feedItems.size());

        if (targetPos >= 0 && targetPos < feedItems.size()) {
            Object item = feedItems.get(targetPos);
            if (!(item instanceof Video)) {
                Log.d(TAG, "Target position is not a video, skipping preload");
                return;
            }
            Video video = (Video) item;
            String videoIdStr = String.valueOf(video.getVideoId());
            if (!playerManager.isCached(videoIdStr) && !playerManager.isPreloading(videoIdStr)) {
                playerManager.preloadVideo(videoIdStr, video.getUrl());
                Log.d(TAG, "Preloading upcoming video: " + targetPos + " - " + video.getTitle());
            } else {
                Log.d(TAG, "Video already cached or preloading: " + video.getVideoId());
            }
        } else {
            Log.d(TAG, "Position out of bounds: " + targetPos);
        }

        // 同时预加载前后的视频
        schedulePreloading(position);
    }

    /**
     * 获取视频列表
     * @return 视频列表
     */
    public List<Video> getVideoList() {
        return videoList;
    }

    /**
     * 获取混合数据列表
     * @return 混合数据列表（Video + ImageCard）
     */
    public List<Object> getFeedItems() {
        return feedItems;
    }

    /**
     * 获取当前播放位置
     * @return 当前播放位置（-1表示没有正在播放的视频）
     */
    public int getCurrentPlayingPosition() {
        return currentPlayingPosition;
    }

    /**
     * 获取指定位置的视频（仅对视频项有效）
     * @param position 在 feedItems 中的位置
     * @return 视频对象（越界或非视频项返回null）
     */
    public Video getVideoAtPosition(int position) {
        if (position >= 0 && position < feedItems.size()) {
            Object item = feedItems.get(position);
            if (item instanceof Video) {
                return (Video) item;
            }
        }
        return null;
    }
}