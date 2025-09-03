package com.ibylin.app.reader

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ibylin.app.R
import com.ibylin.app.databinding.ActivityReadiumReaderBinding
import com.ibylin.app.utils.LibreraHelper
import com.ibylin.app.ui.LibreraSettingsActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Librera Reader阅读器主Activity
 * 支持EPUB、PDF等多种格式的阅读
 */
@AndroidEntryPoint
class ReadiumReaderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReadiumReaderBinding
    private var currentBookPath: String? = null
    
    @Inject
    lateinit var libreraHelper: LibreraHelper
    
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadiumReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 获取传入的书籍路径
        currentBookPath = intent.getStringExtra("book_path")
        
        initViews()
        setupLibreraReader()
        // 延迟加载书籍，确保 LibreraHelper 完全初始化
        binding.root.post {
            loadBook()
        }
    }

    private fun initViews() {
        // 设置工具栏
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Librera Reader"
        
        // 添加设置菜单按钮
        binding.toolbar.inflateMenu(R.menu.menu_reader)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_settings -> {
                    openSettings()
                    true
                }
                else -> false
            }
        }
        
        // 设置阅读器容器
        binding.readerContainer.visibility = View.GONE
        binding.loadingView.visibility = View.VISIBLE
    }

    private fun setupControlButtons() {
        binding.btnPreviousPage.setOnClickListener {
            try {
                if (::libreraHelper.isInitialized) {
                    val success = libreraHelper.previousPage()
                    if (success) {
                        updatePageInfo()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ReadiumReaderActivity, "翻页失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        binding.btnNextPage.setOnClickListener {
            try {
                if (::libreraHelper.isInitialized) {
                    val success = libreraHelper.nextPage()
                    if (success) {
                        updatePageInfo()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ReadiumReaderActivity, "翻页失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLibreraReader() {
        try {
            // 检查 LibreraHelper 是否已正确注入
            if (!::libreraHelper.isInitialized) {
                throw Exception("LibreraHelper 未正确注入")
            }
            
            // 记录初始化开始
            android.util.Log.d("ReadiumReader", "开始初始化 LibreraHelper")
            
            // 设置阅读器容器
            libreraHelper.setReaderContainer(binding.readerContainer)
            
            // 设置控制按钮
            setupControlButtons()
            
            // 记录成功日志
            android.util.Log.d("ReadiumReader", "LibreraHelper 初始化成功")
            
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("ReadiumReader", "初始化失败: ${e.message}", e)
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            
            // 显示错误信息给用户
            binding.loadingView.visibility = View.GONE
            binding.readerContainer.visibility = View.VISIBLE
            binding.tvPageInfo.text = "初始化失败: ${e.message}"
        }
    }

    private fun loadBook() {
        try {
            currentBookPath?.let { path ->
                android.util.Log.d("ReadiumReader", "开始加载书籍: $path")
                
                // 显示加载状态
                binding.loadingView.visibility = View.VISIBLE
                binding.readerContainer.visibility = View.GONE
                
                // 在后台线程中加载书籍
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val success = libreraHelper.openBook(path)
                        
                        withContext(Dispatchers.Main) {
                            binding.loadingView.visibility = View.GONE
                            
                            if (success) {
                                binding.readerContainer.visibility = View.VISIBLE
                                // 更新页码信息
                                updatePageInfo()
                                Toast.makeText(this@ReadiumReaderActivity, "成功打开: $path", Toast.LENGTH_SHORT).show()
                                
                                android.util.Log.d("ReadiumReader", "书籍加载成功")
                            } else {
                                binding.readerContainer.visibility = View.VISIBLE
                                binding.tvPageInfo.text = "加载失败: 无法打开书籍"
                                Toast.makeText(this@ReadiumReaderActivity, "加载失败: 无法打开书籍", Toast.LENGTH_LONG).show()
                                
                                android.util.Log.e("ReadiumReader", "书籍加载失败")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            binding.loadingView.visibility = View.GONE
                            binding.readerContainer.visibility = View.VISIBLE
                            binding.tvPageInfo.text = "加载失败: ${e.message}"
                            Toast.makeText(this@ReadiumReaderActivity, "加载失败: ${e.message}", Toast.LENGTH_LONG).show()
                            
                            android.util.Log.e("ReadiumReader", "书籍加载异常: ${e.message}", e)
                        }
                    }
                }
                
            } ?: run {
                // 没有书籍路径
                binding.loadingView.visibility = View.GONE
                binding.readerContainer.visibility = View.VISIBLE
                binding.tvPageInfo.text = "错误: 未提供书籍路径"
                Toast.makeText(this, "未提供书籍路径", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.loadingView.visibility = View.GONE
            binding.readerContainer.visibility = View.VISIBLE
            binding.tvPageInfo.text = "初始化失败: ${e.message}"
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updatePageInfo() {
        try {
            if (::libreraHelper.isInitialized) {
                val currentPage = libreraHelper.getCurrentPage()
                val totalPages = libreraHelper.getTotalPages()
                binding.tvPageInfo.text = "第 $currentPage 页 / 共 $totalPages 页"
            }
        } catch (e: Exception) {
            binding.tvPageInfo.text = "页码信息获取失败"
        }
    }

    /**
     * 打开设置界面
     */
    private fun openSettings() {
        val intent = Intent(this, LibreraSettingsActivity::class.java)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理Librera Reader资源
        try {
            if (::libreraHelper.isInitialized) {
                libreraHelper.cleanup()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
