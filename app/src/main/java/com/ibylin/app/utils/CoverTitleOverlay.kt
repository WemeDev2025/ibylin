package com.ibylin.app.utils

import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.text.TextUtils
import android.util.Log
import com.ibylin.app.ui.TitlePosition
import com.ibylin.app.ui.TitleLayout
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * 封面标题叠加工具类
 * 负责将书名合成到封面图片上
 */
object CoverTitleOverlay {
    
    private const val TAG = "CoverTitleOverlay"
    
    // 字体配置
    private const val TITLE_TEXT_SIZE_RATIO = 0.20f // 标题字体大小比例（进一步增大）
    private const val TITLE_MARGIN_RATIO = 0.05f    // 标题边距比例
    private const val MAX_TITLE_LINES_HORIZONTAL = 3 // 水平布局最大行数
    private const val MAX_TITLE_LINES_VERTICAL = 7   // 垂直布局最大行数
    private const val MAX_CHARS_PER_COLUMN = 6       // 垂直布局每列最大字符数
    private const val ELLIPSIS = "..."              // 省略号
    
    // 颜色配置
    private val TITLE_COLOR = Color.WHITE
    private val SHADOW_COLOR = Color.BLACK
    private val SHADOW_RADIUS = 4f
    private val SHADOW_OFFSET_X = 2f
    private val SHADOW_OFFSET_Y = 2f
    
    /**
     * 将书名合成到封面图片上
     * @param context 上下文
     * @param originalImagePath 原始图片路径
     * @param bookTitle 书名
     * @param bookName 书籍文件名（用于生成新文件名）
     * @param position 书名位置
     * @param layout 书名布局
     * @return 合成后的图片路径，失败返回null
     */
    fun addTitleToCover(
        context: Context,
        originalImagePath: String,
        bookTitle: String,
        bookName: String,
        position: TitlePosition = TitlePosition.CENTER,
        layout: TitleLayout = TitleLayout.HORIZONTAL,
        color: TitleColor = TitleColor.BLACK,
        font: TitleFont = TitleFont.SYSTEM
    ): String? {
        return try {
            Log.d(TAG, "开始合成书名到封面: $bookTitle")
            
            // 加载原始图片
            val originalBitmap = BitmapFactory.decodeFile(originalImagePath)
            if (originalBitmap == null) {
                Log.e(TAG, "无法加载原始图片: $originalImagePath")
                return null
            }
            
            // 创建合成后的图片
            val overlayBitmap = createOverlayBitmap(context, originalBitmap, bookTitle, position, layout, color, font)
            if (overlayBitmap == null) {
                Log.e(TAG, "创建合成图片失败")
                return null
            }
            
            // 保存合成后的图片
            val outputPath = generateOutputPath(context, bookName, bookTitle)
            val success = saveBitmapToFile(overlayBitmap, outputPath)
            
            if (success) {
                Log.d(TAG, "书名合成成功: $outputPath")
                outputPath
            } else {
                Log.e(TAG, "保存合成图片失败")
                null
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "合成书名到封面失败", e)
            null
        }
    }
    
    /**
     * 创建带标题的合成图片
     */
    private fun createOverlayBitmap(context: Context, originalBitmap: Bitmap, bookTitle: String, position: TitlePosition, layout: TitleLayout, color: TitleColor, font: TitleFont): Bitmap? {
        return try {
            val width = originalBitmap.width
            val height = originalBitmap.height
            
            // 创建新的画布
            val overlayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(overlayBitmap)
            
            // 绘制原始图片
            canvas.drawBitmap(originalBitmap, 0f, 0f, null)
            
            // 绘制书名
            drawTitleOnCanvas(context, canvas, width, height, bookTitle, position, layout, color, font)
            
            overlayBitmap
        } catch (e: Exception) {
            Log.e(TAG, "创建合成图片失败", e)
            null
        }
    }
    
