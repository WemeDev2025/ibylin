package com.ibylin.app.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 阅读历史数据类
 */
@Parcelize
data class ReadingHistory(
    val bookId: String,           // 书籍唯一标识
    val bookPath: String,         // 书籍文件路径
    val bookTitle: String,        // 书籍标题
    val bookAuthor: String?,      // 书籍作者
    val lastReadTime: Long,       // 最后阅读时间
    val readProgress: Float,      // 阅读进度 (0.0 - 1.0)
    val currentPage: Int,         // 当前页码
    val totalPages: Int,          // 总页数
    val coverPath: String?        // 封面路径
) : Parcelable
