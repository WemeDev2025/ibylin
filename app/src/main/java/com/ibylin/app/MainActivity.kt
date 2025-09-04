package com.ibylin.app

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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.content.ContextCompat

import com.ibylin.app.reader.ReadiumEpubReaderActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    

    private lateinit var btnBookLibrary: android.widget.Button
    
    // 最后阅读图书相关视图
    private lateinit var llLastReadCard: android.widget.LinearLayout
    private lateinit var llWelcomeContainer: android.widget.LinearLayout
    private lateinit var ivLastReadCover: android.widget.ImageView
    private lateinit var tvLastReadTitle: android.widget.TextView
    private lateinit var tvLastReadProgress: android.widget.TextView
    private lateinit var tvLastReadTime: android.widget.TextView
    private lateinit var btnContinueReading: android.widget.Button
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    // 权限请求回调（保留用于兼容性）
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "文件权限已获取", Toast.LENGTH_SHORT).show()
        } else {
            // 权限被拒绝，直接跳转设置页面
            openAppPermissionSettings()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupClickListeners()
    }
    
    override fun onResume() {
        super.onResume()
        // 检查并显示最后阅读的图书
        checkAndShowLastReadBook()
    }
    
    private fun initViews() {
        btnBookLibrary = findViewById(R.id.btn_book_library)
        
        // 初始化最后阅读图书相关视图
        llLastReadCard = findViewById(R.id.ll_last_read_card)
        llWelcomeContainer = findViewById(R.id.ll_welcome_container)
        ivLastReadCover = findViewById(R.id.iv_last_read_cover)
        tvLastReadTitle = findViewById(R.id.tv_last_read_title)
        tvLastReadProgress = findViewById(R.id.tv_last_read_progress)
        tvLastReadTime = findViewById(R.id.tv_last_read_time)
        btnContinueReading = findViewById(R.id.btn_continue_reading)
    }
    
    private fun setupClickListeners() {

        // 书架按钮点击事件
        btnBookLibrary.setOnClickListener {
            openBookLibrary()
        }
        
        // 继续阅读按钮点击事件
        btnContinueReading.setOnClickListener {
            openLastReadBook()
        }
        


    }
    
    private fun showPermissionDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("授权") { _, _ ->
                checkAndRequestPermissions()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun checkAndRequestPermissions() {
        // 获取所有文件权限
        val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (permissionsToRequest.isEmpty()) {
            // 检查是否需要MANAGE_EXTERNAL_STORAGE权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (!Environment.isExternalStorageManager()) {
                    // 直接跳转到MANAGE_EXTERNAL_STORAGE权限页面
                    openManageStorageSettings()
                } else {
                    Toast.makeText(this, "所有文件权限已获取", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "所有文件权限已获取", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 直接跳转到应用权限设置页面
            openAppPermissionSettings()
        }
    }
    
    private fun showManageStoragePermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("完整文件访问权限")
            .setMessage("为了访问所有文件，需要授予完整文件访问权限。\n\n请在设置页面中开启此权限。")
            .setPositiveButton("去设置") { _, _ ->
                openManageStorageSettings()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun openManageStorageSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            } catch (e: Exception) {
                // 如果无法打开应用权限页面，尝试打开所有文件访问权限页面
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                } catch (e2: Exception) {
                    Toast.makeText(this, "无法打开文件权限设置", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    /**
     * 直接跳转到文件管理授权页面
     */
    private fun openAppPermissionSettings() {
        try {
            // 优先跳转到文件管理授权页面
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // 备用方案：跳转到所有文件访问权限页面
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            } catch (e2: Exception) {
                // 最后备用：应用权限页面
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    startActivity(intent)
                } catch (e3: Exception) {
                    Toast.makeText(this, "无法打开文件权限设置页面", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("权限被拒绝")
            .setMessage("文件访问权限被拒绝，需要在系统设置中手动开启。\n\n请在设置页面中授予权限。")
            .setPositiveButton("去设置") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示Material 3风格的权限申请弹窗
     */
    private fun showMaterial3PermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("文件访问权限")
            .setMessage("为了访问您的EPUB图书，需要授予文件访问权限。\n\n请在设置页面中开启\"所有文件访问权限\"。")
            .setPositiveButton("去设置") { _, _ ->
                // 跳转到设置页面
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    } else {
                        openAppSettings()
                    }
                } catch (e: Exception) {
                    openAppSettings()
                }
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // 如果无法打开应用详情页面，尝试打开应用列表页面
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(this, "无法打开系统设置", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 检查是否拥有所有必需的权限
     */
    private fun hasAllRequiredPermissions(): Boolean {
        android.util.Log.d("MainActivity", "开始检查权限")
        
        // 在 Android 13+ 上，主要检查 MANAGE_EXTERNAL_STORAGE 权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val hasManagePermission = Environment.isExternalStorageManager()
            android.util.Log.d("MainActivity", "Android 13+ 权限检查: MANAGE_EXTERNAL_STORAGE = $hasManagePermission")
            return hasManagePermission
        } else {
            // 在 Android 13 以下版本，检查基础存储权限
            val hasReadPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            val hasWritePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            android.util.Log.d("MainActivity", "Android 13以下权限检查: READ_EXTERNAL_STORAGE = $hasReadPermission, WRITE_EXTERNAL_STORAGE = $hasWritePermission")
            return hasReadPermission && hasWritePermission
        }
    }
    

    
    /**
     * 打开书架页面
     */
    private fun openBookLibrary() {
        android.util.Log.d("MainActivity", "openBookLibrary被调用")
        
        // 检查权限后再打开书架页面
        val hasPermissions = hasAllRequiredPermissions()
        android.util.Log.d("MainActivity", "权限检查结果: $hasPermissions")
        
        if (hasPermissions) {
            android.util.Log.d("MainActivity", "权限已授予，准备打开书库页面")
            try {
                val intent = Intent(this, com.ibylin.app.ui.BookLibraryActivity::class.java)
                startActivity(intent)
                android.util.Log.d("MainActivity", "书库页面启动成功")
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "启动书库页面失败", e)
                Toast.makeText(this, "启动书库页面失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            android.util.Log.d("MainActivity", "权限未授予，显示权限申请弹窗")
            // 显示Material 3风格的权限申请弹窗
            showMaterial3PermissionDialog()
        }
    }
    
    /**
     * 打开本地图书页面
     */
    private fun openLocalBooks() {
        val intent = Intent(this, com.ibylin.app.ui.LocalBooksActivity::class.java)
        startActivity(intent)
    }
    

    
    /**
     * 打开Readium阅读器
     */
    private fun openReadiumReader(bookPath: String) {
        try {
            val intent = Intent(this, ReadiumEpubReaderActivity::class.java).apply {
                putExtra("book_path", bookPath)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开阅读器: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 检查并显示最后阅读的图书
     */
    private fun checkAndShowLastReadBook() {
        try {
            val lastReadBook = com.ibylin.app.utils.ReadingProgressManager.getLastReadBook(this)
            
            if (lastReadBook != null) {
                // 显示最后阅读图书卡片
                showLastReadBookCard(lastReadBook)
            } else {
                // 显示默认欢迎界面
                showWelcomeInterface()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "检查最后阅读图书失败", e)
            showWelcomeInterface()
        }
    }
    
    /**
     * 显示最后阅读图书卡片
     */
    private fun showLastReadBookCard(lastReadBook: com.ibylin.app.utils.LastReadBook) {
        try {
            // 设置图书信息
            tvLastReadTitle.text = lastReadBook.name
            tvLastReadProgress.text = lastReadBook.getProgressText()
            tvLastReadTime.text = lastReadBook.getFormattedLastReadTime()
            
            // 尝试加载图书封面
            loadBookCover(lastReadBook.path)
            
            // 显示最后阅读卡片，隐藏欢迎界面
            llLastReadCard.visibility = android.view.View.VISIBLE
            llWelcomeContainer.visibility = android.view.View.GONE
            
            android.util.Log.d("MainActivity", "显示最后阅读图书卡片: ${lastReadBook.name}")
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "显示最后阅读图书卡片失败", e)
            showWelcomeInterface()
        }
    }
    
    /**
     * 显示欢迎界面
     */
    private fun showWelcomeInterface() {
        llLastReadCard.visibility = android.view.View.GONE
        llWelcomeContainer.visibility = android.view.View.VISIBLE
        android.util.Log.d("MainActivity", "显示欢迎界面")
    }
    
    /**
     * 加载图书封面
     */
    private fun loadBookCover(bookPath: String) {
        coroutineScope.launch {
            try {
                // 检查是否有自定义封面
                val hasCustomCover = com.ibylin.app.utils.CoverManager.hasCustomCover(
                    this@MainActivity, 
                    java.io.File(bookPath).name
                )
                
                if (hasCustomCover) {
                    // 加载自定义封面
                    val customCoverPath = com.ibylin.app.utils.CoverManager.getBookCover(
                        this@MainActivity, 
                        java.io.File(bookPath).name
                    )
                    
                    if (customCoverPath != null) {
                        val bitmap = android.graphics.BitmapFactory.decodeFile(customCoverPath)
                        if (bitmap != null) {
                            ivLastReadCover.setImageBitmap(bitmap)
                            return@launch
                        }
                    }
                }
                
                // 如果没有自定义封面，尝试从EPUB文件中提取
                val coverResult = com.ibylin.app.utils.AdvancedCoverExtractor.extractCover(bookPath)
                if (coverResult.isSuccess && coverResult.bitmap != null) {
                    ivLastReadCover.setImageBitmap(coverResult.bitmap)
                } else {
                    // 使用默认封面
                    ivLastReadCover.setImageResource(R.drawable.placeholder_cover)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "加载图书封面失败", e)
                ivLastReadCover.setImageResource(R.drawable.placeholder_cover)
            }
        }
    }
    
    /**
     * 打开最后阅读的图书
     */
    private fun openLastReadBook() {
        try {
            val lastReadBook = com.ibylin.app.utils.ReadingProgressManager.getLastReadBook(this)
            if (lastReadBook != null) {
                openReadiumReader(lastReadBook.path)
            } else {
                Toast.makeText(this, "未找到最后阅读的图书", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "打开最后阅读图书失败", e)
            Toast.makeText(this, "打开图书失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
