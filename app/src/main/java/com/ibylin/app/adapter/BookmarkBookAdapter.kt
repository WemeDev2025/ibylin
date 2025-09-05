package com.ibylin.app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ibylin.app.R
import com.ibylin.app.utils.BookmarkManager
import com.ibylin.app.utils.CoverManager

/**
 * 书签图书适配器
 */
class BookmarkBookAdapter(
    private val context: Context,
    private val onBookmarkClick: (BookmarkManager.BookmarkBook) -> Unit
) : RecyclerView.Adapter<BookmarkBookAdapter.BookmarkBookViewHolder>() {

    private var bookmarkBooks = mutableListOf<BookmarkManager.BookmarkBook>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkBookViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_bookmark_book, parent, false)
        return BookmarkBookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookmarkBookViewHolder, position: Int) {
        val bookmarkBook = bookmarkBooks[position]
        holder.bind(bookmarkBook)
    }

    override fun getItemCount(): Int = bookmarkBooks.size

    /**
     * 更新书签图书列表
     */
    fun updateBookmarkBooks(newBookmarkBooks: List<BookmarkManager.BookmarkBook>) {
        bookmarkBooks.clear()
        bookmarkBooks.addAll(newBookmarkBooks)
        notifyDataSetChanged()
    }

    inner class BookmarkBookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivBookCover: ImageView = itemView.findViewById(R.id.iv_book_cover)
        private val tvBookTitle: TextView = itemView.findViewById(R.id.tv_book_title)

        fun bind(bookmarkBook: BookmarkManager.BookmarkBook) {
            // 设置书籍标题
            tvBookTitle.text = bookmarkBook.bookTitle

            // 加载书籍封面
            loadBookCover(bookmarkBook.bookPath)

            // 设置点击事件
            itemView.setOnClickListener {
                onBookmarkClick(bookmarkBook)
            }
        }

        /**
         * 加载书籍封面
         */
        private fun loadBookCover(bookPath: String) {
            try {
                val bookName = java.io.File(bookPath).name
                
                // 首先检查是否有自定义封面
                val customCoverPath = CoverManager.getBookCover(context, bookName)
                if (customCoverPath != null && java.io.File(customCoverPath).exists()) {
                    // 使用Glide加载自定义封面
                    com.bumptech.glide.Glide.with(context)
                        .load(customCoverPath)
                        .placeholder(R.drawable.default_book_cover)
                        .error(R.drawable.default_book_cover)
                        .into(ivBookCover)
                    return
                }
                
                // 如果没有自定义封面，使用高级封面解析器
                val coverResult = com.ibylin.app.utils.AdvancedCoverExtractor.extractCover(bookPath)
                if (coverResult.isSuccess && coverResult.bitmap != null) {
                    ivBookCover.setImageBitmap(coverResult.bitmap)
                } else {
                    ivBookCover.setImageResource(R.drawable.default_book_cover)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("BookmarkBookAdapter", "加载封面失败", e)
                ivBookCover.setImageResource(R.drawable.default_book_cover)
            }
        }
    }
}
