package com.ibylin.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ibylin.app.R
import com.ibylin.app.utils.EpubFile

class EpubFileAdapter(
    private var epubFiles: List<EpubFile> = emptyList(),
    private val onItemClick: ((EpubFile) -> Unit)? = null
) : RecyclerView.Adapter<EpubFileAdapter.EpubFileViewHolder>() {
    
    class EpubFileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFileName: TextView = itemView.findViewById(R.id.tv_file_name)
        val tvFilePath: TextView = itemView.findViewById(R.id.tv_file_path)
        val tvEpubVersion: TextView = itemView.findViewById(R.id.tv_epub_version)
        val tvFileSize: TextView = itemView.findViewById(R.id.tv_file_size)
        val tvFileDate: TextView = itemView.findViewById(R.id.tv_file_date)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EpubFileViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_epub_file, parent, false)
        return EpubFileViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: EpubFileViewHolder, position: Int) {
        val epubFile = epubFiles[position]
        
        holder.tvFileName.text = epubFile.name
        holder.tvFilePath.text = epubFile.path
        
        // 显示EPUB版本信息
        val version = epubFile.metadata?.version ?: "基础EPUB"
        holder.tvEpubVersion.text = version
        
        // 根据版本设置不同的背景颜色
        val (backgroundColor, textColor) = when {
            version.contains("3.0") -> Pair("#FF5722", "#FFFFFF") // 橙色背景，白色文字
            version.contains("2.0") -> Pair("#4CAF50", "#FFFFFF") // 绿色背景，白色文字
            else -> Pair("#9E9E9E", "#FFFFFF") // 灰色背景，白色文字
        }
        
        // 动态设置背景颜色
        holder.tvEpubVersion.setBackgroundColor(android.graphics.Color.parseColor(backgroundColor))
        holder.tvEpubVersion.setTextColor(android.graphics.Color.parseColor(textColor))
        
        holder.tvFileSize.text = epubFile.getFormattedSize()
        holder.tvFileDate.text = epubFile.getFormattedDate()
        
        // 设置点击事件
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(epubFile)
        }
    }
    
    override fun getItemCount(): Int = epubFiles.size
    
    fun updateEpubFiles(newEpubFiles: List<EpubFile>) {
        epubFiles = newEpubFiles
        notifyDataSetChanged()
    }
}
