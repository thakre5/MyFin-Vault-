package com.example.myfin.data

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest

class SecurityManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStoredPin(): String? {
        return prefs.getString(KEY_PIN_HASH, null)
    }

    fun hasPin(): Boolean {
        return !getStoredPin().isNullOrBlank()
    }

    fun setPin(pin: String) {
        val cleanPin = pin.trim()
        if (cleanPin.isBlank()) {
            prefs.edit().remove(KEY_PIN_HASH).apply()
        } else {
            val hash = hashString(cleanPin)
            prefs.edit().putString(KEY_PIN_HASH, hash).apply()
        }
    }

    fun verifyPin(inputPin: String): Boolean {
        val storedHash = getStoredPin() ?: return false
        return hashString(inputPin.trim()) == storedHash
    }

    fun setRecoveryDob(dob: String) {
        prefs.edit().putString(KEY_RECOVERY_DOB, dob.trim()).apply()
    }

    fun getRecoveryDob(): String? {
        return prefs.getString(KEY_RECOVERY_DOB, null)
    }

    fun verifyDob(inputDob: String): Boolean {
        val storedDob = getRecoveryDob() ?: return false
        val cleanInput = inputDob.filter { it.isDigit() }
        val cleanStored = storedDob.filter { it.isDigit() }
        return inputDob.trim().equals(storedDob.trim(), ignoreCase = true) ||
                (cleanInput.isNotEmpty() && cleanInput == cleanStored)
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
                onError("Biometric authentication failed")
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

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "myfin_vault_security_prefs"
        private const val KEY_PIN_HASH = "master_pin_sha256"
        private const val KEY_RECOVERY_DOB = "master_recovery_dob"
    }
}
