package com.ibylin.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.ibylin.app.utils.BookFile
import org.json.JSONObject
import java.io.File

/**
 * 阅读进度管理器
 * 负责记录和获取用户的阅读进度信息
 */
object ReadingProgressManager {
    
    private const val TAG = "ReadingProgressManager"
    private const val PREFS_NAME = "reading_progress"
    private const val KEY_LAST_READ_BOOK = "last_read_book"
    private const val KEY_READING_POSITION = "reading_position"
    private const val KEY_LAST_READ_TIME = "last_read_time"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 记录阅读进度
     */
    fun recordReadingProgress(
        context: Context,
        bookPath: String,
        position: Double = 0.0,
        timestamp: Long = System.currentTimeMillis()
    ) {
        try {
            val prefs = getPrefs(context)
            val bookFile = File(bookPath)
            
            // 记录最后阅读的图书信息
            val bookInfo = JSONObject().apply {
                put("path", bookPath)
                put("name", bookFile.name)
                put("lastReadTime", timestamp)
                put("position", position)
            }
            
            prefs.edit()
                .putString(KEY_LAST_READ_BOOK, bookInfo.toString())
                .putLong(KEY_LAST_READ_TIME, timestamp)
                .apply()
            
            Log.d(TAG, "阅读进度已记录: ${bookFile.name}, 位置: $position")
            
        } catch (e: Exception) {
            Log.e(TAG, "记录阅读进度失败", e)
        }
    }
    
    /**
     * 获取最后阅读的图书信息
     */
    fun getLastReadBook(context: Context): LastReadBook? {
        return try {
            val prefs = getPrefs(context)
            val bookInfoJson = prefs.getString(KEY_LAST_READ_BOOK, null)
            
            if (bookInfoJson != null) {
                val bookInfo = JSONObject(bookInfoJson)
                val bookPath = bookInfo.getString("path")
                val bookName = bookInfo.getString("name")
                val lastReadTime = bookInfo.getLong("lastReadTime")
                val position = bookInfo.getDouble("position")
                
                // 检查文件是否仍然存在
                val bookFile = File(bookPath)
                if (bookFile.exists()) {
                    LastReadBook(
                        path = bookPath,
                        name = bookName,
                        lastReadTime = lastReadTime,
                        position = position
                    )
                } else {
                    Log.w(TAG, "最后阅读的图书文件不存在: $bookPath")
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取最后阅读图书信息失败", e)
            null
        }
    }
    
    /**
     * 检查是否有阅读记录
     */
    fun hasReadingHistory(context: Context): Boolean {
        return getLastReadBook(context) != null
    }
    
    /**
     * 清除阅读记录
     */
    fun clearReadingHistory(context: Context) {
        try {
            val prefs = getPrefs(context)
            prefs.edit().clear().apply()
            Log.d(TAG, "阅读记录已清除")
        } catch (e: Exception) {
            Log.e(TAG, "清除阅读记录失败", e)
        }
    }
    
    /**
     * 获取阅读统计信息
     */
    fun getReadingStats(context: Context): ReadingStats {
        return try {
            val prefs = getPrefs(context)
            val lastReadTime = prefs.getLong(KEY_LAST_READ_TIME, 0)
            
            ReadingStats(
                hasHistory = lastReadTime > 0,
                lastReadTime = lastReadTime
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取阅读统计信息失败", e)
            ReadingStats(hasHistory = false, lastReadTime = 0)
        }
    }
}

/**
 * 最后阅读的图书信息
 */
data class LastReadBook(
    val path: String,
    val name: String,
    val lastReadTime: Long,
    val position: Double
) {
    fun getFormattedLastReadTime(): String {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - lastReadTime
        
        return when {
            diff < 60 * 1000 -> "刚刚"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
            else -> {
                val date = java.util.Date(lastReadTime)
                val formatter = java.text.SimpleDateFormat("MM-dd", java.util.Locale.getDefault())
                formatter.format(date)
            }
        }
    }
    
    fun getProgressText(): String {
        val percentage = (position * 100).toInt()
        return "已读 $percentage%"
    }
}

/**
 * 阅读统计信息
 */
data class ReadingStats(
    val hasHistory: Boolean,
    val lastReadTime: Long
)
