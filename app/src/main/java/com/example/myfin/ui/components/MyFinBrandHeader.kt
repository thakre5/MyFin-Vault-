package com.example.myfin.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.theme.TextDark
import com.example.myfin.ui.theme.TextMuted

@Composable
fun MyFinBrandHeader(
    modifier: Modifier = Modifier,
    logoSize: Dp = 42.dp,
    showVaultBadge: Boolean = true,
    isDarkTheme: Boolean = false,
    subtitle: String = "3-TIER WEALTH ARCHITECTURE"
) {
    val primaryTextColor = if (isDarkTheme) Color.White else TextDark
    val brandGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF00D2EE), // Operating Cyan
            Color(0xFF8B5CF6), // Commitments Purple
            Color(0xFF10B981)  // Fortress Teal
        )
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MyFinAppLogo(
            size = logoSize,
            showBackgroundContainer = true,
            elevation = if (isDarkTheme) 0.dp else 4.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "My",
                    fontSize = (logoSize.value * 0.48f).sp,
                    fontWeight = FontWeight.Black,
                    color = primaryTextColor,
                    letterSpacing = (-0.5).sp
                )

                Text(
                    text = "Fin",
                    fontSize = (logoSize.value * 0.48f).sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(brush = brandGradient),
                    letterSpacing = (-0.5).sp
                )

                if (showVaultBadge) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VAULT",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDarkTheme) Color(0xFF00D2EE) else Color(0xFF6C5CE7),
                        letterSpacing = 1.sp
                    )
                }
            }

            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.50f) else TextMuted,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}
