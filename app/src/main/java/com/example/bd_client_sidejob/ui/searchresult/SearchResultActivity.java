package com.example.bd_client_sidejob.ui.searchresult;

import android.content.Intent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bd_client_sidejob.R;
import com.example.bd_client_sidejob.base.BaseActivity;
import com.example.bd_client_sidejob.data.model.Video;
import com.example.bd_client_sidejob.ui.main.MainActivity;
import com.example.bd_client_sidejob.ui.searchresult.adapter.SearchResultVideoAdapter;

import java.util.List;

/**
 * 搜索结果页面 - MVP View 层
 * 展示搜索结果视频列表
 */
public class SearchResultActivity extends BaseActivity<SearchResultContract.Presenter> implements SearchResultContract.View {

    public static final String EXTRA_KEYWORD = "keyword";

    private EditText etSearch;
    private ImageView ivBack;
    private TextView tvSearch;
    private RecyclerView rvSearchResult;
    private LinearLayout llEmpty;

    private SearchResultVideoAdapter videoAdapter;
    private String currentKeyword;

    /**
     * 获取布局资源ID
     * @return 布局文件的资源ID
     */
    @Override
    protected int getLayoutId() {
        return R.layout.activity_search_result;
    }

    /**
     * 创建 Presenter 实例
     * MVP 标准初始化模式：View 负责创建并管理 Presenter，确保生命周期同步
     * @return SearchResultPresenter 实例
     */
    @Override
    protected SearchResultContract.Presenter createPresenter() {
        return new SearchResultPresenter();
    }

    /**
     * 初始化视图组件
     * 绑定布局控件、设置事件监听
     */
    @Override
    protected void initView() {
        etSearch = findViewById(R.id.etSearch);
        ivBack = findViewById(R.id.ivBack);
        tvSearch = findViewById(R.id.tvSearch);
        rvSearchResult = findViewById(R.id.rvSearchResult);
        llEmpty = findViewById(R.id.llEmpty);

        // 设置视频列表
        rvSearchResult.setLayoutManager(new LinearLayoutManager(this));
        videoAdapter = new SearchResultVideoAdapter();
        rvSearchResult.setAdapter(videoAdapter);

        // 设置视频点击监听
        videoAdapter.setOnItemClickListener(video -> {
            if (mPresenter != null) {
                mPresenter.onVideoClick(video);
            }
        });

        // 设置事件监听
        setupListeners();
    }

    /**
     * 初始化数据
     * View 层通知 Presenter 加载搜索结果
     */
    @Override
    protected void initData() {
        // 获取传入的搜索关键词
        currentKeyword = getIntent().getStringExtra(EXTRA_KEYWORD);
        if (currentKeyword != null && !currentKeyword.isEmpty()) {
            etSearch.setText(currentKeyword);
            // 加载搜索结果
            if (mPresenter != null) {
                mPresenter.loadSearchResults(currentKeyword);
            }
        }
    }

    /** 设置事件监听器 */
    private void setupListeners() {
        // 返回按钮
        ivBack.setOnClickListener(v -> finish());

        // 搜索按钮
        tvSearch.setOnClickListener(v -> {
            String keyword = etSearch.getText().toString().trim();
            if (!keyword.isEmpty() && mPresenter != null) {
                hideKeyboard();
                currentKeyword = keyword;
                mPresenter.loadSearchResults(keyword);
            }
        });

        // 键盘搜索按钮
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String keyword = etSearch.getText().toString().trim();
                if (!keyword.isEmpty() && mPresenter != null) {
                    hideKeyboard();
                    currentKeyword = keyword;
                    mPresenter.loadSearchResults(keyword);
                    return true;
                }
            }
            return false;
        });
    }
    
    /** 隐藏软键盘 （内部高度封装，直接调用外部API即可，不必关系内部结构）*/
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    // ==================== View 接口实现 ====================

    /**
     * 获取 Context
     */
    @Override
    public android.content.Context getContext() {
        return this;
    }

    @Override
    public void showSearchResults(List<Video> videos) {
        llEmpty.setVisibility(View.GONE);
        rvSearchResult.setVisibility(View.VISIBLE);
        videoAdapter.setVideos(videos);
    }

    @Override
    public void showEmptyResult() {
        rvSearchResult.setVisibility(View.GONE);
        llEmpty.setVisibility(View.VISIBLE);
    }

    @Override
    public void showRelatedSearch(List<String> keywords) {
        // 搜索结果页面不显示相关搜索，此方法留空
    }

    @Override
    public void showLoading() {
        // 可以添加加载动画
    }

    @Override
    public void hideLoading() {
        // 隐藏加载动画
    }

    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void navigateToVideoPlay(Video video) {
        // 跳转到主页面播放视频，同时传递搜索关键词让 MainActivity 先展示搜索结果
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_VIDEO_ID, video.getVideoId());
        intent.putExtra(MainActivity.EXTRA_SEARCH_KEYWORD, currentKeyword);
        startActivity(intent);
    }
}
