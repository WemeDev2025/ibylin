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
import android.widget.Toast
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.appbar.MaterialToolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.ibylin.app.R
import com.ibylin.app.adapter.BookGridAdapter
import com.ibylin.app.reader.ReadiumEpubReaderActivity

import com.ibylin.app.utils.EpubFile
import com.ibylin.app.utils.BookScanner
import com.ibylin.app.utils.ReadiumHelper
import com.ibylin.app.utils.ReadiumConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookLibraryActivity : AppCompatActivity() {
    
    private lateinit var toolbar: MaterialToolbar
    private lateinit var rvBooks: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
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
        
        // 设置状态栏透明，确保背景图片透明度正确显示
        setupTransparentStatusBar()
        
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
    private var isUpdatingFromDelete = false // 防止删除后的重复计数更新
    
    // 持久化缓存
    private val sharedPreferences by lazy { getSharedPreferences("book_cache", MODE_PRIVATE)     }
    
    private fun setupTransparentStatusBar() {
        // 设置状态栏透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.decorView.systemUiVisibility = 
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }
    
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvBooks = findViewById(R.id.rv_books)
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        llScanning = findViewById(R.id.ll_scanning)
        llNoBooks = findViewById(R.id.ll_no_books)
        
        // 设置标题栏
        setupToolbar()
    }
    
    /**
     * 设置标题栏
     */
    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        
        // 设置返回按钮点击事件
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        
        // 设置菜单项点击事件
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_search -> {
                    // TODO: 实现搜索功能
                    true
                }
                R.id.action_settings -> {
                    // TODO: 打开设置页面
                    true
                }
                R.id.action_more -> {
                    // TODO: 显示更多选项
                    true
                }
                else -> false
            }
        }
    }
    
    private fun setupRecyclerView() {
        // 设置网格布局，一行2个
        val gridLayoutManager = GridLayoutManager(this, 2)
        rvBooks.layoutManager = gridLayoutManager
        
        // 添加自定义间距装饰器，减少垂直间距40%
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        rvBooks.addItemDecoration(GridSpacingItemDecoration(2, spacing, true))
        
        // iOS 风格的滑动优化
        setupIOSStyleScrolling()
        
        // 初始化适配器
        bookGridAdapter = BookGridAdapter(
            onItemClick = { bookFile ->
                // 根据文件格式选择正确的阅读器
                when {
                    bookFile.path.endsWith(".epub", ignoreCase = true) -> {
                        // EPUB文件使用ReadiumEpubReaderActivity
                        openReadiumReader(bookFile.path)
                    }

                    bookFile.path.endsWith(".pdf", ignoreCase = true) -> {
                        // PDF文件使用PDF阅读器
                        openPdfReader(bookFile.path)
                    }
                    else -> {
                        // 其他格式使用通用阅读器
                        openGenericReader(bookFile.path)
                    }
                }
            },
            onBookDeleted = { newCount ->
                // 书籍删除后更新计数和缓存
                android.util.Log.d("BookLibraryActivity", "收到书籍删除通知，新数量: $newCount")
                
                // 直接使用适配器返回的新数量，避免重复计算
                android.util.Log.d("BookLibraryActivity", "使用适配器返回的新数量: $newCount")
                
                // 更新缓存的书籍列表，过滤掉不存在的文件
                val originalCacheSize = cachedEpubFiles.size
                val filteredEpubFiles = cachedEpubFiles.filter { epubFile ->
                    val file = java.io.File(epubFile.path)
                    val exists = file.exists()
                    if (!exists) {
                        android.util.Log.d("BookLibraryActivity", "缓存中发现已删除的文件: ${epubFile.name}")
                    }
                    exists
                }
                
                // 按最后修改时间排序：最新添加的书籍显示在最前面
                val sortedFilteredEpubFiles = filteredEpubFiles.sortedByDescending { it.lastModified }
                cachedEpubFiles = sortedFilteredEpubFiles
                
                android.util.Log.d("BookLibraryActivity", "缓存过滤完成: 原始缓存数量=$originalCacheSize, 过滤后缓存数量=${filteredEpubFiles.size}, 排序后数量=${sortedFilteredEpubFiles.size}")
                
                // 保存更新后的缓存
                saveCacheData(cachedEpubFiles)
                
                // 书籍删除后更新完成
                android.util.Log.d("BookLibraryActivity", "删除后更新完成: 新数量=$newCount, 缓存数量=${cachedEpubFiles.size}")
                
                android.util.Log.d("BookLibraryActivity", "删除后更新完成: 标题数量=$newCount, 缓存数量=${cachedEpubFiles.size}")
            },
            onItemLongClick = { epubFile, view ->
                // 长按书籍时弹出菜单
                showBookLongPressMenu(epubFile, view)
            }
        )
        rvBooks.adapter = bookGridAdapter
        
        // 配置下拉刷新
        setupSwipeRefresh()
    }
    
    private fun setupIOSStyleScrolling() {
        // 使用 Android 原生最新动画
        rvBooks.setItemAnimator(androidx.recyclerview.widget.DefaultItemAnimator().apply {
            addDuration = 300L
            removeDuration = 300L
            moveDuration = 300L
            changeDuration = 300L
        })
        
        // 设置滑动行为
        rvBooks.setHasFixedSize(true)
        
        // 使用系统默认的过度滚动效果
        rvBooks.setOverScrollMode(android.view.View.OVER_SCROLL_IF_CONTENT_SCROLLS)
        
        // 添加滑动监听器，在滑动结束时播放原生弹性动画
        setupScrollListener()
        
        android.util.Log.d("BookLibraryActivity", "Android 原生动画滑动配置完成")
    }
    
    /**
     * 配置下拉刷新
     */
    private fun setupSwipeRefresh() {
        // 设置下拉刷新的颜色
        swipeRefreshLayout.setColorSchemeResources(
            R.color.apple_books_primary,
            R.color.apple_books_secondary,
            R.color.apple_books_accent
        )
        
        // 设置下拉刷新的背景颜色
        swipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.apple_books_bg)
        
        // 设置下拉刷新的监听器
        swipeRefreshLayout.setOnRefreshListener {
            android.util.Log.d("BookLibraryActivity", "下拉刷新触发")
            // 开始刷新
            startRefresh()
        }
        
        android.util.Log.d("BookLibraryActivity", "下拉刷新配置完成")
    }
    
    /**
     * 开始刷新
     */
    private fun startRefresh() {
        // 清除缓存，强制重新扫描
        clearCacheData()
        
        // 开始扫描书籍
        startBookScan()
    }
    
    private fun setupScrollListener() {
        rvBooks.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            private var isScrolling = false
            
            override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                when (newState) {
                    androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE -> {
                        if (isScrolling) {
                            // 滑动结束时播放原生弹性动画
                            playNativeBounceAnimation(rvBooks)
                            isScrolling = false
                            android.util.Log.d("BookLibraryActivity", "滑动结束，播放原生弹性动画")
                        }
                    }
                    androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING -> {
                        isScrolling = true
                    }
                    androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_SETTLING -> {
                        isScrolling = true
                    }
                }
            }
        })
    }
    
    private fun playNativeBounceAnimation(view: View) {
        try {
            // 使用 Android 原生的弹性动画
            val bounceAnimation = android.view.animation.AnimationUtils.loadAnimation(
                this,
                android.R.anim.overshoot_interpolator
            ).apply {
                duration = 400L
                interpolator = android.view.animation.OvershootInterpolator(1.5f)
            }
            
            view.startAnimation(bounceAnimation)
            android.util.Log.d("BookLibraryActivity", "原生弹性动画播放完成")
            
        } catch (e: Exception) {
            android.util.Log.e("BookLibraryActivity", "播放原生弹性动画失败", e)
        }
    }
    
    /**
     * 更新标题，显示书籍计数
     */
    private fun updateTitle(bookCount: Int) {
        // 如果正在从删除操作更新，跳过计数更新，避免重复
        if (isUpdatingFromDelete) {
            android.util.Log.d("BookLibraryActivity", "跳过计数更新，正在从删除操作更新: $bookCount")
            return
        }
        
        android.util.Log.d("BookLibraryActivity", "标题更新: 书籍数量=$bookCount")
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
                android.util.Log.d("BookLibraryActivity", "开始调用BookScanner.scanAllBooks")
                val bookScanner = BookScanner()
                val allBooks = bookScanner.scanAllBooks(this@BookLibraryActivity)
                android.util.Log.d("BookLibraryActivity", "BookScanner返回结果: 文件数量=${allBooks.size}")
                
                // 打印每个文件的详细信息
                allBooks.forEachIndexed { index, bookFile ->
                    android.util.Log.d("BookLibraryActivity", "文件[$index]: 名称=${bookFile.name}, 路径=${bookFile.path}, 大小=${bookFile.size}, 格式=${bookFile.format.displayName}")
                }
                
                withContext(Dispatchers.Main) {
                    android.util.Log.d("BookLibraryActivity", "切换到主线程，开始显示书籍")
                    hideScanningProgress()
                    
                    // 停止下拉刷新动画
                    swipeRefreshLayout.isRefreshing = false
                    
                    // 过滤掉不存在的文件，确保数据一致性
                    val validBooks = allBooks.filter { bookFile ->
                        val file = java.io.File(bookFile.path)
                        val exists = file.exists()
                        if (!exists) {
                            android.util.Log.w("BookLibraryActivity", "扫描发现已删除的文件: ${bookFile.name}")
                        }
                        exists
                    }
                    
                    android.util.Log.d("BookLibraryActivity", "扫描完成: 原始数量=${allBooks.size}, 有效数量=${validBooks.size}")
                    
                    // 缓存数据
                    cachedEpubFiles = validBooks.map { bookFile ->
                        // 转换为EpubFile格式以保持兼容性
                        EpubFile(
                            name = bookFile.name,
                            path = bookFile.path,
                            size = bookFile.size,
                            lastModified = bookFile.lastModified,
                            metadata = bookFile.metadata?.let { metadata ->
                                com.ibylin.app.utils.EpubFileMetadata(
                                    title = metadata.title,
                                    author = metadata.author,
                                    coverImagePath = metadata.coverImagePath,
                                    description = metadata.description,
                                    version = metadata.version
                                )
                            }
                        )
                    }
                    isDataCached = true
                    
                    // 保存缓存到SharedPreferences
                    saveCacheData(cachedEpubFiles)
                    
                    showBooks(cachedEpubFiles)
                }
            } catch (e: Exception) {
                android.util.Log.e("BookLibraryActivity", "扫描书籍时发生异常", e)
                withContext(Dispatchers.Main) {
                    android.util.Log.d("BookLibraryActivity", "异常处理：隐藏扫描进度，显示无书籍")
                    hideScanningProgress()
                    
                    // 停止下拉刷新动画
                    swipeRefreshLayout.isRefreshing = false
                    
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
        
        // 确保数据一致性：再次过滤不存在的文件
        val validEpubFiles = epubFiles.filter { epubFile ->
            val file = java.io.File(epubFile.path)
            val exists = file.exists()
            if (!exists) {
                android.util.Log.w("BookLibraryActivity", "showBooks中发现已删除的文件: ${epubFile.name}")
            }
            exists
        }
        
        // 去除重复的书籍（基于文件路径）
        val uniqueEpubFiles = validEpubFiles.distinctBy { it.path.lowercase().trim() }
        
        // 按最后修改时间排序：最新添加的书籍显示在最前面
        val sortedEpubFiles = uniqueEpubFiles.sortedByDescending { it.lastModified }
        
        android.util.Log.d("BookLibraryActivity", "最终显示: 原始数量=${epubFiles.size}, 有效数量=${validEpubFiles.size}, 去重后数量=${uniqueEpubFiles.size}, 排序后数量=${sortedEpubFiles.size}")
        
        if (sortedEpubFiles.isEmpty()) {
            android.util.Log.d("BookLibraryActivity", "最终文件列表为空，显示无书籍提示")
            showNoBooks()
        } else {
            android.util.Log.d("BookLibraryActivity", "最终文件列表不为空，显示书籍列表")
            android.util.Log.d("BookLibraryActivity", "设置llNoBooks为GONE, rvBooks为VISIBLE")
            llNoBooks.visibility = View.GONE
            rvBooks.visibility = View.VISIBLE
            
            android.util.Log.d("BookLibraryActivity", "调用bookGridAdapter.updateEpubFiles")
            bookGridAdapter.updateEpubFiles(sortedEpubFiles)
            android.util.Log.d("BookLibraryActivity", "bookGridAdapter.updateEpubFiles调用完成")
            
            // 在适配器更新后，使用适配器中的实际数量更新标题
            val actualCount = bookGridAdapter.itemCount
            android.util.Log.d("BookLibraryActivity", "适配器更新后，实际显示数量: $actualCount")
            updateTitle(actualCount)
        }
    }
    
    /**
     * 显示无书籍提示
     */
    private fun showNoBooks() {
        // 更新标题显示无书籍
        updateTitle(0)
        
        // 显示简单的无书籍提示
        llNoBooks.visibility = View.VISIBLE
        rvBooks.visibility = View.GONE
        
        android.util.Log.d("BookLibraryActivity", "显示无书籍提示")
    }
    
    /**
     * 打开Readium阅读器 (EPUB格式)
     */
    private fun openReadiumReader(bookPath: String) {
        val intent = Intent(this, ReadiumEpubReaderActivity::class.java).apply {
            putExtra("book_path", bookPath)
        }
        startActivity(intent)
    }
    

    
    /**
     * 打开PDF阅读器
     */
    private fun openPdfReader(bookPath: String) {
        Toast.makeText(this, "PDF阅读器功能开发中", Toast.LENGTH_SHORT).show()
        // TODO: 实现PDF阅读器
    }
    
    /**
     * 打开通用阅读器
     */
    private fun openGenericReader(bookPath: String) {
        Toast.makeText(this, "通用阅读器功能开发中", Toast.LENGTH_SHORT).show()
        // TODO: 实现通用阅读器
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
                        // 按最后修改时间排序：最新添加的书籍显示在最前面
                        val sortedRestoredEpubFiles = restoredEpubFiles.sortedByDescending { it.lastModified }
                        cachedEpubFiles = sortedRestoredEpubFiles
                        isDataCached = true
                        android.util.Log.d("BookLibraryActivity", "恢复完整缓存数据: 文件数量=${restoredEpubFiles.size}, 排序后数量=${sortedRestoredEpubFiles.size}")
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
        val intent = Intent(this, com.ibylin.app.ui.ReadiumSettingsActivity::class.java)
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
            // Android 11+ 显示Material 3风格的权限说明弹窗
            showMaterial3PermissionDialog()
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
     * 显示Material 3风格的权限说明弹窗
     */
    private fun showMaterial3PermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("文件访问权限")
            .setMessage("为了访问您的EPUB图书，需要授予文件访问权限。\n\n请在设置页面中开启\"所有文件访问权限\"。")
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
        MaterialAlertDialogBuilder(this)
            .setTitle("权限被拒绝")
            .setMessage("没有文件访问权限，无法访问图书。\n\n请在设置中手动授予权限。")
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
    
    /**
     * 处理封面更新的结果
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == com.ibylin.app.ui.CoverSelectionActivity.RESULT_COVER_UPDATED && 
            resultCode == com.ibylin.app.ui.CoverSelectionActivity.RESULT_COVER_UPDATED) {
            
            // 封面已更新，刷新书架显示
            android.util.Log.d("BookLibraryActivity", "封面已更新，刷新书架")
            refreshBookCovers()
        }
    }
    
    /**
     * 刷新书籍封面显示
     */
    private fun refreshBookCovers() {
        // 通知适配器刷新封面显示
        bookGridAdapter?.let { adapter ->
            adapter.notifyDataSetChanged()
            android.util.Log.d("BookLibraryActivity", "书架封面已刷新")
        }
    }
    
    /**
     * 自定义网格间距装饰器
     */
    private class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val spacing: Int,
        private val includeEdge: Boolean
    ) : RecyclerView.ItemDecoration() {
        
        override fun getItemOffsets(
            outRect: android.graphics.Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val column = position % spanCount
            
            if (includeEdge) {
                // 设置左右间距
                outRect.left = spacing - column * spacing / spanCount
                outRect.right = (column + 1) * spacing / spanCount
                
                // 设置上下间距 - 增加25dp
                if (position < spanCount) {
                    outRect.top = 25  // 顶部间距增加25dp
                }
                outRect.bottom = 25   // 底部间距增加25dp
            } else {
                // 设置左右间距
                outRect.left = column * spacing / spanCount
                outRect.right = spacing - (column + 1) * spacing / spanCount
                
                // 设置上下间距 - 增加25dp
                if (position >= spanCount) {
                    outRect.top = 25  // 顶部间距增加25dp
                }
            }
        }
    }
    

    


    fun cleanupDuplicateFiles(view: View) {
        android.util.Log.d("BookLibraryActivity", "清理重复文件被调用")

        val uniqueEpubFiles = cachedEpubFiles.distinctBy { it.path.lowercase().trim() }
        val originalCount = cachedEpubFiles.size
        val newCount = uniqueEpubFiles.size

        if (newCount < originalCount) {
            android.util.Log.d("BookLibraryActivity", "发现重复文件，清理前数量: $originalCount, 清理后数量: $newCount")
            cachedEpubFiles = uniqueEpubFiles
            saveCacheData(cachedEpubFiles)
            showBooks(cachedEpubFiles)
            android.util.Log.d("BookLibraryActivity", "重复文件清理完成，缓存已更新")
        } else {
            android.util.Log.d("BookLibraryActivity", "没有发现重复文件，缓存数量保持不变")
            android.widget.Toast.makeText(this, "没有发现重复文件", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 显示书籍长按菜单
     */
    private fun showBookLongPressMenu(epubFile: EpubFile, anchorView: View) {
        android.util.Log.d("BookLibraryActivity", "显示书籍长按菜单: ${epubFile.name}")
        
        val popupMenu = android.widget.PopupMenu(this, anchorView)
        popupMenu.inflate(R.menu.menu_book_item_long_press)
        
        // 设置菜单项点击事件
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_book_info -> {
                    android.util.Log.d("BookLibraryActivity", "长按菜单：显示图书信息")
                    showBookInfo(epubFile)
                    true
                }
                R.id.action_change_cover -> {
                    android.util.Log.d("BookLibraryActivity", "长按菜单：更换封面")
                    openCoverSelection(epubFile)
                    true
                }
                R.id.action_share_book -> {
                    android.util.Log.d("BookLibraryActivity", "长按菜单：分享")
                    shareBook(epubFile)
                    true
                }
                R.id.action_delete_book -> {
                    android.util.Log.d("BookLibraryActivity", "长按菜单：删除")
                    showDeleteBookDialog(epubFile)
                    true
                }
                else -> false
            }
        }
        
        // 显示菜单
        popupMenu.show()
    }
    
    /**
     * 显示书籍信息
     */
    private fun showBookInfo(epubFile: EpubFile) {
        val message = """
            书名：${epubFile.metadata?.title ?: epubFile.name}
            作者：${epubFile.metadata?.author ?: "未知"}
            文件大小：${android.text.format.Formatter.formatFileSize(this, epubFile.size)}
            文件路径：${epubFile.path}
            最后修改：${android.text.format.DateFormat.format("yyyy-MM-dd HH:mm:ss", epubFile.lastModified)}
        """.trimIndent()
        
        MaterialAlertDialogBuilder(this)
            .setTitle("图书信息")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }
    
    /**
     * 打开封面选择
     */
    private fun openCoverSelection(epubFile: EpubFile) {
        val intent = Intent(this, com.ibylin.app.ui.CoverSelectionActivity::class.java).apply {
            putExtra(com.ibylin.app.ui.CoverSelectionActivity.EXTRA_BOOK, epubFile)
        }
        startActivity(intent)
    }
    
    /**
     * 分享书籍
     */
    private fun shareBook(epubFile: EpubFile) {
        try {
            val file = java.io.File(epubFile.path)
            if (!file.exists()) {
                android.widget.Toast.makeText(this, "文件不存在，无法分享", android.widget.Toast.LENGTH_SHORT).show()
                return
            }

            // 使用FileProvider来安全地分享文件
            val fileUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream" // 使用通用的二进制文件类型
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "分享书籍：${epubFile.metadata?.title ?: epubFile.name}")
                putExtra(Intent.EXTRA_TEXT, "我正在阅读《${epubFile.metadata?.title ?: epubFile.name}》，推荐给你！")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // 创建分享选择器
            val chooserIntent = Intent.createChooser(shareIntent, "分享书籍")
            chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            
            startActivity(chooserIntent)
            
        } catch (e: Exception) {
            android.util.Log.e("BookLibraryActivity", "分享失败", e)
            android.widget.Toast.makeText(this, "分享失败：${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 显示删除书籍确认对话框
     */
    private fun showDeleteBookDialog(epubFile: EpubFile) {
        MaterialAlertDialogBuilder(this)
            .setTitle("删除确认")
            .setMessage("确定要删除《${epubFile.metadata?.title ?: epubFile.name}》吗？\n\n此操作不可撤销。")
            .setPositiveButton("删除") { _, _ ->
                deleteBook(epubFile)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 删除书籍
     */
    private fun deleteBook(epubFile: EpubFile) {
        try {
            val file = java.io.File(epubFile.path)
            if (file.exists() && file.delete()) {
                android.util.Log.d("BookLibraryActivity", "书籍删除成功: ${epubFile.name}")
                android.widget.Toast.makeText(this, "书籍删除成功", android.widget.Toast.LENGTH_SHORT).show()
                
                // 刷新书籍列表
                startBookScan()
            } else {
                android.util.Log.e("BookLibraryActivity", "书籍删除失败: ${epubFile.name}")
                android.widget.Toast.makeText(this, "删除失败，请重试", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("BookLibraryActivity", "删除书籍异常", e)
            android.widget.Toast.makeText(this, "删除失败：${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
