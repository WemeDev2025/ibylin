package com.ibylin.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.ibylin.app.api.UnsplashService
import com.ibylin.app.api.UnsplashPhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class UnsplashManager private constructor() {
    
    companion object {
        private const val TAG = "UnsplashManager"
        private const val UNSPLASH_ACCESS_KEY = "YOUR_UNSPLASH_ACCESS_KEY" // 需要替换为实际的API Key
        private const val UNSPLASH_BASE_URL = "https://api.unsplash.com/"
        private const val COVER_IMAGES_DIR = "book_covers"
        
        @Volatile
        private var INSTANCE: UnsplashManager? = null
        
        fun getInstance(): UnsplashManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UnsplashManager().also { INSTANCE = it }
            }
        }
    }
    
    private val unsplashService: UnsplashService by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Client-ID $UNSPLASH_ACCESS_KEY")
                    .build()
                chain.proceed(request)
            }
            .build()
        
        Retrofit.Builder()
            .baseUrl(UNSPLASH_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UnsplashService::class.java)
    }
    
    /**
     * 搜索封面图片
     */
    suspend fun searchCoverImages(
        bookTitle: String,
        author: String? = null,
        page: Int = 1
    ): List<UnsplashPhoto> = withContext(Dispatchers.IO) {
        try {
            val query = buildSearchQuery(bookTitle, author)
            Log.d(TAG, "搜索封面图片: $query")
            
            val response = unsplashService.searchPhotos(
                query = query,
                page = page,
                perPage = 20,
                orientation = "portrait"
            )
            
            Log.d(TAG, "搜索到 ${response.results.size} 张图片")
            response.results
        } catch (e: Exception) {
            Log.e(TAG, "搜索封面图片失败", e)
            emptyList()
        }
    }
    
    /**
     * 下载图片
     */
    suspend fun downloadImage(
        context: Context,
        photo: UnsplashPhoto,
        bookName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val imageUrl = photo.urls.regular
            Log.d(TAG, "开始下载图片: $imageUrl")
            
            val client = OkHttpClient()
            val request = okhttp3.Request.Builder().url(imageUrl).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val inputStream: InputStream? = response.body?.byteStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                
                if (bitmap != null) {
                    val filePath = saveImageToLocal(context, bitmap, bookName, photo.id)
                    Log.d(TAG, "图片下载成功: $filePath")
                    return@withContext filePath
                }
            }
            
            Log.e(TAG, "图片下载失败: ${response.code}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "下载图片异常", e)
            null
        }
    }
    
    /**
     * 保存图片到本地
     */
    private fun saveImageToLocal(
        context: Context,
        bitmap: Bitmap,
        bookName: String,
        photoId: String
    ): String {
        val coverDir = File(context.filesDir, COVER_IMAGES_DIR)
        if (!coverDir.exists()) {
            coverDir.mkdirs()
        }
        
        val fileName = "${bookName.replace(Regex("[^a-zA-Z0-9\u4e00-\u9fa5]"), "_")}_$photoId.jpg"
        val file = File(coverDir, fileName)
        
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Log.d(TAG, "图片保存成功: ${file.absolutePath}")
            return file.absolutePath
        } catch (e: IOException) {
            Log.e(TAG, "保存图片失败", e)
            throw e
        }
    }
    
    /**
     * 构建搜索查询
     */
    private fun buildSearchQuery(bookTitle: String, author: String?): String {
        val cleanTitle = bookTitle.replace(Regex("[《》【】()（）]"), "").trim()
        val query = if (author != null && author.isNotBlank()) {
            "$cleanTitle $author book cover"
        } else {
            "$cleanTitle book cover"
        }
        return query
    }
    
    /**
     * 获取随机封面图片
     */
    suspend fun getRandomCoverImage(
        bookTitle: String,
        author: String? = null
    ): UnsplashPhoto? = withContext(Dispatchers.IO) {
        try {
            val query = buildSearchQuery(bookTitle, author)
            val photos = unsplashService.getRandomPhoto(query, "portrait")
            photos.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "获取随机封面图片失败", e)
            null
        }
    }
    
    /**
     * 清理本地封面图片
     */
    fun clearLocalCovers(context: Context) {
        try {
            val coverDir = File(context.filesDir, COVER_IMAGES_DIR)
            if (coverDir.exists()) {
                coverDir.deleteRecursively()
                Log.d(TAG, "本地封面图片已清理")
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理本地封面图片失败", e)
        }
    }
}
