package com.ibylin.app.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ibylin.app.databinding.ActivityReadiumSimpleTestBinding
import com.ibylin.app.utils.ReadiumConfigManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Readium 简单测试界面
 * 用于快速测试配置是否生效
 */
@AndroidEntryPoint
class ReadiumSimpleTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReadiumSimpleTestBinding
    
    @Inject
    lateinit var configManager: ReadiumConfigManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadiumSimpleTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d("ReadiumSimpleTest", "Activity创建开始")
        
        setupToolbar()
        setupTestButtons()
        displayCurrentConfig()
        
        Log.d("ReadiumSimpleTest", "Activity创建完成")
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Readium 配置测试"
    }
    
    private fun setupTestButtons() {
        // 字体大小测试
        binding.btnFontSize.setOnClickListener {
            testFontSize()
        }
        
        // 主题测试
        binding.btnTheme.setOnClickListener {
            testTheme()
        }
        
        // 字体族测试
        binding.btnFontFamily.setOnClickListener {
            testFontFamily()
        }
        
        // 行高测试
        binding.btnLineHeight.setOnClickListener {
            testLineHeight()
        }
        
        // 页边距测试
        binding.btnPageMargins.setOnClickListener {
            testPageMargins()
        }
        
        // 重置配置
        binding.btnReset.setOnClickListener {
            resetConfig()
        }
        
        // 显示配置
        binding.btnShowConfig.setOnClickListener {
            displayCurrentConfig()
        }
        
        // 配置摘要
        binding.btnSummary.setOnClickListener {
            showConfigSummary()
        }
    }
    
    private fun testFontSize() {
        try {
            val currentPrefs = configManager.getCurrentPreferences()
            val currentSize = (currentPrefs.fontSize ?: 1.0) * 16.0
            val newSize = if (currentSize < 20.0) currentSize + 2.0 else 12.0
            
            Log.d("ReadiumSimpleTest", "字体大小测试: $currentSize -> $newSize")
            
            configManager.setFontSize(newSize.toFloat())
            
            val updatedPrefs = configManager.getCurrentPreferences()
            Log.d("ReadiumSimpleTest", "更新后的配置: $updatedPrefs")
            
            Toast.makeText(this, "字体大小: ${currentSize.toInt()}pt -> ${newSize.toInt()}pt", Toast.LENGTH_SHORT).show()
            
            // 强制刷新UI显示
            runOnUiThread {
                displayCurrentConfig()
            }
            
        } catch (e: Exception) {
            Log.e("ReadiumSimpleTest", "字体大小测试失败", e)
            Toast.makeText(this, "测试失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun testTheme() {
        try {
            val themes = listOf("default", "sepia", "night", "high_contrast")
            val currentPrefs = configManager.getCurrentPreferences()
            val currentTheme = when (currentPrefs.theme) {
                org.readium.r2.navigator.preferences.Theme.SEPIA -> "sepia"
                org.readium.r2.navigator.preferences.Theme.DARK -> "night"
                else -> "default"
            }
            
            val currentIndex = themes.indexOf(currentTheme)
            val nextIndex = (currentIndex + 1) % themes.size
            val newTheme = themes[nextIndex]
            
            Log.d("ReadiumSimpleTest", "主题测试: $currentTheme -> $newTheme")
            
            configManager.setTheme(newTheme)
            
            val updatedPrefs = configManager.getCurrentPreferences()
            Log.d("ReadiumSimpleTest", "更新后的配置: $updatedPrefs")
            
            Toast.makeText(this, "主题: $currentTheme -> $newTheme", Toast.LENGTH_SHORT).show()
            
            // 强制刷新UI显示
            runOnUiThread {
                displayCurrentConfig()
            }
            
        } catch (e: Exception) {
            Log.e("ReadiumSimpleTest", "主题测试失败", e)
            Toast.makeText(this, "测试失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun testFontFamily() {
        try {
            val families = listOf("default", "serif", "sans-serif", "monospace", "cursive")
            val currentPrefs = configManager.getCurrentPreferences()
            val currentFamily = currentPrefs.fontFamily?.name ?: "default"
            
            val currentIndex = families.indexOf(currentFamily)
            val nextIndex = (currentIndex + 1) % families.size
            val newFamily = families[nextIndex]
            
            Log.d("ReadiumSimpleTest", "字体族测试: $currentFamily -> $newFamily")
            
            configManager.setFontFamily(newFamily)
            
            val updatedPrefs = configManager.getCurrentPreferences()
            Log.d("ReadiumSimpleTest", "更新后的配置: $updatedPrefs")
            
            Toast.makeText(this, "字体族: $currentFamily -> $newFamily", Toast.LENGTH_SHORT).show()
            
            // 强制刷新UI显示
            runOnUiThread {
                displayCurrentConfig()
            }
            
        } catch (e: Exception) {
            Log.e("ReadiumSimpleTest", "字体族测试失败", e)
            Toast.makeText(this, "测试失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun testLineHeight() {
        try {
            val currentPrefs = configManager.getCurrentPreferences()
            val currentHeight = currentPrefs.lineHeight ?: 1.2
            val newHeight = if (currentHeight < 2.0) currentHeight + 0.2 else 1.0
            
            Log.d("ReadiumSimpleTest", "行高测试: $currentHeight -> $newHeight")
            
            configManager.setLineHeight(newHeight.toFloat())
            
            val updatedPrefs = configManager.getCurrentPreferences()
            Log.d("ReadiumSimpleTest", "更新后的配置: $updatedPrefs")
            
            Toast.makeText(this, "行高: ${String.format("%.1f", currentHeight)} -> ${String.format("%.1f", newHeight)}", Toast.LENGTH_SHORT).show()
            
            // 强制刷新UI显示
            runOnUiThread {
                displayCurrentConfig()
            }
            
        } catch (e: Exception) {
            Log.e("ReadiumSimpleTest", "行高测试失败", e)
            Toast.makeText(this, "测试失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun testPageMargins() {
        try {
            val currentPrefs = configManager.getCurrentPreferences()
            val currentMargins = currentPrefs.pageMargins ?: 1.0
            val newMargins = if (currentMargins < 2.0) currentMargins + 0.2 else 0.5
            
            Log.d("ReadiumSimpleTest", "页边距测试: $currentMargins -> $newMargins")
            
            configManager.setPageMargins(newMargins.toFloat())
            
            val updatedPrefs = configManager.getCurrentPreferences()
            Log.d("ReadiumSimpleTest", "更新后的配置: $updatedPrefs")
            
            Toast.makeText(this, "页边距: ${String.format("%.1f", currentMargins)} -> ${String.format("%.1f", newMargins)}", Toast.LENGTH_SHORT).show()
            
            // 强制刷新UI显示
            runOnUiThread {
                displayCurrentConfig()
            }
            
        } catch (e: Exception) {
            Log.e("ReadiumSimpleTest", "页边距测试失败", e)
            Toast.makeText(this, "测试失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun resetConfig() {
        try {
            Log.d("ReadiumSimpleTest", "重置配置")
            configManager.resetToDefaults()
                    Toast.makeText(this, "配置已重置为默认值", Toast.LENGTH_SHORT).show()
        
        // 强制刷新UI显示
        runOnUiThread {
            displayCurrentConfig()
        }
        } catch (e: Exception) {
            Log.e("ReadiumSimpleTest", "重置配置失败", e)
            Toast.makeText(this, "重置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun displayCurrentConfig() {
        try {
            val preferences = configManager.getCurrentPreferences()
            
            val configText = """
                当前配置:
                ==========
                主题: ${preferences.theme}
                字体大小: ${(preferences.fontSize ?: 1.0) * 16.0}pt
                字体族: ${preferences.fontFamily?.name ?: "默认"}
                行高: ${preferences.lineHeight ?: 1.2}
                页边距: ${preferences.pageMargins ?: 1.0}
                文本对齐: ${preferences.textAlign}
                列数: ${preferences.columnCount}
                滚动模式: ${if (preferences.scroll == true) "是" else "否"}
                出版商样式: ${if (preferences.publisherStyles == true) "启用" else "禁用"}
                夜间模式: ${if (preferences.theme == org.readium.r2.navigator.preferences.Theme.DARK) "是" else "否"}
                高对比度: ${if (preferences.textColor != null && preferences.backgroundColor != null) "是" else "否"}
            """.trimIndent()
            
            binding.tvCurrentConfig.text = configText
            
            // 同时显示EpubPreferences的原始数据
            val preferencesText = """
                EpubPreferences原始数据:
                ======================
                $preferences
            """.trimIndent()
            
            binding.tvEpubPreferences.text = preferencesText
            
            Log.d("ReadiumSimpleTest", "配置显示更新完成")
            
        } catch (e: Exception) {
            Log.e("ReadiumSimpleTest", "显示配置失败", e)
            binding.tvCurrentConfig.text = "显示配置失败: ${e.message}"
            binding.tvEpubPreferences.text = "EpubPreferences获取失败: ${e.message}"
        }
    }
    
    private fun showConfigSummary() {
        try {
            val summary = configManager.getConfigSummary()
            Toast.makeText(this, summary, Toast.LENGTH_LONG).show()
            Log.d("ReadiumSimpleTest", "配置摘要: $summary")
        } catch (e: Exception) {
            Log.e("ReadiumSimpleTest", "显示配置摘要失败", e)
            Toast.makeText(this, "获取摘要失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
