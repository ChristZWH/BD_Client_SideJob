package com.example.bd_client_sidejob.data.api;

import com.google.gson.annotations.SerializedName;

/**
 * 统一 API 响应包装类（对接 Go 服务）
 * @param <T> 业务数据类型
 */
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    public ApiResponse() {}

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    /** 返回 根 下的 data字段 */
    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    /** 根据 根 下的 业务状态码进行判断，相应是否成功*/
    public boolean isSuccess() {
        return code == 0;
    }
}
