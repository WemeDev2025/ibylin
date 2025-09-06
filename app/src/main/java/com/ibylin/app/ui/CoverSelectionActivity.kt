package com.ibylin.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.lottiefiles.dotlottie.core.widget.DotLottieAnimation
import com.lottiefiles.dotlottie.core.model.Config
import com.lottiefiles.dotlottie.core.util.DotLottieSource
import com.dotlottie.dlplayer.Mode
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.ibylin.app.R
import com.ibylin.app.api.PexelsPhoto
import com.ibylin.app.utils.CoverManager
import com.ibylin.app.utils.CoverTitleOverlay
import com.ibylin.app.utils.EpubFile
import com.ibylin.app.utils.PexelsManager
import com.ibylin.app.utils.SearchHelper
import com.ibylin.app.utils.TitleColor
import com.ibylin.app.utils.TitleFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

class CoverSelectionActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "CoverSelectionActivity"
        const val EXTRA_BOOK = "extra_book"
        const val EXTRA_COVER_PATH = "extra_cover_path"
        const val RESULT_COVER_UPDATED = 100
        
        fun start(context: Context, book: EpubFile) {
            val intent = Intent(context, CoverSelectionActivity::class.java).apply {
                putExtra(EXTRA_BOOK, book)
            }
            context.startActivity(intent)
        }
    }
    
    private lateinit var book: EpubFile
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var customSearchBar: View
    private lateinit var searchInput: TextInputEditText
    private var currentLoadingViewHolder: CoverSelectionAdapter.ViewHolder? = null
    private lateinit var clearButton: ImageView

    private lateinit var adapter: CoverSelectionAdapter
    private val pexelsManager = PexelsManager.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    // 分页相关
    private var currentPage = 1
    private var hasMorePages = true
    private var currentSearchQuery = ""
    private var isLoadingMore = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cover_selection)
        
        Log.d(TAG, "CoverSelectionActivity onCreate开始")
        Log.d(TAG, "Intent extras: ${intent.extras?.keySet()?.joinToString()}")
        
        try {
            book = intent.getParcelableExtra(EXTRA_BOOK) 
                ?: throw IllegalArgumentException("Book data is required")
            Log.d(TAG, "成功获取书籍数据: ${book.name}")
        } catch (e: Exception) {
            Log.e(TAG, "获取书籍数据失败", e)
            Toast.makeText(this, "获取书籍数据失败: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        initViews()
        setupRecyclerView()
        // 每次打开都刷新精选图片
        loadRecommendedImages()
        
        Log.d(TAG, "CoverSelectionActivity onCreate完成")
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.rv_covers)
        progressBar = findViewById(R.id.progress_bar)
        tvStatus = findViewById(R.id.tv_status)
        customSearchBar = findViewById(R.id.custom_search_bar)
        searchInput = customSearchBar.findViewById<TextInputEditText>(R.id.search_input)
        clearButton = customSearchBar.findViewById<ImageView>(R.id.clear_button)

        title = "为《${book.metadata?.title ?: book.name}》选择封面"
        
        // 设置自定义搜索框
        setupCustomSearchBar()
    }
    
    private fun setupRecyclerView() {
        // 优先使用EPUB格式里的书名，如果没有则使用文件名
        val rawBookTitle = if (book.metadata?.title.isNullOrBlank()) {
            Log.w(TAG, "EPUB元数据中没有书名，使用文件名: ${book.name}")
            book.name
        } else {
            Log.d(TAG, "使用EPUB元数据中的书名: '${book.metadata?.title}' (文件名: ${book.name})")
            book.metadata?.title ?: book.name
        }
        
        // 优化书名显示：如果包含《书名》副标题格式，只显示《书名》部分
        val bookTitle = SearchHelper.optimizeBookTitleForDisplay(rawBookTitle)
        
        Log.d(TAG, "原始书名: '$rawBookTitle'")
        Log.d(TAG, "优化后书名: '$bookTitle'")
        Log.d(TAG, "封面腰封将显示的书名: '$bookTitle'")
        
        adapter = CoverSelectionAdapter(
            onCoverClick = { photo, viewHolder ->
                onCoverSelected(photo, viewHolder)
            },
            bookTitle = bookTitle
        )
        
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
        
        // iOS 风格的滑动优化
        setupIOSStyleScrolling()
    }
    
    /**
     * 设置自定义搜索框
     */
    private fun setupCustomSearchBar() {
        // 设置搜索输入监听
        setupSearchInputListener()
        
        // 设置清除按钮监听
        setupClearButtonListener()
        
        // 设置键盘监听
        setupKeyboardListener()
        
        // 如果有保存的搜索关键词，恢复显示
        if (currentSearchQuery.isNotEmpty()) {
            searchInput.setText(currentSearchQuery)
        }
    }
    
    /**
     * 设置搜索输入监听
     */
    private fun setupSearchInputListener() {
        // 监听搜索提交
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            Log.d(TAG, "检测到编辑器动作: actionId=$actionId")
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = searchInput.text?.toString()?.trim() ?: ""
                Log.d(TAG, "检测到搜索查询: '$query'")
                if (query.isNotEmpty()) {
                    Log.d(TAG, "执行搜索: $query")
                    performSearch(query)
                    // 自动收起键盘
                    hideKeyboard()
                    return@setOnEditorActionListener true
                } else {
                    Log.w(TAG, "搜索查询为空")
                    Toast.makeText(this@CoverSelectionActivity, "请输入搜索关键词", Toast.LENGTH_SHORT).show()
                    return@setOnEditorActionListener true
                }
            }
            false
        }
        
        // 监听文本变化
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString() ?: ""
                currentSearchQuery = query
                
                // 根据文本内容显示/隐藏清除按钮
                clearButton.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                
                Log.d(TAG, "搜索关键词更新: $query")
            }
        })
    }
    
    /**
     * 设置清除按钮监听
     */
    private fun setupClearButtonListener() {
        clearButton.setOnClickListener {
            searchInput.setText("")
            searchInput.requestFocus()
            currentSearchQuery = ""
            Log.d(TAG, "清除搜索关键词")
        }
    }
    
    /**
     * 隐藏键盘
     */
    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val currentFocus = currentFocus
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            currentFocus.clearFocus()
        }
    }
    
    // 这个方法已经被 setupSearchInputListener() 替代，不再需要
    
    /**
     * 设置键盘监听器
     */
    private fun setupKeyboardListener() {
        // 监听键盘状态变化
        searchInput.viewTreeObserver.addOnGlobalLayoutListener {
            val r = android.graphics.Rect()
            searchInput.getWindowVisibleDisplayFrame(r)
            
            val screenHeight = searchInput.rootView.height
            val keypadHeight = screenHeight - r.bottom
            
            if (keypadHeight > screenHeight * 0.15) {
                // 键盘弹出
                Log.d(TAG, "键盘弹出，高度: $keypadHeight")
                // 搜索框已经在顶部，键盘弹出时会自然上移
            } else {
                // 键盘隐藏
                Log.d(TAG, "键盘隐藏")
            }
        }
    }
    
    private fun setupIOSStyleScrolling() {
        // 使用 Android 原生最新动画
        recyclerView.setItemAnimator(androidx.recyclerview.widget.DefaultItemAnimator().apply {
            addDuration = 300L
            removeDuration = 300L
            moveDuration = 300L
            changeDuration = 300L
        })
        
        // 设置滑动行为
        recyclerView.setHasFixedSize(true)
        
        // 使用系统默认的过度滚动效果
        recyclerView.setOverScrollMode(android.view.View.OVER_SCROLL_IF_CONTENT_SCROLLS)
        
        // 添加滑动监听器，在滑动结束时播放原生弹性动画
        setupScrollListener()
        
        // 添加滚动监听器，实现自动加载下一页
        setupAutoLoadMore()
        
        Log.d(TAG, "Android 原生动画滑动配置完成")
    }
    
    private fun setupScrollListener() {
        recyclerView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            private var isScrolling = false
            
            override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                when (newState) {
                    androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE -> {
                        if (isScrolling) {
                            // 滑动结束时播放原生弹性动画
                            playNativeBounceAnimation(recyclerView)
                            isScrolling = false
                            Log.d(TAG, "滑动结束，播放原生弹性动画")
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
    
    /**
     * 设置自动加载更多功能
     */
    private fun setupAutoLoadMore() {
        recyclerView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager as? androidx.recyclerview.widget.GridLayoutManager
                if (layoutManager != null) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                    
                    // 当滚动到底部附近时自动加载下一页
                    if (visibleItemCount + firstVisibleItemPosition >= totalItemCount - 5 && 
                        firstVisibleItemPosition >= 0 && 
                        totalItemCount > 0 &&
                        hasMorePages) {
                        
                        Log.d(TAG, "触发自动加载更多: visibleItemCount=$visibleItemCount, totalItemCount=$totalItemCount, firstVisibleItemPosition=$firstVisibleItemPosition")
                        
                        // 防止重复加载
                        if (!isLoadingMore) {
                            isLoadingMore = true
                            loadMoreImages()
                        }
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
            Log.d(TAG, "原生弹性动画播放完成")
            
        } catch (e: Exception) {
            Log.e(TAG, "播放原生弹性动画失败", e)
        }
    }
    
    private fun buildDefaultSearchQuery(): String {
        val title = book.metadata?.title ?: book.name
        val author = book.metadata?.author
        return if (author != null && author.isNotBlank()) {
            "$title $author book cover"
        } else {
            "$title book cover"
        }
    }
    
    /**
     * 加载API推荐的图片
     */
    private fun loadRecommendedImages() {
        currentSearchQuery = ""
        currentPage = 1
        hasMorePages = true
        
        Log.d(TAG, "加载API推荐的图片")
        tvStatus.text = "正在加载推荐图片..."
        
        // 调用PexelsManager获取精选图片
        loadCuratedImages()
    }
    
    /**
     * 加载精选图片
     */
    private fun loadCuratedImages() {
        showLoading(true)
        tvStatus.text = "正在加载精选图片..."
        
        coroutineScope.launch {
            try {
                val photos = pexelsManager.getCuratedPhotos(page = 1)
                
                Log.d(TAG, "精选图片加载完成: 找到 ${photos.size} 张图片")
                
                if (photos.isNotEmpty()) {
                    adapter.updatePhotos(photos)
                    showLoading(false)
                    tvStatus.text = ""
                    Log.d(TAG, "精选图片加载完成，更新适配器")
                    
                    // 精选图片固定为20张，不需要分页
                    hasMorePages = false
                } else {
                    showLoading(false)
                    tvStatus.text = "暂无推荐图片"
                    Log.w(TAG, "精选图片加载失败，没有找到图片")
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载精选图片失败", e)
                showLoading(false)
                tvStatus.text = "加载失败: ${e.message}"
                Toast.makeText(this@CoverSelectionActivity, "加载精选图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun performSearch(query: String) {
        Log.d(TAG, "performSearch被调用，原始查询: '$query'")
        
        if (query.isEmpty()) {
            Log.w(TAG, "搜索查询为空，显示提示")
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.d(TAG, "开始处理搜索查询: '$query'")
        
        // 新搜索开始时，立即清空旧数据
        adapter.updatePhotos(emptyList())
        tvStatus.text = "正在搜索..."
        
        // 使用优化后的搜索策略
        val optimizedQuery = SearchHelper.getOptimizedSearchQuery(query)
        currentSearchQuery = query  // 保存原始查询用于显示
        currentPage = 1
        hasMorePages = true
        
        // 确保 SearchView 显示搜索的关键词
        // searchView.setQuery(query, false) // M3 SearchView 不支持此方法
        
        Log.d(TAG, "优化搜索转换: '$query' -> '$optimizedQuery'")
        
        // 显示搜索状态
        tvStatus.text = ""
        
        Log.d(TAG, "调用searchCoverImages开始搜索")
        searchCoverImages(optimizedQuery, currentPage, isNewSearch = true)
        Log.d(TAG, "performSearch执行完成")
    }
    
    private fun loadMoreImages() {
        if (hasMorePages && !isLoadingMore) {
            currentPage++
            if (currentSearchQuery.isEmpty()) {
                // 精选图片固定为20张，不需要加载更多
                Log.d(TAG, "精选图片已达到最大数量，不加载更多")
                return
            } else {
                // 如果是搜索结果，加载更多搜索结果
                searchCoverImages(currentSearchQuery, currentPage, isNewSearch = false)
            }
        }
    }
    
    /**
     * 加载更多精选图片
     */
    private fun loadMoreCuratedImages() {
        showLoadingMore(true)
        tvStatus.text = "正在加载更多精选图片..."
        
        coroutineScope.launch {
            try {
                val photos = pexelsManager.getCuratedPhotos(page = currentPage)
                
                Log.d(TAG, "更多精选图片加载完成: 找到 ${photos.size} 张图片")
                
                if (photos.isNotEmpty()) {
                    adapter.addPhotos(photos)
                    showLoadingMore(false)
                    tvStatus.text = ""
                    Log.d(TAG, "更多精选图片加载完成，当前总数: ${adapter.itemCount}")
                    
                    // 检查是否还有更多页面
                    hasMorePages = photos.size >= 30
                    isLoadingMore = false
                } else {
                    showLoadingMore(false)
                    tvStatus.text = ""
                    hasMorePages = false
                    isLoadingMore = false
                    Log.d(TAG, "精选图片加载完成，没有更多页面")
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载更多精选图片失败", e)
                showLoadingMore(false)
                tvStatus.text = "加载失败: ${e.message}"
                Toast.makeText(this@CoverSelectionActivity, "加载更多精选图片失败: ${e.message}", Toast.LENGTH_SHORT).show()
                isLoadingMore = false
            }
        }
    }
    
    private fun searchCoverImages(query: String, page: Int, isNewSearch: Boolean) {
        if (isNewSearch) {
            showLoading(true)
            tvStatus.text = "正在搜索封面图片..."
        } else {
            showLoadingMore(true)
            tvStatus.text = "正在加载更多图片..."
        }
        
        Log.d(TAG, "开始搜索: query=$query, page=$page, isNewSearch=$isNewSearch")
        
        coroutineScope.launch {
            try {
                Log.d(TAG, "开始调用pexelsManager.searchCoverImages: query=$query, page=$page")
                val photos = pexelsManager.searchCoverImages(
                    query = query,
                    page = page,
                    perPage = 30  // 增加每页数量
                )
                
                Log.d(TAG, "搜索完成: 找到 ${photos.size} 张图片")
                Log.d(TAG, "搜索结果详情: ${photos.take(3).map { "ID=${it.id}, URL=${it.src.medium}" }}")
                
                if (photos.isNotEmpty()) {
                    if (isNewSearch) {
                        adapter.updatePhotos(photos)
                        showLoading(false)
                        tvStatus.text = ""
                        Log.d(TAG, "新搜索完成，更新适配器")
                    } else {
                        adapter.addPhotos(photos)
                        showLoadingMore(false)
                        tvStatus.text = ""
                        Log.d(TAG, "加载更多完成，当前总数: ${adapter.itemCount}")
                    }
                    
                    // 检查是否还有更多页面
                    hasMorePages = photos.size >= 30
                    isLoadingMore = false
                    Log.d(TAG, "分页状态: hasMorePages=$hasMorePages, 当前页图片数=${photos.size}")
                    
                } else {
                    if (isNewSearch) {
                        showLoading(false)
                        tvStatus.text = "未找到合适的封面图片，请尝试其他关键词"
                        Toast.makeText(this@CoverSelectionActivity, "未找到封面图片，请尝试其他关键词", Toast.LENGTH_SHORT).show()
                        Log.w(TAG, "新搜索未找到图片: query=$query")
                    } else {
                        showLoadingMore(false)
                        tvStatus.text = "没有更多图片了"
                        hasMorePages = false
                        isLoadingMore = false
                        Log.d(TAG, "加载更多完成，没有更多页面")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "搜索封面图片失败: query=$query, page=$page", e)
                if (isNewSearch) {
                    showLoading(false)
                } else {
                    showLoadingMore(false)
                }
                tvStatus.text = "搜索失败: ${e.message}"
                Toast.makeText(this@CoverSelectionActivity, "搜索失败: ${e.message}", Toast.LENGTH_SHORT).show()
                isLoadingMore = false
            }
        }
    }
    
    private fun onCoverSelected(photo: PexelsPhoto, viewHolder: CoverSelectionAdapter.ViewHolder) {
        try {
            Log.d(TAG, "onCoverSelected被调用: photo.id=${photo.id}")
            android.util.Log.d("CoverSelectionActivity", "开始下载封面，显示doLottie loading动画")
            
            // 隐藏之前的loading动画
            currentLoadingViewHolder?.hideLoadingAnimation()
            
            // 显示新的loading动画
            currentLoadingViewHolder = viewHolder
            viewHolder.showLoadingAnimation()
            
            // 只更新状态文本，不隐藏整个列表
            tvStatus.text = "正在下载封面图片..."
            
            coroutineScope.launch {
                try {
                    // 记录动画开始时间
                    val animationStartTime = System.currentTimeMillis()
                    val minAnimationDuration = 2000L // 至少播放2秒动画
                    
                    val localPath = pexelsManager.downloadImage(this@CoverSelectionActivity, photo, book.name)
                    
                    // 计算已播放时间
                    val elapsedTime = System.currentTimeMillis() - animationStartTime
                    val remainingTime = minAnimationDuration - elapsedTime
                    
                    // 如果下载太快，等待动画播放完成
                    if (remainingTime > 0) {
                        android.util.Log.d("CoverSelectionActivity", "下载完成太快，等待动画播放完成，剩余时间: ${remainingTime}ms")
                        kotlinx.coroutines.delay(remainingTime)
                    }
                    
                    if (localPath != null) {
                        // 隐藏loading动画
                        viewHolder.hideLoadingAnimation()
                        currentLoadingViewHolder = null
                        
                        // 弹出书名编辑对话框 - 确保在主线程执行
                        Log.d(TAG, "下载成功，准备显示书名编辑对话框: $localPath")
                        withContext(Dispatchers.Main) {
                            showTitleEditDialog(localPath)
                        }
                    } else {
                        // 下载失败，隐藏loading动画
                        viewHolder.hideLoadingAnimation()
                        currentLoadingViewHolder = null
                        tvStatus.text = "下载失败"
                        Toast.makeText(this@CoverSelectionActivity, "下载失败", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "下载封面图片失败", e)
                    // 下载失败，隐藏loading动画
                    viewHolder.hideLoadingAnimation()
                    currentLoadingViewHolder = null
                    tvStatus.text = "下载失败: ${e.message}"
                    Toast.makeText(this@CoverSelectionActivity, "下载失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            
        } catch (e: Exception) {
            android.util.Log.e("CoverSelectionActivity", "处理封面选择失败", e)
            viewHolder.hideLoadingAnimation()
            currentLoadingViewHolder = null
        }
    }
    
    private fun updateBookCover(localPath: String) {
        try {
            // 使用CoverManager保存封面路径
            CoverManager.saveBookCover(this, book.name, localPath)
            Log.d(TAG, "封面已更新并保存: $localPath")
        } catch (e: Exception) {
            Log.e(TAG, "保存封面路径失败", e)
        }
    }
    
    private fun showLoading(show: Boolean) {
        if (show) {
            progressBar.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            tvStatus.visibility = View.GONE
        } else {
            progressBar.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            tvStatus.visibility = View.VISIBLE
        }
    }
    
    /**
     * 显示加载更多状态（不隐藏图片列表）
     */
    private fun showLoadingMore(show: Boolean) {
        if (show) {
            progressBar.visibility = View.VISIBLE
            // 不隐藏图片列表，让用户看到现有内容
            recyclerView.visibility = View.VISIBLE
            tvStatus.visibility = View.VISIBLE
        } else {
            progressBar.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            tvStatus.visibility = View.VISIBLE
        }
    }
    
    /**
     * 显示书名编辑对话框
     */
    private fun showTitleEditDialog(originalImagePath: String) {
        try {
            Log.d(TAG, "开始显示书名编辑对话框，图片路径: $originalImagePath")
            
            // 获取默认书名
            val defaultTitle = if (book.metadata?.title.isNullOrBlank()) {
                book.name
            } else {
                book.metadata?.title ?: book.name
            }
            
            Log.d(TAG, "默认书名: $defaultTitle")
            
            // 优化书名显示
            val optimizedTitle = SearchHelper.optimizeBookTitleForDisplay(defaultTitle)
            Log.d(TAG, "优化后书名: $optimizedTitle")
            
            // 创建并显示对话框
            val dialog = TitleEditDialog.newInstance(optimizedTitle)
            Log.d(TAG, "创建TitleEditDialog实例成功")
            
            dialog.setOnTitleConfirmedListener { newTitle, position, layout, color, font ->
                Log.d(TAG, "用户确认书名: $newTitle, 位置: $position, 布局: $layout, 颜色: $color, 字体: $font")
                // 用户确认书名后，合成封面并保存
                processTitleConfirmation(originalImagePath, newTitle, position, layout, color, font)
            }
            
            Log.d(TAG, "准备显示对话框")
            Log.d(TAG, "supportFragmentManager状态: ${supportFragmentManager.isStateSaved}")
            Log.d(TAG, "Activity状态: ${if (isFinishing) "正在结束" else "正常运行"}")
            
            if (!supportFragmentManager.isStateSaved && !isFinishing) {
                dialog.show(supportFragmentManager, "TitleEditDialog")
                Log.d(TAG, "对话框显示命令已执行")
            } else {
                Log.e(TAG, "无法显示对话框: FragmentManager状态已保存或Activity正在结束")
                Toast.makeText(this, "无法显示编辑对话框", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "显示书名编辑对话框失败", e)
            Toast.makeText(this, "显示编辑对话框失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 处理书名确认
     */
    private fun processTitleConfirmation(originalImagePath: String, newTitle: String, position: TitlePosition, layout: TitleLayout, color: TitleColor, font: TitleFont) {
        try {
            // 显示处理状态
            tvStatus.text = "正在合成封面..."
            
            // 在后台线程处理图片合成
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    // 合成书名到封面
                    val overlayPath = CoverTitleOverlay.addTitleToCover(
                        this@CoverSelectionActivity,
                        originalImagePath,
                        newTitle,
                        book.name,
                        position,
                        layout,
                        color,
                        font
                    )
                    
                    if (overlayPath != null) {
                        // 切换到主线程更新UI
                        launch(Dispatchers.Main) {
                            // 保存合成后的封面
                            updateBookCover(overlayPath)
                            Toast.makeText(this@CoverSelectionActivity, "封面更新成功", Toast.LENGTH_SHORT).show()
                            
                            // 返回结果给调用方
                            val resultIntent = Intent().apply {
                                putExtra(EXTRA_COVER_PATH, overlayPath)
                            }
                            setResult(RESULT_COVER_UPDATED, resultIntent)
                            finish()
                        }
                    } else {
                        // 合成失败，使用原始图片
                        launch(Dispatchers.Main) {
                            updateBookCover(originalImagePath)
                            Toast.makeText(this@CoverSelectionActivity, "封面更新成功（未添加书名）", Toast.LENGTH_SHORT).show()
                            
                            val resultIntent = Intent().apply {
                                putExtra(EXTRA_COVER_PATH, originalImagePath)
                            }
                            setResult(RESULT_COVER_UPDATED, resultIntent)
                            finish()
                        }
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "处理书名确认失败", e)
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@CoverSelectionActivity, "处理失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "处理书名确认失败", e)
            Toast.makeText(this, "处理失败", Toast.LENGTH_SHORT).show()
        }
    }

    
    private class CoverSelectionAdapter(
        private val onCoverClick: (PexelsPhoto, ViewHolder) -> Unit,
        private val bookTitle: String
    ) : RecyclerView.Adapter<CoverSelectionAdapter.ViewHolder>() {
        
        private var photos: List<PexelsPhoto> = emptyList()
        
        fun updatePhotos(newPhotos: List<PexelsPhoto>) {
            photos = newPhotos
            notifyDataSetChanged()
        }
        
        fun addPhotos(newPhotos: List<PexelsPhoto>) {
            val oldSize = photos.size
            photos = photos + newPhotos
            notifyItemRangeInserted(oldSize, newPhotos.size)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_cover_selection, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val photo = photos[position]
            holder.bind(photo, bookTitle)
        }
        
        override fun getItemCount(): Int = photos.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivCover: ImageView = itemView.findViewById(R.id.iv_cover)
            private val loadingOverlay: View = itemView.findViewById(R.id.loading_overlay)
            private val lottieLoading: DotLottieAnimation = itemView.findViewById(R.id.lottie_loading)
            
            fun bind(photo: PexelsPhoto, bookTitle: String) {
                // 使用Glide加载图片
                com.bumptech.glide.Glide.with(itemView.context)
                    .load(photo.src.medium)
                    .placeholder(R.drawable.placeholder_cover)
                    .error(R.drawable.placeholder_cover)
                    .into(ivCover)
                
                // 图片已经设置为撑满容器，使用 centerCrop 确保图片填满整个容器
                ivCover.scaleType = ImageView.ScaleType.CENTER_CROP
                
                // 确保loading动画初始状态为隐藏
                hideLoadingAnimation()
                
                itemView.setOnClickListener {
                    Log.d("CoverSelectionAdapter", "图片被点击: photo.id=${photo.id}")
                    onCoverClick(photo, this)
                }
            }
            
            /**
             * 显示dotLottie loading动画
             */
            fun showLoadingAnimation() {
                try {
                    android.util.Log.d("CoverSelectionActivity", "开始显示dotLottie loading动画")
                    
                    // 确保在主线程执行
                    if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
                        android.util.Log.w("CoverSelectionActivity", "不在主线程，切换到主线程显示动画")
                        itemView.post {
                            showLoadingAnimationOnMainThread()
                        }
                        return
                    }
                    
                    showLoadingAnimationOnMainThread()
                    
                } catch (e: Exception) {
                    android.util.Log.e("CoverSelectionActivity", "显示dotLottie loading动画失败", e)
                }
            }
            
            private fun showLoadingAnimationOnMainThread() {
                try {
                    android.util.Log.d("CoverSelectionActivity", "在主线程显示loading动画")
                    
                    // 显示loading覆盖层
                    loadingOverlay.visibility = View.VISIBLE
                    android.util.Log.d("CoverSelectionActivity", "loading覆盖层已显示")
                    
                    // 配置dotLottie动画
                    val config = Config.Builder()
                        .autoplay(true)
                        .speed(1f)
                        .loop(true)
                        .source(DotLottieSource.Asset("waiting_animation.json"))
                        .playMode(Mode.FORWARD)
                        .useFrameInterpolation(true)
                        .build()
                    
                    android.util.Log.d("CoverSelectionActivity", "dotLottie配置已创建")
                    
                    // 加载并启动dotLottie动画
                    lottieLoading.load(config)
                    
                    android.util.Log.d("CoverSelectionActivity", "dotLottie loading动画已启动")
                    
                } catch (e: Exception) {
                    android.util.Log.e("CoverSelectionActivity", "在主线程显示loading动画失败", e)
                    // 如果dotLottie失败，至少显示覆盖层
                    try {
                        loadingOverlay.visibility = View.VISIBLE
                        android.util.Log.d("CoverSelectionActivity", "fallback: 仅显示覆盖层")
                    } catch (e2: Exception) {
                        android.util.Log.e("CoverSelectionActivity", "显示覆盖层也失败", e2)
                    }
                }
            }
            
            /**
             * 隐藏dotLottie loading动画
             */
            fun hideLoadingAnimation() {
                try {
                    android.util.Log.d("CoverSelectionActivity", "开始隐藏dotLottie loading动画")
                    
                    // 确保在主线程执行
                    if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
                        android.util.Log.w("CoverSelectionActivity", "不在主线程，切换到主线程隐藏动画")
                        itemView.post {
                            hideLoadingAnimationOnMainThread()
                        }
                        return
                    }
                    
                    hideLoadingAnimationOnMainThread()
                    
                } catch (e: Exception) {
                    android.util.Log.e("CoverSelectionActivity", "隐藏dotLottie loading动画失败", e)
                }
            }
            
            private fun hideLoadingAnimationOnMainThread() {
                try {
                    android.util.Log.d("CoverSelectionActivity", "在主线程隐藏loading动画")
                    
                    // 停止dotLottie动画
                    lottieLoading.stop()
                    
                    // 隐藏loading覆盖层
                    loadingOverlay.visibility = View.GONE
                    
                    android.util.Log.d("CoverSelectionActivity", "dotLottie loading动画已隐藏")
                    
                } catch (e: Exception) {
                    android.util.Log.e("CoverSelectionActivity", "在主线程隐藏loading动画失败", e)
                    // 如果dotLottie失败，至少隐藏覆盖层
                    try {
                        loadingOverlay.visibility = View.GONE
                        android.util.Log.d("CoverSelectionActivity", "fallback: 仅隐藏覆盖层")
                    } catch (e2: Exception) {
                        android.util.Log.e("CoverSelectionActivity", "隐藏覆盖层也失败", e2)
                    }
                }
            }
        }
    }
}
