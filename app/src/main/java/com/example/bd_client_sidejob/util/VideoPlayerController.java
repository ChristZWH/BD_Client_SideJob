package com.example.bd_client_sidejob.util;

import android.content.Context;
import android.net.Uri;
import android.view.SurfaceView;

import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

/**
 * 视频播放器控制器
 * 基于 ExoPlayer 封装，提供视频播放、暂停、进度控制、清晰度切换等功能
 * 支持视频缓存，实现秒开效果
 */
public class VideoPlayerController {

    // ==================== 清晰度常量 ====================

    /** 清晰度：360p */
    public static final int QUALITY_360P = 360;
    /** 清晰度：720p */
    public static final int QUALITY_720P = 720;
    /** 默认清晰度 */
    public static final int QUALITY_DEFAULT = QUALITY_720P;

    // ==================== 成员变量 ====================

    /** ExoPlayer 是控制视频播放的核心引擎，负责解码、播放、暂停、进度控制等底层功能 */
    private ExoPlayer player;
    /** 视频显示的 SurfaceView 是视频组件，ExoPlayer 将解码后的视频帧渲染到这个 Surface 上*/
    private SurfaceView surfaceView;
    /** VideoPlayerController 内部持有 ———— 播放状态监听器 */
    private PlayStateListener playStateListener;
    /** 清晰度切换完成监听器 */
    private QualityChangeListener qualityChangeListener;
    /** 当前播放位置 */
    private long currentPosition = 0;
    /** 视频是否已准备好 */
    private boolean isPrepared = false;
    /** 是否使用缓存 */
    private boolean useCache = true;  // 启用 ExoPlayer 磁盘缓存，实现跨 session 数据复用
    /** 当前加载的视频 URL（用于避免重复 prepare） */
    private String currentUrl;
    /** 当前清晰度（默认720p） */
    private int currentQuality = QUALITY_DEFAULT;
    /** 是否正在进行清晰度切换（用于在持久监听器中区分切换场景） */
    private boolean isQualitySwitching = false;
    /** 是否是进行中的播放（非预加载），用于区分 URL 失败的清理策略（用户看没看到） */
    private boolean isActivePlayback = false;
    /** 进度更新 Handler */
    private android.os.Handler progressHandler;
    /** 进度更新 Runnable */
    private Runnable progressRunnable;

    /**
     * 播放状态监听器接口————专门为 VideoPlayerController 设计的回调接口
     * 用于回调播放状态变化、进度更新、错误信息和准备完成
     */
    public interface PlayStateListener {
        /**
         * 播放状态变化回调
         * @param isPlaying 是否正在播放
         */
        void onPlayStateChanged(boolean isPlaying);

        /**
         * 播放进度更新回调
         * @param position 当前播放位置（毫秒）
         * @param duration 视频总时长（毫秒）
         */
        void onProgressChanged(long position, long duration);

        /**
         * 播放错误回调
         * @param errorMessage 错误信息
         */
        void onError(String errorMessage);

        /**
         * 视频准备完成回调
         */
        void onPrepared();
    }

    /**
     * 清晰度切换监听器接口
     * 用于回傳清晰度切换的结果
     */
    public interface QualityChangeListener {
        /**
         * 清晰度切换成功回调
         * @param newQuality 切换后的清晰度（QUALITY_360P 或 QUALITY_720P）
         */
        void onQualityChanged(int newQuality);

        /**
         * 清晰度切换失败回调
         * @param quality 尝试切换的目标清晰度
         * @param errorMessage 错误信息
         */
        void onQualityChangeError(int quality, String errorMessage);
    }

