package com.streamvault.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.streamvault.data.local.AppPreferences
import com.streamvault.data.models.*
import com.streamvault.data.repository.Result
import com.streamvault.data.repository.StreamRepository
import com.streamvault.ui.components.LandscapeCard
import com.streamvault.ui.components.MediaCard
import com.streamvault.ui.components.LoadingShimmer
import com.streamvault.ui.theme.StreamColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: StreamRepository,
    private val preferences: AppPreferences,
    savedState: SavedStateHandle
) : ViewModel() {

    private val contentId = savedState.get<String>("contentId") ?: ""

    private val _item        = MutableStateFlow<MediaItem?>(null)
    private val _isLoading   = MutableStateFlow(true)
    private val _error       = MutableStateFlow<String?>(null)
    private val _inWatchlist = MutableStateFlow(false)
    private val _progress    = MutableStateFlow<WatchProgress?>(null)
    private val _selectedSeason = MutableStateFlow(0)

    val item: StateFlow<MediaItem?>       = _item.asStateFlow()
    val isLoading: StateFlow<Boolean>     = _isLoading.asStateFlow()
    val error: StateFlow<String?>         = _error.asStateFlow()
    val inWatchlist: StateFlow<Boolean>   = _inWatchlist.asStateFlow()
    val progress: StateFlow<WatchProgress?> = _progress.asStateFlow()
    val selectedSeason: StateFlow<Int>    = _selectedSeason.asStateFlow()

    private val profileId get() = _profileId
    private var _profileId = ""

    init {
        viewModelScope.launch {
            preferences.profileId.filterNotNull().collect { _profileId = it }
        }
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            _isLoading.value = true
            when (val r = repository.getContent(contentId)) {
                is Result.Success -> {
                    _item.value = r.data
                    checkWatchlist()
                    loadProgress()
                }
                is Result.Error -> _error.value = r.message
                else -> {}
            }
            _isLoading.value = false
        }
    }

    private fun checkWatchlist() {
        viewModelScope.launch {
            repository.isInWatchlist(contentId, _profileId).collect { _inWatchlist.value = it }
        }
    }

    private fun loadProgress() {
        viewModelScope.launch {
            _progress.value = repository.getProgress(contentId, _profileId)
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            if (_inWatchlist.value) repository.removeFromWatchlist(contentId, _profileId)
            else repository.addToWatchlist(contentId, _profileId)
        }
    }

    fun selectSeason(idx: Int) { _selectedSeason.value = idx }

    fun buildPlayerState(episode: Episode? = null, screenshotProtection: Boolean = true): PlayerState? {
        val mediaItem = _item.value ?: return null
        return if (episode != null) {
            PlayerState(
                contentId           = episode.id,
                title               = "${mediaItem.title} — ${episode.title}",
                streamUrl           = episode.streamUrl,
                videoFormat         = episode.videoFormat,
                drmScheme           = episode.drmScheme,
                drmLicenseUrl       = episode.drmLicenseUrl,
                drmHeaders          = episode.drmHeaders,
                subtitles           = episode.subtitles,
                startPositionMs     = 0L,
                screenshotProtection = screenshotProtection
            )
        } else {
            val url = mediaItem.streamUrl ?: return null
            PlayerState(
                contentId           = mediaItem.id,
                title               = mediaItem.title,
                streamUrl           = url,
                videoFormat         = mediaItem.videoFormat,
                drmScheme           = mediaItem.drmScheme,
                drmLicenseUrl       = mediaItem.drmLicenseUrl,
                drmHeaders          = mediaItem.drmHeaders,
                subtitles           = mediaItem.subtitles,
                startPositionMs     = _progress.value?.positionMs ?: 0L,
                screenshotProtection = screenshotProtection
            )
        }
    }
}

