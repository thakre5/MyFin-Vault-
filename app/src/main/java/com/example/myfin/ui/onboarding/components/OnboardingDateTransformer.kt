package com.example.myfin.ui.onboarding.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Visual transformation that formats an 8-digit date string (DDMMYYYY) into DD/MM/YYYY.
 *
 * Designed specifically for digits-only inputs. Ensure the host TextField sanitizes
 * input via `sanitizeDobDigits(it)`.
 */
object OnboardingDateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text.take(8)

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
                if (raw.isEmpty()) return 0
                val slashesBefore = when {
                    clamped <= 2 -> if (clamped == 2 && raw.length > 2) 1 else 0
                    clamped <= 4 -> if (clamped == 4 && raw.length > 4) 2 else 1
                    else -> 2
                }
                return (clamped + slashesBefore).coerceIn(0, out.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, out.length)
                return when {
                    clamped <= 2 -> clamped
                    clamped <= 5 -> clamped - 1
                    else -> clamped - 2
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
