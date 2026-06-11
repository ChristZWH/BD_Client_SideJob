package com.example.bd_client_sidejob.util;

/**
 * 视频预加载配置类
 * 提供可配置的预加载参数，支持动态调整预加载策略
 */
public class PreloadConfig {
    // 是否启用预加载（默认开启）
    public boolean enabled = true;
    
    // 向前预加载数量（当前视频后面的视频）
    public int forwardCount = 2;
    
    // 向后预加载数量（当前视频前面的视频）
    public int backwardCount = 1;
    
    // 预加载数据量（毫秒）
    public int preloadDurationMs = 5000;
    
    // 最大同时缓存的播放器数量（降低到 2 个以控制内存，每个 ExoPlayer 约 40-80MB）
    // 防止OOM故障
    public int maxCachedPlayers = 2;
    
    // 是否启用数据量化统计
    public boolean enableStatistics = true;

    /**
     * 创建默认配置
     */
    public static PreloadConfig createDefault() {
        return new PreloadConfig();
    }

    /**
     * 创建禁用预加载的配置
     */
    public static PreloadConfig createDisabled() {
        PreloadConfig config = new PreloadConfig();
        config.enabled = false;
        return config;
    }

    /**
     * 构建器模式，方便链式配置
     */
    public static class Builder {
        private final PreloadConfig config = new PreloadConfig();

        public Builder enabled(boolean enabled) {
            config.enabled = enabled;
            return this; // 返回 this 实现链式调用
        }

        public Builder forwardCount(int count) {
            config.forwardCount = count;
            return this;
        }

        public Builder backwardCount(int count) {
            config.backwardCount = count;
            return this;
        }

        public Builder preloadDurationMs(int duration) {
            config.preloadDurationMs = duration;
            return this;
        }

        public Builder maxCachedPlayers(int count) {
            config.maxCachedPlayers = count;
            return this;
        }

        public Builder enableStatistics(boolean enable) {
            config.enableStatistics = enable;
            return this;
        }

        public PreloadConfig build() {
            return config;
        }
    }
}
/***
 * // 方式1：使用默认配置
 * PreloadConfig config = PreloadConfig.createDefault();
 * 
 * // 方式2：使用 Builder 链式配置
 * PreloadConfig config = new PreloadConfig.Builder()
 *  .forwardCount(3)          // 预加载后面 3 个视频
 *  .maxCachedPlayers(1)      // 只缓存 1 个播放器（节省内存）
 *  .preloadDurationMs(3000)  // 只预加载 3 秒
 *  .build();
 *
 * // 方式3：禁用预加载
 * PreloadConfig config = PreloadConfig.createDisabled();
 */