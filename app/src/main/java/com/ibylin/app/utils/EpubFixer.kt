package com.ibylin.app.utils

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream

/**
 * EPUB文件自动修复工具
 * 用于修复常见的XML语法错误，如缺少引号、编码问题等
 */
class EpubFixer {
    
    companion object {
        private const val TAG = "EpubFixer"
        
        /**
         * 自动修复EPUB文件
         * @param originalPath 原始EPUB文件路径
         * @return 修复后的EPUB文件路径，如果修复失败返回null
         */
        fun fixEpubFile(originalPath: String): String? {
            return try {
                Log.d(TAG, "开始修复EPUB文件: $originalPath")
                
                val originalFile = File(originalPath)
                if (!originalFile.exists()) {
                    Log.e(TAG, "原始文件不存在: $originalPath")
                    return null
                }
                
                // 创建临时修复后的文件
                val fixedPath = originalPath.replace(".epub", "_fixed.epub")
                val fixedFile = File(fixedPath)
                
                // 修复EPUB文件
                val success = fixEpubContent(originalFile, fixedFile)
                
                if (success) {
                    Log.d(TAG, "EPUB文件修复成功: $fixedPath")
                    
                    // 修复成功后，删除原文件并重命名修复后的文件
                    try {
                        if (originalFile.delete()) {
                            Log.d(TAG, "原文件已删除: $originalPath")
                            
                            // 将修复后的文件重命名为原文件名
                            val finalPath = originalPath
                            if (fixedFile.renameTo(File(finalPath))) {
                                Log.d(TAG, "修复后的文件已重命名: $finalPath")
                                finalPath
                            } else {
                                Log.w(TAG, "重命名失败，返回修复后的文件路径: $fixedPath")
                                fixedPath
                            }
                        } else {
                            Log.w(TAG, "删除原文件失败，返回修复后的文件路径: $fixedPath")
                            fixedPath
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "处理修复后文件时出错", e)
                        fixedPath
                    }
                } else {
                    Log.e(TAG, "EPUB文件修复失败")
                    null
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "修复EPUB文件时出错", e)
                null
            }
        }
        
        /**
         * 修复EPUB内容
         */
        private fun fixEpubContent(originalFile: File, fixedFile: File): Boolean {
            return try {
                val zipInputStream = ZipInputStream(FileInputStream(originalFile))
                val zipOutputStream = ZipOutputStream(FileOutputStream(fixedFile))
                
                var entry: ZipEntry?
                while (zipInputStream.nextEntry.also { entry = it } != null) {
                    val currentEntry = entry!!
                    val entryName = currentEntry.name
                    
                    // 读取条目内容
                    val content = readZipEntryContent(zipInputStream)
                    
                    // 修复HTML/XML文件
                    val fixedContent = if (isHtmlFile(entryName)) {
                        fixHtmlContent(content)
                    } else {
                        content
                    }
                    
                    // 写入修复后的内容
                    val newEntry = ZipEntry(entryName)
                    zipOutputStream.putNextEntry(newEntry)
                    zipOutputStream.write(fixedContent)
                    zipOutputStream.closeEntry()
                }
                
                zipInputStream.close()
                zipOutputStream.close()
                
                true
                
            } catch (e: Exception) {
                Log.e(TAG, "修复EPUB内容时出错", e)
                false
            }
        }
        
        /**
         * 读取ZIP条目内容
         */
        private fun readZipEntryContent(zipInputStream: ZipInputStream): ByteArray {
            val buffer = ByteArrayOutputStream()
            val tempBuffer = ByteArray(1024)
            var bytesRead: Int
            
            while (zipInputStream.read(tempBuffer).also { bytesRead = it } != -1) {
                buffer.write(tempBuffer, 0, bytesRead)
            }
            
            return buffer.toByteArray()
        }
        
        /**
         * 判断是否为HTML文件
         */
        private fun isHtmlFile(entryName: String): Boolean {
            return entryName.endsWith(".html") || 
                   entryName.endsWith(".htm") || 
                   entryName.endsWith(".xhtml")
        }
        
        /**
         * 修复HTML内容中的常见XML语法错误
         */
        private fun fixHtmlContent(content: ByteArray): ByteArray {
            val htmlContent = String(content, Charsets.UTF_8)
            var fixedContent = htmlContent
            
            try {
                // 修复1: href属性缺少引号
                fixedContent = fixHrefAttributes(fixedContent)
                
                // 修复2: src属性缺少引号
                fixedContent = fixSrcAttributes(fixedContent)
                
                // 修复3: 其他常见属性缺少引号
                fixedContent = fixOtherAttributes(fixedContent)
                
                // 修复4: 编码问题
                fixedContent = fixEncodingIssues(fixedContent)
                
                // 修复5: 特殊字符问题
                fixedContent = fixSpecialCharacters(fixedContent)
                
                Log.d(TAG, "HTML内容修复完成")
                
            } catch (e: Exception) {
                Log.e(TAG, "修复HTML内容时出错", e)
            }
            
            return fixedContent.toByteArray(Charsets.UTF_8)
        }
        
        /**
         * 修复href属性缺少引号的问题
         */
        private fun fixHrefAttributes(content: String): String {
            // 修复 href=http://... 为 href="http://..."
            var fixed = content.replace(
                Regex("href=([^\"'\\s>]+)"),
                "href=\"$1\""
            )
            
            // 修复 href=www.xxx.com 为 href="www.xxx.com"
            fixed = fixed.replace(
                Regex("href=([a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})"),
                "href=\"$1\""
            )
            
            return fixed
        }
        
        /**
         * 修复src属性缺少引号的问题
         */
        private fun fixSrcAttributes(content: String): String {
            // 修复 src=xxx 为 src="xxx"
            return content.replace(
                Regex("src=([^\"'\\s>]+)"),
                "src=\"$1\""
            )
        }
        
        /**
         * 修复其他常见属性缺少引号的问题
         */
        private fun fixOtherAttributes(content: String): String {
            var fixed = content
            
            // 修复 class=xxx 为 class="xxx"
            fixed = fixed.replace(
                Regex("class=([^\"'\\s>]+)"),
                "class=\"$1\""
            )
            
            // 修复 id=xxx 为 id="xxx"
            fixed = fixed.replace(
                Regex("id=([^\"'\\s>]+)"),
                "id=\"$1\""
            )
            
            // 修复 style=xxx 为 style="xxx"
            fixed = fixed.replace(
                Regex("style=([^\"'\\s>]+)"),
                "style=\"$1\""
            )
            
            return fixed
        }
        
        /**
         * 修复编码问题
         */
        private fun fixEncodingIssues(content: String): String {
            var fixed = content
            
            // 确保XML声明正确
            if (!fixed.contains("<?xml")) {
                fixed = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n$fixed"
            }
            
            // 修复编码声明
            fixed = fixed.replace(
                Regex("encoding=\"([^\"]*)\""),
                "encoding=\"utf-8\""
            )
            
            return fixed
        }
        
        /**
         * 修复特殊字符问题
         */
        private fun fixSpecialCharacters(content: String): String {
            var fixed = content
            
            // 修复 & 符号（在HTML中应该是 &amp;）
            // 但这里我们保持原样，因为可能是故意的链接
            
            // 修复其他可能的特殊字符问题
            fixed = fixed.replace("&nbsp;", " ")
            
            return fixed
        }
        
        /**
         * 检查EPUB文件是否需要修复
         */
        fun needsFixing(epubPath: String): Boolean {
            return try {
                val file = File(epubPath)
                if (!file.exists()) return false
                
                val zipInputStream = ZipInputStream(FileInputStream(file))
                var entry: ZipEntry?
                var needsFixing = false
                
                while (zipInputStream.nextEntry.also { entry = it } != null) {
                    val currentEntry = entry!!
                    if (isHtmlFile(currentEntry.name)) {
                        val content = readZipEntryContent(zipInputStream)
                        val htmlContent = String(content, Charsets.UTF_8)
                        
                        // 检查是否有常见的XML语法错误
                        if (hasXmlErrors(htmlContent)) {
                            needsFixing = true
                            break
                        }
                    }
                }
                
                zipInputStream.close()
                needsFixing
                
            } catch (e: Exception) {
                Log.e(TAG, "检查EPUB是否需要修复时出错", e)
                false
            }
        }
        
        /**
         * 检查HTML内容是否有XML语法错误
         */
        private fun hasXmlErrors(htmlContent: String): Boolean {
            // 检查href属性是否缺少引号
            if (htmlContent.contains(Regex("href=[^\"'\\s>]"))) {
                return true
            }
            
            // 检查src属性是否缺少引号
            if (htmlContent.contains(Regex("src=[^\"'\\s>]"))) {
                return true
            }
            
            // 检查其他常见属性是否缺少引号
            if (htmlContent.contains(Regex("(class|id|style)=[^\"'\\s>]"))) {
                return true
            }
            
            return false
        }
        
        /**
         * 清理临时修复文件
         */
        fun cleanupTempFile(fixedPath: String) {
            try {
                val tempFile = File(fixedPath)
                if (tempFile.exists()) {
                    tempFile.delete()
                    Log.d(TAG, "临时修复文件已清理: $fixedPath")
                }
            } catch (e: Exception) {
                Log.e(TAG, "清理临时文件时出错", e)
            }
        }
        
        /**
         * 清理重复的修复文件
         * 删除所有 _fixed.epub 文件，保留原文件
         */
        fun cleanupDuplicateFixedFiles(directory: String) {
            try {
                val dir = File(directory)
                if (!dir.exists() || !dir.isDirectory) {
                    Log.w(TAG, "目录不存在或不是目录: $directory")
                    return
                }
                
                val fixedFiles = dir.listFiles { file ->
                    file.name.endsWith("_fixed.epub")
                }
                
                fixedFiles?.forEach { file ->
                    try {
                        if (file.delete()) {
                            Log.d(TAG, "重复的修复文件已删除: ${file.name}")
                        } else {
                            Log.w(TAG, "删除重复文件失败: ${file.name}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "删除重复文件时出错: ${file.name}", e)
                    }
                }
                
                Log.d(TAG, "重复文件清理完成，共删除 ${fixedFiles?.size ?: 0} 个文件")
                
            } catch (e: Exception) {
                Log.e(TAG, "清理重复文件时出错", e)
            }
        }
        
        /**
         * 智能清理：保留修复后的文件，删除原文件
         * 用于清理已经存在的重复文件
         */
        fun smartCleanupDuplicates(directory: String) {
            try {
                val dir = File(directory)
                if (!dir.exists() || !dir.isDirectory) {
                    Log.w(TAG, "目录不存在或不是目录: $directory")
                    return
                }
                
                val allFiles = dir.listFiles { file ->
                    file.name.endsWith(".epub")
                } ?: return
                
                val fileGroups = mutableMapOf<String, MutableList<File>>()
                
                // 按书名分组
                allFiles.forEach { file ->
                    val baseName = file.name.replace("_fixed.epub", ".epub").replace(".epub", "")
                    fileGroups.getOrPut(baseName) { mutableListOf() }.add(file)
                }
                
                // 处理每组文件
                fileGroups.forEach { (baseName, files) ->
                    if (files.size > 1) {
                        Log.d(TAG, "发现重复文件组: $baseName, 文件数量: ${files.size}")
                        
                        // 找到修复后的文件
                        val fixedFile = files.find { it.name.endsWith("_fixed.epub") }
                        val originalFile = files.find { !it.name.endsWith("_fixed.epub") }
                        
                        if (fixedFile != null && originalFile != null) {
                            try {
                                // 删除原文件
                                if (originalFile.delete()) {
                                    Log.d(TAG, "原文件已删除: ${originalFile.name}")
                                    
                                    // 重命名修复后的文件
                                    val newName = originalFile.name
                                    val newFile = File(originalFile.parent, newName)
                                    
                                    if (fixedFile.renameTo(newFile)) {
                                        Log.d(TAG, "修复文件已重命名: ${fixedFile.name} -> $newName")
                                    } else {
                                        Log.w(TAG, "重命名失败: ${fixedFile.name}")
                                    }
                                } else {
                                    Log.w(TAG, "删除原文件失败: ${originalFile.name}")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "处理重复文件时出错: $baseName", e)
                            }
                        }
                    }
                }
                
                Log.d(TAG, "智能清理完成")
                
            } catch (e: Exception) {
                Log.e(TAG, "智能清理重复文件时出错", e)
            }
        }
    }
}
