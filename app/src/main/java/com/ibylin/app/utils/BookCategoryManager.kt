package com.ibylin.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 智能图书分类管理器
 * 基于EPUB元数据自动分类图书
 */
object BookCategoryManager {
    
    private const val TAG = "BookCategoryManager"
    private const val PREFS_NAME = "book_categories"
    private const val KEY_CATEGORIES = "categories"
    private const val KEY_CATEGORY_STATS = "category_stats"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 智能分类图书 - 增强版
     */
    suspend fun classifyBook(epubFile: EpubFile): String = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔍 开始分类图书: ${epubFile.name}")
        
        // 即使metadata为null，也要尝试从文件名分析
        val metadata = epubFile.metadata
        if (metadata != null) {
            Log.d(TAG, "📚 元数据: title='${metadata.title}', author='${metadata.author}', description='${metadata.description}'")
        } else {
            Log.d(TAG, "⚠️ 元数据为空，将使用文件名进行分析")
        }
        
        try {
            // 1. 基于文件名分析（新增，优先级最高）
            val fileNameCategory = classifyByFileName(epubFile.name)
            Log.d(TAG, "📁 文件名分类结果: '${epubFile.name}' -> ${fileNameCategory.displayName}")
            if (fileNameCategory != BookCategory.UNKNOWN) {
                Log.d(TAG, "✅ 通过文件名分类成功: ${fileNameCategory.displayName}")
                return@withContext fileNameCategory.displayName
            }
            
            // 1.5. 基于百度百科爬虫分类（优先级第二）
            try {
                val baiduCategory = classifyByBaiduBaike(epubFile.name, metadata?.title)
                Log.d(TAG, "🌐 百度百科爬虫分类结果: '${epubFile.name}' -> ${baiduCategory}")
                if (baiduCategory != BookCategory.UNKNOWN.displayName) {
                    Log.d(TAG, "✅ 通过百度百科爬虫分类成功: ${baiduCategory}")
                    return@withContext baiduCategory
                }
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ 百度百科爬虫分类失败: ${e.message}")
            }
            
            // 2. 基于作者分类
            if (metadata != null) {
                val authorCategory = classifyByAuthor(metadata.author)
                Log.d(TAG, "👤 作者分类结果: ${metadata.author} -> ${authorCategory.displayName}")
                if (authorCategory != BookCategory.UNKNOWN) {
                    Log.d(TAG, "✅ 通过作者分类成功: ${authorCategory.displayName}")
                    return@withContext authorCategory.displayName
                }
                
                // 3. 基于标题关键词分类
                val titleCategory = classifyByTitle(metadata.title)
                Log.d(TAG, "📖 标题分类结果: '${metadata.title}' -> ${titleCategory.displayName}")
                if (titleCategory != BookCategory.UNKNOWN) {
                    Log.d(TAG, "✅ 通过标题分类成功: ${titleCategory.displayName}")
                    return@withContext titleCategory.displayName
                }
                
                // 4. 基于描述关键词分类
                val descCategory = classifyByDescription(metadata.description)
                Log.d(TAG, "📝 描述分类结果: '${metadata.description}' -> ${descCategory.category}")
                if (descCategory != DescriptionCategory.UNKNOWN) {
                    Log.d(TAG, "✅ 通过描述分类成功: ${descCategory.category}")
                    return@withContext descCategory.category
                }
                
                // 5. 基于语言分类
                val languageCategory = classifyByLanguageFromContent(metadata.title, metadata.description)
                Log.d(TAG, "🌐 语言分类结果: title='${metadata.title}', description='${metadata.description}' -> ${languageCategory.displayName}")
                if (languageCategory != BookCategory.UNKNOWN) {
                    Log.d(TAG, "✅ 通过语言分类成功: ${languageCategory.displayName}")
                    return@withContext languageCategory.displayName
                }
            }
            
            Log.d(TAG, "❌ 所有分类方法都失败，返回未分类")
            return@withContext BookCategory.UNKNOWN.displayName
            
        } catch (e: Exception) {
            Log.e(TAG, "分类图书失败: ${epubFile.name}", e)
            return@withContext BookCategory.UNKNOWN.displayName
        }
    }
    
    /**
     * 基于文件名分类 - 新增方法
     */
    private fun classifyByFileName(fileName: String): BookCategory {
        if (fileName.isBlank()) return BookCategory.UNKNOWN
        
        val lowerFileName = fileName.lowercase()
        Log.d(TAG, "🔍 分析文件名: '$fileName'")
        
        return when {
            // 理财/投资类关键词
            lowerFileName.contains("rich dad") || 
            lowerFileName.contains("富爸爸") || 
            lowerFileName.contains("穷爸爸") ||
            lowerFileName.contains("理财") || 
            lowerFileName.contains("投资") || 
            lowerFileName.contains("财富") ||
            lowerFileName.contains("finance") || 
            lowerFileName.contains("money") -> {
                Log.d(TAG, "💰 文件名包含理财关键词")
                BookCategory.FINANCE
            }
            
            // 文学作品集关键词
            lowerFileName.contains("作品全集") || 
            lowerFileName.contains("works") || 
            lowerFileName.contains("全集") ||
            lowerFileName.contains("余华") || 
            lowerFileName.contains("莫言") || 
            lowerFileName.contains("鲁迅") ||
            lowerFileName.contains("literature") -> {
                Log.d(TAG, "📚 文件名包含文学关键词")
                BookCategory.LITERATURE
            }
            
            // 武侠小说关键词
            lowerFileName.contains("武侠") || 
            lowerFileName.contains("金庸") || 
            lowerFileName.contains("古龙") ||
            lowerFileName.contains("梁羽生") || 
            lowerFileName.contains("温瑞安") -> {
                Log.d(TAG, "⚔️ 文件名包含武侠关键词")
                BookCategory.WUXIA
            }
            
            // 仙侠小说关键词
            lowerFileName.contains("仙侠") || 
            lowerFileName.contains("修仙") || 
            lowerFileName.contains("修真") ||
            lowerFileName.contains("萧鼎") || 
            lowerFileName.contains("我吃西红柿") -> {
                Log.d(TAG, "✨ 文件名包含仙侠关键词")
                BookCategory.XIANXIA
            }
            
            // 科幻小说关键词
            lowerFileName.contains("科幻") || 
            lowerFileName.contains("三体") || 
            lowerFileName.contains("刘慈欣") ||
            lowerFileName.contains("science fiction") || 
            lowerFileName.contains("sci-fi") -> {
                Log.d(TAG, "🚀 文件名包含科幻关键词")
                Log.d(TAG, "  文件名: '$fileName'")
                Log.d(TAG, "  小写文件名: '$lowerFileName'")
                Log.d(TAG, "  分类结果: ${BookCategory.SCIENCE_FICTION.displayName}")
                BookCategory.SCIENCE_FICTION
            }
            
            // 言情小说关键词
            lowerFileName.contains("言情") || 
            lowerFileName.contains("爱情") || 
            lowerFileName.contains("琼瑶") ||
            lowerFileName.contains("romance") -> {
                Log.d(TAG, "💕 文件名包含言情关键词")
                BookCategory.ROMANCE
            }
            
            // 都市小说关键词
            lowerFileName.contains("都市") || 
            lowerFileName.contains("现代") || 
            lowerFileName.contains("都市小说") -> {
                Log.d(TAG, "🏙️ 文件名包含都市关键词")
                BookCategory.URBAN_FICTION
            }
            
            // 历史小说关键词
            lowerFileName.contains("历史") || 
            lowerFileName.contains("古代") || 
            lowerFileName.contains("历史小说") ||
            lowerFileName.contains("history") -> {
                Log.d(TAG, "📜 文件名包含历史关键词")
                BookCategory.HISTORY
            }
            
            // 语言分类
            lowerFileName.contains("english") || 
            lowerFileName.contains("英文") -> {
                Log.d(TAG, "🇺🇸 文件名包含英文关键词")
                BookCategory.ENGLISH
            }
            
            lowerFileName.contains("japanese") || 
            lowerFileName.contains("日文") -> {
                Log.d(TAG, "🇯🇵 文件名包含日文关键词")
                BookCategory.JAPANESE
            }
            
            // 默认中文
            lowerFileName.matches(Regex(".*[\\u4e00-\\u9fa5]+.*")) -> {
                Log.d(TAG, "🇨🇳 文件名包含中文字符，分类为中文")
                BookCategory.CHINESE
            }
            
            else -> {
                Log.d(TAG, "❓ 文件名无法识别分类")
                BookCategory.UNKNOWN
            }
        }
    }
    
    /**
     * 基于百度百科的WebView爬虫分类
     */
    private suspend fun classifyByBaiduBaike(fileName: String, title: String?): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🌐 开始百度百科爬虫分类: fileName='$fileName', title='$title'")
            
            // 优先使用标题，其次使用文件名
            val searchQuery = title ?: fileName
            val cleanQuery = cleanSearchQuery(searchQuery)
            
            Log.d(TAG, "🔍 清理后的搜索关键词: '$cleanQuery'")
            
            // 构建百度百科URL
            val baiduUrl = if (cleanQuery.matches(Regex(".*[\\u4e00-\\u9fa5]+.*"))) {
                // 包含中文字符，直接使用
                "https://baike.baidu.com/item/$cleanQuery"
            } else {
                // 纯英文，尝试翻译为中文或使用搜索
                val chineseTitle = translateEnglishTitle(cleanQuery)
                if (chineseTitle != cleanQuery) {
                    Log.d(TAG, "🔄 英文书名翻译: '$cleanQuery' -> '$chineseTitle'")
                    "https://baike.baidu.com/item/$chineseTitle"
                } else {
                    // 如果无法翻译，跳过百度百科搜索
                    Log.d(TAG, "⚠️ 英文书名无法翻译，跳过百度百科搜索: '$cleanQuery'")
                    return@withContext BookCategory.UNKNOWN.displayName
                }
            }
            
            Log.d(TAG, "📚 百度百科URL: $baiduUrl")
            Log.d(TAG, "📚 清理后的查询: $cleanQuery")
            
            // 发送HTTP请求获取页面内容
            val connection = URL(baiduUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            
            Log.d(TAG, "🌐 发送HTTP请求到百度百科...")
            
            if (connection.responseCode == 200) {
                Log.d(TAG, "✅ 百度百科请求成功，开始解析HTML内容")
                val htmlContent = connection.inputStream.bufferedReader().use { it.readText() }
                
                Log.d(TAG, "📄 HTML内容长度: ${htmlContent.length}")
                Log.d(TAG, "📄 HTML内容前500字符: ${htmlContent.take(500)}")
                
                val category = parseBaiduBaikeCategory(htmlContent, cleanQuery)
                
                if (category != BookCategory.UNKNOWN.displayName) {
                    Log.d(TAG, "✅ 百度百科爬虫分类成功: $category")
                    return@withContext category
                } else {
                    Log.d(TAG, "❌ 百度百科爬虫未能提取到有效分类")
                }
            } else {
                Log.w(TAG, "❌ 百度百科请求失败，状态码: ${connection.responseCode}")
                Log.w(TAG, "❌ 响应消息: ${connection.responseMessage}")
            }
            
            Log.d(TAG, "❌ 百度百科爬虫分类失败")
            BookCategory.UNKNOWN.displayName
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 百度百科爬虫分类异常", e)
            Log.e(TAG, "❌ 异常详情: ${e.message}")
            BookCategory.UNKNOWN.displayName
        }
    }
    
    /**
     * 解析百度百科HTML内容，提取分类信息
     */
    private fun parseBaiduBaikeCategory(htmlContent: String, bookTitle: String): String {
        try {
            Log.d(TAG, "🔍 开始解析百度百科HTML内容，书名: '$bookTitle'")
            
            // 1. 查找标题下方的分类描述
            // 匹配模式：<h1>书名</h1> 后面的分类描述
            val titlePattern = Regex("<h1[^>]*>.*?$bookTitle.*?</h1>", RegexOption.DOT_MATCHES_ALL)
            val titleMatch = titlePattern.find(htmlContent)
            
            Log.d(TAG, "🔍 搜索标题模式: <h1[^>]*>.*?$bookTitle.*?</h1>")
            Log.d(TAG, "🔍 标题匹配结果: ${if (titleMatch != null) "找到" else "未找到"}")
            
            if (titleMatch != null) {
                Log.d(TAG, "📖 找到标题匹配: '${titleMatch.value}'")
                val titleEnd = titleMatch.range.last + 1
                val afterTitle = htmlContent.substring(titleEnd)
                
                Log.d(TAG, "📖 标题后的内容前200字符: ${afterTitle.take(200)}")
                
                // 2. 查找分类描述（通常在标题后的第一个段落或div中）
                val categoryPatterns = listOf(
                    Regex("创作的([^，。]+)"), // "创作的网游小说"
                    Regex("([^，。]*小说)"), // "网游小说"
                    Regex("([^，。]*文学)"), // "网络文学"
                    Regex("([^，。]*作品)"), // "文学作品"
                    Regex("([^，。]*书)") // "工具书"
                )
                
                Log.d(TAG, "🔍 开始匹配分类模式...")
                for ((index, pattern) in categoryPatterns.withIndex()) {
                    Log.d(TAG, "🔍 尝试模式 $index: ${pattern.pattern}")
                    val match = pattern.find(afterTitle)
                    if (match != null) {
                        val categoryText = match.groupValues[1].trim()
                        Log.d(TAG, "📖 找到分类描述: '$categoryText'")
                        
                        // 3. 映射到应用分类
                        val mappedCategory = mapBaiduCategoryToAppCategory(categoryText)
                        if (mappedCategory != BookCategory.UNKNOWN.displayName) {
                            Log.d(TAG, "✅ 分类映射成功: '$categoryText' -> '$mappedCategory'")
                            return mappedCategory
                        } else {
                            Log.d(TAG, "❓ 分类映射失败: '$categoryText' -> 未分类")
                        }
                    } else {
                        Log.d(TAG, "❌ 模式 $index 未匹配")
                    }
                }
            } else {
                Log.d(TAG, "❌ 未找到标题匹配，尝试其他方法")
            }
            
            // 4. 如果没找到，尝试从页面内容中搜索关键词
            Log.d(TAG, "🔍 尝试从页面内容搜索关键词...")
            val contentCategory = searchCategoryInContent(htmlContent)
            if (contentCategory != BookCategory.UNKNOWN.displayName) {
                Log.d(TAG, "✅ 从页面内容找到分类: '$contentCategory'")
                return contentCategory
            }
            
            Log.d(TAG, "❌ 未能从百度百科提取分类信息")
            return BookCategory.UNKNOWN.displayName
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 解析百度百科HTML失败", e)
            Log.e(TAG, "❌ 异常详情: ${e.message}")
            return BookCategory.UNKNOWN.displayName
        }
    }
    
    /**
     * 将百度百科的分类描述映射到应用分类
     */
    private fun mapBaiduCategoryToAppCategory(baiduCategory: String): String {
        val lowerCategory = baiduCategory.lowercase()
        
        return when {
            // 科幻类
            lowerCategory.contains("科幻") || 
            lowerCategory.contains("science fiction") || 
            lowerCategory.contains("sci-fi") -> BookCategory.SCIENCE_FICTION.displayName
            
            // 武侠类
            lowerCategory.contains("武侠") || 
            lowerCategory.contains("武侠小说") -> BookCategory.WUXIA.displayName
            
            // 仙侠类
            lowerCategory.contains("仙侠") || 
            lowerCategory.contains("修仙") || 
            lowerCategory.contains("修真") -> BookCategory.XIANXIA.displayName
            
            // 言情类
            lowerCategory.contains("言情") || 
            lowerCategory.contains("爱情") || 
            lowerCategory.contains("romance") -> BookCategory.ROMANCE.displayName
            
            // 都市类
            lowerCategory.contains("都市") || 
            lowerCategory.contains("现代") || 
            lowerCategory.contains("网游") || 
            lowerCategory.contains("网络") -> BookCategory.URBAN_FICTION.displayName
            
            // 历史类
            lowerCategory.contains("历史") || 
            lowerCategory.contains("historical") -> BookCategory.HISTORY.displayName
            
            // 文学类
            lowerCategory.contains("文学") || 
            lowerCategory.contains("小说") || 
            lowerCategory.contains("literature") -> BookCategory.LITERATURE.displayName
            
            // 理财类
            lowerCategory.contains("理财") || 
            lowerCategory.contains("投资") || 
            lowerCategory.contains("财富") -> BookCategory.FINANCE.displayName
            
            else -> {
                Log.d(TAG, "❓ 未识别的分类: '$baiduCategory'")
                BookCategory.UNKNOWN.displayName
            }
        }
    }
    
    /**
     * 从页面内容中搜索分类关键词
     */
    private fun searchCategoryInContent(htmlContent: String): String {
        val content = htmlContent.lowercase()
        
        // 搜索常见的分类关键词
        val categoryKeywords = mapOf(
            "科幻" to BookCategory.SCIENCE_FICTION.displayName,
            "武侠" to BookCategory.WUXIA.displayName,
            "仙侠" to BookCategory.XIANXIA.displayName,
            "言情" to BookCategory.ROMANCE.displayName,
            "都市" to BookCategory.URBAN_FICTION.displayName,
            "网游" to BookCategory.URBAN_FICTION.displayName,
            "历史" to BookCategory.HISTORY.displayName,
            "文学" to BookCategory.LITERATURE.displayName,
            "理财" to BookCategory.FINANCE.displayName
        )
        
        for ((keyword, category) in categoryKeywords) {
            if (content.contains(keyword)) {
                Log.d(TAG, "🔍 在页面内容中找到关键词: '$keyword' -> '$category'")
                return category
            }
        }
        
        return BookCategory.UNKNOWN.displayName
    }
    
    /**
     * 翻译英文书名为中文
     */
    private fun translateEnglishTitle(englishTitle: String): String {
        // 常见英文书名的中文翻译
        val translations = mapOf(
            "A Christmas Carol" to "圣诞颂歌",
            "The Three-Body Problem" to "三体",
            "Rich Dad Poor Dad" to "富爸爸穷爸爸",
            "The Great Gatsby" to "了不起的盖茨比",
            "To Kill a Mockingbird" to "杀死一只知更鸟",
            "1984" to "一九八四",
            "Pride and Prejudice" to "傲慢与偏见",
            "The Catcher in the Rye" to "麦田里的守望者",
            "Lord of the Flies" to "蝇王",
            "The Hobbit" to "霍比特人",
            "The Lord of the Rings" to "指环王",
            "Harry Potter" to "哈利波特",
            "The Chronicles of Narnia" to "纳尼亚传奇",
            "Alice in Wonderland" to "爱丽丝梦游仙境",
            "The Little Prince" to "小王子",
            "Gone with the Wind" to "飘",
            "The Old Man and the Sea" to "老人与海",
            "The Sun Also Rises" to "太阳照常升起",
            "For Whom the Bell Tolls" to "丧钟为谁而鸣",
            "The Grapes of Wrath" to "愤怒的葡萄"
        )
        
        val lowerTitle = englishTitle.lowercase().trim()
        for ((english, chinese) in translations) {
            if (lowerTitle.contains(english.lowercase())) {
                Log.d(TAG, "📚 找到英文书名翻译: '$englishTitle' -> '$chinese'")
                return chinese
            }
        }
        
        Log.d(TAG, "❓ 未找到英文书名翻译: '$englishTitle'")
        return englishTitle
    }
    
    /**
     * 清理搜索关键词
     */
    private fun cleanSearchQuery(query: String): String {
        return query
            .replace(Regex("[\\[\\]()（）【】《》\"\"''`~!@#$%^&*+=|\\\\:;\"'<>,.?/]"), " ") // 移除特殊字符
            .replace(Regex("\\s+"), " ") // 合并多个空格
            .trim()
            .take(50) // 限制长度
    }
    
    /**
     * 搜索Open Library API
     */
    private suspend fun searchOpenLibrary(query: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "📚 搜索Open Library: '$query'")
            
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://openlibrary.org/search.json?title=$encodedQuery&limit=1"
            
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible; BookClassifier/1.0)")
            connection.connectTimeout = 5000
            connection.readTimeout = 10000
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                
                val docs = json.optJSONArray("docs")
                if (docs != null && docs.length() > 0) {
                    val firstBook = docs.getJSONObject(0)
                    val subjects = firstBook.optJSONArray("subject")
                    
                    if (subjects != null) {
                        for (i in 0 until subjects.length()) {
                            val subject = subjects.getString(i)
                            val category = mapOpenLibrarySubject(subject)
                            if (category != BookCategory.UNKNOWN.displayName) {
                                Log.d(TAG, "📖 Open Library找到分类: '$subject' -> '$category'")
                                return@withContext category
                            }
                        }
                    }
                }
            }
            
            Log.d(TAG, "❌ Open Library搜索无结果")
            BookCategory.UNKNOWN.displayName
            
        } catch (e: Exception) {
            Log.e(TAG, "Open Library搜索失败", e)
            BookCategory.UNKNOWN.displayName
        }
    }
    
    /**
     * 映射Open Library主题到应用分类
     */
    private fun mapOpenLibrarySubject(subject: String): String {
        val lowerSubject = subject.lowercase()
        
        return when {
            // 文学类
            lowerSubject.contains("fiction") || 
            lowerSubject.contains("novel") || 
            lowerSubject.contains("literature") ||
            lowerSubject.contains("小说") || 
            lowerSubject.contains("文学") -> BookCategory.LITERATURE.displayName
            
            // 科幻类
            lowerSubject.contains("science fiction") || 
            lowerSubject.contains("sci-fi") || 
            lowerSubject.contains("科幻") -> BookCategory.SCIENCE_FICTION.displayName
            
            // 言情类
            lowerSubject.contains("romance") || 
            lowerSubject.contains("love") || 
            lowerSubject.contains("言情") -> BookCategory.ROMANCE.displayName
            
            // 历史类
            lowerSubject.contains("history") || 
            lowerSubject.contains("historical") || 
            lowerSubject.contains("历史") -> BookCategory.HISTORY.displayName
            
            // 理财类
            lowerSubject.contains("business") || 
            lowerSubject.contains("economics") || 
            lowerSubject.contains("finance") || 
            lowerSubject.contains("理财") || 
            lowerSubject.contains("经济") -> BookCategory.FINANCE.displayName
            
            // 武侠类
            lowerSubject.contains("martial arts") || 
            lowerSubject.contains("武侠") -> BookCategory.WUXIA.displayName
            
            // 仙侠类
            lowerSubject.contains("fantasy") || 
            lowerSubject.contains("仙侠") || 
            lowerSubject.contains("修仙") -> BookCategory.XIANXIA.displayName
            
            // 都市类
            lowerSubject.contains("urban") || 
            lowerSubject.contains("都市") -> BookCategory.URBAN_FICTION.displayName
            
            else -> BookCategory.UNKNOWN.displayName
        }
    }
    
    /**
     * 简单的网络内容搜索（备选方案）
     */
    private suspend fun searchWebContent(query: String): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔍 执行简单网络搜索: '$query'")
            
            // 这里可以实现简单的网络搜索逻辑
            // 比如搜索书名 + "分类" 关键词
            val searchQuery = "$query 分类 类型"
            Log.d(TAG, "🔍 搜索关键词: '$searchQuery'")
            
            // 暂时返回未知，实际实现中可以解析搜索结果
            BookCategory.UNKNOWN.displayName
            
        } catch (e: Exception) {
            Log.e(TAG, "网络内容搜索失败", e)
            BookCategory.UNKNOWN.displayName
        }
    }
    
    /**
     * 基于作者分类
     */
    private fun classifyByAuthor(author: String?): BookCategory {
        if (author.isNullOrBlank()) return BookCategory.UNKNOWN
        
        return when {
            // 武侠小说作者
            author.contains("金庸") || author.contains("古龙") || 
            author.contains("梁羽生") || author.contains("温瑞安") ||
            author.contains("黄易") || author.contains("卧龙生") ||
            author.contains("司马翎") || author.contains("诸葛青云") -> BookCategory.WUXIA
            
            // 仙侠小说作者
            author.contains("萧鼎") || author.contains("我吃西红柿") ||
            author.contains("唐家三少") || author.contains("天蚕土豆") ||
            author.contains("辰东") || author.contains("忘语") -> BookCategory.XIANXIA
            
            // 科幻小说作者
            author.contains("刘慈欣") || author.contains("王晋康") ||
            author.contains("何夕") || author.contains("韩松") -> BookCategory.SCIENCE_FICTION
            
            // 言情小说作者
            author.contains("琼瑶") || author.contains("席绢") ||
            author.contains("古灵") || author.contains("楼雨晴") -> BookCategory.ROMANCE
            
            // 都市小说作者
            author.contains("都市") || author.contains("现代") -> BookCategory.URBAN_FICTION
            
            // 历史小说作者
            author.contains("历史") || author.contains("古代") -> BookCategory.HISTORY
            
            // 文学作者
            author.contains("鲁迅") || author.contains("老舍") ||
            author.contains("巴金") || author.contains("茅盾") ||
            author.contains("沈从文") || author.contains("张爱玲") -> BookCategory.LITERATURE
            
            else -> BookCategory.UNKNOWN
        }
    }
    
    /**
     * 基于标题关键词分类
     */
    private fun classifyByTitle(title: String): BookCategory {
        if (title.isBlank()) return BookCategory.UNKNOWN
        
        return when {
            // 武侠关键词
            title.contains("武侠") || title.contains("侠") ||
            title.contains("剑") || title.contains("刀") ||
            title.contains("江湖") || title.contains("武林") ||
            title.contains("少林") || title.contains("武当") ||
            title.contains("射雕") || title.contains("神雕") ||
            title.contains("倚天") || title.contains("天龙") ||
            title.contains("笑傲") || title.contains("鹿鼎") -> BookCategory.WUXIA
            
            // 仙侠关键词
            title.contains("仙侠") || title.contains("修仙") ||
            title.contains("修真") || title.contains("仙") ||
            title.contains("道") || title.contains("魔") ||
            title.contains("神") || title.contains("妖") -> BookCategory.XIANXIA
            
            // 科幻关键词
            title.contains("科幻") || title.contains("未来") ||
            title.contains("科技") || title.contains("机器人") ||
            title.contains("星际") || title.contains("宇宙") ||
            title.contains("时空") || title.contains("基因") -> BookCategory.SCIENCE_FICTION
            
            // 言情关键词
            title.contains("言情") || title.contains("爱情") ||
            title.contains("恋") || title.contains("婚") ||
            title.contains("情") || title.contains("爱") -> BookCategory.ROMANCE
            
            // 都市关键词
            title.contains("都市") || title.contains("现代") ||
            title.contains("都市") || title.contains("城市") -> BookCategory.URBAN_FICTION
            
            // 历史关键词
            title.contains("历史") || title.contains("古代") ||
            title.contains("王朝") || title.contains("皇帝") ||
            title.contains("将军") || title.contains("公主") -> BookCategory.HISTORY
            
            // 文学关键词
            title.contains("文学") || title.contains("小说") ||
            title.contains("散文") || title.contains("诗歌") -> BookCategory.LITERATURE
            
            else -> BookCategory.UNKNOWN
        }
    }
    
    /**
     * 基于描述关键词分类
     */
    private fun classifyByDescription(description: String?): DescriptionCategory {
        if (description.isNullOrBlank()) return DescriptionCategory.UNKNOWN
        
        return when {
            description.contains("武侠") || description.contains("江湖") ||
            description.contains("侠义") || description.contains("武功") -> DescriptionCategory.WUXIA
            
            description.contains("修仙") || description.contains("修真") ||
            description.contains("仙法") || description.contains("法术") -> DescriptionCategory.XIANXIA
            
            description.contains("科幻") || description.contains("未来") ||
            description.contains("科技") || description.contains("机器人") -> DescriptionCategory.SCIENCE_FICTION
            
            description.contains("言情") || description.contains("爱情") ||
            description.contains("浪漫") || description.contains("情感") -> DescriptionCategory.ROMANCE
            
            description.contains("都市") || description.contains("现代") ||
            description.contains("城市") || description.contains("职场") -> DescriptionCategory.URBAN_FICTION
            
            description.contains("历史") || description.contains("古代") ||
            description.contains("王朝") || description.contains("战争") -> DescriptionCategory.HISTORY
            
            description.contains("文学") || description.contains("小说") ||
            description.contains("散文") || description.contains("诗歌") -> DescriptionCategory.LITERATURE
            
            else -> DescriptionCategory.UNKNOWN
        }
    }
    
    /**
     * 基于主题词分类
     */
    private fun classifyBySubject(subject: String?): BookCategory {
        if (subject.isNullOrBlank()) return BookCategory.UNKNOWN
        
        return when {
            subject.contains("武侠") || subject.contains("仙侠") -> BookCategory.WUXIA
            subject.contains("科幻") -> BookCategory.SCIENCE_FICTION
            subject.contains("言情") -> BookCategory.ROMANCE
            subject.contains("都市") -> BookCategory.URBAN_FICTION
            subject.contains("历史") -> BookCategory.HISTORY
            subject.contains("文学") -> BookCategory.LITERATURE
            else -> BookCategory.UNKNOWN
        }
    }
    
    /**
     * 基于语言分类
     */
    private fun classifyByLanguage(language: String?): BookCategory {
        if (language.isNullOrBlank()) return BookCategory.UNKNOWN
        
        return when {
            language.contains("zh") || language.contains("中文") -> BookCategory.CHINESE
            language.contains("en") || language.contains("英文") -> BookCategory.ENGLISH
            language.contains("ja") || language.contains("日文") -> BookCategory.JAPANESE
            else -> BookCategory.UNKNOWN
        }
    }
    
    /**
     * 基于内容判断语言分类（通过标题和描述）
     */
    private fun classifyByLanguageFromContent(title: String?, description: String?): BookCategory {
        val titleText = title ?: ""
        val descText = description ?: ""
        val combinedText = "$titleText $descText"
        
        Log.d(TAG, "🔍 语言分类分析:")
        Log.d(TAG, "  标题: '$titleText'")
        Log.d(TAG, "  描述: '$descText'")
        Log.d(TAG, "  组合文本: '$combinedText'")
        
        // 检查是否包含英文内容
        val hasEnglishContent = combinedText.matches(Regex(".*[a-zA-Z]+.*"))
        val hasChineseContent = combinedText.matches(Regex(".*[\\u4e00-\\u9fa5]+.*"))
        
        Log.d(TAG, "  包含英文内容: $hasEnglishContent")
        Log.d(TAG, "  包含中文内容: $hasChineseContent")
        
        val result = when {
            // 如果包含英文且不包含中文，分类为英文
            hasEnglishContent && !hasChineseContent -> {
                Log.d(TAG, "  ✅ 纯英文内容，分类为英文")
                BookCategory.ENGLISH
            }
            // 如果包含中文且不包含英文，分类为中文
            hasChineseContent && !hasEnglishContent -> {
                Log.d(TAG, "  ✅ 纯中文内容，分类为中文")
                BookCategory.CHINESE
            }
            // 如果同时包含中英文，根据主要语言判断
            hasEnglishContent && hasChineseContent -> {
                // 统计中英文字符数量
                val englishCount = combinedText.count { it.isLetter() && it.code in 65..122 }
                val chineseCount = combinedText.count { it.code in 0x4e00..0x9fa5 }
                
                Log.d(TAG, "  📊 中英混合内容: 英文字符=$englishCount, 中文字符=$chineseCount")
                
                if (englishCount > chineseCount) {
                    Log.d(TAG, "  ✅ 英文为主，分类为英文")
                    BookCategory.ENGLISH
                } else {
                    Log.d(TAG, "  ✅ 中文为主，分类为中文")
                    BookCategory.CHINESE
                }
            }
            else -> {
                Log.d(TAG, "  ❌ 无法判断语言，分类为未分类")
                BookCategory.UNKNOWN
            }
        }
        
        Log.d(TAG, "  最终语言分类结果: ${result.displayName}")
        return result
    }
    
    /**
     * 保存图书分类
     */
    fun saveBookCategory(context: Context, bookPath: String, category: String) {
        try {
            val prefs = getPrefs(context)
            val categoriesJson = prefs.getString(KEY_CATEGORIES, "{}")
            val categories = JSONObject(categoriesJson)
            
            categories.put(bookPath, category)
            prefs.edit().putString(KEY_CATEGORIES, categories.toString()).apply()
            
            // 更新分类统计
            updateCategoryStats(context, category)
            
            Log.d(TAG, "保存图书分类: $bookPath -> $category")
            
            // 特别针对科幻分类进行详细调试
            if (category == "科幻" || File(bookPath).name.lowercase().contains("三体")) {
                Log.d(TAG, "🚀 科幻分类保存详细调试:")
                Log.d(TAG, "  图书路径: $bookPath")
                Log.d(TAG, "  图书名称: ${File(bookPath).name}")
                Log.d(TAG, "  保存的分类: '$category'")
                Log.d(TAG, "  分类长度: ${category.length}")
                Log.d(TAG, "  分类字符: ${category.toCharArray().joinToString()}")
                Log.d(TAG, "  是否等于'科幻': ${category == "科幻"}")
                Log.d(TAG, "  是否等于BookCategory.SCIENCE_FICTION.displayName: ${category == BookCategory.SCIENCE_FICTION.displayName}")
                Log.d(TAG, "  保存后的JSON: ${categories.toString()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存图书分类失败", e)
        }
    }
    
    /**
     * 获取图书分类
     */
    fun getBookCategory(context: Context, bookPath: String): String {
        return try {
            val prefs = getPrefs(context)
            val categoriesJson = prefs.getString(KEY_CATEGORIES, "{}")
            val categories = JSONObject(categoriesJson)
            
            val category = categories.optString(bookPath, BookCategory.UNKNOWN.displayName)
            Log.d(TAG, "🔍 获取图书分类:")
            Log.d(TAG, "  图书路径: $bookPath")
            Log.d(TAG, "  图书名称: ${File(bookPath).name}")
            Log.d(TAG, "  分类结果: $category")
            Log.d(TAG, "  JSON数据: $categoriesJson")
            
            // 特别针对科幻分类进行详细调试
            if (category == "科幻" || File(bookPath).name.lowercase().contains("三体")) {
                Log.d(TAG, "🚀 科幻分类详细调试:")
                Log.d(TAG, "  图书名称: ${File(bookPath).name}")
                Log.d(TAG, "  分类结果: '$category'")
                Log.d(TAG, "  分类长度: ${category.length}")
                Log.d(TAG, "  分类字符: ${category.toCharArray().joinToString()}")
                Log.d(TAG, "  是否等于'科幻': ${category == "科幻"}")
                Log.d(TAG, "  是否等于BookCategory.SCIENCE_FICTION.displayName: ${category == BookCategory.SCIENCE_FICTION.displayName}")
            }
            
            category
        } catch (e: Exception) {
            Log.e(TAG, "❌ 获取图书分类失败", e)
            Log.e(TAG, "❌ 异常详情: ${e.message}")
            BookCategory.UNKNOWN.displayName
        }
    }
    
    /**
     * 调试方法：打印所有已保存的分类
     */
    fun debugPrintAllCategories(context: Context) {
        try {
            val prefs = getPrefs(context)
            val categoriesJson = prefs.getString(KEY_CATEGORIES, "{}")
            val categories = JSONObject(categoriesJson)
            
            Log.d(TAG, "🔍 调试：所有已保存的分类:")
            categories.keys().forEach { key ->
                val category = categories.getString(key)
                val fileName = File(key).name
                Log.d(TAG, "  $fileName -> $category")
            }
        } catch (e: Exception) {
            Log.e(TAG, "调试打印分类失败", e)
        }
    }
    
    /**
     * 获取所有分类
     */
    fun getAllCategories(): List<BookCategory> {
        return BookCategory.values().toList()
    }
    
    /**
     * 获取分类统计
     */
    fun getCategoryStats(context: Context): Map<String, Int> {
        return try {
            val prefs = getPrefs(context)
            val statsJson = prefs.getString(KEY_CATEGORY_STATS, "{}")
            val stats = JSONObject(statsJson)
            
            val result = mutableMapOf<String, Int>()
            val iterator = stats.keys()
            while (iterator.hasNext()) {
                val key = iterator.next()
                result[key] = stats.getInt(key)
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "获取分类统计失败", e)
            emptyMap()
        }
    }
    
    /**
     * 更新分类统计
     */
    private fun updateCategoryStats(context: Context, category: String) {
        try {
            val prefs = getPrefs(context)
            val statsJson = prefs.getString(KEY_CATEGORY_STATS, "{}")
            val stats = JSONObject(statsJson)
            
            val currentCount = stats.optInt(category, 0)
            stats.put(category, currentCount + 1)
            
            prefs.edit().putString(KEY_CATEGORY_STATS, stats.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "更新分类统计失败", e)
        }
    }
    
    /**
     * 批量分类图书 - 支持协程
     */
    suspend fun classifyBooks(context: Context, books: List<EpubFile>): Map<String, String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "🚀 开始批量分类 ${books.size} 本图书")
        val classifications = mutableMapOf<String, String>()
        
        books.forEachIndexed { index, book ->
            Log.d(TAG, "📚 分类第 ${index + 1}/${books.size} 本图书: ${book.name}")
            val category = classifyBook(book)
            classifications[book.path] = category
            saveBookCategory(context, book.path, category)
            Log.d(TAG, "  ✅ 分类完成: ${book.name} -> $category")
        }
        
        Log.d(TAG, "✅ 批量分类完成: ${books.size} 本图书")
        Log.d(TAG, "  分类结果:")
        val categoryCounts = classifications.values.groupingBy { it }.eachCount()
        categoryCounts.forEach { (category, count) ->
            Log.d(TAG, "    $category: ${count}本")
        }
        
        classifications
    }
    
    /**
     * 清除分类数据
     */
    fun clearCategories(context: Context) {
        try {
            val prefs = getPrefs(context)
            prefs.edit().clear().apply()
            Log.d(TAG, "分类数据已清除")
        } catch (e: Exception) {
            Log.e(TAG, "清除分类数据失败", e)
        }
    }
}

