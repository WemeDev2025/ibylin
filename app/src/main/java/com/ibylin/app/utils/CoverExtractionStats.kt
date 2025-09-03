package com.ibylin.app.utils

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 封面解析统计工具
 * 用于收集和分析封面检测的成功率、性能等数据
 */
object CoverExtractionStats {
    
    private const val TAG = "CoverExtractionStats"
    
    // 统计数据
    private val methodSuccessCount = ConcurrentHashMap<String, AtomicInteger>()
    private val methodFailureCount = ConcurrentHashMap<String, AtomicInteger>()
    private val methodPerformance = ConcurrentHashMap<String, MutableList<Long>>()
    private val totalExtractions = AtomicInteger(0)
    private val successfulExtractions = AtomicInteger(0)
    
    /**
     * 记录封面检测结果
     */
    fun recordResult(coverResult: CoverResult, extractionTimeMs: Long) {
        totalExtractions.incrementAndGet()
        
        if (coverResult.isSuccess) {
            successfulExtractions.incrementAndGet()
            
            val methodName = coverResult.method?.description ?: "未知方法"
            methodSuccessCount.computeIfAbsent(methodName) { AtomicInteger(0) }.incrementAndGet()
            
            Log.d(TAG, "封面检测成功: $methodName, 耗时: ${extractionTimeMs}ms")
        } else {
            val methodName = coverResult.method?.description ?: "未知方法"
            methodFailureCount.computeIfAbsent(methodName) { AtomicInteger(0) }.incrementAndGet()
            
            Log.w(TAG, "封面检测失败: $methodName, 错误: ${coverResult.errorMessage}")
        }
        
        // 记录性能数据
        val methodName = coverResult.method?.description ?: "未知方法"
        methodPerformance.computeIfAbsent(methodName) { mutableListOf() }.add(extractionTimeMs)
    }
    
    /**
     * 获取统计报告
     */
    fun getStatsReport(): String {
        val total = totalExtractions.get()
        val successful = successfulExtractions.get()
        val successRate = if (total > 0) (successful.toDouble() / total * 100) else 0.0
        
        val report = StringBuilder()
        report.append("=== 封面解析统计报告 ===\n")
        report.append("总检测次数: $total\n")
        report.append("成功次数: $successful\n")
        report.append("成功率: ${String.format("%.2f", successRate)}%\n\n")
        
        report.append("各方法成功率:\n")
        val allMethods = (methodSuccessCount.keys + methodFailureCount.keys).distinct()
        
        for (method in allMethods.sorted()) {
            val successCount = methodSuccessCount[method]?.get() ?: 0
            val failureCount = methodFailureCount[method]?.get() ?: 0
            val totalCount = successCount + failureCount
            
            if (totalCount > 0) {
                val methodSuccessRate = (successCount.toDouble() / totalCount * 100)
                val avgTime = getAveragePerformance(method)
                
                report.append("$method:\n")
                report.append("  成功: $successCount, 失败: $failureCount\n")
                report.append("  成功率: ${String.format("%.2f", methodSuccessRate)}%\n")
                report.append("  平均耗时: ${String.format("%.2f", avgTime)}ms\n\n")
            }
        }
        
        return report.toString()
    }
    
    /**
     * 获取平均性能
     */
    private fun getAveragePerformance(method: String): Double {
        val times = methodPerformance[method] ?: return 0.0
        return if (times.isNotEmpty()) times.average() else 0.0
    }
    
    /**
     * 获取最佳检测方法
     */
    fun getBestMethod(): String? {
        var bestMethod: String? = null
        var bestSuccessRate = 0.0
        
        val allMethods = (methodSuccessCount.keys + methodFailureCount.keys).distinct()
        
        for (method in allMethods) {
            val successCount = methodSuccessCount[method]?.get() ?: 0
            val failureCount = methodFailureCount[method]?.get() ?: 0
            val totalCount = successCount + failureCount
            
            if (totalCount > 0) {
                val successRate = successCount.toDouble() / totalCount
                if (successRate > bestSuccessRate) {
                    bestSuccessRate = successRate
                    bestMethod = method
                }
            }
        }
        
        return bestMethod
    }
    
    /**
     * 获取性能最佳的方法
     */
    fun getFastestMethod(): String? {
        var fastestMethod: String? = null
        var fastestTime = Double.MAX_VALUE
        
        for ((method, times) in methodPerformance) {
            if (times.isNotEmpty()) {
                val avgTime = times.average()
                if (avgTime < fastestTime) {
                    fastestTime = avgTime
                    fastestMethod = method
                }
            }
        }
        
        return fastestMethod
    }
    
    /**
     * 重置统计数据
     */
    fun resetStats() {
        methodSuccessCount.clear()
        methodFailureCount.clear()
        methodPerformance.clear()
        totalExtractions.set(0)
        successfulExtractions.set(0)
        Log.d(TAG, "统计数据已重置")
    }
    
    /**
     * 打印统计报告到日志
     */
    fun logStatsReport() {
        Log.i(TAG, getStatsReport())
    }
}
