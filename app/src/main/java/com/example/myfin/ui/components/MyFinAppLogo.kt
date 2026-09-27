package com.example.myfin.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myfin.R
import com.example.myfin.ui.theme.AccentPurple
import com.example.myfin.ui.theme.AccentPurpleDark
import com.example.myfin.ui.theme.MyfinTheme

@Composable
fun MyFinAppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showBackgroundContainer: Boolean = true,
    containerCornerRadius: Dp = size * 0.28f,
    elevation: Dp = 8.dp,
    border: BorderStroke? = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
    gradientColors: List<Color> = listOf(
        Color(0xFF2D1B69),
        AccentPurpleDark,
        AccentPurple
    ),
    @DrawableRes iconResId: Int = R.drawable.ic_launcher_foreground,
    fallbackVector: ImageVector = Icons.Default.Shield,
    contentDescription: String? = "MyFin Vault Shield Logo",
    // 0.95f accounts for built-in 18/108dp safe-zone padding in adaptive launcher icons
    iconScale: Float = if (showBackgroundContainer) 0.95f else 1.0f
) {
    val cornerShape = RoundedCornerShape(containerCornerRadius)
    val isPreview = LocalInspectionMode.current

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showBackgroundContainer) {
                    Modifier
                        .shadow(
                            elevation = elevation,
                            shape = cornerShape,
                            ambientColor = Color.Black.copy(alpha = 0.20f),
                            spotColor = AccentPurple.copy(alpha = 0.45f)
                        )
                        .clip(cornerShape)
                        .then(if (border != null) Modifier.border(border, cornerShape) else Modifier)
                        .background(Brush.verticalGradient(gradientColors))
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isPreview) {
            try {
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(iconScale)
                )
            } catch (_: Exception) {
                Icon(
                    imageVector = fallbackVector,
                    contentDescription = contentDescription,
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize(0.55f)
                )
            }
        } else {
            // IDE Preview safe rendering
            Icon(
                imageVector = fallbackVector,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.fillMaxSize(0.55f)
            )
        }
    }
}

@Preview(name = "Logo - Container Light", showBackground = true, backgroundColor = 0xFFF7F8FA)
@Composable
private fun PreviewMyFinAppLogoLight() {
    MyfinTheme {
        Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            MyFinAppLogo(size = 64.dp)
        }
    }
}

@Preview(name = "Logo - Drawer Dark Canvas", showBackground = true, backgroundColor = 0xFF231B38)
@Composable
private fun PreviewMyFinAppLogoDark() {
    MyfinTheme {
        Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            MyFinAppLogo(size = 64.dp)
        }
    }
}

@Preview(name = "Logo - Bare Graphic")
@Composable
private fun PreviewMyFinAppLogoBare() {
    MyfinTheme {
        MyFinAppLogo(
            size = 48.dp,
            showBackgroundContainer = false
        )
    }
}
