package com.ibylin.app.reader

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
// import org.readium.adapter.pdfium.document.PdfiumDocumentFactory // 暂时移除
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.shared.util.getOrElse
import java.io.File
import javax.inject.Inject

/**
 * 使用正确Readium API的EPUB阅读器Activity
 * 严格按照官方文档实现
 */
@AndroidEntryPoint
class ReadiumEpubReaderActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "ReadiumEpubReader"
        const val EXTRA_EPUB_PATH = "epub_path"
        const val EXTRA_EPUB_FILE = "epub_file"
    }
    
    // Readium组件
    private var publication: Publication? = null
    private var navigatorFactory: EpubNavigatorFactory? = null
    private var navigatorFragment: EpubNavigatorFragment? = null
    
    // UI组件
    private lateinit var navigatorContainer: View
    private lateinit var loadingView: View
    
    // 阅读状态
    private var currentLocation: String? = null
    private var isBookLoaded = false
    
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
        }
    }
    
    private fun loadEpub() {
        val epubPath = intent.getStringExtra(EXTRA_EPUB_PATH)
        val epubFile = intent.getParcelableExtra<EpubFile>(EXTRA_EPUB_FILE)
        
        if (epubPath.isNullOrEmpty() && epubFile == null) {
            Toast.makeText(this, "未找到EPUB文件", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        val filePath = epubPath ?: epubFile?.path
        if (filePath.isNullOrEmpty()) {
            Toast.makeText(this, "EPUB文件路径无效", Toast.LENGTH_SHORT).show()
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
                
                // 创建EPUB导航器工厂
                val navigatorFactory = EpubNavigatorFactory(
                    publication = publication,
                    configuration = EpubNavigatorFactory.Configuration(
                        defaults = EpubDefaults(
                            pageMargins = 1.4
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
                    
                    Log.d(TAG, "EPUB文件加载成功: ${publication.metadata.title}")
                    Toast.makeText(this@ReadiumEpubReaderActivity, "EPUB加载成功", Toast.LENGTH_SHORT).show()
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
    
    // 菜单相关
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
    
    private fun increaseFontSize() {
        navigatorFragment?.let { nav ->
            // 这里需要根据实际的API来调整字体大小
            Toast.makeText(this, "字体大小增加", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun decreaseFontSize() {
        navigatorFragment?.let { nav ->
            // 这里需要根据实际的API来调整字体大小
            Toast.makeText(this, "字体大小减少", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun toggleTheme() {
        navigatorFragment?.let { nav ->
            // 这里需要根据实际的API来切换主题
            Toast.makeText(this, "主题切换", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
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
