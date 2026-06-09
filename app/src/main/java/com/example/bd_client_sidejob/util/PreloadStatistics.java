package com.example.bd_client_sidejob.util;

import android.util.Log;

/**
 * 预加载统计类
 * 用于记录和计算视频起播优化效果
 */
public class PreloadStatistics {
    private static final String TAG = "PreloadStatistics";

    // 优化前数据（首次播放，无预加载）
    public long baselineStartupDelayMs;
    
    // 优化后数据（有预加载）
    public long optimizedStartupDelayMs;
    
    // 累计统计
    public int totalPlayCount;
    public long totalSavedTimeMs;
    public int cacheHits;
    public int cacheMisses;

    /**
     * 记录基准起播延迟（首次播放，无预加载状态）
     */
    public synchronized void recordBaselineDelay(long delayMs) {
        if (baselineStartupDelayMs == 0) {
            baselineStartupDelayMs = delayMs;
            Log.d(TAG, "Baseline delay recorded: " + delayMs + "ms");
        }
    }

    /**
     * 记录优化后起播延迟
     */
    public synchronized void recordOptimizedDelay(long delayMs) {
        optimizedStartupDelayMs = delayMs;
        totalPlayCount++;
        
        // 计算节省的时间
        if (baselineStartupDelayMs > 0) {
            long saved = baselineStartupDelayMs - delayMs;
            if (saved > 0) {
                totalSavedTimeMs += saved;
            }
        }
        
        Log.d(TAG, "Optimized delay recorded: " + delayMs + "ms, total plays: " + totalPlayCount);
    }

    /**
     * 记录缓存命中
     */
    public synchronized void recordCacheHit() {
        cacheHits++;
        Log.d(TAG, "Cache hit, total hits: " + cacheHits);
    }

    /**
     * 记录缓存未命中
     */
    public synchronized void recordCacheMiss() {
        cacheMisses++;
        Log.d(TAG, "Cache miss, total misses: " + cacheMisses);
    }

    /**
     * 计算优化幅度（百分比）
     */
    public synchronized double getOptimizationRatio() {
        if (baselineStartupDelayMs == 0) return 0;
        if (optimizedStartupDelayMs == 0) return 0;
        
        double ratio = (baselineStartupDelayMs - optimizedStartupDelayMs) * 100.0 / baselineStartupDelayMs;
        return Math.max(0, ratio); // 避免出现负数
    }

    /**
     * 计算缓冲命中率
     */
    public synchronized double getCacheHitRate() {
        int total = cacheHits + cacheMisses;
        if (total == 0) return 0;
        return cacheHits * 100.0 / total;
    }

    /**
     * 获取平均节省时间
     */
    public synchronized long getAverageSavedTimeMs() {
        if (totalPlayCount == 0) return 0;
        return totalSavedTimeMs / totalPlayCount;
    }

    /**
     * 输出统计报告到日志
     */
    public void printReport() {
        StringBuilder report = new StringBuilder();
        report.append("\n┌─────────────────────────────────────┐\n");
        report.append("│      Preload Statistics Report      │\n");
        report.append("├─────────────────────────────────────┤\n");
        report.append(String.format("│ Baseline Delay:     %6d ms   │\n", baselineStartupDelayMs));
        report.append(String.format("│ Optimized Delay:    %6d ms   │\n", optimizedStartupDelayMs));
        report.append(String.format("│ Optimization Ratio:  %6.1f%%   │\n", getOptimizationRatio()));
        report.append(String.format("│ Total Play Count:     %3d      │\n", totalPlayCount));
        report.append(String.format("│ Total Saved Time:  %6d ms   │\n", totalSavedTimeMs));
        report.append(String.format("│ Avg Saved Time:    %6d ms   │\n", getAverageSavedTimeMs()));
        report.append(String.format("│ Cache Hit Rate:     %6.1f%%   │\n", getCacheHitRate()));
        report.append("└─────────────────────────────────────┘\n");
        
        Log.d(TAG, report.toString());
    }

    /**
     * 重置统计数据
     */
    public synchronized void reset() {
        baselineStartupDelayMs = 0;
        optimizedStartupDelayMs = 0;
        totalPlayCount = 0;
        totalSavedTimeMs = 0;
        cacheHits = 0;
        cacheMisses = 0;
        Log.d(TAG, "Statistics reset");
    }
}