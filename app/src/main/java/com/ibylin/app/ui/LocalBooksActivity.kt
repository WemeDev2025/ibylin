package com.ibylin.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ibylin.app.R
import com.ibylin.app.adapter.EpubFileAdapter
import com.ibylin.app.reader.ReadiumReaderActivity
import com.ibylin.app.utils.EpubFile
import com.ibylin.app.utils.EpubScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LocalBooksActivity : AppCompatActivity() {
    
    private lateinit var rvEpubFiles: RecyclerView
    private lateinit var llScanning: android.widget.LinearLayout
    private lateinit var llNoFiles: android.widget.LinearLayout
    private lateinit var epubFileAdapter: EpubFileAdapter
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView(R.layout.activity_local_books) // 暂时注释掉，布局文件缺失
        
        // 暂时显示一个简单的提示
        Toast.makeText(this, "LocalBooks功能暂未实现", Toast.LENGTH_LONG).show()
        finish()
        
        // initViews()
        // setupRecyclerView()
        // startBookScan()
    }
    
    // private fun initViews() {
    //     rvEpubFiles = findViewById(R.id.rv_epub_files)
    //     llScanning = findViewById(R.id.ll_scanning)
    //     llNoFiles = findViewById(R.id.ll_no_files)
    //     
    //     // 设置扫描按钮点击事件
    //     findViewById<android.widget.ImageButton>(R.id.btn_scan).setOnClickListener {
    //         startBookScan()
    //     }
    // }
    
    private fun setupRecyclerView() {
        // 设置线性布局
        val linearLayoutManager = LinearLayoutManager(this)
        rvEpubFiles.layoutManager = linearLayoutManager
        
        // 初始化适配器
        epubFileAdapter = EpubFileAdapter { epubFile ->
            // 点击EPUB文件时跳转到阅读器
            openReadiumReader(epubFile.path)
        }
        rvEpubFiles.adapter = epubFileAdapter
    }
    
    /**
     * 开始扫描书籍
     */
    private fun startBookScan() {
        showScanningProgress()
        
        coroutineScope.launch {
            try {
                val epubFiles = EpubScanner().scanEpubFiles(this@LocalBooksActivity)
                
                withContext(Dispatchers.Main) {
                    hideScanningProgress()
                    showEpubFiles(epubFiles)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideScanningProgress()
                    showNoFiles()
                }
            }
        }
    }
    
    /**
     * 显示扫描进度
     */
    private fun showScanningProgress() {
        llScanning.visibility = View.VISIBLE
        rvEpubFiles.visibility = View.GONE
        llNoFiles.visibility = View.GONE
    }
    
    /**
     * 隐藏扫描进度
     */
    private fun hideScanningProgress() {
        llScanning.visibility = View.GONE
    }
    
    /**
     * 显示EPUB文件列表
     */
    private fun showEpubFiles(epubFiles: List<EpubFile>) {
        if (epubFiles.isEmpty()) {
            showNoFiles()
        } else {
            llNoFiles.visibility = View.GONE
            rvEpubFiles.visibility = View.VISIBLE
            epubFileAdapter.updateEpubFiles(epubFiles)
        }
    }
    
    /**
     * 显示无文件提示
     */
    private fun showNoFiles() {
        llNoFiles.visibility = View.VISIBLE
        rvEpubFiles.visibility = View.GONE
    }
    
    /**
     * 打开Readium阅读器
     */
    private fun openReadiumReader(bookPath: String) {
        try {
            val intent = Intent(this, ReadiumReaderActivity::class.java).apply {
                putExtra("book_path", bookPath)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // 处理错误
        }
    }
}
