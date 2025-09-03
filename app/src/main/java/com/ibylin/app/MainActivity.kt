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
import androidx.core.content.ContextCompat

import com.ibylin.app.reader.ReadiumReaderActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private lateinit var rectangle1: View
    private lateinit var btnBookLibrary: android.widget.Button
    private lateinit var btnLocalBooks: android.widget.Button
    private lateinit var btnReaderSettings: android.widget.Button
    
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
        // 不在这里检查权限，改为在点击书架时检查
    }
    
    private fun initViews() {
        rectangle1 = findViewById(R.id.rectangle_1)
        btnBookLibrary = findViewById(R.id.btn_book_library)
        btnLocalBooks = findViewById(R.id.btn_local_books)
        btnReaderSettings = findViewById(R.id.btn_reader_settings)
    }
    
    private fun setupClickListeners() {
        // 卡片点击事件
        rectangle1.setOnClickListener {
            // 卡片点击后可以跳转到本地图书页面
            openLocalBooks()
        }
        
        // 书架按钮点击事件
        btnBookLibrary.setOnClickListener {
            openBookLibrary()
        }
        
        // 本地图书按钮点击事件
        btnLocalBooks.setOnClickListener {
            openLocalBooks()
        }
        
        // 阅读器设置按钮点击事件
        btnReaderSettings.setOnClickListener {
            openReaderSettings()
        }
    }
    
    private fun showPermissionDialog(title: String, message: String) {
        AlertDialog.Builder(this)
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
        AlertDialog.Builder(this)
            .setTitle("需要完整文件访问权限")
            .setMessage("为了扫描所有文件，需要授予完整文件访问权限")
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
        AlertDialog.Builder(this)
            .setTitle("权限被拒绝")
            .setMessage("文件访问权限被拒绝，需要在系统设置中手动开启")
            .setPositiveButton("去设置") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    /**
     * 显示Apple风格的权限申请弹窗
     */
    private fun showAppleStylePermissionDialog() {
        AlertDialog.Builder(this, R.style.AppleStyleDialog)
            .setTitle("需要文件访问权限")
            .setMessage("为了扫描和显示您的EPUB图书，需要访问设备上的文件。请在接下来的设置页面中授予\"所有文件访问权限\"。")
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
        // 在 Android 13+ 上，主要检查 MANAGE_EXTERNAL_STORAGE 权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager()
        } else {
            // 在 Android 13 以下版本，检查基础存储权限
            val hasReadPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            val hasWritePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            return hasReadPermission && hasWritePermission
        }
    }
    

    
    /**
     * 打开书架页面
     */
    private fun openBookLibrary() {
        // 检查权限后再打开书架页面
        if (hasAllRequiredPermissions()) {
            val intent = Intent(this, com.ibylin.app.ui.BookLibraryActivity::class.java)
            startActivity(intent)
        } else {
            // 显示Apple风格的权限申请弹窗
            showAppleStylePermissionDialog()
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
     * 打开阅读器设置页面
     */
    private fun openReaderSettings() {
        val intent = Intent(this, com.ibylin.app.ui.LibreraSettingsActivity::class.java)
        startActivity(intent)
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
            Toast.makeText(this, "无法打开阅读器: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
