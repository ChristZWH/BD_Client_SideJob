package com.example.bd_client_sidejob.data.api;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Retrofit 客户端单例
 * Base URL 指向 Go 视频数据服务
 */
public class RetrofitClient {
    private static final String TAG = "RetrofitClient";

    // Go 服务地址 — 开发环境本地地址（模拟器用 10.0.2.2 映射到宿主机 localhost）
    // 真机调试请改为电脑局域网 IP
    public static final String BASE_URL_EMULATOR = "http://10.0.2.2:8080/";
    public static final String BASE_URL_DEVICE = "http://192.168.1.100:8080/";

    // 当前使用的 Base URL
    private static String baseUrl = BASE_URL_EMULATOR;

    private static RetrofitClient instance;
    private Retrofit retrofit;
    private ApiService apiService;

    // 私有初始化函数，由公有 getInstance() 调用初始化
    private RetrofitClient() {
        // HTTP 日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(
                message -> Log.d(TAG, message)
        );
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // 1.OkHttp 客户端配置
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();

        // 2.Gson 配置（注册 FeedResponse，自定义反序列化器）
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(FeedResponse.class, new FeedResponse.FeedResponseDeserializer())
                .create();

        // 3.Retrofit 实例（类似 Gin 中创建 Engine）
        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        // 4.创建 API 接口实例（类似 Gin 中 Engine 的路由注册）
        apiService = retrofit.create(ApiService.class);
    }

    public static synchronized RetrofitClient getInstance() {
        if (instance == null) {
            instance = new RetrofitClient();
        }
        return instance;
    }

    /**
     * 设置 Base URL（用于真机调试切换地址）
     */
    public static void setBaseUrl(String url) {
        baseUrl = url;
        // 重新初始化
        instance = null;
    }

    public ApiService getApiService() {
        return apiService;
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }
}
