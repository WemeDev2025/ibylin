package com.ibylin.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ibylin.app.R
import com.ibylin.app.api.UnsplashPhoto
import com.ibylin.app.utils.EpubFile
import com.ibylin.app.utils.UnsplashManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CoverSelectionActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "CoverSelectionActivity"
        private const val EXTRA_BOOK = "extra_book"
        
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
    private lateinit var adapter: CoverSelectionAdapter
    private val unsplashManager = UnsplashManager.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cover_selection)
        
        book = intent.getParcelableExtra(EXTRA_BOOK) 
            ?: throw IllegalArgumentException("Book data is required")
        
        initViews()
        setupRecyclerView()
        searchCoverImages()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.rv_covers)
        progressBar = findViewById(R.id.progress_bar)
        tvStatus = findViewById(R.id.tv_status)
        
        title = "为《${book.metadata?.title ?: book.name}》选择封面"
    }
    
    private fun setupRecyclerView() {
        adapter = CoverSelectionAdapter { photo ->
            onCoverSelected(photo)
        }
        
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = adapter
    }
    
    private fun searchCoverImages() {
        showLoading(true)
        tvStatus.text = "正在搜索封面图片..."
        
        coroutineScope.launch {
            try {
                val photos = unsplashManager.searchCoverImages(
                    bookTitle = book.metadata?.title ?: book.name,
                    author = book.metadata?.author
                )
                
                if (photos.isNotEmpty()) {
                    adapter.updatePhotos(photos)
                    showLoading(false)
                    tvStatus.text = "找到 ${photos.size} 张封面图片"
                } else {
                    showLoading(false)
                    tvStatus.text = "未找到合适的封面图片"
                    Toast.makeText(this@CoverSelectionActivity, "未找到封面图片", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "搜索封面图片失败", e)
                showLoading(false)
                tvStatus.text = "搜索失败: ${e.message}"
                Toast.makeText(this@CoverSelectionActivity, "搜索失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun onCoverSelected(photo: UnsplashPhoto) {
        showLoading(true)
        tvStatus.text = "正在下载封面图片..."
        
        coroutineScope.launch {
            try {
                val localPath = unsplashManager.downloadImage(this@CoverSelectionActivity, photo, book.name)
                
                if (localPath != null) {
                    // 更新书籍封面
                    updateBookCover(localPath)
                    Toast.makeText(this@CoverSelectionActivity, "封面更新成功", Toast.LENGTH_SHORT).show()
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
        // TODO: 这里需要实现更新书籍封面的逻辑
        // 可以通过SharedPreferences保存封面路径，或者更新数据库
        Log.d(TAG, "封面已更新: $localPath")
    }
    
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    private class CoverSelectionAdapter(
        private val onCoverClick: (UnsplashPhoto) -> Unit
    ) : RecyclerView.Adapter<CoverSelectionAdapter.ViewHolder>() {
        
        private var photos: List<UnsplashPhoto> = emptyList()
        
        fun updatePhotos(newPhotos: List<UnsplashPhoto>) {
            photos = newPhotos
            notifyDataSetChanged()
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
            
            fun bind(photo: UnsplashPhoto) {
                // 使用Glide加载图片
                com.bumptech.glide.Glide.with(itemView.context)
                    .load(photo.urls.small)
                    .placeholder(R.drawable.placeholder_cover)
                    .error(R.drawable.placeholder_cover)
                    .into(ivCover)
                
                tvAuthor.text = "by ${photo.user.name}"
                
                itemView.setOnClickListener {
                    onCoverClick(photo)
                }
            }
        }
    }
}
