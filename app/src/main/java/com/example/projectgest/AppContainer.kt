package com.example.projectgest

import android.content.Context
import com.example.projectgest.data.AuthRepository
import com.example.projectgest.data.NOMBRE_COOKIE
import com.example.projectgest.data.ProjectGestApi
import com.example.projectgest.data.Repository
import com.example.projectgest.data.SessionStore
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Dependencias de la app, creadas una sola vez en [ProjectGestApp]. */
class AppContainer(context: Context) {

    val baseUrl: String = BuildConfig.BASE_URL

    val sessionStore = SessionStore(context)

    private val _sesionExpirada = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Emite cuando el servidor responde 401 a una request autenticada. */
    val sesionExpirada: SharedFlow<Unit> = _sesionExpirada.asSharedFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val token = sessionStore.tokenActual
            val request = if (token != null) {
                // La API autentica por cookie; una app nativa no tiene cookie
                // jar de navegador, así que se envía a mano.
                chain.request().newBuilder().header("Cookie", "$NOMBRE_COOKIE=$token").build()
            } else {
                chain.request()
            }
            val response = chain.proceed(request)
            val esLogin = request.url.encodedPath.endsWith("/api/auth/login")
            if (response.code == 401 && token != null && !esLogin) {
                _sesionExpirada.tryEmit(Unit)
            }
            response
        }
        .apply {
            if (BuildConfig.DEBUG) {
                // BASIC no imprime headers, así el token no queda en el logcat.
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    private val api: ProjectGestApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(ProjectGestApi::class.java)

    val repository = Repository(api, json, baseUrl)

    val authRepository = AuthRepository(api, sessionStore, repository, baseUrl)
}
