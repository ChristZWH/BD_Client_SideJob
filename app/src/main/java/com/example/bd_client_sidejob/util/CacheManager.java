package com.example.bd_client_sidejob.util;

import android.content.Context;

import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import java.io.File;

/**
 * 缓存管理器
 * 使用 ExoPlayer 的 CacheDataSource 实现视频缓存，实现秒开效果
 * 虽然磁盘比内存慢，但比网络 快得多 。磁盘缓存的核心价值是 避免重复网络请求 ，而不是替代内存
 */
@UnstableApi
public class CacheManager {

    private static final String TAG = "CacheManager";
    private static final long CACHE_MAX_SIZE = 500 * 1024 * 1024; // 500MB 缓存大小

    private static CacheManager instance;
    private Cache cache;
    private DataSource.Factory cacheDataSourceFactory;

    private CacheManager(Context context) {
        // 创建缓存目录
        File cacheDir = new File(context.getCacheDir(), "video_cache");
        
        // 创建 LRU 缓存驱逐策略（当缓存超过最大大小时，删除最久未使用的文件）
        LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(CACHE_MAX_SIZE);
        
        // 1. 先创建简单缓存 Cache（底层存储）内部使用
        cache = new SimpleCache(cacheDir, evictor);
        
        // 创建缓存数据源工厂（默认数据源，只能从网络读取）
        DataSource.Factory defaultDataSourceFactory = new DefaultDataSource.Factory(
                context,
                new DefaultHttpDataSource.Factory()
        );
        
        // 2️. 再创建 cacheDataSourceFactory（基于 Cache 实现秒开效果）
        // ExoPlayer 使用
        cacheDataSourceFactory = new CacheDataSource.Factory()
                .setCache(cache) // 增加缓存功能
                .setUpstreamDataSourceFactory(defaultDataSourceFactory) // 保留网络读取
                // 忽略缓存错误，继续从网络读取数据
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR); // 容错机制
    }

    /**
     * 获取单例实例
     */
    public static synchronized CacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new CacheManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * 获取缓存数据源工厂
     */
    public DataSource.Factory getCacheDataSourceFactory() {
        return cacheDataSourceFactory;
    }

    /**
     * 获取缓存实例
     */
    public Cache getCache() {
        return cache;
    }

    /**
     * 获取缓存大小
     */
    public long getCacheSize() {
        return cache.getCacheSpace();
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        try {
            cache.release();
        } catch (Exception e) {
            android.util.Log.e(TAG, "Failed to clear cache", e);
        }
    }
}