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
            .remove(KEY_PIN_LEGACY) // Purge plaintext legacy storage
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

        // Seamless migration for legacy unhashed PINs
        val legacyPin = sharedPreferences.getString(KEY_PIN_LEGACY, null)
        if (legacyPin != null && legacyPin == trimmedEntered) {
            setPin(trimmedEntered) // Auto-upgrade to salted hash
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
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }

    // --- RECOVERY DOB KEYS ---

    fun setRecoveryDob(dob: String): Boolean {
        return sharedPreferences.edit().putString(KEY_RECOVERY_DOB, dob.trim()).commit()
    }

    fun getRecoveryDob(): String? {
        return sharedPreferences.getString(KEY_RECOVERY_DOB, null)
    }

    fun verifyRecoveryDob(enteredDob: String): Boolean {
        val storedDob = getRecoveryDob() ?: return false
        val cleanStored = storedDob.replace("[^0-9]".toRegex(), "")
        val cleanEntered = enteredDob.replace("[^0-9]".toRegex(), "")
        return cleanStored.isNotEmpty() && cleanStored == cleanEntered
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
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
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
            .commit()
    }

    fun shouldLockOnResume(timeoutMillis: Long = DEFAULT_LOCK_TIMEOUT_MILLIS): Boolean {
        val lastBackground = sharedPreferences.getLong(KEY_LAST_BACKGROUND_TIME, 0L)
        if (lastBackground == 0L) return false
        val elapsed = System.currentTimeMillis() - lastBackground
        return elapsed >= timeoutMillis
    }

    fun clearSessionLock() {
        sharedPreferences.edit()
            .putLong(KEY_LAST_BACKGROUND_TIME, 0L)
            .commit()
    }

    // --- VAULT RESET & WIPES ---

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
        const val DEFAULT_LOCK_TIMEOUT_MILLIS = 60_000L // 60 Seconds
    }
}
