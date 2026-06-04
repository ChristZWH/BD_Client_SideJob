package com.example.bd_client_sidejob.base;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

// 基础Activity类
public abstract class BaseActivity<P> extends AppCompatActivity implements BaseView<P> {

    protected P mPresenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId()); // 通过抽象方法获取布局ID并设置为当前Activity的布局视图
        initPresenter();               // 初始化Presenter
        initView();                    // 初始化视图
        initData();                    // 初始化数据
    }

    protected abstract int getLayoutId();

    protected abstract P createPresenter();

    private void initPresenter() {
        mPresenter = createPresenter();
        if (mPresenter != null && mPresenter instanceof BasePresenter) {
            ((BasePresenter) mPresenter).attachView(this);
        }
    }

    protected abstract void initView();

    protected abstract void initData();

    @Override
    public void setPresenter(P presenter) {
        this.mPresenter = presenter;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPresenter != null && mPresenter instanceof BasePresenter) {
            ((BasePresenter) mPresenter).detachView();
        }
    }
}