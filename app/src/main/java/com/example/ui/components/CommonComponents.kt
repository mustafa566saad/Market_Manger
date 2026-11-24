package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConnectionStatus
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BlueInfo
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.ProfitGreen
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }
    return "${formatter.format(amount)} ج.م"
}

fun formatDate(date: Date, pattern: String = "yyyy/MM/dd"): String {
    val sdf = SimpleDateFormat(pattern, Locale("ar"))
    return sdf.format(date)
}

fun formatDate(date: java.time.LocalDate, pattern: String = "yyyy/MM/dd"): String {
    val d = Date.from(date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
    val sdf = SimpleDateFormat(pattern, Locale("ar"))
    return sdf.format(d)
}

fun formatDate(dateTime: java.time.LocalDateTime, pattern: String = "yyyy/MM/dd"): String {
    val d = Date.from(dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant())
    val sdf = SimpleDateFormat(pattern, Locale("ar"))
    return sdf.format(d)
}

fun formatDateTime(date: Date): String {
    val sdf = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar"))
    return sdf.format(date)
}

fun formatDateTime(dateTime: java.time.LocalDateTime): String {
    val d = Date.from(dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant())
    val sdf = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar"))
    return sdf.format(d)
}

fun formatTime(date: Date): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale("ar"))
    return sdf.format(date)
}

fun formatTime(dateTime: java.time.LocalDateTime): String {
    val d = Date.from(dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant())
    val sdf = SimpleDateFormat("hh:mm a", Locale("ar"))
    return sdf.format(d)
}

@Composable
fun ConnectionBadge(
    status: ConnectionStatus,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (color, icon, text) = when (status) {
        ConnectionStatus.CONNECTED -> Triple(ProfitGreen, Icons.Default.CloudDone, "متصل بالسيرفر")
        ConnectionStatus.CONNECTING -> Triple(BlueInfo, Icons.Default.CloudSync, "جاري الاتصال...")
        ConnectionStatus.RETRYING -> Triple(AmberWarning, Icons.Default.CloudSync, "إعادة المحاولة...")
        ConnectionStatus.DISCONNECTED -> Triple(Color.Gray, Icons.Default.CloudOff, "غير متصل")
        ConnectionStatus.ERROR -> Triple(ExpenseRed, Icons.Default.Warning, "فشل الاتصال - انقر للإعادة")
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = status == ConnectionStatus.ERROR || status == ConnectionStatus.DISCONNECTED) { onRetry() }
            .testTag("connection_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionButton: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (actionButton != null) {
            actionButton()
        }
    }
}
