package com.ibylin.app.utils

import android.util.Log

object SearchHelper {
    
    private const val TAG = "SearchHelper"
    
    /**
     * 中文到英文的常用词汇映射
     */
    private val chineseToEnglishMap = mapOf(
        // 自然景观
        "高山" to "mountain",
        "大山" to "mountain",
        "山峰" to "mountain peak",
        "海洋" to "ocean",
        "大海" to "ocean",
        "海" to "ocean",
        "河流" to "river",
        "湖泊" to "lake",
        "森林" to "forest",
        "树木" to "tree",
        "花朵" to "flower",
        "草地" to "grassland",
        "沙漠" to "desert",
        "雪" to "snow",
        "雨" to "rain",
        "云" to "cloud",
        "天空" to "sky",
        "太阳" to "sun",
        "月亮" to "moon",
        "星星" to "star",
        
        // 风格和主题
        "科幻" to "sci-fi science fiction",
        "科幻小说" to "sci-fi science fiction",
        "奇幻" to "fantasy",
        "魔幻" to "magical",
        "古风" to "vintage traditional",
        "传统" to "traditional",
        "现代" to "modern",
        "简约" to "minimal simple",
        "简单" to "simple",
        "复杂" to "complex",
        "抽象" to "abstract",
        "写实" to "realistic",
        "卡通" to "cartoon",
        "动漫" to "anime",
        "手绘" to "hand drawn",
        "油画" to "oil painting",
        "水彩" to "watercolor",
        "素描" to "sketch",
        
        // 颜色
        "红色" to "red",
        "蓝色" to "blue",
        "绿色" to "green",
        "黄色" to "yellow",
        "紫色" to "purple",
        "橙色" to "orange",
        "粉色" to "pink",
        "黑色" to "black",
        "白色" to "white",
        "灰色" to "gray",
        "金色" to "gold",
        "银色" to "silver",
        "棕色" to "brown",
        
        // 情感和氛围
        "温暖" to "warm",
        "寒冷" to "cold",
        "神秘" to "mysterious",
        "浪漫" to "romantic",
        "激情" to "passionate",
        "平静" to "calm peaceful",
        "宁静" to "serene",
        "活力" to "energetic",
        "优雅" to "elegant",
        "豪华" to "luxury",
        "古典" to "classical",
        "时尚" to "fashion trendy",
        "复古" to "retro vintage",
        
        // 书籍相关
        "封面" to "cover",
        "书籍" to "book",
        "阅读" to "reading",
        "知识" to "knowledge",
        "智慧" to "wisdom",
        "学习" to "learning",
        "教育" to "education",
        "文学" to "literature",
        "艺术" to "art",
        "设计" to "design",
        "创意" to "creative",
        "灵感" to "inspiration",
        
        // 其他常用词汇
        "城市" to "city urban",
        "建筑" to "architecture building",
        "人物" to "people person",
        "动物" to "animal",
        "植物" to "plant",
        "食物" to "food",
        "音乐" to "music",
        "舞蹈" to "dance",
        "运动" to "sports",
        "旅行" to "travel",
        "冒险" to "adventure",
        "探索" to "explore exploration"
    )
    
    /**
     * 智能搜索关键词转换
     * 将中文关键词转换为英文，提高搜索效果
     */
    fun convertToEnglishSearch(query: String): String {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return ""
        
        // 检查是否包含中文字符
        if (!containsChinese(trimmedQuery)) {
            // 如果不包含中文，直接返回原查询
            return "$trimmedQuery book cover"
        }
        
        // 尝试直接匹配完整的中文词汇
        val directMatch = chineseToEnglishMap[trimmedQuery]
        if (directMatch != null) {
            Log.d(TAG, "直接匹配: $trimmedQuery -> $directMatch")
            return "$directMatch book cover"
        }
        
        // 尝试部分匹配
        val convertedQuery = convertPartialChinese(trimmedQuery)
        Log.d(TAG, "部分转换: $trimmedQuery -> $convertedQuery")
        
        return "$convertedQuery book cover"
    }
    
    /**
     * 检查字符串是否包含中文字符
     */
    private fun containsChinese(text: String): Boolean {
        return text.any { it.code in 0x4E00..0x9FFF }
    }
    
    /**
     * 部分中文转换
     * 将查询中的中文词汇替换为对应的英文
     */
    private fun convertPartialChinese(query: String): String {
        var result = query
        
        // 按长度降序排序，优先匹配长词汇
        val sortedEntries = chineseToEnglishMap.entries.sortedByDescending { it.key.length }
        
        for ((chinese, english) in sortedEntries) {
            if (result.contains(chinese)) {
                result = result.replace(chinese, english)
                Log.d(TAG, "替换: $chinese -> $english")
            }
        }
        
        return result
    }
    
