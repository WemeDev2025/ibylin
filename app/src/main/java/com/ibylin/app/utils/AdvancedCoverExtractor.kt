package com.ibylin.app.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.io.InputStream
import java.util.regex.Pattern
import java.util.regex.Matcher

/**
 * 高级EPUB封面解析器
 * 使用最新的解析技术和智能算法，提高封面检测成功率
 */
class AdvancedCoverExtractor {
    
    companion object {
        private const val TAG = "AdvancedCoverExtractor"
        
        /**
         * 智能封面检测优先级
         */
        enum class CoverDetectionMethod(val priority: Int, val description: String) {
            METADATA_COVER_ID(1, "元数据中的cover ID"),
            METADATA_COVER_PROPERTIES(2, "元数据中的cover属性"),
            COMMON_COVER_NAMES(3, "常见封面文件名"),
            FIRST_IMAGE_FILE(4, "第一个图片文件"),
            LARGEST_IMAGE_FILE(5, "最大的图片文件"),
            IMAGE_IN_HTML(6, "HTML中的图片"),
            FUZZY_SEARCH(7, "模糊搜索"),
            FALLBACK_COLOR(8, "备用颜色")
        }
        
        /**
         * 智能封面检测
         */
        fun extractCover(epubPath: String, opfContent: String? = null): CoverResult {
            Log.d(TAG, "开始智能封面检测: $epubPath")
            
            return try {
                val zipFile = ZipFile(epubPath)
                
                // 如果没有提供OPF内容，尝试解析
                val opfXml = opfContent ?: extractOpfContent(zipFile)
                
                if (opfXml != null) {
                    // 使用智能检测算法
                    val coverResult = intelligentCoverDetection(zipFile, opfXml)
                    zipFile.close()
                    return coverResult
                } else {
                    // 降级到基础检测
                    val fallbackResult = fallbackCoverDetection(zipFile)
                    zipFile.close()
                    return fallbackResult
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "封面检测失败: ${e.message}", e)
                CoverResult.failure("封面检测异常: ${e.message}")
            }
        }
        
        /**
         * 智能封面检测算法
         */
        private fun intelligentCoverDetection(zipFile: ZipFile, opfXml: String): CoverResult {
            Log.d(TAG, "使用智能检测算法")
            
            // 方法1: 元数据中的cover ID (最高优先级)
            val coverIdResult = detectByCoverId(zipFile, opfXml)
            if (coverIdResult.isSuccess) {
                Log.d(TAG, "通过cover ID找到封面: ${coverIdResult.coverPath}")
                return coverIdResult
            }
            
            // 方法2: 元数据中的cover属性
            val coverPropResult = detectByCoverProperties(zipFile, opfXml)
            if (coverPropResult.isSuccess) {
                Log.d(TAG, "通过cover属性找到封面: ${coverPropResult.coverPath}")
                return coverPropResult
            }
            
            // 方法3: 常见封面文件名
            val commonNameResult = detectByCommonNames(zipFile)
            if (commonNameResult.isSuccess) {
                Log.d(TAG, "通过常见文件名找到封面: ${commonNameResult.coverPath}")
                return commonNameResult
            }
            
            // 方法4: 第一个图片文件
            val firstImageResult = detectByFirstImage(zipFile, opfXml)
            if (firstImageResult.isSuccess) {
                Log.d(TAG, "通过第一个图片找到封面: ${firstImageResult.coverPath}")
                return firstImageResult
            }
            
            // 方法5: 最大的图片文件
            val largestImageResult = detectByLargestImage(zipFile, opfXml)
            if (largestImageResult.isSuccess) {
                Log.d(TAG, "通过最大图片找到封面: ${largestImageResult.coverPath}")
                return largestImageResult
            }
            
            // 方法6: HTML中的图片
            val htmlImageResult = detectByHtmlImages(zipFile, opfXml)
            if (htmlImageResult.isSuccess) {
                Log.d(TAG, "通过HTML图片找到封面: ${htmlImageResult.coverPath}")
                return htmlImageResult
            }
            
            // 方法7: 模糊搜索
            val fuzzyResult = detectByFuzzySearch(zipFile, opfXml)
            if (fuzzyResult.isSuccess) {
                Log.d(TAG, "通过模糊搜索找到封面: ${fuzzyResult.coverPath}")
                return fuzzyResult
            }
            
            // 所有方法都失败，返回失败结果
            Log.w(TAG, "所有智能检测方法都失败")
            return CoverResult.failure("未找到合适的封面图片")
        }
        
        /**
         * 通过cover ID检测封面
         */
        private fun detectByCoverId(zipFile: ZipFile, opfXml: String): CoverResult {
            try {
                // 查找id为"cover"的item
                val coverIdPattern = Pattern.compile("""id="cover"[^>]*href="([^"]*?)"""")
                val matcher = coverIdPattern.matcher(opfXml)
                
                if (matcher.find()) {
                    val coverPath = matcher.group(1)
                    val fullPath = resolvePath(coverPath)
                    
                    if (isValidImageFile(zipFile, fullPath)) {
                        val bitmap = loadImageFromZip(zipFile, fullPath)
                        if (bitmap != null) {
                            return CoverResult.success(fullPath, bitmap, CoverDetectionMethod.METADATA_COVER_ID)
                        }
                    }
                }
                
                return CoverResult.failure("cover ID检测失败")
            } catch (e: Exception) {
                Log.w(TAG, "cover ID检测异常: ${e.message}")
                return CoverResult.failure("cover ID检测异常")
            }
        }
        
        /**
         * 通过cover属性检测封面
         */
        private fun detectByCoverProperties(zipFile: ZipFile, opfXml: String): CoverResult {
            try {
                // 查找包含cover属性的item
                val coverPropPattern = Pattern.compile("""<item[^>]*properties="[^"]*cover[^"]*"[^>]*href="([^"]*?)"""")
                val matcher = coverPropPattern.matcher(opfXml)
                
                if (matcher.find()) {
                    val coverPath = matcher.group(1)
                    val fullPath = resolvePath(coverPath)
                    
                    if (isValidImageFile(zipFile, fullPath)) {
                        val bitmap = loadImageFromZip(zipFile, fullPath)
                        if (bitmap != null) {
                            return CoverResult.success(fullPath, bitmap, CoverDetectionMethod.METADATA_COVER_PROPERTIES)
                        }
                    }
                }
                
                return CoverResult.failure("cover属性检测失败")
            } catch (e: Exception) {
                Log.w(TAG, "cover属性检测异常: ${e.message}")
                return CoverResult.failure("cover属性检测异常")
            }
        }
        
        /**
         * 通过常见文件名检测封面
         */
        private fun detectByCommonNames(zipFile: ZipFile): CoverResult {
            try {
                val commonNames = listOf(
                    "cover.jpg", "cover.png", "cover.jpeg", "cover.gif", "cover.bmp",
                    "Cover.jpg", "Cover.png", "Cover.jpeg", "Cover.gif", "Cover.bmp",
                    "COVER.jpg", "COVER.png", "COVER.jpeg", "COVER.gif", "COVER.bmp",
                    "front.jpg", "front.png", "front.jpeg", "front.gif", "front.bmp",
                    "Front.jpg", "Front.png", "Front.jpeg", "Front.gif", "Front.bmp",
                    "title.jpg", "title.png", "title.jpeg", "title.gif", "title.bmp",
                    "Title.jpg", "Title.png", "Title.jpeg", "Title.gif", "Title.bmp"
                )
                
                for (name in commonNames) {
                    val entry = zipFile.getEntry(name)
                    if (entry != null) {
                        val bitmap = loadImageFromZip(zipFile, name)
                        if (bitmap != null) {
                            return CoverResult.success(name, bitmap, CoverDetectionMethod.COMMON_COVER_NAMES)
                        }
                    }
                }
                
                return CoverResult.failure("常见文件名检测失败")
            } catch (e: Exception) {
                Log.w(TAG, "常见文件名检测异常: ${e.message}")
                return CoverResult.failure("常见文件名检测异常")
            }
        }
        
        /**
         * 通过第一个图片文件检测封面
         */
        private fun detectByFirstImage(zipFile: ZipFile, opfXml: String): CoverResult {
            try {
                val imagePattern = Pattern.compile("""<item[^>]*href="([^"]*?\.(?:jpg|jpeg|png|gif|bmp))"[^>]*>""", Pattern.CASE_INSENSITIVE)
                val matcher = imagePattern.matcher(opfXml)
                
                if (matcher.find()) {
                    val imagePath = matcher.group(1)
                    val fullPath = resolvePath(imagePath)
                    
                    if (isValidImageFile(zipFile, fullPath)) {
                        val bitmap = loadImageFromZip(zipFile, fullPath)
                        if (bitmap != null) {
                            return CoverResult.success(fullPath, bitmap, CoverDetectionMethod.FIRST_IMAGE_FILE)
                        }
                    }
                }
                
                return CoverResult.failure("第一个图片检测失败")
            } catch (e: Exception) {
                Log.w(TAG, "第一个图片检测异常: ${e.message}")
                return CoverResult.failure("第一个图片检测异常")
            }
        }
        
        /**
         * 通过最大图片文件检测封面
         */
        private fun detectByLargestImage(zipFile: ZipFile, opfXml: String): CoverResult {
            try {
                val imagePattern = Pattern.compile("""<item[^>]*href="([^"]*?\.(?:jpg|jpeg|png|gif|bmp))"[^>]*>""", Pattern.CASE_INSENSITIVE)
                val matcher = imagePattern.matcher(opfXml)
                
                var largestImage: String? = null
                var largestSize: Long = 0
                
                while (matcher.find()) {
                    val imagePath = matcher.group(1)
                    val fullPath = resolvePath(imagePath)
                    
                    val entry = zipFile.getEntry(fullPath)
                    if (entry != null && entry.size > largestSize) {
                        largestSize = entry.size
                        largestImage = fullPath
                    }
                }
                
                if (largestImage != null) {
                    val bitmap = loadImageFromZip(zipFile, largestImage)
                    if (bitmap != null) {
                        return CoverResult.success(largestImage, bitmap, CoverDetectionMethod.LARGEST_IMAGE_FILE)
                    }
                }
                
                return CoverResult.failure("最大图片检测失败")
            } catch (e: Exception) {
                Log.w(TAG, "最大图片检测异常: ${e.message}")
                return CoverResult.failure("最大图片检测异常")
            }
        }
        
        /**
         * 通过HTML中的图片检测封面
         */
        private fun detectByHtmlImages(zipFile: ZipFile, opfXml: String): CoverResult {
            try {
                // 查找HTML文件
                val htmlPattern = Pattern.compile("""<item[^>]*href="([^"]*?\.html?)"[^>]*>""", Pattern.CASE_INSENSITIVE)
                val htmlMatcher = htmlPattern.matcher(opfXml)
                
                while (htmlMatcher.find()) {
                    val htmlPath = htmlMatcher.group(1)
                    val fullPath = resolvePath(htmlPath)
                    
                    val htmlEntry = zipFile.getEntry(fullPath)
                    if (htmlEntry != null) {
                        try {
                            val htmlContent = zipFile.getInputStream(htmlEntry).bufferedReader().use { it.readText() }
                            
                            // 在HTML中查找图片
                            val imgPattern = Pattern.compile("""<img[^>]*src="([^"]*?)"[^>]*>""", Pattern.CASE_INSENSITIVE)
                            val imgMatcher = imgPattern.matcher(htmlContent)
                            
                            if (imgMatcher.find()) {
                                val imgPath = imgMatcher.group(1)
                                val imgFullPath = resolveImagePath(imgPath, fullPath)
                                
                                if (isValidImageFile(zipFile, imgFullPath)) {
                                    val bitmap = loadImageFromZip(zipFile, imgFullPath)
                                    if (bitmap != null) {
                                        return CoverResult.success(imgFullPath, bitmap, CoverDetectionMethod.IMAGE_IN_HTML)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "HTML解析异常: ${e.message}")
                        }
                    }
                }
                
                return CoverResult.failure("HTML图片检测失败")
            } catch (e: Exception) {
                Log.w(TAG, "HTML图片检测异常: ${e.message}")
                return CoverResult.failure("HTML图片检测异常")
            }
        }
        
        /**
         * 模糊搜索检测封面
         */
        private fun detectByFuzzySearch(zipFile: ZipFile, opfXml: String): CoverResult {
            try {
                val entries = zipFile.entries()
                val imageEntries = mutableListOf<ZipEntry>()
                
                // 收集所有图片文件
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (isImageFile(entry.name)) {
                        imageEntries.add(entry)
                    }
                }
                
                // 按文件名相似度排序
                val sortedEntries = imageEntries.sortedByDescending { entry ->
                    calculateCoverSimilarity(entry.name)
                }
                
                // 尝试加载相似度最高的图片
                for (entry in sortedEntries.take(5)) {
                    val bitmap = loadImageFromZip(zipFile, entry.name)
                    if (bitmap != null) {
                        return CoverResult.success(entry.name, bitmap, CoverDetectionMethod.FUZZY_SEARCH)
                    }
                }
                
                return CoverResult.failure("模糊搜索检测失败")
            } catch (e: Exception) {
                Log.w(TAG, "模糊搜索检测异常: ${e.message}")
                return CoverResult.failure("模糊搜索检测异常")
            }
        }
        
        /**
         * 基础封面检测（降级方案）
         */
        private fun fallbackCoverDetection(zipFile: ZipFile): CoverResult {
            try {
                val entries = zipFile.entries()
                val imageEntries = mutableListOf<ZipEntry>()
                
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (isImageFile(entry.name)) {
                        imageEntries.add(entry)
                    }
                }
                
                if (imageEntries.isNotEmpty()) {
                    // 选择第一个图片
                    val firstImage = imageEntries.first()
                    val bitmap = loadImageFromZip(zipFile, firstImage.name)
                    if (bitmap != null) {
                        return CoverResult.success(firstImage.name, bitmap, CoverDetectionMethod.FIRST_IMAGE_FILE)
                    }
                }
                
                return CoverResult.failure("基础检测失败")
            } catch (e: Exception) {
                Log.w(TAG, "基础检测异常: ${e.message}")
                return CoverResult.failure("基础检测异常")
            }
        }
        
        /**
         * 提取OPF文件内容
         */
        private fun extractOpfContent(zipFile: ZipFile): String? {
            return try {
                // 查找container.xml
                val containerEntry = zipFile.getEntry("META-INF/container.xml")
                if (containerEntry == null) return null
                
                val containerXml = zipFile.getInputStream(containerEntry).bufferedReader().use { it.readText() }
                
                // 提取根文件路径
                val rootPathPattern = Pattern.compile("""<rootfile.*?full-path="([^"]*?)".*?>""")
                val rootMatcher = rootPathPattern.matcher(containerXml)
                
                if (rootMatcher.find()) {
                    val rootFilePath = rootMatcher.group(1)
                    val opfEntry = zipFile.getEntry(rootFilePath)
                    
                    if (opfEntry != null) {
                        return zipFile.getInputStream(opfEntry).bufferedReader().use { it.readText() }
                    }
                }
                
                null
            } catch (e: Exception) {
                Log.w(TAG, "OPF内容提取失败: ${e.message}")
                null
            }
        }
        
        /**
         * 解析路径
         */
        private fun resolvePath(path: String): String {
            return if (path.startsWith("/")) path else "OEBPS/$path"
        }
        
        /**
         * 解析HTML中的图片路径
         */
        private fun resolveImagePath(imgPath: String, htmlPath: String): String {
            return if (imgPath.startsWith("/")) {
                imgPath
            } else {
                val htmlDir = htmlPath.substringBeforeLast("/")
                "$htmlDir/$imgPath"
            }
        }
        
        /**
         * 检查是否是有效的图片文件
         */
        private fun isValidImageFile(zipFile: ZipFile, path: String): Boolean {
            val entry = zipFile.getEntry(path)
            return entry != null && isImageFile(path)
        }
        
        /**
         * 检查是否是图片文件
         */
        private fun isImageFile(fileName: String): Boolean {
            val imageExtensions = listOf(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp")
            return imageExtensions.any { fileName.lowercase().endsWith(it) }
        }
        
        /**
         * 从ZIP文件加载图片
         */
        private fun loadImageFromZip(zipFile: ZipFile, path: String): Bitmap? {
            return try {
                val entry = zipFile.getEntry(path)
                if (entry != null) {
                    val inputStream = zipFile.getInputStream(entry)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    bitmap
                } else null
            } catch (e: Exception) {
                Log.w(TAG, "图片加载失败: $path, 错误: ${e.message}")
                null
            }
        }
        
        /**
         * 计算封面相似度
         */
        private fun calculateCoverSimilarity(fileName: String): Int {
            var score = 0
            val lowerFileName = fileName.lowercase()
            
            // 包含"cover"关键词
            if (lowerFileName.contains("cover")) score += 10
            if (lowerFileName.contains("front")) score += 8
            if (lowerFileName.contains("title")) score += 6
            
            // 文件大小权重（假设封面通常较大）
            // 这里可以根据实际需求调整
            
            // 路径权重（封面通常在根目录或images目录）
            if (!fileName.contains("/")) score += 5
            if (fileName.contains("images")) score += 3
            
            return score
        }
    }
}

/**
 * 封面检测结果
 */
data class CoverResult(
    val isSuccess: Boolean,
    val coverPath: String?,
    val bitmap: Bitmap?,
    val method: AdvancedCoverExtractor.Companion.CoverDetectionMethod?,
    val errorMessage: String?
) {
    companion object {
        fun success(path: String, bitmap: Bitmap, method: AdvancedCoverExtractor.Companion.CoverDetectionMethod): CoverResult {
            return CoverResult(true, path, bitmap, method, null)
        }
        
        fun failure(message: String): CoverResult {
            return CoverResult(false, null, null, null, message)
        }
    }
}
