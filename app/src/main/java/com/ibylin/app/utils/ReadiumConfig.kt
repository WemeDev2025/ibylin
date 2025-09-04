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
        private const val PREFS_NAME = "librera_config"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_LINE_SPACING = "line_spacing"
        private const val KEY_THEME = "theme"
        private const val KEY_PAGE_TURN_MODE = "page_turn_mode"
        private const val KEY_AUTO_SCROLL = "auto_scroll"
        private const val KEY_NIGHT_MODE = "night_mode"
        private const val KEY_FULLSCREEN = "fullscreen"
        private const val KEY_READING_PROGRESS = "reading_progress"
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
            Librera Reader 配置
            ===================
            字体大小: $fontSize
            行间距: $lineSpacing
            主题: $theme
            翻页模式: $pageTurnMode
            自动滚动: $autoScroll
            夜间模式: $nightMode
            全屏模式: $fullscreen
        """.trimIndent()
    }
}
