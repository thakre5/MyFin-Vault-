@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myfin.ui.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.onboarding.CountryCurrencyMapping
import com.example.myfin.ui.onboarding.CyanPrimary
import com.example.myfin.ui.onboarding.PurplePrimary
import com.example.myfin.ui.onboarding.SupportedCountries
import com.example.myfin.ui.onboarding.components.OnboardingDateVisualTransformation
import com.example.myfin.ui.onboarding.components.SolnexTiltedCardsHero
import com.example.myfin.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingStep1Identity(
    displayName: String,
    emailAddress: String,
    rawDobDigits: String,
    selectedCountry: CountryCurrencyMapping,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onDobChange: (String) -> Unit,
    onCountrySelect: (CountryCurrencyMapping) -> Unit,
    onRegisterVault: () -> Unit,
    onRestoreVault: () -> Unit
) {
    var showCountryPickerSheet by remember { mutableStateOf(false) }
    var showRestoreConfirmationSheet by remember { mutableStateOf(false) }

    val countrySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val restoreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // =========================================================
            // TOP BRANDING HEADER (1:1 with Step 0)
            // =========================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(36.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(CyanPrimary, AccentPurple, PurplePrimary)
                            )
                        )
                        val c = center
                        val r = size.minDimension * 0.32f
                        repeat(8) { i ->
                            val angleRad = Math.toRadians((i * 45.0))
                            val px = c.x + (r * cos(angleRad)).toFloat()
                            val py = c.y + (r * sin(angleRad)).toFloat()
                            drawLine(
                                color = Color.White,
                                start = c,
                                end = Offset(px, py),
                                strokeWidth = 2.4.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = c)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "MyFin",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // =========================================================
            // 3D TILTED CARDS HERO
            // =========================================================
            SolnexTiltedCardsHero(currencySymbol = selectedCountry.currencySymbol)

            Spacer(modifier = Modifier.weight(0.4f))

            // =========================================================
            // 4 PROFILE TEXT BOXES (Capsule Shapes Matching Reference)
            // =========================================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Username (Full Width Pill)
                OutlinedTextField(
                    value = displayName,
                    onValueChange = onDisplayNameChange,
                    placeholder = { Text("Username", fontSize = 13.5.sp, color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Person,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardWhite,
                        unfocusedContainerColor = CardWhite,
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = BorderLight.copy(alpha = 0.9f)
                    )
                )

                // 2. Email Address (Full Width Pill)
                OutlinedTextField(
                    value = emailAddress,
                    onValueChange = onEmailChange,
                    placeholder = { Text("Email Address", fontSize = 13.5.sp, color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Mail,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardWhite,
                        unfocusedContainerColor = CardWhite,
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = BorderLight.copy(alpha = 0.9f)
                    )
                )

                // 3. 50:50 Split Row (DOB & Country Currency Mapping)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Left 50%: Date of Birth
                    OutlinedTextField(
                        value = rawDobDigits,
                        onValueChange = { input ->
                            onDobChange(input.filter { it.isDigit() }.take(8))
                        },
                        placeholder = { Text("DD/MM/YYYY", fontSize = 12.5.sp, color = TextMuted) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(17.dp)
                            )
                        },
                        visualTransformation = OnboardingDateVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardWhite,
                            unfocusedContainerColor = CardWhite,
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = BorderLight.copy(alpha = 0.9f)
                        )
                    )

                    // Right 50%: Country & Currency Selector
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .clickable { showCountryPickerSheet = true },
                        shape = RoundedCornerShape(26.dp),
                        color = CardWhite,
                        border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.9f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(selectedCountry.flagEmoji, fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${selectedCountry.currencySymbol} ${selectedCountry.currencyCode}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select Currency",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            // =========================================================
            // BOTTOM ACTION BUTTONS (Full Register + Compact Centered Restore)
            // =========================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRegisterVault,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TextDark)
                ) {
                    Text(
                        text = "Register Vault",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = { showRestoreConfirmationSheet = true },
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(21.dp),
                    border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.9f)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CardWhite),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    Text(
                        text = "Restore Vault",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = TextDark
                    )
                }
            }
        }

        // =========================================================
        // COUNTRY / CURRENCY SELECTOR SHEET
        // =========================================================
        if (showCountryPickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCountryPickerSheet = false },
                sheetState = countrySheetState,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                containerColor = CardWhite,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp, top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = AccentPurple.copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Public, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Country & Currency", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextDark)
                            Text("Select region for local denomination formats", fontSize = 11.5.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(SupportedCountries) { item ->
                            val isSelected = selectedCountry.countryName == item.countryName
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        onCountrySelect(item)
                                        showCountryPickerSheet = false
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) AccentPurple.copy(alpha = 0.10f) else CanvasLight,
                                border = BorderStroke(0.8.dp, if (isSelected) AccentPurple else BorderLight)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.flagEmoji, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            item.countryName,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = TextDark
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) AccentPurple else CardWhite,
                                        border = BorderStroke(0.6.dp, BorderLight)
                                    ) {
                                        Text(
                                            text = "${item.currencySymbol}  (${item.currencyCode})",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else TextDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================
        // RESTORE VAULT CONFIRMATION BOTTOM SHEET
        // =========================================================
        if (showRestoreConfirmationSheet) {
            ModalBottomSheet(
                onDismissRequest = { showRestoreConfirmationSheet = false },
                sheetState = restoreSheetState,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                containerColor = CardWhite,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp, top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape,
                        color = AccentPurple.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.25f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Restore from Local Backup",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Import your offline encrypted snapshot to restore your accounts, budget plans, and historical records.",
                        fontSize = 12.5.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CanvasLight,
                        border = BorderStroke(0.8.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LockReset, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Requires the Master PIN used when creating the backup.", fontSize = 11.5.sp, color = TextDark, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Restores all bank accounts, taxonomies, and fixed bills.", fontSize = 11.5.sp, color = TextDark, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("100% offline verification with hardware keystore security.", fontSize = 11.5.sp, color = TextDark, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showRestoreConfirmationSheet = false
                            onRestoreVault()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextDark)
                    ) {
                        Text("Choose Backup File (.json)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showRestoreConfirmationSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                    }
                }
            }
        }
    }
}
