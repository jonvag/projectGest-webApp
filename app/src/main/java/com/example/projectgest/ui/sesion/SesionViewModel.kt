package com.example.projectgest.ui.sesion

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.projectgest.ProjectGestApp
import com.example.projectgest.data.Resultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface EstadoSesion {
    data object Cargando : EstadoSesion
    data class SinSesion(val mensaje: String? = null) : EstadoSesion
    data object Bloqueada : EstadoSesion
    data object Activa : EstadoSesion
}

data class UiSesion(
    val estado: EstadoSesion = EstadoSesion.Cargando,
    val nombre: String? = null,
    val email: String? = null,
    val biometriaActivada: Boolean = false,
    /** Mostrar una vez, después del primer login, la oferta de usar huella. */
    val ofrecerBiometria: Boolean = false,
    val ingresando: Boolean = false,
    val errorLogin: String? = null,
)

class SesionViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as ProjectGestApp).container
    private val sessionStore = container.sessionStore
    private val authRepository = container.authRepository

    private val _ui = MutableStateFlow(UiSesion())
    val ui: StateFlow<UiSesion> = _ui.asStateFlow()

    private var biometriaPreguntada = false
    private var enSegundoPlanDesde: Long? = null

    // Al volver a la app después de un rato, pedir la huella otra vez.
    private val observadorProceso = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            enSegundoPlanDesde = SystemClock.elapsedRealtime()
        }

        override fun onStart(owner: LifecycleOwner) {
            val desde = enSegundoPlanDesde ?: return
            enSegundoPlanDesde = null
            val ausente = SystemClock.elapsedRealtime() - desde
            val ui = _ui.value
            if (ui.estado == EstadoSesion.Activa && ui.biometriaActivada && ausente >= BLOQUEO_TRAS_MS) {
                _ui.update { it.copy(estado = EstadoSesion.Bloqueada) }
            }
        }
    }

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(observadorProceso)

        viewModelScope.launch {
            val guardada = sessionStore.cargar()
            biometriaPreguntada = guardada?.biometriaPreguntada ?: false
            _ui.update {
                if (guardada == null) {
                    it.copy(estado = EstadoSesion.SinSesion())
                } else {
                    it.copy(
                        estado = if (guardada.biometriaActivada) EstadoSesion.Bloqueada else EstadoSesion.Activa,
                        nombre = guardada.nombre,
                        email = guardada.email,
                        biometriaActivada = guardada.biometriaActivada,
                    )
                }
            }
        }

        viewModelScope.launch {
            container.sesionExpirada.collect {
                if (_ui.value.estado is EstadoSesion.SinSesion) return@collect
                authRepository.logout()
                _ui.update {
                    UiSesion(
                        estado = EstadoSesion.SinSesion("Tu sesión expiró. Vuelve a ingresar."),
                        email = it.email,
                        biometriaActivada = it.biometriaActivada,
                    )
                }
            }
        }

        viewModelScope.launch {
            // Mantener el nombre del saludo al día con el perfil del servidor.
            container.repository.estado.collect { datos ->
                val nombre = datos.perfil?.nombre ?: return@collect
                if (nombre != _ui.value.nombre) {
                    _ui.update { it.copy(nombre = nombre) }
                    sessionStore.actualizarNombre(nombre)
                }
            }
        }
    }

    fun login(email: String, password: String, biometriaEnDispositivo: Boolean) {
        if (email.isBlank() || password.isBlank()) {
            _ui.update { it.copy(errorLogin = "Ingresa tu email y contraseña.") }
            return
        }
        _ui.update { it.copy(ingresando = true, errorLogin = null) }
        viewModelScope.launch {
            when (val resultado = authRepository.login(email, password)) {
                is Resultado.Ok -> {
                    val guardada = sessionStore.cargar()
                    _ui.update {
                        it.copy(
                            estado = EstadoSesion.Activa,
                            nombre = guardada?.nombre,
                            email = guardada?.email,
                            ingresando = false,
                            ofrecerBiometria = biometriaEnDispositivo && !biometriaPreguntada,
                        )
                    }
                }
                is Resultado.Error -> _ui.update {
                    it.copy(ingresando = false, errorLogin = resultado.mensaje)
                }
            }
        }
    }

    fun desbloquear() {
        _ui.update { it.copy(estado = EstadoSesion.Activa) }
    }

    fun responderOfertaBiometria(activar: Boolean) {
        biometriaPreguntada = true
        _ui.update { it.copy(ofrecerBiometria = false, biometriaActivada = activar) }
        viewModelScope.launch { sessionStore.setBiometria(activar) }
    }

    fun setBiometria(activada: Boolean) {
        biometriaPreguntada = true
        _ui.update { it.copy(biometriaActivada = activada) }
        viewModelScope.launch { sessionStore.setBiometria(activada) }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _ui.update {
                UiSesion(estado = EstadoSesion.SinSesion(), biometriaActivada = it.biometriaActivada)
            }
        }
    }

    override fun onCleared() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(observadorProceso)
    }

    private companion object {
        const val BLOQUEO_TRAS_MS = 2 * 60 * 1000L // 2 minutos fuera de la app
    }
}
