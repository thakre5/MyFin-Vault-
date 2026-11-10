package com.example.myfin.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================================
// 1. DESIGN SYSTEM TOKENS (50 - 900 SCALE)
// ============================================================================

// Primary Scale (Royal Cobalt Blue)
val Primary50 = Color(0xFFEFF6FF)
val Primary100 = Color(0xFFDBEAFE)
val Primary200 = Color(0xFFBFDBFE)
val Primary300 = Color(0xFF93C5FD)
val Primary400 = Color(0xFF60A5FA)
val Primary500 = Color(0xFF3B82F6)
val Primary600 = Color(0xFF2563EB)
val Primary700 = Color(0xFF1D4ED8)
val Primary800 = Color(0xFF1E40AF)
val Primary900 = Color(0xFF1E3A8A)

// Neutral Scale (Cool Slate)
val Neutral50 = Color(0xFFF8FAFC)
val Neutral100 = Color(0xFFF1F5F9)
val Neutral200 = Color(0xFFE2E8F0)
val Neutral300 = Color(0xFFCBD5E1)
val Neutral400 = Color(0xFF94A3B8)
val Neutral500 = Color(0xFF64748B)
val Neutral600 = Color(0xFF475569)
val Neutral700 = Color(0xFF334155)
val Neutral800 = Color(0xFF1E293B)
val Neutral900 = Color(0xFF0F172A)

// Success Triad (Emerald)
val Success100 = Color(0xFFD1FAE5)
val Success500 = Color(0xFF10B981)
val Success700 = Color(0xFF047857)

// Warning Triad (Amber)
val Warning100 = Color(0xFFFEF3C7)
val Warning500 = Color(0xFFF59E0B)
val Warning700 = Color(0xFFB45309)

// Error Triad (Coral Red)
val Error100 = Color(0xFFFEE2E2)
val Error500 = Color(0xFFEF4444)
val Error700 = Color(0xFFB91C1C)

// ============================================================================
// 2. SEMANTIC APP ALIASES (100% Backward-Compatible Across All Screens)
// ============================================================================

val CanvasLight = Neutral50
val CardWhite = Color(0xFFFFFFFF)
val TextDark = Neutral900
val TextMuted = Neutral500
val BorderLight = Neutral200

// Mapped from Legacy Purple to the new Primary Royal Blue Scale
val AccentPurple = Primary600
val AccentPurpleDark = Primary800
val AccentPurpleLight = Primary50

// Semantic Status Aliases
val SoftGreen = Success500
val SoftGreenLight = Success100
val SoftGreenDark = Success700

val SoftRed = Error500
val SoftRedLight = Error100
val SoftRedDark = Error700

val SoftAmber = Warning500
val SoftAmberLight = Warning100
val SoftAmberDark = Warning700

// Secondary Asset / Wealth Accent
val SoftTeal = Color(0xFF0284C7)
val SoftTealLight = Color(0xFFE0F2FE)

// Shared Gradients
val HeroCardGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFFFFFF),
        Primary50
    )
)

val PrimaryHeroGradient = Brush.linearGradient(
    colors = listOf(
        Primary700,
        Primary900
    )
)

val MidnightHeroGradient = Brush.linearGradient(
    colors = listOf(
        Neutral900,
        Primary900
    )
)
