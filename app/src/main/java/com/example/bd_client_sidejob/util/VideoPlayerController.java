package com.example.bd_client_sidejob.util;

import android.content.Context;
import android.net.Uri;
import android.view.SurfaceView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

/**
 * 视频播放器控制器
 * 基于 ExoPlayer 封装，提供视频播放、暂停、进度控制等功能
 */
public class VideoPlayerController {

    /** ExoPlayer 是控制视频播放的核心引擎，负责解码、播放、暂停、进度控制等底层功能 */
    private ExoPlayer player;
    /** 视频显示的 SurfaceView 是视频组件，ExoPlayer 将解码后的视频帧渲染到这个 Surface 上*/
    private SurfaceView surfaceView;
    /** VideoPlayerController 内部持有 ———— 播放状态监听器 */
    private PlayStateListener playStateListener;
    /** 当前播放位置 */
    private long currentPosition = 0;
    /** 视频是否已准备好 */
    private boolean isPrepared = false;

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
     * 初始化播放器
     * @param context 上下文
     * @param surfaceView 视频显示的 SurfaceView
     */
    public void initialize(Context context, SurfaceView surfaceView) {
        this.surfaceView = surfaceView;

        // 创建 ExoPlayer 实例
        player = new ExoPlayer.Builder(context).build();
        // 设置视频输出 Surface，将播放器与显示视图绑定
        player.setVideoSurfaceView(surfaceView);

        // 添加播放器监听器
        player.addListener(new Player.Listener() { // ExoPlayer的系统回调
            @Override
            public void onIsPlayingChanged(boolean isPlaying) { // 播放/暂停状态变化时（系统触发，VideoPlayerController 收到的具体回调，通知外部播放状态变化）
                android.util.Log.d("VideoPlayer", "onIsPlayingChanged: " + isPlaying);
                if (playStateListener != null) {
                    playStateListener.onPlayStateChanged(isPlaying); // 转发给自定义回调
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) { // 播放状态（准备中/就绪/播放中/结束）变化时（系统触发，VideoPlayerController 收到的具体回调，处理不同播放状态下的逻辑）
                android.util.Log.d("VideoPlayer", "onPlaybackStateChanged: " + playbackState);
                
                // 视频解析完之后，会触发更新UI状态
                if (playbackState == Player.STATE_READY && !isPrepared) {
                    isPrepared = true;
                    android.util.Log.d("VideoPlayer", "Video prepared, duration: " + player.getDuration());
                    if (playStateListener != null) {
                        playStateListener.onPrepared(); // 转发给外部回调（onPrepared() 被调用后，在 VideoPlayerView 中的处理）
                    }
                } 
                // 视频缓冲中时，会触发onPlaybackStateChanged回调，更新UI状态为缓冲中
                else if (playbackState == Player.STATE_BUFFERING) {
                    android.util.Log.d("VideoPlayer", "Video buffering...");
                    // 后续根据业务需求继续扩展
                }
                // 视频播放结束时，会触发onPlaybackStateChanged回调，更新UI状态为结束
                else if (playbackState == Player.STATE_ENDED) {
                    android.util.Log.d("VideoPlayer", "Video ended");
                } 
                // 视频空闲时，会触发onPlaybackStateChanged回调，更新UI状态为空空闲
                else if (playbackState == Player.STATE_IDLE) {
                    android.util.Log.d("VideoPlayer", "Video idle");
                }
            }

            @Override
            public void onPlayerError(androidx.media3.common.PlaybackException error) { // 播放出错时（系统触发，VideoPlayerController 收到的具体回调，通知外部错误信息处理）
                android.util.Log.e("VideoPlayer", "Player error: " + error.getMessage(), error);
                if (playStateListener != null) {
                    playStateListener.onError(error.getMessage()); // 转发给自定义回调
                }
            }
        });
    }

    /**
     * 设置视频 URL 并开始准备
     * @param url 视频地址
     */
    public void setVideoUrl(String url) {
        if (player == null) {
            android.util.Log.e("VideoPlayer", "Player is null, cannot set video URL");
            return;
        }
        android.util.Log.d("VideoPlayer", "Setting video URL: " + url);
        
        // 重置状态 清除上一个状态，避免混淆
        isPrepared = false;
        currentPosition = 0;
        
        // 创建媒体项 是ExoPayer的固定流程，封装了视频的URL、标题、字幕等信息；ExoPlayer需要通过MediaItem来了解要播什么内容
        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
        player.setMediaItem(mediaItem); // 设置视频资源
        // 开始准备视频（异步操作），开始视频解析
        // 下载视频元数据（时长、分辨率），初始化解码器，缓冲初始数据
        // 准备完成之后，会触发onPlaybackStateChanged回调，更新UI状态
        player.prepare();
        android.util.Log.d("VideoPlayer", "Video prepare called");
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
     * 释放播放器资源
     */
    public void release() {
        if (player != null) {
            currentPosition = player.getCurrentPosition();
            player.release();
            player = null;
        }
        isPrepared = false;
    }

    /**
     * 设置播放状态监听器
     * @param listener 监听器
     */
    public void setPlayStateListener(PlayStateListener listener) {
        this.playStateListener = listener;
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
     * 每秒更新一次播放进度
     */
    public void startProgressUpdate() {
        if (player == null) return;

        new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                if (player != null && isPlaying() && playStateListener != null) {
                    playStateListener.onProgressChanged(getCurrentPosition(), getDuration());
                }
                if (player != null) {
                    android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
                    handler.postDelayed(this, 1000);
                }
            }
        });
    }

    /**
     * 停止进度更新
     */
    public void stopProgressUpdate() {
        // Handler会自动停止，因为player为null时不会继续post
    }
}