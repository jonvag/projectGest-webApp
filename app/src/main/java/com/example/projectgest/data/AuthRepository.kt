package com.example.projectgest.data

import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl

/** Nombre de la cookie de sesión de la web (lib/auth-constants.ts). */
const val NOMBRE_COOKIE = "projectgest_session"

class AuthRepository(
    private val api: ProjectGestApi,
    private val sessionStore: SessionStore,
    private val repository: Repository,
    private val baseUrl: String,
) {
    suspend fun login(email: String, password: String): Resultado<Unit> {
        val respuesta = try {
            api.login(LoginRequest(email.trim(), password))
        } catch (e: Exception) {
            return Resultado.Error(repository.mensajeDe(e))
        }

        if (!respuesta.isSuccessful) {
            return Resultado.Error(
                if (respuesta.code() == 401) "Email o contraseña incorrectos."
                else repository.mensajeDe(retrofit2.HttpException(respuesta)),
            )
        }

        val cuerpo = respuesta.body()
        // El backend actualizado devuelve el token en el cuerpo. Si todavía
        // corre la versión anterior, se toma de la cookie Set-Cookie.
        val token = cuerpo?.token ?: tokenDesdeCookies(respuesta.headers().values("Set-Cookie"))
            ?: return Resultado.Error("El servidor no devolvió una sesión válida.")

        sessionStore.guardar(token, cuerpo?.nombre, cuerpo?.email ?: email.trim())

        if (cuerpo?.rol != "EMPRESA") {
            // Esta app está pensada solo para cuentas de empresa.
            logout()
            return Resultado.Error("Esta app es solo para cuentas de empresa.")
        }

        return Resultado.Ok(Unit)
    }

    suspend fun logout() {
        runCatching { api.logout() }
        sessionStore.borrar()
        repository.limpiar()
    }

    private fun tokenDesdeCookies(headers: List<String>): String? {
        val url = baseUrl.toHttpUrl()
        return headers
            .mapNotNull { Cookie.parse(url, it) }
            .firstOrNull { it.name == NOMBRE_COOKIE }
            ?.value
    }
}
