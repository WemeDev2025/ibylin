package com.ibylin.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject
import java.io.File
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
            
            
            // 2. 基于作者分类
            if (metadata != null) {
                val authorCategory = classifyByAuthor(metadata.author)
                Log.d(TAG, "👤 作者分类结果: ${metadata.author} -> ${authorCategory.displayName}")
                if (authorCategory != BookCategory.UNKNOWN) {
                    Log.d(TAG, "✅ 通过作者分类成功: ${authorCategory.displayName}")
                    return@withContext authorCategory.displayName
                }
                
                // 2. 基于标题关键词分类
                val titleCategory = classifyByTitle(metadata.title)
                Log.d(TAG, "📖 标题分类结果: '${metadata.title}' -> ${titleCategory.displayName}")
                if (titleCategory != BookCategory.UNKNOWN) {
                    Log.d(TAG, "✅ 通过标题分类成功: ${titleCategory.displayName}")
                    return@withContext titleCategory.displayName
                }
                
                // 3. 基于描述关键词分类
                val descCategory = classifyByDescription(metadata.description)
                Log.d(TAG, "📝 描述分类结果: '${metadata.description}' -> ${descCategory.category}")
                if (descCategory != DescriptionCategory.UNKNOWN) {
                    Log.d(TAG, "✅ 通过描述分类成功: ${descCategory.category}")
                    return@withContext descCategory.category
                }
                
                // 4. 基于语言分类
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
     * 基于文件名分类 - 增强版
     * 支持更丰富的关键词匹配和权重计算
     */
    private fun classifyByFileName(fileName: String): BookCategory {
        if (fileName.isBlank()) return BookCategory.UNKNOWN
        
        val lowerFileName = fileName.lowercase()
        Log.d(TAG, "🔍 分析文件名: '$fileName'")
        
        // 使用权重计算提高准确性
        val categoryScores = mutableMapOf<BookCategory, Int>()
        
        // 理财/投资类关键词 - 权重增强
        val financeKeywords = listOf(
            "rich dad", "富爸爸", "穷爸爸", "理财", "投资", "财富", 
            "finance", "money", "股票", "基金", "债券", "经济", "商业",
            // 理财子分类关键词
            "个人理财", "家庭理财", "企业理财", "投资理财", "财富管理", "资产管理",
            "股票投资", "基金投资", "债券投资", "期货投资", "外汇投资", "黄金投资",
            "房地产投资", "房产投资", "地产投资", "商铺投资", "写字楼投资", "住宅投资",
            "创业投资", "风险投资", "天使投资", "私募投资", "公募投资", "机构投资",
            "价值投资", "成长投资", "趋势投资", "技术分析", "基本面分析", "量化投资",
            "股票", "基金", "债券", "期货", "外汇", "黄金", "白银", "原油",
            "房地产", "房产", "地产", "商铺", "写字楼", "住宅", "别墅", "公寓",
            "银行", "保险", "证券", "信托", "私募", "公募", "资管", "理财",
            "储蓄", "存款", "贷款", "信用卡", "消费", "支出", "收入", "预算",
            "税务", "税收", "税务筹划", "税务优化", "税务规划", "税务管理",
            "财务报表", "财务分析", "财务规划", "财务自由", "财务独立", "财务安全",
            "经济学", "金融学", "会计学", "管理学", "市场营销", "商业管理",
            "创业", "企业家", "企业家精神", "商业模式", "商业计划", "商业策略"
        )
        if (financeKeywords.any { lowerFileName.contains(it) }) {
            categoryScores[BookCategory.FINANCE] = 10
            Log.d(TAG, "💰 文件名包含理财关键词")
        }
        
        // 文学作品集关键词 - 权重增强
        val literatureKeywords = listOf(
            "作品全集", "works", "全集", "余华", "莫言", "鲁迅", "literature",
            "围城", "活着", "平凡的世界", "白鹿原", "红高粱", "经典", "名著",
            // 文学子分类关键词
            "现代文学", "当代文学", "古典文学", "古代文学", "近代文学", "现代小说",
            "当代小说", "古典小说", "古代小说", "近代小说", "现实主义", "浪漫主义",
            "现代主义", "后现代主义", "意识流", "魔幻现实主义", "超现实主义", "存在主义",
            "散文", "诗歌", "小说", "戏剧", "剧本", "随笔", "杂文", "评论",
            "传记", "自传", "回忆录", "日记", "书信", "游记", "报告文学", "纪实文学",
            "茅盾文学奖", "诺贝尔文学奖", "鲁迅文学奖", "老舍文学奖", "冰心文学奖",
            "鲁迅", "老舍", "巴金", "茅盾", "沈从文", "张爱玲", "钱钟书", "林语堂",
            "余华", "莫言", "贾平凹", "陈忠实", "路遥", "王小波", "王朔", "苏童",
            "格非", "余秋雨", "史铁生", "汪曾祺", "孙犁", "赵树理", "周立波", "丁玲",
            "冰心", "萧红", "萧军", "端木蕻良", "艾青", "臧克家", "何其芳", "卞之琳",
            "经典", "名著", "传世", "不朽", "永恒", "伟大", "杰出", "优秀",
            "文学史", "文学理论", "文学批评", "文学研究", "文学评论", "文学创作"
        )
        if (literatureKeywords.any { lowerFileName.contains(it) }) {
            categoryScores[BookCategory.LITERATURE] = 10
            Log.d(TAG, "📚 文件名包含文学关键词")
        }
        
        // 武侠小说关键词 - 权重增强
        val wuxiaKeywords = listOf(
            "武侠", "金庸", "古龙", "梁羽生", "温瑞安", "黄易", "卧龙生",
            "仙侠", "修仙", "修真", "萧鼎", "我吃西红柿", "唐家三少", "天蚕土豆",
            "江湖", "武林", "剑客", "侠客", "武功", "内功", "轻功",
            // 武侠子分类关键词
            "玄幻", "奇幻", "魔幻", "东方玄幻", "西方奇幻", "异世", "异界",
            "修炼", "境界", "突破", "渡劫", "飞升", "成仙", "成神", "成圣",
            "功法", "秘籍", "心法", "武技", "神通", "法术", "符咒", "阵法",
            "灵根", "灵脉", "丹田", "经脉", "穴位", "真气", "灵力", "法力",
            "法宝", "法器", "灵器", "仙器", "神器", "圣器", "武器", "装备",
            "丹药", "灵药", "仙药", "神药", "炼器", "炼丹", "制符", "布阵",
            "宗门", "门派", "帮派", "势力", "家族", "皇朝", "帝国", "王朝",
            "师父", "师傅", "弟子", "师兄", "师姐", "师弟", "师妹", "师叔",
            "魔道", "正道", "邪道", "妖道", "鬼道", "佛道", "儒道", "道门",
            "妖兽", "魔兽", "神兽", "圣兽", "灵兽", "凶兽", "异兽", "神兽",
            "秘境", "洞府", "遗迹", "古墓", "禁地", "险地", "福地", "宝地"
        )
        if (wuxiaKeywords.any { lowerFileName.contains(it) }) {
            categoryScores[BookCategory.WUXIA] = 10
            Log.d(TAG, "⚔️ 文件名包含武侠关键词")
        }
        
        // 科幻小说关键词 - 权重增强
        val scifiKeywords = listOf(
            "科幻", "三体", "刘慈欣", "science fiction", "sci-fi", "太空", "星际",
            "未来", "机器人", "AI", "虚拟现实", "时间旅行", "银河", "宇宙",
            // 科幻子分类关键词
            "硬科幻", "软科幻", "太空歌剧", "赛博朋克", "蒸汽朋克", "反乌托邦",
            "外星人", "UFO", "星际战争", "星际迷航", "星球大战", "银河系",
            "时空", "虫洞", "黑洞", "量子", "基因工程", "克隆", "人工智能",
            "机械", "机甲", "飞船", "太空站", "殖民", "星际文明", "超能力",
            "变异", "进化", "末日", "灾难", "核战", "生化", "病毒", "疫苗",
            "纳米", "生物技术", "神经", "意识", "记忆", "梦境", "平行宇宙",
            "多维", "维度", "穿越", "重生", "轮回", "预言", "先知", "超自然"
        )
        if (scifiKeywords.any { lowerFileName.contains(it) }) {
            categoryScores[BookCategory.SCIENCE_FICTION] = 10
            Log.d(TAG, "🚀 文件名包含科幻关键词")
        }
        
        // 言情小说关键词 - 权重增强
        val romanceKeywords = listOf(
            "言情", "爱情", "琼瑶", "romance", "霸道总裁", "甜宠", "虐恋",
            "重生", "穿越", "豪门", "都市", "现代", "都市小说", "总裁",
            // 言情子分类关键词
            "现代言情", "古代言情", "都市言情", "校园言情", "职场言情", "豪门言情",
            "总裁文", "霸总", "高冷", "腹黑", "温柔", "暖男", "男神", "女神",
            "校草", "校花", "学霸", "学渣", "老师", "学生", "医生", "律师",
            "明星", "演员", "歌手", "模特", "设计师", "画家", "作家", "记者",
            "警察", "军人", "特工", "保镖", "司机", "厨师", "服务员", "秘书",
            "青梅竹马", "两小无猜", "一见钟情", "日久生情", "暗恋", "单恋", "失恋",
            "初恋", "热恋", "分手", "复合", "结婚", "离婚", "再婚", "单身",
            "甜文", "虐文", "宠文", "爽文", "苏文", "玛丽苏", "杰克苏", "白莲花",
            "绿茶", "心机", "腹黑", "傲娇", "病娇", "忠犬", "狼狗", "奶狗",
            "重生文", "穿越文", "快穿文", "系统文", "空间文", "末世文", "校园文",
            "娱乐圈", "娱乐圈文", "娱乐圈小说", "明星文", "影帝", "影后", "流量",
            "网红", "直播", "短视频", "综艺", "选秀", "出道", "粉丝", "黑粉"
        )
        if (romanceKeywords.any { lowerFileName.contains(it) }) {
            categoryScores[BookCategory.ROMANCE] = 10
            Log.d(TAG, "💕 文件名包含言情关键词")
        }
        
        // 历史小说关键词 - 权重增强
        val historyKeywords = listOf(
            "历史", "古代", "历史小说", "history", "王朝", "皇帝", "将军",
            "公主", "宫廷", "战争", "古代", "汉朝", "唐朝", "宋朝",
            // 历史子分类关键词
            "历史架空", "架空历史", "穿越历史", "重生历史", "历史军事", "历史权谋",
            "夏朝", "商朝", "周朝", "春秋", "战国", "秦朝", "汉朝", "三国",
            "晋朝", "南北朝", "隋朝", "唐朝", "五代十国", "宋朝", "元朝", "明朝", "清朝",
            "民国", "近代", "现代", "当代", "古代", "上古", "中古", "近古",
            "皇帝", "皇后", "太后", "太子", "皇子", "公主", "郡主", "县主",
            "丞相", "宰相", "尚书", "侍郎", "将军", "元帅", "都督", "都尉",
            "太监", "宫女", "侍卫", "禁军", "御林军", "锦衣卫", "东厂", "西厂",
            "科举", "状元", "榜眼", "探花", "进士", "举人", "秀才", "童生",
            "宫廷", "皇宫", "紫禁城", "后宫", "东宫", "西宫", "冷宫", "御花园",
            "战争", "战役", "战斗", "军事", "兵法", "谋略", "计策", "策略",
            "权谋", "政治", "朝政", "朝堂", "朝会", "朝议", "朝臣", "朝野",
            "江湖", "武林", "门派", "帮派", "绿林", "山贼", "土匪", "盗匪",
            "商贾", "商人", "商队", "贸易", "商业", "经济", "财政", "税收"
        )
        if (historyKeywords.any { lowerFileName.contains(it) }) {
            categoryScores[BookCategory.HISTORY] = 10
            Log.d(TAG, "📜 文件名包含历史关键词")
        }
        
        // 英文图书关键词 - 权重增强
        val englishKeywords = listOf(
            "english", "英文", "English", "ENGLISH", "eng", "ENG",
            // 英文图书常见标题关键词
            "The", "A", "An", "Of", "And", "In", "On", "At", "To", "For",
            "With", "By", "From", "About", "Into", "Through", "During",
            "Before", "After", "Above", "Below", "Between", "Among",
            // 英文图书分类关键词
            "Novel", "Story", "Tale", "Fiction", "Non-fiction", "Biography",
            "Autobiography", "Memoir", "History", "Science", "Technology",
            "Business", "Finance", "Investment", "Marketing", "Management",
            "Psychology", "Philosophy", "Religion", "Politics", "Economics",
            "Literature", "Poetry", "Drama", "Comedy", "Tragedy", "Romance",
            "Mystery", "Thriller", "Horror", "Fantasy", "Adventure", "Action",
            "Crime", "Detective", "Suspense", "Drama", "Comedy", "Romance"
        )
        val isEnglishOnly = lowerFileName.matches(Regex(".*[a-zA-Z]+.*")) && 
                           !lowerFileName.matches(Regex(".*[\\u4e00-\\u9fa5]+.*"))
        
        if (englishKeywords.any { lowerFileName.contains(it) } || isEnglishOnly) {
            categoryScores[BookCategory.ENGLISH] = 10
            Log.d(TAG, "🇺🇸 文件名包含英文关键词，分类为英文")
        }
        
        // 返回得分最高的分类
        return if (categoryScores.isNotEmpty()) {
            val bestCategory = categoryScores.maxByOrNull { it.value }?.key ?: BookCategory.UNKNOWN
            Log.d(TAG, "✅ 文件名分类结果: ${bestCategory.displayName} (得分: ${categoryScores[bestCategory]})")
            bestCategory
        } else {
            Log.d(TAG, "❓ 文件名无法识别分类")
            BookCategory.UNKNOWN
        }
    }
    
    
    
    
    
    
    
    
    
    
    /**
     * 基于作者分类
     */
    private fun classifyByAuthor(author: String?): BookCategory {
        if (author.isNullOrBlank()) return BookCategory.UNKNOWN
        
        return when {
            // 武侠小说作者 - 传统武侠
            author.contains("金庸") || author.contains("古龙") || 
            author.contains("梁羽生") || author.contains("温瑞安") ||
            author.contains("黄易") || author.contains("卧龙生") ||
            author.contains("司马翎") || author.contains("诸葛青云") ||
            author.contains("还珠楼主") || author.contains("平江不肖生") ||
            author.contains("王度庐") || author.contains("宫白羽") ||
            author.contains("小椴") || author.contains("凤歌") ||
            author.contains("沧月") || author.contains("步非烟") ||
            author.contains("时未寒") || author.contains("方白羽") ||
            author.contains("杨叛") || author.contains("李凉") ||
            author.contains("司马紫烟") || author.contains("云中岳") ||
            author.contains("柳残阳") || author.contains("独孤红") ||
            author.contains("陈青云") || author.contains("萧逸") -> BookCategory.WUXIA
            
            // 仙侠小说作者 - 网络仙侠
            author.contains("萧鼎") || author.contains("我吃西红柿") ||
            author.contains("唐家三少") || author.contains("天蚕土豆") ||
            author.contains("辰东") || author.contains("忘语") ||
            author.contains("耳根") || author.contains("猫腻") ||
            author.contains("烽火戏诸侯") || author.contains("梦入神机") ||
            author.contains("血红") || author.contains("跳舞") ||
            author.contains("月关") || author.contains("孑与2") ||
            author.contains("厌笔萧生") || author.contains("风凌天下") ||
            author.contains("净无痕") || author.contains("鱼人二代") ||
            author.contains("宅猪") || author.contains("牧神记") ||
            author.contains("圣墟") || author.contains("完美世界") ||
            author.contains("遮天") || author.contains("神墓") ||
            author.contains("长生界") || author.contains("不死不灭") ||
            author.contains("番茄") || author.contains("三少") ||
            author.contains("土豆") || author.contains("东哥") -> BookCategory.WUXIA
            
            // 科幻小说作者 - 中国科幻
            author.contains("刘慈欣") || author.contains("王晋康") ||
            author.contains("何夕") || author.contains("韩松") ||
            author.contains("郝景芳") || author.contains("陈楸帆") ||
            author.contains("江波") || author.contains("夏笳") ||
            author.contains("刘宇昆") || author.contains("宝树") ||
            author.contains("大刘") || author.contains("三体") ||
            author.contains("流浪地球") || author.contains("球状闪电") ||
            author.contains("超新星纪元") || author.contains("中国太阳") ||
            author.contains("乡村教师") || author.contains("全频带阻塞干扰") -> BookCategory.SCIENCE_FICTION
            
            // 科幻小说作者 - 国际科幻
            author.contains("阿西莫夫") || author.contains("海因莱因") ||
            author.contains("克拉克") || author.contains("菲利普·K·迪克") ||
            author.contains("阿瑟·克拉克") || author.contains("罗伯特·海因莱因") ||
            author.contains("艾萨克·阿西莫夫") || author.contains("菲利普·迪克") ||
            author.contains("弗兰克·赫伯特") || author.contains("沙丘") ||
            author.contains("基地") || author.contains("银河帝国") ||
            author.contains("机器人") || author.contains("我，机器人") ||
            author.contains("2001太空漫游") || author.contains("2010太空漫游") -> BookCategory.SCIENCE_FICTION
            
            // 言情小说作者 - 传统言情
            author.contains("琼瑶") || author.contains("席绢") ||
            author.contains("古灵") || author.contains("楼雨晴") ||
            author.contains("于晴") || author.contains("典心") ||
            author.contains("决明") || author.contains("绿痕") ||
            author.contains("寄秋") || author.contains("简璎") ||
            author.contains("子纹") || author.contains("子心") ||
            author.contains("子澄") || author.contains("子心") -> BookCategory.ROMANCE
            
            // 言情小说作者 - 网络言情
            author.contains("顾漫") || author.contains("丁墨") ||
            author.contains("匪我思存") || author.contains("桐华") ||
            author.contains("辛夷坞") || author.contains("明晓溪") ||
            author.contains("八月长安") || author.contains("九夜茴") ||
            author.contains("饶雪漫") || author.contains("郭敬明") ||
            author.contains("墨宝非宝") || author.contains("北倾") ||
            author.contains("东奔西顾") || author.contains("酒小七") ||
            author.contains("板栗子") || author.contains("春风榴火") ||
            author.contains("时星草") -> BookCategory.ROMANCE
            
            // 都市小说作者 - 合并到言情
            author.contains("都市") || author.contains("现代") -> BookCategory.ROMANCE
            
            // 历史小说作者
            author.contains("历史") || author.contains("古代") ||
            author.contains("当年明月") || author.contains("易中天") ||
            author.contains("袁腾飞") || author.contains("高晓松") -> BookCategory.HISTORY
            
            // 文学作者 - 现代文学
            author.contains("鲁迅") || author.contains("老舍") ||
            author.contains("巴金") || author.contains("茅盾") ||
            author.contains("沈从文") || author.contains("张爱玲") ||
            author.contains("钱钟书") || author.contains("林语堂") ||
            author.contains("郁达夫") || author.contains("徐志摩") -> BookCategory.LITERATURE
            
            // 文学作者 - 当代文学
            author.contains("余华") || author.contains("莫言") ||
            author.contains("贾平凹") || author.contains("陈忠实") ||
            author.contains("路遥") || author.contains("王小波") ||
            author.contains("王朔") || author.contains("苏童") ||
            author.contains("格非") || author.contains("余秋雨") ||
            author.contains("史铁生") || author.contains("汪曾祺") -> BookCategory.LITERATURE
            
            // 理财投资作者
            author.contains("罗伯特·清崎") || author.contains("富爸爸") ||
            author.contains("穷爸爸") || author.contains("巴菲特") ||
            author.contains("查理·芒格") || author.contains("彼得·林奇") ||
            author.contains("本杰明·格雷厄姆") || author.contains("约翰·博格") ||
            author.contains("瑞·达利欧") || author.contains("纳西姆·塔勒布") ||
            author.contains("沃伦·巴菲特") || author.contains("查理·芒格") ||
            author.contains("彼得·林奇") || author.contains("本杰明·格雷厄姆") ||
            author.contains("约翰·博格") || author.contains("瑞·达利欧") ||
            author.contains("纳西姆·塔勒布") -> BookCategory.FINANCE
            
            // 英文作者识别
            author.contains("Stephen King") || author.contains("J.K. Rowling") ||
            author.contains("George R.R. Martin") || author.contains("Dan Brown") ||
            author.contains("Agatha Christie") || author.contains("Ernest Hemingway") ||
            author.contains("Mark Twain") || author.contains("Charles Dickens") ||
            author.contains("Jane Austen") || author.contains("William Shakespeare") ||
            author.contains("J.R.R. Tolkien") || author.contains("Lord of the Rings") ||
            author.contains("The Hobbit") || author.contains("Harry Potter") ||
            author.contains("Game of Thrones") || author.contains("A Song of Ice and Fire") ||
            author.contains("The Da Vinci Code") || author.contains("Angels and Demons") ||
            author.contains("Murder on the Orient Express") || author.contains("The Old Man and the Sea") -> BookCategory.ENGLISH
            
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
            
            // 仙侠关键词 - 合并到武侠
            title.contains("仙侠") || title.contains("修仙") ||
            title.contains("修真") || title.contains("仙") ||
            title.contains("道") || title.contains("魔") ||
            title.contains("神") || title.contains("妖") -> BookCategory.WUXIA
            
            // 科幻关键词
            title.contains("科幻") || title.contains("未来") ||
            title.contains("科技") || title.contains("机器人") ||
            title.contains("星际") || title.contains("宇宙") ||
            title.contains("时空") || title.contains("基因") -> BookCategory.SCIENCE_FICTION
            
            // 言情关键词
            title.contains("言情") || title.contains("爱情") ||
            title.contains("恋") || title.contains("婚") ||
            title.contains("情") || title.contains("爱") -> BookCategory.ROMANCE
            
            // 都市关键词 - 合并到言情
            title.contains("都市") || title.contains("现代") ||
            title.contains("都市") || title.contains("城市") -> BookCategory.ROMANCE
            
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
            description.contains("仙法") || description.contains("法术") -> DescriptionCategory.WUXIA
            
            description.contains("科幻") || description.contains("未来") ||
            description.contains("科技") || description.contains("机器人") -> DescriptionCategory.SCIENCE_FICTION
            
            description.contains("言情") || description.contains("爱情") ||
            description.contains("浪漫") || description.contains("情感") -> DescriptionCategory.ROMANCE
            
            description.contains("都市") || description.contains("现代") ||
            description.contains("城市") || description.contains("职场") -> DescriptionCategory.ROMANCE
            
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
            subject.contains("言情") || subject.contains("都市") -> BookCategory.ROMANCE
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
            language.contains("en") || language.contains("英文") -> BookCategory.ENGLISH
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
                    Log.d(TAG, "  ✅ 中文为主，分类为未分类")
                    BookCategory.UNKNOWN
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
     * 获取分类统计 - 动态计算版本
     */
    fun getCategoryStats(context: Context): Map<String, Int> {
        return try {
            val prefs = getPrefs(context)
            val categoriesJson = prefs.getString(KEY_CATEGORIES, "{}")
            val categories = JSONObject(categoriesJson)
            
            val result = mutableMapOf<String, Int>()
            val iterator = categories.keys()
            while (iterator.hasNext()) {
                val bookPath = iterator.next()
                val category = categories.getString(bookPath)
                result[category] = result.getOrDefault(category, 0) + 1
            }
            
            Log.d(TAG, "📊 动态分类统计:")
            result.forEach { (category, count) ->
                Log.d(TAG, "  $category: ${count}本")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "获取分类统计失败", e)
            emptyMap()
        }
    }
    
    /**
     * 获取分类统计 - 旧版本（保留兼容性）
     */
    fun getCategoryStatsLegacy(context: Context): Map<String, Int> {
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
     * 重新计算所有分类统计 - 基于实际分类数据
     */
    fun recalculateCategoryStats(context: Context) {
        try {
            Log.d(TAG, "🔄 开始重新计算分类统计...")
            
            val prefs = getPrefs(context)
            val categoriesJson = prefs.getString(KEY_CATEGORIES, "{}")
            val categories = JSONObject(categoriesJson)
            
            // 计算新的统计
            val newStats = mutableMapOf<String, Int>()
            val iterator = categories.keys()
            while (iterator.hasNext()) {
                val bookPath = iterator.next()
                val category = categories.getString(bookPath)
                newStats[category] = newStats.getOrDefault(category, 0) + 1
            }
            
            // 保存新的统计
            val newStatsJson = JSONObject()
            newStats.forEach { (category, count) ->
                newStatsJson.put(category, count)
            }
            
            prefs.edit().putString(KEY_CATEGORY_STATS, newStatsJson.toString()).apply()
            
            Log.d(TAG, "✅ 分类统计重新计算完成:")
            newStats.forEach { (category, count) ->
                Log.d(TAG, "  $category: ${count}本")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "重新计算分类统计失败", e)
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
    
    /**
     * 机器学习分类器接口 - 为未来扩展准备
     */
    interface MLClassifier {
        suspend fun train(trainingData: List<BookSample>)
        suspend fun classify(book: EpubFile): String
        fun getAccuracy(): Double
    }
    
    /**
     * 图书样本数据类 - 用于机器学习训练
     */
    data class BookSample(
        val fileName: String,
        val title: String?,
        val author: String?,
        val description: String?,
        val trueCategory: String,
        val features: BookFeatures = BookFeatures()
    )
    
    /**
     * 图书特征数据类
     */
    data class BookFeatures(
        val fileNameKeywords: List<String> = emptyList(),
        val authorKeywords: List<String> = emptyList(),
        val titleKeywords: List<String> = emptyList(),
        val descriptionKeywords: List<String> = emptyList(),
        val languageFeatures: LanguageFeatures = LanguageFeatures(),
        val lengthFeatures: LengthFeatures = LengthFeatures()
    )
    
    /**
     * 语言特征
     */
    data class LanguageFeatures(
        val hasChinese: Boolean = false,
        val hasEnglish: Boolean = false,
        val chineseRatio: Double = 0.0,
        val englishRatio: Double = 0.0
    )
    
    /**
     * 长度特征
     */
    data class LengthFeatures(
        val fileNameLength: Int = 0,
        val titleLength: Int = 0,
        val descriptionLength: Int = 0
    )
    
    /**
     * 用户反馈数据类
     */
    data class UserFeedback(
        val bookPath: String,
        val predictedCategory: String,
        val userCorrectedCategory: String,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * 保存用户反馈
     */
    fun saveUserFeedback(context: Context, feedback: UserFeedback) {
        try {
            val prefs = getPrefs(context)
            val feedbackKey = "user_feedback_${feedback.timestamp}"
            val feedbackJson = JSONObject().apply {
                put("bookPath", feedback.bookPath)
                put("predictedCategory", feedback.predictedCategory)
                put("userCorrectedCategory", feedback.userCorrectedCategory)
                put("timestamp", feedback.timestamp)
            }
            prefs.edit().putString(feedbackKey, feedbackJson.toString()).apply()
            Log.d(TAG, "用户反馈已保存: ${feedback.bookPath}")
        } catch (e: Exception) {
            Log.e(TAG, "保存用户反馈失败", e)
        }
    }
    
    /**
     * 获取所有用户反馈
     */
    fun getAllUserFeedback(context: Context): List<UserFeedback> {
        return try {
            val prefs = getPrefs(context)
            val allPrefs = prefs.all
            val feedbacks = mutableListOf<UserFeedback>()
            
            allPrefs.forEach { (key, value) ->
                if (key.startsWith("user_feedback_") && value is String) {
                    try {
                        val json = JSONObject(value)
                        val feedback = UserFeedback(
                            bookPath = json.getString("bookPath"),
                            predictedCategory = json.getString("predictedCategory"),
                            userCorrectedCategory = json.getString("userCorrectedCategory"),
                            timestamp = json.getLong("timestamp")
                        )
                        feedbacks.add(feedback)
                    } catch (e: Exception) {
                        Log.w(TAG, "解析用户反馈失败: $key", e)
                    }
                }
            }
            
            feedbacks.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "获取用户反馈失败", e)
            emptyList()
        }
    }
}

/**
 * 图书分类枚举 - 优化版
 * 精简为8个核心分类，去掉中文分类，加入英文分类
 */
enum class BookCategory(
    val displayName: String,
    val color: Int,
    val icon: String
) {
    SCIENCE_FICTION("科幻", -0xCB6BA2, "🚀"),
    LITERATURE("文学", -0xC0634, "📖"),
    WUXIA("武侠", -0x71B5B53, "⚔️"),
    ROMANCE("言情", -0x18B3C4, "💕"),
    HISTORY("历史", -0x6A5A5A, "📚"),
    FINANCE("理财", -0x3F7F3F, "💰"),
    ENGLISH("英文", -0xE54363, "🇺🇸"),
    UNKNOWN("未分类", -0x807372, "❓");
    
    companion object {
        fun fromDisplayName(name: String): BookCategory {
            return values().find { it.displayName == name } ?: UNKNOWN
        }
    }
}

/**
 * 描述分类（用于描述关键词匹配）- 优化版
 */
enum class DescriptionCategory(val category: String) {
    WUXIA("武侠"),
    SCIENCE_FICTION("科幻"),
    ROMANCE("言情"),
    HISTORY("历史"),
    LITERATURE("文学"),
    UNKNOWN("未分类");
}
