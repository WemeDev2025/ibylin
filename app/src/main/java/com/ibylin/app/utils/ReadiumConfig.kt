package com.ibylin.app.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Readium 阅读器配置管理类
 * 负责管理阅读器的各种设置和偏好
 */
@Singleton
class ReadiumConfig @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PREFS_NAME = "readium_config"
        
        // 基础设置
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_LINE_SPACING = "line_spacing"
        private const val KEY_THEME = "theme"
        private const val KEY_PAGE_TURN_MODE = "page_turn_mode"
        private const val KEY_AUTO_SCROLL = "auto_scroll"
        private const val KEY_NIGHT_MODE = "night_mode"
        private const val KEY_FULLSCREEN = "fullscreen"
        private const val KEY_READING_PROGRESS = "reading_progress"
        
        // 字体和排版设置
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_FONT_WEIGHT = "font_weight"
        private const val KEY_TEXT_ALIGN = "text_align"
        private const val KEY_COLUMN_COUNT = "column_count"
        private const val KEY_PAGE_MARGINS = "page_margins"
        private const val KEY_PARAGRAPH_SPACING = "paragraph_spacing"
        private const val KEY_PARAGRAPH_INDENT = "paragraph_indent"
        private const val KEY_WORD_SPACING = "word_spacing"
        private const val KEY_LETTER_SPACING = "letter_spacing"
        
        // 阅读模式设置
        private const val KEY_SCROLL_MODE = "scroll_mode"
        private const val KEY_SPREAD_MODE = "spread_mode"
        private const val KEY_FIT_MODE = "fit_mode"
        private const val KEY_READING_DIRECTION = "reading_direction"
        private const val KEY_PUBLISHER_STYLES = "publisher_styles"
        
        // 交互设置
        private const val KEY_TAP_TO_TURN = "tap_to_turn"
        private const val KEY_VOLUME_KEY_TURN = "volume_key_turn"
        private const val KEY_SWIPE_GESTURE = "swipe_gesture"
        private const val KEY_BRIGHTNESS_CONTROL = "brightness_control"
        private const val KEY_SCREEN_ORIENTATION = "screen_orientation"
        
        // 音频和TTS设置
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_TTS_PITCH = "tts_pitch"
        private const val KEY_TTS_VOICE = "tts_voice"
        private const val KEY_AUDIO_HIGHLIGHTS = "audio_highlights"
        
        // 搜索和导航设置
        private const val KEY_SEARCH_HIGHLIGHT = "search_highlight"
        private const val KEY_BOOKMARK_STYLE = "bookmark_style"
        private const val KEY_ANNOTATION_ENABLED = "annotation_enabled"
        private const val KEY_HIGHLIGHT_COLORS = "highlight_colors"
        
        // 性能和缓存设置
        private const val KEY_PRELOAD_PAGES = "preload_pages"
        private const val KEY_IMAGE_LOADING = "image_loading"
        private const val KEY_JAVASCRIPT_ENABLED = "javascript_enabled"
        private const val KEY_CSS_OVERRIDE = "css_override"
        
        // 可访问性设置
        private const val KEY_HIGH_CONTRAST = "high_contrast"
        private const val KEY_DYSLEXIA_FONT = "dyslexia_font"
        private const val KEY_REDUCE_MOTION = "reduce_motion"
        private const val KEY_SCREEN_READER = "screen_reader"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * 字体大小设置
     */
    var fontSize: Float
        get() = prefs.getFloat(KEY_FONT_SIZE, 16f)
        set(value) = prefs.edit().putFloat(KEY_FONT_SIZE, value).apply()
    
    /**
     * 行间距设置
     */
    var lineSpacing: Float
        get() = prefs.getFloat(KEY_LINE_SPACING, 1.2f)
        set(value) = prefs.edit().putFloat(KEY_LINE_SPACING, value).apply()
    
    /**
     * 主题设置
     */
    var theme: String
        get() = prefs.getString(KEY_THEME, "default") ?: "default"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()
    
    /**
     * 翻页模式
     */
    var pageTurnMode: String
        get() = prefs.getString(KEY_PAGE_TURN_MODE, "slide") ?: "slide"
        set(value) = prefs.edit().putString(KEY_PAGE_TURN_MODE, value).apply()
    
    /**
     * 自动滚动
     */
    var autoScroll: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SCROLL, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SCROLL, value).apply()
    
    /**
     * 夜间模式
     */
    var nightMode: Boolean
        get() = prefs.getBoolean(KEY_NIGHT_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_NIGHT_MODE, value).apply()
    
    /**
     * 全屏模式
     */
    var fullscreen: Boolean
        get() = prefs.getBoolean(KEY_FULLSCREEN, false)
        set(value) = prefs.edit().putBoolean(KEY_FULLSCREEN, value).apply()

    // ========== 字体和排版设置 ==========
    
    /**
     * 字体族
     */
    var fontFamily: String
        get() = prefs.getString(KEY_FONT_FAMILY, "default") ?: "default"
        set(value) = prefs.edit().putString(KEY_FONT_FAMILY, value).apply()
    
    /**
     * 字体粗细
     */
    var fontWeight: String
        get() = prefs.getString(KEY_FONT_WEIGHT, "normal") ?: "normal"
        set(value) = prefs.edit().putString(KEY_FONT_WEIGHT, value).apply()
    
    /**
     * 文本对齐
     */
    var textAlign: String
        get() = prefs.getString(KEY_TEXT_ALIGN, "justify") ?: "justify"
        set(value) = prefs.edit().putString(KEY_TEXT_ALIGN, value).apply()
    
    /**
     * 列数
     */
    var columnCount: Int
        get() = prefs.getInt(KEY_COLUMN_COUNT, 1)
        set(value) = prefs.edit().putInt(KEY_COLUMN_COUNT, value).apply()
    
    /**
     * 页边距
     */
    var pageMargins: Float
        get() = prefs.getFloat(KEY_PAGE_MARGINS, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_PAGE_MARGINS, value).apply()
    
    /**
     * 段落间距
     */
    var paragraphSpacing: Float
        get() = prefs.getFloat(KEY_PARAGRAPH_SPACING, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_PARAGRAPH_SPACING, value).apply()
    
    /**
     * 段落缩进
     */
    var paragraphIndent: Float
        get() = prefs.getFloat(KEY_PARAGRAPH_INDENT, 0.0f)
        set(value) = prefs.edit().putFloat(KEY_PARAGRAPH_INDENT, value).apply()
    
    /**
     * 字间距
     */
    var wordSpacing: Float
        get() = prefs.getFloat(KEY_WORD_SPACING, 0.0f)
        set(value) = prefs.edit().putFloat(KEY_WORD_SPACING, value).apply()
    
    /**
     * 字母间距
     */
    var letterSpacing: Float
        get() = prefs.getFloat(KEY_LETTER_SPACING, 0.0f)
        set(value) = prefs.edit().putFloat(KEY_LETTER_SPACING, value).apply()

    // ========== 阅读模式设置 ==========
    
    /**
     * 滚动模式 (true: 滚动, false: 分页)
     */
    var scrollMode: Boolean
        get() = prefs.getBoolean(KEY_SCROLL_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_SCROLL_MODE, value).apply()
    
    /**
     * 跨页模式
     */
    var spreadMode: String
        get() = prefs.getString(KEY_SPREAD_MODE, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_SPREAD_MODE, value).apply()
    
    /**
     * 适应模式
     */
    var fitMode: String
        get() = prefs.getString(KEY_FIT_MODE, "width") ?: "width"
        set(value) = prefs.edit().putString(KEY_FIT_MODE, value).apply()
    
    /**
     * 阅读方向
     */
    var readingDirection: String
        get() = prefs.getString(KEY_READING_DIRECTION, "ltr") ?: "ltr"
        set(value) = prefs.edit().putString(KEY_READING_DIRECTION, value).apply()
    
    /**
     * 启用出版商样式
     */
    var publisherStyles: Boolean
        get() = prefs.getBoolean(KEY_PUBLISHER_STYLES, true)
        set(value) = prefs.edit().putBoolean(KEY_PUBLISHER_STYLES, value).apply()

    // ========== 交互设置 ==========
    
    /**
     * 点击翻页
     */
    var tapToTurn: Boolean
        get() = prefs.getBoolean(KEY_TAP_TO_TURN, true)
        set(value) = prefs.edit().putBoolean(KEY_TAP_TO_TURN, value).apply()
    
    /**
     * 音量键翻页
     */
    var volumeKeyTurn: Boolean
        get() = prefs.getBoolean(KEY_VOLUME_KEY_TURN, false)
        set(value) = prefs.edit().putBoolean(KEY_VOLUME_KEY_TURN, value).apply()
    
    /**
     * 滑动手势
     */
    var swipeGesture: Boolean
        get() = prefs.getBoolean(KEY_SWIPE_GESTURE, true)
        set(value) = prefs.edit().putBoolean(KEY_SWIPE_GESTURE, value).apply()
    
    /**
     * 亮度控制
     */
    var brightnessControl: Boolean
        get() = prefs.getBoolean(KEY_BRIGHTNESS_CONTROL, false)
        set(value) = prefs.edit().putBoolean(KEY_BRIGHTNESS_CONTROL, value).apply()
    
    /**
     * 屏幕方向
     */
    var screenOrientation: String
        get() = prefs.getString(KEY_SCREEN_ORIENTATION, "auto") ?: "auto"
        set(value) = prefs.edit().putString(KEY_SCREEN_ORIENTATION, value).apply()

    // ========== 音频和TTS设置 ==========
    
    /**
     * TTS启用
     */
    var ttsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_TTS_ENABLED, value).apply()
    
    /**
     * TTS语速
     */
    var ttsSpeed: Float
        get() = prefs.getFloat(KEY_TTS_SPEED, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_TTS_SPEED, value).apply()
    
    /**
     * TTS音调
     */
    var ttsPitch: Float
        get() = prefs.getFloat(KEY_TTS_PITCH, 1.0f)
        set(value) = prefs.edit().putFloat(KEY_TTS_PITCH, value).apply()
    
    /**
     * TTS语音
     */
    var ttsVoice: String
        get() = prefs.getString(KEY_TTS_VOICE, "default") ?: "default"
        set(value) = prefs.edit().putString(KEY_TTS_VOICE, value).apply()
    
    /**
     * 音频高亮
     */
    var audioHighlights: Boolean
        get() = prefs.getBoolean(KEY_AUDIO_HIGHLIGHTS, true)
        set(value) = prefs.edit().putBoolean(KEY_AUDIO_HIGHLIGHTS, value).apply()

    // ========== 搜索和导航设置 ==========
    
    /**
     * 搜索高亮
     */
    var searchHighlight: Boolean
        get() = prefs.getBoolean(KEY_SEARCH_HIGHLIGHT, true)
        set(value) = prefs.edit().putBoolean(KEY_SEARCH_HIGHLIGHT, value).apply()
    
    /**
     * 书签样式
     */
    var bookmarkStyle: String
        get() = prefs.getString(KEY_BOOKMARK_STYLE, "default") ?: "default"
        set(value) = prefs.edit().putString(KEY_BOOKMARK_STYLE, value).apply()
    
    /**
     * 注释启用
     */
    var annotationEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANNOTATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ANNOTATION_ENABLED, value).apply()
    
    /**
     * 高亮颜色
     */
    var highlightColors: String
        get() = prefs.getString(KEY_HIGHLIGHT_COLORS, "yellow,green,blue,pink") ?: "yellow,green,blue,pink"
        set(value) = prefs.edit().putString(KEY_HIGHLIGHT_COLORS, value).apply()

    // ========== 性能和缓存设置 ==========
    
    /**
     * 预加载页数
     */
    var preloadPages: Int
        get() = prefs.getInt(KEY_PRELOAD_PAGES, 3)
        set(value) = prefs.edit().putInt(KEY_PRELOAD_PAGES, value).apply()
    
    /**
     * 图片加载
     */
    var imageLoading: Boolean
        get() = prefs.getBoolean(KEY_IMAGE_LOADING, true)
        set(value) = prefs.edit().putBoolean(KEY_IMAGE_LOADING, value).apply()
    
    /**
     * JavaScript启用
     */
    var javascriptEnabled: Boolean
        get() = prefs.getBoolean(KEY_JAVASCRIPT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_JAVASCRIPT_ENABLED, value).apply()
    
    /**
     * CSS重写
     */
    var cssOverride: Boolean
        get() = prefs.getBoolean(KEY_CSS_OVERRIDE, false)
        set(value) = prefs.edit().putBoolean(KEY_CSS_OVERRIDE, value).apply()

    // ========== 可访问性设置 ==========
    
    /**
     * 高对比度
     */
    var highContrast: Boolean
        get() = prefs.getBoolean(KEY_HIGH_CONTRAST, false)
        set(value) = prefs.edit().putBoolean(KEY_HIGH_CONTRAST, value).apply()
    
    /**
     * 阅读障碍字体
     */
    var dyslexiaFont: Boolean
        get() = prefs.getBoolean(KEY_DYSLEXIA_FONT, false)
        set(value) = prefs.edit().putBoolean(KEY_DYSLEXIA_FONT, value).apply()
    
    /**
     * 减少动画
     */
    var reduceMotion: Boolean
        get() = prefs.getBoolean(KEY_REDUCE_MOTION, false)
        set(value) = prefs.edit().putBoolean(KEY_REDUCE_MOTION, value).apply()
    
    /**
     * 屏幕阅读器支持
     */
    var screenReader: Boolean
        get() = prefs.getBoolean(KEY_SCREEN_READER, false)
        set(value) = prefs.edit().putBoolean(KEY_SCREEN_READER, value).apply()
    
    // ========== 扩展属性支持 ==========
    
    /**
     * 连字启用
     */
    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }
    
    /**
     * 获取浮点数值
     */
    fun getFloat(key: String, defaultValue: Float): Float {
        return prefs.getFloat(key, defaultValue)
    }
    
    /**
     * 获取整数值
     */
    fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }
    
    /**
     * 获取字符串值
     */
    fun getString(key: String, defaultValue: String?): String? {
        return prefs.getString(key, defaultValue)
    }
    
    /**
     * 保存阅读进度
     */
    fun saveReadingProgress(bookPath: String, progress: Float) {
        prefs.edit().putFloat("${KEY_READING_PROGRESS}_$bookPath", progress).apply()
    }
    
    /**
     * 获取阅读进度
     */
    fun getReadingProgress(bookPath: String): Float {
        return prefs.getFloat("${KEY_READING_PROGRESS}_$bookPath", 0f)
    }
    
    /**
     * 获取可用主题列表
     */
    fun getAvailableThemes(): List<String> {
        return listOf(
            "default",      // 默认主题
            "sepia",        // 护眼主题
            "night",        // 夜间主题
            "high_contrast" // 高对比度主题
        )
    }
    
    /**
     * 获取可用翻页模式
     */
    fun getAvailablePageTurnModes(): List<String> {
        return listOf(
            "slide",    // 滑动翻页
            "curl",     // 翻书效果
            "fade",     // 淡入淡出
            "instant"   // 瞬间切换
        )
    }

    /**
     * 获取可用字体族
     */
    fun getAvailableFontFamilies(): List<String> {
        return listOf(
            "default",     // 系统默认
            "serif",       // 衬线字体
            "sans-serif",  // 无衬线字体
            "monospace",   // 等宽字体
            "dyslexia",    // 阅读障碍友好字体
            "noto-serif",  // Noto Serif
            "noto-sans",   // Noto Sans
            "source-serif",// Source Serif
            "source-sans"  // Source Sans
        )
    }

    /**
     * 获取可用字体粗细
     */
    fun getAvailableFontWeights(): List<String> {
        return listOf(
            "thin",        // 100
            "light",       // 300
            "normal",      // 400
            "medium",      // 500
            "bold",        // 700
            "black"        // 900
        )
    }

    /**
     * 获取可用文本对齐方式
     */
    fun getAvailableTextAlignments(): List<String> {
        return listOf(
            "left",        // 左对齐
            "center",      // 居中
            "right",       // 右对齐
            "justify"      // 两端对齐
        )
    }

    /**
     * 获取可用跨页模式
     */
    fun getAvailableSpreadModes(): List<String> {
        return listOf(
            "auto",        // 自动
            "never",       // 从不
            "always"       // 总是
        )
    }

    /**
     * 获取可用适应模式
     */
    fun getAvailableFitModes(): List<String> {
        return listOf(
            "width",       // 适应宽度
            "height",      // 适应高度
            "contain",     // 完全包含
            "cover"        // 覆盖
        )
    }

    /**
     * 获取可用阅读方向
     */
    fun getAvailableReadingDirections(): List<String> {
        return listOf(
            "ltr",         // 从左到右
            "rtl",         // 从右到左
            "auto"         // 自动检测
        )
    }

    /**
     * 获取可用屏幕方向
     */
    fun getAvailableScreenOrientations(): List<String> {
        return listOf(
            "auto",        // 自动
            "portrait",    // 竖屏
            "landscape"    // 横屏
        )
    }

    /**
     * 获取可用书签样式
     */
    fun getAvailableBookmarkStyles(): List<String> {
        return listOf(
            "default",     // 默认
            "modern",      // 现代
            "classic",     // 经典
            "minimal"      // 简约
        )
    }

    /**
     * 获取可用高亮颜色
     */
    fun getAvailableHighlightColors(): List<String> {
        return listOf(
            "yellow",      // 黄色
            "green",       // 绿色
            "blue",        // 蓝色
            "pink",        // 粉色
            "purple",      // 紫色
            "orange",      // 橙色
            "red",         // 红色
            "gray"         // 灰色
        )
    }
    
    /**
     * 重置为默认设置
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
    }
    
    /**
     * 导出配置
     */
    fun exportConfig(): String {
        return """
            Readium 阅读器配置
            ===================
            
            基础设置:
            字体大小: ${fontSize}pt
            行间距: $lineSpacing
            主题: $theme
            翻页模式: $pageTurnMode
            自动滚动: $autoScroll
            夜间模式: $nightMode
            全屏模式: $fullscreen
            
            字体和排版:
            字体族: $fontFamily
            字体粗细: $fontWeight
            文本对齐: $textAlign
            列数: $columnCount
            页边距: $pageMargins
            段落间距: $paragraphSpacing
            段落缩进: $paragraphIndent
            字间距: $wordSpacing
            字母间距: $letterSpacing
            
            阅读模式:
            滚动模式: $scrollMode
            跨页模式: $spreadMode
            适应模式: $fitMode
            阅读方向: $readingDirection
            出版商样式: $publisherStyles
            
            交互设置:
            点击翻页: $tapToTurn
            音量键翻页: $volumeKeyTurn
            滑动手势: $swipeGesture
            亮度控制: $brightnessControl
            屏幕方向: $screenOrientation
            
            TTS设置:
            TTS启用: $ttsEnabled
            TTS语速: $ttsSpeed
            TTS音调: $ttsPitch
            TTS语音: $ttsVoice
            音频高亮: $audioHighlights
            
            导航设置:
            搜索高亮: $searchHighlight
            书签样式: $bookmarkStyle
            注释启用: $annotationEnabled
            高亮颜色: $highlightColors
            
            性能设置:
            预加载页数: $preloadPages
            图片加载: $imageLoading
            JavaScript: $javascriptEnabled
            CSS重写: $cssOverride
            
            可访问性:
            高对比度: $highContrast
            阅读障碍字体: $dyslexiaFont
            减少动画: $reduceMotion
            屏幕阅读器: $screenReader
        """.trimIndent()
    }
}
