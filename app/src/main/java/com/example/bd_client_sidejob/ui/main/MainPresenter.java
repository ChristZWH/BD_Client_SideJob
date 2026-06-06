package com.example.bd_client_sidejob.ui.main;

import com.example.bd_client_sidejob.base.BasePresenter;
import com.example.bd_client_sidejob.data.model.Video;
import com.example.bd_client_sidejob.data.model.VideoList;
import com.example.bd_client_sidejob.data.repository.VideoRepository;
import com.example.bd_client_sidejob.data.repository.VideoRepositoryImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * MainPresenter - 主页面的业务逻辑控制器
 * 采用 MVP 架构模式，作为 Presenter 层负责业务逻辑处理和状态管理
 * 负责视频数据加载、播放控制、页面切换等核心业务
 */
public class MainPresenter extends BasePresenter<MainContract.View> implements MainContract.Presenter {

    /** 视频数据仓库 - 用于获取视频数据 */
    private final VideoRepository videoRepository;
    /** 视频列表数据 - 缓存已加载的视频 */
    private List<Video> videoList;
    /** 当前页码 - 用于分页加载 */
    private int currentPage = 0;
    /** 每页大小 - 每页加载的视频数量 */
    private final int pageSize = 5;
    /** 是否有更多数据 - 用于判断是否可以继续加载 */
    private boolean hasMore = true;
    /** 是否正在加载 - 防止重复请求 */
    private boolean isLoading = false;
    /** 当前播放位置 - 记录正在播放的视频位置 */
    private int currentPlayingPosition = -1;

    /**
     * 构造函数 - 初始化数据仓库和视频列表
     */
    public MainPresenter() {
        this.videoRepository = VideoRepositoryImpl.getInstance();
        this.videoList = new ArrayList<>();
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
        
        // 调用 Repository 获取数据（异步回调）
        videoRepository.getVideoList(page, pageSize, new VideoRepository.VideoListCallback() {
            @Override
            public void onSuccess(VideoList result) {
                isLoading = false;
                currentPage = result.getCurrentPage();
                hasMore = result.isHasMore();

                // 第一页需要清空旧数据（刷新或首次加载）
                if (page == 0) {
                    videoList.clear();
                }
                // 追加新数据到列表
                videoList.addAll(result.getVideos());

                // 通知 View 更新 UI
                if (isViewAttached()) {
                    getView().hideLoading();

                    if (page == 0) {
                        getView().showVideoList(result.getVideos());
                    } else {
                        getView().onMoreVideosLoaded(result.getVideos());
                    }

                    getView().hasMoreVideos(hasMore);
                }
            }

            @Override
            public void onError(String message) {
                isLoading = false;
                // 加载失败，通知 View 显示错误
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
     * 播放指定位置的视频
     * @param position 视频位置
     */
    @Override
    public void playVideo(int position) {
        if (position >= 0 && position < videoList.size()) {
            // 更新当前播放位置
            currentPlayingPosition = position;
            Video video = videoList.get(position);
            // 通知 View 播放视频
            if (isViewAttached()) {
                getView().onVideoLoaded(video, position);
            }
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
        if (position >= 0 && position < videoList.size()) {
            // 如果有其他视频在播放，先暂停
            if (currentPlayingPosition != -1 && currentPlayingPosition != position) {
                pauseVideo(currentPlayingPosition);
            }
            // 播放当前视频
            playVideo(position);
        }
    }

    /**
     * 获取视频列表
     * @return 视频列表
     */
    public List<Video> getVideoList() {
        return videoList;
    }

    /**
     * 获取当前播放位置
     * @return 当前播放位置（-1表示没有正在播放的视频）
     */
    public int getCurrentPlayingPosition() {
        return currentPlayingPosition;
    }

    /**
     * 获取指定位置的视频
     * @param position 视频位置
     * @return 视频对象（越界返回null）
     */
    public Video getVideoAtPosition(int position) {
        if (position >= 0 && position < videoList.size()) {
            return videoList.get(position);
        }
        return null;
    }
}