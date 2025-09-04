package com.ibylin.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject
import java.io.File

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
     * 智能分类图书
     */
    fun classifyBook(epubFile: EpubFile): String {
        val metadata = epubFile.metadata ?: return BookCategory.UNKNOWN.displayName
        
        return try {
            // 1. 基于作者分类（优先级最高）
            val authorCategory = classifyByAuthor(metadata.author)
            if (authorCategory != BookCategory.UNKNOWN) {
                return authorCategory.displayName
            }
            
            // 2. 基于标题关键词分类
            val titleCategory = classifyByTitle(metadata.title)
            if (titleCategory != BookCategory.UNKNOWN) {
                return titleCategory.displayName
            }
            
            // 3. 基于描述关键词分类
            val descCategory = classifyByDescription(metadata.description)
            if (descCategory != DescriptionCategory.UNKNOWN) {
                return descCategory.category
            }
            
            // 4. 基于主题词分类（暂时跳过，因为EpubFileMetadata中没有subject字段）
            // val subjectCategory = classifyBySubject(metadata.subject)
            // if (subjectCategory != BookCategory.UNKNOWN) {
            //     return subjectCategory.displayName
            // }
            
            // 5. 基于语言分类（暂时跳过，因为EpubFileMetadata中没有language字段）
            // val languageCategory = classifyByLanguage(metadata.language)
            // if (languageCategory != BookCategory.UNKNOWN) {
            //     return languageCategory.displayName
            // }
            
            BookCategory.UNKNOWN.displayName
            
        } catch (e: Exception) {
            Log.e(TAG, "分类图书失败: ${epubFile.name}", e)
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
            
            categories.optString(bookPath, BookCategory.UNKNOWN.displayName)
        } catch (e: Exception) {
            Log.e(TAG, "获取图书分类失败", e)
            BookCategory.UNKNOWN.displayName
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
     * 批量分类图书
     */
    fun classifyBooks(context: Context, books: List<EpubFile>): Map<String, String> {
        val classifications = mutableMapOf<String, String>()
        
        books.forEach { book ->
            val category = classifyBook(book)
            classifications[book.path] = category
            saveBookCategory(context, book.path, category)
        }
        
        Log.d(TAG, "批量分类完成: ${books.size} 本图书")
        return classifications
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
