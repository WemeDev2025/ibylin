package com.ibylin.app.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class EpubScanner {
    
    companion object {
        private const val TAG = "EpubScanner"
        private val EPUB_EXTENSIONS = listOf(".epub", ".EPUB")
    }
    
    /**
     * 扫描设备上的所有EPUB文件
     */
    suspend fun scanEpubFiles(context: Context): List<EpubFile> = withContext(Dispatchers.IO) {
        val epubFiles = mutableListOf<EpubFile>()
        
        try {
            // 扫描外部存储
            val externalStorage = Environment.getExternalStorageDirectory()
            if (externalStorage.exists() && externalStorage.canRead()) {
                scanDirectory(externalStorage, epubFiles)
            }
            
            // 扫描内部存储
            val internalStorage = context.filesDir.parentFile
            if (internalStorage != null && internalStorage.exists() && internalStorage.canRead()) {
                scanDirectory(internalStorage, epubFiles)
            }
            
            // 扫描下载目录
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadDir.exists() && downloadDir.canRead()) {
                scanDirectory(downloadDir, epubFiles)
            }
            
            // 扫描文档目录
            val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (documentsDir.exists() && documentsDir.canRead()) {
                scanDirectory(documentsDir, epubFiles)
            }
            
            Log.d(TAG, "扫描完成，找到 ${epubFiles.size} 个EPUB文件")
            
        } catch (e: Exception) {
            Log.e(TAG, "扫描EPUB文件时出错", e)
        }
        
        epubFiles.sortedBy { it.name }
    }
    
    /**
     * 递归扫描目录
     */
    private fun scanDirectory(directory: File, epubFiles: MutableList<EpubFile>) {
        try {
            if (!directory.exists() || !directory.canRead()) return
            
            val files = directory.listFiles() ?: return
            
            for (file in files) {
                if (file.isFile) {
                    // 检查是否是EPUB文件
                    if (isEpubFile(file.name)) {
                        // 尝试解析EPUB元数据
                        val metadata = tryParseEpubMetadata(file.absolutePath)
                        
                        val epubFile = EpubFile(
                            name = file.name,
                            path = file.absolutePath,
                            size = file.length(),
                            lastModified = file.lastModified(),
                            metadata = metadata
                        )
                        epubFiles.add(epubFile)
                        Log.d(TAG, "找到EPUB文件: ${file.absolutePath}, 元数据: ${metadata?.title}")
                    }
                } else if (file.isDirectory && !isSystemDirectory(file.name)) {
                    // 递归扫描子目录，但跳过系统目录
                    scanDirectory(file, epubFiles)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "扫描目录 ${directory.absolutePath} 时出错", e)
        }
    }
    
    /**
     * 检查文件是否是EPUB格式
     */
    private fun isEpubFile(fileName: String): Boolean {
        return EPUB_EXTENSIONS.any { fileName.endsWith(it) }
    }
    
    /**
     * 尝试解析EPUB文件的元数据
     */
    private fun tryParseEpubMetadata(filePath: String): EpubFileMetadata? {
        return try {
            val zipFile = java.util.zip.ZipFile(filePath)
            
            // 查找container.xml
            val containerEntry = zipFile.getEntry("META-INF/container.xml")
            if (containerEntry == null) {
                zipFile.close()
                return null
            }
            
            val containerXml = zipFile.getInputStream(containerEntry).bufferedReader().use { it.readText() }
            val rootFilePath = extractRootFilePath(containerXml)
            
            // 查找OPF文件
            val opfEntry = zipFile.getEntry(rootFilePath)
            if (opfEntry == null) {
                zipFile.close()
                return null
            }
            
            val opfXml = zipFile.getInputStream(opfEntry).bufferedReader().use { it.readText() }
            
            // 提取基本信息
            val title = extractTitle(opfXml) ?: "未知书名"
            val author = extractAuthor(opfXml)
            val description = extractDescription(opfXml)
            val coverImagePath = findCoverImage(opfXml, zipFile)
            val version = detectEpubVersion(opfXml)
            
            zipFile.close()
            
            EpubFileMetadata(
                title = title,
                author = author,
                coverImagePath = coverImagePath,
                description = description,
                version = version
            )
            
        } catch (e: Exception) {
            Log.w(TAG, "解析EPUB元数据失败: ${e.message}")
            null
        }
    }
    
    /**
     * 从container.xml中提取根文件路径
     */
    private fun extractRootFilePath(containerXml: String): String {
        val regex = """<rootfile.*?full-path="([^"]*?)".*?>""".toRegex()
        val matchResult = regex.find(containerXml)
        return matchResult?.groupValues?.get(1) ?: "OEBPS/content.opf"
    }
    
    /**
     * 从OPF文件中提取标题
     */
    private fun extractTitle(opfXml: String): String? {
        val titleRegex = """<dc:title[^>]*>([^<]*)</dc:title>""".toRegex()
        val matchResult = titleRegex.find(opfXml)
        return matchResult?.groupValues?.get(1)?.trim()
    }
    
    /**
     * 从OPF文件中提取作者
     */
    private fun extractAuthor(opfXml: String): String? {
        val authorRegex = """<dc:creator[^>]*>([^<]*)</dc:creator>""".toRegex()
        val matchResult = authorRegex.find(opfXml)
        return matchResult?.groupValues?.get(1)?.trim()
    }
    
    /**
     * 从OPF文件中提取描述
     */
    private fun extractDescription(opfXml: String): String? {
        val descriptionRegex = """<dc:description[^>]*>([^<]*)</dc:description>""".toRegex()
        val matchResult = descriptionRegex.find(opfXml)
        return matchResult?.groupValues?.get(1)?.trim()
    }
    
    /**
     * 检测EPUB版本
     */
    private fun detectEpubVersion(opfXml: String): String {
        return try {
            // 检查EPUB 3.0特征 - 更精确的检测
            if (opfXml.contains("epub:type") || 
                opfXml.contains("epub:prefix") || 
                opfXml.contains("epub:role") ||
                opfXml.contains("epub:scheme") ||
                opfXml.contains("epub:vocabulary") ||
                opfXml.contains("epub:switch") ||
                opfXml.contains("epub:trigger") ||
                opfXml.contains("epub:media-overlay") ||
                opfXml.contains("epub:media-overlay-active-class") ||
                opfXml.contains("epub:media-overlay-playback-active-class")) {
                "EPUB 3.0"
            }
            // 检查EPUB 2.0特征 - 更精确的检测
            else if (opfXml.contains("dc:identifier") || 
                     opfXml.contains("dc:title") || 
                     opfXml.contains("dc:creator") ||
                     opfXml.contains("dc:language") ||
                     opfXml.contains("dc:publisher") ||
                     opfXml.contains("dc:date") ||
                     opfXml.contains("dc:subject") ||
                     opfXml.contains("dc:description") ||
                     opfXml.contains("dc:rights")) {
                "EPUB 2.0"
            }
            // 检查是否有基本的OPF结构
            else if (opfXml.contains("<?xml") && 
                     opfXml.contains("<package") && 
                     opfXml.contains("<metadata") && 
                     opfXml.contains("<manifest")) {
                "基础EPUB"
            }
            // 默认返回基础版本
            else {
                "基础EPUB"
            }
        } catch (e: Exception) {
            "基础EPUB"
        }
    }

    /**
     * 查找封面图片
     */
    private fun findCoverImage(opfXml: String, zipFile: java.util.zip.ZipFile): String? {
        return try {
            // 方法1: 查找id为"cover"的item
            val coverIdRegex = """id="cover"[^>]*href="([^"]*?)"""".toRegex()
            val coverIdMatch = coverIdRegex.find(opfXml)
            if (coverIdMatch != null) {
                val coverPath = coverIdMatch.groupValues[1]
                val fullPath = if (coverPath.startsWith("/")) coverPath else "OEBPS/$coverPath"
                if (zipFile.getEntry(fullPath) != null) {
                    return fullPath
                }
            }
            
            // 方法2: 查找包含"cover"的item
            val coverItemRegex = """<item[^>]*href="([^"]*?cover[^"]*?)"[^>]*>""".toRegex(RegexOption.IGNORE_CASE)
            val coverItemMatch = coverItemRegex.find(opfXml)
            if (coverItemMatch != null) {
                val coverPath = coverItemMatch.groupValues[1]
                val fullPath = if (coverPath.startsWith("/")) coverPath else "OEBPS/$coverPath"
                if (zipFile.getEntry(fullPath) != null) {
                    return fullPath
                }
            }
            
            // 方法3: 查找常见的图片文件
            val imageRegex = """<item[^>]*href="([^"]*?\.(?:jpg|jpeg|png|gif))"[^>]*>""".toRegex(RegexOption.IGNORE_CASE)
            val imageMatches = imageRegex.findAll(opfXml)
            
            for (match in imageMatches) {
                val imagePath = match.groupValues[1]
                val fullPath = if (imagePath.startsWith("/")) imagePath else "OEBPS/$imagePath"
                if (zipFile.getEntry(fullPath) != null) {
                    return fullPath
                }
            }
            
            null
            
        } catch (e: Exception) {
            Log.w(TAG, "查找封面图片失败: ${e.message}")
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
 * EPUB文件信息数据类
 */
@androidx.annotation.Keep
@kotlinx.parcelize.Parcelize
data class EpubFile(
    val name: String,
    val path: String,
    val size: Long,
    val lastModified: Long,
    val metadata: EpubFileMetadata? = null
) : android.os.Parcelable {
    /**
     * 获取文件大小的人类可读格式
     */
    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "${size} B"
            size < 1024 * 1024 -> "${String.format("%.1f", size / 1024.0)} KB"
            size < 1024 * 1024 * 1024 -> "${String.format("%.1f", size / (1024.0 * 1024.0))} MB"
            else -> "${String.format("%.1f", size / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }
    
    /**
     * 获取最后修改时间的格式化字符串
     */
    fun getFormattedDate(): String {
        val date = java.util.Date(lastModified)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }
}

/**
 * EPUB文件元数据信息数据类
 */
@androidx.annotation.Keep
@kotlinx.parcelize.Parcelize
data class EpubFileMetadata(
    val title: String,
    val author: String?,
    val coverImagePath: String?,
    val description: String?,
    val version: String? = null
) : android.os.Parcelable
