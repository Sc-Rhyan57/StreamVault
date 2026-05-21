package com.streamvault.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.streamvault.data.models.NotificationItem
import com.streamvault.ui.theme.StreamColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(StreamColors.Background)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, null, tint = Color.White)
            }
            Text("Notificações", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Notifications, null, tint = StreamColors.TextMuted, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Sem notificações", color = StreamColors.TextSecondary, fontSize = 15.sp)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(notifications) { notif ->
                    NotificationRow(notif)
                    HorizontalDivider(color = StreamColors.Divider, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notif: NotificationItem) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (!notif.isRead) StreamColors.Surface else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.size(8.dp).background(if (!notif.isRead) StreamColors.Primary else Color.Transparent, CircleShape))
        if (notif.imageUrl != null) {
            AsyncImage(
                model = notif.imageUrl,
                contentDescription = null,
                modifier = Modifier.width(80.dp).height(45.dp).clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Column(Modifier.weight(1f)) {
            Text(notif.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(notif.body, color = StreamColors.TextSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                formatTimestamp(notif.timestamp),
                color = StreamColors.TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

private fun formatTimestamp(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 3_600_000  -> "${diff / 60_000}m atrás"
        diff < 86_400_000 -> "${diff / 3_600_000}h atrás"
        diff < 604_800_000 -> "${diff / 86_400_000} dias atrás"
        else -> SimpleDateFormat("d MMM", Locale("pt", "BR")).format(Date(ts))
    }
}
