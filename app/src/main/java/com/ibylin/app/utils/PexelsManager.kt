package com.ibylin.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.ibylin.app.api.PexelsPhoto
import com.ibylin.app.api.PexelsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class PexelsManager private constructor() {
    
    companion object {
        private const val TAG = "PexelsManager"
        // 使用您提供的Pexels API Key
        private const val PEXELS_API_KEY = "pKd11a3Vwbn1jHjDmPTAJcn1bsQyOFH4LcDHEqpBfyBcexiO3opJEgPI"
        private const val PEXELS_BASE_URL = "https://api.pexels.com/v1/"
        private const val COVER_IMAGES_DIR = "book_covers"
        
        @Volatile
        private var INSTANCE: PexelsManager? = null
        
        fun getInstance(): PexelsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PexelsManager().also { INSTANCE = it }
            }
        }
    }
    
    private val pexelsService: PexelsService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        
        Retrofit.Builder()
            .baseUrl(PEXELS_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PexelsService::class.java)
    }
    
    /**
     * 搜索封面图片
     */
    suspend fun searchCoverImages(
        query: String,
        page: Int = 1,
        perPage: Int = 30
    ): List<PexelsPhoto> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始搜索封面图片: query=$query, 页码=$page, 每页=$perPage")
            Log.d(TAG, "使用API Key: ${PEXELS_API_KEY.take(10)}...")
            Log.d(TAG, "API Base URL: $PEXELS_BASE_URL")
            
            // 尝试多种搜索策略来提高结果相关性
            val searchResults = mutableListOf<PexelsPhoto>()
            
            // 策略1：使用原始查询
            val response1 = try {
                pexelsService.searchPhotos(
                    apiKey = PEXELS_API_KEY,
                    query = query,
                    page = page,
                    perPage = perPage,
                    orientation = "portrait"
                )
            } catch (e: Exception) {
                Log.w(TAG, "策略1搜索失败: $query", e)
                null
            }
            
            if (response1 != null && response1.photos.isNotEmpty()) {
                Log.d(TAG, "策略1成功: 找到 ${response1.photos.size} 张图片")
                searchResults.addAll(response1.photos)
            }
            
            // 策略2：如果第一页结果不够相关，尝试更精确的搜索
            if (searchResults.isEmpty() || page == 1) {
                val refinedQuery = refineSearchQuery(query)
                if (refinedQuery != query) {
                    try {
                        val response2 = pexelsService.searchPhotos(
                            apiKey = PEXELS_API_KEY,
                            query = refinedQuery,
                            page = 1,
                            perPage = 20,
                            orientation = "portrait"
                        )
                        
                        if (response2.photos.isNotEmpty()) {
                            Log.d(TAG, "策略2成功: 使用 '$refinedQuery' 找到 ${response2.photos.size} 张图片")
                            // 将策略2的结果添加到前面，提高相关性
                            searchResults.addAll(0, response2.photos)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "策略2搜索失败: $refinedQuery", e)
                    }
                }
            }
            
            Log.d(TAG, "最终搜索结果: 总共 ${searchResults.size} 张图片")
            
            // 记录第一张图片的信息用于调试
            if (searchResults.isNotEmpty()) {
                val firstPhoto = searchResults[0]
                Log.d(TAG, "第一张图片: ID=${firstPhoto.id}, 摄影师=${firstPhoto.photographer}, URL=${firstPhoto.src.medium}")
            }
            
            searchResults
        } catch (e: Exception) {
            Log.e(TAG, "搜索封面图片失败: query=$query, page=$page", e)
            Log.e(TAG, "异常详情: ${e.javaClass.simpleName} - ${e.message}")
            Log.e(TAG, "异常堆栈: ${e.stackTrace.joinToString("\n")}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 优化搜索查询，提高结果相关性
     */
    private fun refineSearchQuery(query: String): String {
        val lowerQuery = query.lowercase()
        
        // 针对特定关键词的优化
        return when {
            lowerQuery.contains("bike") -> "bicycle transportation cover"
            lowerQuery.contains("car") -> "automobile vehicle cover"
            lowerQuery.contains("mountain") -> "mountain landscape cover"
            lowerQuery.contains("ocean") -> "ocean water cover"
            lowerQuery.contains("sci-fi") -> "science fiction cover"
            lowerQuery.contains("fantasy") -> "fantasy magical cover"
            lowerQuery.contains("minimal") -> "minimal design cover"
            else -> query
        }
    }
    
    /**
     * 搜索封面图片（兼容旧版本）
     */
    suspend fun searchCoverImages(
        bookTitle: String,
        author: String? = null,
        page: Int = 1
    ): List<PexelsPhoto> = withContext(Dispatchers.IO) {
        val query = buildSearchQuery(bookTitle, author)
        searchCoverImagesByQuery(query, page, 30)
    }
    
    /**
     * 内部搜索方法，避免递归调用
     */
    private suspend fun searchCoverImagesByQuery(
        query: String,
        page: Int = 1,
        perPage: Int = 30
    ): List<PexelsPhoto> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "搜索封面图片: $query, 页码: $page, 每页: $perPage")
            
            val response = pexelsService.searchPhotos(
                apiKey = PEXELS_API_KEY,
                query = query,
                page = page,
                perPage = perPage,
                orientation = "portrait"
            )
            
            Log.d(TAG, "搜索到 ${response.photos.size} 张图片")
            response.photos
        } catch (e: Exception) {
            Log.e(TAG, "搜索封面图片失败", e)
            emptyList()
        }
    }
    
    /**
     * 获取精选图片
     */
    suspend fun getCuratedPhotos(page: Int = 1): List<PexelsPhoto> = withContext(Dispatchers.IO) {
        try {
            // 使用随机页码，确保每次获取不同的图片
            val randomPage = (1..100).random()
            Log.d(TAG, "获取精选图片，随机页码: $randomPage")
            
            val response = pexelsService.getCuratedPhotos(
                apiKey = PEXELS_API_KEY,
                page = randomPage,
                perPage = 20
            )
            
            Log.d(TAG, "获取到 ${response.photos.size} 张精选图片")
            response.photos
        } catch (e: Exception) {
            Log.e(TAG, "获取精选图片失败", e)
            emptyList()
        }
    }
    
    /**
     * 下载图片
     */
    suspend fun downloadImage(
        context: Context,
        photo: PexelsPhoto,
        bookName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            // 使用portrait尺寸的图片，适合作为封面
            val imageUrl = photo.src.portrait
            Log.d(TAG, "开始下载图片: $imageUrl")
            
            val client = OkHttpClient()
            val request = okhttp3.Request.Builder().url(imageUrl).build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val inputStream: InputStream? = response.body?.byteStream()
                val bitmap = BitmapFactory.decodeStream(inputStream)
                
                if (bitmap != null) {
                    val filePath = saveImageToLocal(context, bitmap, bookName, photo.id.toString())
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
    ): String? {
        return try {
            val fileName = "${bookName}_${photoId}.jpg"
            val dir = File(context.filesDir, COVER_IMAGES_DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            
            val file = File(dir, fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "保存图片失败", e)
            null
        }
    }
    
    /**
     * 构建搜索查询
     */
    private fun buildSearchQuery(bookTitle: String, author: String?): String {
        return if (author != null && author.isNotBlank()) {
            "$bookTitle $author book cover"
        } else {
            "$bookTitle book cover"
        }
    }
    
    /**
     * 清理本地图片缓存
     */
    fun clearImageCache(context: Context) {
        try {
            val dir = File(context.filesDir, COVER_IMAGES_DIR)
            if (dir.exists()) {
                dir.deleteRecursively()
                Log.d(TAG, "图片缓存清理完成")
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理图片缓存失败", e)
        }
    }
}
