package com.ibylin.app.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PexelsService {
    
    @GET("search")
    suspend fun searchPhotos(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("orientation") orientation: String = "portrait"
    ): PexelsSearchResponse
    
    @GET("curated")
    suspend fun getCuratedPhotos(
        @Header("Authorization") apiKey: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): PexelsSearchResponse
}

data class PexelsSearchResponse(
    val total_results: Int,
    val page: Int,
    val per_page: Int,
    val photos: List<PexelsPhoto>,
    val next_page: String?
)

data class PexelsPhoto(
    val id: Int,
    val width: Int,
    val height: Int,
    val url: String,
    val photographer: String,
    val photographer_url: String,
    val photographer_id: Long,  // 改为Long类型，支持更大的数值
    val avg_color: String,
    val src: PexelsPhotoSrc,
    val liked: Boolean,
    val alt: String
)

data class PexelsPhotoSrc(
    val original: String,
    val large2x: String,
    val large: String,
    val medium: String,
    val small: String,
    val portrait: String,
    val landscape: String,
    val tiny: String
)
