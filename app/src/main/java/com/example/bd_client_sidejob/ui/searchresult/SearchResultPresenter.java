package com.example.bd_client_sidejob.ui.searchresult;

import com.example.bd_client_sidejob.base.BasePresenter;
import com.example.bd_client_sidejob.data.local.MockVideoData;
import com.example.bd_client_sidejob.data.model.Video;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果页面 Presenter
 */
public class SearchResultPresenter extends BasePresenter<SearchResultContract.View> implements SearchResultContract.Presenter {

    @Override
    public void loadSearchResults(String keyword) {
        if (!isViewAttached()) {
            return;
        }

        getView().showLoading();

        // 搜索视频
        List<Video> results = MockVideoData.searchVideos(keyword);

        if (!isViewAttached()) {
            return;
        }

        getView().hideLoading();
        if (results.isEmpty()) {
            getView().showEmptyResult();
        } else {
            getView().showSearchResults(results);
        }
    }

    @Override
    public void loadRelatedSearch(String keyword) {
        if (!isViewAttached()) {
            return;
        }

        // 获取相关搜索推荐
        String[] relatedKeywords = MockVideoData.getRecommendKeywords();
        List<String> filtered = new ArrayList<>();
        
        // 过滤掉与当前关键词相同的推荐
        for (String kw : relatedKeywords) {
            if (!kw.equalsIgnoreCase(keyword)) {
                filtered.add(kw);
            }
        }

        getView().showRelatedSearch(filtered);
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
