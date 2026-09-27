package com.example.projectgest

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.projectgest.data.Tema
import com.example.projectgest.ui.MainScreen
import com.example.projectgest.ui.components.CargandoPantalla
import com.example.projectgest.ui.sesion.BloqueoScreen
import com.example.projectgest.ui.sesion.EstadoSesion
import com.example.projectgest.ui.sesion.LoginScreen
import com.example.projectgest.ui.sesion.SesionViewModel
import com.example.projectgest.ui.theme.ProjectGestTheme

private enum class Pantalla { Cargando, Login, App }

// FragmentActivity (y no ComponentActivity) porque BiometricPrompt la requiere.
class MainActivity : FragmentActivity() {

    private val sesionViewModel: SesionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // Mantener el splash mientras se lee la sesión guardada, para no
        // mostrar el login un instante a alguien que ya estaba logueado.
        splash.setKeepOnScreenCondition { sesionViewModel.ui.value.estado == EstadoSesion.Cargando }
        enableEdgeToEdge()

        val repository = (application as ProjectGestApp).container.repository

        setContent {
            val ui by sesionViewModel.ui.collectAsStateWithLifecycle()
            val oscuro = when (ui.tema) {
                Tema.Sistema -> isSystemInDarkTheme()
                Tema.Claro -> false
                Tema.Oscuro -> true
            }

            // Los íconos de la barra de estado siguen al tema de la app, no
            // al del sistema, para que se lean cuando se fuerza claro/oscuro.
            LaunchedEffect(oscuro) {
                val estilo = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { oscuro }
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
            }

            ProjectGestTheme(darkTheme = oscuro) {

                val pantalla = when (ui.estado) {
                    EstadoSesion.Cargando -> Pantalla.Cargando
                    is EstadoSesion.SinSesion -> Pantalla.Login
                    EstadoSesion.Bloqueada, EstadoSesion.Activa -> Pantalla.App
                }

                AnimatedContent(
                    targetState = pantalla,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "estado-sesion",
                ) { destino ->
                    when (destino) {
                        Pantalla.Cargando -> CargandoPantalla()
                        Pantalla.Login -> LoginScreen(
                            ui = ui,
                            mensaje = (ui.estado as? EstadoSesion.SinSesion)?.mensaje,
                            onIngresar = sesionViewModel::login,
                        )
                        Pantalla.App -> Box(Modifier.fillMaxSize()) {
                            // La app queda montada debajo del bloqueo: al
                            // desbloquear se sigue exactamente donde estaba, y
                            // en frío los datos ya cargan mientras se pide la huella.
                            MainScreen(
                                repository = repository,
                                sesion = ui,
                                onCambiarBiometria = sesionViewModel::setBiometria,
                                onCambiarTema = sesionViewModel::setTema,
                                onCerrarSesion = sesionViewModel::logout,
                            )
                            AnimatedVisibility(
                                visible = ui.estado == EstadoSesion.Bloqueada,
                                enter = fadeIn(tween(120)),
                                exit = fadeOut(tween(220)),
                            ) {
                                BloqueoScreen(
                                    nombre = ui.nombre,
                                    onDesbloqueado = sesionViewModel::desbloquear,
                                    onUsarOtraCuenta = sesionViewModel::logout,
                                )
                            }
                        }
                    }
                }

                if (ui.ofrecerBiometria) {
                    AlertDialog(
                        onDismissRequest = { sesionViewModel.responderOfertaBiometria(false) },
                        title = { Text("¿Entrar con tu huella?") },
                        text = {
                            Text("La próxima vez podrás abrir projectGest con tu huella o rostro, sin escribir la contraseña.")
                        },
                        confirmButton = {
                            TextButton(onClick = { sesionViewModel.responderOfertaBiometria(true) }) { Text("Activar") }
                        },
                        dismissButton = {
                            TextButton(onClick = { sesionViewModel.responderOfertaBiometria(false) }) { Text("Ahora no") }
                        },
                    )
                }
            }
        }
    }
}
