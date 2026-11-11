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
import com.example.myfin.ui.theme.*

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
        colors = if (isDarkTheme) {
            listOf(Primary300, Primary500)
        } else {
            listOf(Primary600, Primary800)
        }
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
        MyFinAppLogo(
            size = logoSize,
            tint = if (isDarkTheme) Primary300 else Primary600
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
                        color = if (isDarkTheme) Primary600.copy(alpha = 0.25f) else Primary50,
                        border = BorderStroke(
                            0.5.dp,
                            if (isDarkTheme) Primary400.copy(alpha = 0.5f) else Primary200
                        )
                    ) {
                        Text(
                            text = "VAULT",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDarkTheme) Primary200 else Primary700,
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
                    color = if (isDarkTheme) Neutral400 else TextMuted,
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Brand Header - Light Canvas", showBackground = true, backgroundColor = 0xFFF8FAFC)
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

@Preview(name = "Brand Header - Dark Canvas", showBackground = true, backgroundColor = 0xFF0F172A)
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
