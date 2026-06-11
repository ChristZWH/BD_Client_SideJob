package com.example.bd_client_sidejob.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 播放器管理器
 * 负责管理播放器实例池、预加载策略和统计功能
 * 实现真正的"提前创建播放器并预缓冲数据"逻辑
 *
 * 注意：ExoPlayer必须在主线程中创建和访问！
 */
public class PlayerManager {
    private static final String TAG = "PlayerManager";
    private static final int MAX_CONCURRENT_PRELOAD = 1; // 最大并发预加载数
    private static final long PRELOAD_TIMEOUT_MS = 8000; // 预加载超时 8 秒，超时后自动释放
    private static final int MAX_CACHED_PLAYERS_LIMIT = 2; // 硬上限：最多缓存 2 个播放器（降低内存峰值）

    // 单例实例
    private static volatile PlayerManager instance;

    // 预加载配置
    private PreloadConfig config;

    // 预加载统计
    private PreloadStatistics statistics;

    // 播放器池（LRU缓存）- 存放已完成预加载的播放器
    private LinkedHashMap<String, VideoPlayerController> playerPool;

    // 预加载任务映射 - 存放正在预加载的任务（防止重复预加载）；PreloadTask内部封装了 player，因为预加载也需要player
    private Map<String, PreloadTask> preloadingMap;

    // 已预加载完成的视频ID集合
    private Map<String, Boolean> preloadedSet;

    // 线程池 - 用于异步等待预加载完成
    private ExecutorService preloadExecutor;

    // 主线程Handler - 用于切换到主线程执行播放器操作
    private Handler mainHandler;

    // 上下文
    private Context context;

    // 持有临时 SurfaceView 引用的映射（用于释放临时 Surface）
    private Map<String, SurfaceView> tempSurfaceMap;

    // 预加载任务内部类
    private static class PreloadTask {
        String videoId;
        String videoUrl;
        VideoPlayerController player;
        boolean isCancelled;
        boolean isPrepared;

        PreloadTask(String videoId, String videoUrl, VideoPlayerController player) {
            this.videoId = videoId;
            this.videoUrl = videoUrl;
            this.player = player;
            this.isCancelled = false;
            this.isPrepared = false;
        }
    }

    // 创建超时清理 Runnable对象
    private class PreloadTimeoutTask implements Runnable {
        private final String videoId;

        PreloadTimeoutTask(String videoId) {
            this.videoId = videoId;
        }

        @Override
        public void run() {
            synchronized (preloadingMap) {
                PreloadTask task = preloadingMap.get(videoId);
                if (task != null && !task.isPrepared) {
                    Log.w(TAG, "Preload timed out after " + PRELOAD_TIMEOUT_MS + "ms, cleaning up: " + videoId);
                    cancelPreload(videoId);
                }
            }
        }
    }

    private PlayerManager(Context context) {
        this.context = context.getApplicationContext();
        this.config = PreloadConfig.createDefault(); // 创建默认开启预加载的配置
        this.statistics = new PreloadStatistics();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.tempSurfaceMap = new HashMap<>();

        // LRU缓存：最近最少使用策略，自动淘汰最久未使用的播放器
        // maxCachedPlayers 必须是最终值，不能用构造时尚未设置的 config 值
        this.playerPool = new LinkedHashMap<String, VideoPlayerController>(MAX_CACHED_PLAYERS_LIMIT, 0.75f, true) {
            @SuppressWarnings("unchecked")
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                if (size() > MAX_CACHED_PLAYERS_LIMIT) {
                    VideoPlayerController player = (VideoPlayerController) eldest.getValue();
                    String videoId = (String) eldest.getKey();
                    if (player != null) {
                        // ExoPlayer.release() 必须在主线程调用
                        if (Looper.myLooper() == Looper.getMainLooper()) {
                            player.release();
                        } else {
                            mainHandler.post(player::release);
                        }
                        Log.d(TAG, "LRU evicted: " + videoId + ", pool size: " + (size() - 1));
                    }
                    // 清理临时 SurfaceView 引用
                    tempSurfaceMap.remove(videoId);
                    return true; // 淘汰
                }
                return false; // 不淘汰
            }
        };