    /**
     * 在画布上绘制书名
     */
    private fun drawTitleOnCanvas(context: Context, canvas: Canvas, width: Int, height: Int, bookTitle: String, position: TitlePosition, layout: TitleLayout, color: TitleColor, font: TitleFont) {
        Log.d("CoverTitleOverlay", "drawTitleOnCanvas: title='$bookTitle', position=$position, layout=$layout")
        // 创建文字画笔
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            // 设置颜色
            this.color = when (color) {
                TitleColor.BLACK -> Color.BLACK
                TitleColor.WHITE -> Color.WHITE
            }
            // 垂直布局时使用正常字体大小（因为现在是多列显示）
            textSize = (width * TITLE_TEXT_SIZE_RATIO).toInt().toFloat()
            
            // 设置字体
            typeface = when (font) {
                TitleFont.SYSTEM -> Typeface.DEFAULT_BOLD
                TitleFont.MASHANZHENG -> {
                    try {
                        context.resources.getFont(com.ibylin.app.R.font.mashanzheng_regular)
                    } catch (e: Exception) {
                        Log.w(TAG, "加载马善政字体失败，使用默认字体", e)
                        Typeface.DEFAULT_BOLD
                    }
                }
                TitleFont.NOTOSERIF -> Typeface.create("serif", Typeface.NORMAL)
            }
            
            isAntiAlias = true
            isSubpixelText = true
            setShadowLayer(SHADOW_RADIUS, SHADOW_OFFSET_X, SHADOW_OFFSET_Y, SHADOW_COLOR)
        }
        
        // 计算文字区域
        val margin = (width * TITLE_MARGIN_RATIO).toInt()
        val maxWidth = width - margin * 2
        // 使用正常的行间距
        val lineHeight = textPaint.fontSpacing.toInt()
        
        // 根据布局处理书名
        val lines = if (layout == TitleLayout.VERTICAL) {
            // 垂直布局：多列显示，每列最多6个汉字
            splitTextIntoVerticalColumns(bookTitle)
        } else {
            // 水平布局：正常分行
            splitTextIntoLines(bookTitle, textPaint, maxWidth)
        }
        
        // 根据布局选择最大行数
        val maxLines = if (layout == TitleLayout.VERTICAL) {
            MAX_TITLE_LINES_VERTICAL
        } else {
            MAX_TITLE_LINES_HORIZONTAL
        }
        
        val finalLines = if (lines.size > maxLines) {
            // 如果行数过多，截断并添加省略号
            val truncatedLines = lines.take(maxLines - 1)
            val lastLine = lines[maxLines - 1]
            val truncatedLastLine = if (layout == TitleLayout.VERTICAL) {
                lastLine
            } else {
                truncateText(lastLine, textPaint, maxWidth)
            }
            truncatedLines + truncatedLastLine
        } else {
            lines
        }
        
        // 计算垂直位置
        val totalTextHeight = finalLines.size * lineHeight
        val textStartY = when (position) {
            TitlePosition.TOP -> margin + textPaint.fontSpacing.toInt() + 10 // 增加10dp的顶部间距
            TitlePosition.CENTER -> (height - totalTextHeight) / 2 + textPaint.fontSpacing.toInt()
            TitlePosition.BOTTOM -> height - margin - totalTextHeight + margin / 4 // 减少底部间距，拉近一些
            TitlePosition.LEFT, TitlePosition.RIGHT -> (height - totalTextHeight) / 2 + textPaint.fontSpacing.toInt() // 垂直居中
        }
        
