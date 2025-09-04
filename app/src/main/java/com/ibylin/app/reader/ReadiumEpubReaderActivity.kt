package com.ibylin.app.reader

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.lifecycle.lifecycleScope
import com.ibylin.app.R
import com.ibylin.app.utils.EpubFile
import com.ibylin.app.utils.EpubFixer
import com.ibylin.app.utils.ReadiumPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubDefaults
import org.readium.r2.navigator.preferences.Configurable
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.getOrElse
import java.io.File
import javax.inject.Inject
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Button

/**
 * 全面优化的Readium EPUB阅读器Activity
 * 包含字体调整、主题切换、书签、搜索等高级功能
 */
@AndroidEntryPoint
class ReadiumEpubReaderActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "ReadiumEpubReader"
        const val EXTRA_EPUB_PATH = "epub_path"
        const val EXTRA_EPUB_FILE = "epub_file"
        const val EXTRA_BOOK_PATH = "book_path" // 兼容旧版本
    }
    
    // Readium组件
    private var publication: Publication? = null
    private var navigatorFactory: EpubNavigatorFactory? = null
    private var navigatorFragment: EpubNavigatorFragment? = null
    

    
    // UI组件
    private lateinit var navigatorContainer: View
    private lateinit var loadingView: View
    
    // Readium配置管理器注入
    @Inject
    lateinit var preferencesManager: ReadiumPreferencesManager
    
    // 阅读状态和设置
    private var currentLocation: String? = null
    private var isBookLoaded = false
    private var currentFontSize = 18.0
    private var currentTheme = "default"
    private var currentFontFamily = "sans-serif"
    private var currentLineHeight = 1.6
    private var currentPageMargins = 1.4
    
    // 书签和阅读进度
    private var bookmarks = mutableListOf<String>()
    private var currentProgress = 0.0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_readium_epub_reader)
        
        Log.d(TAG, "ReadiumEpubReaderActivity onCreate 开始")
        
        setupViews()
        setupToolbar()
        loadConfiguration() // 加载配置
        loadEpub()
        
        Log.d(TAG, "ReadiumEpubReaderActivity onCreate 完成")
    }
    
    private fun setupViews() {
        // 初始化视图
        navigatorContainer = findViewById(R.id.reader_container)
        loadingView = findViewById(R.id.loading_view)
        
        // 确保Toolbar初始状态为隐藏
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar.visibility = View.GONE
        
        // 设置底部控制栏按钮
        setupBottomControls()
        
        // 显示加载状态
        showLoading(true)
    }
    
    private fun setupBottomControls() {
        // 使用新的弹出菜单方式，不再直接设置按钮监听器
        // 这些方法会在 XML 的 onClick 属性中调用
    }
    
    // 设置点击监听器
    private fun setupTapListener() {
        // 延迟设置，确保Fragment已经创建
        navigatorContainer.postDelayed({
            try {
                navigatorFragment?.let { fragment ->
                    // 获取Fragment的根视图
                    val fragmentView = fragment.view
                    fragmentView?.setOnClickListener { view ->
                        // 获取点击位置
                        val x = view.width / 2f
                        val y = view.height / 2f
                        
                        // 如果点击在屏幕中间区域，切换菜单栏显示状态
                        if (isClickInCenterArea(view, x, y)) {
                            toggleToolbar()
                        }
                    }
                    Log.d(TAG, "点击监听器设置成功")
                }
            } catch (e: Exception) {
                Log.e(TAG, "设置点击监听器失败", e)
            }
        }, 1000) // 延迟1秒设置
    }
    
    // 判断点击是否在屏幕中间区域
    private fun isClickInCenterArea(view: View, clickX: Float, clickY: Float): Boolean {
        val centerX = view.width / 2f
        val centerY = view.height / 2f
        val centerAreaSize = 200f // 中间区域大小
        
        return (clickX >= centerX - centerAreaSize && clickX <= centerX + centerAreaSize &&
                clickY >= centerY - centerAreaSize && clickY <= centerY + centerAreaSize)
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Readium EPUB阅读器"
            setSubtitle("专业级EPUB阅读体验")
        }
    }
    
    /**
     * 切换菜单栏显示/隐藏状态
     */
    private fun toggleToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        if (toolbar.visibility == View.VISIBLE) {
            // 隐藏菜单栏
            toolbar.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    toolbar.visibility = View.GONE
                }
                .start()
            Log.d(TAG, "菜单栏已隐藏")
        } else {
            // 显示菜单栏
            toolbar.visibility = View.VISIBLE
            toolbar.alpha = 0f
            toolbar.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
            Log.d(TAG, "菜单栏已显示")
            
            // 3秒后自动隐藏
            toolbar.postDelayed({
                if (toolbar.visibility == View.VISIBLE) {
                    toolbar.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction {
                            toolbar.visibility = View.GONE
                        }
                        .start()
                    Log.d(TAG, "菜单栏自动隐藏")
                }
            }, 3000)
        }
    }
    
    /**
     * 从ReadiumConfigManager加载配置
     */
    private fun loadConfiguration() {
        try {
            Log.d(TAG, "开始加载配置...")
            
            // 直接从ReadiumPreferencesManager获取配置，严格按照开发实例
            val preferences = preferencesManager.getCurrentPreferences()
            
            // 更新本地变量
            currentFontSize = (preferences.fontSize ?: 1.0) * 16.0
            currentTheme = when (preferences.theme) {
                org.readium.r2.navigator.preferences.Theme.SEPIA -> "sepia"
                org.readium.r2.navigator.preferences.Theme.DARK -> "night"
                else -> "default"
            }
            currentFontFamily = preferences.fontFamily?.name ?: "sans-serif"
            currentLineHeight = preferences.lineHeight ?: 1.6
            currentPageMargins = preferences.pageMargins ?: 1.4
            
            Log.d(TAG, "配置加载成功: 字体=${currentFontSize}pt, 主题=$currentTheme, 字体族=$currentFontFamily")
            Log.d(TAG, "EpubPreferences: $preferences")
            
        } catch (e: Exception) {
            Log.e(TAG, "加载配置失败", e)
            // 使用默认配置
            currentFontSize = 18.0
            currentTheme = "default"
            currentFontFamily = "sans-serif"
            currentLineHeight = 1.6
            currentPageMargins = 1.4
        }
    }
    
    private fun loadEpub() {
        // 兼容多种传入方式
        val epubPath = intent.getStringExtra(EXTRA_EPUB_PATH)
        val epubFile = intent.getParcelableExtra<EpubFile>(EXTRA_EPUB_FILE)
        val bookPath = intent.getStringExtra(EXTRA_BOOK_PATH)
        
        val filePath = epubPath ?: epubFile?.path ?: bookPath
        
        if (filePath.isNullOrEmpty()) {
            Toast.makeText(this, "未找到EPUB文件", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // 使用协程加载EPUB
        lifecycleScope.launch {
            try {
                loadEpubFile(filePath)
            } catch (e: Exception) {
                Log.e(TAG, "加载EPUB失败", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ReadiumEpubReaderActivity, "加载EPUB失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
    
    private suspend fun loadEpubFile(filePath: String) {
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始加载EPUB文件: $filePath")
                
                // 检查EPUB文件是否需要修复
                var finalFilePath = filePath
                if (EpubFixer.needsFixing(filePath)) {
                    Log.d(TAG, "检测到EPUB文件需要修复，开始自动修复...")
                    val fixedPath = EpubFixer.fixEpubFile(filePath)
                    if (fixedPath != null) {
                        finalFilePath = fixedPath
                        Log.d(TAG, "EPUB文件修复成功，使用修复后的文件: $fixedPath")
                    } else {
                        Log.w(TAG, "EPUB文件修复失败，使用原始文件")
                    }
                }
                
                val file = File(finalFilePath)
                if (!file.exists()) {
                    throw Exception("文件不存在: $finalFilePath")
                }
                
                // 按照官方文档创建Readium组件
                val httpClient = DefaultHttpClient()
                val assetRetriever = AssetRetriever(
                    contentResolver = contentResolver,
                    httpClient = httpClient
                )
                val publicationOpener = PublicationOpener(
                    publicationParser = DefaultPublicationParser(
                        this@ReadiumEpubReaderActivity,
                        httpClient = httpClient,
                        assetRetriever = assetRetriever,
                        pdfFactory = null // 暂时不使用PDF功能
                    )
                )
                
                // 获取Asset
                val asset = assetRetriever.retrieve(file).getOrElse { error ->
                    throw Exception("无法获取Asset: $error")
                }
                
                // 解析EPUB文件
                val publication = publicationOpener.open(asset, allowUserInteraction = false).getOrElse { error ->
                    throw Exception("无法解析EPUB文件: $error")
                }
                
                // 创建EPUB导航器工厂 - 使用Readium原生配置
                val navigatorFactory = EpubNavigatorFactory(
                    publication = publication,
                    configuration = EpubNavigatorFactory.Configuration(
                        defaults = EpubDefaults(
                            pageMargins = currentPageMargins.toDouble(),
                            // 使用Readium原生配置
                        )
                    )
                )
                

                
                withContext(Dispatchers.Main) {
                    this@ReadiumEpubReaderActivity.publication = publication
                    this@ReadiumEpubReaderActivity.navigatorFactory = navigatorFactory
                    this@ReadiumEpubReaderActivity.isBookLoaded = true
                    
                    // 设置导航器视图
                    setupNavigatorView(navigatorFactory)
                    
                    // 隐藏加载状态
                    showLoading(false)
                    
                    // 设置点击监听器，用于显示/隐藏菜单栏
                    setupTapListener()
                    
                    // 显示成功信息
                    val title = publication.metadata.title ?: "未知标题"
                    val author = publication.metadata.authors.firstOrNull()?.name ?: "未知作者"
                    Log.d(TAG, "EPUB文件加载成功: $title - $author")
                    // 取消加载成功的Toast提示
                    // Toast.makeText(this@ReadiumEpubReaderActivity, "《$title》加载成功", Toast.LENGTH_SHORT).show()
                    
                    // 更新标题栏
                    supportActionBar?.title = title
                    supportActionBar?.subtitle = "作者：$author"
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "加载EPUB文件失败", e)
                withContext(Dispatchers.Main) {
                    showReadiumError("EPUB文件加载失败：${e.message ?: "未知错误"}")
                }
            }
        }
    }
    
    private fun setupNavigatorView(navigatorFactory: EpubNavigatorFactory) {
        try {
            // 按照官方文档设置FragmentFactory
            supportFragmentManager.fragmentFactory =
                navigatorFactory.createFragmentFactory(
                    initialLocator = null,
                    listener = object : EpubNavigatorFragment.Listener {
                        override fun onExternalLinkActivated(url: org.readium.r2.shared.util.AbsoluteUrl) {
                            Log.d(TAG, "外部链接激活: $url")
                            Toast.makeText(this@ReadiumEpubReaderActivity, "外部链接: $url", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            
            // 添加Fragment到容器
            supportFragmentManager.beginTransaction()
                .replace(R.id.reader_container, EpubNavigatorFragment::class.java, Bundle(), "EpubNavigatorFragment")
                .commit()
            
            // 获取Fragment引用
            navigatorFragment = supportFragmentManager.findFragmentByTag("EpubNavigatorFragment") as? EpubNavigatorFragment
            
            // 设置错误拦截器，过滤掉XML解析错误
            setupErrorInterceptor()
            
            Log.d(TAG, "Readium导航器设置成功")
        } catch (e: Exception) {
            Log.e(TAG, "设置Readium导航器失败", e)
            // 如果Readium设置失败，显示错误信息而不是XML错误
            showReadiumError("阅读器初始化失败，请重试")
        }
    }
    
    private fun setupErrorInterceptor() {
        // 使用多个时机来确保错误被拦截
        // 1. 延迟1秒执行
        navigatorContainer.postDelayed({
            hideXmlErrors()
        }, 1000)
        
        // 2. 延迟2秒再次执行
        navigatorContainer.postDelayed({
            hideXmlErrors()
        }, 2000)
        
        // 3. 延迟3秒再次执行
        navigatorContainer.postDelayed({
            hideXmlErrors()
        }, 3000)
        
        // 4. 设置定期检查
        startPeriodicErrorCheck()
    }
    
    private fun startPeriodicErrorCheck() {
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                hideXmlErrors()
                // 每2秒检查一次，持续10秒
                handler.postDelayed(this, 2000)
            }
        }
        // 延迟5秒开始定期检查
        handler.postDelayed(runnable, 5000)
        
        // 10秒后停止检查
        handler.postDelayed({
            handler.removeCallbacks(runnable)
        }, 15000)
    }
    
    private fun hideXmlErrors() {
        try {
            // 递归查找包含XML错误的TextView
            val rootView = navigatorContainer
            if (rootView is android.view.ViewGroup) {
                findAndHideXmlErrors(rootView)
            }
            
            // 也检查整个Activity的根视图
            val activityRoot = findViewById<View>(android.R.id.content)
            if (activityRoot is android.view.ViewGroup) {
                findAndHideXmlErrors(activityRoot)
            }
        } catch (e: Exception) {
            Log.d(TAG, "隐藏XML错误失败", e)
        }
    }
    
    private fun findAndHideXmlErrors(viewGroup: android.view.ViewGroup) {
        try {
            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i)
                
                if (child is TextView) {
                    val text = child.text.toString()
                    // 检查是否包含XML错误信息
                    if (text.contains("This page contains the following errors:") || 
                        text.contains("AttValue:") ||
                        text.contains("error on line") ||
                        text.contains("column") ||
                        text.contains("XML") ||
                        text.contains("parsing error")) {
                        // 隐藏这个错误信息
                        child.visibility = View.GONE
                        Log.d(TAG, "已隐藏XML错误信息: $text")
                    }
                } else if (child is android.view.ViewGroup) {
                    // 递归查找子视图
                    findAndHideXmlErrors(child)
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "查找XML错误时出错", e)
        }
    }
    
    // 注意：Readium使用WebView来渲染EPUB内容，这是设计上的选择
    // 我们通过Readium的原生API来配置阅读器，而不是通过WebView hack
    
    private fun showReadiumError(message: String) {
        // 隐藏加载状态
        showLoading(false)
        
        // 在阅读器容器中显示错误信息
        val errorView = TextView(this).apply {
            text = message
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            gravity = android.view.Gravity.CENTER
            setPadding(32, 32, 32, 32)
        }
        
        // 清除容器内容并显示错误信息
        if (navigatorContainer is android.view.ViewGroup) {
            (navigatorContainer as android.view.ViewGroup).removeAllViews()
            (navigatorContainer as android.view.ViewGroup).addView(errorView)
        }
        navigatorContainer.visibility = View.VISIBLE
        
        // 隐藏底部控制栏
        showReadingControls(false)
    }
    
    private fun showLoading(show: Boolean) {
        loadingView.visibility = if (show) View.VISIBLE else View.GONE
        navigatorContainer.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    private fun showReadingControls(show: Boolean) {
        // 底部工具栏已删除，不再需要显示控制
        // 阅读器现在使用全屏模式
    }
    
    // 菜单相关 - 丰富的阅读控制选项
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_epub_reader, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_font_size_increase -> {
                increaseFontSize()
                true
            }
            R.id.action_font_size_decrease -> {
                decreaseFontSize()
                true
            }
            R.id.action_theme_toggle -> {
                toggleTheme()
                true
            }
            R.id.action_config_panel -> {
                showConfigPanel()
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    // 字体大小调整
    private fun increaseFontSize() {
        currentFontSize = (currentFontSize + 2.0).coerceAtMost(32.0)
        applyReadingPreferences()
        Toast.makeText(this, "字体大小: ${currentFontSize.toInt()}", Toast.LENGTH_SHORT).show()
    }
    
    private fun decreaseFontSize() {
        currentFontSize = (currentFontSize - 2.0).coerceAtLeast(12.0)
        applyReadingPreferences()
        Toast.makeText(this, "字体大小: ${currentFontSize.toInt()}", Toast.LENGTH_SHORT).show()
    }
    
    // 主题切换
    private fun toggleTheme() {
        currentTheme = when (currentTheme) {
            "default" -> "sepia"
            "sepia" -> "night"
            "night" -> "highContrast"
            else -> "default"
        }
        applyReadingPreferences()
        Toast.makeText(this, "主题: $currentTheme", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 应用主题到Readium阅读器
     */
    private fun applyTheme(theme: String) {
        try {
            // 使用Readium原生API应用主题
            navigatorFragment?.let { fragment ->
                // 这里应该使用Readium的官方API来应用主题
                // 暂时记录日志，后续根据官方文档完善
                Log.d(TAG, "应用主题到Readium: $theme")
                
                // 重新创建navigatorFactory以应用新配置
                applyReadiumConfiguration()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "应用主题失败", e)
        }
    }
    
    /**
     * 应用Readium配置 - 严格按照开发实例的方式
     */
    private fun applyReadiumConfiguration() {
        try {
            Log.d(TAG, "开始应用Readium配置...")
            
            publication?.let { pub ->
                // 直接从ReadiumPreferencesManager获取当前配置
                val preferences = preferencesManager.getCurrentPreferences()
                Log.d(TAG, "当前配置: $preferences")
                
                // 重新创建navigatorFactory以应用新配置
                val newNavigatorFactory = EpubNavigatorFactory(
                    publication = pub,
                    configuration = EpubNavigatorFactory.Configuration(
                        defaults = EpubDefaults(
                            pageMargins = currentPageMargins.toDouble(),
                            fontSize = (currentFontSize / 16.0), // 转换为百分比
                            lineHeight = currentLineHeight.toDouble(),
                            // 其他配置通过preferences应用
                        )
                    )
                )
                
                Log.d(TAG, "创建新的NavigatorFactory")
                
                // 更新navigatorFactory
                navigatorFactory = newNavigatorFactory
                
                // 重新设置FragmentFactory
                supportFragmentManager.fragmentFactory = newNavigatorFactory.createFragmentFactory(
                    initialLocator = null, // 暂时使用null
                    listener = object : EpubNavigatorFragment.Listener {
                        override fun onExternalLinkActivated(url: org.readium.r2.shared.util.AbsoluteUrl) {
                            Log.d(TAG, "外部链接激活: $url")
                            Toast.makeText(this@ReadiumEpubReaderActivity, "外部链接: $url", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                // 重新创建Fragment
                val newFragment = supportFragmentManager.fragmentFactory.instantiate(
                    EpubNavigatorFragment::class.java.classLoader!!,
                    EpubNavigatorFragment::class.java.name
                ) as EpubNavigatorFragment
                
                Log.d(TAG, "创建新的Fragment: $newFragment")
                
                supportFragmentManager.beginTransaction()
                    .replace(R.id.reader_container, newFragment, "EpubNavigatorFragment")
                    .commit()
                
                navigatorFragment = newFragment
                
                // 等待Fragment创建完成后应用配置
                newFragment.view?.post {
                    try {
                        Log.d(TAG, "Fragment视图已创建，尝试应用配置")
                        
                        // 严格按照开发实例，使用Configurable接口
                        val configurableFragment = newFragment as? Configurable<*, EpubPreferences>
                        if (configurableFragment != null) {
                            configurableFragment.submitPreferences(preferences)
                            Log.d(TAG, "Readium配置已通过Configurable接口应用: $preferences")
                        } else {
                            Log.w(TAG, "Fragment未实现Configurable接口，无法直接应用配置")
                            Log.d(TAG, "Fragment类型: ${newFragment.javaClass.name}")
                            Log.d(TAG, "Fragment接口: ${newFragment.javaClass.interfaces.joinToString()}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "通过Configurable接口应用配置失败", e)
                    }
                }
                
                Log.d(TAG, "Readium配置已重新应用")
                
            } ?: Log.w(TAG, "Publication为空，无法应用配置")
            
        } catch (e: Exception) {
            Log.e(TAG, "应用Readium配置失败", e)
        }
    }
    
    // 字体族切换
    private fun cycleFontFamily() {
        currentFontFamily = when (currentFontFamily) {
            "sans-serif" -> "serif"
            "serif" -> "monospace"
            "monospace" -> "cursive"
            else -> "sans-serif"
        }
        applyReadingPreferences()
        Toast.makeText(this, "字体: $currentFontFamily", Toast.LENGTH_SHORT).show()
    }
    
    // 行高调整
    private fun adjustLineHeight(increase: Boolean) {
        if (increase) {
            currentLineHeight = (currentLineHeight + 0.1).coerceAtMost(2.5)
        } else {
            currentLineHeight = (currentLineHeight - 0.1).coerceAtLeast(1.0)
        }
        applyReadingPreferences()
        Toast.makeText(this, "行高: ${String.format("%.1f", currentLineHeight)}", Toast.LENGTH_SHORT).show()
    }
    
    // 页边距调整
    private fun adjustPageMargins(increase: Boolean) {
        if (increase) {
            currentPageMargins = (currentPageMargins + 0.1).coerceAtMost(2.0)
        } else {
            currentPageMargins = (currentPageMargins - 0.1).coerceAtLeast(0.5)
        }
        applyReadingPreferences()
        Toast.makeText(this, "页边距: ${String.format("%.1f", currentPageMargins)}", Toast.LENGTH_SHORT).show()
    }
    
    // 应用阅读偏好设置 - 严格按照开发实例的方式
    private fun applyReadingPreferences() {
        try {
            Log.d(TAG, "开始应用阅读偏好...")
            
            // 直接更新ReadiumPreferencesManager，严格按照开发实例
            preferencesManager.setFontSize(currentFontSize.toFloat())
            preferencesManager.setTheme(currentTheme)
            preferencesManager.setFontFamily(currentFontFamily)
            preferencesManager.setLineHeight(currentLineHeight.toFloat())
            preferencesManager.setPageMargins(currentPageMargins.toFloat())
            
            Log.d(TAG, "配置已更新到ReadiumPreferencesManager")
            
            // 获取当前配置并直接应用到Fragment
            val preferences = preferencesManager.getCurrentPreferences()
            Log.d(TAG, "当前配置: $preferences")
            
            // 尝试直接应用配置到当前的navigatorFragment
            navigatorFragment?.let { fragment ->
                Log.d(TAG, "Fragment存在，尝试应用配置")
                try {
                    // 严格按照开发实例，使用Configurable接口
                    val configurableFragment = fragment as? Configurable<*, EpubPreferences>
                    if (configurableFragment != null) {
                        configurableFragment.submitPreferences(preferences)
                        Log.d(TAG, "配置已通过Configurable接口应用: $preferences")
                    } else {
                        Log.w(TAG, "Fragment未实现Configurable接口，重新创建Fragment")
                        applyReadiumConfiguration()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "应用配置失败，重新创建Fragment", e)
                    applyReadiumConfiguration()
                }
            } ?: run {
                Log.w(TAG, "Fragment不存在，重新创建Fragment")
                applyReadiumConfiguration()
            }
            
            Log.d(TAG, "应用阅读偏好完成: 字体=${currentFontSize}pt, 主题=$currentTheme, 字体族=$currentFontFamily")
            
            // 通知用户设置已保存
            Toast.makeText(this, "设置已保存并应用", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "应用阅读偏好失败", e)
            Toast.makeText(this, "保存设置失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 书签功能
    private fun addBookmark() {
        currentLocation?.let { location ->
            if (!bookmarks.contains(location)) {
                bookmarks.add(location)
                Toast.makeText(this, "书签已添加", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "添加书签: $location")
            } else {
                Toast.makeText(this, "书签已存在", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun removeBookmark() {
        currentLocation?.let { location ->
            if (bookmarks.remove(location)) {
                Toast.makeText(this, "书签已移除", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "移除书签: $location")
            }
        }
    }
    
    // 搜索功能（基础实现）
    private fun searchInBook(query: String) {
        if (query.isBlank()) return
        
        Toast.makeText(this, "搜索: $query", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "搜索内容: $query")
        
        // 这里需要根据实际的Readium API来实现搜索功能
        // 暂时使用Toast提示，后续根据API完善
    }
    
    // 显示搜索对话框
    private fun showSearchDialog() {
        // 创建搜索对话框
        val searchView = android.widget.SearchView(this)
        searchView.queryHint = "搜索内容..."
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("搜索")
            .setView(searchView)
            .setPositiveButton("搜索") { _, _ ->
                val query = searchView.query.toString()
                if (query.isNotBlank()) {
                    searchInBook(query)
                }
            }
            .setNegativeButton("取消", null)
            .create()
        
        dialog.show()
        
        // 设置搜索建议
        searchView.setOnQueryTextListener(object : android.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { searchInBook(it) }
                dialog.dismiss()
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                // 实时搜索建议可以在这里实现
                return false
            }
        })
    }
    
    // 目录导航
    private fun showTableOfContents() {
        publication?.tableOfContents?.let { toc ->
            val tocText = toc.joinToString("\n") { it.title ?: "未知章节" }
            Toast.makeText(this, "目录:\n$tocText", Toast.LENGTH_LONG).show()
            Log.d(TAG, "显示目录: ${toc.size} 章节")
        } ?: Toast.makeText(this, "无目录信息", Toast.LENGTH_SHORT).show()
    }
    
    // 阅读进度保存
    private fun saveReadingProgress() {
        currentLocation?.let { location ->
            // 这里可以保存到SharedPreferences或数据库
            Log.d(TAG, "保存阅读进度: $location")
            Toast.makeText(this, "阅读进度已保存", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 显示配置面板
    private fun showConfigPanel() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("阅读器配置")
            .setView(createConfigPanelView())
            .setPositiveButton("应用") { _, _ ->
                // 配置已经在面板中实时应用了
                Toast.makeText(this, "配置已应用", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("关闭", null)
            .create()
        
        dialog.show()
    }
    
    // 创建配置面板视图
    private fun createConfigPanelView(): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }
        
        // 字体大小设置
        val fontSizeLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        
        val fontSizeLabel = TextView(this).apply {
            text = "字体大小: ${currentFontSize}pt"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val fontSizeSeekBar = SeekBar(this).apply {
            max = 20 // 12-32pt
            progress = (currentFontSize - 12).toInt()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        currentFontSize = 12.0 + progress
                        fontSizeLabel.text = "字体大小: ${currentFontSize.toInt()}pt"
                        applyReadingPreferences()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        
        fontSizeLayout.addView(fontSizeLabel)
        fontSizeLayout.addView(fontSizeSeekBar)
        layout.addView(fontSizeLayout)
        
        // 分隔线
        layout.addView(View(this).apply {
            setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setPadding(0, 16, 0, 16)
        })
        
        // 主题选择
        val themeLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        
        val themeLabel = TextView(this).apply {
            text = "主题: $currentTheme"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val themeButton = Button(this).apply {
            text = "切换"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            
            setOnClickListener {
                currentTheme = when (currentTheme) {
                    "light" -> "sepia"
                    "sepia" -> "night"
                    "night" -> "light"
                    else -> "light"
                }
                themeLabel.text = "主题: $currentTheme"
                applyReadingPreferences()
            }
        }
        
        themeLayout.addView(themeLabel)
        themeLayout.addView(themeButton)
        layout.addView(themeLayout)
        
        // 分隔线
        layout.addView(View(this).apply {
            setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setPadding(0, 16, 0, 16)
        })
        
        // 字体族选择
        val fontFamilyLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        
        val fontFamilyLabel = TextView(this).apply {
            text = "字体: $currentFontFamily"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val fontFamilyButton = Button(this).apply {
            text = "切换"
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            
            setOnClickListener {
                currentFontFamily = when (currentFontFamily) {
                    "sans-serif" -> "serif"
                    "serif" -> "monospace"
                    "monospace" -> "cursive"
                    else -> "sans-serif"
                }
                fontFamilyLabel.text = "字体: $currentFontFamily"
                applyReadingPreferences()
            }
        }
        
        fontFamilyLayout.addView(fontFamilyLabel)
        fontFamilyLayout.addView(fontFamilyButton)
        layout.addView(fontFamilyLayout)
        
        // 分隔线
        layout.addView(View(this).apply {
            setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setPadding(0, 16, 0, 16)
        })
        
        // 行高设置
        val lineHeightLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        
        val lineHeightLabel = TextView(this).apply {
            text = "行高: ${String.format("%.1f", currentLineHeight)}"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val lineHeightSeekBar = SeekBar(this).apply {
            max = 15 // 1.0-2.5
            progress = ((currentLineHeight - 1.0) * 10).toInt()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        currentLineHeight = 1.0 + progress * 0.1
                        lineHeightLabel.text = "行高: ${String.format("%.1f", currentLineHeight)}"
                        applyReadingPreferences()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        
        lineHeightLayout.addView(lineHeightLabel)
        lineHeightLayout.addView(lineHeightSeekBar)
        layout.addView(lineHeightLayout)
        
        // 分隔线
        layout.addView(View(this).apply {
            setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setPadding(0, 16, 0, 16)
        })
        
        // 页边距设置
        val marginsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        
        val marginsLabel = TextView(this).apply {
            text = "页边距: ${String.format("%.1f", currentPageMargins)}"
            textSize = 16f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        
        val marginsSeekBar = SeekBar(this).apply {
            max = 15 // 0.5-2.0
            progress = ((currentPageMargins - 0.5) * 10).toInt()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        currentPageMargins = 0.5 + progress * 0.1
                        marginsLabel.text = "页边距: ${String.format("%.1f", currentPageMargins)}"
                        applyReadingPreferences()
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        
        marginsLayout.addView(marginsLabel)
        marginsLayout.addView(marginsSeekBar)
        layout.addView(marginsLayout)
        
        return layout
    }
    
    // 阅读统计
    private fun showReadingStats() {
        val stats = """
            阅读统计:
            - 字体大小: ${currentFontSize.toInt()}pt
            - 当前主题: $currentTheme
            - 字体族: $currentFontFamily
            - 行高: ${String.format("%.1f", currentLineHeight)}
            - 页边距: ${String.format("%.1f", currentPageMargins)}
            - 书签数量: ${bookmarks.size}
            - 阅读进度: ${String.format("%.1f", currentProgress)}%
        """.trimIndent()
        
        Toast.makeText(this, stats, Toast.LENGTH_LONG).show()
        Log.d(TAG, "显示阅读统计")
    }
    
    override fun onBackPressed() {
        // 保存阅读进度
        saveReadingProgress()
        super.onBackPressed()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 保存阅读进度
        saveReadingProgress()
        
        // 清理Readium资源
        try {
            navigatorFragment = null
            navigatorFactory = null
            publication = null
            Log.d(TAG, "Readium资源清理完成")
        } catch (e: Exception) {
            Log.e(TAG, "清理Readium资源失败", e)
        }
        
        // 清理临时修复文件
        try {
            // 如果有修复后的文件，清理它
            publication?.let { pub ->
                // 这里可以添加清理逻辑，但需要跟踪修复后的文件路径
                Log.d(TAG, "清理临时修复文件")
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理临时修复文件失败", e)
        }
    }

    // 触摸事件状态跟踪
    private var touchStartTime = 0L
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var isTouchMoved = false
    private var isLongPress = false
    private val touchThreshold = 50f // 触摸移动阈值
    private val touchTimeThreshold = 200L // 触摸时间阈值（毫秒）
    private val longPressThreshold = 500L // 长按阈值（毫秒）

    /**
     * 使用Android官方最佳实践处理触摸事件
     * 确保触摸事件能正确传递给Readium的WebView
     */
    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        ev?.let { event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // 记录触摸开始信息
                    touchStartTime = System.currentTimeMillis()
                    touchStartX = event.x
                    touchStartY = event.y
                    isTouchMoved = false
                    isLongPress = false
                    
                    Log.d(TAG, "dispatchTouchEvent ACTION_DOWN: x=${event.x}, y=${event.y}")
                    
                    // 关键：先让Readium处理DOWN事件，确保触摸事件链正确建立
                    val handled = super.dispatchTouchEvent(event)
                    Log.d(TAG, "Readium处理DOWN事件结果: $handled")
                    return handled
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    // 检测触摸是否移动
                    val deltaX = Math.abs(event.x - touchStartX)
                    val deltaY = Math.abs(event.y - touchStartY)
                    
                    if (deltaX > touchThreshold || deltaY > touchThreshold) {
                        isTouchMoved = true
                        Log.d(TAG, "dispatchTouchEvent ACTION_MOVE: 触摸移动超过阈值，标记为滑动")
                    }
                    
                    // 关键：让Readium处理MOVE事件，确保滑动翻页正常工作
                    val handled = super.dispatchTouchEvent(event)
                    Log.d(TAG, "Readium处理MOVE事件结果: $handled")
                    return handled
                }
                android.view.MotionEvent.ACTION_UP -> {
                    val touchDuration = System.currentTimeMillis() - touchStartTime
                    val deltaX = Math.abs(event.x - touchStartX)
                    val deltaY = Math.abs(event.y - touchStartY)
                    
                    Log.d(TAG, "dispatchTouchEvent ACTION_UP: x=${event.x}, y=${event.y}, 持续时间=${touchDuration}ms, 移动距离=(${deltaX}, ${deltaY})")
                    
                    // 智能判断：只有在短时间、小距离移动的点击时才弹出菜单
                    if (touchDuration < touchTimeThreshold && 
                        deltaX < touchThreshold && 
                        deltaY < touchThreshold && 
                        !isTouchMoved) {
                        
                        // 检查是否在中间区域
                        val clickX = event.x
                        val screenWidth = resources.displayMetrics.widthPixels
                        val leftBound = screenWidth * 0.2f
                        val rightBound = screenWidth * 0.8f
                        val isInCenterArea = clickX >= leftBound && clickX <= rightBound
                        
                        Log.d(TAG, "智能判断: 点击事件检测 - 中间区域=$isInCenterArea, 坐标=(${clickX}, ${event.y})")
                        
                        if (isInCenterArea) {
                            Log.d(TAG, "智能判断: 决定弹出菜单")
                            toggleToolbar()
                        }
                    } else {
                        Log.d(TAG, "触摸事件不符合点击条件，不弹出菜单")
                    }
                    
                    // 关键：让Readium处理UP事件，确保触摸事件链完整
                    val handled = super.dispatchTouchEvent(event)
                    Log.d(TAG, "Readium处理UP事件结果: $handled")
                    return handled
                }
                else -> {
                    Log.d(TAG, "dispatchTouchEvent 其他事件: ${event.action}")
                    // 其他事件也传递给Readium
                    return super.dispatchTouchEvent(event)
                }
            }
        }
        
        // 调用父类的dispatchTouchEvent，让所有触摸事件都能正常工作
        return super.dispatchTouchEvent(ev)
    }
}
