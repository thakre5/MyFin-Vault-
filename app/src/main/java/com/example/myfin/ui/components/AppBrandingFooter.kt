package com.example.myfin.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.BuildConfig
import com.example.myfin.ui.theme.MyfinTheme
import com.example.myfin.ui.theme.Neutral400
import com.example.myfin.ui.theme.Primary300
import com.example.myfin.ui.theme.Primary600
import com.example.myfin.ui.theme.TextDark
import com.example.myfin.ui.theme.TextMuted

@Composable
fun AppBrandingFooter(
    modifier: Modifier = Modifier,
    version: String = "v${BuildConfig.VERSION_NAME}",
    showIcon: Boolean = true,
    iconSize: Dp = 26.dp,
    isDarkTheme: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val primaryTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.90f) else TextDark
    val secondaryTextColor = if (isDarkTheme) Neutral400 else TextMuted
    val logoTint = if (isDarkTheme) Primary300 else Primary600

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showIcon) {
            MyFinAppLogo(
                size = iconSize,
                tint = logoTint
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = "MyFin Vault $version",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = primaryTextColor,
            letterSpacing = 0.2.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "100% Offline Local SQLite Storage • Zero Cloud Telemetry",
            fontSize = 10.sp,
            color = secondaryTextColor,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Footer - Light Canvas", showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
private fun PreviewAppBrandingFooterLight() {
    MyfinTheme {
        AppBrandingFooter(
            version = "v1.0.0",
            isDarkTheme = false
        )
    }
}

@Preview(name = "Footer - Dark Canvas", showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun PreviewAppBrandingFooterDark() {
    MyfinTheme {
        AppBrandingFooter(
            version = "v1.0.0",
            isDarkTheme = true
        )
    }
}
