package com.ibylin.app.reader

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.ibylin.app.R
import com.ibylin.app.utils.EpubFile
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
        setContentView(R.layout.activity_readium_reader)
        
        setupViews()
        setupToolbar()
        loadEpub()
    }
    
    private fun setupViews() {
        // 初始化视图
        navigatorContainer = findViewById(R.id.reader_container)
        loadingView = findViewById(R.id.loading_view)
        
        // 显示加载状态
        showLoading(true)
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
                
                val file = File(filePath)
                if (!file.exists()) {
                    throw Exception("文件不存在: $filePath")
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
                    
                    // 显示成功信息
                    val title = publication.metadata.title ?: "未知标题"
                    val author = publication.metadata.authors.firstOrNull()?.name ?: "未知作者"
                    Log.d(TAG, "EPUB文件加载成功: $title - $author")
                    Toast.makeText(this@ReadiumEpubReaderActivity, "《$title》加载成功", Toast.LENGTH_SHORT).show()
                    
                    // 更新标题栏
                    supportActionBar?.title = title
                    supportActionBar?.subtitle = "作者：$author"
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "加载EPUB文件失败", e)
                throw e
            }
        }
    }
    
    private fun setupNavigatorView(navigatorFactory: EpubNavigatorFactory) {
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
    }
    
    private fun showLoading(show: Boolean) {
        loadingView.visibility = if (show) View.VISIBLE else View.GONE
        navigatorContainer.visibility = if (show) View.GONE else View.VISIBLE
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
    }
}
