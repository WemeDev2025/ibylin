package com.ibylin.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ibylin.app.R
import com.ibylin.app.api.PexelsPhoto
import com.ibylin.app.utils.CoverManager
import com.ibylin.app.utils.EpubFile
import com.ibylin.app.utils.PexelsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CoverSelectionActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "CoverSelectionActivity"
        private const val EXTRA_BOOK = "extra_book"
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
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnLoadMore: Button
    private lateinit var adapter: CoverSelectionAdapter
    private val pexelsManager = PexelsManager.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    // 分页相关
    private var currentPage = 1
    private var hasMorePages = true
    private var currentSearchQuery = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cover_selection)
        
        book = intent.getParcelableExtra(EXTRA_BOOK) 
            ?: throw IllegalArgumentException("Book data is required")
        
        initViews()
        setupRecyclerView()
        // 不自动搜索，等用户输入关键词
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.rv_covers)
        progressBar = findViewById(R.id.progress_bar)
        tvStatus = findViewById(R.id.tv_status)
        etSearch = findViewById(R.id.et_search)
        btnSearch = findViewById(R.id.btn_search)
        btnLoadMore = findViewById(R.id.btn_load_more)
        
        title = "为《${book.metadata?.title ?: book.name}》选择封面"
        
        // 不设置默认搜索关键词，让用户自由输入
        etSearch.setText("")
        currentSearchQuery = ""
    }
    
    private fun setupRecyclerView() {
        adapter = CoverSelectionAdapter { photo ->
            onCoverSelected(photo)
        }
        
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
        
        // 设置搜索按钮点击事件
        btnSearch.setOnClickListener {
            performSearch()
        }
        
        // 设置加载更多按钮点击事件
        btnLoadMore.setOnClickListener {
            loadMoreImages()
        }
        
        // 设置搜索框回车键事件
        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
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
    
    private fun performSearch() {
        val query = etSearch.text.toString().trim()
        if (query.isEmpty()) {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show()
            return
        }
        
        currentSearchQuery = query
        currentPage = 1
        hasMorePages = true
        
        searchCoverImages(query, currentPage, isNewSearch = true)
    }
    
    private fun loadMoreImages() {
        if (hasMorePages) {
            currentPage++
            searchCoverImages(currentSearchQuery, currentPage, isNewSearch = false)
        }
    }
    
    private fun searchCoverImages(query: String, page: Int, isNewSearch: Boolean) {
        showLoading(true)
        tvStatus.text = "正在搜索封面图片..."
        
        Log.d(TAG, "开始搜索: query=$query, page=$page, isNewSearch=$isNewSearch")
        
        coroutineScope.launch {
            try {
                val photos = pexelsManager.searchCoverImages(
                    query = query,
                    page = page,
                    perPage = 30  // 增加每页数量
                )
                
                Log.d(TAG, "搜索完成: 找到 ${photos.size} 张图片")
                
                if (photos.isNotEmpty()) {
                    if (isNewSearch) {
                        adapter.updatePhotos(photos)
                        showLoading(false)
                        tvStatus.text = "找到 ${photos.size} 张封面图片"
                        Log.d(TAG, "新搜索完成，更新适配器")
                    } else {
                        adapter.addPhotos(photos)
                        showLoading(false)
                        tvStatus.text = "已加载 ${adapter.itemCount} 张封面图片"
                        Log.d(TAG, "加载更多完成，当前总数: ${adapter.itemCount}")
                    }
                    
                    // 检查是否还有更多页面
                    hasMorePages = photos.size >= 30
                    btnLoadMore.visibility = if (hasMorePages) View.VISIBLE else View.GONE
                    Log.d(TAG, "分页状态: hasMorePages=$hasMorePages, 当前页图片数=${photos.size}")
                    
                } else {
                    showLoading(false)
                    if (isNewSearch) {
                        tvStatus.text = "未找到合适的封面图片，请尝试其他关键词"
                        Toast.makeText(this@CoverSelectionActivity, "未找到封面图片，请尝试其他关键词", Toast.LENGTH_SHORT).show()
                        Log.w(TAG, "新搜索未找到图片: query=$query")
                    } else {
                        tvStatus.text = "没有更多图片了"
                        hasMorePages = false
                        btnLoadMore.visibility = View.GONE
                        Log.d(TAG, "加载更多完成，没有更多页面")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "搜索封面图片失败: query=$query, page=$page", e)
                showLoading(false)
                tvStatus.text = "搜索失败: ${e.message}"
                Toast.makeText(this@CoverSelectionActivity, "搜索失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun onCoverSelected(photo: PexelsPhoto) {
        showLoading(true)
        tvStatus.text = "正在下载封面图片..."
        
        coroutineScope.launch {
            try {
                val localPath = pexelsManager.downloadImage(this@CoverSelectionActivity, photo, book.name)
                
                if (localPath != null) {
                    // 更新书籍封面
                    updateBookCover(localPath)
                    Toast.makeText(this@CoverSelectionActivity, "封面更新成功", Toast.LENGTH_SHORT).show()
                    
                    // 返回结果给调用方
                    val resultIntent = Intent().apply {
                        putExtra(EXTRA_COVER_PATH, localPath)
                    }
                    setResult(RESULT_COVER_UPDATED, resultIntent)
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
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            recyclerView.visibility = View.GONE
        } else {
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    private class CoverSelectionAdapter(
        private val onCoverClick: (PexelsPhoto) -> Unit
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
            holder.bind(photo)
        }
        
        override fun getItemCount(): Int = photos.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivCover: ImageView = itemView.findViewById(R.id.iv_cover)
            private val tvAuthor: TextView = itemView.findViewById(R.id.tv_author)
            
            fun bind(photo: PexelsPhoto) {
                // 使用Glide加载图片
                com.bumptech.glide.Glide.with(itemView.context)
                    .load(photo.src.medium)
                    .placeholder(R.drawable.placeholder_cover)
                    .error(R.drawable.placeholder_cover)
                    .into(ivCover)
                
                tvAuthor.text = "by ${photo.photographer}"
                
                itemView.setOnClickListener {
                    onCoverClick(photo)
                }
            }
        }
    }
}
