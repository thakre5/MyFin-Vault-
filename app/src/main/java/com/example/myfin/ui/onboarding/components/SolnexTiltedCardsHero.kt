package com.example.myfin.ui.onboarding.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.theme.AccentPurple
import com.example.myfin.ui.theme.AccentPurpleDark

@Composable
fun SolnexTiltedCardsHero(
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(235.dp),
        contentAlignment = Alignment.Center
    ) {
        // Back Card (Deep Indigo & Purple Gradient)
        Box(
            modifier = Modifier
                .offset(x = (-32).dp, y = (-10).dp)
                .graphicsLayer {
                    rotationZ = -22f
                }
                .shadow(14.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .width(225.dp)
                .height(140.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF2D1B69),
                            Color(0xFF1E1045),
                            Color(0xFF0F0824)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Text(
                text = "✦",
                color = Color(0xFFC084FC).copy(alpha = 0.85f),
                fontSize = 14.sp
            )
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = "Reserve Vault",
                    fontSize = 9.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "$currencySymbol 12,450.00",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC084FC)
                )
            }
        }

        // Front Card (Primary AccentPurple Gradient)
        Box(
            modifier = Modifier
                .offset(x = 14.dp, y = 16.dp)
                .graphicsLayer {
                    rotationZ = -11f
                }
                .shadow(22.dp, RoundedCornerShape(22.dp))
                .clip(RoundedCornerShape(22.dp))
                .width(245.dp)
                .height(152.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF5A4BD1),
                            AccentPurple,
                            Color(0xFF8B5CF6),
                            Color(0xFFA855F7)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Text(
                text = "✦",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )

            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = "Total Balance",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = "$currencySymbol 84,250.00",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        // Top Vault Badge (Shield Glyph)
        Box(
            modifier = Modifier
                .size(62.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-14).dp, y = (-42).dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF2E1065), Color(0xFF1E1B4B))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 2.dp.toPx()
                drawCircle(
                    color = AccentPurple,
                    radius = size.minDimension * 0.44f,
                    style = Stroke(width = strokePx)
                )
            }
            Text(
                text = "🛡",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }

        // Bottom Vault Currency Badge
        Box(
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-46).dp, y = 6.dp)
                .shadow(14.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1E1B4B), Color(0xFF0F0A1C))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 2.5.dp.toPx()
                drawCircle(
                    color = Color(0xFFC084FC),
                    radius = size.minDimension * 0.44f,
                    style = Stroke(width = strokePx)
                )
            }
            Text(
                text = currencySymbol,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFC084FC)
            )
        }
    }
}
