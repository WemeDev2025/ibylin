package com.ibylin.app.ui

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.ibylin.app.databinding.ActivityReadiumSettingsBinding
import com.ibylin.app.utils.ReadiumConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Readium 阅读器设置界面
 * 提供阅读器的各种配置选项
 */
@AndroidEntryPoint
class ReadiumSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReadiumSettingsBinding
    
    @Inject
    lateinit var readiumConfig: ReadiumConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadiumSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        setupListeners()
        loadCurrentSettings()
    }

    private fun initViews() {
        // 设置工具栏
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "阅读器设置"
        
        // 设置主题选择器
        val themes = readiumConfig.getAvailableThemes()
        val themeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, themes)
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTheme.adapter = themeAdapter
        
        // 设置翻页模式选择器
        val pageTurnModes = readiumConfig.getAvailablePageTurnModes()
        val pageTurnAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, pageTurnModes)
        pageTurnAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPageTurnMode.adapter = pageTurnAdapter
    }

    private fun setupListeners() {
        // 字体大小调节
        binding.seekBarFontSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val fontSize = 12f + progress * 2f // 12-32px
                    binding.tvFontSizeValue.text = "${fontSize.toInt()}px"
                    readiumConfig.fontSize = fontSize
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 行间距调节
        binding.seekBarLineSpacing.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val lineSpacing = 1.0f + progress * 0.1f // 1.0-2.0
                    binding.tvLineSpacingValue.text = String.format("%.1f", lineSpacing)
                    readiumConfig.lineSpacing = lineSpacing
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 主题选择
        binding.spinnerTheme.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedTheme = parent?.getItemAtPosition(position) as String
                readiumConfig.theme = selectedTheme
                applyTheme(selectedTheme)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
        
        // 翻页模式选择
        binding.spinnerPageTurnMode.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedMode = parent?.getItemAtPosition(position) as String
                readiumConfig.pageTurnMode = selectedMode
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
        
        // 开关设置
        binding.switchAutoScroll.setOnCheckedChangeListener { _, isChecked ->
            readiumConfig.autoScroll = isChecked
        }
        
        binding.switchNightMode.setOnCheckedChangeListener { _, isChecked ->
            readiumConfig.nightMode = isChecked
            applyNightMode(isChecked)
        }
        
        binding.switchFullscreen.setOnCheckedChangeListener { _, isChecked ->
            readiumConfig.fullscreen = isChecked
        }
        
        // 按钮事件
        binding.btnResetSettings.setOnClickListener {
            resetSettings()
        }
        
        binding.btnExportConfig.setOnClickListener {
            exportConfiguration()
        }
    }

    private fun loadCurrentSettings() {
        // 加载当前配置
        binding.seekBarFontSize.progress = ((readiumConfig.fontSize - 12f) / 2f).toInt()
        binding.tvFontSizeValue.text = "${readiumConfig.fontSize.toInt()}px"
        
        binding.seekBarLineSpacing.progress = ((readiumConfig.lineSpacing - 1.0f) / 0.1f).toInt()
        binding.tvLineSpacingValue.text = String.format("%.1f", readiumConfig.lineSpacing)
        
        // 设置主题选择器
        val themeIndex = readiumConfig.getAvailableThemes().indexOf(readiumConfig.theme)
        if (themeIndex >= 0) {
            binding.spinnerTheme.setSelection(themeIndex)
        }
        
        // 设置翻页模式选择器
        val pageTurnIndex = readiumConfig.getAvailablePageTurnModes().indexOf(readiumConfig.pageTurnMode)
        if (pageTurnIndex >= 0) {
            binding.spinnerPageTurnMode.setSelection(pageTurnIndex)
        }
        
        // 设置开关状态
        binding.switchAutoScroll.isChecked = readiumConfig.autoScroll
        binding.switchNightMode.isChecked = readiumConfig.nightMode
        binding.switchFullscreen.isChecked = readiumConfig.fullscreen
    }

    private fun applyTheme(theme: String) {
        when (theme) {
            "sepia" -> {
                // 应用护眼主题
                binding.root.setBackgroundColor(android.graphics.Color.parseColor("#F4F1E8"))
            }
            "night" -> {
                // 应用夜间主题
                binding.root.setBackgroundColor(android.graphics.Color.parseColor("#1A1A1A"))
            }
            "high_contrast" -> {
                // 应用高对比度主题
                binding.root.setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
            }
            else -> {
                // 默认主题
                binding.root.setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
            }
        }
    }

    private fun applyNightMode(enabled: Boolean) {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun resetSettings() {
        readiumConfig.resetToDefaults()
        loadCurrentSettings()
        Toast.makeText(this, "设置已重置为默认值", Toast.LENGTH_SHORT).show()
    }

    private fun exportConfiguration() {
        val configText = readiumConfig.exportConfig()
        // 这里可以添加导出到文件或分享的功能
        Toast.makeText(this, "配置已导出", Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
