package com.streamvault.data.remote

import com.streamvault.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface StreamApiService {

    @POST
    suspend fun authenticate(
        @Url url: String,
        @Body body: Map<String, String>,
        @HeaderMap headers: Map<String, String> = emptyMap()
    ): Response<Map<String, Any>>

    @GET
    suspend fun getHome(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Response<List<HomeRowDto>>

    @GET
    suspend fun getContent(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Response<MediaItemDto>

    @GET
    suspend fun search(
        @Url url: String,
        @Query("q") query: String,
        @HeaderMap headers: Map<String, String>
    ): Response<List<MediaItemDto>>

    @GET
    suspend fun getProfiles(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Response<List<UserProfileDto>>

    @GET
    suspend fun getCategories(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Response<List<HomeRowDto>>

    @GET
    suspend fun getNotifications(
        @Url url: String,
        @HeaderMap headers: Map<String, String>
    ): Response<List<NotificationDto>>

    @POST
    suspend fun refreshToken(
        @Url url: String,
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>
}

data class HomeRowDto(
    val id: String,
    val title: String,
    val items: List<MediaItemDto>,
    val displayType: String?
)

data class MediaItemDto(
    val id: String,
    val title: String,
    val description: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val logoUrl: String?,
    val year: Int?,
    val rating: String?,
    val imdbRating: Float?,
    val duration: Int?,
    val genres: List<String>?,
    val cast: List<String>?,
    val director: String?,
    val type: String?,
    val seasons: List<SeasonDto>?,
    val streamUrl: String?,
    val videoFormat: String?,
    val drmScheme: String?,
    val drmLicenseUrl: String?,
    val drmHeaders: Map<String, String>?,
    val subtitles: List<SubtitleDto>?,
    val audioTracks: List<AudioTrackDto>?,
    val trailerUrl: String?,
    val isFeatured: Boolean?,
    val isTopTen: Boolean?,
    val rank: Int?,
    val maturityRating: String?,
    val tags: List<String>?
)

data class SeasonDto(
    val number: Int,
    val name: String?,
    val episodes: List<EpisodeDto>
)

data class EpisodeDto(
    val id: String,
    val number: Int,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val duration: Int?,
    val streamUrl: String,
    val videoFormat: String?,
    val drmScheme: String?,
    val drmLicenseUrl: String?,
    val drmHeaders: Map<String, String>?,
    val subtitles: List<SubtitleDto>?
)

data class SubtitleDto(
    val language: String,
    val label: String,
    val url: String,
    val mimeType: String?
)

data class AudioTrackDto(
    val language: String,
    val label: String,
    val channelCount: Int?
)

data class UserProfileDto(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val type: String?,
    val maturityLevel: Int?
)

data class NotificationDto(
    val id: String,
    val title: String,
    val body: String,
    val imageUrl: String?,
    val timestamp: Long,
    val isRead: Boolean?
)
