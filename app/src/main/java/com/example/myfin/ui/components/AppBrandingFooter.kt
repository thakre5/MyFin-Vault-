package com.example.myfin.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.BuildConfig
import com.example.myfin.ui.theme.TextDark
import com.example.myfin.ui.theme.TextMuted

@Composable
fun AppBrandingFooter(
    modifier: Modifier = Modifier,
    version: String = "v${BuildConfig.VERSION_NAME}",
    showIcon: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showIcon) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF1C1D21),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "MyFin Vault Security Logo",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = "MyFin Vault $version",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "100% Offline Local SQLite Storage • Zero Cloud Telemetry",
            fontSize = 10.5.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}
