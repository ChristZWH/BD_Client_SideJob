package com.example.bd_client_sidejob.ui.search;

import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bd_client_sidejob.R;
import com.example.bd_client_sidejob.base.BaseActivity;
import com.example.bd_client_sidejob.data.model.SearchHistory;
import com.example.bd_client_sidejob.ui.searchresult.SearchResultActivity;

import java.util.List;

/**
 * 搜索页面 - MVP View 层
 * 负责搜索界面的展示和用户交互处理
 * 功能：搜索框 + 历史记录 + 推荐词
 */
public class SearchActivity extends BaseActivity<SearchContract.Presenter> implements SearchContract.View {

    /** 搜索输入框 */
    private EditText etSearch;
    /** 返回按钮 */
    private ImageView ivBack;
    /** 搜索按钮 */
    private TextView tvSearch;
    /** 搜索历史容器 */
    private LinearLayout llHistory;
    /** 清空历史按钮 */
    private ImageView ivClearHistory;
    /** 搜索历史左列 */
    private LinearLayout llHistoryLeft;
    /** 搜索历史右列 */
    private LinearLayout llHistoryRight;

    /**
     * 获取布局资源ID
     * @return 布局文件的资源ID
     */
    @Override
    protected int getLayoutId() {
        return R.layout.activity_search;
    }

    /**
     * 创建 Presenter 实例
     * MVP 标准初始化模式：View 负责创建并管理 Presenter，确保生命周期同步
     * @return SearchPresenter 实例
     */
    @Override
    protected SearchContract.Presenter createPresenter() {
        return new SearchPresenter();
    }

    /**
     * 初始化视图组件
     * 绑定布局控件、设置事件监听
     */
    @Override
    protected void initView() {
        // 绑定视图组件
        etSearch = findViewById(R.id.etSearch);
        ivBack = findViewById(R.id.ivBack);
        tvSearch = findViewById(R.id.tvSearch);
        llHistory = findViewById(R.id.llHistory);
        ivClearHistory = findViewById(R.id.ivClearHistory);
        llHistoryLeft = findViewById(R.id.llHistoryLeft);
        llHistoryRight = findViewById(R.id.llHistoryRight);

        // 设置事件监听
        setupListeners();
    }

    /**
     * 初始化数据
     * View 层通知 Presenter 加载搜索历史和推荐关键词
     */
    @Override
    protected void initData() {
        // 通过 Presenter 加载搜索历史和推荐关键词
        if (mPresenter != null) {
            mPresenter.loadSearchHistoryAndRecommend();
        }
    }

    /**
     * Activity 恢复时调用（从其他页面返回时）
     */
    @Override
    protected void onResume() {
        super.onResume();
        // 重新加载搜索历史，确保从搜索结果页面返回时能看到最新的历史记录
        if (mPresenter != null) {
            mPresenter.loadSearchHistoryAndRecommend();
        }
    }

    /**
     * 设置所有事件监听器
     */
    private void setupListeners() {
        // 返回按钮：关闭当前页面
        ivBack.setOnClickListener(v -> finish());

        // 搜索文本变化监听
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // 如果文本为空，显示历史和推荐
                if (s.toString().trim().isEmpty() && mPresenter != null) {
                    mPresenter.loadSearchHistoryAndRecommend();
                }
            }
        });

        // 键盘搜索按钮监听
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                if (mPresenter != null) {
                    mPresenter.performSearch(etSearch.getText().toString().trim());
                }
                return true;
            }
            return false;
        });

        // 搜索按钮点击监听
        tvSearch.setOnClickListener(v -> {
            String keyword = etSearch.getText().toString().trim();
            if (!keyword.isEmpty() && mPresenter != null) {
                hideKeyboard();
                mPresenter.performSearch(keyword);
            }
        });

        // 清空历史按钮点击监听
        ivClearHistory.setOnClickListener(v -> {
            if (mPresenter != null) {
                mPresenter.clearAllSearchHistory();
            }
        });
    }

    /** 隐藏软键盘 （内部高度封装，直接调用外部API即可，不必关系内部结构）*/
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * dp转px 工具方法
     * @param dp dp值
     * @return px值
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    /**
     * 创建标签视图（用于搜索历史和推荐关键词）
     * @param text 标签文本
     * @param isHistory 是否是搜索历史（搜索历史使用线性布局参数，推荐使用流式布局参数）
     * @return TextView 标签视图
     */
    private TextView createTagView(String text, boolean isHistory) {
        TextView tag = new TextView(this);
        tag.setText(text);
        tag.setTextColor(0xFF333333);
        tag.setTextSize(13);
        tag.setBackgroundResource(R.drawable.bg_tag);
        tag.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));

        // 设置布局参数（统一使用 LinearLayout.LayoutParams）
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dpToPx(8)); // 底边距
        tag.setLayoutParams(params);

        return tag;
    }

    // ==================== View 接口实现 ====================

    /**
     * 获取 Context
     */
    @Override
    public android.content.Context getContext() {
        return this;
    }

    /**
     * 显示搜索历史
     * @param historyList 搜索历史列表
     */
    @Override
    public void showSearchHistory(List<SearchHistory> historyList) {
        // 清空历史视图
        llHistoryLeft.removeAllViews();
        llHistoryRight.removeAllViews();

        // 如果历史为空，隐藏历史区域
        if (historyList.isEmpty()) {
            llHistory.setVisibility(View.GONE);
            return;
        }

        // 显示历史区域
        llHistory.setVisibility(View.VISIBLE);

        // 计算每列的数量
        int size = historyList.size();
        int leftCount = (size + 1) / 2; // 左列数量（向上取整）

        // 遍历历史列表，按两列排放
        // 最近的搜索在上方，按行填充：第一行左→右，第二行左→右...
        for (int i = 0; i < size; i++) {
            SearchHistory history = historyList.get(i);
            TextView tag = createTagView(history.getKeyword(), true); // true 表示是搜索历史
            // 点击标签：填充搜索框并执行搜索
            tag.setOnClickListener(v -> {
                etSearch.setText(history.getKeyword());
                etSearch.setSelection(history.getKeyword().length());
                hideKeyboard();
                if (mPresenter != null) {
                    mPresenter.performSearch(history.getKeyword());
                }
            });
            
            // 长按标签：删除该条历史
            tag.setOnLongClickListener(v -> {
                if (mPresenter != null) {
                    mPresenter.removeSearchHistory(history.getKeyword());
                }
                return true;
            });

            // 按行填充：偶数索引(0,2,4...)放在左列，奇数索引(1,3,5...)放在右列
            // 这样：第0个（最近）→ 左列第1行，第1个→右列第1行，第2个→左列第2行，第3个→右列第2行...
            if (i % 2 == 0) {
                llHistoryLeft.addView(tag);
            } else {
                llHistoryRight.addView(tag);
            }
        }
    }

    /**
     * 跳转到搜索结果页面
     * @param keyword 搜索关键词
     */
    @Override
    public void navigateToSearchResult(String keyword) {
        Intent intent = new Intent(this, SearchResultActivity.class);
        // 键值对，传输数据；也可以通过bundle进行传输
        intent.putExtra(SearchResultActivity.EXTRA_KEYWORD, keyword);
        startActivity(intent);
    }

    /** 显示加载状态（预留方法） */
    @Override
    public void showLoading() {
        // 可以添加加载动画
    }

    /** 隐藏加载状态（预留方法） */
    @Override
    public void hideLoading() {
        // 隐藏加载动画
    }

    /**
     * 显示 Toast 提示
     * @param message 提示消息
     */
    @Override
    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}