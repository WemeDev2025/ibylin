package com.ibylin.app.api

import retrofit2.http.GET
import retrofit2.http.Query

interface UnsplashService {
    
    @GET("search/photos")
    suspend fun searchPhotos(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("orientation") orientation: String = "portrait"
    ): UnsplashSearchResponse
    
    @GET("photos/random")
    suspend fun getRandomPhoto(
        @Query("query") query: String,
        @Query("orientation") orientation: String = "portrait"
    ): List<UnsplashPhoto>
}

data class UnsplashSearchResponse(
    val total: Int,
    val total_pages: Int,
    val results: List<UnsplashPhoto>
)

data class UnsplashPhoto(
    val id: String,
    val created_at: String,
    val updated_at: String,
    val promoted_at: String?,
    val width: Int,
    val height: Int,
    val color: String,
    val blur_hash: String?,
    val description: String?,
    val alt_description: String?,
    val urls: UnsplashUrls,
    val links: UnsplashLinks,
    val likes: Int,
    val liked_by_user: Boolean,
    val user: UnsplashUser
)

data class UnsplashUrls(
    val raw: String,
    val full: String,
    val regular: String,
    val small: String,
    val thumb: String
)

data class UnsplashLinks(
    val self: String,
    val html: String,
    val download: String,
    val download_location: String
)

data class UnsplashUser(
    val id: String,
    val updated_at: String,
    val username: String,
    val name: String,
    val first_name: String,
    val last_name: String?,
    val twitter_username: String?,
    val portfolio_url: String?,
    val bio: String?,
    val location: String?,
    val links: UnsplashUserLinks,
    val profile_image: UnsplashProfileImage,
    val instagram_username: String?,
    val total_collections: Int,
    val total_likes: Int,
    val total_photos: Int,
    val accepted_tos: Boolean,
    val for_hire: Boolean,
    val social: UnsplashSocial
)

data class UnsplashUserLinks(
    val self: String,
    val html: String,
    val photos: String,
    val likes: String,
    val portfolio: String,
    val following: String,
    val followers: String
)

data class UnsplashProfileImage(
    val small: String,
    val medium: String,
    val large: String
)

data class UnsplashSocial(
    val instagram_username: String?,
    val portfolio_url: String?,
    val twitter_username: String?,
    val paypal_email: String?
)
