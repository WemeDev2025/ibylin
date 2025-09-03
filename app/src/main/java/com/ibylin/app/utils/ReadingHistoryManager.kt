package com.ibylin.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.ibylin.app.data.model.ReadingHistory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 阅读历史管理器
 */
class ReadingHistoryManager private constructor(context: Context) {
    
    companion object {
        private const val TAG = "ReadingHistoryManager"
        private const val PREF_NAME = "reading_history"
        private const val KEY_LAST_READ_BOOK = "last_read_book"
        private const val KEY_READING_HISTORY = "reading_history"
        
        @Volatile
        private var INSTANCE: ReadingHistoryManager? = null
        
        fun getInstance(context: Context): ReadingHistoryManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReadingHistoryManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    /**
     * 记录阅读行为
     */
    fun recordReading(
        context: Context,
        bookPath: String,
        bookTitle: String,
        bookAuthor: String?,
        currentPage: Int,
        totalPages: Int,
        readProgress: Float
    ) {
        try {
            val bookId = generateBookId(bookPath)
            val readingHistory = ReadingHistory(
                bookId = bookId,
                bookPath = bookPath,
                bookTitle = bookTitle,
                bookAuthor = bookAuthor,
                lastReadTime = System.currentTimeMillis(),
                readProgress = readProgress,
                currentPage = currentPage,
                totalPages = totalPages,
                coverPath = CoverManager.getBookCover(context.applicationContext, bookTitle)
            )
            
            // 保存为最后阅读的书籍
            saveLastReadBook(readingHistory)
            
            // 更新阅读历史
            updateReadingHistory(readingHistory)
            
            Log.d(TAG, "阅读记录已保存: $bookTitle, 进度: ${(readProgress * 100).toInt()}%")
        } catch (e: Exception) {
            Log.e(TAG, "保存阅读记录失败", e)
        }
    }
    
    /**
     * 获取最后阅读的书籍
     */
    fun getLastReadBook(): ReadingHistory? {
        return try {
            val json = sharedPreferences.getString(KEY_LAST_READ_BOOK, null)
            if (json != null) {
                gson.fromJson(json, ReadingHistory::class.java)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "获取最后阅读书籍失败", e)
            null
        }
    }
    
    /**
     * 获取阅读历史列表
     */
    fun getReadingHistory(): List<ReadingHistory> {
        return try {
            val json = sharedPreferences.getString(KEY_READING_HISTORY, "[]")
            val type = object : TypeToken<List<ReadingHistory>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "获取阅读历史失败", e)
            emptyList()
        }
    }
    
    /**
     * 更新单个阅读历史记录
     */
    fun updateReadingHistoryRecord(updatedRecord: ReadingHistory) {
        try {
            val history = getReadingHistory().toMutableList()
            
            // 查找并更新记录
            val existingIndex = history.indexOfFirst { it.bookId == updatedRecord.bookId }
            if (existingIndex != -1) {
                history[existingIndex] = updatedRecord
                
                // 保存更新后的历史
                val json = gson.toJson(history)
                sharedPreferences.edit()
                    .putString(KEY_READING_HISTORY, json)
                    .apply()
                
                // 如果这是最后阅读的书籍，也更新最后阅读记录
                val lastReadBook = getLastReadBook()
                if (lastReadBook?.bookId == updatedRecord.bookId) {
                    saveLastReadBook(updatedRecord)
                }
                
                Log.d(TAG, "阅读历史记录已更新: ${updatedRecord.bookTitle}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "更新阅读历史记录失败", e)
        }
    }
    
    /**
     * 清除阅读历史
     */
    fun clearReadingHistory() {
        try {
            sharedPreferences.edit()
                .remove(KEY_LAST_READ_BOOK)
                .remove(KEY_READING_HISTORY)
                .apply()
            Log.d(TAG, "阅读历史已清除")
        } catch (e: Exception) {
            Log.e(TAG, "清除阅读历史失败", e)
        }
    }
    
    /**
     * 生成书籍唯一标识
     */
    private fun generateBookId(bookPath: String): String {
        return bookPath.hashCode().toString()
    }
    
    /**
     * 保存最后阅读的书籍
     */
    private fun saveLastReadBook(readingHistory: ReadingHistory) {
        try {
            val json = gson.toJson(readingHistory)
            sharedPreferences.edit()
                .putString(KEY_LAST_READ_BOOK, json)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "保存最后阅读书籍失败", e)
        }
    }
    
    /**
     * 更新阅读历史
     */
    private fun updateReadingHistory(newRecord: ReadingHistory) {
        try {
            val history = getReadingHistory().toMutableList()
            
            // 查找是否已存在该书籍的记录
            val existingIndex = history.indexOfFirst { it.bookId == newRecord.bookId }
            
            if (existingIndex != -1) {
                // 更新现有记录
                history[existingIndex] = newRecord
            } else {
                // 添加新记录
                history.add(newRecord)
            }
            
            // 按最后阅读时间排序，最新的在前面
            history.sortByDescending { it.lastReadTime }
            
            // 限制历史记录数量（最多保存50本）
            if (history.size > 50) {
                history.removeAt(history.size - 1)
            }
            
            // 保存到SharedPreferences
            val json = gson.toJson(history)
            sharedPreferences.edit()
                .putString(KEY_READING_HISTORY, json)
                .apply()
                
        } catch (e: Exception) {
            Log.e(TAG, "更新阅读历史失败", e)
        }
    }
}