    /**
     * 获取搜索建议
     * 根据输入提供相关的搜索建议
     */
    fun getSearchSuggestions(input: String): List<String> {
        val suggestions = mutableListOf<String>()
        val trimmedInput = input.trim()
        
        if (trimmedInput.isEmpty()) {
            // 提供一些通用的搜索建议
            suggestions.addAll(listOf(
                "科幻封面", "简约设计", "蓝色背景", "古风插画",
                "mountain", "ocean", "sci-fi", "minimal"
            ))
        } else {
            // 根据输入提供相关建议
            if (containsChinese(trimmedInput)) {
                // 中文输入，提供英文建议
                val englishQuery = convertToEnglishSearch(trimmedInput)
                suggestions.add(englishQuery)
                suggestions.add("$trimmedInput 封面")
                suggestions.add("$trimmedInput 设计")
            } else {
                // 英文输入，提供中文建议
                suggestions.add("$trimmedInput cover")
                suggestions.add("$trimmedInput design")
                suggestions.add("$trimmedInput art")
            }
        }
        
        return suggestions.distinct()
    }
    
    /**
     * 优化搜索查询
     * 添加一些通用的搜索词来提高结果数量
     */
    fun optimizeSearchQuery(query: String): String {
        val baseQuery = convertToEnglishSearch(query)
        
        // 如果查询太短，添加一些通用词汇
        if (baseQuery.split(" ").size < 3) {
            return "$baseQuery design art"
        }
        
        return baseQuery
    }
    
    /**
     * 生成多个搜索策略，提高结果质量
     */
    fun generateSearchStrategies(query: String): List<String> {
        val strategies = mutableListOf<String>()
        val trimmedQuery = query.trim()
        
        if (trimmedQuery.isEmpty()) return strategies
        
        // 策略1：精确搜索（最相关）
        val exactQuery = convertToEnglishSearch(trimmedQuery)
        strategies.add(exactQuery)
        
        // 策略2：添加"cover"关键词
        if (!exactQuery.contains("cover")) {
            strategies.add("$exactQuery cover")
        }
        
        // 策略3：添加"book"关键词
        if (!exactQuery.contains("book")) {
            strategies.add("$exactQuery book")
        }
        
        // 策略4：更精确的封面搜索
        strategies.add("$exactQuery book cover design")
        
        // 策略5：如果包含中文，尝试纯英文搜索
        if (containsChinese(trimmedQuery)) {
            val englishOnly = convertPartialChinese(trimmedQuery)
            strategies.add("$englishOnly cover art")
        }
        
        return strategies.distinct()
    }
    
    /**
     * 智能搜索优化
     * 针对特定关键词提供更精确的搜索策略
     */
    fun getOptimizedSearchQuery(query: String): String {
        val trimmedQuery = query.trim().lowercase()
        
        // 针对特定关键词的优化策略
        when {
            trimmedQuery.contains("bike") || trimmedQuery.contains("自行车") -> {
                return "bicycle bike transportation cover"
            }
            trimmedQuery.contains("car") || trimmedQuery.contains("汽车") -> {
                return "car automobile vehicle cover"
            }
            trimmedQuery.contains("mountain") || trimmedQuery.contains("山") -> {
                return "mountain landscape nature cover"
            }
            trimmedQuery.contains("ocean") || trimmedQuery.contains("海") -> {
                return "ocean sea water landscape cover"
            }
            trimmedQuery.contains("sci-fi") || trimmedQuery.contains("科幻") -> {
                return "science fiction futuristic technology cover"
            }
            trimmedQuery.contains("fantasy") || trimmedQuery.contains("奇幻") -> {
                return "fantasy magical mystical cover"
            }
            trimmedQuery.contains("minimal") || trimmedQuery.contains("简约") -> {
                return "minimal simple clean design cover"
            }
            else -> {
                // 默认优化策略
                val baseQuery = convertToEnglishSearch(query)
                return "$baseQuery cover design"
            }
        }
    }
    
    /**
     * 优化书名显示：如果标题包含《书名》副标题格式，只显示《书名》部分
     */
    fun optimizeBookTitleForDisplay(title: String): String {
        if (title.isBlank()) return title
        
        // 匹配《书名》副标题 格式
        val regex = """《([^》]+)》""".toRegex()
        val matchResult = regex.find(title)
        
        return if (matchResult != null) {
            // 找到《书名》格式，只返回《书名》部分
            val bookName = matchResult.groupValues[1]
            "《$bookName》"
        } else {
            // 没有找到《书名》格式，返回原标题
            title
        }
    }
    
    /**
     * 测试书名优化功能
     */
    fun testTitleOptimization() {
        val testCases = listOf(
            "《三体》刘慈欣科幻小说" to "《三体》",
            "《活着》余华作品" to "《活着》",
            "《百年孤独》加西亚·马尔克斯" to "《百年孤独》",
            "《红楼梦》曹雪芹著" to "《红楼梦》",
            "《西游记》吴承恩" to "《西游记》",
            "平凡的世界" to "平凡的世界",
            "1984" to "1984",
            "The Great Gatsby" to "The Great Gatsby"
        )
        
        testCases.forEach { (input, expected) ->
            val result = optimizeBookTitleForDisplay(input)
            Log.d(TAG, "测试: '$input' -> '$result' (期望: '$expected')")
            assert(result == expected) { "书名优化失败: '$input' 应该得到 '$expected'，但得到了 '$result'" }
        }
        
        Log.d(TAG, "所有书名优化测试通过！")
    }
}
