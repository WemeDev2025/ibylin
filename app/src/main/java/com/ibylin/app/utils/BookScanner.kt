package com.ibylin.app.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 统一书籍扫描器
 * 支持多种电子书格式的扫描和解析
 */
class BookScanner {
    
    companion object {
        private const val TAG = "BookScanner"
    }
    
    /**
     * 扫描所有支持的电子书格式
     */
    suspend fun scanAllBooks(context: Context): List<BookFile> = withContext(Dispatchers.IO) {
        val allBooks = mutableListOf<BookFile>()
        
        try {
            Log.d(TAG, "开始扫描所有电子书格式")
            
            // 扫描EPUB文件
            val epubScanner = EpubScanner()
            val epubFiles = epubScanner.scanEpubFiles(context)
            Log.d(TAG, "EPUB扫描完成，找到 ${epubFiles.size} 个文件")
            
            // 将EPUB文件转换为统一的BookFile格式
            epubFiles.forEach { epubFile ->
                allBooks.add(BookFile(
                    name = epubFile.name,
                    path = epubFile.path,
                    size = epubFile.size,
                    lastModified = epubFile.lastModified,
                    format = BookFormat.EPUB,
                    metadata = epubFile.metadata?.let { metadata ->
                        BookMetadata(
                            title = metadata.title,
                            author = metadata.author,
                            coverImagePath = metadata.coverImagePath,
                            description = metadata.description,
                            version = metadata.version
                        )
                    }
                ))
            }
            

            
            Log.d(TAG, "所有格式扫描完成，总计 ${allBooks.size} 个文件")
            
        } catch (e: Exception) {
            Log.e(TAG, "扫描电子书时出错", e)
        }
        
        // 按最后修改时间排序，最新的在前面
        allBooks.sortedByDescending { it.lastModified }
    }
    
    /**
     * 扫描指定格式的电子书
     */
    suspend fun scanBooksByFormat(context: Context, format: BookFormat): List<BookFile> = withContext(Dispatchers.IO) {
        when (format) {
            BookFormat.EPUB -> {
                val epubScanner = EpubScanner()
                val epubFiles = epubScanner.scanEpubFiles(context)
                epubFiles.map { epubFile ->
                    BookFile(
                        name = epubFile.name,
                        path = epubFile.path,
                        size = epubFile.size,
                        lastModified = epubFile.lastModified,
                        format = BookFormat.EPUB,
                        metadata = epubFile.metadata?.let { metadata ->
                            BookMetadata(
                                title = metadata.title,
                                author = metadata.author,
                                coverImagePath = metadata.coverImagePath,
                                description = metadata.description,
                                version = metadata.version
                            )
                        }
                    )
                }
            }

        }
    }
    
    /**
     * 获取支持的文件扩展名
     */
    fun getSupportedExtensions(): List<String> {
        return listOf(".epub", ".EPUB")
    }
    
    /**
     * 检查文件是否是支持的电子书格式
     */
    fun isSupportedBookFormat(fileName: String): Boolean {
        return fileName.endsWith(".epub", ignoreCase = true)
    }
    
    /**
     * 根据文件扩展名判断书籍格式
     */
    fun getBookFormatByExtension(fileName: String): BookFormat? {
        return when {
            fileName.endsWith(".epub", ignoreCase = true) -> BookFormat.EPUB
            else -> null
        }
    }
}

/**
 * 电子书格式枚举
 */
enum class BookFormat(val displayName: String, val fileExtensions: List<String>) {
    EPUB("EPUB", listOf(".epub", ".EPUB"))
}

/**
 * 统一书籍文件信息
 */
@androidx.annotation.Keep
@kotlinx.parcelize.Parcelize
data class BookFile(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val format: BookFormat,
    val metadata: BookMetadata? = null
) : android.os.Parcelable {
    /**
     * 获取显示名称
     */
    fun getDisplayName(): String {
        return metadata?.title ?: name
    }
    
    /**
     * 获取作者信息
     */
    fun getAuthorInfo(): String {
        return metadata?.author ?: "未知作者"
    }
    
    /**
     * 获取文件大小描述
     */
    fun getSizeDescription(): String {
        return when {
            size < 1024 -> "${size} B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }
    
    /**
     * 获取格式显示名称
     */
    fun getFormatDisplayName(): String {
        return format.displayName
    }
}

/**
 * 统一书籍元数据
 */
@androidx.annotation.Keep
@kotlinx.parcelize.Parcelize
data class BookMetadata(
    val title: String,
    val author: String? = null,
    val coverImagePath: String? = null,
    val description: String? = null,
    val version: String? = null
) : android.os.Parcelable
