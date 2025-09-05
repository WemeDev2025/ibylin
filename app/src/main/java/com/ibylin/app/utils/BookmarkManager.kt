package com.ibylin.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.io.File

/**
 * 书签图书管理器
 * 负责管理所有已添加书签的图书信息
 */
object BookmarkManager {
    
    private const val TAG = "BookmarkManager"
    private const val PREFS_NAME = "reader_settings"
    
    /**
     * 书签图书数据类
     */
    data class BookmarkBook(
        val bookPath: String,
        val bookTitle: String,
        val bookAuthor: String?,
        val bookmarkTime: Long,
        val bookmarkId: String,
        val locatorJson: String? = null,
        val currentPage: Int = 0,
        val totalPages: Int = 0,
        val progress: Float = 0f
    )
    
    /**
     * 获取所有已添加书签的图书
     * 按书签时间从新到旧排序
     */
    fun getAllBookmarkBooks(context: Context): List<BookmarkBook> {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val allPrefs = prefs.all
            
            val bookmarkBooks = mutableListOf<BookmarkBook>()
            
            // 遍历所有SharedPreferences条目，查找书签相关的条目
            for ((key, value) in allPrefs) {
                if (key.startsWith("bookmarks_") && value is Set<*>) {
                    // 提取书名
                    val bookTitle = key.removePrefix("bookmarks_")
                    
                    // 查找对应的书籍信息
                    val bookInfo = findBookInfo(context, bookTitle)
                    if (bookInfo != null) {
                        // 为每个书签创建BookmarkBook对象
                        value.forEach { bookmarkId ->
                            if (bookmarkId is String) {
                                val bookmarkTime = extractTimestampFromBookmarkId(bookmarkId)
                                val bookmarkBook = BookmarkBook(
                                    bookPath = bookInfo.path,
                                    bookTitle = bookInfo.title,
                                    bookAuthor = bookInfo.author,
                                    bookmarkTime = bookmarkTime,
                                    bookmarkId = bookmarkId,
                                    locatorJson = bookInfo.locatorJson,
                                    currentPage = bookInfo.currentPage,
                                    totalPages = bookInfo.totalPages,
                                    progress = bookInfo.progress
                                )
                                bookmarkBooks.add(bookmarkBook)
                            }
                        }
                    }
                }
            }
            
            // 按书签时间从新到旧排序
            bookmarkBooks.sortByDescending { it.bookmarkTime }
            
            Log.d(TAG, "找到${bookmarkBooks.size}本已添加书签的图书")
            return bookmarkBooks
            
        } catch (e: Exception) {
            Log.e(TAG, "获取书签图书失败", e)
            return emptyList()
        }
    }
    
    /**
     * 查找书籍信息
     */
    private fun findBookInfo(context: Context, bookTitle: String): BookInfo? {
        try {
            val prefs = context.getSharedPreferences("reading_progress", Context.MODE_PRIVATE)
            val allPrefs = prefs.all
            
            // 查找匹配的书籍路径
            for ((key, value) in allPrefs) {
                if (key.endsWith("_title") && value == bookTitle) {
                    val bookPath = key.removeSuffix("_title")
                    
                    // 获取其他书籍信息
                    val author = prefs.getString("${bookPath}_author", null)
                    val locatorJson = prefs.getString("${bookPath}_locator", null)
                    val currentPage = prefs.getInt("${bookPath}_current_page", 0)
                    val totalPages = prefs.getInt("${bookPath}_total_pages", 0)
                    val progress = prefs.getFloat("${bookPath}_progress", 0f)
                    
                    return BookInfo(
                        path = bookPath,
                        title = bookTitle,
                        author = author,
                        locatorJson = locatorJson,
                        currentPage = currentPage,
                        totalPages = totalPages,
                        progress = progress
                    )
                }
            }
            
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "查找书籍信息失败", e)
            return null
        }
    }
    
    /**
     * 从书签ID中提取时间戳
     */
    private fun extractTimestampFromBookmarkId(bookmarkId: String): Long {
        return try {
            val parts = bookmarkId.split("_")
            if (parts.size >= 2) {
                parts.last().toLong()
            } else {
                System.currentTimeMillis()
            }
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    /**
     * 书籍信息数据类
     */
    private data class BookInfo(
        val path: String,
        val title: String,
        val author: String?,
        val locatorJson: String?,
        val currentPage: Int,
        val totalPages: Int,
        val progress: Float
    )
    
    /**
     * 检查书籍是否有书签
     */
    fun hasBookmarks(context: Context, bookTitle: String): Boolean {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val bookmarks = prefs.getStringSet("bookmarks_$bookTitle", emptySet())
            return !bookmarks.isNullOrEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "检查书签失败", e)
            return false
        }
    }
    
    /**
     * 获取书籍的书签数量
     */
    fun getBookmarkCount(context: Context, bookTitle: String): Int {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val bookmarks = prefs.getStringSet("bookmarks_$bookTitle", emptySet())
            return bookmarks?.size ?: 0
        } catch (e: Exception) {
            Log.e(TAG, "获取书签数量失败", e)
            return 0
        }
    }
}
