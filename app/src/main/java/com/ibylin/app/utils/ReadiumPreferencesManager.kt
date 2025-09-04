package com.ibylin.app.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi

/**
 * 严格按照开发实例实现的Readium配置管理器
 * 参考: /Users/zhaojing/Downloads/kotlin-toolkit-develop/demos/navigator/src/main/java/org/readium/demo/navigator/preferences/PreferencesManager.kt
 */
@OptIn(ExperimentalReadiumApi::class)
class ReadiumPreferencesManager(
    initialPreferences: EpubPreferences
) {
    companion object {
        private const val TAG = "ReadiumPreferencesManager"
        
        // 默认配置
        fun getDefaultPreferences(): EpubPreferences {
            return EpubPreferences(
                fontSize = 1.0, // 100%
                theme = Theme.LIGHT,
                fontFamily = FontFamily("sans-serif"),
                lineHeight = 1.2,
                pageMargins = 1.0,
                textAlign = org.readium.r2.navigator.preferences.TextAlign.JUSTIFY
            )
        }
    }
    
    // 使用StateFlow管理配置状态，严格按照开发实例
    private val preferencesMutable: MutableStateFlow<EpubPreferences> = 
        MutableStateFlow(initialPreferences)
    
    val preferences: StateFlow<EpubPreferences> = preferencesMutable
    
    /**
     * 设置新的配置，严格按照开发实例的方式
     */
    fun setPreferences(preferences: EpubPreferences) {
        Log.d(TAG, "设置新配置: $preferences")
        preferencesMutable.value = preferences
    }
    
    /**
     * 获取当前配置
     */
    fun getCurrentPreferences(): EpubPreferences {
        return preferencesMutable.value
    }
    
    /**
     * 更新字体大小
     */
    fun setFontSize(sizePt: Float) {
        val fontSize = (sizePt / 16.0) // 转换为百分比
        val current = preferencesMutable.value
        val newPrefs = current.copy(fontSize = fontSize)
        Log.d(TAG, "字体大小: ${sizePt}pt -> ${fontSize} (${(fontSize * 100).toInt()}%)")
        setPreferences(newPrefs)
    }
    
    /**
     * 更新主题
     */
    fun setTheme(themeName: String) {
        val theme = when (themeName) {
            "light", "默认" -> Theme.LIGHT
            "sepia", "护眼", "复古" -> Theme.SEPIA
            "dark", "night", "夜间" -> Theme.DARK
            else -> Theme.LIGHT
        }
        val current = preferencesMutable.value
        val newPrefs = current.copy(theme = theme)
        Log.d(TAG, "主题: $themeName -> $theme")
        setPreferences(newPrefs)
    }
    
    /**
     * 更新字体族
     */
    fun setFontFamily(familyName: String) {
        val fontFamily = FontFamily(familyName)
        val current = preferencesMutable.value
        val newPrefs = current.copy(fontFamily = fontFamily)
        Log.d(TAG, "字体族: $familyName")
        setPreferences(newPrefs)
    }
    
    /**
     * 更新行高
     */
    fun setLineHeight(height: Float) {
        val current = preferencesMutable.value
        val newPrefs = current.copy(lineHeight = height.toDouble())
        Log.d(TAG, "行高: $height")
        setPreferences(newPrefs)
    }
    
    /**
     * 更新页边距
     */
    fun setPageMargins(margins: Float) {
        val current = preferencesMutable.value
        val newPrefs = current.copy(pageMargins = margins.toDouble())
        Log.d(TAG, "页边距: $margins")
        setPreferences(newPrefs)
    }
    
    /**
     * 重置为默认配置
     */
    fun resetToDefaults() {
        Log.d(TAG, "重置为默认配置")
        setPreferences(getDefaultPreferences())
    }
}