        // 绘制每一行文字
        Log.d("CoverTitleOverlay", "开始绘制文字: layout=$layout, finalLines.size=${finalLines.size}")
        if (layout == TitleLayout.VERTICAL) {
            // 垂直布局：每个字符单独一行，多列显示
            val maxCharsPerColumn = 6 // 每列最多6个字符
            val columnWidth = lineHeight.toFloat() // 每列宽度等于行高
            val maxColumns = (width - margin * 2) / columnWidth.toInt() // 计算最多能放几列
            
            finalLines.forEachIndexed { index, line ->
                val columnIndex = index / maxCharsPerColumn // 第几列
                val rowIndex = index % maxCharsPerColumn    // 第几行
                
                val x = when (position) {
                    TitlePosition.LEFT -> margin.toFloat() + columnIndex * columnWidth
                    TitlePosition.CENTER -> {
                        val totalColumns = (finalLines.size + maxCharsPerColumn - 1) / maxCharsPerColumn
                        val totalWidth = totalColumns * columnWidth
                        val startX = (width - totalWidth) / 2
                        startX + columnIndex * columnWidth
                    }
                    TitlePosition.RIGHT -> {
                        val totalColumns = (finalLines.size + maxCharsPerColumn - 1) / maxCharsPerColumn
                        val totalWidth = totalColumns * columnWidth
                        width - margin - totalWidth + columnIndex * columnWidth
                    }
                    else -> {
                        // 如果是从水平布局切换过来的，默认使用居中
                        val totalColumns = (finalLines.size + maxCharsPerColumn - 1) / maxCharsPerColumn
                        val totalWidth = totalColumns * columnWidth
                        val startX = (width - totalWidth) / 2
                        startX + columnIndex * columnWidth
                    }
                }
                
                val y = textStartY + rowIndex * lineHeight.toFloat()
                canvas.drawText(line, x, y, textPaint)
            }
        } else {
            // 水平布局：文字从左到右排列，一行一行往下
            finalLines.forEachIndexed { index, line ->
                val lineWidth = textPaint.measureText(line)
                val x = when (position) {
                    TitlePosition.TOP, TitlePosition.CENTER, TitlePosition.BOTTOM -> (width - lineWidth) / 2
                    TitlePosition.LEFT -> margin.toFloat()
                    TitlePosition.RIGHT -> width - margin - lineWidth
                }
                
                val y = textStartY + index * lineHeight.toFloat()
                canvas.drawText(line, x, y, textPaint)
            }
        }
    }
    
    /**
     * 将文本分行
     */
    private fun splitTextIntoLines(text: String, textPaint: TextPaint, maxWidth: Int): List<String> {
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        for (char in text) {
            val testLine = currentLine + char
            val testWidth = textPaint.measureText(testLine)
            
            if (testWidth <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = char.toString()
                } else {
                    lines.add(char.toString())
                }
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        return lines
    }
    
    /**
     * 截断文本并添加省略号
     */
    private fun truncateText(text: String, textPaint: TextPaint, maxWidth: Int): String {
        if (textPaint.measureText(text) <= maxWidth) {
            return text
        }
        
        val ellipsisWidth = textPaint.measureText(ELLIPSIS)
        val availableWidth = maxWidth - ellipsisWidth
        
        var truncatedText = text
        while (textPaint.measureText(truncatedText) > availableWidth && truncatedText.length > 1) {
            truncatedText = truncatedText.dropLast(1)
        }
        
        return truncatedText + ELLIPSIS
    }
    
    /**
     * 生成输出文件路径
     */
    private fun generateOutputPath(context: Context, bookName: String, bookTitle: String): String {
        val coverDir = File(context.filesDir, "cover_images")
        if (!coverDir.exists()) {
            coverDir.mkdirs()
        }
        
        val timestamp = System.currentTimeMillis()
        val safeBookName = bookName.replace(Regex("[^a-zA-Z0-9\u4e00-\u9fa5]"), "_")
        val safeTitle = bookTitle.replace(Regex("[^a-zA-Z0-9\u4e00-\u9fa5]"), "_")
        
        val fileName = "title_overlay_${safeBookName}_${safeTitle}_$timestamp.jpg"
        return File(coverDir, fileName).absolutePath
    }
    
    /**
     * 保存位图到文件
     */
    private fun saveBitmapToFile(bitmap: Bitmap, filePath: String): Boolean {
        return try {
            val file = File(filePath)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            true
        } catch (e: IOException) {
            Log.e(TAG, "保存图片失败", e)
            false
        }
    }
    
    /**
     * 将文本分割为垂直布局的多列
     * 垂直布局：每列最多6个汉字，超过6个字符时往右侧多列显示
     */
    private fun splitTextIntoVerticalColumns(text: String): List<String> {
        // 垂直布局：每个字符单独一行
        return text.toCharArray().map { it.toString() }
    }
}
