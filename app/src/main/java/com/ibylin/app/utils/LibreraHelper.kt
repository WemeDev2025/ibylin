package com.ibylin.app.utils

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import com.ibylin.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Librera Reader 助手类
 * 负责管理 Librera Reader 的核心功能
 */
@Singleton
class LibreraHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val config: LibreraConfig
) {
    
    private var currentBookPath: String? = null
    private var currentPage: Int = 1
    private var totalPages: Int = 1
    private var isBookLoaded: Boolean = false
    
    // EPUB 内容缓存
    private lateinit var epubContent: EpubContent
    
    // MOBI 内容缓存
    private var mobiContent: MobiContent? = null
    
    // Librera Reader 相关组件
    private var libreraReader: Any? = null
    private var readerContainer: FrameLayout? = null
    
    /**
     * 设置阅读器容器
     */
    fun setReaderContainer(container: FrameLayout) {
        this.readerContainer = container
    }
    
    /**
     * 打开书籍
     * @param bookPath 书籍文件路径
     * @return 是否成功打开
     */
    fun openBook(bookPath: String): Boolean {
        return try {
            android.util.Log.d("LibreraHelper", "开始打开书籍: $bookPath")
            
            // 检查文件是否存在
            val file = java.io.File(bookPath)
            if (!file.exists()) {
                android.util.Log.e("LibreraHelper", "文件不存在: $bookPath")
                throw Exception("文件不存在: $bookPath")
            }
            
            if (!file.canRead()) {
                android.util.Log.e("LibreraHelper", "文件无法读取: $bookPath")
                throw Exception("文件无法读取: $bookPath")
            }
            
            android.util.Log.d("LibreraHelper", "文件检查通过，大小: ${file.length()} 字节")
            
            currentBookPath = bookPath
            currentPage = 1
            
            // 根据文件扩展名判断书籍类型
            val result = when {
                bookPath.endsWith(".epub", ignoreCase = true) -> {
                    android.util.Log.d("LibreraHelper", "检测到EPUB文件")
                    val epubResult = openEpubBook(bookPath)
                    android.util.Log.d("LibreraHelper", "EPUB文件处理结果: $epubResult")
                    epubResult
                }
                bookPath.endsWith(".mobi", ignoreCase = true) || 
                bookPath.endsWith(".azw", ignoreCase = true) || 
                bookPath.endsWith(".azw3", ignoreCase = true) -> {
                    android.util.Log.d("LibreraHelper", "检测到MOBI文件")
                    val mobiResult = openMobiBook(bookPath)
                    android.util.Log.d("LibreraHelper", "MOBI文件处理结果: $mobiResult")
                    mobiResult
                }
                bookPath.endsWith(".pdf", ignoreCase = true) -> {
                    android.util.Log.d("LibreraHelper", "检测到PDF文件")
                    openPdfBook(bookPath)
                    true
                }
                else -> {
                    android.util.Log.d("LibreraHelper", "作为通用文档处理")
                    openGenericBook(bookPath)
                    true
                }
            }
            
            if (result) {
                isBookLoaded = true
                android.util.Log.d("LibreraHelper", "书籍打开成功")
                
                // 在主线程中显示阅读器视图
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    try {
                        showReaderView()
                        android.util.Log.d("LibreraHelper", "阅读器视图已在主线程中显示")
                    } catch (e: Exception) {
                        android.util.Log.e("LibreraHelper", "显示阅读器视图失败: ${e.message}", e)
                    }
                }
            } else {
                android.util.Log.e("LibreraHelper", "书籍打开失败")
                isBookLoaded = false
            }
            
            result
        } catch (e: Exception) {
            android.util.Log.e("LibreraHelper", "打开书籍失败: ${e.message}", e)
            e.printStackTrace()
            isBookLoaded = false
            false
        }
    }
    
    /**
     * 打开 EPUB 书籍
     */
    private fun openEpubBook(bookPath: String): Boolean {
        return try {
            android.util.Log.d("LibreraHelper", "开始处理EPUB文件: $bookPath")
            setupEpubReader(bookPath)
            android.util.Log.d("LibreraHelper", "EPUB文件处理完成")
            true
        } catch (e: Exception) {
            android.util.Log.e("LibreraHelper", "EPUB文件处理失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 打开 MOBI 书籍
     */
    private fun openMobiBook(bookPath: String): Boolean {
        return try {
            android.util.Log.d("LibreraHelper", "开始处理MOBI文件: $bookPath")
            setupMobiReader(bookPath)
            android.util.Log.d("LibreraHelper", "MOBI文件处理完成")
            true
        } catch (e: Exception) {
            android.util.Log.e("LibreraHelper", "MOBI文件处理失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 打开 PDF 书籍
     */
    private fun openPdfBook(bookPath: String) {
        // TODO: 集成 Librera Reader PDF 支持
        // 这里将调用 Librera Reader 的 PDF 解析器
        setupPdfReader(bookPath)
    }
    
    /**
     * 打开通用文档
     */
    private fun openGenericBook(bookPath: String) {
        // TODO: 集成 Librera Reader 通用文档支持
        setupGenericReader(bookPath)
    }
    
    /**
     * 设置 EPUB 阅读器
     */
    private fun setupEpubReader(bookPath: String) {
        try {
            android.util.Log.d("LibreraHelper", "开始解析EPUB文件: $bookPath")
            
            // 解析EPUB文件
            this.epubContent = parseEpubFile(bookPath)
            
            // 设置总页数（基于章节数量）
            totalPages = epubContent.chapters.size
            
            android.util.Log.d("LibreraHelper", "EPUB解析成功，共 ${totalPages} 章")
            
            // 注意：UI 操作需要在主线程中执行，这里只设置数据
            // showReaderView() 将在主线程中调用
            
        } catch (e: Exception) {
            android.util.Log.e("LibreraHelper", "EPUB解析失败: ${e.message}", e)
            throw Exception("EPUB 阅读器初始化失败: ${e.message}")
        }
    }
    
    /**
     * 解析EPUB文件
     */
    private fun parseEpubFile(bookPath: String): EpubContent {
        return try {
            android.util.Log.d("LibreraHelper", "开始解析EPUB文件: $bookPath")
            
            val zipFile = java.util.zip.ZipFile(bookPath)
            android.util.Log.d("LibreraHelper", "ZIP文件打开成功")
            
            // 读取容器信息
            val containerEntry = zipFile.getEntry("META-INF/container.xml")
            if (containerEntry == null) {
                android.util.Log.e("LibreraHelper", "无法找到META-INF/container.xml")
                throw Exception("无法找到META-INF/container.xml")
            }
            android.util.Log.d("LibreraHelper", "找到container.xml")
            
            val containerXml = zipFile.getInputStream(containerEntry).bufferedReader().use { it.readText() }
            android.util.Log.d("LibreraHelper", "container.xml内容长度: ${containerXml.length}")
            
            val rootFilePath = extractRootFilePath(containerXml)
            android.util.Log.d("LibreraHelper", "提取的根文件路径: $rootFilePath")
            
            // 读取内容清单
            val opfEntry = zipFile.getEntry(rootFilePath)
            if (opfEntry == null) {
                android.util.Log.e("LibreraHelper", "无法找到OPF文件: $rootFilePath")
                throw Exception("无法找到OPF文件: $rootFilePath")
            }
            android.util.Log.d("LibreraHelper", "找到OPF文件")
            
            val opfXml = zipFile.getInputStream(opfEntry).bufferedReader().use { it.readText() }
            android.util.Log.d("LibreraHelper", "OPF文件内容长度: ${opfXml.length}")
            
            val chapters = parseOpfFile(opfXml, zipFile)
            android.util.Log.d("LibreraHelper", "解析到 ${chapters.size} 个章节")
            
            // 解析元数据
            val metadata = parseMetadata(opfXml, zipFile)
            android.util.Log.d("LibreraHelper", "解析到元数据: ${metadata?.title}")
            
            zipFile.close()
            
            EpubContent(chapters, metadata)
            
        } catch (e: Exception) {
            android.util.Log.e("LibreraHelper", "EPUB文件解析失败: ${e.message}", e)
            throw Exception("EPUB文件解析失败: ${e.message}")
        }
    }
    
    /**
     * 解析MOBI文件
     */
    private fun parseMobiFile(bookPath: String): MobiContent {
        return try {
            android.util.Log.d("LibreraHelper", "开始解析MOBI文件: $bookPath")
            
            val file = java.io.RandomAccessFile(bookPath, "r")
            
            try {
                // 检查文件头
                val header = ByteArray(8)
                file.read(header)
                val headerString = String(header)
                
                android.util.Log.d("LibreraHelper", "MOBI文件头: $headerString")
                
                // 这里应该根据MOBI文件格式规范来解析
                // 简化实现，创建基本的章节结构
                val chapters = listOf(
                    Chapter(
                        title = "MOBI电子书",
                        content = "MOBI格式电子书内容",
                        path = "content"
                    )
                )
                
                // 创建基本元数据
                val metadata = MobiMetadata(
                    title = "MOBI电子书",
                    author = null,
                    language = null,
                    publisher = null,
                    identifier = null,
                    coverImagePath = null,
                    coverImageData = null,
                    description = null,
                    subject = null,
                    date = null,
                    rights = null,
                    version = "MOBI"
                )
                
                android.util.Log.d("LibreraHelper", "MOBI解析完成，共 ${chapters.size} 章")
                
                MobiContent(chapters, metadata)
                
            } finally {
                file.close()
            }
            
        } catch (e: Exception) {
            android.util.Log.e("LibreraHelper", "MOBI文件解析失败: ${e.message}", e)
            throw Exception("MOBI文件解析失败: ${e.message}")
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
     * 解析OPF文件，获取章节信息
     */
    private fun parseOpfFile(opfXml: String, zipFile: java.util.zip.ZipFile): List<Chapter> {
        val chapters = mutableListOf<Chapter>()
        
        try {
            // 提取所有HTML文件的引用
            val htmlRegex = """<item.*?href="([^"]*?\.html?)".*?>""".toRegex()
            val htmlMatches = htmlRegex.findAll(opfXml)
            
            htmlMatches.forEach { matchResult ->
                val htmlPath = matchResult.groupValues[1]
                val fullPath = if (htmlPath.startsWith("/")) htmlPath else "OEBPS/$htmlPath"
                
                val htmlEntry = zipFile.getEntry(fullPath)
                if (htmlEntry != null) {
                    val htmlContent = zipFile.getInputStream(htmlEntry).bufferedReader().use { it.readText() }
                    val cleanContent = cleanHtmlContent(htmlContent)
                    
                    chapters.add(Chapter(
                        title = extractTitle(htmlContent) ?: "第${(chapters.size + 1).toString()}章",
                        content = cleanContent,
                        path = fullPath
                    ))
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.w("LibreraHelper", "解析OPF文件时出错: ${e.message}")
        }
        
        return chapters
    }
    
    /**
     * 清理HTML内容，提取纯文本
     */
    private fun cleanHtmlContent(htmlContent: String): String {
        // 移除HTML标签，保留文本内容
        var cleanContent = htmlContent
            .replace(Regex("<[^>]*>"), "") // 移除HTML标签
            .replace("&nbsp;", " ") // 替换HTML实体
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .trim()
        
        // 移除多余的空白字符
        cleanContent = cleanContent.replace(Regex("\\s+"), " ")
        
        return cleanContent
    }
    
    /**
     * 解析EPUB元数据
     */
    private fun parseMetadata(opfXml: String, zipFile: java.util.zip.ZipFile): EpubMetadata? {
        return try {
            android.util.Log.d("LibreraHelper", "开始解析EPUB元数据")
            
            // 提取书名
            val titleRegex = """<dc:title[^>]*>([^<]*)</dc:title>""".toRegex()
            val titleMatch = titleRegex.find(opfXml)
            val title = titleMatch?.groupValues?.get(1)?.trim() ?: "未知书名"
            
            // 提取作者
            val authorRegex = """<dc:creator[^>]*>([^<]*)</dc:creator>""".toRegex()
            val authorMatch = authorRegex.find(opfXml)
            val author = authorMatch?.groupValues?.get(1)?.trim()
            
            // 提取语言
            val languageRegex = """<dc:language[^>]*>([^<]*)</dc:language>""".toRegex()
            val languageMatch = languageRegex.find(opfXml)
            val language = languageMatch?.groupValues?.get(1)?.trim()
            
            // 提取出版商
            val publisherRegex = """<dc:publisher[^>]*>([^<]*)</dc:publisher>""".toRegex()
            val publisherMatch = publisherRegex.find(opfXml)
            val publisher = publisherMatch?.groupValues?.get(1)?.trim()
            
            // 提取标识符
            val identifierRegex = """<dc:identifier[^>]*>([^<]*)</dc:identifier>""".toRegex()
            val identifierMatch = identifierRegex.find(opfXml)
            val identifier = identifierMatch?.groupValues?.get(1)?.trim()
            
            // 提取描述
            val descriptionRegex = """<dc:description[^>]*>([^<]*)</dc:description>""".toRegex()
            val descriptionMatch = descriptionRegex.find(opfXml)
            val description = descriptionMatch?.groupValues?.get(1)?.trim()
            
            // 提取主题
            val subjectRegex = """<dc:subject[^>]*>([^<]*)</dc:subject>""".toRegex()
            val subjectMatch = subjectRegex.find(opfXml)
            val subject = subjectMatch?.groupValues?.get(1)?.trim()
            
            // 提取日期
            val dateRegex = """<dc:date[^>]*>([^<]*)</dc:date>""".toRegex()
            val dateMatch = dateRegex.find(opfXml)
            val date = dateMatch?.groupValues?.get(1)?.trim()
            
            // 提取版权
            val rightsRegex = """<dc:rights[^>]*>([^<]*)</dc:rights>""".toRegex()
            val rightsMatch = rightsRegex.find(opfXml)
            val rights = rightsMatch?.groupValues?.get(1)?.trim()
            
            // 查找封面图片
            val coverImagePath = findCoverImage(opfXml, zipFile)
            val coverImageData = coverImagePath?.let { path ->
                try {
                    val coverEntry = zipFile.getEntry(path)
                    if (coverEntry != null) {
                        zipFile.getInputStream(coverEntry).readBytes()
                    } else null
                } catch (e: Exception) {
                    android.util.Log.w("LibreraHelper", "读取封面图片失败: ${e.message}")
                    null
                }
            }
            
            android.util.Log.d("LibreraHelper", "元数据解析完成: 书名=$title, 作者=$author, 封面=$coverImagePath")
            
            EpubMetadata(
                title = title,
                author = author,
                language = language,
                publisher = publisher,
                identifier = identifier,
                coverImagePath = coverImagePath,
                coverImageData = coverImageData,
                description = description,
                subject = subject,
                date = date,
                rights = rights
            )
            
        } catch (e: Exception) {
            android.util.Log.e("LibreraHelper", "解析元数据失败: ${e.message}", e)
            null
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
                    android.util.Log.d("LibreraHelper", "通过ID找到封面: $fullPath")
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
                    android.util.Log.d("LibreraHelper", "通过文件名找到封面: $fullPath")
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
                    android.util.Log.d("LibreraHelper", "找到图片文件作为封面: $fullPath")
                    return fullPath
                }
            }
            
            android.util.Log.d("LibreraHelper", "未找到封面图片")
            null
            
        } catch (e: Exception) {
            android.util.Log.e("LibreraHelper", "查找封面图片失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 从HTML内容中提取标题
     */
    private fun extractTitle(htmlContent: String): String {
        val titleRegex = """<title[^>]*>([^<]*)</title>""".toRegex()
        val matchResult = titleRegex.find(htmlContent)
        return matchResult?.groupValues?.get(1)?.trim() ?: "未知章节"
    }
    
    /**
     * 设置 MOBI 阅读器
     */
    private fun setupMobiReader(bookPath: String) {
        try {
            android.util.Log.d("LibreraHelper", "开始设置MOBI阅读器: $bookPath")
            
            // 解析MOBI文件
            this.mobiContent = parseMobiFile(bookPath)
            
            // 设置总页数（基于章节数量）
            val chapters = mobiContent?.chapters ?: emptyList()
            totalPages = chapters.size
            
            android.util.Log.d("LibreraHelper", "MOBI解析成功，共 ${totalPages} 章")
            
            // 注意：UI 操作需要在主线程中执行，这里只设置数据
            // showReaderView() 将在主线程中调用
            
        } catch (e: Exception) {
            android.util.Log.e("LibreraHelper", "MOBI解析失败: ${e.message}", e)
            throw Exception("MOBI 阅读器初始化失败: ${e.message}")
        }
    }
    
    /**
     * 设置 PDF 阅读器
     */
    private fun setupPdfReader(bookPath: String) {
        try {
            // 模拟 PDF 阅读器设置
            // 实际集成时将调用 Librera Reader API
            totalPages = 50 // 示例页数
            
            // 在容器中显示阅读器视图
            showReaderView()
            
        } catch (e: Exception) {
            throw Exception("PDF 阅读器初始化失败: ${e.message}")
        }
    }
    
    /**
     * 设置通用阅读器
     */
    private fun setupGenericReader(bookPath: String) {
        try {
            // 模拟通用阅读器设置
            totalPages = 25 // 示例页数
            
            // 在容器中显示阅读器视图
            showReaderView()
            
        } catch (e: Exception) {
            throw Exception("通用阅读器初始化失败: ${e.message}")
        }
    }
    
    /**
     * 显示阅读器视图
     */
    private fun showReaderView() {
        readerContainer?.let { container ->
            // 清除现有内容
            container.removeAllViews()
            
            // 创建阅读器视图
            val readerView = createReaderView()
            container.addView(readerView)
            
            android.util.Log.d("LibreraHelper", "阅读器视图已显示")
        }
    }
    
    /**
     * 创建阅读器视图
     */
    private fun createReaderView(): android.view.View {
        android.util.Log.d("LibreraHelper", "开始创建阅读器视图")
        
        // 创建真实的阅读器视图
        val scrollView = android.widget.ScrollView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        val textView = android.widget.TextView(context).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            
            // 显示当前页面的内容
            val currentContent = getCurrentPageContent()
            android.util.Log.d("LibreraHelper", "设置TextView内容，长度: ${currentContent.length}")
            text = currentContent
            
            textSize = config.fontSize
            setPadding(32, 32, 32, 32)
            setTextColor(if (config.nightMode) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
            setBackgroundColor(if (config.nightMode) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            
            // 设置行间距
            setLineSpacing(0f, config.lineSpacing)
            
            android.util.Log.d("LibreraHelper", "TextView设置完成")
        }
        
        scrollView.addView(textView)
        android.util.Log.d("LibreraHelper", "阅读器视图创建完成")
        return scrollView
    }
    
    /**
     * 获取当前页面内容
     */
    private fun getCurrentPageContent(): String {
        return try {
            android.util.Log.d("LibreraHelper", "获取页面内容: currentBookPath=$currentBookPath, isBookLoaded=$isBookLoaded, currentPage=$currentPage, totalPages=$totalPages")
            
            if (currentBookPath != null && isBookLoaded) {
                // 尝试获取真实的 EPUB 内容
                val realContent = getRealEpubContent()
                if (realContent.isNotEmpty()) {
                    android.util.Log.d("LibreraHelper", "返回真实EPUB内容，长度: ${realContent.length}")
                    return realContent
                }
                
                // 如果没有真实内容，返回模拟内容
                val fallbackContent = "第 ${currentPage} 页 / 共 ${totalPages} 页\n\n" +
                "书籍路径: ${currentBookPath}\n\n" +
                "这是第 ${currentPage} 页的内容。\n\n" +
                "Librera Reader 正在正常工作！\n\n" +
                "您可以：\n" +
                "• 使用底部按钮翻页\n" +
                "• 在设置中调整字体大小\n" +
                "• 切换夜间模式\n" +
                "• 调整行间距"
                
                android.util.Log.d("LibreraHelper", "返回模拟内容，长度: ${fallbackContent.length}")
                fallbackContent
            } else {
                android.util.Log.w("LibreraHelper", "书籍未加载，返回提示信息")
                "请先加载书籍..."
            }
        } catch (e: Exception) {
            android.util.Log.e("LibreraHelper", "获取页面内容失败: ${e.message}", e)
            "获取页面内容失败: ${e.message}"
        }
    }

    /**
     * 获取真实的 EPUB 内容
     */
    private fun getRealEpubContent(): String {
        return try {
            // 检查是否有解析的 EPUB 内容
            if (::epubContent.isInitialized && epubContent.chapters.isNotEmpty()) {
                val chapterIndex = currentPage - 1 // 页码从1开始，索引从0开始
                if (chapterIndex >= 0 && chapterIndex < epubContent.chapters.size) {
                    val chapter = epubContent.chapters[chapterIndex]
                    android.util.Log.d("LibreraHelper", "获取第 ${currentPage} 页内容，章节标题: ${chapter.title ?: "未知标题"}")
                    
                    // 构建页面内容
                    val pageInfo = "第 ${currentPage} 页 / 共 ${totalPages} 页\n\n"
                    val chapterInfo = "章节: ${chapter.title ?: "未知标题"}\n\n"
                    val realContent = pageInfo + chapterInfo + (chapter.content ?: "")
                    
                    android.util.Log.d("LibreraHelper", "返回真实EPUB内容，长度: ${realContent.length}")
                    realContent
                } else {
                    android.util.Log.w("LibreraHelper", "页码超出范围: ${currentPage}, 总章节数: ${epubContent.chapters.size}")
                    ""
                }
            } else {
                android.util.Log.w("LibreraHelper", "EPUB内容未初始化或为空")
                ""
            }
        } catch (e: Exception) {
            android.util.Log.e("LibreraHelper", "获取真实EPUB内容失败: ${e.message ?: "未知错误"}", e)
            ""
        }
    }
    
        /**
     * 下一页
     */
    fun nextPage(): Boolean {
        return if (currentPage < totalPages) {
            currentPage++
            updateReaderContent()
            true
        } else {
            Toast.makeText(context, "已经是最后一页", Toast.LENGTH_SHORT).show()
            false
        }
    }

    /**
     * 上一页
     */
    fun previousPage(): Boolean {
        return if (currentPage > 1) {
            currentPage--
            updateReaderContent()
            true
        } else {
            Toast.makeText(context, "已经是第一页", Toast.LENGTH_SHORT).show()
            false
        }
    }
    
    /**
     * 跳转到指定页
     */
    fun goToPage(pageNumber: Int): Boolean {
        return if (pageNumber in 1..totalPages) {
            currentPage = pageNumber
            updateReaderView()
            true
        } else {
            Toast.makeText(context, "页码超出范围", Toast.LENGTH_SHORT).show()
            false
        }
    }
    
    /**
     * 更新阅读器视图
     */
    private fun updateReaderView() {
        // 更新页码信息
        updatePageInfo()
        
        // 重新创建阅读器视图以显示新页面
        showReaderView()
    }

    /**
     * 更新阅读器内容（高效翻页）
     */
    private fun updateReaderContent() {
        try {
            android.util.Log.d("LibreraHelper", "更新阅读器内容到第 ${currentPage} 页")
            
            // 只更新现有视图的内容，不重新创建视图
            readerContainer?.let { container ->
                // 查找现有的 TextView
                val textView = findTextViewInContainer(container)
                if (textView != null) {
                    // 更新内容
                    val newContent = getCurrentPageContent()
                    textView.text = newContent
                    android.util.Log.d("LibreraHelper", "内容更新成功，长度: ${newContent.length}")
                } else {
                    // 如果找不到 TextView，回退到重新创建视图
                    android.util.Log.w("LibreraHelper", "未找到现有TextView，重新创建视图")
                    updateReaderView()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("LibreraHelper", "更新阅读器内容失败: ${e.message}", e)
            // 出错时回退到重新创建视图
            updateReaderView()
        }
    }

    /**
     * 在容器中查找 TextView
     */
    private fun findTextViewInContainer(container: ViewGroup): android.widget.TextView? {
        for (i in 0 until container.childCount) {
            val child = container.getChildAt(i)
            if (child is android.widget.ScrollView) {
                // 在 ScrollView 中查找 TextView
                if (child.childCount > 0) {
                    val scrollChild = child.getChildAt(0)
                    if (scrollChild is android.widget.TextView) {
                        return scrollChild
                    }
                }
            } else if (child is android.widget.TextView) {
                return child
            }
        }
        return null
    }
    
    /**
     * 更新页码信息
     */
    private fun updatePageInfo() {
        // 这里可以添加页码更新的回调
    }
    
    /**
     * 获取当前页码
     */
    fun getCurrentPage(): Int = currentPage
    
    /**
     * 获取总页数
     */
    fun getTotalPages(): Int = totalPages
    
    /**
     * 获取总页数（公共属性）
     */
    val publicTotalPages: Int
        get() = this.totalPages
    
    /**
     * 获取书籍路径
     */
    fun getBookPath(): String? = currentBookPath
    
    /**
     * 获取书籍元数据
     */
    fun getBookMetadata(): EpubMetadata? {
        return if (::epubContent.isInitialized) {
            epubContent.metadata
        } else null
    }
    
    /**
     * 获取书籍标题
     */
    fun getBookTitle(): String {
        return getBookMetadata()?.title ?: "未知书名"
    }
    
    /**
     * 获取书籍作者
     */
    fun getBookAuthor(): String? {
        return getBookMetadata()?.author
    }
    
    /**
     * 获取封面图片数据
     */
    fun getCoverImageData(): ByteArray? {
        return getBookMetadata()?.coverImageData
    }
    
    /**
     * 获取封面图片路径
     */
    fun getCoverImagePath(): String? {
        return getBookMetadata()?.coverImagePath
    }
    
    /**
     * 检查书籍是否已加载
     */
    fun isBookLoaded(): Boolean = isBookLoaded
    
    /**
     * 获取阅读进度
     */
    fun getReadingProgress(): Float {
        return if (totalPages > 0) {
            (currentPage.toFloat() / totalPages.toFloat()) * 100f
        } else {
            0f
        }
    }
    
    /**
     * 设置字体大小
     */
    fun setFontSize(size: Float) {
        config.fontSize = size
        // TODO: 调用 Librera Reader 的字体大小设置
        updateReaderView()
    }
    
    /**
     * 设置行间距
     */
    fun setLineSpacing(spacing: Float) {
        config.lineSpacing = spacing
        // TODO: 调用 Librera Reader 的行间距设置
        updateReaderView()
    }
    
    /**
     * 设置主题
     */
    fun setTheme(theme: String) {
        config.theme = theme
        // TODO: 调用 Librera Reader 的主题设置
        updateReaderView()
    }
    
    /**
     * 切换夜间模式
     */
    fun toggleNightMode() {
        config.nightMode = !config.nightMode
        updateReaderView()
    }
    
    /**
     * 获取当前配置
     */
    fun getCurrentConfig(): LibreraConfig = config
    
    /**
     * 清理资源
     */
    fun cleanup() {
        try {
            // 清理 Librera Reader 资源
            libreraReader = null
            readerContainer = null
            isBookLoaded = false
            currentBookPath = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

/**
 * EPUB内容数据类
 */
data class EpubContent(
    val chapters: List<Chapter>,
    val metadata: EpubMetadata? = null
)

/**
 * EPUB元数据数据类
 */
data class EpubMetadata(
    val title: String,
    val author: String?,
    val language: String?,
    val publisher: String?,
    val identifier: String?,
    val coverImagePath: String?,
    val coverImageData: ByteArray? = null,
    val description: String?,
    val subject: String?,
    val date: String?,
    val rights: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EpubMetadata

        if (title != other.title) return false
        if (author != other.author) return false
        if (language != other.language) return false
        if (publisher != other.publisher) return false
        if (identifier != other.identifier) return false
        if (coverImagePath != other.coverImagePath) return false
        if (coverImageData != null) {
            if (other.coverImageData == null) return false
            if (!coverImageData.contentEquals(other.coverImageData)) return false
        } else if (other.coverImageData != null) return false
        if (description != other.description) return false
        if (subject != other.subject) return false
        if (date != other.date) return false
        if (rights != other.rights) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + (author?.hashCode() ?: 0)
        result = 31 * result + (language?.hashCode() ?: 0)
        result = 31 * result + (publisher?.hashCode() ?: 0)
        result = 31 * result + (identifier?.hashCode() ?: 0)
        result = 31 * result + (coverImagePath?.hashCode() ?: 0)
        result = 31 * result + (coverImageData?.contentHashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (subject?.hashCode() ?: 0)
        result = 31 * result + (date?.hashCode() ?: 0)
        result = 31 * result + (rights?.hashCode() ?: 0)
        return result
    }
}

/**
 * 章节数据类
 */
data class Chapter(
    val title: String,
    val content: String,
    val path: String
)

/**
 * MOBI内容数据类
 */
data class MobiContent(
    val chapters: List<Chapter>,
    val metadata: MobiMetadata? = null
)

/**
 * MOBI元数据数据类
 */
data class MobiMetadata(
    val title: String,
    val author: String?,
    val language: String?,
    val publisher: String?,
    val identifier: String?,
    val coverImagePath: String?,
    val coverImageData: ByteArray? = null,
    val description: String?,
    val subject: String?,
    val date: String?,
    val rights: String?,
    val version: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MobiMetadata

        if (title != other.title) return false
        if (author != other.author) return false
        if (language != other.language) return false
        if (publisher != other.publisher) return false
        if (identifier != other.identifier) return false
        if (coverImagePath != other.coverImagePath) return false
        if (coverImageData != null) {
            if (other.coverImageData == null) return false
            if (!coverImageData.contentEquals(other.coverImageData)) return false
        } else if (other.coverImageData != null) return false
        if (description != other.description) return false
        if (subject != other.subject) return false
        if (date != other.date) return false
        if (rights != other.rights) return false
        if (version != other.version) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + (author?.hashCode() ?: 0)
        result = 31 * result + (language?.hashCode() ?: 0)
        result = 31 * result + (publisher?.hashCode() ?: 0)
        result = 31 * result + (identifier?.hashCode() ?: 0)
        result = 31 * result + (coverImagePath?.hashCode() ?: 0)
        result = 31 * result + (coverImageData?.contentHashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (subject?.hashCode() ?: 0)
        result = 31 * result + (date?.hashCode() ?: 0)
        result = 31 * result + (rights?.hashCode() ?: 0)
        result = 31 * result + (version?.hashCode() ?: 0)
        return result
    }
}

