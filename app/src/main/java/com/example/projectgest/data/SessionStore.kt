package com.example.projectgest.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.dataStore by preferencesDataStore(name = "sesion")

data class SesionGuardada(
    val token: String,
    val nombre: String?,
    val email: String,
    val biometriaActivada: Boolean,
    val biometriaPreguntada: Boolean,
)

/**
 * Guarda el token de sesión cifrado con una clave AES del Android Keystore
 * (la clave nunca sale del hardware seguro del dispositivo). El resto de los
 * datos (nombre, email, preferencias) van en DataStore en claro.
 */
class SessionStore(private val context: Context) {

    // Copia en memoria para que el interceptor HTTP no tenga que descifrar
    // en cada request.
    @Volatile
    var tokenActual: String? = null
        private set

    suspend fun cargar(): SesionGuardada? {
        val prefs = context.dataStore.data.first()
        val cifrado = prefs[KEY_TOKEN] ?: return null
        val email = prefs[KEY_EMAIL] ?: return null
        val token = runCatching { descifrar(cifrado) }.getOrNull() ?: run {
            // La clave del Keystore se pudo haber invalidado (ej. restauración
            // de backup en otro dispositivo): tratar como sin sesión.
            borrar()
            return null
        }
        tokenActual = token
        return SesionGuardada(
            token = token,
            nombre = prefs[KEY_NOMBRE],
            email = email,
            biometriaActivada = prefs[KEY_BIOMETRIA] ?: false,
            biometriaPreguntada = prefs[KEY_BIOMETRIA_PREGUNTADA] ?: false,
        )
    }

    suspend fun guardar(token: String, nombre: String?, email: String) {
        val cifrado = cifrar(token)
        context.dataStore.edit { prefs ->
            prefs[KEY_TOKEN] = cifrado
            prefs[KEY_EMAIL] = email
            if (nombre != null) prefs[KEY_NOMBRE] = nombre else prefs.remove(KEY_NOMBRE)
        }
        tokenActual = token
    }

    suspend fun actualizarNombre(nombre: String?) {
        context.dataStore.edit { prefs ->
            if (nombre != null) prefs[KEY_NOMBRE] = nombre else prefs.remove(KEY_NOMBRE)
        }
    }

    suspend fun setBiometria(activada: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIA] = activada
            prefs[KEY_BIOMETRIA_PREGUNTADA] = true
        }
    }

    /** Borra la sesión pero conserva las preferencias de huella del dispositivo. */
    suspend fun borrar() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_NOMBRE)
            prefs.remove(KEY_EMAIL)
        }
        tokenActual = null
    }

    private fun clave(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(ALIAS_CLAVE, null) as? SecretKey)?.let { return it }

        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generador.init(
            KeyGenParameterSpec.Builder(
                ALIAS_CLAVE,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generador.generateKey()
    }

    private fun cifrar(texto: String): String {
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.ENCRYPT_MODE, clave())
        val iv = cipher.iv
        val cifrado = cipher.doFinal(texto.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + cifrado, Base64.NO_WRAP)
    }

    private fun descifrar(valor: String): String {
        val bytes = Base64.decode(valor, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_BYTES)
        val cifrado = bytes.copyOfRange(IV_BYTES, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMACION)
        cipher.init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(cifrado), Charsets.UTF_8)
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val ALIAS_CLAVE = "projectgest_sesion"
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12

        val KEY_TOKEN: Preferences.Key<String> = stringPreferencesKey("token_cifrado")
        val KEY_NOMBRE = stringPreferencesKey("nombre")
        val KEY_EMAIL = stringPreferencesKey("email")
        val KEY_BIOMETRIA = booleanPreferencesKey("biometria_activada")
        val KEY_BIOMETRIA_PREGUNTADA = booleanPreferencesKey("biometria_preguntada")
    }
}
