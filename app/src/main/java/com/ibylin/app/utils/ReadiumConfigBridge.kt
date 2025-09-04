package com.ibylin.app.utils

import android.util.Log
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.*
import org.readium.r2.shared.util.Language

/**
 * Readium 配置桥接器
 * 负责将 ReadiumConfig 的配置转换为 Readium 原生的 EpubPreferences
 */
class ReadiumConfigBridge(private val readiumConfig: ReadiumConfig) {
    
    /**
     * 将 ReadiumConfig 转换为 EpubPreferences
     */
    fun toEpubPreferences(): EpubPreferences {
        return EpubPreferences(
            // 主题设置
            theme = when (readiumConfig.theme) {
                "sepia" -> Theme.SEPIA
                "night" -> Theme.DARK
                "high_contrast" -> Theme.LIGHT // 高对比度使用亮色主题
                else -> Theme.LIGHT
            },
            
            // 字体设置
            fontFamily = when (readiumConfig.fontFamily) {
                "serif" -> FontFamily("serif")
                "sans-serif" -> FontFamily("sans-serif")
                "monospace" -> FontFamily("monospace")
                "cursive" -> FontFamily("cursive")
                "dyslexia" -> FontFamily("OpenDyslexic")
                "noto-serif" -> FontFamily("Noto Serif")
                "noto-sans" -> FontFamily("Noto Sans")
                "source-serif" -> FontFamily("Source Serif")
                "source-sans" -> FontFamily("Source Sans")
                else -> FontFamily("sans-serif")
            },
            
            // 字体大小 (转换为百分比，Readium使用百分比)
            fontSize = (readiumConfig.fontSize / 16.0), // 16pt作为基准100%
            
            // 字体粗细
            fontWeight = when (readiumConfig.fontWeight) {
                "thin" -> 0.4
                "light" -> 0.7
                "normal" -> 1.0
                "medium" -> 1.2
                "bold" -> 1.5
                "black" -> 2.0
                else -> 1.0
            },
            
            // 行高
            lineHeight = readiumConfig.lineSpacing.toDouble(),
            
            // 页边距
            pageMargins = readiumConfig.pageMargins.toDouble(),
            
            // 段落间距
            paragraphSpacing = readiumConfig.paragraphSpacing.toDouble(),
            
            // 段落缩进
            paragraphIndent = readiumConfig.paragraphIndent.toDouble(),
            
            // 字间距
            wordSpacing = readiumConfig.wordSpacing.toDouble(),
            
            // 字母间距
            letterSpacing = readiumConfig.letterSpacing.toDouble(),
            
            // 文本对齐
            textAlign = when (readiumConfig.textAlign) {
                "left" -> TextAlign.START
                "center" -> TextAlign.CENTER
                "right" -> TextAlign.END
                "justify" -> TextAlign.JUSTIFY
                else -> TextAlign.JUSTIFY
            },
            
            // 列数
            columnCount = when (readiumConfig.columnCount) {
                1 -> ColumnCount.ONE
                2 -> ColumnCount.TWO
                else -> ColumnCount.AUTO
            },
            
            // 滚动模式
            scroll = readiumConfig.scrollMode,
            
            // 跨页模式
            spread = when (readiumConfig.spreadMode) {
                "never" -> Spread.NEVER
                "always" -> Spread.ALWAYS
                else -> null // auto
            },
            
            // 阅读方向
            readingProgression = when (readiumConfig.readingDirection) {
                "rtl" -> ReadingProgression.RTL
                "ltr" -> ReadingProgression.LTR
                else -> ReadingProgression.LTR
            },
            
            // 出版商样式
            publisherStyles = readiumConfig.publisherStyles,
            
            // 夜间模式图片滤镜
            imageFilter = if (readiumConfig.nightMode) ImageFilter.DARKEN else null,
            
            // 高对比度
            textColor = if (readiumConfig.highContrast) Color(0x000000) else null,
            backgroundColor = if (readiumConfig.highContrast) Color(0xFFFFFF) else null,
            
            // 阅读障碍字体
            textNormalization = readiumConfig.dyslexiaFont,
            
            // 连字
            ligatures = readiumConfig.ligaturesEnabled,
            
            // 连字符
            hyphens = readiumConfig.hyphensEnabled,
            
            // 类型缩放
            typeScale = readiumConfig.typeScale.toDouble(),
            
            // 垂直文本（CJK语言）
            verticalText = readiumConfig.verticalTextEnabled
        )
    }
    
    /**
     * 从 EpubPreferences 更新 ReadiumConfig
     */
    fun updateFromEpubPreferences(preferences: EpubPreferences) {
        // 主题
        readiumConfig.theme = when (preferences.theme) {
            Theme.SEPIA -> "sepia"
            Theme.DARK -> "night"
            Theme.LIGHT -> "default"
            else -> "default"
        }
        
        // 字体大小
        preferences.fontSize?.let { fontSize ->
            readiumConfig.fontSize = (fontSize * 16.0).toFloat()
        }
        
        // 字体族
        preferences.fontFamily?.let { fontFamily ->
            readiumConfig.fontFamily = fontFamily.name
        }
        
        // 行高
        preferences.lineHeight?.let { lineHeight ->
            readiumConfig.lineSpacing = lineHeight.toFloat()
        }
        
        // 页边距
        preferences.pageMargins?.let { pageMargins ->
            readiumConfig.pageMargins = pageMargins.toFloat()
        }
        
        // 文本对齐
        preferences.textAlign?.let { textAlign ->
            readiumConfig.textAlign = when (textAlign) {
                TextAlign.START -> "left"
                TextAlign.CENTER -> "center"
                TextAlign.END -> "right"
                TextAlign.JUSTIFY -> "justify"
                else -> "justify"
            }
        }
        
        // 列数
        preferences.columnCount?.let { columnCount ->
            readiumConfig.columnCount = when (columnCount) {
                ColumnCount.ONE -> 1
                ColumnCount.TWO -> 2
                else -> 1
            }
        }
        
        // 滚动模式
        preferences.scroll?.let { scroll ->
            readiumConfig.scrollMode = scroll
        }
        
        // 夜间模式
        readiumConfig.nightMode = preferences.theme == Theme.DARK
    }
    
    /**
     * 获取默认配置
     */
    fun getDefaultPreferences(): EpubPreferences {
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
     * 应用配置到阅读器
     * @param configurable Readium的Configurable接口
     */
    fun applyToReader(configurable: Configurable<*, EpubPreferences>) {
        try {
            val preferences = toEpubPreferences()
            configurable.submitPreferences(preferences)
            Log.d("ReadiumConfigBridge", "配置已应用到阅读器: $preferences")
        } catch (e: Exception) {
            Log.e("ReadiumConfigBridge", "应用配置到阅读器失败", e)
        }
    }
}

// 扩展属性，为ReadiumConfig添加一些缺失的属性
val ReadiumConfig.ligaturesEnabled: Boolean
    get() = this.getBoolean("ligatures_enabled", false)

val ReadiumConfig.hyphensEnabled: Boolean
    get() = this.getBoolean("hyphens_enabled", false)

val ReadiumConfig.typeScale: Float
    get() = this.getFloat("type_scale", 1.0f)

val ReadiumConfig.verticalTextEnabled: Boolean
    get() = this.getBoolean("vertical_text_enabled", false)
