package com.ibylin.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    
    // 权限相关
    private var hasScanned = false
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // 权限已授予，开始扫描
            startBookScan()
        } else {
            // 权限被拒绝，显示设置引导
            showPermissionDeniedDialog()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_library)
        
        initViews()
        setupRecyclerView()
        // 不在这里自动扫描，等权限确认后再扫描
    }
    
    override fun onResume() {
        super.onResume()
        // 如果还没有扫描过，且权限已授予，则开始扫描
        if (!hasScanned && checkPermissions()) {
            startBookScan()
        }
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
        // 设置网格布局，一行2个
        val gridLayoutManager = GridLayoutManager(this, 2)
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
        // 检查权限
        if (!checkPermissions()) {
            requestPermissions()
            return
        }
        
        android.util.Log.d("BookLibraryActivity", "开始扫描书籍")
        showScanningProgress()
        hasScanned = true
        
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
    
    /**
     * 检查权限
     */
    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 检查MANAGE_EXTERNAL_STORAGE权限
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                // 如果能找到这个Activity，说明权限可能已授予
                packageManager.resolveActivity(intent, 0) != null
            } catch (e: Exception) {
                false
            }
        } else {
            // Android 10及以下使用传统权限
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * 请求权限
     */
    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 显示Apple风格的权限说明弹窗
            showAppleStylePermissionDialog()
        } else {
            // Android 10及以下请求传统权限
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }
    
    /**
     * 显示Apple风格的权限说明弹窗
     */
    private fun showAppleStylePermissionDialog() {
        AlertDialog.Builder(this, R.style.AppleStyleDialog)
            .setTitle("需要文件访问权限")
            .setMessage("为了扫描和显示您的EPUB图书，需要访问设备上的文件。请在接下来的设置页面中授予\"所有文件访问权限\"。")
            .setPositiveButton("去设置") { _, _ ->
                // 跳转到设置页面
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 显示权限被拒绝的弹窗
     */
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this, R.style.AppleStyleDialog)
            .setTitle("权限被拒绝")
            .setMessage("没有文件访问权限，无法扫描图书。请在设置中手动授予权限。")
            .setPositiveButton("去设置") { _, _ ->
                // 跳转到应用设置页面
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
}
