package com.streamvault.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ConnectionType { REST, WEBSOCKET }
enum class VideoFormat { HLS, DASH, SMOOTH_STREAMING, MP4, MKV, RTSP, RTMP, PROGRESSIVE }
enum class DrmScheme { WIDEVINE, PLAYREADY, CLEARKEY, NONE }
enum class ContentType { MOVIE, SERIES, EPISODE }
enum class ProfileType { ADULT, KID }

data class ServerConfig(
    val baseUrl: String,
    val connectionType: ConnectionType,
    val websocketUrl: String?,
    val apiKey: String?,
    val authHeader: String?,
    val drmLicenseUrl: String?,
    val drmHeaders: Map<String, String>
)

data class UserProfile(
    val id: String,
    val name: String,
    val avatarUrl: String?,
    val type: ProfileType,
    val pin: String?,
    val maturityLevel: Int
)

data class MediaItem(
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
    val genres: List<String>,
    val cast: List<String>,
    val director: String?,
    val type: ContentType,
    val seasons: List<Season>?,
    val streamUrl: String?,
    val videoFormat: VideoFormat,
    val drmScheme: DrmScheme,
    val drmLicenseUrl: String?,
    val drmHeaders: Map<String, String>,
    val subtitles: List<Subtitle>,
    val audioTracks: List<AudioTrack>,
    val trailerUrl: String?,
    val isFeatured: Boolean,
    val isTopTen: Boolean,
    val rank: Int?,
    val maturityRating: String?,
    val tags: List<String>
)

data class Season(
    val number: Int,
    val name: String?,
    val episodes: List<Episode>
)

data class Episode(
    val id: String,
    val number: Int,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val duration: Int?,
    val streamUrl: String,
    val videoFormat: VideoFormat,
    val drmScheme: DrmScheme,
    val drmLicenseUrl: String?,
    val drmHeaders: Map<String, String>,
    val subtitles: List<Subtitle>
)

data class Subtitle(
    val language: String,
    val label: String,
    val url: String,
    val mimeType: String
)

data class AudioTrack(
    val language: String,
    val label: String,
    val channelCount: Int?
)

data class HomeRow(
    val id: String,
    val title: String,
    val items: List<MediaItem>,
    val displayType: RowDisplayType
)

enum class RowDisplayType { PORTRAIT, LANDSCAPE, HERO, NUMBERED }

data class SearchResult(
    val query: String,
    val items: List<MediaItem>
)

data class AuthRequest(
    val endpoint: String,
    val body: Map<String, String>,
    val method: String
)

data class AuthResponse(
    val token: String,
    val refreshToken: String?,
    val expiresAt: Long?,
    val profile: UserProfile?
)

@Entity(tableName = "watch_progress")
data class WatchProgress(
    @PrimaryKey val contentId: String,
    val profileId: String,
    val positionMs: Long,
    val durationMs: Long,
    val seasonNumber: Int?,
    val episodeId: String?,
    val updatedAt: Long
)

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val contentId: String,
    val profileId: String,
    val addedAt: Long
)

data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val imageUrl: String?,
    val timestamp: Long,
    val isRead: Boolean
)

data class PlayerState(
    val contentId: String,
    val title: String,
    val streamUrl: String,
    val videoFormat: VideoFormat,
    val drmScheme: DrmScheme,
    val drmLicenseUrl: String?,
    val drmHeaders: Map<String, String>,
    val subtitles: List<Subtitle>,
    val startPositionMs: Long,
    val screenshotProtection: Boolean
)
