package com.ibylin.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ibylin.app.R
import com.ibylin.app.utils.EpubFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL

class CoverSelectionActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "CoverSelectionActivity"
        private const val EXTRA_BOOK = "extra_book"
        
        fun start(context: Context, book: EpubFile) {
            val intent = Intent(context, CoverSelectionActivity::class.java).apply {
                putExtra(EXTRA_BOOK, book)
            }
            context.startActivity(intent)
        }
    }
    
    private lateinit var book: EpubFile
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cover_selection)
        
        book = intent.getParcelableExtra(EXTRA_BOOK) 
            ?: throw IllegalArgumentException("Book data is required")
        
        initViews()
        setupWebView()
        loadUnsplashSearch()
    }
    
    private fun initViews() {
        webView = findViewById(R.id.web_view)
        progressBar = findViewById(R.id.progress_bar)
        tvStatus = findViewById(R.id.tv_status)
        
        title = "为《${book.metadata?.title ?: book.name}》选择封面"
    }
    
    private fun setupWebView() {
        val webSettings = webView.settings
        webSettings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
        }
        
        // 添加JavaScript接口
        webView.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun downloadImage(downloadUrl: String, originalUrl: String) {
                runOnUiThread {
                    handleImageDownload(downloadUrl, originalUrl)
                }
            }
        }, "AndroidInterface")
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                showLoading(true)
                tvStatus.text = "正在加载..."
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                showLoading(false)
                tvStatus.text = "Unsplash图片搜索"
                
                // 注入JavaScript来监听图片点击
                injectImageClickListener()
            }
            
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                showLoading(false)
                tvStatus.text = "加载失败: ${error?.description}"
                Toast.makeText(this@CoverSelectionActivity, "加载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadUnsplashSearch() {
        val searchQuery = buildSearchQuery(book.metadata?.title ?: book.name, book.metadata?.author)
        val searchUrl = "https://unsplash.com/s/photos/$searchQuery"
        webView.loadUrl(searchUrl)
    }
    
    private fun buildSearchQuery(bookTitle: String, author: String?): String {
        val cleanTitle = bookTitle.replace(Regex("[《》【】()（）]"), "").trim()
        val query = if (author != null && author.isNotBlank()) {
            "$cleanTitle $author book cover"
        } else {
            "$cleanTitle book cover"
        }
        return query.replace(" ", "-")
    }
    
    private fun injectImageClickListener() {
        val js = """
            (function() {
                var images = document.querySelectorAll('img[src*="images.unsplash.com"]');
                for (var i = 0; i < images.length; i++) {
                    images[i].style.cursor = 'pointer';
                    images[i].addEventListener('click', function() {
                        var imgSrc = this.src;
                        var originalSrc = imgSrc.split('?')[0];
                        var downloadUrl = originalSrc + '?w=800&h=1200&fit=crop';
                        
                        // 发送消息到Android
                        window.AndroidInterface.downloadImage(downloadUrl, originalSrc);
                    });
                }
                
                // 添加下载提示
                var style = document.createElement('style');
                style.textContent = '
                    img[src*="images.unsplash.com"]:hover {
                        opacity: 0.8;
                        transform: scale(1.05);
                        transition: all 0.2s ease;
                    }
                    .download-hint {
                        position: fixed;
                        top: 20px;
                        right: 20px;
                        background: rgba(0,0,0,0.8);
                        color: white;
                        padding: 10px;
                        border-radius: 5px;
                        z-index: 10000;
                        font-size: 14px;
                    }
                ';
                document.head.appendChild(style);
                
                var hint = document.createElement('div');
                hint.className = 'download-hint';
                hint.textContent = '点击图片下载封面';
                document.body.appendChild(hint);
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(js, null)
    }
    
    private fun handleImageDownload(imageUrl: String, originalUrl: String) {
        showLoading(true)
        tvStatus.text = "正在下载封面图片..."
        
        coroutineScope.launch {
            try {
                val localPath = downloadImageToLocal(imageUrl, book.name)
                
                if (localPath != null) {
                    // 更新书籍封面
                    updateBookCover(localPath)
                    Toast.makeText(this@CoverSelectionActivity, "封面更新成功", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    showLoading(false)
                    tvStatus.text = "下载失败"
                    Toast.makeText(this@CoverSelectionActivity, "下载失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "下载封面图片失败", e)
                showLoading(false)
                tvStatus.text = "下载失败: ${e.message}"
                Toast.makeText(this@CoverSelectionActivity, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun downloadImageToLocal(imageUrl: String, bookName: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始下载图片: $imageUrl")
            
            val url = URL(imageUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val inputStream = connection.getInputStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            if (bitmap != null) {
                val filePath = saveImageToLocal(bitmap, bookName)
                Log.d(TAG, "图片下载成功: $filePath")
                filePath
            } else {
                Log.e(TAG, "图片解码失败")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "下载图片异常", e)
            null
        }
    }
    
    private fun saveImageToLocal(bitmap: Bitmap, bookName: String): String {
        val coverDir = File(filesDir, "book_covers")
        if (!coverDir.exists()) {
            coverDir.mkdirs()
        }
        
        val fileName = "${bookName.replace(Regex("[^a-zA-Z0-9\u4e00-\u9fa5]"), "_")}_cover.jpg"
        val file = File(coverDir, fileName)
        
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Log.d(TAG, "图片保存成功: ${file.absolutePath}")
            return file.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "保存图片失败", e)
            throw e
        }
    }
    
    private fun updateBookCover(localPath: String) {
        // TODO: 这里需要实现更新书籍封面的逻辑
        // 可以通过SharedPreferences保存封面路径，或者更新数据库
        Log.d(TAG, "封面已更新: $localPath")
    }
    
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        webView.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
