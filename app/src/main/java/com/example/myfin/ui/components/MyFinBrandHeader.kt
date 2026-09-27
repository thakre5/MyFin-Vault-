package com.example.myfin.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.theme.AccentPurple
import com.example.myfin.ui.theme.AccentPurpleDark
import com.example.myfin.ui.theme.AccentPurpleLight
import com.example.myfin.ui.theme.MyfinTheme
import com.example.myfin.ui.theme.TextDark
import com.example.myfin.ui.theme.TextMuted

@Composable
fun MyFinBrandHeader(
    modifier: Modifier = Modifier,
    logoSize: Dp = 38.dp,
    showVaultBadge: Boolean = true,
    isDarkTheme: Boolean = false,
    subtitle: String = "3-TIER WEALTH ARCHITECTURE",
    onClick: (() -> Unit)? = null
) {
    val primaryTextColor = if (isDarkTheme) Color.White else TextDark
    val brandGradient = Brush.horizontalGradient(
        colors = listOf(
            AccentPurple,
            AccentPurpleDark
        )
    )

    val scaledTitleSize = (logoSize.value * 0.52f).coerceIn(15f, 26f).sp

    Row(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Outline-style App Logo
        MyFinAppLogo(
            size = logoSize,
            tint = if (isDarkTheme) Color.White else AccentPurple
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(verticalArrangement = Arrangement.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = "My",
                    fontSize = scaledTitleSize,
                    lineHeight = scaledTitleSize,
                    fontWeight = FontWeight.Black,
                    color = primaryTextColor,
                    letterSpacing = (-0.5).sp
                )

                Text(
                    text = "Fin",
                    fontSize = scaledTitleSize,
                    lineHeight = scaledTitleSize,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(brush = brandGradient),
                    letterSpacing = (-0.5).sp
                )

                if (showVaultBadge) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDarkTheme) AccentPurple.copy(alpha = 0.25f) else AccentPurpleLight,
                        border = BorderStroke(
                            0.5.dp,
                            if (isDarkTheme) AccentPurple.copy(alpha = 0.5f) else AccentPurple.copy(alpha = 0.35f)
                        )
                    ) {
                        Text(
                            text = "VAULT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDarkTheme) Color(0xFFD0BCFF) else AccentPurple,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.5.dp)
                        )
                    }
                }
            }

            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle.uppercase(),
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.55f) else TextMuted,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Brand Header - Light Canvas", showBackground = true, backgroundColor = 0xFFF7F8FA)
@Composable
private fun PreviewBrandHeaderLight() {
    MyfinTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            MyFinBrandHeader(
                logoSize = 40.dp,
                isDarkTheme = false
            )
        }
    }
}

@Preview(name = "Brand Header - Dark Canvas", showBackground = true, backgroundColor = 0xFF231B38)
@Composable
private fun PreviewBrandHeaderDark() {
    MyfinTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            MyFinBrandHeader(
                logoSize = 40.dp,
                isDarkTheme = true
            )
        }
    }
}
