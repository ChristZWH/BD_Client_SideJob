package com.example.bd_client_sidejob.base;

// 基础Presenter类
public abstract class BasePresenter<V extends BaseView> {
    protected V mView;

    public void attachView(V view) {
        this.mView = view;
        mView.setPresenter(this);
    }

    public void detachView() {
        if (mView != null) {
            mView.setPresenter(null);
        }
        this.mView = null;
    }

    public boolean isViewAttached() {
        return mView != null;
    }

    public V getView() {
        return mView;
    }
}