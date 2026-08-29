package com.example.myfin.ui.onboarding.steps

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.onboarding.CoralAccent
import com.example.myfin.ui.onboarding.CountryCurrencyMapping
import com.example.myfin.ui.onboarding.TealPrimary
import com.example.myfin.ui.onboarding.components.HumanDeliberationSceneCanvas
import com.example.myfin.ui.onboarding.components.rememberImageBitmapFromUri
import com.example.myfin.ui.theme.AccentPurple
import com.example.myfin.ui.theme.BorderLight
import com.example.myfin.ui.theme.CardWhite
import com.example.myfin.ui.theme.TextDark
import com.example.myfin.ui.theme.TextMuted
import java.util.Locale

@Composable
fun OnboardingStep6VaultSealing(
    displayName: String,
    emailAddress: String,
    profileImageUri: Uri?,
    country: CountryCurrencyMapping,
    strategy: String,
    totalLiquidity: Double,
    totalCommitments: Double,
    remainingSeconds: Int,
    onSealImmediately: () -> Unit
) {
    val context = LocalContext.current
    val avatarBitmap = rememberImageBitmapFromUri(context, profileImageUri)
    val countdownFraction = remainingSeconds / 10f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Vault Sealed & Secured", fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextDark)
            Text("Your personal financial hub is initialized", fontSize = 12.sp, color = TextMuted)

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = CardWhite,
                border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap,
                                    contentDescription = null,
                                    modifier = Modifier.size(38.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(38.dp).clip(CircleShape).background(AccentPurple.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(displayName.take(1).uppercase().ifEmpty { "J" }, fontWeight = FontWeight.Black, fontSize = 16.sp, color = AccentPurple)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(displayName, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = TextDark)
                                Text(if (emailAddress.isNotBlank()) emailAddress else "${country.countryName} ${country.flagEmoji}", fontSize = 11.sp, color = TextMuted)
                            }
                        }

                        Surface(shape = RoundedCornerShape(8.dp), color = TealPrimary.copy(alpha = 0.14f)) {
                            Text(strategy, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TealPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Opening Liquidity", fontSize = 10.sp, color = TextMuted)
                            Text("${country.currencySymbol}${String.format(Locale.US, "%,.2f", totalLiquidity)}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = TextDark)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Monthly Commitments", fontSize = 10.sp, color = TextMuted)
                            Text("${country.currencySymbol}${String.format(Locale.US, "%,.0f", totalCommitments)}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = CoralAccent)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HumanDeliberationSceneCanvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 18.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(AccentPurple)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Auto-sealing vault in ${remainingSeconds}s...",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { countdownFraction },
                        modifier = Modifier
                            .width(130.dp)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = AccentPurple,
                        trackColor = BorderLight.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSealImmediately,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Enter Dashboard Now", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}