@Composable
fun DetailScreen(
    onPlay: (PlayerState) -> Unit,
    onBack: () -> Unit,
    vm: DetailViewModel = hiltViewModel()
) {
    val item      by vm.item.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error     by vm.error.collectAsState()
    val inWatchlist by vm.inWatchlist.collectAsState()
    val progress  by vm.progress.collectAsState()
    val selectedSeason by vm.selectedSeason.collectAsState()

    Box(Modifier.fillMaxSize().background(StreamColors.Background)) {
        when {
            isLoading -> DetailShimmer()
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error!!, color = StreamColors.TextSecondary)
            }
            item != null -> {
                val media = item!!
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(280.dp)) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(media.backdropUrl ?: media.posterUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(listOf(Color.Transparent, StreamColors.Background))
                                )
                            )
                        }
                    }

                    item {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            if (media.logoUrl != null) {
                                AsyncImage(
                                    model = media.logoUrl,
                                    contentDescription = media.title,
                                    modifier = Modifier.width(180.dp).height(70.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Text(media.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (media.isTopTen) TopTenBadge(media.rank)
                                if (media.imdbRating != null) ImdbBadge(media.imdbRating)
                                media.year?.let { Text(it.toString(), color = StreamColors.TextSecondary, fontSize = 13.sp) }
                                media.duration?.let { Text(formatDuration(it), color = StreamColors.TextSecondary, fontSize = 13.sp) }
                                media.maturityRating?.let {
                                    Box(Modifier.border(1.dp, StreamColors.TextMuted, RoundedCornerShape(2.dp)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                                        Text(it, color = StreamColors.TextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val state = vm.buildPlayerState()
                                    if (state != null) onPlay(state)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors   = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                                shape    = RoundedCornerShape(4.dp),
                                enabled  = media.streamUrl != null
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (progress != null) "Continuar" else "Reproduzir",
                                    fontWeight = FontWeight.Bold, fontSize = 15.sp
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick  = {},
                                modifier = Modifier.fillMaxWidth(),
                                colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border   = BorderStroke(1.dp, StreamColors.TextMuted),
                                shape    = RoundedCornerShape(4.dp)
                            ) {
                                Icon(Icons.Outlined.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Baixar", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            if (!media.description.isNullOrBlank()) {
                                Text(media.description, color = StreamColors.TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                                Spacer(Modifier.height(12.dp))
                            }
                            if (media.cast.isNotEmpty()) {
                                Text("Elenco: ${media.cast.take(3).joinToString(", ")}", color = StreamColors.TextMuted, fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                            }
                            if (!media.director.isNullOrBlank()) {
                                Text("Direção: ${media.director}", color = StreamColors.TextMuted, fontSize = 12.sp)
                                Spacer(Modifier.height(4.dp))
                            }
                            if (media.genres.isNotEmpty()) {
                                Text("Gêneros: ${media.genres.joinToString(", ")}", color = StreamColors.TextMuted, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                ActionButton(
                                    icon  = if (inWatchlist) Icons.Filled.Check else Icons.Outlined.Add,
                                    label = "Minha lista",
                                    onClick = { vm.toggleWatchlist() }
                                )
                                ActionButton(Icons.Outlined.ThumbUp,  "Avaliar",    onClick = {})
                                ActionButton(Icons.Outlined.Share,     "Compartilhar", onClick = {})
                            }
                        }
                    }

                    if (media.type == ContentType.SERIES && !media.seasons.isNullOrEmpty()) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = StreamColors.Divider)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Episódios", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                if (media.seasons!!.size > 1) {
                                    SeasonDropdown(
                                        seasons    = media.seasons,
                                        selected   = selectedSeason,
                                        onSelected = vm::selectSeason
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        val episodes = media.seasons?.getOrNull(selectedSeason)?.episodes ?: emptyList()
                        items(episodes) { ep ->
                            EpisodeRow(
                                episode  = ep,
                                progress = null,
                                onClick  = {
                                    val state = vm.buildPlayerState(episode = ep)
                                    if (state != null) onPlay(state)
                                }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }

        IconButton(
            onClick  = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .background(StreamColors.Background.copy(0.7f), CircleShape)
        ) {
            Icon(Icons.Outlined.ArrowBack, null, tint = Color.White)
        }
    }
}

@Composable
private fun TopTenBadge(rank: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.background(StreamColors.Primary, RoundedCornerShape(2.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
            Text("TOP", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
        rank?.let { Text("#$it em filmes hoje", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun ImdbBadge(rating: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("IMDb", color = StreamColors.GoldRating, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(3.dp))
        Text(String.format("%.1f", rating), color = StreamColors.TextSecondary, fontSize = 12.sp)
    }
}

@Composable
private fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Text(label, color = StreamColors.TextSecondary, fontSize = 11.sp)
    }
}

@Composable
private fun SeasonDropdown(seasons: List<Season>, selected: Int, onSelected: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick  = { expanded = true },
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border   = BorderStroke(1.dp, StreamColors.TextMuted),
            shape    = RoundedCornerShape(4.dp)
        ) {
            Text("Temporada ${seasons[selected].number}", fontSize = 13.sp)
            Icon(Icons.Outlined.KeyboardArrowDown, null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(StreamColors.SurfaceVar)) {
            seasons.forEachIndexed { idx, season ->
                DropdownMenuItem(
                    text    = { Text("Temporada ${season.number}", color = Color.White) },
                    onClick = { onSelected(idx); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, progress: WatchProgress?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.width(140.dp).height(79.dp).clip(RoundedCornerShape(4.dp)).background(StreamColors.SurfaceVar)) {
            if (episode.thumbnailUrl != null) {
                AsyncImage(model = episode.thumbnailUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Box(Modifier.align(Alignment.Center).size(36.dp).background(Color.Black.copy(0.6f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            if (progress != null && progress.durationMs > 0) {
                val pct = (progress.positionMs.toFloat() / progress.durationMs).coerceIn(0f, 1f)
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(StreamColors.Divider)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(pct).background(StreamColors.Primary))
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text("${episode.number}. ${episode.title}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            episode.duration?.let { Text(formatDuration(it), color = StreamColors.TextMuted, fontSize = 12.sp) }
            episode.description?.let { Text(it, color = StreamColors.TextSecondary, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
        }
        Icon(Icons.Outlined.Download, null, tint = StreamColors.TextMuted, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DetailShimmer() {
    Column(Modifier.fillMaxSize()) {
        LoadingShimmer(Modifier.fillMaxWidth().height(280.dp))
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LoadingShimmer(Modifier.width(200.dp).height(24.dp))
            LoadingShimmer(Modifier.width(140.dp).height(16.dp))
            LoadingShimmer(Modifier.fillMaxWidth().height(46.dp))
            LoadingShimmer(Modifier.fillMaxWidth().height(46.dp))
            repeat(3) { LoadingShimmer(Modifier.fillMaxWidth().height(14.dp)) }
        }
    }
}

private fun formatDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
