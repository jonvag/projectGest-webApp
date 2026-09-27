package com.example.projectgest.ui.sesion

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// Huella/rostro, con el PIN o patrón del teléfono como alternativa.
private const val AUTENTICADORES = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

fun biometriaDisponible(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(AUTENTICADORES) == BiometricManager.BIOMETRIC_SUCCESS

fun pedirBiometria(
    activity: FragmentActivity,
    titulo: String,
    onExito: () -> Unit,
    onError: (String?) -> Unit,
) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onExito()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // Cancelar a propósito no es un error que haya que mostrar.
                val cancelado = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                onError(if (cancelado) null else errString.toString())
            }
        },
    )
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle(titulo)
        .setSubtitle("Confirma que eres tú para entrar a projectGest")
        .setAllowedAuthenticators(AUTENTICADORES)
        .build()
    prompt.authenticate(info)
}
