package com.ibylin.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object CoverManager {
    
    private const val TAG = "CoverManager"
    private const val PREFS_NAME = "book_covers"
    private const val KEY_COVER_PREFIX = "cover_"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 保存书籍封面路径
     */
    fun saveBookCover(context: Context, bookName: String, coverPath: String) {
        try {
            val prefs = getPrefs(context)
            val key = KEY_COVER_PREFIX + bookName.hashCode()
            prefs.edit().putString(key, coverPath).apply()
            Log.d(TAG, "保存封面路径: $bookName -> $coverPath")
        } catch (e: Exception) {
            Log.e(TAG, "保存封面路径失败", e)
        }
    }
    
    /**
     * 获取书籍封面路径
     */
    fun getBookCover(context: Context, bookName: String): String? {
        return try {
            val prefs = getPrefs(context)
            val key = KEY_COVER_PREFIX + bookName.hashCode()
            val coverPath = prefs.getString(key, null)
            Log.d(TAG, "获取封面路径: $bookName -> $coverPath")
            coverPath
        } catch (e: Exception) {
            Log.e(TAG, "获取封面路径失败", e)
            null
        }
    }
    
    /**
     * 检查书籍是否有自定义封面
     */
    fun hasCustomCover(context: Context, bookName: String): Boolean {
        val coverPath = getBookCover(context, bookName)
        return coverPath != null && java.io.File(coverPath).exists()
    }
    
    /**
     * 删除书籍封面
     */
    fun removeBookCover(context: Context, bookName: String) {
        try {
            val prefs = getPrefs(context)
            val key = KEY_COVER_PREFIX + bookName.hashCode()
            prefs.edit().remove(key).apply()
            
            // 同时删除本地文件
            val coverPath = getBookCover(context, bookName)
            if (coverPath != null) {
                val file = java.io.File(coverPath)
                if (file.exists()) {
                    file.delete()
                    Log.d(TAG, "删除封面文件: $coverPath")
                }
            }
            
            Log.d(TAG, "删除封面: $bookName")
        } catch (e: Exception) {
            Log.e(TAG, "删除封面失败", e)
        }
    }
    
    /**
     * 获取所有封面路径
     */
    fun getAllCoverPaths(context: Context): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            val prefs = getPrefs(context)
            val allPrefs = prefs.all
            
            allPrefs.forEach { (key, value) ->
                if (key.startsWith(KEY_COVER_PREFIX) && value is String) {
                    val bookName = key.substring(KEY_COVER_PREFIX.length)
                    result[bookName] = value
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取所有封面路径失败", e)
        }
        return result
    }
    
    /**
     * 清理无效的封面路径
     */
    fun cleanupInvalidCovers(context: Context) {
        try {
            val allCovers = getAllCoverPaths(context)
            val invalidCovers = mutableListOf<String>()
            
            allCovers.forEach { (bookName, coverPath) ->
                val file = java.io.File(coverPath)
                if (!file.exists()) {
                    invalidCovers.add(bookName)
                }
            }
            
            invalidCovers.forEach { bookName ->
                removeBookCover(context, bookName)
            }
            
            if (invalidCovers.isNotEmpty()) {
                Log.d(TAG, "清理了 ${invalidCovers.size} 个无效封面")
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理无效封面失败", e)
        }
    }
}
