package com.ibylin.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Readium 配置管理器 - 直接使用Readium原生配置系统
 * 替换旧的复杂配置系统，直接管理EpubPreferences
 */
@Singleton
class ReadiumConfigManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val TAG = "ReadiumConfigManager"
        private const val PREFS_NAME = "readium_preferences"
        
        // 配置键名
        private const val KEY_THEME = "theme"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_LINE_HEIGHT = "line_height"
        private const val KEY_PAGE_MARGINS = "page_margins"
        private const val KEY_TEXT_ALIGN = "text_align"
        private const val KEY_COLUMN_COUNT = "column_count"
        private const val KEY_SCROLL_MODE = "scroll_mode"
        private const val KEY_PUBLISHER_STYLES = "publisher_styles"
        private const val KEY_NIGHT_MODE = "night_mode"
        private const val KEY_HIGH_CONTRAST = "high_contrast"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // 当前配置缓存
    private var currentPreferences: EpubPreferences? = null
    
    /**
     * 获取当前配置
     */
    fun getCurrentPreferences(): EpubPreferences {
        if (currentPreferences == null) {
            currentPreferences = loadPreferencesFromStorage()
            Log.d(TAG, "从存储加载配置: $currentPreferences")
        }
        return currentPreferences!!
    }
    
    /**
     * 更新配置
     */
    fun updatePreferences(updater: (EpubPreferences) -> EpubPreferences) {
        val current = getCurrentPreferences()
        val updated = updater(current)
        
        Log.d(TAG, "更新配置: $current -> $updated")
        
        currentPreferences = updated
        savePreferencesToStorage(updated)
    }
    
    /**
     * 应用主题
     */
    fun setTheme(theme: String) {
        Log.d(TAG, "设置主题: $theme")
        updatePreferences { current ->
            val newTheme = when (theme) {
                "sepia" -> Theme.SEPIA
                "night" -> Theme.DARK
                "high_contrast" -> Theme.LIGHT
                else -> Theme.LIGHT
            }
            current.copy(theme = newTheme)
        }
    }
    
    /**
     * 应用字体大小
     */
    fun setFontSize(size: Float) {
        Log.d(TAG, "设置字体大小: ${size}pt")
        updatePreferences { current ->
            val fontSize = (size / 16.0) // 转换为百分比
            current.copy(fontSize = fontSize)
        }
    }
    
    /**
     * 应用字体族
     */
    fun setFontFamily(family: String) {
        Log.d(TAG, "设置字体族: $family")
        updatePreferences { current ->
            val fontFamily = when (family) {
                "serif" -> FontFamily("serif")
                "sans-serif" -> FontFamily("sans-serif")
                "monospace" -> FontFamily("monospace")
                "cursive" -> FontFamily("cursive")
                else -> FontFamily("sans-serif")
            }
            current.copy(fontFamily = fontFamily)
        }
    }
    
    /**
     * 应用行高
     */
    fun setLineHeight(height: Float) {
        Log.d(TAG, "设置行高: $height")
        updatePreferences { current ->
            current.copy(lineHeight = height.toDouble())
        }
    }
    
    /**
     * 应用页边距
     */
    fun setPageMargins(margins: Float) {
        Log.d(TAG, "设置页边距: $margins")
        updatePreferences { current ->
            current.copy(pageMargins = margins.toDouble())
        }
    }
    
    /**
     * 应用文本对齐
     */
    fun setTextAlign(align: String) {
        Log.d(TAG, "设置文本对齐: $align")
        updatePreferences { current ->
            val textAlign = when (align) {
                "left" -> TextAlign.START
                "center" -> TextAlign.CENTER
                "right" -> TextAlign.END
                "justify" -> TextAlign.JUSTIFY
                else -> TextAlign.JUSTIFY
            }
            current.copy(textAlign = textAlign)
        }
    }
    
    /**
     * 应用列数
     */
    fun setColumnCount(count: Int) {
        Log.d(TAG, "设置列数: $count")
        updatePreferences { current ->
            val columnCount = when (count) {
                1 -> ColumnCount.ONE
                2 -> ColumnCount.TWO
                else -> ColumnCount.AUTO
            }
            current.copy(columnCount = columnCount)
        }
    }
    
    /**
     * 应用滚动模式
     */
    fun setScrollMode(scroll: Boolean) {
        Log.d(TAG, "设置滚动模式: $scroll")
        updatePreferences { current ->
            current.copy(scroll = scroll)
        }
    }
    
    /**
     * 应用出版商样式
     */
    fun setPublisherStyles(enabled: Boolean) {
        Log.d(TAG, "设置出版商样式: $enabled")
        updatePreferences { current ->
            current.copy(publisherStyles = enabled)
        }
    }
    
    /**
     * 应用夜间模式
     */
    fun setNightMode(enabled: Boolean) {
        Log.d(TAG, "设置夜间模式: $enabled")
        updatePreferences { current ->
            val theme = if (enabled) Theme.DARK else Theme.LIGHT
            val imageFilter = if (enabled) ImageFilter.DARKEN else null
            current.copy(theme = theme, imageFilter = imageFilter)
        }
    }
    
    /**
     * 应用高对比度
     */
    fun setHighContrast(enabled: Boolean) {
        Log.d(TAG, "设置高对比度: $enabled")
        updatePreferences { current ->
            if (enabled) {
                current.copy(
                    textColor = Color(0x000000),
                    backgroundColor = Color(0xFFFFFF)
                )
            } else {
                current.copy(
                    textColor = null,
                    backgroundColor = null
                )
            }
        }
    }
    
    /**
     * 重置为默认配置
     */
    fun resetToDefaults() {
        Log.d(TAG, "重置为默认配置")
        val defaultPrefs = getDefaultPreferences()
        currentPreferences = defaultPrefs
        savePreferencesToStorage(defaultPrefs)
    }
    
    /**
     * 获取默认配置
     */
    private fun getDefaultPreferences(): EpubPreferences {
        return EpubPreferences(
            theme = Theme.LIGHT,
            fontFamily = FontFamily("sans-serif"),
            fontSize = 1.0, // 100%
            fontWeight = 1.0,
            lineHeight = 1.2,
            pageMargins = 1.0,
            textAlign = TextAlign.JUSTIFY,
            columnCount = ColumnCount.AUTO,
            scroll = false,
            publisherStyles = true,
            textNormalization = false
        )
    }
    
    /**
     * 从存储加载配置
     */
    private fun loadPreferencesFromStorage(): EpubPreferences {
        return try {
            val theme = when (prefs.getString(KEY_THEME, "light")) {
                "sepia" -> Theme.SEPIA
                "night" -> Theme.DARK
                else -> Theme.LIGHT
            }
            
            val fontSize = prefs.getFloat(KEY_FONT_SIZE, 16f).toDouble() / 16.0
            val fontFamily = FontFamily(prefs.getString(KEY_FONT_FAMILY, "sans-serif") ?: "sans-serif")
            val lineHeight = prefs.getFloat(KEY_LINE_HEIGHT, 1.2f).toDouble()
            val pageMargins = prefs.getFloat(KEY_PAGE_MARGINS, 1.0f).toDouble()
            
            val textAlign = when (prefs.getString(KEY_TEXT_ALIGN, "justify")) {
                "left" -> TextAlign.START
                "center" -> TextAlign.CENTER
                "right" -> TextAlign.END
                else -> TextAlign.JUSTIFY
            }
            
            val columnCount = when (prefs.getInt(KEY_COLUMN_COUNT, 0)) {
                1 -> ColumnCount.ONE
                2 -> ColumnCount.TWO
                else -> ColumnCount.AUTO
            }
            
            val scroll = prefs.getBoolean(KEY_SCROLL_MODE, false)
            val publisherStyles = prefs.getBoolean(KEY_PUBLISHER_STYLES, true)
            val nightMode = prefs.getBoolean(KEY_NIGHT_MODE, false)
            val highContrast = prefs.getBoolean(KEY_HIGH_CONTRAST, false)
            
            val finalTheme = if (nightMode) Theme.DARK else theme
            val imageFilter = if (nightMode) ImageFilter.DARKEN else null
            
            val textColor = if (highContrast) Color(0x000000) else null
            val backgroundColor = if (highContrast) Color(0xFFFFFF) else null
            
            EpubPreferences(
                theme = finalTheme,
                fontFamily = fontFamily,
                fontSize = fontSize,
                fontWeight = 1.0,
                lineHeight = lineHeight,
                pageMargins = pageMargins,
                textAlign = textAlign,
                columnCount = columnCount,
                scroll = scroll,
                publisherStyles = publisherStyles,
                imageFilter = imageFilter,
                textColor = textColor,
                backgroundColor = backgroundColor
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "从存储加载配置失败，使用默认配置", e)
            getDefaultPreferences()
        }
    }
    
    /**
     * 保存配置到存储
     */
    private fun savePreferencesToStorage(preferences: EpubPreferences) {
        try {
            prefs.edit().apply {
                // 主题
                putString(KEY_THEME, when (preferences.theme) {
                    Theme.SEPIA -> "sepia"
                    Theme.DARK -> "night"
                    else -> "light"
                })
                
                // 字体大小
                putFloat(KEY_FONT_SIZE, ((preferences.fontSize ?: 1.0) * 16.0).toFloat())
                
                // 字体族
                putString(KEY_FONT_FAMILY, preferences.fontFamily?.name ?: "sans-serif")
                
                // 行高
                putFloat(KEY_LINE_HEIGHT, (preferences.lineHeight ?: 1.2).toFloat())
                
                // 页边距
                putFloat(KEY_PAGE_MARGINS, (preferences.pageMargins ?: 1.0).toFloat())
                
                // 文本对齐
                putString(KEY_TEXT_ALIGN, when (preferences.textAlign) {
                    TextAlign.START -> "left"
                    TextAlign.CENTER -> "center"
                    TextAlign.END -> "right"
                    TextAlign.JUSTIFY -> "justify"
                    else -> "justify"
                })
                
                // 列数
                putInt(KEY_COLUMN_COUNT, when (preferences.columnCount) {
                    ColumnCount.ONE -> 1
                    ColumnCount.TWO -> 2
                    else -> 0
                })
                
                // 滚动模式
                putBoolean(KEY_SCROLL_MODE, preferences.scroll ?: false)
                
                // 出版商样式
                putBoolean(KEY_PUBLISHER_STYLES, preferences.publisherStyles ?: true)
                
                // 夜间模式
                putBoolean(KEY_NIGHT_MODE, preferences.theme == Theme.DARK)
                
                // 高对比度
                putBoolean(KEY_HIGH_CONTRAST, preferences.textColor != null && preferences.backgroundColor != null)
                
            }.apply()
            
            Log.d(TAG, "配置已保存到存储")
            
        } catch (e: Exception) {
            Log.e(TAG, "保存配置到存储失败", e)
        }
    }
    
    /**
     * 获取配置摘要
     */
    fun getConfigSummary(): String {
        val prefs = getCurrentPreferences()
        return """
            当前配置摘要:
            ==============
            主题: ${prefs.theme}
            字体大小: ${(prefs.fontSize ?: 1.0) * 16.0}pt
            字体族: ${prefs.fontFamily?.name ?: "默认"}
            行高: ${prefs.lineHeight ?: 1.2}
            页边距: ${prefs.pageMargins ?: 1.0}
            文本对齐: ${prefs.textAlign}
            列数: ${prefs.columnCount}
            滚动模式: ${if (prefs.scroll == true) "是" else "否"}
            出版商样式: ${if (prefs.publisherStyles == true) "启用" else "禁用"}
            夜间模式: ${if (prefs.theme == Theme.DARK) "是" else "否"}
            高对比度: ${if (prefs.textColor != null && prefs.backgroundColor != null) "是" else "否"}
        """.trimIndent()
    }
}
