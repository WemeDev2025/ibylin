package com.ibylin.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ibylin.app.R
import com.ibylin.app.adapter.BookGridAdapter
import com.ibylin.app.reader.ReadiumReaderActivity
import com.ibylin.app.utils.EpubFile
import com.ibylin.app.utils.EpubScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookLibraryActivity : AppCompatActivity() {
    
    private lateinit var rvBooks: RecyclerView
    private lateinit var llScanning: android.widget.LinearLayout
    private lateinit var llNoBooks: android.widget.LinearLayout
    private lateinit var bookGridAdapter: BookGridAdapter
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_library)
        
        initViews()
        setupRecyclerView()
        startBookScan()
    }
    
    private fun initViews() {
        rvBooks = findViewById(R.id.rv_books)
        llScanning = findViewById(R.id.tv_scanning)
        llNoBooks = findViewById(R.id.tv_no_books)
        
        // 设置按钮点击事件
        findViewById<android.widget.ImageButton>(R.id.btn_settings).setOnClickListener {
            openReaderSettings()
        }
    }
    
    private fun setupRecyclerView() {
        // 设置网格布局，一行3个
        val gridLayoutManager = GridLayoutManager(this, 3)
        rvBooks.layoutManager = gridLayoutManager
        
        // 初始化适配器
        bookGridAdapter = BookGridAdapter { epubFile ->
            // 点击书籍时跳转到阅读器
            openReadiumReader(epubFile.path)
        }
        rvBooks.adapter = bookGridAdapter
    }
    
    /**
     * 开始扫描书籍
     */
    private fun startBookScan() {
        android.util.Log.d("BookLibraryActivity", "开始扫描书籍")
        showScanningProgress()
        
        coroutineScope.launch {
            try {
                android.util.Log.d("BookLibraryActivity", "开始调用EpubScanner.scanEpubFiles")
                val epubFiles = EpubScanner().scanEpubFiles(this@BookLibraryActivity)
                android.util.Log.d("BookLibraryActivity", "EpubScanner返回结果: 文件数量=${epubFiles.size}")
                
                // 打印每个文件的详细信息
                epubFiles.forEachIndexed { index, epubFile ->
                    android.util.Log.d("BookLibraryActivity", "文件[$index]: 名称=${epubFile.name}, 路径=${epubFile.path}, 大小=${epubFile.size}")
                }
                
                withContext(Dispatchers.Main) {
                    android.util.Log.d("BookLibraryActivity", "切换到主线程，开始显示书籍")
                    hideScanningProgress()
                    showBooks(epubFiles)
                }
            } catch (e: Exception) {
                android.util.Log.e("BookLibraryActivity", "扫描书籍时发生异常", e)
                withContext(Dispatchers.Main) {
                    android.util.Log.d("BookLibraryActivity", "异常处理：隐藏扫描进度，显示无书籍")
                    hideScanningProgress()
                    showNoBooks()
                }
            }
        }
    }
    
    /**
     * 显示扫描进度
     */
    private fun showScanningProgress() {
        llScanning.visibility = View.VISIBLE
        rvBooks.visibility = View.GONE
        llNoBooks.visibility = View.GONE
    }
    
    /**
     * 隐藏扫描进度
     */
    private fun hideScanningProgress() {
        llScanning.visibility = View.GONE
    }
    
    /**
     * 显示书籍列表
     */
    private fun showBooks(epubFiles: List<EpubFile>) {
        android.util.Log.d("BookLibraryActivity", "showBooks被调用: 文件数量=${epubFiles.size}")
        
        if (epubFiles.isEmpty()) {
            android.util.Log.d("BookLibraryActivity", "文件列表为空，显示无书籍提示")
            showNoBooks()
        } else {
            android.util.Log.d("BookLibraryActivity", "文件列表不为空，显示书籍列表")
            android.util.Log.d("BookLibraryActivity", "设置llNoBooks为GONE, rvBooks为VISIBLE")
            llNoBooks.visibility = View.GONE
            rvBooks.visibility = View.VISIBLE
            
            android.util.Log.d("BookLibraryActivity", "调用bookGridAdapter.updateEpubFiles")
            bookGridAdapter.updateEpubFiles(epubFiles)
            android.util.Log.d("BookLibraryActivity", "bookGridAdapter.updateEpubFiles调用完成")
        }
    }
    
    /**
     * 显示无书籍提示
     */
    private fun showNoBooks() {
        llNoBooks.visibility = View.VISIBLE
        rvBooks.visibility = View.GONE
    }
    
    /**
     * 打开Readium阅读器
     */
    private fun openReadiumReader(bookPath: String) {
        val intent = Intent(this, ReadiumReaderActivity::class.java).apply {
            putExtra("book_path", bookPath)
        }
        startActivity(intent)
    }
    
    /**
     * 打开阅读器设置页面
     */
    private fun openReaderSettings() {
        val intent = Intent(this, com.ibylin.app.ui.LibreraSettingsActivity::class.java)
        startActivity(intent)
    }
}
