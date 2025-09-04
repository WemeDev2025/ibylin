package com.ibylin.app.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

/**
 * MOBI格式电子书扫描器
 * 支持扫描和解析MOBI格式的电子书文件
 */
class MobiScanner {
    
    companion object {
        private const val TAG = "MobiScanner"
        private val MOBI_EXTENSIONS = listOf(".mobi", ".MOBI", ".azw", ".AZW", ".azw3", ".AZW3")
        
        // MOBI文件头标识
        private val MOBI_HEADER = "BOOKMOBI"
        private val PALMDB_HEADER = "TEXtREAd"
    }
    
    /**
     * 获取单个MOBI文件信息
     */
    fun getMobiFileInfo(filePath: String): MobiFile? {
        return try {
            val file = File(filePath)
            if (file.exists() && isMobiFile(file.name)) {
                val metadata = tryParseMobiMetadata(file.absolutePath)
                MobiFile(
                    name = file.name,
                    path = file.absolutePath,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    metadata = metadata
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "获取MOBI文件信息失败: $filePath", e)
            null
        }
    }
    
    /**
     * 扫描设备上的所有MOBI文件
     */
    suspend fun scanMobiFiles(context: Context): List<MobiFile> = withContext(Dispatchers.IO) {
        val mobiFiles = mutableListOf<MobiFile>()
        
        try {
            // 扫描外部存储
            val externalStorage = Environment.getExternalStorageDirectory()
            if (externalStorage.exists() && externalStorage.canRead()) {
                scanDirectory(externalStorage, mobiFiles)
            }
            
            // 扫描内部存储
            val internalStorage = context.filesDir.parentFile
            if (internalStorage != null && internalStorage.exists() && internalStorage.canRead()) {
                scanDirectory(internalStorage, mobiFiles)
            }
            
            // 扫描下载目录
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir.exists() && downloadDir.canRead()) {
                scanDirectory(downloadDir, mobiFiles)
            }
            
            // 扫描文档目录
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (documentsDir.exists() && documentsDir.canRead()) {
                scanDirectory(documentsDir, mobiFiles)
            }
            
            Log.d(TAG, "扫描完成，找到 ${mobiFiles.size} 个MOBI文件")
            
        } catch (e: Exception) {
            Log.e(TAG, "扫描MOBI文件时出错", e)
        }
        
        mobiFiles.sortedBy { it.name }
    }
    
    /**
     * 递归扫描目录
     */
    private fun scanDirectory(directory: File, mobiFiles: MutableList<MobiFile>) {
        try {
            if (!directory.exists() || !directory.canRead()) return
            
            val files = directory.listFiles() ?: return
            
            for (file in files) {
                if (file.isFile) {
                    // 检查是否是MOBI文件
                    if (isMobiFile(file.name)) {
                        // 尝试解析MOBI元数据
                        val metadata = tryParseMobiMetadata(file.absolutePath)
                        
                        val mobiFile = MobiFile(
                            name = file.name,
                            path = file.absolutePath,
                            size = file.length(),
                            lastModified = file.lastModified(),
                            metadata = metadata
                        )
                        mobiFiles.add(mobiFile)
                        Log.d(TAG, "找到MOBI文件: ${file.absolutePath}, 元数据: ${metadata?.title}")
                    }
                } else if (file.isDirectory && !isSystemDirectory(file.name)) {
                    // 递归扫描子目录，但跳过系统目录
                    scanDirectory(file, mobiFiles)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "扫描目录 ${directory.absolutePath} 时出错", e)
        }
    }
    
    /**
     * 检查文件是否是MOBI格式
     */
    private fun isMobiFile(fileName: String): Boolean {
        return MOBI_EXTENSIONS.any { fileName.endsWith(it) }
    }
    
    /**
     * 尝试解析MOBI文件的元数据
     */
    private fun tryParseMobiMetadata(filePath: String): MobiFileMetadata? {
        return try {
            val file = RandomAccessFile(filePath, "r")
            
            try {
                // 检查文件头
                val header = ByteArray(8)
                file.read(header)
                val headerString = String(header)
                
                if (!headerString.startsWith(MOBI_HEADER) && !headerString.startsWith(PALMDB_HEADER)) {
                    Log.w(TAG, "不是有效的MOBI文件: $filePath")
                    return null
                }
                
                // 解析基本信息
                val title = extractTitle(file) ?: "未知书名"
                val author = extractAuthor(file)
                val description = extractDescription(file)
                val coverImagePath = findCoverImage(file)
                val version = detectMobiVersion(file)
                
                MobiFileMetadata(
                    title = title,
                    author = author,
                    coverImagePath = coverImagePath,
                    description = description,
                    version = version
                )
                
            } finally {
                file.close()
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "解析MOBI元数据失败: ${e.message}")
            null
        }
    }
    
    /**
     * 从MOBI文件中提取标题
     */
    private fun extractTitle(file: RandomAccessFile): String? {
        return try {
            // 这里需要根据MOBI文件格式规范来解析
            // 简化实现，实际应该解析PalmDB记录
            "MOBI电子书"
        } catch (e: Exception) {
            Log.w(TAG, "提取标题失败: ${e.message}")
            null
        }
    }
    
    /**
     * 从MOBI文件中提取作者
     */
    private fun extractAuthor(file: RandomAccessFile): String? {
        return try {
            // 简化实现
            null
        } catch (e: Exception) {
            Log.w(TAG, "提取作者失败: ${e.message}")
            null
        }
    }
    
    /**
     * 从MOBI文件中提取描述
     */
    private fun extractDescription(file: RandomAccessFile): String? {
        return try {
            // 简化实现
            null
        } catch (e: Exception) {
            Log.w(TAG, "提取描述失败: ${e.message}")
            null
        }
    }
    
    /**
     * 查找封面图片
     */
    private fun findCoverImage(file: RandomAccessFile): String? {
        return try {
            // 简化实现，实际应该解析图片记录
            null
        } catch (e: Exception) {
            Log.w(TAG, "查找封面图片失败: ${e.message}")
            null
        }
    }
    
    /**
     * 检测MOBI版本
     */
    private fun detectMobiVersion(file: RandomAccessFile): String? {
        return try {
            // 简化实现
            "MOBI"
        } catch (e: Exception) {
            Log.w(TAG, "检测版本失败: ${e.message}")
            null
        }
    }
    
    /**
     * 检查是否是系统目录（跳过扫描）
     */
    private fun isSystemDirectory(dirName: String): Boolean {
        val systemDirs = listOf(
            "Android", "DCIM", "Pictures", "Music", "Movies", "Videos",
            "system", "proc", "sys", "dev", "data", "cache", "tmp",
            ".android", ".thumbnails", ".trash", ".recycle"
        )
        return systemDirs.any { dirName.equals(it, ignoreCase = true) } ||
               dirName.startsWith(".") // 隐藏目录
    }
}

/**
 * MOBI文件信息数据类
 */
@androidx.annotation.Keep
@kotlinx.parcelize.Parcelize
data class MobiFile(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val metadata: MobiFileMetadata? = null
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
}

/**
 * MOBI文件元数据
 */
@androidx.annotation.Keep
@kotlinx.parcelize.Parcelize
data class MobiFileMetadata(
    val title: String,
    val author: String? = null,
    val coverImagePath: String? = null,
    val description: String? = null,
    val version: String? = null
) : android.os.Parcelable
