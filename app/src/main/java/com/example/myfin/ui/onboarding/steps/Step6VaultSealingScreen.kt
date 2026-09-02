package com.example.myfin.ui.onboarding.steps

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.SubcomposeAsyncImage
import com.example.myfin.ui.components.MyFinBrandHeader
import com.example.myfin.ui.onboarding.CountryCurrencyMapping
import com.example.myfin.ui.onboarding.TealPrimary
import com.example.myfin.ui.onboarding.components.SolnexTiltedCardsHero
import com.example.myfin.ui.theme.*
import java.io.File
import java.util.Locale

@Composable
fun OnboardingStep6VaultSealing(
    displayName: String,
    emailAddress: String,
    profileImageUri: String?,
    country: CountryCurrencyMapping,
    strategy: String,
    totalLiquidity: Double,
    totalCommitments: Double,
    remainingSeconds: Int,
    onSealImmediately: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF3E8FF),
                        Color(0xFFEDE9FE).copy(alpha = 0.65f),
                        Color(0xFFF8FAFC),
                        CanvasLight
                    )
                )
            )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val minScreenHeight = maxHeight

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .defaultMinSize(minHeight = minScreenHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(48.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp),
                    contentAlignment = Alignment.Center
                ) {
                    SolnexTiltedCardsHero(
                        currencySymbol = country.currencySymbol,
                        modifier = Modifier.graphicsLayer {
                            scaleX = 0.54f
                            scaleY = 0.54f
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sealing Offline Vault",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Encrypting local SQLite ledger with Hardware Keystore",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(22.dp),
                        color = AccentPurple.copy(alpha = 0.08f),
                        border = BorderStroke(0.8.dp, AccentPurple.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AccentPurple.copy(alpha = pulseAlpha))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Auto-sealing in ${remainingSeconds}s...",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextDark
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = AccentPurple.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Lock,
                                        contentDescription = null,
                                        tint = AccentPurple,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ENCRYPTING",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentPurple,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(27.dp),
                            color = CardWhite,
                            border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.9f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(32.dp),
                                        shape = CircleShape,
                                        color = CanvasLight
                                    ) {
                                        val imageModel = remember(profileImageUri) {
                                            if (!profileImageUri.isNullOrBlank()) {
                                                File(profileImageUri).takeIf { it.exists() } ?: profileImageUri
                                            } else null
                                        }

                                        Box(contentAlignment = Alignment.Center) {
                                            if (imageModel != null) {
                                                SubcomposeAsyncImage(
                                                    model = imageModel,
                                                    contentDescription = "Profile Picture",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop,
                                                    error = {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Person,
                                                            contentDescription = null,
                                                            tint = AccentPurple,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Outlined.Person,
                                                    contentDescription = null,
                                                    tint = AccentPurple,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text(
                                            text = displayName.ifBlank { "Vault User" },
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (emailAddress.isNotBlank()) emailAddress else "Offline Account",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TealPrimary,
                                            lineHeight = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .height(46.dp)
                                        .width(115.dp),
                                    shape = RoundedCornerShape(23.dp),
                                    color = CanvasLight,
                                    border = BorderStroke(0.8.dp, BorderLight)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(country.flagEmoji, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${country.currencySymbol} ${country.currencyCode}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                    }
                                }
                            }
                        }

                        val is3Tier = strategy == "3-VAULT"
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(27.dp),
                            color = CardWhite,
                            border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.9f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(32.dp),
                                        shape = CircleShape,
                                        color = CanvasLight
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (is3Tier) Icons.Outlined.AccountBalance else Icons.Outlined.AccountBalanceWallet,
                                                contentDescription = null,
                                                tint = AccentPurple,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text(
                                            text = if (is3Tier) "Smart 3-Tier Strategy" else "Simple Unified Vault",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (is3Tier) "Operating • Commitments • Fortress" else "Unified Cash Flow Ledger",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TealPrimary,
                                            lineHeight = 11.sp
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .height(46.dp)
                                        .width(115.dp),
                                    shape = RoundedCornerShape(23.dp),
                                    color = CanvasLight,
                                    border = BorderStroke(0.8.dp, BorderLight)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Active",
                                            tint = TealPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "CONFIGURED",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(27.dp),
                            color = CardWhite,
                            border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.9f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(32.dp),
                                        shape = CircleShape,
                                        color = CanvasLight
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Payments,
                                                contentDescription = null,
                                                tint = AccentPurple,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(verticalArrangement = Arrangement.Center) {
                                        Text(
                                            text = "Opening Capital",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%s %,.0f Initial", country.currencySymbol, totalLiquidity),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TealPrimary,
                                            lineHeight = 11.sp
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .height(46.dp)
                                        .width(115.dp),
                                    shape = RoundedCornerShape(23.dp),
                                    color = CanvasLight,
                                    border = BorderStroke(0.8.dp, BorderLight)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "AutoPay",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextMuted,
                                            lineHeight = 9.sp
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%s %,.0f", country.currencySymbol, totalCommitments),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentPurple,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = AccentPurple.copy(alpha = 0.8f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Note: Air-gapped active. Zero network calls or cloud telemetry leaves this device.",
                            fontSize = 10.sp,
                            color = TextMuted,
                            lineHeight = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSealImmediately()
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.58f)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextDark)
                    ) {
                        Text(
                            text = "Enter Vault Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .zIndex(10f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF3E8FF).copy(alpha = 0.95f),
                            Color(0xFFF3E8FF).copy(alpha = 0.60f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            MyFinBrandHeader(
                logoSize = 34.dp,
                showVaultBadge = true,
                subtitle = "OFFLINE 3-TIER WEALTH LEDGER"
            )
        }
    }
}
