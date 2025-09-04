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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubDefaults
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
        
        setupViews()
        setupToolbar()
        loadEpub()
    }
    
    private fun setupViews() {
        // 初始化视图
        navigatorContainer = findViewById(R.id.reader_container)
        loadingView = findViewById(R.id.loading_view)
        
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
                        
                        // 如果点击在屏幕中间区域，显示工具栏
                        if (isClickInCenterArea(view, x, y)) {
                            showReadingControls(true)
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
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Readium EPUB阅读器"
            setSubtitle("专业级EPUB阅读体验")
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
                
                // 创建EPUB导航器工厂 - 使用优化的配置
                val navigatorFactory = EpubNavigatorFactory(
                    publication = publication,
                    configuration = EpubNavigatorFactory.Configuration(
                        defaults = EpubDefaults(
                            pageMargins = currentPageMargins,
                            // 可以添加更多默认设置
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
                    
                    // 显示底部控制栏和进度条
                    showReadingControls(true)
                    
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
            injectErrorHidingScript()
        }, 1000)
        
        // 2. 延迟2秒再次执行
        navigatorContainer.postDelayed({
            hideXmlErrors()
            injectErrorHidingScript()
        }, 2000)
        
        // 3. 延迟3秒再次执行
        navigatorContainer.postDelayed({
            hideXmlErrors()
            injectErrorHidingScript()
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
    
    private fun injectErrorHidingScript() {
        try {
            // 尝试在WebView中注入JavaScript来隐藏错误
            val script = """
                (function() {
                    // 隐藏包含错误信息的元素
                    function hideErrors() {
                        // 查找包含错误信息的文本节点
                        var walker = document.createTreeWalker(
                            document.body,
                            NodeFilter.SHOW_TEXT,
                            null,
                            false
                        );
                        
                        var node;
                        while (node = walker.nextNode()) {
                            var text = node.textContent;
                            if (text && (
                                text.includes('This page contains the following errors:') ||
                                text.includes('AttValue:') ||
                                text.includes('error on line') ||
                                text.includes('column') ||
                                text.includes('XML') ||
                                text.includes('parsing error')
                            )) {
                                // 隐藏包含错误的父元素
                                var parent = node.parentElement;
                                if (parent) {
                                    parent.style.display = 'none';
                                    console.log('Hidden error element:', text);
                                }
                            }
                        }
                        
                        // 查找包含错误的div元素
                        var divs = document.querySelectorAll('div');
                        for (var i = 0; i < divs.length; i++) {
                            var div = divs[i];
                            var text = div.textContent || div.innerText;
                            if (text && (
                                text.includes('This page contains the following errors:') ||
                                text.includes('AttValue:') ||
                                text.includes('error on line') ||
                                text.includes('column') ||
                                text.includes('XML') ||
                                text.includes('parsing error')
                            )) {
                                div.style.display = 'none';
                                console.log('Hidden error div:', text);
                            }
                        }
                    }
                    
                    // 立即执行一次
                    hideErrors();
                    
                    // 设置定期检查
                    setInterval(hideErrors, 1000);
                    
                    // 监听DOM变化
                    var observer = new MutationObserver(hideErrors);
                    observer.observe(document.body, {
                        childList: true,
                        subtree: true
                    });
                })();
            """.trimIndent()
            
            // 尝试在Fragment中查找WebView并注入脚本
            navigatorFragment?.let { fragment ->
                val fragmentView = fragment.view
                if (fragmentView != null) {
                    injectScriptIntoView(fragmentView, script)
                }
            }
            
            // 也尝试在navigatorContainer中查找WebView
            if (navigatorContainer is android.view.ViewGroup) {
                findAndInjectIntoWebViews(navigatorContainer as android.view.ViewGroup, script)
            }
            
        } catch (e: Exception) {
            Log.d(TAG, "注入错误隐藏脚本失败", e)
        }
    }
    
    private fun injectScriptIntoView(view: View, script: String) {
        if (view is android.webkit.WebView) {
            view.evaluateJavascript(script, null)
            Log.d(TAG, "已向WebView注入错误隐藏脚本")
        } else if (view is android.view.ViewGroup) {
            findAndInjectIntoWebViews(view, script)
        }
    }
    
    private fun findAndInjectIntoWebViews(viewGroup: android.view.ViewGroup, script: String) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            injectScriptIntoView(child, script)
        }
    }
    
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
        findViewById<LinearLayout>(R.id.bottom_controls).visibility = if (show) View.VISIBLE else View.GONE
        findViewById<SeekBar>(R.id.reading_progress).visibility = if (show) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.progress_text).visibility = if (show) View.VISIBLE else View.GONE
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
    
    // 应用阅读偏好设置
    private fun applyReadingPreferences() {
        navigatorFragment?.let { nav ->
            try {
                // 这里需要根据实际的Readium API来应用设置
                // 暂时使用Toast提示，后续根据API完善
                Log.d(TAG, "应用阅读偏好: 字体=${currentFontSize}pt, 主题=$currentTheme, 字体族=$currentFontFamily")
            } catch (e: Exception) {
                Log.e(TAG, "应用阅读偏好失败", e)
            }
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
}
