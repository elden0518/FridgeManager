package com.example.fridgemanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green80  = Color(0xFF4CAF50)
private val Green40  = Color(0xFF2E7D32)
private val Orange80 = Color(0xFFFF9800)
private val Red80    = Color(0xFFF44336)

private val LightColorScheme = lightColorScheme(
    primary         = Green40,
    onPrimary       = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    secondary       = Orange80,
    error           = Red80,
    background      = Color(0xFFF8FBF8),
    surface         = Color.White,
)

@Composable
fun FridgeManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}

// 到期状态色
val ExpiryColors = mapOf(
    "expired"  to Color(0xFFF44336),  // 红：已过期
    "critical" to Color(0xFFFF9800),  // 橙：3天内
    "warning"  to Color(0xFFFFEB3B),  // 黄：7天内
    "safe"     to Color(0xFF4CAF50)   // 绿：充足
)

fun expiryColor(daysLeft: Long?): Color = when {
    daysLeft == null       -> Color(0xFF9E9E9E)
    daysLeft < 0           -> ExpiryColors["expired"]!!
    daysLeft <= 3          -> ExpiryColors["critical"]!!
    daysLeft <= 7          -> ExpiryColors["warning"]!!
    else                   -> ExpiryColors["safe"]!!
}