        this.preloadingMap = new HashMap<>();
        this.preloadedSet = new HashMap<>();
        this.preloadExecutor = Executors.newSingleThreadExecutor(); // 异步任务执行器，用于后台等待预加载完成
    }

    /**
     * 获取单例实例
     */
    public static PlayerManager getInstance(Context context) {
        if (instance == null) {
            synchronized (PlayerManager.class) { // 类锁
                if (instance == null) {
                    instance = new PlayerManager(context);
                }
            }
        }
        return instance;
    }

    /**
     * 获取预加载配置
     */
    public PreloadConfig getConfig() {
        return config;
    }

    /**
     * 设置预加载配置
     */
    public void setConfig(PreloadConfig config) {
        this.config = config;
    }

    /**
     * 获取预加载统计
     */
    public PreloadStatistics getStatistics() {
        return statistics;
    }

    /**
     * 获取或创建播放器（非阻塞，主线程安全），调用方应该在主线程！
     * 优先从缓存池获取预加载完成的播放器
     * @param videoId 视频ID
     * @param surfaceView 显示视图
     * @return 播放器控制器（可能为 null，调用方需要检查）
     */
    public VideoPlayerController getPlayer(String videoId, SurfaceView surfaceView) {
        // 1. 优先从缓存池获取已预加载的播放器（秒开路径）
        synchronized (playerPool) {
            VideoPlayerController player = playerPool.remove(videoId); // 返回被移除的值；原子操作：移除并获取对象，避免先 get 再 remove 导致的并发问题
            if (player != null) {
                // 用轻量的 attachSurfaceView 换 Surface，保留已缓冲的 ExoPlayer
                // 必须在主线程调用
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    player.attachSurfaceView(surfaceView);
                } else {
                    mainHandler.post(() -> player.attachSurfaceView(surfaceView));
                }
                // 清理预加载用的临时 SurfaceView 引用（临时的 SurfaceView 不再需要）
                tempSurfaceMap.remove(videoId);
                statistics.recordCacheHit();
                Log.d(TAG, "✓ FAST PLAY - Preloaded player reused: " + videoId);
                return player;
            }
        }

        // 2. 检查是否正在预加载中（不等待，避免阻塞主线程）
        synchronized (preloadingMap) {
            PreloadTask task = preloadingMap.get(videoId);
            if (task != null) {
                Log.d(TAG, "Video is currently preloading: " + videoId + ", creating fallback player");
                // 不在主线程阻塞等待 —— 创建临时播放器作为降级方案
                // 注意：预加载完成后 playerPool 中会有一份，优先使用（见上方逻辑）
            }
        }

        // 3. 创建新播放器（降级路径：无预加载可用时）
        // 创建前检查可用内存，内存紧张时跳过创建
        if (!hasEnoughMemory()) {
            Log.w(TAG, "Insufficient memory, refusing to create new player");
            return null;
        }

        VideoPlayerController player = new VideoPlayerController();
        player.initialize(context, surfaceView);

        synchronized (playerPool) {
            playerPool.put(videoId, player);
        }
        statistics.recordCacheMiss();
        Log.d(TAG, "✗ FALLBACK - Created new player: " + videoId);
        return player;
    }

    /**
     * 预加载视频（真正的预缓冲逻辑）
     * 提前创建播放器并缓冲数据，存入 playerPool 供后续 getPlayer 快速使用
     * @param videoId 视频ID
     * @param videoUrl 视频URL
     */
    public void preloadVideo(String videoId, String videoUrl) {
        if (!config.enabled) {
            Log.d(TAG, "Preloading is disabled");
            return;
        }

        synchronized (preloadingMap) {
            // 检查是否已缓存、正在预加载中或已完成预加载
            synchronized (playerPool) {
                if (playerPool.containsKey(videoId)) {
                    Log.d(TAG, "Video already cached in pool: " + videoId);
                    return;
                }
            }
            if (preloadingMap.containsKey(videoId)) {
                Log.d(TAG, "Video already preloading: " + videoId);
                return;
            }
            if (preloadedSet.containsKey(videoId)) {
                Log.d(TAG, "Video already preloaded: " + videoId);
                return;
            }

            // 检查并发上限 一次只能预加载一个视频
            if (preloadingMap.size() >= MAX_CONCURRENT_PRELOAD) {
                Log.d(TAG, "Preload queue full (" + MAX_CONCURRENT_PRELOAD + "), skipping: " + videoId);
                return;
            }

            // 创建预加载任务
            PreloadTask task = new PreloadTask(videoId, videoUrl, null);
            preloadingMap.put(videoId, task);

            // 注册超时清理（8 秒后自动取消）
            mainHandler.postDelayed(new PreloadTimeoutTask(videoId), PRELOAD_TIMEOUT_MS);

            Log.d(TAG, "Scheduling preload for: " + videoId);

            // 在主线程创建播放器并开始预加载（ExoPlayer必须在主线程创建）
            mainHandler.post(() -> { // 将任务投递到主线程中，下面的内容回在下一轮消息循环中执行
                synchronized (preloadingMap) {
                    PreloadTask currentTask = preloadingMap.get(videoId);
                    if (currentTask == null || currentTask.isCancelled) {
                        Log.d(TAG, "Preload cancelled before start: " + videoId);
                        return;
                    }

                    // 创建前检查可用内存，紧张时跳过
                    if (!hasEnoughMemory()) {
                        Log.w(TAG, "Low memory, skipping preload for: " + videoId);
                        preloadingMap.remove(videoId);
                        preloadingMap.notifyAll();
                        return;
                    }

                    // 在主线程创建播放器
                    VideoPlayerController player = new VideoPlayerController();

                    // 创建临时 SurfaceView 用于预加载（只需 Surface 存在即可触发缓冲）
                    // 这里创建的 tempSurface 会在 getPlayer() 被调用时自动替换，
                    // 或在预加载完成超时清理时释放
                    SurfaceView tempSurface = new SurfaceView(context);
                    tempSurfaceMap.put(videoId, tempSurface);
                    player.initialize(context, tempSurface);
                    currentTask.player = player;

                    // 设置播放状态监听器，监听缓冲完成，然后设置URL
                    player.setPlayStateListener(new VideoPlayerController.PlayStateListener() {
                        @Override
                        public void onPlayStateChanged(boolean isPlaying) {}

                        @Override
                        public void onProgressChanged(long position, long duration) {}

                        @Override
                        public void onError(String errorMessage) {
                            Log.e(TAG, "Preload error: " + videoId + ", " + errorMessage);
                            synchronized (preloadingMap) {
                                preloadingMap.remove(videoId);
                                preloadingMap.notifyAll();
                            }
                            // 出错时清理所有资源（包括临时 SurfaceView）
                            cleanupPreloadPlayer(videoId, player);
                        }

                        @Override
                        public void onPrepared() { // 主线程回调onPrepared() ,preloadExecutor.execute(() -> {...}子线程中继续缓冲
                            // 视频准备完成（首帧已缓冲），让缓冲区继续填充 preloadDurationMs
                            Log.d(TAG, "✓ Preload prepared (buffering...): " + videoId);

                            // 在后台线程等待额外缓冲时间
                            preloadExecutor.execute(() -> { // 切换至后台线程继续缓冲
                                try {
                                    // 让播放器继续缓冲 config.preloadDurationMs 毫秒的数据
                                    Thread.sleep(config.preloadDurationMs);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                                
                                // 缓冲成功并且没有被取消
                                if (!currentTask.isCancelled) {
                                    synchronized (preloadingMap) {
                                        synchronized (playerPool) {
                                            // 放入池前，先检查容量并淘汰最旧项
                                            if (playerPool.size() >= MAX_CACHED_PLAYERS_LIMIT) {
                                                // 手动淘汰最旧的条目
                                                String eldestKey = playerPool.keySet().iterator().next();
                                                VideoPlayerController eldestPlayer = playerPool.remove(eldestKey);
                                                if (eldestPlayer != null) {
                                                    // ExoPlayer 必须在主线程访问，post 到主线程释放
                                                    final String finalEldestKey = eldestKey;
                                                    mainHandler.post(() -> {
                                                        eldestPlayer.release(); // 确保在主线程中释放 最旧的 player
                                                        Log.d(TAG, "Evicted eldest player (posted to main): " + finalEldestKey);
                                                    });
                                                }
                                                tempSurfaceMap.remove(eldestKey); // 预加载完成，释放临时创建的 SurfaceView
                                            }
                                            // 将预加载完成的播放器放入缓存池
                                            playerPool.put(videoId, player);
                                            preloadedSet.put(videoId, true);
                                            currentTask.isPrepared = true;
                                        }
                                        preloadingMap.remove(videoId);
                                        preloadingMap.notifyAll();
                                        Log.d(TAG, "✓ Preload completed and cached in pool: " + videoId + " (pool size: " + playerPool.size() + ")");
                                    }
                                } else {
                                    // 任务被取消，清理包括临时 SurfaceView 在内的所有资源
                                    cleanupPreloadPlayer(videoId, player);
                                    Log.d(TAG, "Preload discarded (cancelled): " + videoId);
                                }
                            });
                        }
                    });

                    // 设置视频URL并开始预加载准备（preloadMode=true: 只缓冲不播放）
                    player.setVideoUrl(videoUrl, true);
                }
            });
        }
    }

    /**
     * 清理预加载播放器相关资源（包括临时 SurfaceView）
     * 临时 SurfaceView 原本用于 ExoPlayer 预缓冲，一旦 ExoPlayer release，
     * 其内部的 Surface 引用也会释放，但 SurfaceView 本身可能泄漏，这里显式清掉
     */
    private void cleanupPreloadPlayer(String videoId, VideoPlayerController player) {
        tempSurfaceMap.remove(videoId);
        if (player != null) {
            // ExoPlayer 所有操作必须在主线程，统一 post 到主线程
            mainHandler.post(() -> {
                player.detachSurface(); // 先断开 Surface 连接
                player.release();
            });
        }
    }

    /**
     * 检查当前内存是否足够创建新播放器
     * 每个 ExoPlayer + MediaCodec 约消耗 40-80MB，这里预留至少 64MB 可用空间
     * 防止OOM异常
     */
    private boolean hasEnoughMemory() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long availableMemory = maxMemory - usedMemory;
        // 至少需要 64MB 可用堆内存才创建新播放器
        boolean enough = availableMemory > 64 * 1024 * 1024;
        if (!enough) {
            Log.w(TAG, "Memory check failed: available=" + (availableMemory / (1024 * 1024)) + "MB, max=" + (maxMemory / (1024 * 1024)) + "MB");
        }
        return enough;
    }

    /**
     * 取消预加载
     * @param videoId 视频ID
     */
    public void cancelPreload(String videoId) {
        synchronized (preloadingMap) {
            PreloadTask task = preloadingMap.get(videoId);
            if (task != null) {
                task.isCancelled = true;
                if (task.player != null) {
                    cleanupPreloadPlayer(videoId, task.player);
                }
                preloadingMap.remove(videoId);
                preloadingMap.notifyAll();
                Log.d(TAG, "Preload cancelled: " + videoId);
            }
        }
    }

    /**
     * 取消所有正在预加载的任务（窗口外清理）
     */
    public void cancelAllPreloads() {
        synchronized (preloadingMap) {
            for (String videoId : new ArrayList<>(preloadingMap.keySet())) {
                cancelPreload(videoId);
            }
        }
    }

    /**
     * 释放指定视频的播放器
     * @param videoId 视频ID
     */
    public void releasePlayer(String videoId) {
        synchronized (playerPool) {
            VideoPlayerController player = playerPool.remove(videoId);
            if (player != null) {
                player.release();
                Log.d(TAG, "Player released: " + videoId);
            }
        }
        preloadedSet.remove(videoId);
        tempSurfaceMap.remove(videoId);
    }

    /**
     * 释放所有播放器资源（调用方确保在主线程调用）
     */
    public void releaseAll() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.w(TAG, "releaseAll called from non-main thread, posting to main");
            mainHandler.post(this::releaseAll);  // 确保在主线程！（推到主线程）
            return;
        }

        synchronized (playerPool) {
            for (VideoPlayerController player : playerPool.values()) {
                player.release();
            }
            playerPool.clear();
        }

        synchronized (preloadingMap) {
            for (PreloadTask task : preloadingMap.values()) {
                task.isCancelled = true;
                if (task.player != null) {
                    task.player.detachSurface(); // 因为在预加载的时候需要 tempSerface，当真正播放的时候使用用户提供的Serface
                    task.player.release();
                }
            }
            preloadingMap.clear();
            preloadingMap.notifyAll();
        }

        preloadedSet.clear();
        tempSurfaceMap.clear();
        preloadExecutor.shutdown(); // 停止接收新任务
        try {
            if (!preloadExecutor.awaitTermination(1, TimeUnit.SECONDS)) { //等待一秒
                preloadExecutor.shutdownNow(); // 中断所有正在执行的任务
            }
        } catch (InterruptedException e) { // 等待过程中被中断，则强制关闭
            preloadExecutor.shutdownNow();
        }
        Log.d(TAG, "All players released");
    }

    /**
     * 获取缓存池中播放器数量
     */
    public int getCachedPlayerCount() {
        synchronized (playerPool) {
            return playerPool.size();
        }
    }

    /**
     * 获取正在预加载的任务数量
     */
    public int getPreloadingCount() {
        synchronized (preloadingMap) {
            return preloadingMap.size();
        }
    }

    /**
     * 判断视频是否已缓存（在播放器池中）
     */
    public boolean isCached(String videoId) {
        synchronized (playerPool) {
            return playerPool.containsKey(videoId);
        }
    }

    /**
     * 判断视频是否正在预加载
     */
    public boolean isPreloading(String videoId) {
        synchronized (preloadingMap) {
            return preloadingMap.containsKey(videoId);
        }
    }
}
