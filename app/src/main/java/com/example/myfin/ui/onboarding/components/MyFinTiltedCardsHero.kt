package com.example.myfin.ui.onboarding.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.components.MyFinAppLogo
import com.example.myfin.ui.theme.*

@Composable
fun SolnexTiltedCardsHero(
    currencySymbol: String,
    modifier: Modifier = Modifier,
    primaryCardLabel: String = "Total Net Balance",
    primaryBalance: String = "84,250.00",
    secondaryCardLabel: String = "Fortress Reserve",
    secondaryBalance: String = "25,000.00"
) {
    MyFinTiltedCardsHero(
        currencySymbol = currencySymbol,
        modifier = modifier,
        primaryCardLabel = primaryCardLabel,
        primaryBalance = primaryBalance,
        secondaryCardLabel = secondaryCardLabel,
        secondaryBalance = secondaryBalance
    )
}

@Composable
fun MyFinTiltedCardsHero(
    currencySymbol: String,
    modifier: Modifier = Modifier,
    primaryCardLabel: String = "Total Net Balance",
    primaryBalance: String = "84,250.00",
    secondaryCardLabel: String = "Fortress Reserve",
    secondaryBalance: String = "25,000.00"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(235.dp),
        contentAlignment = Alignment.Center
    ) {
        // Back Card (Deep Slate-Navy Reserve Card)
        Box(
            modifier = Modifier
                .offset(x = (-32).dp, y = (-10).dp)
                .graphicsLayer {
                    rotationZ = -22f
                }
                .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(20.dp))
                .width(225.dp)
                .height(140.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Neutral800,
                            Color(0xFF132347),
                            Neutral900
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Text(
                text = "✦",
                color = Primary300.copy(alpha = 0.90f),
                fontSize = 14.sp
            )
            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = secondaryCardLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Neutral400
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "$currencySymbol $secondaryBalance",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary200
                )
            }
        }

        // Front Card (Royal Cobalt Blue Hero Card)
        Box(
            modifier = Modifier
                .offset(x = 14.dp, y = 16.dp)
                .graphicsLayer {
                    rotationZ = -11f
                }
                .shadow(24.dp, RoundedCornerShape(22.dp), spotColor = Primary600.copy(alpha = 0.45f))
                .clip(RoundedCornerShape(22.dp))
                .width(245.dp)
                .height(152.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Primary800,
                            Primary600,
                            Primary500,
                            Primary400
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
                    text = primaryCardLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Primary100
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$currencySymbol $primaryBalance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-0.4).sp
                )
            }
        }

        // Top Shield Badge
        Box(
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-14).dp, y = (-40).dp)
                .shadow(12.dp, CircleShape, spotColor = Primary600.copy(alpha = 0.5f))
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Primary900, Neutral900)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 1.8.dp.toPx()
                drawCircle(
                    color = Primary400.copy(alpha = 0.65f),
                    radius = size.minDimension * 0.45f,
                    style = Stroke(width = strokePx)
                )
            }
            MyFinAppLogo(
                size = 28.dp,
                tint = Color.White
            )
        }

        // Bottom Currency Badge
        Box(
            modifier = Modifier
                .size(68.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-46).dp, y = 8.dp)
                .shadow(16.dp, CircleShape, spotColor = Color.Black.copy(alpha = 0.4f))
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Neutral800, Neutral900)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 2.dp.toPx()
                drawCircle(
                    color = Primary300.copy(alpha = 0.55f),
                    radius = size.minDimension * 0.44f,
                    style = Stroke(width = strokePx)
                )
            }
            Text(
                text = currencySymbol,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Primary200
            )
        }
    }
}
