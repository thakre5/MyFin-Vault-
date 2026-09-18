package com.example.myfin.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myfin.R

@Composable
fun MyFinAppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showBackgroundContainer: Boolean = true,
    containerCornerRadius: Dp = size * 0.28f,
    elevation: Dp = 6.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showBackgroundContainer) {
                    Modifier
                        .shadow(elevation, RoundedCornerShape(containerCornerRadius))
                        .clip(RoundedCornerShape(containerCornerRadius))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E2034),
                                    Color(0xFF131522),
                                    Color(0xFF0C0D15)
                                )
                            )
                        )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "MyFin Vault Shield Logo",
            modifier = Modifier.fillMaxSize(if (showBackgroundContainer) 0.72f else 1.0f)
        )
    }
}
