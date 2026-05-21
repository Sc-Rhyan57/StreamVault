package com.streamvault.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.streamvault.data.models.*
import com.streamvault.ui.theme.StreamColors

@Composable
fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showRank: Boolean = false
) {
    val scale = remember { Animatable(1f) }
    Box(
        modifier = modifier
            .scale(scale.value)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.posterUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (showRank && item.rank != null) {
            RankOverlay(rank = item.rank, modifier = Modifier.align(Alignment.BottomStart))
        }
        if (item.isTopTen) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .background(StreamColors.Primary, RoundedCornerShape(2.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("TOP 10", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, StreamColors.Background.copy(0.85f)))
                )
        )
    }
}

@Composable
fun LandscapeCard(item: MediaItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.backdropUrl ?: item.posterUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, StreamColors.Background.copy(0.7f)),
                        startY = 0.5f
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(item.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun MediaRowSection(row: HomeRow, onItemClick: (MediaItem) -> Unit) {
    Column {
        Text(
            row.title,
            color = StreamColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        when (row.displayType) {
            RowDisplayType.PORTRAIT  -> PortraitRow(row.items, onItemClick)
            RowDisplayType.LANDSCAPE -> LandscapeRow(row.items, onItemClick)
            RowDisplayType.NUMBERED  -> NumberedRow(row.items, onItemClick)
            RowDisplayType.HERO      -> HeroRow(row.items, onItemClick)
        }
    }
}

@Composable
fun PortraitRow(items: List<MediaItem>, onItemClick: (MediaItem) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            MediaCard(
                item     = item,
                onClick  = { onItemClick(item) },
                modifier = Modifier.width(110.dp).height(165.dp)
            )
        }
    }
}

@Composable
fun LandscapeRow(items: List<MediaItem>, onItemClick: (MediaItem) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            LandscapeCard(
                item     = item,
                onClick  = { onItemClick(item) },
                modifier = Modifier.width(200.dp).height(112.dp)
            )
        }
    }
}

@Composable
fun NumberedRow(items: List<MediaItem>, onItemClick: (MediaItem) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(items) { idx, item ->
            Box(Modifier.width(130.dp).height(180.dp)) {
                Text(
                    text = (idx + 1).toString(),
                    fontSize = 90.sp,
                    fontWeight = FontWeight.Black,
                    color = StreamColors.SurfaceVar,
                    modifier = Modifier.align(Alignment.BottomStart).offset(x = (-8).dp)
                )
                MediaCard(
                    item     = item,
                    onClick  = { onItemClick(item) },
                    modifier = Modifier
                        .width(90.dp)
                        .height(135.dp)
                        .align(Alignment.BottomEnd)
                )
            }
        }
    }
}

@Composable
fun HeroRow(items: List<MediaItem>, onItemClick: (MediaItem) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            LandscapeCard(
                item     = item,
                onClick  = { onItemClick(item) },
                modifier = Modifier.width(300.dp).height(170.dp)
            )
        }
    }
}

