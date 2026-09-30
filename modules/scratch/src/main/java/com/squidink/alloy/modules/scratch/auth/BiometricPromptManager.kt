package com.squidink.alloy.modules.scratch.auth

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

/**
 * Modern biometric and device credential manager using Android platform BiometricPrompt (minSdk 37).
 * Pure Compose & ComponentActivity compatible: eliminates all Fragment / FragmentActivity dependencies.
 */
class BiometricPromptManager(
    private val context: Context
) {
    private var cancellationSignal: CancellationSignal? = null

    /**
     * Checks whether biometric or screen lock authentication is supported and enrolled.
     */
    fun canAuthenticate(): Boolean {
        val biometricManager = context.getSystemService(Context.BIOMETRIC_SERVICE) as? BiometricManager ?: return false
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Displays the system authentication prompt for biometric or screen lock challenge.
     *
     * @param title Prompt dialog title.
     * @param subtitle Prompt dialog subtitle.
     * @param onSuccess Callback executed when authentication succeeds.
     * @param onError Callback executed with error message when authentication fails or is dismissed with error.
     */
    fun promptAuth(
        title: String = "Unlock Secure Document",
        subtitle: String = "Confirm biometric or device screen lock to view",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        cancellationSignal?.cancel()
        val signal = CancellationSignal()
        cancellationSignal = signal

        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED &&
                    errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        }

        val prompt = BiometricPrompt.Builder(context)
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(signal, executor, callback)
    }

    /**
     * Cancels any in-flight authentication challenge.
     */
    fun cancel() {
        cancellationSignal?.cancel()
        cancellationSignal = null
    }
}
