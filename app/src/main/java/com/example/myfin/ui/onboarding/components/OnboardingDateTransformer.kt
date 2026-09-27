package com.example.myfin.ui.onboarding.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Visual transformation that formats an 8-digit date string (DDMMYYYY) into DD/MM/YYYY.
 *
 * Designed specifically for digits-only inputs. Ensure the host TextField sanitizes
 * input via `it.filter { it.isDigit() }.take(8)`.
 */
object OnboardingDateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.filter { it.isDigit() }.take(8)

        val out = buildString {
            for (i in raw.indices) {
                append(raw[i])
                if ((i == 1 || i == 3) && i != raw.lastIndex) {
                    append('/')
                }
            }
        }

        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                return when {
                    clamped <= 1 -> clamped
                    clamped <= 3 -> clamped + 1
                    clamped <= 8 -> clamped + 2
                    else -> out.length
                }.coerceIn(0, out.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, out.length)
                return when {
                    clamped <= 2 -> clamped
                    clamped <= 5 -> clamped - 1
                    clamped <= 10 -> clamped - 2
                    else -> raw.length
                }.coerceIn(0, text.text.length)
            }
        }

        return TransformedText(AnnotatedString(out), offsetTranslator)
    }
}

/**
 * Input filter helper to be used in onValueChange:
 * `onValueChange = { rawDobDigits = sanitizeDobDigits(it) }`
 */
fun sanitizeDobDigits(input: String): String {
    return input.filter { it.isDigit() }.take(8)
}
