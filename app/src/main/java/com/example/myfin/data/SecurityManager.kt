package com.example.myfin.data

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SecurityManager(context: Context) {

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback for custom OEM ROMs or environments where Keystore initialization fails
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getStoredPin(): String? {
        return prefs.getString(KEY_PIN_HASH, null)
    }

    fun hasPin(): Boolean {
        return !getStoredPin().isNullOrBlank()
    }

    fun setPin(pin: String) {
        val cleanPin = pin.trim()
        if (cleanPin.isBlank()) {
            prefs.edit()
                .remove(KEY_PIN_HASH)
                .remove(KEY_PIN_SALT)
                .apply()
        } else {
            val salt = getOrCreateSalt()
            val hash = hashPinWithSalt(cleanPin, salt)
            prefs.edit().putString(KEY_PIN_HASH, hash).apply()
        }
    }

    fun verifyPin(inputPin: String): Boolean {
        val storedHash = getStoredPin() ?: return false
        val cleanInput = inputPin.trim()
        val salt = prefs.getString(KEY_PIN_SALT, null)

        // 1. Verify against cryptographic salted hash
        if (!salt.isNullOrBlank()) {
            return hashPinWithSalt(cleanInput, salt) == storedHash
        }

        // 2. Legacy fallback for unsalted hashes with automatic upgrade
        val legacyHash = hashLegacy(cleanInput)
        if (legacyHash == storedHash) {
            setPin(cleanInput) // Auto-upgrade to salted hash
            return true
        }

        return false
    }

    fun setRecoveryDob(dob: String) {
        val cleanDob = dob.trim()
        if (cleanDob.isBlank()) {
            prefs.edit().remove(KEY_RECOVERY_DOB).apply()
        } else {
            prefs.edit().putString(KEY_RECOVERY_DOB, cleanDob).apply()
        }
    }

    fun getRecoveryDob(): String? {
        return prefs.getString(KEY_RECOVERY_DOB, null)
    }

    fun verifyDob(inputDob: String): Boolean {
        val storedDob = getRecoveryDob() ?: return false
        val cleanInput = inputDob.trim()
        if (cleanInput.isBlank()) return false

        // 1. Semantic ISO Date Normalization (handles DD/MM/YYYY, YYYY-MM-DD, DD-MM-YYYY)
        val normInput = normalizeDateString(cleanInput)
        val normStored = normalizeDateString(storedDob)

        if (normInput.isNotEmpty() && normStored.isNotEmpty() && normInput == normStored) {
            return true
        }

        // 2. Fallback: Digit-only stripped comparison or exact string match
        val rawDigitsInput = cleanInput.filter { it.isDigit() }
        val rawDigitsStored = storedDob.filter { it.isDigit() }

        return cleanInput.equals(storedDob.trim(), ignoreCase = true) ||
                (rawDigitsInput.isNotEmpty() && rawDigitsInput == rawDigitsStored)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun canAuthenticateWithBiometrics(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // BiometricPrompt UI remains visible and prompts user to retry naturally
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock MyFin Vault")
            .setSubtitle("Authenticate using your biometric credentials")
            .setNegativeButtonText("Use Master PIN")
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }

    private fun getOrCreateSalt(): String {
        var salt = prefs.getString(KEY_PIN_SALT, null)
        if (salt.isNullOrBlank()) {
            val randomBytes = ByteArray(16)
            SecureRandom().nextBytes(randomBytes)
            salt = randomBytes.joinToString("") { "%02x".format(it) }
            prefs.edit().putString(KEY_PIN_SALT, salt).apply()
        }
        return salt
    }

    private fun hashPinWithSalt(pin: String, salt: String): String {
        val combined = "$salt:$pin"
        val bytes = MessageDigest.getInstance("SHA-256").digest(combined.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashLegacy(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun normalizeDateString(dateStr: String): String {
        val candidatePatterns = listOf(
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "yyyy/MM/dd",
            "dd.MM.yyyy",
            "yyyyMMdd",
            "ddMMyyyy"
        )

        for (pattern in candidatePatterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsedDate: Date? = sdf.parse(dateStr)
                if (parsedDate != null) {
                    return SimpleDateFormat("yyyyMMdd", Locale.US).format(parsedDate)
                }
            } catch (_: Exception) {
                // Try next pattern
            }
        }

        return ""
    }

    companion object {
        private const val PREFS_NAME = "myfin_vault_security_prefs"
        private const val KEY_PIN_HASH = "master_pin_sha256"
        private const val KEY_PIN_SALT = "master_pin_salt"
        private const val KEY_RECOVERY_DOB = "master_recovery_dob"
    }
}