    /**
     * 初始化播放器（首次创建时调用或完全重建立）
     * 如果已有 ExoPlayer 实例，只换 SurfaceView，不重建播放器（保留已缓冲的数据）
     * @param context 上下文
     * @param surfaceView 视频显示的 SurfaceView
     */
    @OptIn(markerClass = UnstableApi.class) public void initialize(Context context, SurfaceView surfaceView) {
        this.surfaceView = surfaceView;

        // 如果已经有播放器实例，只换 Surface，不重建（保留预加载缓冲成果）
        if (player != null) {
            player.setVideoSurfaceView(surfaceView);
            android.util.Log.d("VideoPlayer", "SurfaceView updated, player reused (no rebuild)");
            return;
        }

        ExoPlayer.Builder playerBuilder = new ExoPlayer.Builder(context);

        // 启用缓存数据源（磁盘缓存 + 内存缓冲）
        if (useCache) {
            DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(
                    CacheManager.getInstance(context).getCacheDataSourceFactory()
            );
            playerBuilder.setMediaSourceFactory(mediaSourceFactory);
            android.util.Log.d("VideoPlayer", "Cache enabled");
        }

        // 创建 ExoPlayer 实例
        player = playerBuilder.build();
        // 设置视频输出 Surface，将播放器与显示视图绑定
        player.setVideoSurfaceView(surfaceView);

        // 添加播放器监听器（只添加一次）；是系统回调，由系统调用
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                android.util.Log.d("VideoPlayer", "onIsPlayingChanged: " + isPlaying);
                if (playStateListener != null) {
                    playStateListener.onPlayStateChanged(isPlaying);
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                android.util.Log.d("VideoPlayer", "onPlaybackStateChanged: " + playbackState);

                if (playbackState == Player.STATE_READY && !isPrepared) {
                    // 清晰度切换期间，不在此处设置 isPrepared——交给 switchQuality 的一次性监听器处理
                    if (isQualitySwitching) {
                        android.util.Log.d("VideoPlayer", "Quality switching in progress, deferring onPrepared notification");
                        return;
                    }
                    isPrepared = true;
                    android.util.Log.d("VideoPlayer", "Video prepared, duration: " + player.getDuration());
                    if (playStateListener != null) {
                        playStateListener.onPrepared();
                    }
                } else if (playbackState == Player.STATE_BUFFERING) {
                    android.util.Log.d("VideoPlayer", "Video buffering...");
                } else if (playbackState == Player.STATE_ENDED) {
                    android.util.Log.d("VideoPlayer", "Video ended");
                } else if (playbackState == Player.STATE_IDLE) {
                    android.util.Log.d("VideoPlayer", "Video idle");
                }
            }

            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) {
                android.util.Log.e("VideoPlayer", "Player error: " + error.getMessage(), error);
                if (playStateListener != null) {
                    playStateListener.onError(error.getMessage());
                }
            }
        });
    }

    /**
     * 轻量换 SurfaceView（不重建 ExoPlayer，保留已缓冲的数据）
     * 用于预加载播放器复用时，从 temp SurfaceView 切换到真正的 SurfaceView
     * @param surfaceView 新的显示视图
     */
    public void attachSurfaceView(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
        if (player != null) {
            player.setVideoSurfaceView(surfaceView);
        }
    }

    /**
     * 断开当前 SurfaceView 连接（不释放播放器和缓冲数据）
     * 必须给每个即将被复用的 SurfaceView 调用此方法，否则两个解码器会争抢同一个 Surface 导致崩溃
     * 线程安全：自动 post 到主线程
     */
    public void detachSurface() {
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            android.util.Log.w("VideoPlayer", "detachSurface() called from non-main thread, posting to main");
            new android.os.Handler(android.os.Looper.getMainLooper()).post(this::detachSurface);
            return;
        }
        if (player != null) {
            // 内部会操作UI 所以需要在主线程调用
            player.setVideoSurfaceView(null);
        }
        this.surfaceView = null;
    }

    /**
     * 设置视频 URL 并开始准备；默认不预加载
     * 如果 URL 与当前已缓冲的相同，则跳过准备（避免重复 prepare）
     * @param url 视频地址
     */
    public void setVideoUrl(String url) {
        setVideoUrl(url, false);
    }

    /**
     * 设置视频 URL 并开始准备，可选 preloadMode
     * @param url 视频地址
     * @param preloadMode 是否预加载（只缓冲不播放）
     */
    public void setVideoUrl(String url, boolean preloadMode) {
        if (player == null) {
            android.util.Log.e("VideoPlayer", "Player is null, cannot set video URL");
            return;
        }

        if (url != null && url.equals(currentUrl) && isPrepared) {
            android.util.Log.d("VideoPlayer", "Video already prepared for URL, skipping: " + url);
            if (playStateListener != null) {
                // 直接回调 onPrepared 通知外部准备完成，不再重复下载/解析
                playStateListener.onPrepared();
            }
            return;
        }

        android.util.Log.d("VideoPlayer", "Setting video URL (preload=" + preloadMode + "): " + url);

        this.currentUrl = url;
        isPrepared = false;
        currentPosition = 0;
        isActivePlayback = !preloadMode;  // 只有非预加载才是活跃播放

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
        player.setMediaItem(mediaItem);
        player.prepare();

        if (preloadMode) {
            // 预加载模式：不自动播放
            player.setPlayWhenReady(false);
            android.util.Log.d("VideoPlayer", "Preload mode enabled");
        } else {
            // 非预加载模式：准备好后自动播放
            player.setPlayWhenReady(true);
        }
    }

    /**
     * 取消当前 prepare（用于超时自动清理）
     */
    public void abortPrepare() {
        if (player != null) {
            player.stop();
            player.clearMediaItems();
        }
        isPrepared = false;
        currentUrl = null;
    }

    /**
     * 开始播放
     */
    public void play() {
        if (player != null) {
            android.util.Log.d("VideoPlayer", "Playing video - prepared: " + isPrepared);
            player.play();
        } else {
            android.util.Log.w("VideoPlayer", "Cannot play - player is null");
        }
    }

    /**
     * 暂停播放
     */
    public void pause() {
        if (player != null) {
            player.pause();
        }
    }

    /**
     * 跳转到指定位置（实现进度条拖动）
     * @param position 目标位置（毫秒）
     */
    public void seekTo(long position) {
        if (player != null) {
            player.seekTo(position);
        }
    }

    /**
     * 释放播放器资源，确保 MediaCodec 和原生内存完全释放
     * ExoPlayer release 会触发 MediaCodec.release()，释放原生解码器缓冲区
     * 线程安全：如果非主线程调用，自动 post 到主线程执行
     */
    public void release() {
        // ExoPlayer 所有操作必须在主线程执行
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            android.util.Log.w("VideoPlayer", "release() called from non-main thread, posting to main");
            new android.os.Handler(android.os.Looper.getMainLooper()).post(this::release);
            return;
        }

        stopProgressUpdate();
        if (player != null) {
            currentPosition = player.getCurrentPosition();
            // 1. 先停止播放，让 ExoPlayer 停止向 MediaCodec 喂数据
            player.stop();
            // 2. 清除 MediaItem（释放对视频源的引用）
            player.clearMediaItems();
            // 3. 断开 SurfaceView 连接（释放渲染目标，解除对 Surface 的原生引用）
            player.setVideoSurfaceView(null);
            // 4. 最终释放 ExoPlayer（触发 MediaCodec.release() 释放原生内存）
            player.release();
            player = null;
        }
        isPrepared = false;
        currentUrl = null;
        isQualitySwitching = false;
        surfaceView = null;
        // 不置 null playStateListener / qualityChangeListener —— 它们是外部引用，由外部管理
    }

    /**
     * 设置播放状态监听器
     * @param listener 监听器
     */
    public void setPlayStateListener(PlayStateListener listener) {
        this.playStateListener = listener;
    }

    /**
     * 设置清晰度切换监听器
     * @param listener 清晰度切换监听器
     */
    public void setQualityChangeListener(QualityChangeListener listener) {
        this.qualityChangeListener = listener;
    }

    /**
     * 获取当前清晰度
     * @return 当前清晰度（QUALITY_360P 或 QUALITY_720P）
     */
    public int getCurrentQuality() {
        return currentQuality;
    }

    /**
     * 切换清晰度（保持播放进度并继续播放）
     *
     * 核心流程：
     * 1. 保存当前播放位置和播放状态
     * 2. 设置 isQualitySwitching 标志（阻止持久监听器重复设置 isPrepared）
     * 3. 切换视频源 URL 重新 prepare
     * 4. 准备完成后：seekTo 恢复播放位置 → 恢复播放状态 → 恢复标志位
     *
     * @param quality  目标清晰度（QUALITY_360P 或 QUALITY_720P）
     * @param url360p  360p 视频地址
     * @param url720p  720p 视频地址
     */
    public void switchQuality(int quality, String url360p, String url720p) {
        if (player == null) {
            android.util.Log.e("VideoPlayer", "Cannot switch quality: player is null");
            if (qualityChangeListener != null) {
                qualityChangeListener.onQualityChangeError(quality, "播放器未初始化");
            }
            return;
        }

        // 同清晰度不重复切换
        if (quality == currentQuality) {
            android.util.Log.d("VideoPlayer", "Already at quality " + quality + "p, skipping switch");
            return;
        }

        // 确定目标 URL
        String targetUrl = null;
        if (quality == QUALITY_360P && url360p != null) {
            targetUrl = url360p;
        } else if (quality == QUALITY_720P && url720p != null) {
            targetUrl = url720p;
        }

        if (targetUrl == null) {
            android.util.Log.e("VideoPlayer", "Cannot switch quality: URL for " + quality + "p is null");
            if (qualityChangeListener != null) {
                qualityChangeListener.onQualityChangeError(quality, quality + "p 视频源不可用");
            }
            return;
        }

        android.util.Log.d("VideoPlayer", "Switching quality from " + currentQuality + "p to " + quality + "p");

        // 1. 保存当前播放位置和播放状态
        long savedPosition = player.getCurrentPosition();
        boolean wasPlaying = player.isPlaying();
        android.util.Log.d("VideoPlayer", "Saving position: " + savedPosition + "ms, wasPlaying: " + wasPlaying);

        // 2. 设置切换锁 + 切换视频源
        isQualitySwitching = true;
        currentQuality = quality;
        isPrepared = false;
        this.currentUrl = targetUrl;

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(targetUrl));
        player.setMediaItem(mediaItem);
        player.prepare();

        // 3. 一次性监听器 —— 准备完成后恢复进度和播放状态
        final long finalSavedPosition = savedPosition;
        final boolean finalWasPlaying = wasPlaying;
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY && !isPrepared) {
                    isPrepared = true;
                    isQualitySwitching = false; // 解锁，恢复持久监听器的正常工作

                    // 恢复播放进度（clamp 到新视频时长以内）
                    long newDuration = player.getDuration();
                    long seekTo = Math.min(finalSavedPosition, newDuration);
                    if (seekTo > 0) {
                        player.seekTo(seekTo);
                        android.util.Log.d("VideoPlayer",
                                "Seeking to saved position: " + seekTo + "ms / " + newDuration + "ms");
                    }

                    // 恢复播放状态（之前正在播放则立即继续播放）
                    if (finalWasPlaying) {
                        player.setPlayWhenReady(true);
                        player.play();
                    } else {
                        player.setPlayWhenReady(false);
                    }

                    // 通知监听器
                    if (qualityChangeListener != null) {
                        qualityChangeListener.onQualityChanged(quality);
                    }
                    if (playStateListener != null) {
                        playStateListener.onPrepared();
                    }

                    // 移除自身，避免重复触发，实现一次性监听
                    player.removeListener(this);

                    android.util.Log.d("VideoPlayer",
                            "Quality switched to " + quality + "p, position restored to " + seekTo + "ms");
                }
            }

            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) {
                isQualitySwitching = false; // 解锁
                player.removeListener(this);

                android.util.Log.e("VideoPlayer", "Quality switch failed: " + error.getMessage());
                if (qualityChangeListener != null) {
                    qualityChangeListener.onQualityChangeError(quality, "清晰度切换失败: " + error.getMessage());
                }
            }
        });
    }

    /**
     * 判断是否正在播放
     * @return true 表示正在播放
     */
    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    /**
     * 获取当前播放位置
     * @return 当前位置（毫秒）
     */
    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }

    /**
     * 获取视频总时长
     * @return 时长（毫秒），未准备好返回 0
     */
    public long getDuration() {
        return player != null && isPrepared ? player.getDuration() : 0;
    }

    /**
     * 设置音量
     * @param volume 音量值（0.0f - 1.0f）
     */
    public void setVolume(float volume) {
        if (player != null) {
            player.setVolume(volume);
        }
    }

    /**
     * 设置播放速度
     * @param speed 播放速度（1.0f 为正常速度）
     */
    public void setPlaybackSpeed(float speed) {
        if (player != null) {
            player.setPlaybackSpeed(speed);
        }
    }

    /**
     * 开始进度更新
     * 每秒更新一次播放进度，复用 Handler 实例
     */
    public void startProgressUpdate() {
        if (player == null) return;

        // 复用 Handler 和 Runnable，避免重复创建
        if (progressHandler == null) {
            progressHandler = new android.os.Handler(android.os.Looper.getMainLooper());
        }
        stopProgressUpdate();

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (player != null && isPlaying() && playStateListener != null) {
                    playStateListener.onProgressChanged(getCurrentPosition(), getDuration()); // 回调通知 UI 更新
                }
                if (player != null && progressHandler != null) {
                    progressHandler.postDelayed(this, 1000);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    /**
     * 停止进度更新（清理 Handler 回调，防止内存泄漏）
     */
    public void stopProgressUpdate() {
        if (progressHandler != null && progressRunnable != null) {
            progressHandler.removeCallbacks(progressRunnable);
        }
        progressRunnable = null;
    }
}
