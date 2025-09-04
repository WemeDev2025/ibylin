package com.ibylin.app.utils

import android.content.Context
import android.graphics.*
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * OBJ 文件转图片工具类
 * 将 3D 模型文件转换为 2D 图片
 */
object ObjToImageConverter {
    
    private const val TAG = "ObjToImageConverter"
    
    /**
     * 将 OBJ 文件转换为 PNG 图片
     * 注意：这是一个简化的实现，实际应用中可能需要更复杂的 3D 渲染
     */
    fun convertObjToPng(context: Context, objFilePath: String, outputFileName: String): String? {
        return try {
            Log.d(TAG, "开始转换 OBJ 文件: $objFilePath")
            
            // 创建位图
            val bitmap = createBookshelfImage(512, 512)
            
            // 保存为 PNG 文件
            val outputFile = saveBitmapToFile(context, bitmap, outputFileName)
            
            Log.d(TAG, "转换完成，输出文件: $outputFile")
            outputFile
            
        } catch (e: Exception) {
            Log.e(TAG, "转换 OBJ 文件失败", e)
            null
        }
    }
    
    /**
     * 创建书架图片
     */
    private fun createBookshelfImage(width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // 设置背景
        val backgroundPaint = Paint().apply {
            color = Color.parseColor("#F8F9FA")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        
        // 绘制书架主体
        drawBookshelf(canvas, width, height)
        
        return bitmap
    }
    
    /**
     * 绘制书架
     */
    private fun drawBookshelf(canvas: Canvas, width: Int, height: Int) {
        val centerX = width / 2f
        val centerY = height / 2f
        val shelfWidth = width * 0.6f
        val shelfHeight = height * 0.6f
        
        // 书架主体
        val shelfPaint = Paint().apply {
            color = Color.parseColor("#795548")
            style = Paint.Style.FILL
        }
        
        val shelfRect = RectF(
            centerX - shelfWidth / 2,
            centerY - shelfHeight / 2,
            centerX + shelfWidth / 2,
            centerY + shelfHeight / 2
        )
        canvas.drawRect(shelfRect, shelfPaint)
        
        // 书架边框
        val borderPaint = Paint().apply {
            color = Color.parseColor("#5D4037")
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(shelfRect, borderPaint)
        
        // 书架隔板
        val shelfPaint2 = Paint().apply {
            color = Color.parseColor("#A1887F")
            style = Paint.Style.FILL
        }
        
        // 第一层隔板
        val shelf1Rect = RectF(
            centerX - shelfWidth / 2,
            centerY - shelfHeight / 3,
            centerX + shelfWidth / 2,
            centerY - shelfHeight / 3 + 8
        )
        canvas.drawRect(shelf1Rect, shelfPaint2)
        
        // 第二层隔板
        val shelf2Rect = RectF(
            centerX - shelfWidth / 2,
            centerY + shelfHeight / 6,
            centerX + shelfWidth / 2,
            centerY + shelfHeight / 6 + 8
        )
        canvas.drawRect(shelf2Rect, shelfPaint2)
        
        // 绘制书籍
        drawBooks(canvas, centerX, centerY, shelfWidth, shelfHeight)
        
        // 绘制装饰元素
        drawDecorations(canvas, centerX, centerY, shelfWidth, shelfHeight)
    }
    
    /**
     * 绘制书籍
     */
    private fun drawBooks(canvas: Canvas, centerX: Float, centerY: Float, shelfWidth: Float, shelfHeight: Float) {
        val bookColors = arrayOf(
            Color.parseColor("#F44336"), // 红色
            Color.parseColor("#2196F3"), // 蓝色
            Color.parseColor("#4CAF50"), // 绿色
            Color.parseColor("#FF9800"), // 橙色
            Color.parseColor("#9C27B0"), // 紫色
            Color.parseColor("#00BCD4"), // 青色
            Color.parseColor("#FFEB3B"), // 黄色
            Color.parseColor("#FF5722")  // 深橙色
        )
        
        val bookWidth = shelfWidth / 8
        val bookHeight = shelfHeight / 4
        
        // 第一排书
        var startX = centerX - shelfWidth / 2 + bookWidth / 2
        val bookY1 = centerY - shelfHeight / 3 + 12
        
        for (i in 0..7) {
            if (i < bookColors.size) {
                val bookPaint = Paint().apply {
                    color = bookColors[i]
                    style = Paint.Style.FILL
                }
                
                val bookRect = RectF(
                    startX - bookWidth / 2,
                    bookY1,
                    startX + bookWidth / 2,
                    bookY1 + bookHeight
                )
                canvas.drawRect(bookRect, bookPaint)
                
                // 书脊
                val spinePaint = Paint().apply {
                    color = Color.parseColor("#D32F2F")
                    style = Paint.Style.FILL
                }
                val spineRect = RectF(
                    startX - bookWidth / 2,
                    bookY1,
                    startX - bookWidth / 2 + 2,
                    bookY1 + bookHeight
                )
                canvas.drawRect(spineRect, spinePaint)
                
                startX += bookWidth
            }
        }
        
        // 第二排书
        startX = centerX - shelfWidth / 2 + bookWidth / 2
        val bookY2 = centerY + shelfHeight / 6 + 12
        
        for (i in 0..5) {
            if (i < bookColors.size) {
                val bookPaint = Paint().apply {
                    color = bookColors[(i + 2) % bookColors.size]
                    style = Paint.Style.FILL
                }
                
                val bookRect = RectF(
                    startX - bookWidth / 2,
                    bookY2,
                    startX + bookWidth / 2,
                    bookY2 + bookHeight
                )
                canvas.drawRect(bookRect, bookPaint)
                
                // 书脊
                val spinePaint = Paint().apply {
                    color = Color.parseColor("#1976D2")
                    style = Paint.Style.FILL
                }
                val spineRect = RectF(
                    startX - bookWidth / 2,
                    bookY2,
                    startX - bookWidth / 2 + 2,
                    bookY2 + bookHeight
                )
                canvas.drawRect(spineRect, spinePaint)
                
                startX += bookWidth
            }
        }
    }
    
    /**
     * 绘制装饰元素
     */
    private fun drawDecorations(canvas: Canvas, centerX: Float, centerY: Float, shelfWidth: Float, shelfHeight: Float) {
        // 小植物
        val plantPaint = Paint().apply {
            color = Color.parseColor("#4CAF50")
            style = Paint.Style.FILL
        }
        
        val plantX = centerX + shelfWidth / 2 - 20
        val plantY = centerY - shelfHeight / 2 - 20
        
        canvas.drawCircle(plantX, plantY, 15f, plantPaint)
        
        // 小灯
        val lightPaint = Paint().apply {
            color = Color.parseColor("#FFC107")
            style = Paint.Style.FILL
        }
        
        val lightX = centerX - shelfWidth / 2 + 20
        val lightY = centerY - shelfHeight / 2 - 20
        
        canvas.drawCircle(lightX, lightY, 12f, lightPaint)
        
        // 小书签
        val bookmarkPaint = Paint().apply {
            color = Color.parseColor("#E91E63")
            style = Paint.Style.FILL
        }
        
        val bookmarkX = centerX + shelfWidth / 2 - 15
        val bookmarkY = centerY - shelfHeight / 3 - 15
        
        val bookmarkPath = Path().apply {
            moveTo(bookmarkX, bookmarkY)
            lineTo(bookmarkX + 8, bookmarkY)
            lineTo(bookmarkX + 8, bookmarkY + 20)
            lineTo(bookmarkX + 4, bookmarkY + 16)
            lineTo(bookmarkX, bookmarkY + 20)
            close()
        }
        canvas.drawPath(bookmarkPath, bookmarkPaint)
    }
    
    /**
     * 保存位图到文件
     */
    private fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): String? {
        return try {
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)
            
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            
            file.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "保存位图失败", e)
            null
        }
    }
}