@Composable
fun RankOverlay(rank: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .background(StreamColors.Primary, RoundedCornerShape(2.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text("#$rank", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
}

@Composable
fun HeroBanner(item: MediaItem, onPlay: () -> Unit, onInfo: () -> Unit, onMyList: () -> Unit, isInWatchlist: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(500.dp)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.backdropUrl ?: item.posterUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors   = listOf(Color.Transparent, StreamColors.Background),
                        startY   = 200f
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors   = listOf(StreamColors.Background.copy(0.4f), Color.Transparent)
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
        ) {
            if (item.logoUrl != null) {
                AsyncImage(
                    model = item.logoUrl,
                    contentDescription = item.title,
                    modifier = Modifier.width(200.dp).height(80.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(item.title, style = MaterialTheme.typography.displayMedium, color = Color.White)
            }
            Spacer(Modifier.height(8.dp))
            if (item.isTopTen) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .background(StreamColors.Primary, RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("TOP 10", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Filmes hoje", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape  = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reproduzir", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Button(
                    onClick = onInfo,
                    colors = ButtonDefaults.buttonColors(containerColor = StreamColors.SurfaceVar.copy(0.85f), contentColor = Color.White),
                    shape  = RoundedCornerShape(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Info, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Mais info", fontWeight = FontWeight.Medium, fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    item: MediaItem,
    progress: com.streamvault.data.models.WatchProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val percent = if (progress.durationMs > 0) (progress.positionMs.toFloat() / progress.durationMs).coerceIn(0f, 1f) else 0f
    Column(modifier = modifier.width(200.dp)) {
        Box(
            Modifier
                .width(200.dp)
                .height(112.dp)
                .clip(RoundedCornerShape(4.dp))
                .clickable(onClick = onClick)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.backdropUrl ?: item.posterUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(44.dp)
                    .background(StreamColors.Background.copy(0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(StreamColors.Divider)
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(percent)
                        .background(StreamColors.Primary)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(item.title, color = StreamColors.TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
fun StreamVaultTopBar(
    profileAvatarUrl: String?,
    profileName: String?,
    notificationCount: Int,
    currentRoute: String,
    onRouteChange: (String) -> Unit,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(StreamColors.Background, Color.Transparent)
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                val tabs = listOf("Início" to "home", "Séries" to "series", "Filmes" to "movies", "Jogos" to "games", "Novidades" to "new")
                tabs.forEach { (label, route) ->
                    Text(
                        label,
                        color = if (currentRoute == route) Color.White else StreamColors.TextSecondary,
                        fontSize = if (currentRoute == route) 14.sp else 13.sp,
                        fontWeight = if (currentRoute == route) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.clickable { onRouteChange(route) }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                BadgedBox(
                    badge = {
                        if (notificationCount > 0) Badge { Text(notificationCount.toString()) }
                    }
                ) {
                    Icon(
                        Icons.Outlined.Notifications,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp).clickable(onClick = onNotificationsClick)
                    )
                }
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(StreamColors.Primary)
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileAvatarUrl != null) {
                        AsyncImage(model = profileAvatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Text(
                            profileName?.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(currentRoute: String, onRouteChange: (String) -> Unit) {
    NavigationBar(containerColor = StreamColors.Surface, contentColor = Color.White, tonalElevation = 0.dp) {
        NavigationBarItem(
            selected = currentRoute == "home",
            onClick  = { onRouteChange("home") },
            icon     = { Icon(if (currentRoute == "home") Icons.Filled.Home else Icons.Outlined.Home, null) },
            label    = { Text("Início") },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = Color.White,
                unselectedIconColor = StreamColors.TextMuted,
                selectedTextColor   = Color.White,
                unselectedTextColor = StreamColors.TextMuted,
                indicatorColor      = StreamColors.SurfaceVar
            )
        )
        NavigationBarItem(
            selected = currentRoute == "search",
            onClick  = { onRouteChange("search") },
            icon     = { Icon(if (currentRoute == "search") Icons.Filled.Search else Icons.Outlined.Search, null) },
            label    = { Text("Buscar") },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = Color.White,
                unselectedIconColor = StreamColors.TextMuted,
                selectedTextColor   = Color.White,
                unselectedTextColor = StreamColors.TextMuted,
                indicatorColor      = StreamColors.SurfaceVar
            )
        )
        NavigationBarItem(
            selected = currentRoute == "mynetflix",
            onClick  = { onRouteChange("mynetflix") },
            icon     = {
                Icon(
                    if (currentRoute == "mynetflix") Icons.Filled.Person else Icons.Outlined.Person,
                    null,
                    modifier = Modifier.size(24.dp)
                )
            },
            label    = { Text("Minha área") },
            colors   = NavigationBarItemDefaults.colors(
                selectedIconColor   = Color.White,
                unselectedIconColor = StreamColors.TextMuted,
                selectedTextColor   = Color.White,
                unselectedTextColor = StreamColors.TextMuted,
                indicatorColor      = StreamColors.SurfaceVar
            )
        )
    }
}

@Composable
fun LoadingShimmer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "sx"
    )
    Box(
        modifier
            .background(
                Brush.linearGradient(
                    colors      = listOf(StreamColors.SurfaceVar, StreamColors.Card, StreamColors.SurfaceVar),
                    start       = Offset(shimmerX - 400, 0f),
                    end         = Offset(shimmerX, 0f)
                ),
                RoundedCornerShape(4.dp)
            )
    )
}

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.ErrorOutline, null, tint = StreamColors.Primary, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, color = StreamColors.TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = StreamColors.Primary)) {
            Text("Tentar novamente", color = Color.White)
        }
    }
}
