package com.ibylin.app.reader

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import android.widget.ImageButton
import android.os.CountDownTimer
import android.view.Gravity
import android.view.ViewGroup
import android.content.res.ColorStateList
import android.graphics.Color

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
    
    // Readium配置管理器注入
    @Inject
    lateinit var preferencesManager: ReadiumPreferencesManager
    
    // 阅读状态和设置
    private var isBookLoaded = false
    private var currentFontSize = 18.0
    private var currentTheme = "default"
    private var currentFontFamily = "sans-serif"
    private var currentLineHeight = 1.6
    private var currentPageMargins = 1.4
    
    // 书签和阅读进度
    private var bookmarks = mutableListOf<String>()
    private var currentProgress = 0.0
    
    // 当前书籍信息
    private var currentBookPath: String? = null
    private var currentBookTitle: String = ""
    private var currentBookAuthor: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_readium_epub_reader)
        
        Log.d(TAG, "ReadiumEpubReaderActivity onCreate 开始")
        
        // 默认隐藏状态栏，实现全屏阅读
        hideStatusBar()
        
        setupViews()
        setupIOSMenu() // 设置iOS风格菜单
        loadConfiguration() // 加载配置
        loadEpub()
        
        Log.d(TAG, "ReadiumEpubReaderActivity onCreate 完成")
    }
    
    private fun setupViews() {
        // 初始化视图
        navigatorContainer = findViewById(R.id.reader_container)
        tvChapterTitle = findViewById(R.id.tv_chapter_title)
        tvPageInfo = findViewById(R.id.tv_page_info)
        
        // 显示加载状态
        showLoading(true)
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
            
            // 不再自动隐藏，改为点击其他区域关闭
            Log.d(TAG, "菜单栏已显示，点击其他区域可关闭")
        }
    }
    
    /**
     * 从ReadiumConfigManager和本地设置加载配置
     */
    private fun loadConfiguration() {
        try {
            Log.d(TAG, "开始加载配置...")
            
            // 加载本地保存的设置
            val sharedPrefs = getSharedPreferences("reader_settings", MODE_PRIVATE)
            
            // 加载亮度设置
            val savedBrightness = sharedPrefs.getInt("screen_brightness", 50)
            adjustScreenBrightness(savedBrightness)
            
            // 加载主题设置
            val savedTheme = sharedPrefs.getString("theme", "默认") ?: "默认"
            currentTheme = savedTheme
            
            // 加载字体大小设置
            val savedFontSize = sharedPrefs.getFloat("font_size", 1.0f)
            currentFontSize = savedFontSize * 16.0
            
            // 直接从ReadiumPreferencesManager获取配置，严格按照开发实例
            val preferences = preferencesManager.getCurrentPreferences()
            
            // 更新其他本地变量
            currentFontFamily = preferences.fontFamily?.name ?: "sans-serif"
            currentLineHeight = preferences.lineHeight ?: 1.6
            currentPageMargins = preferences.pageMargins ?: 1.4
            
            Log.d(TAG, "配置加载成功: 字体=${currentFontSize}pt, 主题=$currentTheme, 字体族=$currentFontFamily")
            Log.d(TAG, "本地设置: 亮度=$savedBrightness%, 主题=$savedTheme, 字体大小=${savedFontSize}")
            Log.d(TAG, "EpubPreferences: $preferences")
            
        } catch (e: Exception) {
            Log.e(TAG, "加载配置失败", e)
            // 使用默认配置
            currentFontSize = 18.0
            currentTheme = "默认"
            currentFontFamily = "sans-serif"
            currentLineHeight = 1.6
            currentPageMargins = 1.4
        }
    }
    
    private fun loadEpub() {
        // 兼容多种传入方式
        var epubPath = intent.getStringExtra(EXTRA_EPUB_PATH)
        var epubFile = intent.getParcelableExtra<EpubFile>(EXTRA_EPUB_FILE)
        var bookPath = intent.getStringExtra(EXTRA_BOOK_PATH)
        
        // 处理从其他应用打开文件的情况
        if (epubPath.isNullOrEmpty() && epubFile == null && bookPath.isNullOrEmpty()) {
            // 检查是否是VIEW action
            if (intent.action == Intent.ACTION_VIEW) {
                val data = intent.data
                if (data != null) {
                    when (data.scheme) {
                        "file" -> {
                            val filePath = data.path
                            if (filePath != null) {
                                Log.d(TAG, "从其他应用打开文件: $filePath")
                                if (filePath.endsWith(".epub", true)) {
                                    epubPath = filePath
                                } else {
                                    bookPath = filePath
                                }
                            }
                        }
                        "content" -> {
                            // 处理content URI
                            try {
                                val inputStream = contentResolver.openInputStream(data)
                                val tempFile = File.createTempFile("temp_book", ".epub", cacheDir)
                                tempFile.outputStream().use { outputStream ->
                                    inputStream?.copyTo(outputStream)
                                }
                                epubPath = tempFile.absolutePath
                                Log.d(TAG, "从content URI创建临时文件: $epubPath")
                            } catch (e: Exception) {
                                Log.e(TAG, "处理content URI失败", e)
                            }
                        }
                    }
                }
            }
        }
        
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
                    
                    // 保存当前书籍信息
                    currentBookTitle = publication.metadata.title ?: "未知标题"
                    currentBookAuthor = publication.metadata.authors.firstOrNull()?.name ?: "未知作者"
                    
                    // 设置导航器视图
                    setupNavigatorView(navigatorFactory)
                    
                    // 隐藏加载状态
                    showLoading(false)
                    
                    // 显示成功信息
                    Log.d(TAG, "EPUB文件加载成功: $currentBookTitle - $currentBookAuthor")
                    
                    // 更新标题栏
                    supportActionBar?.title = currentBookTitle
                    supportActionBar?.subtitle = "作者：$currentBookAuthor"
                    
                    // 初始化页面信息显示
                    updatePageInfoDisplay(false)
                    
                    Log.d(TAG, "页面信息初始化完成")
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
            Log.d(TAG, "=== 开始设置Readium导航器视图 ===")
            
            // 获取保存的阅读位置
            val savedLocator = getSavedLocator()
            
            Log.d(TAG, "获取到的保存位置: $savedLocator")
            Log.d(TAG, "savedLocator类型: ${savedLocator?.javaClass?.simpleName}")
            
            // 严格按照Readium官方示例，创建FragmentFactory
            Log.d(TAG, "开始创建FragmentFactory，initialLocator: $savedLocator")
            val fragmentFactory = navigatorFactory.createFragmentFactory(
                initialLocator = savedLocator,
                                        listener = object : EpubNavigatorFragment.Listener {
                            override fun onExternalLinkActivated(url: org.readium.r2.shared.util.AbsoluteUrl) {
                                Log.d(TAG, "外部链接激活: $url")
                                try {
                                    // 调用系统浏览器打开链接
                                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url.toString()))
                                    startActivity(intent)
                                    Log.d(TAG, "已调用系统浏览器打开链接: $url")
                                } catch (e: Exception) {
                                    Log.e(TAG, "调用系统浏览器失败", e)
                                    Toast.makeText(this@ReadiumEpubReaderActivity, "无法打开链接: $url", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
            )
            Log.d(TAG, "FragmentFactory创建成功")
            
            // 设置FragmentFactory
            supportFragmentManager.fragmentFactory = fragmentFactory
            Log.d(TAG, "FragmentFactory已设置到supportFragmentManager")
            
            // 添加Fragment到容器
            supportFragmentManager.beginTransaction()
                .replace(R.id.reader_container, EpubNavigatorFragment::class.java, Bundle(), "EpubNavigatorFragment")
                .commitNow() // 使用commitNow()确保同步执行
            Log.d(TAG, "EpubNavigatorFragment已添加到容器")
            
            // 获取Fragment引用
            navigatorFragment = supportFragmentManager.findFragmentByTag("EpubNavigatorFragment") as? EpubNavigatorFragment
            Log.d(TAG, "获取到的navigatorFragment: $navigatorFragment")
            
            // 如果Fragment仍然为null，尝试延迟获取
            if (navigatorFragment == null) {
                Log.d(TAG, "Fragment为null，尝试延迟获取")
                navigatorContainer.postDelayed({
                    navigatorFragment = supportFragmentManager.findFragmentByTag("EpubNavigatorFragment") as? EpubNavigatorFragment
                    Log.d(TAG, "延迟获取的navigatorFragment: $navigatorFragment")
                }, 100)
            }
            
            // 设置错误拦截器，过滤掉XML解析错误
            setupErrorInterceptor()
            
            // 设置阅读进度监听器
            setupReadingProgressListener()
            
            // 如果有保存的位置，显示恢复提示
            if (savedLocator != null) {
                Log.d(TAG, "检测到保存的位置，显示恢复提示")
                showProgressRestoredMessage()
            } else {
                Log.d(TAG, "没有检测到保存的位置")
            }
            
            Log.d(TAG, "=== Readium导航器设置成功 ===")
        } catch (e: Exception) {
            Log.e(TAG, "设置Readium导航器失败", e)
            e.printStackTrace()
            // 如果Readium设置失败，显示错误信息而不是XML错误
            showReadiumError("阅读器初始化失败，请重试")
        }
    }
    
    /**
     * 设置阅读进度监听器
     */
    private fun setupReadingProgressListener() {
        try {
            // 监听Readium的阅读进度变化
            navigatorFragment?.let { fragment ->
                // 使用Handler定期检查阅读进度
                val progressHandler = android.os.Handler(android.os.Looper.getMainLooper())
                val progressRunnable = object : Runnable {
                    override fun run() {
                        try {
                            // 更新页面信息显示
                            if (isMenuVisible) {
                                updatePageInfoDisplay(true)
                            } else {
                                updatePageInfoDisplay(false)
                            }
                            
                            // 每500ms检查一次进度
                            progressHandler.postDelayed(this, 500)
                        } catch (e: Exception) {
                            Log.w(TAG, "更新阅读进度失败", e)
                        }
                    }
                }
                
                // 开始监听进度
                progressHandler.post(progressRunnable)
                
                // 启动自动保存进度
                startAutoSaveProgress()
                
                Log.d(TAG, "阅读进度监听器设置成功")
            }
            
            // 添加Locator变化监听器，实时更新章节和页码信息
            setupLocatorChangeListener()
            
        } catch (e: Exception) {
            Log.w(TAG, "设置阅读进度监听器失败", e)
        }
    }
    
    /**
     * 设置Locator变化监听器
     */
    private fun setupLocatorChangeListener() {
        try {
            navigatorFragment?.let { fragment ->
                // 监听currentLocator的变化
                fragment.currentLocator.value?.let { initialLocator ->
                    Log.d(TAG, "初始Locator: $initialLocator")
                    updatePageInfoDisplay(false)
                }
                
                // 使用Handler监听Locator变化
                val locatorHandler = android.os.Handler(android.os.Looper.getMainLooper())
                var lastLocatorHash = 0
                var lastPageInfo = ""
                
                val locatorRunnable = object : Runnable {
                    override fun run() {
                        try {
                            val currentLocator = navigatorFragment?.currentLocator?.value
                            if (currentLocator != null) {
                                // 计算Locator的哈希值，检测是否真的发生了变化
                                val currentHash = currentLocator.hashCode()
                                val newChapterTitle = getCurrentChapterTitle()
                                val newPage = getCurrentPage()
                                val newPageInfo = "$newChapterTitle-$newPage"
                                
                                // 只有当Locator真正变化时才更新UI
                                if (currentHash != lastLocatorHash || newPageInfo != lastPageInfo) {
                                    lastLocatorHash = currentHash
                                    lastPageInfo = newPageInfo
                                    
                                    // 更新UI显示
                                    updateChapterAndPageInfo(newChapterTitle, newPage)
                                    
                                    Log.d(TAG, "Locator真正变化: 章节=$newChapterTitle, 页码=$newPage, hash=$currentHash")
                                }
                            }
                            
                            // 每100ms检查一次Locator变化（提高频率）
                            locatorHandler.postDelayed(this, 100)
                        } catch (e: Exception) {
                            Log.w(TAG, "监听Locator变化失败", e)
                        }
                    }
                }
                
                // 开始监听
                locatorHandler.post(locatorRunnable)
                
                Log.d(TAG, "Locator变化监听器设置成功")
            }
        } catch (e: Exception) {
            Log.w(TAG, "设置Locator变化监听器失败", e)
        }
    }
    
    /**
     * 更新章节和页码信息
     */
    private fun updateChapterAndPageInfo(chapterTitle: String, currentPage: Int) {
        try {
            runOnUiThread {
                // 更新章节标题
                tvChapterTitle.text = chapterTitle
                
                // 更新页码信息
                val chapterTotalPages = getCurrentChapterTotalPages()
                if (isMenuVisible) {
                    // 菜单显示时显示详细信息
                    val remainingPages = getRemainingPages()
                    tvPageInfo.text = "本章还剩${remainingPages}页"
                } else {
                    // 菜单隐藏时显示简单信息：当前页/当前章节总页数
                    tvPageInfo.text = "$currentPage / $chapterTotalPages"
                }
                
                Log.d(TAG, "UI更新: 章节=$chapterTitle, 页码=$currentPage/$chapterTotalPages")
            }
        } catch (e: Exception) {
            Log.w(TAG, "更新章节和页码信息失败", e)
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
        // 由于移除了loadingView，只控制navigatorContainer的可见性
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
            // 更新本地主题变量
            currentTheme = theme
            
            Log.d(TAG, "开始应用主题: $theme")
            
            // 更新PreferencesManager中的主题设置
            preferencesManager.setTheme(theme)
            
            // 保存主题设置到本地
            getSharedPreferences("reader_settings", MODE_PRIVATE)
                .edit()
                .putString("theme", theme)
                .apply()
            
            // 尝试直接应用配置到当前Fragment，避免重新创建
            val currentFragment = supportFragmentManager.findFragmentByTag("EpubNavigatorFragment") as? EpubNavigatorFragment
            if (currentFragment != null && currentFragment.view != null) {
                Log.d(TAG, "直接应用主题配置，无需重新创建Fragment")
                
                // 创建主题配置
                val currentPrefs = preferencesManager.getCurrentPreferences()
                val themePreferences = when (theme) {
                    "默认" -> currentPrefs.copy(
                        theme = org.readium.r2.navigator.preferences.Theme.LIGHT
                    )
                    "护眼" -> currentPrefs.copy(
                        theme = org.readium.r2.navigator.preferences.Theme.SEPIA
                    )
                    "夜间" -> currentPrefs.copy(
                        theme = org.readium.r2.navigator.preferences.Theme.DARK
                    )
                    "复古" -> currentPrefs.copy(
                        theme = org.readium.r2.navigator.preferences.Theme.SEPIA
                    )
                    else -> currentPrefs
                }
                
                // 使用Readium原生API直接应用
                if (currentFragment is Configurable<*, EpubPreferences>) {
                    try {
                        // 应用主题配置
                        currentFragment.submitPreferences(themePreferences)
                        Log.d(TAG, "主题已直接应用到当前Fragment: $theme")
                        
                        // 添加渐显效果
                        currentFragment.view?.let { view ->
                            applyThemeTransitionEffect(view, theme)
                        }
                        
                        // 应用自定义背景色（如果需要）
                        if (theme == "复古") {
                            applyCustomBackgroundColor(currentFragment, "#F2E2C9")
                        }
                        
                        Toast.makeText(this, "主题已切换为: $theme", Toast.LENGTH_SHORT).show()
                        
                        // 保持二级菜单显示状态，不退出
                        if (isMenuPanelVisible) {
                            Log.d(TAG, "主题切换完成，保持二级菜单显示")
                        }
                        
                        return // 成功应用，直接返回
                    } catch (e: Exception) {
                        Log.w(TAG, "直接应用失败，回退到重新创建方式: ${e.message}")
                    }
                }
            }
            
            // 如果直接应用失败，回退到重新创建方式
            Log.d(TAG, "回退到重新创建Navigator以应用主题变化")
            
            // 重新创建Navigator以应用主题变化
            publication?.let { pub ->
                 Log.d(TAG, "重新创建Navigator以应用主题变化")
                 
                                 // 获取当前的阅读位置（如果有的话）
                val currentLocator = navigatorFragment?.currentLocator?.value
                Log.d(TAG, "当前阅读位置: $currentLocator")
                
                // 保存当前阅读位置到本地
                currentLocator?.let { locator ->
                    getSharedPreferences("reader_settings", MODE_PRIVATE)
                        .edit()
                        .putString("last_location", locator.toString())
                        .apply()
                    Log.d(TAG, "已保存阅读位置到本地")
                }
                
                // 创建新的NavigatorFactory，使用更新后的配置
                val newNavigatorFactory = EpubNavigatorFactory(
                    publication = pub,
                    configuration = EpubNavigatorFactory.Configuration(
                        defaults = EpubDefaults(
                            pageMargins = currentPageMargins.toDouble(),
                            fontSize = (currentFontSize / 16.0), // 转换为百分比
                            lineHeight = currentLineHeight.toDouble()
                        )
                    )
                )
                
                // 更新FragmentFactory
                supportFragmentManager.fragmentFactory = newNavigatorFactory.createFragmentFactory(
                    initialLocator = currentLocator, // 保持当前阅读位置
                    listener = object : EpubNavigatorFragment.Listener {
                        override fun onExternalLinkActivated(url: org.readium.r2.shared.util.AbsoluteUrl) {
                            Log.d(TAG, "外部链接激活: $url")
                            Toast.makeText(this@ReadiumEpubReaderActivity, "外部链接: $url", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                // 使用addToBackStack而不是replace，保持阅读位置
                supportFragmentManager.beginTransaction()
                    .addToBackStack("theme_change")
                    .replace(R.id.reader_container, EpubNavigatorFragment::class.java, Bundle(), "EpubNavigatorFragment")
                    .commit()
                
                // 更新Fragment引用
                navigatorFragment = supportFragmentManager.findFragmentByTag("EpubNavigatorFragment") as? EpubNavigatorFragment
                navigatorFactory = newNavigatorFactory
                
                // 等待Fragment创建完成后应用Readium配置
                navigatorFragment?.view?.post {
                    Log.d(TAG, "Fragment视图已创建，开始应用Readium主题配置: $theme")
                    // 使用Readium原生配置方法
                    applyReadiumThemeConfiguration(theme)
                    
                    // 添加渐显效果
                    navigatorFragment?.view?.let { view ->
                        applyThemeTransitionEffect(view, theme)
                    }
                    
                    // 应用自定义背景色（如果需要）
                    if (theme == "复古") {
                        navigatorFragment?.let { fragment ->
                            applyCustomBackgroundColor(fragment, "#F2E2C9")
                        }
                    }
                }
                
                Log.d(TAG, "主题应用成功: $theme，Navigator已重新创建")
                Toast.makeText(this, "主题已切换为: $theme", Toast.LENGTH_SHORT).show()
                
            } ?: run {
                Log.w(TAG, "Publication为空，无法应用主题")
                Toast.makeText(this, "无法应用主题，请重新加载书籍", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "应用主题失败", e)
            Toast.makeText(this, "主题切换失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 应用主题切换渐显效果
     */
    private fun applyThemeTransitionEffect(view: View, theme: String) {
        try {
            Log.d(TAG, "开始应用主题切换渐显效果: $theme")
            
            // 设置初始透明度
            view.alpha = 0.3f
            
            // 创建渐显动画
            view.animate()
                .alpha(1.0f)
                .setDuration(300) // 300毫秒渐显
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withStartAction {
                    Log.d(TAG, "主题切换渐显动画开始: $theme")
                }
                .withEndAction {
                    Log.d(TAG, "主题切换渐显动画完成: $theme")
                }
                .start()
                
        } catch (e: Exception) {
            Log.e(TAG, "应用主题切换渐显效果失败", e)
            // 如果动画失败，直接设置为完全不透明
            view.alpha = 1.0f
        }
    }
    
    /**
     * 应用自定义背景色 - 严格按照开发实例的Readium技术方案
     */
    private fun applyCustomBackgroundColor(fragment: EpubNavigatorFragment, backgroundColor: String) {
        try {
            Log.d(TAG, "开始应用自定义背景色: $backgroundColor")
            
            // 等待Fragment视图准备完成
            fragment.view?.post {
                try {
                    // 查找WebView并注入自定义CSS
                    findWebViewAndApplyCustomBackground(fragment.view!!, backgroundColor)
                } catch (e: Exception) {
                    Log.e(TAG, "应用自定义背景色失败", e)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "应用自定义背景色失败", e)
        }
    }
    
    /**
     * 查找WebView并应用自定义背景色
     */
    private fun findWebViewAndApplyCustomBackground(view: View, backgroundColor: String) {
        when (view) {
            is android.webkit.WebView -> {
                Log.d(TAG, "找到WebView，应用自定义背景色: $backgroundColor")
                injectCustomBackgroundCSS(view, backgroundColor)
            }
            is android.view.ViewGroup -> {
                for (i in 0 until view.childCount) {
                    findWebViewAndApplyCustomBackground(view.getChildAt(i), backgroundColor)
                }
            }
        }
    }
    
    /**
     * 注入自定义背景色CSS
     */
    private fun injectCustomBackgroundCSS(webView: android.webkit.WebView, backgroundColor: String) {
        try {
            val customCSS = """
                <style>
                    body { 
                        background-color: $backgroundColor !important; 
                    }
                    * { 
                        background-color: $backgroundColor !important; 
                    }
                </style>
            """.trimIndent()
            
            val jsCode = """
                (function() {
                    var style = document.createElement('style');
                    style.innerHTML = '$customCSS';
                    document.head.appendChild(style);
                    console.log('自定义背景色CSS已注入: $backgroundColor');
                })();
            """.trimIndent()
            
            webView.evaluateJavascript(jsCode) { result ->
                Log.d(TAG, "自定义背景色CSS注入结果: $result")
            }
            
            Log.d(TAG, "自定义背景色CSS已注入WebView: $backgroundColor")
            
        } catch (e: Exception) {
            Log.e(TAG, "注入自定义背景色CSS失败", e)
        }
    }
    
    /**
     * 应用字体大小切换渐显效果
     */
    private fun applyFontSizeTransitionEffect(view: View, sizeName: String) {
        try {
            Log.d(TAG, "开始应用字体大小切换渐显效果: $sizeName")
            
            // 设置初始透明度
            view.alpha = 0.3f
            
            // 创建渐显动画
            view.animate()
                .alpha(1.0f)
                .setDuration(300) // 300毫秒渐显
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withStartAction {
                    Log.d(TAG, "字体大小切换渐显动画开始: $sizeName")
                }
                .withEndAction {
                    Log.d(TAG, "字体大小切换渐显动画完成: $sizeName")
                }
                .start()
                
        } catch (e: Exception) {
            Log.e(TAG, "应用字体大小切换渐显效果失败", e)
            // 如果动画失败，直接设置为完全不透明
            view.alpha = 1.0f
        }
    }
    
    /**
     * 应用Readium主题配置 - 严格按照开发实例的方式
     */
    private fun applyReadiumThemeConfiguration(theme: String) {
        try {
            Log.d(TAG, "开始应用Readium主题配置: $theme")
            
            // 获取当前的Fragment
            val currentFragment = supportFragmentManager.findFragmentByTag("EpubNavigatorFragment") as? EpubNavigatorFragment
            if (currentFragment == null) {
                Log.w(TAG, "Fragment不存在，无法应用主题配置")
                return
            }
            
            // 创建主题配置
            val currentPrefs = preferencesManager.getCurrentPreferences()
            val themePreferences = when (theme) {
                "默认" -> currentPrefs.copy(
                    theme = org.readium.r2.navigator.preferences.Theme.LIGHT
                )
                "护眼" -> currentPrefs.copy(
                    theme = org.readium.r2.navigator.preferences.Theme.SEPIA
                )
                "夜间" -> currentPrefs.copy(
                    theme = org.readium.r2.navigator.preferences.Theme.DARK
                )
                "复古" -> currentPrefs.copy(
                    theme = org.readium.r2.navigator.preferences.Theme.SEPIA
                )
                else -> currentPrefs
            }
            
            Log.d(TAG, "主题配置详情: $themePreferences")
            
            // 使用Readium原生API应用主题
            if (currentFragment is Configurable<*, EpubPreferences>) {
                Log.d(TAG, "Fragment支持主题配置，开始应用主题: $theme")
                try {
                    currentFragment.submitPreferences(themePreferences)
                    Log.d(TAG, "主题已成功应用到Readium: $theme")
                    Toast.makeText(this, "主题已切换为: $theme", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "应用主题时发生异常", e)
                    Toast.makeText(this, "主题切换失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w(TAG, "Fragment不支持主题配置，Fragment类型: ${currentFragment.javaClass.name}")
                Log.w(TAG, "Fragment接口: ${currentFragment.javaClass.interfaces.joinToString()}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "应用Readium主题配置失败", e)
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
    
    // 应用阅读偏好设置 - 使用正确的方式
    private fun applyReadingPreferences() {
        try {
            Log.d(TAG, "开始应用阅读偏好...")
            
            // 更新ReadiumPreferencesManager
            preferencesManager.setFontSize(currentFontSize.toFloat())
            preferencesManager.setTheme(currentTheme)
            preferencesManager.setFontFamily(currentFontFamily)
            preferencesManager.setLineHeight(currentLineHeight.toFloat())
            preferencesManager.setPageMargins(currentPageMargins.toFloat())
            
            Log.d(TAG, "配置已更新到ReadiumPreferencesManager")
            
                         // 重新创建Navigator以应用所有配置变化
             publication?.let { pub ->
                 Log.d(TAG, "重新创建Navigator以应用所有配置变化")
                 
                 // 获取当前的阅读位置（如果有的话）
                 val currentLocator = navigatorFragment?.currentLocator?.value
                
                // 创建新的NavigatorFactory，使用更新后的所有配置
                val newNavigatorFactory = EpubNavigatorFactory(
                    publication = pub,
                    configuration = EpubNavigatorFactory.Configuration(
                        defaults = EpubDefaults(
                            pageMargins = currentPageMargins.toDouble(),
                            fontSize = (currentFontSize / 16.0), // 转换为百分比
                            lineHeight = currentLineHeight.toDouble(),
                            // 其他默认配置由Readium内部处理
                        )
                    )
                )
                
                // 更新FragmentFactory
                supportFragmentManager.fragmentFactory = newNavigatorFactory.createFragmentFactory(
                    initialLocator = currentLocator, // 保持当前阅读位置
                    listener = object : EpubNavigatorFragment.Listener {
                        override fun onExternalLinkActivated(url: org.readium.r2.shared.util.AbsoluteUrl) {
                            Log.d(TAG, "外部链接激活: $url")
                            Toast.makeText(this@ReadiumEpubReaderActivity, "外部链接: $url", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                // 替换Fragment
                supportFragmentManager.beginTransaction()
                    .replace(R.id.reader_container, EpubNavigatorFragment::class.java, Bundle(), "EpubNavigatorFragment")
                    .commit()
                
                // 更新Fragment引用
                navigatorFragment = supportFragmentManager.findFragmentByTag("EpubNavigatorFragment") as? EpubNavigatorFragment
                navigatorFactory = newNavigatorFactory
                
                Log.d(TAG, "所有阅读偏好应用成功，Navigator已重新创建")
                Toast.makeText(this, "设置已保存并应用", Toast.LENGTH_SHORT).show()
                
            } ?: run {
                Log.w(TAG, "Publication为空，无法应用阅读偏好")
                Toast.makeText(this, "无法应用设置，请重新加载书籍", Toast.LENGTH_SHORT).show()
            }
            
            Log.d(TAG, "应用阅读偏好完成: 字体=${currentFontSize}pt, 主题=$currentTheme, 字体族=$currentFontFamily")
            
        } catch (e: Exception) {
            Log.e(TAG, "应用阅读偏好失败", e)
            Toast.makeText(this, "保存设置失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // 书签功能
    private fun addBookmark() {
        val currentLocator = navigatorFragment?.currentLocator?.value
        if (currentLocator != null) {
            val bookmarkKey = "${currentLocator.href}_${currentLocator.locations.fragments.firstOrNull() ?: ""}"
            if (!bookmarks.contains(bookmarkKey)) {
                bookmarks.add(bookmarkKey)
                Toast.makeText(this, "书签已添加", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "添加书签: $bookmarkKey")
            } else {
                Toast.makeText(this, "书签已存在", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "无法获取当前位置", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun removeBookmark() {
        val currentLocator = navigatorFragment?.currentLocator?.value
        if (currentLocator != null) {
            val bookmarkKey = "${currentLocator.href}_${currentLocator.locations.fragments.firstOrNull() ?: ""}"
            if (bookmarks.remove(bookmarkKey)) {
                Toast.makeText(this, "移除书签: $bookmarkKey", Toast.LENGTH_SHORT).show()
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
        try {
            Log.d(TAG, "=== 开始保存阅读进度 ===")
            
            // 获取当前阅读位置
            val currentLocator = navigatorFragment?.currentLocator?.value
            val bookPath = intent.getStringExtra(EXTRA_EPUB_PATH) ?: intent.getStringExtra(EXTRA_BOOK_PATH)
            
            Log.d(TAG, "当前Locator: $currentLocator")
            Log.d(TAG, "书籍路径: $bookPath")
            Log.d(TAG, "书籍标题: $currentBookTitle")
            Log.d(TAG, "书籍作者: $currentBookAuthor")
            
            if (currentLocator != null && bookPath != null) {
                // 保存到本地SharedPreferences
                val prefs = getSharedPreferences("reading_progress", MODE_PRIVATE)
                val editor = prefs.edit()
                
                // 保存书籍基本信息
                editor.putString("${bookPath}_title", currentBookTitle)
                editor.putString("${bookPath}_author", currentBookAuthor)
                editor.putLong("${bookPath}_last_read_time", System.currentTimeMillis())
                
                // 严格按照Readium官方示例，使用toJSON()保存Locator
                val locatorJson = currentLocator.toJSON()
                Log.d(TAG, "Locator JSON: $locatorJson")
                editor.putString("${bookPath}_locator", locatorJson.toString())
                
                // 保存当前页码和总页数
                val currentPage = getCurrentPage()
                val totalPages = getTotalPages()
                editor.putInt("${bookPath}_current_page", currentPage)
                editor.putInt("${bookPath}_total_pages", totalPages)
                
                // 保存阅读进度百分比
                val progress = if (totalPages > 0) (currentPage.toFloat() / totalPages.toFloat()) else 0f
                editor.putFloat("${bookPath}_progress", progress)
                
                // 提交保存
                val success = editor.commit()
                Log.d(TAG, "SharedPreferences保存结果: $success")
                
                Log.d(TAG, "=== 阅读进度保存完成 ===")
                Log.d(TAG, "保存的进度信息:")
                Log.d(TAG, "  标题: $currentBookTitle")
                Log.d(TAG, "  作者: $currentBookAuthor")
                Log.d(TAG, "  当前页: $currentPage")
                Log.d(TAG, "  总页数: $totalPages")
                Log.d(TAG, "  进度: ${(progress * 100).toInt()}%")
                Log.d(TAG, "  Locator: $locatorJson")
            } else {
                Log.w(TAG, "无法保存进度: currentLocator=$currentLocator, bookPath=$bookPath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存阅读进度失败", e)
            e.printStackTrace()
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
        
        // 设置点击外部区域关闭
        dialog.setCanceledOnTouchOutside(true)
        
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
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "=== onPause() 开始 ===")
        // 应用进入后台时保存进度
        saveReadingProgress()
        Log.d(TAG, "应用进入后台，阅读进度已保存")
        Log.d(TAG, "=== onPause() 结束 ===")
    }
    
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "=== onStop() 开始 ===")
        // 应用停止时保存进度
        saveReadingProgress()
        Log.d(TAG, "应用停止，阅读进度已保存")
        Log.d(TAG, "=== onStop() 结束 ===")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "=== onDestroy() 开始 ===")
        
        // 保存阅读进度
        saveReadingProgress()
        
        // 停止自动保存定时器
        autoSaveTimer?.cancel()
        autoSaveTimer = null
        Log.d(TAG, "自动保存定时器已停止")
        
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
        
        Log.d(TAG, "=== onDestroy() 结束 ===")
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
    
    // iOS风格菜单系统
    private lateinit var iosOverlay: View
    private lateinit var iosMenuContainer: View
    private lateinit var iosMenuPanel: View
    private lateinit var brightnessSeekBar: SeekBar
    private lateinit var chapterContainer: LinearLayout
    
    // 页面信息显示
    private lateinit var tvChapterTitle: TextView
    private lateinit var tvPageInfo: TextView
    
    // 菜单状态
    private var isMenuVisible = false
    private var isMenuPanelVisible = false
    private var isUserInteracting = false
    private var autoHideTimer: CountDownTimer? = null

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
                    
                    // 检查触摸位置，判断是否点击在菜单区域外
                    if (isMenuVisible || isMenuPanelVisible) {
                        val touchPoint = android.graphics.PointF(event.x, event.y)
                        if (!isTouchInMenuArea(touchPoint)) {
                            // 点击在菜单区域外，关闭菜单
                            Log.d(TAG, "点击菜单区域外，关闭菜单")
                            if (isMenuPanelVisible) {
                                hideIOSMenuPanel()
                            }
                            if (isMenuVisible) {
                                hideIOSMenu()
                            }
                        }
                    } else {
                        // iOS风格菜单逻辑：单击屏幕显示菜单图标
                        if (touchDuration < touchTimeThreshold && 
                            deltaX < touchThreshold && 
                            deltaY < touchThreshold && 
                            !isTouchMoved &&
                            !isUserInteracting) {
                            
                            // 显示iOS风格菜单
                            showIOSMenu()
                        } else {
                            Log.d(TAG, "触摸事件不符合点击条件，不显示菜单")
                        }
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
    
    /**
     * 显示iOS风格菜单
     */
    private fun showIOSMenu() {
        if (!isMenuVisible) {
            isMenuVisible = true
            
            // 显示状态栏
            showStatusBar()
            
            // 显示淡蒙层
            iosOverlay.visibility = View.VISIBLE
            iosOverlay.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
            
            // 显示右下角菜单图标
            iosMenuContainer.visibility = View.VISIBLE
            iosMenuContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(android.view.animation.OvershootInterpolator())
                .start()
            
            // 更新页面信息显示
            updatePageInfoDisplay(true)
            
            // 不再自动隐藏，改为点击其他区域关闭
            Log.d(TAG, "iOS风格菜单已显示，点击其他区域可关闭")
            
            Log.d(TAG, "iOS风格菜单已显示")
        }
    }
    
    /**
     * 隐藏iOS风格菜单
     */
    private fun hideIOSMenu() {
        if (isMenuVisible) {
            isMenuVisible = false
            
            // 隐藏状态栏，恢复全屏阅读
            hideStatusBar()
            
            // 隐藏淡蒙层
            iosOverlay.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    iosOverlay.visibility = View.GONE
                }
                .start()
            
            // 隐藏右下角菜单图标
            iosMenuContainer.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(200)
                .withEndAction {
                    iosMenuContainer.visibility = View.GONE
                }
                .start()
            
            // 如果二级菜单面板是显示的，也隐藏它
            if (isMenuPanelVisible) {
                hideIOSMenuPanel()
            }
            
            // 恢复默认页面信息显示
            updatePageInfoDisplay(false)
            
            Log.d(TAG, "iOS风格菜单已隐藏")
        }
    }
    
    /**
     * 显示iOS风格二级菜单面板
     */
    private fun showIOSMenuPanel() {
        if (!isMenuPanelVisible) {
            isMenuPanelVisible = true
            
            // 显示二级菜单面板
            iosMenuPanel.visibility = View.VISIBLE
            iosMenuPanel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(android.view.animation.OvershootInterpolator())
                .start()
            
            Log.d(TAG, "iOS风格二级菜单面板已显示")
        }
    }
    
    /**
     * 隐藏iOS风格二级菜单面板
     */
    private fun hideIOSMenuPanel() {
        if (isMenuPanelVisible) {
            isMenuPanelVisible = false
            
            // 隐藏二级菜单面板
            iosMenuPanel.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(200)
                .withEndAction {
                    iosMenuPanel.visibility = View.GONE
                }
                .start()
            
            Log.d(TAG, "iOS风格二级菜单面板已隐藏")
        }
    }
    
    // CSS方法已删除，回到Readium原生配置
    
    /**
     * 递归查找WebView并应用主题
     */
    private fun findWebViewAndApplyTheme(view: View, theme: String) {
        Log.d(TAG, "🔍 检查视图类型: ${view.javaClass.simpleName}")
        
        when (view) {
            is android.webkit.WebView -> {
                Log.d(TAG, "✅ 找到WebView，开始应用主题: $theme")
                Log.d(TAG, "🌐 WebView信息: URL=${view.url}, Title=${view.title}")
                applyThemeToWebView(view, theme)
            }
            is android.view.ViewGroup -> {
                Log.d(TAG, "📦 发现ViewGroup: ${view.javaClass.simpleName}, 子视图数量: ${view.childCount}")
                for (i in 0 until view.childCount) {
                    val childView = view.getChildAt(i)
                    Log.d(TAG, "👶 检查子视图 $i: ${childView.javaClass.simpleName}")
                    findWebViewAndApplyTheme(childView, theme)
                }
            }
            else -> {
                Log.d(TAG, "ℹ️ 其他视图类型: ${view.javaClass.simpleName}")
            }
        }
    }
    
    /**
     * 向WebView注入CSS主题样式
     */
    private fun applyThemeToWebView(webView: android.webkit.WebView, theme: String) {
        try {
            Log.d(TAG, "🎨 开始向WebView注入主题CSS: $theme")
            val cssTheme = when (theme) {
                "默认" -> """
                    <style>
                        body { 
                            background-color: #FFFFFF !important; 
                            color: #000000 !important; 
                        }
                        * { 
                            background-color: #FFFFFF !important; 
                            color: #000000 !important; 
                        }
                    </style>
                """.trimIndent()
                "护眼" -> """
                    <style>
                        body { 
                            background-color: #F5F5DC !important; 
                            color: #2F2F2F !important; 
                        }
                        * { 
                            background-color: #F5F5DC !important; 
                            color: #2F2F2F !important; 
                        }
                    </style>
                """.trimIndent()
                "夜间" -> """
                    <style>
                        body { 
                            background-color: #1A1A1A !important; 
                            color: #E0E0E0 !important; 
                        }
                        * { 
                            background-color: #1A1A1A !important; 
                            color: #E0E0E0 !important; 
                        }
                    </style>
                """.trimIndent()
                "复古" -> """
                    <style>
                        body { 
                            background-color: #F4F1E8 !important; 
                            color: #2F2F2F !important; 
                        }
                        * { 
                            background-color: #F4F1E8 !important; 
                            color: #2F2F2F !important; 
                        }
                    </style>
                """.trimIndent()
                else -> ""
            }
            
            Log.d(TAG, "📝 生成的CSS主题样式:")
            Log.d(TAG, cssTheme)
            Log.d(TAG, "🎨 主题色彩配置:")
            when (theme) {
                "默认" -> Log.d(TAG, "⚪ 默认主题: 背景=#FFFFFF(白色), 文字=#000000(黑色)")
                "护眼" -> Log.d(TAG, "🟡 护眼主题: 背景=#F5F5DC(米色), 文字=#2F2F2F(深灰)")
                "夜间" -> Log.d(TAG, "⚫ 夜间主题: 背景=#1A1A1A(深黑), 文字=#E0E0E0(浅灰)")
                "复古" -> Log.d(TAG, "🟤 复古主题: 背景=#F2E2C9(复古米色), 文字=#2F2F2F(深灰)")
            }
            
            if (cssTheme.isNotEmpty()) {
                // 注入CSS样式
                val jsCode = """
                    (function() {
                        var style = document.createElement('style');
                        style.innerHTML = '$cssTheme';
                        document.head.appendChild(style);
                        
                        // 强制重新渲染
                        document.body.style.display = 'none';
                        document.body.offsetHeight;
                        document.body.style.display = '';
                    })();
                """.trimIndent()
                
                Log.d(TAG, "📜 准备执行的JavaScript代码:")
                Log.d(TAG, jsCode)
                
                webView.evaluateJavascript(jsCode) { result ->
                    Log.d(TAG, "✅ JavaScript执行结果: $result")
                }
                Log.d(TAG, "🎯 CSS主题样式已注入WebView: $theme")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "向WebView注入CSS主题样式失败", e)
        }
    }
    
    // 字体大小CSS方法已删除，回到Readium原生配置
    
    /**
     * 应用Readium字体大小配置 - 严格按照开发实例的方式
     */
    private fun applyReadiumFontSizeConfiguration(sizeName: String, fontSize: Double) {
        try {
            Log.d(TAG, "开始应用Readium字体大小配置: $sizeName (${fontSize})")
            
            // 获取当前的Fragment
            val currentFragment = supportFragmentManager.findFragmentByTag("EpubNavigatorFragment") as? EpubNavigatorFragment
            if (currentFragment == null) {
                Log.w(TAG, "Fragment不存在，无法应用字体大小配置")
                return
            }
            
            // 创建字体大小配置
            val currentPrefs = preferencesManager.getCurrentPreferences()
            val fontSizePreferences = currentPrefs.copy(
                fontSize = fontSize
            )
            
            Log.d(TAG, "字体大小配置详情: $fontSizePreferences")
            
            // 使用Readium原生API应用字体大小
            if (currentFragment is Configurable<*, EpubPreferences>) {
                Log.d(TAG, "Fragment支持字体大小配置，开始应用: $sizeName")
                try {
                    currentFragment.submitPreferences(fontSizePreferences)
                    Log.d(TAG, "字体大小已成功应用到Readium: $sizeName (${fontSize})")
                    Toast.makeText(this, "字体大小已切换为: $sizeName", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "应用字体大小时发生异常", e)
                    Toast.makeText(this, "字体大小调整失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.w(TAG, "Fragment不支持字体大小配置，Fragment类型: ${currentFragment.javaClass.name}")
                Log.w(TAG, "Fragment接口: ${currentFragment.javaClass.interfaces.joinToString()}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "应用Readium字体大小配置失败", e)
        }
    }
    
    /**
     * 检查触摸点是否在菜单区域内
     */
    private fun isTouchInMenuArea(touchPoint: android.graphics.PointF): Boolean {
        try {
            // 检查是否点击在iOS菜单容器内
            if (isMenuVisible && iosMenuContainer.visibility == View.VISIBLE) {
                val menuRect = android.graphics.Rect()
                iosMenuContainer.getGlobalVisibleRect(menuRect)
                if (menuRect.contains(touchPoint.x.toInt(), touchPoint.y.toInt())) {
                    return true
                }
            }
            
            // 检查是否点击在iOS菜单面板内
            if (isMenuPanelVisible && iosMenuPanel.visibility == View.VISIBLE) {
                val panelRect = android.graphics.Rect()
                iosMenuPanel.getGlobalVisibleRect(panelRect)
                if (panelRect.contains(touchPoint.x.toInt(), touchPoint.y.toInt())) {
                    return true
                }
            }
            
            // 检查是否点击在工具栏内
            val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
            if (toolbar.visibility == View.VISIBLE) {
                val toolbarRect = android.graphics.Rect()
                toolbar.getGlobalVisibleRect(toolbarRect)
                if (toolbarRect.contains(touchPoint.x.toInt(), touchPoint.y.toInt())) {
                    return true
                }
            }
            
            return false
        } catch (e: Exception) {
            Log.e(TAG, "检查触摸点位置失败", e)
            return false
        }
    }
    
    /**
     * 设置iOS风格菜单
     */
    private fun setupIOSMenu() {
        // 初始化视图引用
        iosOverlay = findViewById(R.id.ios_overlay)
        iosMenuContainer = findViewById(R.id.ios_menu_container)
        iosMenuPanel = findViewById(R.id.ios_menu_panel)
        brightnessSeekBar = findViewById(R.id.brightness_seekbar)
        chapterContainer = findViewById(R.id.chapter_container)
        
        // 设置初始状态
        iosOverlay.alpha = 0f
        iosMenuContainer.alpha = 0f
        iosMenuContainer.scaleX = 0.8f
        iosMenuContainer.scaleY = 0.8f
        iosMenuPanel.alpha = 0f
        iosMenuPanel.translationY = 100f
        
        // 设置右下角菜单图标点击事件
        iosMenuContainer.setOnClickListener {
            if (isMenuPanelVisible) {
                hideIOSMenuPanel()
            } else {
                showIOSMenuPanel()
            }
        }
        
        // 设置淡蒙层点击事件，点击淡蒙层关闭菜单
        iosOverlay.setOnClickListener {
            Log.d(TAG, "点击淡蒙层，关闭菜单")
            if (isMenuPanelVisible) {
                hideIOSMenuPanel()
            }
            if (isMenuVisible) {
                hideIOSMenu()
            }
        }
        
        // 设置亮度调节
        val savedBrightness = getSharedPreferences("reader_settings", MODE_PRIVATE)
            .getInt("screen_brightness", 50)
        brightnessSeekBar.progress = savedBrightness
        
        // 优化触摸热区
        brightnessSeekBar.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // 触摸开始时提供震动反馈
                    provideHapticFeedback()
                    // 阻止菜单自动隐藏
                    isUserInteracting = true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    // 触摸结束后延迟重置标志，避免菜单立即隐藏
                    brightnessSeekBar.postDelayed({
                        isUserInteracting = false
                    }, 500)
                }
            }
            false // 让SeekBar继续处理触摸事件
        }
        
        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    adjustScreenBrightness(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 开始触摸时阻止菜单自动隐藏
                isUserInteracting = true
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 触摸结束后延迟重置标志，避免菜单立即隐藏
                seekBar?.postDelayed({
                    isUserInteracting = false
                }, 500)
            }
        })
        
        // 设置功能按钮
        findViewById<ImageButton>(R.id.btn_theme).setOnClickListener {
            showThemeDialog()
        }
        
        findViewById<ImageButton>(R.id.btn_font_size).setOnClickListener {
            showFontSizeDialog()
        }
        
        // 暂时隐藏搜索和收藏功能
        findViewById<ImageButton>(R.id.btn_search).visibility = View.GONE
        findViewById<ImageButton>(R.id.btn_bookmark).visibility = View.GONE
        
        // 设置章节位置滑动器
        setupChapterPositionSlider()
        
        // 加载章节列表
        loadChapterList()
        
        Log.d(TAG, "iOS风格菜单设置完成")
    }
    
    /**
     * 隐藏状态栏，实现全屏阅读
     */
    private fun hideStatusBar() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = 
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
        Log.d(TAG, "状态栏已隐藏，进入全屏模式")
    }
    
    /**
     * 显示状态栏
     */
    private fun showStatusBar() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = 
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        Log.d(TAG, "状态栏已显示")
    }
    
    /**
     * 更新页面信息显示
     */
    private fun updatePageInfoDisplay(showDetailed: Boolean) {
        try {
            val chapterTitle = getCurrentChapterTitle()
            val currentPage = getCurrentPage()
            val totalPages = getTotalPages()
            
            // 使用新的更新方法
            updateChapterAndPageInfo(chapterTitle, currentPage)
            
            Log.d(TAG, "页面信息更新: 章节=$chapterTitle, 页码=$currentPage/$totalPages, 详细=$showDetailed")
        } catch (e: Exception) {
            Log.w(TAG, "更新页面信息显示失败", e)
        }
    }
    
    /**
     * 获取当前章节标题
     */
    private fun getCurrentChapterTitle(): String {
        return try {
            val currentLocator = navigatorFragment?.currentLocator?.value
            if (currentLocator != null) {
                // 优先使用Locator中的title
                currentLocator.title?.let { title ->
                    if (title.isNotEmpty()) return title
                }
                
                // 如果没有title，尝试从href推断章节信息
                val href = currentLocator.href.toString()
                val fileName = href.substringAfterLast("/").substringBeforeLast(".")
                if (fileName.isNotEmpty()) {
                    return fileName
                }
            }
            
            // 如果都获取不到，返回书籍标题
            publication?.metadata?.title ?: "当前章节"
        } catch (e: Exception) {
            Log.w(TAG, "获取章节标题失败", e)
            publication?.metadata?.title ?: "当前章节"
        }
    }
    
    /**
     * 获取当前页码
     */
    private fun getCurrentPage(): Int {
        return try {
            val currentLocator = navigatorFragment?.currentLocator?.value
            if (currentLocator != null) {
                Log.d(TAG, "计算页码，当前Locator: $currentLocator")
                
                // 优先使用Locator中的position
                currentLocator.locations.position?.let { position ->
                    if (position > 0) {
                        Log.d(TAG, "使用position计算页码: $position")
                        return position
                    }
                }
                
                // 如果没有position，尝试从progression计算
                currentLocator.locations.progression?.let { progression ->
                    val totalPages = getTotalPages()
                    if (totalPages > 0) {
                        val calculatedPage = (progression * totalPages).toInt() + 1
                        Log.d(TAG, "使用progression计算页码: $calculatedPage (progression=$progression, totalPages=$totalPages)")
                        return calculatedPage
                    }
                }
                
                // 尝试从totalProgression计算
                currentLocator.locations.totalProgression?.let { totalProgression ->
                    val totalPages = getTotalPages()
                    if (totalPages > 0) {
                        val calculatedPage = (totalProgression * totalPages).toInt() + 1
                        Log.d(TAG, "使用totalProgression计算页码: $calculatedPage (totalProgression=$totalProgression, totalPages=$totalPages)")
                        return calculatedPage
                    }
                }
                
                // 如果都没有，尝试从fragment解析
                currentLocator.locations.fragments.firstOrNull()?.let { fragment ->
                    fragment.toIntOrNull()?.let { page ->
                        if (page > 0) {
                            Log.d(TAG, "使用fragment计算页码: $page")
                            return page
                        }
                    }
                }
                
                // 尝试从href推断页码
                val href = currentLocator.href.toString()
                if (href.contains("page") || href.contains("p")) {
                    val pageMatch = Regex("page[_-]?(\\d+)").find(href)
                    pageMatch?.let { match ->
                        val page = match.groupValues[1].toIntOrNull()
                        if (page != null && page > 0) {
                            Log.d(TAG, "从href推断页码: $page")
                            return page
                        }
                    }
                }
                
                Log.d(TAG, "无法计算页码，使用默认值1")
            } else {
                Log.d(TAG, "currentLocator为null，无法计算页码")
            }
            
            // 默认返回1
            1
        } catch (e: Exception) {
            Log.w(TAG, "获取当前页码失败", e)
            1
        }
    }
    
    /**
     * 获取总页数
     */
    private fun getTotalPages(): Int {
        return try {
            val currentLocator = navigatorFragment?.currentLocator?.value
            if (currentLocator != null) {
                // 尝试从Locator中获取总页数信息
                currentLocator.locations.otherLocations["totalPages"]?.toString()?.toIntOrNull()?.let { total ->
                    if (total > 0) return total
                }
            }
            
            // 如果没有，使用readingOrder的大小作为备选
            publication?.readingOrder?.size?.let { size ->
                if (size > 0) return size
            }
            
            // 默认返回100
            100
        } catch (e: Exception) {
            Log.w(TAG, "获取总页数失败", e)
            100
        }
    }
    
    /**
     * 获取当前章节总页数
     */
    private fun getCurrentChapterTotalPages(): Int {
        return try {
            val currentLocator = navigatorFragment?.currentLocator?.value
            if (currentLocator != null) {
                // 尝试从Locator获取当前章节的总页数
                currentLocator.locations.otherLocations["chapterTotalPages"]?.toString()?.toIntOrNull()?.let { total ->
                    if (total > 0) return total
                }
                
                // 如果没有，尝试从publication的readingOrder获取
                publication?.readingOrder?.size?.let { size ->
                    if (size > 0) return size
                }
                
                // 默认返回100
                100
            } else {
                100
            }
        } catch (e: Exception) {
            Log.w(TAG, "获取当前章节总页数失败", e)
            100
        }
    }
    
    /**
     * 获取本章剩余页数
     */
    private fun getRemainingPages(): Int {
        val currentPage = getCurrentPage()
        val totalPages = getTotalPages()
        return if (totalPages > currentPage) totalPages - currentPage else 0
    }
    
    /**
     * 提供触觉反馈
     */
    private fun provideHapticFeedback() {
        try {
            val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val effect = android.os.VibrationEffect.createOneShot(20, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(20)
            }
            Log.d(TAG, "震动反馈已触发")
        } catch (e: Exception) {
            Log.w(TAG, "震动反馈失败", e)
        }
    }
    
    /**
     * 调整屏幕亮度
     */
    private fun adjustScreenBrightness(brightness: Int) {
        try {
            val window = window
            val layoutParams = window.attributes
            layoutParams.screenBrightness = brightness / 100f
            window.attributes = layoutParams
            
            // 保存亮度设置到SharedPreferences
            getSharedPreferences("reader_settings", MODE_PRIVATE)
                .edit()
                .putInt("screen_brightness", brightness)
                .apply()
            
            Log.d(TAG, "屏幕亮度已调整为: $brightness% 并保存到本地")
        } catch (e: Exception) {
            Log.e(TAG, "调整屏幕亮度失败", e)
        }
    }
    
    /**
     * 设置章节位置滑动器
     */
    private fun setupChapterPositionSlider() {
        // 创建一个包含位置滑动器和章节列表的垂直容器
        val chapterSectionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        
        // 章节位置标签
        val positionLabel = TextView(this).apply {
            text = "章节位置: 0%"
            textSize = 12f
            setTextColor(Color.parseColor("#666666"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(20, 10, 20, 5)
            }
        }
        
        // 章节位置滑动器
        val positionSeekBar = SeekBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(20, 0, 20, 15)
            }
            max = 100
            progress = 0
            progressTintList = ColorStateList.valueOf(Color.parseColor("#1EB4A2"))
            thumbTintList = ColorStateList.valueOf(Color.parseColor("#1EB4A2"))
        }
        
        // 优化触摸热区
        positionSeekBar.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // 触摸开始时提供震动反馈
                    provideHapticFeedback()
                    // 阻止菜单自动隐藏
                    isUserInteracting = true
                }
                android.view.MotionEvent.ACTION_UP -> {
                    // 触摸结束后延迟重置标志，避免菜单立即隐藏
                    positionSeekBar.postDelayed({
                        isUserInteracting = false
                    }, 500)
                }
            }
            false // 让SeekBar继续处理触摸事件
        }
        
        // 设置滑动器事件
        positionSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    positionLabel.text = "章节位置: ${progress}%"
                    // 这里可以添加跳转到指定位置的逻辑
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 开始触摸时阻止菜单自动隐藏
                isUserInteracting = true
            }
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 触摸结束后延迟重置标志，避免菜单立即隐藏
                seekBar?.postDelayed({
                    isUserInteracting = false
                }, 500)
            }
        })
        
        // 将位置滑动器添加到章节区域容器
        chapterSectionContainer.addView(positionLabel)
        chapterSectionContainer.addView(positionSeekBar)
        
        // 将章节容器移动到新的容器中
        val parent = chapterContainer.parent as? ViewGroup
        parent?.let { viewGroup ->
            val index = viewGroup.indexOfChild(chapterContainer)
            // 移除原来的章节容器
            viewGroup.removeView(chapterContainer)
            // 将章节容器添加到新的容器中
            chapterSectionContainer.addView(chapterContainer)
            // 将新的容器添加到原来的位置
            viewGroup.addView(chapterSectionContainer, index)
        }
        
        Log.d(TAG, "章节位置滑动器已设置")
    }
    
    /**
     * 加载章节列表
     */
    private fun loadChapterList() {
        publication?.tableOfContents?.let { toc ->
            chapterContainer.removeAllViews()
            
            toc.forEachIndexed { index, link ->
                val chapterView = TextView(this).apply {
                    text = link.title ?: "第${index + 1}章"
                    textSize = 14f
                    setPadding(16, 8, 16, 8)
                    background = android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#F0F0F0"))
                    setTextColor(android.graphics.Color.parseColor("#333333"))
                    
                    setOnClickListener {
                        // 跳转到指定章节
                        navigateToChapter(link)
                        hideIOSMenuPanel()
                    }
                }
                
                chapterContainer.addView(chapterView)
                
                // 添加分隔符
                if (index < toc.size - 1) {
                    val separator = View(this).apply {
                        layoutParams = LinearLayout.LayoutParams(8, LinearLayout.LayoutParams.MATCH_PARENT)
                        background = android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#E0E0E0"))
                    }
                    chapterContainer.addView(separator)
                }
            }
            
            Log.d(TAG, "章节列表已加载: ${toc.size} 章节")
        }
    }
    
    /**
     * 跳转到指定章节
     */
    private fun navigateToChapter(link: org.readium.r2.shared.publication.Link) {
        try {
            Log.d(TAG, "开始跳转到章节: ${link.title}")
            
            // 使用Readium的导航API跳转到指定章节
            navigatorFragment?.let { fragment ->
                // 使用Readium的导航API跳转到指定章节
                try {
                    // 使用Link对象进行跳转
                    val success = fragment.go(link, animated = true)
                    if (success) {
                        Log.d(TAG, "章节跳转成功: ${link.title}")
                        Toast.makeText(this, "已跳转到: ${link.title}", Toast.LENGTH_SHORT).show()
                        
                        // 隐藏菜单面板
                        hideIOSMenuPanel()
                    } else {
                        Log.w(TAG, "章节跳转失败: ${link.title}")
                        Toast.makeText(this, "章节跳转失败，请重试", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "章节跳转异常: ${e.message}")
                    Toast.makeText(this, "章节跳转功能开发中", Toast.LENGTH_SHORT).show()
                }
                
            } ?: run {
                Log.w(TAG, "NavigatorFragment为空，无法跳转章节")
                Toast.makeText(this, "阅读器未准备好，请稍后重试", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "跳转章节失败", e)
            Toast.makeText(this, "章节跳转失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 显示主题设置对话框
     */
    private fun showThemeDialog() {
        val themes = arrayOf("默认", "护眼", "夜间", "复古")
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("选择主题")
            .setItems(themes) { _, which ->
                applyTheme(themes[which])
            }
            .create()
        
        // 设置点击外部区域关闭
        dialog.setCanceledOnTouchOutside(true)
        
        dialog.show()
    }
    

    
    /**
     * 显示字体大小设置对话框
     */
    private fun showFontSizeDialog() {
        val sizes = arrayOf("小", "中", "大", "特大")
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("选择字体大小")
            .setItems(sizes) { _, which ->
                applyFontSize(sizes[which])
            }
            .create()
        
        // 设置点击外部区域关闭
        dialog.setCanceledOnTouchOutside(true)
        
        dialog.show()
    }
    
        /**
     * 应用字体大小到Readium - 优化版本，无闪烁
     */
    private fun applyFontSize(sizeName: String) {
        try {
            // 根据名称设置字体大小
            val fontSize = when (sizeName) {
                "小" -> 0.8
                "中" -> 1.0
                "大" -> 1.2
                "特大" -> 1.4
                else -> 1.0
            }
            
            // 更新本地字体大小变量
            currentFontSize = fontSize * 16.0
            
            Log.d(TAG, "开始应用字体大小: $sizeName (${fontSize})")
            
            // 更新PreferencesManager中的字体大小设置
            preferencesManager.setFontSize(currentFontSize.toFloat())
            
            // 保存字体大小设置到本地
            getSharedPreferences("reader_settings", MODE_PRIVATE)
                .edit()
                .putFloat("font_size", fontSize.toFloat())
                .apply()
            
            // 尝试直接应用配置到当前Fragment，避免重新创建
            val currentFragment = supportFragmentManager.findFragmentByTag("EpubNavigatorFragment") as? EpubNavigatorFragment
            if (currentFragment != null && currentFragment.view != null) {
                Log.d(TAG, "直接应用字体大小配置，无需重新创建Fragment")
                
                // 创建字体大小配置
                val currentPrefs = preferencesManager.getCurrentPreferences()
                val fontSizePreferences = currentPrefs.copy(
                    fontSize = fontSize
                )
                
                // 使用Readium原生API直接应用
                if (currentFragment is Configurable<*, EpubPreferences>) {
                    try {
                        currentFragment.submitPreferences(fontSizePreferences)
                        Log.d(TAG, "字体大小已直接应用到当前Fragment: $sizeName")
                        
                        // 添加渐显效果
                        currentFragment.view?.let { view ->
                            applyFontSizeTransitionEffect(view, sizeName)
                        }
                        
                        Toast.makeText(this, "字体大小已切换为: $sizeName", Toast.LENGTH_SHORT).show()
                        return // 成功应用，直接返回
                    } catch (e: Exception) {
                        Log.w(TAG, "直接应用失败，回退到重新创建方式: ${e.message}")
                    }
                }
            }
            
            // 如果直接应用失败，回退到重新创建方式
            Log.d(TAG, "回退到重新创建Navigator以应用字体变化")
            
            // 重新创建Navigator以应用字体变化
            publication?.let { pub ->
                // 获取当前的阅读位置（如果有的话）
                val currentLocator = navigatorFragment?.currentLocator?.value
                Log.d(TAG, "当前阅读位置: $currentLocator")
                
                // 保存当前阅读位置到本地
                currentLocator?.let { locator ->
                    getSharedPreferences("reader_settings", MODE_PRIVATE)
                        .edit()
                        .putString("last_location", locator.toString())
                        .apply()
                    Log.d(TAG, "已保存阅读位置到本地")
                }
                
                // 创建新的NavigatorFactory，使用更新后的配置
                val newNavigatorFactory = EpubNavigatorFactory(
                    publication = pub,
                    configuration = EpubNavigatorFactory.Configuration(
                        defaults = EpubDefaults(
                            pageMargins = currentPageMargins.toDouble(),
                            fontSize = fontSize, // 应用新的字体大小
                            lineHeight = currentLineHeight.toDouble()
                        )
                    )
                )
                
                // 更新FragmentFactory
                supportFragmentManager.fragmentFactory = newNavigatorFactory.createFragmentFactory(
                    initialLocator = currentLocator, // 保持当前阅读位置
                    listener = object : EpubNavigatorFragment.Listener {
                        override fun onExternalLinkActivated(url: org.readium.r2.shared.util.AbsoluteUrl) {
                            Log.d(TAG, "外部链接激活: $url")
                            Toast.makeText(this@ReadiumEpubReaderActivity, "外部链接: $url", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                
                // 使用addToBackStack而不是replace，保持阅读位置
                supportFragmentManager.beginTransaction()
                    .addToBackStack("font_size_change")
                    .replace(R.id.reader_container, EpubNavigatorFragment::class.java, Bundle(), "EpubNavigatorFragment")
                    .commit()
                
                // 更新Fragment引用
                navigatorFragment = supportFragmentManager.findFragmentByTag("EpubNavigatorFragment") as? EpubNavigatorFragment
                navigatorFactory = newNavigatorFactory
                
                // 等待Fragment创建完成后应用Readium配置
                navigatorFragment?.view?.post {
                    Log.d(TAG, "Fragment视图已创建，开始应用Readium字体大小配置: $sizeName")
                    // 使用Readium原生配置方法
                    applyReadiumFontSizeConfiguration(sizeName, fontSize)
                    
                    // 添加渐显效果
                    navigatorFragment?.view?.let { view ->
                        applyFontSizeTransitionEffect(view, sizeName)
                    }
                }
                
                Log.d(TAG, "字体大小应用成功: $sizeName (${fontSize})，Navigator已重新创建")
                Toast.makeText(this, "字体大小已切换为: $sizeName", Toast.LENGTH_SHORT).show()
                
            } ?: run {
                Log.w(TAG, "Publication为空，无法应用字体大小")
                Toast.makeText(this, "无法应用字体大小，请重新加载书籍", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "应用字体大小失败", e)
            Toast.makeText(this, "字体大小调整失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 切换书签状态
     */
    private fun toggleBookmark() {
        // 这里需要实现书签功能
        Log.d(TAG, "切换书签状态")
        Toast.makeText(this, "书签功能开发中", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 启动自动保存进度
     */
    private fun startAutoSaveProgress() {
        try {
            // 停止之前的定时器
            autoSaveTimer?.cancel()
            
            // 创建新的定时器，每30秒自动保存一次进度
            autoSaveTimer = object : CountDownTimer(autoSaveInterval, autoSaveInterval) {
                override fun onTick(millisUntilFinished: Long) {
                    // 不需要处理
                }
                
                override fun onFinish() {
                    // 自动保存进度
                    saveReadingProgress()
                    
                    // 重新启动定时器
                    start()
                }
            }.start()
            
            Log.d(TAG, "自动保存进度已启动，间隔: ${autoSaveInterval / 1000}秒")
        } catch (e: Exception) {
            Log.w(TAG, "启动自动保存进度失败", e)
        }
    }
    
    /**
     * 显示进度恢复消息
     */
    private fun showProgressRestoredMessage() {
        try {
            val bookPath = intent.getStringExtra(EXTRA_EPUB_PATH) ?: intent.getStringExtra(EXTRA_BOOK_PATH)
            if (bookPath != null) {
                val prefs = getSharedPreferences("reading_progress", MODE_PRIVATE)
                val savedPage = prefs.getInt("${bookPath}_current_page", -1)
                val savedProgress = prefs.getFloat("${bookPath}_progress", -1f)
                
                if (savedPage > 0) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        Toast.makeText(this, "已恢复到第 $savedPage 页", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "显示进度恢复消息: 第 $savedPage 页")
                    }, 1000) // 延迟1秒显示
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "显示进度恢复消息失败", e)
        }
    }
    
    /**
     * 获取保存的阅读位置
     */
    private fun getSavedLocator(): org.readium.r2.shared.publication.Locator? {
        try {
            Log.d(TAG, "=== 开始获取保存的阅读位置 ===")
            
            val bookPath = intent.getStringExtra(EXTRA_EPUB_PATH) ?: intent.getStringExtra(EXTRA_BOOK_PATH)
            Log.d(TAG, "尝试获取保存的阅读位置，书籍路径: $bookPath")
            
            if (bookPath != null) {
                val prefs = getSharedPreferences("reading_progress", MODE_PRIVATE)
                val savedLocatorString = prefs.getString("${bookPath}_locator", null)
                val savedPage = prefs.getInt("${bookPath}_current_page", -1)
                val savedProgress = prefs.getFloat("${bookPath}_progress", -1f)
                val savedTitle = prefs.getString("${bookPath}_title", null)
                val savedAuthor = prefs.getString("${bookPath}_author", null)
                
                Log.d(TAG, "从SharedPreferences读取的数据:")
                Log.d(TAG, "  locator: $savedLocatorString")
                Log.d(TAG, "  page: $savedPage")
                Log.d(TAG, "  progress: $savedProgress")
                Log.d(TAG, "  title: $savedTitle")
                Log.d(TAG, "  author: $savedAuthor")
                
                if (savedLocatorString != null && savedPage > 0) {
                    Log.d(TAG, "找到保存的阅读位置，开始解析Locator")
                    try {
                        // 严格按照Readium官方示例，使用JSONObject解析Locator
                        val jsonObject = org.json.JSONObject(savedLocatorString)
                        Log.d(TAG, "JSONObject创建成功: $jsonObject")
                        
                        val locator = org.readium.r2.shared.publication.Locator.fromJSON(jsonObject)
                        Log.d(TAG, "=== Locator解析成功 ===")
                        Log.d(TAG, "解析后的Locator: $locator")
                        Log.d(TAG, "Locator.href: ${locator?.href}")
                        Log.d(TAG, "Locator.locations: ${locator?.locations}")
                        Log.d(TAG, "Locator.text: ${locator?.text}")
                        return locator
                    } catch (e: Exception) {
                        Log.e(TAG, "解析保存的Locator失败", e)
                        e.printStackTrace()
                        return null
                    }
                } else {
                    Log.d(TAG, "没有找到有效的保存进度")
                    if (savedLocatorString == null) {
                        Log.d(TAG, "原因: locator字符串为空")
                    }
                    if (savedPage <= 0) {
                        Log.d(TAG, "原因: 页码无效 ($savedPage)")
                    }
                }
            } else {
                Log.w(TAG, "书籍路径为空，无法获取保存的进度")
            }
        } catch (e: Exception) {
            Log.w(TAG, "获取保存的阅读位置失败", e)
            e.printStackTrace()
        }
        Log.d(TAG, "=== 获取保存的阅读位置结束 ===")
        return null
    }

    // 自动保存进度
    private var autoSaveTimer: CountDownTimer? = null
    private val autoSaveInterval = 30000L // 30秒自动保存一次
}
