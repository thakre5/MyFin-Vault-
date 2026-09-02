package com.example.myfin.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class SecurityManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences = createEncryptedPreferences(context)

    private fun createEncryptedPreferences(appContext: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                appContext,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Handle Keystore corruption, master key revocation, or OS upgrades
            if (e is GeneralSecurityException || e is java.io.IOException) {
                try {
                    val prefsFile = File(appContext.filesDir.parent, "shared_prefs/$PREFS_FILE_NAME.xml")
                    if (prefsFile.exists()) {
                        prefsFile.delete()
                    }

                    try {
                        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                        if (keyStore.containsAlias(MasterKey.DEFAULT_MASTER_KEY_ALIAS)) {
                            keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                        }
                    } catch (_: Exception) {}

                    val masterKey = MasterKey.Builder(appContext)
                        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                        .build()

                    EncryptedSharedPreferences.create(
                        appContext,
                        PREFS_FILE_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                } catch (_: Exception) {
                    appContext.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
                }
            } else {
                appContext.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    // --- PBKDF2 PIN HASHING & AUTHENTICATION ---

    fun isPinSet(): Boolean {
        val hasHash = sharedPreferences.getString(KEY_PIN_HASH, null) != null
        val hasLegacyPin = sharedPreferences.getString(KEY_PIN_LEGACY, null) != null
        return hasHash || hasLegacyPin
    }

    fun setPin(pin: String): Boolean {
        if (pin.isBlank()) return false
        val salt = generateSalt()
        val hash = hashPinWithSalt(pin.trim(), salt)
        val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

        return sharedPreferences.edit()
            .putString(KEY_PIN_HASH, hashBase64)
            .putString(KEY_PIN_SALT, saltBase64)
            .remove(KEY_PIN_LEGACY)
            .commit()
    }

    fun verifyPin(enteredPin: String): Boolean {
        val trimmedEntered = enteredPin.trim()
        val storedHashBase64 = sharedPreferences.getString(KEY_PIN_HASH, null)
        val storedSaltBase64 = sharedPreferences.getString(KEY_PIN_SALT, null)

        if (storedHashBase64 != null && storedSaltBase64 != null) {
            val salt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)
            val computedHash = hashPinWithSalt(trimmedEntered, salt)
            val computedHashBase64 = Base64.encodeToString(computedHash, Base64.NO_WRAP)
            return constantTimeEquals(storedHashBase64, computedHashBase64)
        }

        // Migration for unhashed legacy PINs
        val legacyPin = sharedPreferences.getString(KEY_PIN_LEGACY, null)
        if (legacyPin != null && legacyPin == trimmedEntered) {
            setPin(trimmedEntered)
            return true
        }

        return false
    }

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    private fun hashPinWithSalt(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, HASH_ITERATIONS, HASH_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        return MessageDigest.isEqual(a.toByteArray(Charsets.UTF_8), b.toByteArray(Charsets.UTF_8))
    }

    // --- RECOVERY DOB KEYS & NORMALIZATION ---

    fun setRecoveryDob(dob: String): Boolean {
        val normalized = normalizeDobToDmy(dob.replace("[^0-9]".toRegex(), "")) ?: dob.trim()
        return sharedPreferences.edit().putString(KEY_RECOVERY_DOB, normalized).commit()
    }

    fun getRecoveryDob(): String? {
        return sharedPreferences.getString(KEY_RECOVERY_DOB, null)
    }

    fun verifyRecoveryDob(enteredDob: String): Boolean {
        val storedDob = getRecoveryDob() ?: return false
        val cleanStored = storedDob.replace("[^0-9]".toRegex(), "")
        val cleanEntered = enteredDob.replace("[^0-9]".toRegex(), "")

        if (cleanStored.length != 8 || cleanEntered.length != 8) {
            return cleanStored.isNotEmpty() && cleanStored == cleanEntered
        }

        val normalizedStored = normalizeDobToDmy(cleanStored) ?: cleanStored
        val normalizedEntered = normalizeDobToDmy(cleanEntered) ?: cleanEntered

        return normalizedStored == normalizedEntered
    }

    private fun normalizeDobToDmy(digits: String): String? {
        if (digits.length != 8) return null

        // Check if already DDMMYYYY (Day: 1..31, Month: 1..12, Year: 1900..2100)
        val dmyDay = digits.substring(0, 2).toIntOrNull() ?: 0
        val dmyMonth = digits.substring(2, 4).toIntOrNull() ?: 0
        val dmyYear = digits.substring(4, 8).toIntOrNull() ?: 0
        val isDmy = dmyDay in 1..31 && dmyMonth in 1..12 && dmyYear in 1900..2100

        // Check if YYYYMMDD
        val ymdYear = digits.substring(0, 4).toIntOrNull() ?: 0
        val ymdMonth = digits.substring(4, 6).toIntOrNull() ?: 0
        val ymdDay = digits.substring(6, 8).toIntOrNull() ?: 0
        val isYmd = ymdDay in 1..31 && ymdMonth in 1..12 && ymdYear in 1900..2100

        return when {
            isDmy -> digits
            isYmd -> "%02d%02d%04d".format(ymdDay, ymdMonth, ymdYear)
            else -> digits
        }
    }

    fun resetPinWithDob(enteredDob: String, newPin: String): Boolean {
        if (verifyRecoveryDob(enteredDob) && newPin.trim().length >= 4) {
            return setPin(newPin.trim())
        }
        return false
    }

    // --- BIOMETRIC AUTHENTICATION ---

    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean): Boolean {
        return sharedPreferences.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).commit()
    }

    fun canAuthenticateWithBiometrics(targetContext: Context = context): Boolean {
        val biometricManager = BiometricManager.from(targetContext)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                clearSessionLock()
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError()
                }
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock MyFin Vault")
            .setSubtitle("Authenticate using biometrics")
            .setNegativeButtonText("Use PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        prompt.authenticate(promptInfo)
    }

    // --- SESSION AUTO-LOCK TIMEOUT ENGINE (60s) ---

    fun recordAppBackgrounded() {
        sharedPreferences.edit()
            .putLong(KEY_LAST_BACKGROUND_TIME, System.currentTimeMillis())
            .apply()
    }

    fun shouldLockOnResume(timeoutMillis: Long = DEFAULT_LOCK_TIMEOUT_MILLIS): Boolean {
        if (!isPinSet()) return false
        val lastBackground = sharedPreferences.getLong(KEY_LAST_BACKGROUND_TIME, 0L)
        if (lastBackground == 0L) return false
        val elapsed = System.currentTimeMillis() - lastBackground
        return elapsed >= timeoutMillis
    }

    fun clearSessionLock() {
        sharedPreferences.edit()
            .putLong(KEY_LAST_BACKGROUND_TIME, 0L)
            .apply()
    }

    fun clearSecurity(): Boolean {
        return sharedPreferences.edit().clear().commit()
    }

    fun clearAll(): Boolean {
        return clearSecurity()
    }

    companion object {
        private const val PREFS_FILE_NAME = "myfin_secure_prefs"
        private const val FALLBACK_PREFS_NAME = "myfin_fallback_prefs"

        private const val KEY_PIN_HASH = "secure_vault_pin_hash_v2"
        private const val KEY_PIN_SALT = "secure_vault_pin_salt_v2"
        private const val KEY_PIN_LEGACY = "secure_vault_pin"
        private const val KEY_RECOVERY_DOB = "recovery_dob"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_auth_enabled"
        private const val KEY_LAST_BACKGROUND_TIME = "last_background_timestamp"

        private const val HASH_ITERATIONS = 10000
        private const val HASH_KEY_LENGTH = 256
        const val DEFAULT_LOCK_TIMEOUT_MILLIS = 60_000L
    }
}