/**
 * 图书分类枚举
 */
enum class BookCategory(
    val displayName: String,
    val color: Int,
    val icon: String
) {
    WUXIA("武侠", -0x71B5B53, "⚔️"),
    XIANXIA("仙侠", -0x64A4A4A, "✨"),
    SCIENCE_FICTION("科幻", -0xCB6BA2, "🚀"),
    ROMANCE("言情", -0x18B3C4, "💕"),
    URBAN_FICTION("都市", -0xCB6704, "🏙️"),
    HISTORY("历史", -0x6A5A5A, "📚"),
    LITERATURE("文学", -0xC0634, "📖"),
    FINANCE("理财", -0x3F7F3F, "💰"),
    CHINESE("中文", -0xD1338F, "🇨🇳"),
    ENGLISH("英文", -0xE54363, "🇺🇸"),
    JAPANESE("日文", -0x1981E2, "🇯🇵"),
    UNKNOWN("未分类", -0x807372, "❓");
    
    companion object {
        fun fromDisplayName(name: String): BookCategory {
            return values().find { it.displayName == name } ?: UNKNOWN
        }
    }
}

/**
 * 描述分类（用于描述关键词匹配）
 */
enum class DescriptionCategory(val category: String) {
    WUXIA("武侠"),
    XIANXIA("仙侠"),
    SCIENCE_FICTION("科幻"),
    ROMANCE("言情"),
    URBAN_FICTION("都市"),
    HISTORY("历史"),
    LITERATURE("文学"),
    UNKNOWN("未分类");
}
