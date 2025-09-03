package com.ibylin.app.reader

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ibylin.app.R
import com.ibylin.app.utils.EpubFile
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * 基础的WebView EPUB阅读器Activity
 * 先实现基本功能，后续逐步集成Readium
 */
@AndroidEntryPoint
class SimpleEpubReaderActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "SimpleEpubReader"
        const val EXTRA_EPUB_PATH = "epub_path"
        const val EXTRA_EPUB_FILE = "epub_file"
    }
    
    // UI组件
    private lateinit var webView: WebView
    private lateinit var loadingView: View
    
    // EPUB内容
    private var epubContent: String = ""
    private var currentChapter: Int = 0
    private var totalChapters: Int = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_readium_reader)
        
        setupViews()
        setupToolbar()
        setupWebView()
        loadEpub()
    }
    
    private fun setupViews() {
        // 初始化视图
        webView = findViewById(R.id.reader_container)
        loadingView = findViewById(R.id.loading_view)
        
        // 显示加载状态
        showLoading(true)
    }
    
    private fun setupToolbar() {
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "EPUB阅读器"
        }
    }
    
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
        }
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "WebView页面加载完成")
            }
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
                    Toast.makeText(this@SimpleEpubReaderActivity, "加载EPUB失败: ${e.message}", Toast.LENGTH_SHORT).show()
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
                
                // 解析EPUB文件
                val epubData = parseEpubFile(file)
                
                withContext(Dispatchers.Main) {
                    // 显示EPUB内容
                    displayEpubContent(epubData)
                    
                    // 隐藏加载状态
                    showLoading(false)
                    
                    Log.d(TAG, "EPUB文件加载成功")
                    Toast.makeText(this@SimpleEpubReaderActivity, "EPUB加载成功", Toast.LENGTH_SHORT).show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "加载EPUB文件失败", e)
                throw e
            }
        }
    }
    
    private fun parseEpubFile(file: File): EpubData {
        val zipFile = ZipFile(file)
        var containerXml = ""
        var opfContent = ""
        var firstChapterContent = ""
        
        // 读取container.xml
        zipFile.getEntry("META-INF/container.xml")?.let { entry ->
            zipFile.getInputStream(entry).use { input ->
                containerXml = input.bufferedReader().readText()
            }
        }
        
        // 读取OPF文件
        val opfPath = extractOpfPath(containerXml)
        if (opfPath.isNotEmpty()) {
            zipFile.getEntry(opfPath)?.let { entry ->
                zipFile.getInputStream(entry).use { input ->
                    opfContent = input.bufferedReader().readText()
                }
            }
        }
        
        // 读取第一个章节
        val firstChapterPath = extractFirstChapterPath(opfContent)
        if (firstChapterPath.isNotEmpty()) {
            zipFile.getEntry(firstChapterPath)?.let { entry ->
                zipFile.getInputStream(entry).use { input ->
                    firstChapterContent = input.bufferedReader().readText()
                }
            }
        }
        
        zipFile.close()
        
        return EpubData(
            title = extractTitle(opfContent),
            author = extractAuthor(opfContent),
            content = firstChapterContent
        )
    }
    
    private fun extractOpfPath(containerXml: String): String {
        // 简单的正则表达式提取OPF路径
        val regex = "full-path=\"([^\"]+)\""
        val match = regex.toRegex().find(containerXml)
        return match?.groupValues?.get(1) ?: ""
    }
    
    private fun extractFirstChapterPath(opfContent: String): String {
        // 简单的正则表达式提取第一个章节路径
        val regex = "<item[^>]*href=\"([^\"]+\\.html?)\"[^>]*>"
        val match = regex.toRegex().find(opfContent)
        return match?.groupValues?.get(1) ?: ""
    }
    
    private fun extractTitle(opfContent: String): String {
        val regex = "<dc:title[^>]*>([^<]+)</dc:title>"
        val match = regex.toRegex().find(opfContent)
        return match?.groupValues?.get(1) ?: "未知标题"
    }
    
    private fun extractAuthor(opfContent: String): String {
        val regex = "<dc:creator[^>]*>([^<]+)</dc:creator>"
        val match = regex.toRegex().find(opfContent)
        return match?.groupValues?.get(1) ?: "未知作者"
    }
    
    private fun displayEpubContent(epubData: EpubData) {
        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: 'Noto Sans CJK SC', 'Microsoft YaHei', sans-serif;
                        font-size: 18px;
                        line-height: 1.6;
                        margin: 20px;
                        color: #333;
                        background-color: #fefefe;
                    }
                    h1 {
                        color: #2c3e50;
                        border-bottom: 2px solid #3498db;
                        padding-bottom: 10px;
                    }
                    h2 {
                        color: #34495e;
                        margin-top: 30px;
                    }
                    p {
                        text-indent: 2em;
                        margin: 1em 0;
                    }
                </style>
            </head>
            <body>
                <h1>${epubData.title}</h1>
                <h2>作者：${epubData.author}</h2>
                <hr>
                <div id="content">
                    ${epubData.content}
                </div>
            </body>
            </html>
        """.trimIndent()
        
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
    }
    
    private fun showLoading(show: Boolean) {
        loadingView.visibility = if (show) View.VISIBLE else View.GONE
        webView.visibility = if (show) View.GONE else View.VISIBLE
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
        webView.settings.textZoom = (webView.settings.textZoom * 1.2).toInt()
        Toast.makeText(this, "字体大小增加", Toast.LENGTH_SHORT).show()
    }
    
    private fun decreaseFontSize() {
        webView.settings.textZoom = (webView.settings.textZoom * 0.8).toInt()
        Toast.makeText(this, "字体大小减少", Toast.LENGTH_SHORT).show()
    }
    
    private fun toggleTheme() {
        // 简单的主题切换
        val isDark = webView.settings.textZoom < 100
        if (isDark) {
            webView.settings.textZoom = 100
            Toast.makeText(this, "切换到亮色主题", Toast.LENGTH_SHORT).show()
        } else {
            webView.settings.textZoom = 80
            Toast.makeText(this, "切换到暗色主题", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
    
    data class EpubData(
        val title: String,
        val author: String,
        val content: String
    )
}
