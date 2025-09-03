package com.ibylin.app.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ibylin.app.R
import com.ibylin.app.utils.EpubFile
import com.ibylin.app.utils.AdvancedCoverExtractor
import com.ibylin.app.utils.CoverResult
import com.ibylin.app.utils.CoverExtractionStats
import java.util.zip.ZipFile

class BookGridAdapter(
    private var epubFiles: List<EpubFile> = emptyList(),
    private val onItemClick: ((EpubFile) -> Unit)? = null
) : RecyclerView.Adapter<BookGridAdapter.BookGridViewHolder>() {
    
    class BookGridViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCover: ImageView = itemView.findViewById(R.id.iv_book_cover)
        val tvReadingProgress: TextView = itemView.findViewById(R.id.tv_reading_progress)
        val btnBookMenu: ImageButton = itemView.findViewById(R.id.btn_book_menu)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookGridViewHolder {
        android.util.Log.d("BookGridAdapter", "创建ViewHolder: viewType=$viewType")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book_grid, parent, false)
        return BookGridViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: BookGridViewHolder, position: Int) {
        android.util.Log.d("BookGridAdapter", "开始绑定ViewHolder: position=$position, totalItems=${epubFiles.size}")
        
        try {
            val epubFile = epubFiles[position]
            android.util.Log.d("BookGridAdapter", "绑定ViewHolder: position=$position, 书名=${epubFile.name}, 路径=${epubFile.path}")
            
            // 设置阅读进度（暂时显示0%，后续可以从数据库读取）
            holder.tvReadingProgress.text = "0%"
            
            // 加载封面图片
            android.util.Log.d("BookGridAdapter", "开始加载封面: ${epubFile.name}")
            loadCoverImage(holder.ivCover, epubFile)
            
            // 设置封面点击事件
            holder.ivCover.setOnClickListener {
                android.util.Log.d("BookGridAdapter", "封面点击事件触发: ${epubFile.name}")
                onItemClick?.invoke(epubFile)
            }
            
            // 设置菜单按钮点击事件
            holder.btnBookMenu.setOnClickListener { view ->
                android.util.Log.d("BookGridAdapter", "菜单按钮点击事件触发: ${epubFile.name}")
                showBookMenu(view, epubFile)
            }
            
            android.util.Log.d("BookGridAdapter", "ViewHolder绑定完成: position=$position, 书名=${epubFile.metadata?.title ?: epubFile.name}")
        } catch (e: Exception) {
            android.util.Log.e("BookGridAdapter", "绑定ViewHolder失败: position=$position", e)
            // 设置默认值，避免崩溃
            holder.tvReadingProgress.text = "0%"
            holder.ivCover.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
        }
    }
    
    override fun getItemCount(): Int {
        android.util.Log.d("BookGridAdapter", "getItemCount被调用: 返回${epubFiles.size}")
        return epubFiles.size
    }
    
    fun updateEpubFiles(newEpubFiles: List<EpubFile>) {
        android.util.Log.d("BookGridAdapter", "updateEpubFiles被调用: 新文件数量=${newEpubFiles.size}")
        
        // 去重复逻辑：基于书名和作者去重
        val uniqueEpubFiles = newEpubFiles.distinctBy { 
            "${it.metadata?.title ?: it.name}_${it.metadata?.author ?: "未知作者"}" 
        }
        
        android.util.Log.d("BookGridAdapter", "去重后文件数量: ${uniqueEpubFiles.size}")
        
        epubFiles = uniqueEpubFiles
        android.util.Log.d("BookGridAdapter", "epubFiles已更新: 数量=${epubFiles.size}")
        
        notifyDataSetChanged()
        android.util.Log.d("BookGridAdapter", "notifyDataSetChanged已调用")
    }
    
    /**
     * 加载封面图片
     */
    private fun loadCoverImage(imageView: ImageView, epubFile: EpubFile) {
        android.util.Log.d("BookGridAdapter", "开始加载封面图片: ${epubFile.name}")
        
        val startTime = System.currentTimeMillis()
        
        try {
            // 使用高级封面解析器
            val coverResult = AdvancedCoverExtractor.extractCover(epubFile.path)
            
            val extractionTime = System.currentTimeMillis() - startTime
            
            if (coverResult.isSuccess && coverResult.bitmap != null) {
                android.util.Log.d("BookGridAdapter", "封面解析成功: ${coverResult.coverPath}, 方法: ${coverResult.method?.description}")
                android.util.Log.d("BookGridAdapter", "封面尺寸: ${coverResult.bitmap.width}x${coverResult.bitmap.height}")
                
                imageView.setImageBitmap(coverResult.bitmap)
                imageView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                
                android.util.Log.d("BookGridAdapter", "封面设置完成: ${epubFile.name}")
            } else {
                android.util.Log.w("BookGridAdapter", "封面解析失败: ${coverResult.errorMessage}")
                // 使用备用颜色
                setFallbackColor(imageView, epubFile.name)
            }
            
            // 记录统计信息
            CoverExtractionStats.recordResult(coverResult, extractionTime)
            
        } catch (e: Exception) {
            val extractionTime = System.currentTimeMillis() - startTime
            android.util.Log.e("BookGridAdapter", "封面加载异常: ${epubFile.name}", e)
            
            // 记录异常情况
            val errorResult = CoverResult.failure("异常: ${e.message}")
            CoverExtractionStats.recordResult(errorResult, extractionTime)
            
            // 出错时使用默认颜色
            setFallbackColor(imageView, epubFile.name)
        }
    }
    
    /**
     * 设置备用颜色
     */
    private fun setFallbackColor(imageView: ImageView, fileName: String) {
        android.util.Log.d("BookGridAdapter", "设置备用颜色: $fileName")
        
        // 使用基于文件名的颜色作为备用
        val colorSeed = (fileName.hashCode() % 360).toFloat()
        val hue = if (colorSeed < 0) colorSeed + 360 else colorSeed
        val saturation = 0.3f  // 低饱和度，避免太鲜艳
        val value = 0.8f        // 高亮度，确保可见性
        
        // 将HSV转换为RGB颜色
        val color = android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
        
        android.util.Log.d("BookGridAdapter", "备用颜色计算: hue=$hue, saturation=$saturation, value=$value, color=$color")
        
        // 设置封面背景色作为备用
        imageView.setBackgroundColor(color)
        imageView.setImageDrawable(null)
        
        android.util.Log.d("BookGridAdapter", "备用颜色设置完成: $fileName")
    }
    
    /**
     * 显示书籍菜单
     */
    private fun showBookMenu(anchorView: View, epubFile: EpubFile) {
        val popupMenu = androidx.appcompat.widget.PopupMenu(anchorView.context, anchorView)
        popupMenu.menuInflater.inflate(R.menu.menu_book_item, popupMenu.menu)
        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_open_book -> {
                    onItemClick?.invoke(epubFile)
                    true
                }
                R.id.action_book_info -> {
                    showBookInfo(anchorView.context, epubFile)
                    true
                }
                R.id.action_delete_book -> {
                    showDeleteConfirmation(anchorView.context, epubFile)
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }
    
    /**
     * 显示书籍信息
     */
    private fun showBookInfo(context: android.content.Context, epubFile: EpubFile) {
        val title = epubFile.metadata?.title ?: epubFile.name
        val author = epubFile.metadata?.author ?: "未知作者"
        val size = epubFile.getFormattedSize()
        val date = epubFile.getFormattedDate()
        
        val message = """
            书名: $title
            作者: $author
            大小: $size
            修改时间: $date
            路径: ${epubFile.path}
        """.trimIndent()
        
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("书籍信息")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }
    
    /**
     * 显示删除确认对话框
     */
    private fun showDeleteConfirmation(context: android.content.Context, epubFile: EpubFile) {
        val title = epubFile.metadata?.title ?: epubFile.name
        
        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle("删除确认")
            .setMessage("确定要删除《$title》吗？此操作不可撤销。")
            .setPositiveButton("删除") { _, _ ->
                // TODO: 实现删除逻辑
                android.util.Log.d("BookGridAdapter", "用户确认删除: ${epubFile.name}")
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
