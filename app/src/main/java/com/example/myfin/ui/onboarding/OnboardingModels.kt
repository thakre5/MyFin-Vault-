package com.example.myfin.ui.onboarding

import androidx.compose.ui.graphics.Color
import com.example.myfin.data.TransactionType

val CyanPrimary = Color(0xFF00D2EE)
val PurplePrimary = Color(0xFF6C5CE7)
val TealPrimary = Color(0xFF10B981)
val CoralAccent = Color(0xFFFF6B6B)

data class CountryCurrencyMapping(
    val countryName: String,
    val flagEmoji: String,
    val currencySymbol: String,
    val currencyCode: String
)

val SupportedCountries = listOf(
    CountryCurrencyMapping("India", "🇮🇳", "₹", "INR"),
    CountryCurrencyMapping("United States", "🇺🇸", "$", "USD"),
    CountryCurrencyMapping("United Kingdom", "🇬🇧", "£", "GBP"),
    CountryCurrencyMapping("Eurozone", "🇪🇺", "€", "EUR"),
    CountryCurrencyMapping("United Arab Emirates", "🇦🇪", "د.إ", "AED"),
    CountryCurrencyMapping("Singapore", "🇸🇬", "$", "SGD"),
    CountryCurrencyMapping("Australia", "🇦🇺", "$", "AUD"),
    CountryCurrencyMapping("Canada", "🇨🇦", "$", "CAD"),
    CountryCurrencyMapping("Japan", "🇯🇵", "¥", "JPY"),
    CountryCurrencyMapping("Saudi Arabia", "🇸🇦", "﷼", "SAR")
)

data class InitialAccountSetup(
    val name: String,
    val defaultType: String,
    val initialBalanceText: String,
    val minBalanceText: String = if (defaultType.equals("Commitments", ignoreCase = true)) "10000" else "0"
)

data class InitialCommitmentPreset(
    val title: String,
    val categoryName: String,
    val subcategoryName: String,
    val type: TransactionType,
    val defaultDueDay: Int,
    val amountText: String,
    val isSelected: Boolean
)

data class OnboardingCarouselSlide(
    val title: String,
    val subtitle: String
)

val WelcomeCarouselSlides = listOf(
    OnboardingCarouselSlide(
        title = "Own Your Wealth\nArchitecture",
        subtitle = "Create your offline vault to partition, store,\nand grow your capital securely"
    ),
    OnboardingCarouselSlide(
        title = "Zero-Knowledge\nLocal Security",
        subtitle = "Your financial records stay 100% on your device\nwith hardware-backed biometric encryption"
    ),
    OnboardingCarouselSlide(
        title = "Smart 3-Tier\nWealth Strategy",
        subtitle = "Automate your capital between daily spending,\ncommitted bills, and emergency fortress"
    )
)
