package com.ibylin.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object BookLockManager {
    
    private const val TAG = "BookLockManager"
    private const val PREFS_NAME = "book_locks"
    private const val KEY_LOCK_PREFIX = "lock_"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 锁定图书
     */
    fun lockBook(context: Context, bookName: String): Boolean {
        return try {
            val prefs = getPrefs(context)
            val key = KEY_LOCK_PREFIX + bookName.hashCode()
            prefs.edit().putBoolean(key, true).apply()
            Log.d(TAG, "图书已锁定: $bookName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "锁定图书失败", e)
            false
        }
    }
    
    /**
     * 解锁图书
     */
    fun unlockBook(context: Context, bookName: String): Boolean {
        return try {
            val prefs = getPrefs(context)
            val key = KEY_LOCK_PREFIX + bookName.hashCode()
            prefs.edit().putBoolean(key, false).apply()
            Log.d(TAG, "图书已解锁: $bookName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "解锁图书失败", e)
            false
        }
    }
    
    /**
     * 检查图书是否被锁定
     */
    fun isBookLocked(context: Context, bookName: String): Boolean {
        return try {
            val prefs = getPrefs(context)
            val key = KEY_LOCK_PREFIX + bookName.hashCode()
            prefs.getBoolean(key, false)
        } catch (e: Exception) {
            Log.e(TAG, "检查图书锁定状态失败", e)
            false
        }
    }
    
    /**
     * 获取所有锁定的图书
     */
    fun getLockedBooks(context: Context): List<String> {
        val lockedBooks = mutableListOf<String>()
        try {
            val prefs = getPrefs(context)
            val allPrefs = prefs.all
            
            allPrefs.forEach { (key, value) ->
                if (key.startsWith(KEY_LOCK_PREFIX) && value is Boolean && value) {
                    // 这里我们只能通过hashCode反向查找，实际使用中可能需要存储书名映射
                    lockedBooks.add(key.substring(KEY_LOCK_PREFIX.length))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取锁定图书列表失败", e)
        }
        return lockedBooks
    }
    
    /**
     * 移除图书锁定
     */
    fun removeBookLock(context: Context, bookName: String): Boolean {
        return try {
            val prefs = getPrefs(context)
            val key = KEY_LOCK_PREFIX + bookName.hashCode()
            prefs.edit().remove(key).apply()
            Log.d(TAG, "图书锁定已移除: $bookName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "移除图书锁定失败", e)
            false
        }
    }
    
    /**
     * 批量锁定图书
     */
    fun lockBooks(context: Context, bookNames: List<String>): Int {
        var successCount = 0
        try {
            val prefs = getPrefs(context)
            val editor = prefs.edit()
            
            bookNames.forEach { bookName ->
                val key = KEY_LOCK_PREFIX + bookName.hashCode()
                editor.putBoolean(key, true)
                successCount++
            }
            
            editor.apply()
            Log.d(TAG, "批量锁定完成: $successCount/${bookNames.size} 本图书")
        } catch (e: Exception) {
            Log.e(TAG, "批量锁定图书失败", e)
        }
        return successCount
    }
    
    /**
     * 批量解锁图书
     */
    fun unlockBooks(context: Context, bookNames: List<String>): Int {
        var successCount = 0
        try {
            val prefs = getPrefs(context)
            val editor = prefs.edit()
            
            bookNames.forEach { bookName ->
                val key = KEY_LOCK_PREFIX + bookName.hashCode()
                editor.putBoolean(key, false)
                successCount++
            }
            
            editor.apply()
            Log.d(TAG, "批量解锁完成: $successCount/${bookNames.size} 本图书")
        } catch (e: Exception) {
            Log.e(TAG, "批量解锁图书失败", e)
        }
        return successCount
    }
    
    /**
     * 批量移除图书锁定
     */
    fun removeBookLocks(context: Context, bookNames: List<String>): Int {
        var successCount = 0
        try {
            val prefs = getPrefs(context)
            val editor = prefs.edit()
            
            bookNames.forEach { bookName ->
                val key = KEY_LOCK_PREFIX + bookName.hashCode()
                editor.remove(key)
                successCount++
            }
            
            editor.apply()
            Log.d(TAG, "批量移除锁定完成: $successCount/${bookNames.size} 本图书")
        } catch (e: Exception) {
            Log.e(TAG, "批量移除图书锁定失败", e)
        }
        return successCount
    }
    
    /**
     * 获取锁定统计信息
     */
    fun getLockStatistics(context: Context): Map<String, Any> {
        return try {
            val prefs = getPrefs(context)
            val allPrefs = prefs.all
            val lockedCount = allPrefs.count { (key, value) ->
                key.startsWith(KEY_LOCK_PREFIX) && value is Boolean && value
            }
            
            mapOf(
                "total_locked" to lockedCount,
                "total_books" to allPrefs.size,
                "lock_percentage" to if (allPrefs.isNotEmpty()) (lockedCount * 100.0 / allPrefs.size) else 0.0
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取锁定统计信息失败", e)
            mapOf("total_locked" to 0, "total_books" to 0, "lock_percentage" to 0.0)
        }
    }
}
