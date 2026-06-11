package com.example.bd_client_sidejob.ui.searchresult;

import android.util.Log;

import com.example.bd_client_sidejob.base.BasePresenter;
import com.example.bd_client_sidejob.data.model.Video;
import com.example.bd_client_sidejob.data.repository.VideoRepository;
import com.example.bd_client_sidejob.data.repository.VideoRepositoryImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果页面 Presenter（网络优先 + Mock 降级）
 */
public class SearchResultPresenter extends BasePresenter<SearchResultContract.View> implements SearchResultContract.Presenter {
    private static final String TAG = "SearchResultPresenter";

    private final VideoRepository videoRepository;

    public SearchResultPresenter() {
        this.videoRepository = VideoRepositoryImpl.getInstance();
    }

    @Override
    public void loadSearchResults(String keyword) {
        if (!isViewAttached()) {
            return;
        }

        getView().showLoading();

        // 网络优先搜索
        videoRepository.searchVideos(keyword, new VideoRepository.SearchVideoCallback() {
            @Override
            public void onSuccess(List<Video> results) {
                if (!isViewAttached()) return;
                getView().hideLoading();
                if (results.isEmpty()) {
                    getView().showEmptyResult();
                } else {
                    getView().showSearchResults(results);
                }
            }

            @Override
            public void onError(String message) {
                if (!isViewAttached()) return;
                getView().hideLoading();
                getView().showToast(message);
            }
        });
    }

    @Override
    public void loadRelatedSearch(String keyword) {
        if (!isViewAttached()) {
            return;
        }

        // 网络优先获取推荐词
        videoRepository.getRecommendKeywords(new VideoRepository.RecommendKeywordsCallback() {
            @Override
            public void onSuccess(String[] keywords) {
                if (!isViewAttached()) return;

                List<String> filtered = new ArrayList<>();
                for (String kw : keywords) {
                    if (!kw.equalsIgnoreCase(keyword)) {
                        filtered.add(kw);
                    }
                }
                getView().showRelatedSearch(filtered);
            }

            @Override
            public void onError(String message) {
                // ignore
            }
        });
    }

    @Override
    public void onVideoClick(Video video) {
        if (isViewAttached()) {
            getView().navigateToVideoPlay(video);
        }
    }

    @Override
    public void onRelatedSearchClick(String keyword) {
        if (isViewAttached()) {
            // 重新搜索
            loadSearchResults(keyword);
            loadRelatedSearch(keyword);
        }
    }
}
