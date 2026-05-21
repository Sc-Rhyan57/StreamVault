package com.streamvault.data.remote

import com.streamvault.data.models.*

fun MediaItemDto.toDomain(): MediaItem = MediaItem(
    id             = id,
    title          = title,
    description    = description,
    posterUrl      = posterUrl,
    backdropUrl    = backdropUrl,
    logoUrl        = logoUrl,
    year           = year,
    rating         = rating,
    imdbRating     = imdbRating,
    duration       = duration,
    genres         = genres ?: emptyList(),
    cast           = cast ?: emptyList(),
    director       = director,
    type           = runCatching { ContentType.valueOf(type?.uppercase() ?: "MOVIE") }.getOrElse { ContentType.MOVIE },
    seasons        = seasons?.map { it.toDomain() },
    streamUrl      = streamUrl,
    videoFormat    = parseVideoFormat(videoFormat),
    drmScheme      = parseDrmScheme(drmScheme),
    drmLicenseUrl  = drmLicenseUrl,
    drmHeaders     = drmHeaders ?: emptyMap(),
    subtitles      = subtitles?.map { it.toDomain() } ?: emptyList(),
    audioTracks    = audioTracks?.map { it.toDomain() } ?: emptyList(),
    trailerUrl     = trailerUrl,
    isFeatured     = isFeatured ?: false,
    isTopTen       = isTopTen ?: false,
    rank           = rank,
    maturityRating = maturityRating,
    tags           = tags ?: emptyList()
)

fun SeasonDto.toDomain(): Season = Season(
    number   = number,
    name     = name,
    episodes = episodes.map { it.toDomain() }
)

fun EpisodeDto.toDomain(): Episode = Episode(
    id            = id,
    number        = number,
    title         = title,
    description   = description,
    thumbnailUrl  = thumbnailUrl,
    duration      = duration,
    streamUrl     = streamUrl,
    videoFormat   = parseVideoFormat(videoFormat),
    drmScheme     = parseDrmScheme(drmScheme),
    drmLicenseUrl = drmLicenseUrl,
    drmHeaders    = drmHeaders ?: emptyMap(),
    subtitles     = subtitles?.map { it.toDomain() } ?: emptyList()
)

fun SubtitleDto.toDomain(): Subtitle = Subtitle(
    language = language,
    label    = label,
    url      = url,
    mimeType = mimeType ?: "application/x-subrip"
)

fun AudioTrackDto.toDomain(): AudioTrack = AudioTrack(
    language     = language,
    label        = label,
    channelCount = channelCount
)

fun HomeRowDto.toDomain(): HomeRow = HomeRow(
    id          = id,
    title       = title,
    items       = items.map { it.toDomain() },
    displayType = runCatching { RowDisplayType.valueOf(displayType?.uppercase() ?: "PORTRAIT") }.getOrElse { RowDisplayType.PORTRAIT }
)

fun UserProfileDto.toDomain(): UserProfile = UserProfile(
    id            = id,
    name          = name,
    avatarUrl     = avatarUrl,
    type          = runCatching { ProfileType.valueOf(type?.uppercase() ?: "ADULT") }.getOrElse { ProfileType.ADULT },
    pin           = null,
    maturityLevel = maturityLevel ?: 18
)

fun NotificationDto.toDomain(): NotificationItem = NotificationItem(
    id        = id,
    title     = title,
    body      = body,
    imageUrl  = imageUrl,
    timestamp = timestamp,
    isRead    = isRead ?: false
)

private fun parseVideoFormat(raw: String?): VideoFormat = when (raw?.uppercase()) {
    "HLS", "M3U8"  -> VideoFormat.HLS
    "DASH", "MPD"  -> VideoFormat.DASH
    "SMOOTH", "ISM" -> VideoFormat.SMOOTH_STREAMING
    "MKV"          -> VideoFormat.MKV
    "RTSP"         -> VideoFormat.RTSP
    "RTMP"         -> VideoFormat.RTMP
    else           -> VideoFormat.PROGRESSIVE
}

private fun parseDrmScheme(raw: String?): DrmScheme = when (raw?.uppercase()) {
    "WIDEVINE"  -> DrmScheme.WIDEVINE
    "PLAYREADY" -> DrmScheme.PLAYREADY
    "CLEARKEY"  -> DrmScheme.CLEARKEY
    else        -> DrmScheme.NONE
}
