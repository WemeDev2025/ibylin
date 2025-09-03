package com.ibylin.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
            // 权限已授予，检查是否需要扫描
            if (!isDataCached) {
                android.util.Log.d("BookLibraryActivity", "权限授予，开始扫描")
                startBookScan()
            } else {
                android.util.Log.d("BookLibraryActivity", "权限授予，但已有缓存，无需扫描")
            }
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
        
        // 尝试恢复缓存数据
        restoreCacheData()
        
        // 不在这里自动扫描，等权限确认后再扫描
    }
    
    override fun onResume() {
        super.onResume()
        
        android.util.Log.d("BookLibraryActivity", "onResume: isDataCached=$isDataCached, cachedEpubFiles.size=${cachedEpubFiles.size}")
        
        // 如果有完整的缓存数据，直接显示
        if (isDataCached && cachedEpubFiles.isNotEmpty()) {
            android.util.Log.d("BookLibraryActivity", "使用完整缓存数据，文件数量=${cachedEpubFiles.size}")
            showBooks(cachedEpubFiles)
            return
        }
        
        // 如果没有缓存数据，且权限已授予，则开始扫描
        if (checkPermissions()) {
            android.util.Log.d("BookLibraryActivity", "权限已授予，开始扫描")
            startBookScan()
        } else {
            android.util.Log.d("BookLibraryActivity", "权限未授予")
        }
    }
    
    // 缓存相关
    private var cachedEpubFiles: List<EpubFile> = emptyList()
    private var isDataCached = false
    
    // 持久化缓存
    private val sharedPreferences by lazy { getSharedPreferences("book_cache", MODE_PRIVATE) }
    
    private fun initViews() {
        rvBooks = findViewById(R.id.rv_books)
        llScanning = findViewById(R.id.tv_scanning)
        llNoBooks = findViewById(R.id.tv_no_books)
        
        // 设置更新按钮点击事件
        findViewById<android.widget.ImageButton>(R.id.btn_settings).setOnClickListener {
            manualUpdate()
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
        android.util.Log.d("BookLibraryActivity", "startBookScan: 开始检查权限")
        
        // 检查权限
        if (!checkPermissions()) {
            android.util.Log.d("BookLibraryActivity", "权限未授予，请求权限")
            requestPermissions()
            return
        }
        
        android.util.Log.d("BookLibraryActivity", "权限已授予，检查缓存状态: isDataCached=$isDataCached, cachedEpubFiles.size=${cachedEpubFiles.size}")
        
        // 如果已经有完整的缓存数据，避免重复扫描
        if (isDataCached && cachedEpubFiles.isNotEmpty()) {
            android.util.Log.d("BookLibraryActivity", "已有完整缓存数据，避免重复扫描")
            return
        }
        
        android.util.Log.d("BookLibraryActivity", "需要扫描书籍（无缓存或缓存数据为空）")
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
                    
                    // 缓存数据
                    cachedEpubFiles = epubFiles
                    isDataCached = true
                    
                    // 保存缓存到SharedPreferences
                    saveCacheData(epubFiles)
                    
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
     * 手动更新书籍列表
     */
    private fun manualUpdate() {
        android.util.Log.d("BookLibraryActivity", "手动更新书籍列表")
        
        // 清除缓存，强制重新扫描
        clearCacheData()
        startBookScan()
    }
    
    /**
     * 保存缓存数据到SharedPreferences
     */
    private fun saveCacheData(epubFiles: List<EpubFile>) {
        try {
            val editor = sharedPreferences.edit()
            editor.putBoolean("is_data_cached", true)
            editor.putInt("cached_count", epubFiles.size)
            editor.putLong("cache_timestamp", System.currentTimeMillis())
            
            // 保存书籍数据（简化版本，只保存关键信息）
            epubFiles.forEachIndexed { index, epubFile ->
                editor.putString("book_${index}_name", epubFile.name)
                editor.putString("book_${index}_path", epubFile.path)
                editor.putLong("book_${index}_size", epubFile.size)
                editor.putLong("book_${index}_lastModified", epubFile.lastModified)
            }
            
            editor.apply()
            
            android.util.Log.d("BookLibraryActivity", "完整缓存数据已保存: 文件数量=${epubFiles.size}")
        } catch (e: Exception) {
            android.util.Log.e("BookLibraryActivity", "保存缓存数据失败", e)
        }
    }
    
    /**
     * 从SharedPreferences恢复缓存数据
     */
    private fun restoreCacheData() {
        try {
            val isCached = sharedPreferences.getBoolean("is_data_cached", false)
            val cachedCount = sharedPreferences.getInt("cached_count", 0)
            val cacheTimestamp = sharedPreferences.getLong("cache_timestamp", 0)
            
            if (isCached && cachedCount > 0) {
                // 检查缓存是否过期（24小时）
                val isExpired = System.currentTimeMillis() - cacheTimestamp > 24 * 60 * 60 * 1000
                
                if (!isExpired) {
                    // 恢复书籍数据
                    val restoredEpubFiles = mutableListOf<EpubFile>()
                    for (i in 0 until cachedCount) {
                        val name = sharedPreferences.getString("book_${i}_name", "") ?: ""
                        val path = sharedPreferences.getString("book_${i}_path", "") ?: ""
                        val size = sharedPreferences.getLong("book_${i}_size", 0)
                        val lastModified = sharedPreferences.getLong("book_${i}_lastModified", System.currentTimeMillis())
                        
                        if (name.isNotEmpty() && path.isNotEmpty()) {
                            val epubFile = EpubFile(name, path, size, lastModified)
                            restoredEpubFiles.add(epubFile)
                        }
                    }
                    
                    if (restoredEpubFiles.isNotEmpty()) {
                        cachedEpubFiles = restoredEpubFiles
                        isDataCached = true
                        android.util.Log.d("BookLibraryActivity", "恢复完整缓存数据: 文件数量=${restoredEpubFiles.size}")
                    } else {
                        android.util.Log.d("BookLibraryActivity", "恢复的缓存数据为空，清除缓存")
                        clearCacheData()
                    }
                } else {
                    android.util.Log.d("BookLibraryActivity", "缓存已过期，清除缓存")
                    clearCacheData()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("BookLibraryActivity", "恢复缓存数据失败", e)
        }
    }
    
    /**
     * 清除缓存数据
     */
    private fun clearCacheData() {
        isDataCached = false
        cachedEpubFiles = emptyList()
        
        try {
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()
            android.util.Log.d("BookLibraryActivity", "缓存数据已清除")
        } catch (e: Exception) {
            android.util.Log.e("BookLibraryActivity", "清除缓存数据失败", e)
        }
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
            Environment.isExternalStorageManager()
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
