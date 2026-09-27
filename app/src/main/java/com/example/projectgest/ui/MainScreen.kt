package com.example.projectgest.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.projectgest.data.Repository
import com.example.projectgest.data.Suscripcion
import com.example.projectgest.ui.clientes.ClienteDetalleScreen
import com.example.projectgest.ui.clientes.ClientesScreen
import com.example.projectgest.ui.cobros.CobrosScreen
import com.example.projectgest.ui.cobros.HojaRegistrarPago
import com.example.projectgest.ui.inicio.InicioScreen
import com.example.projectgest.ui.mas.MasScreen
import com.example.projectgest.ui.mas.NotificacionesScreen
import com.example.projectgest.ui.mas.PagosScreen
import com.example.projectgest.ui.mas.PerfilScreen
import com.example.projectgest.ui.reportes.ReporteDetalleScreen
import com.example.projectgest.ui.reportes.ReportesScreen
import com.example.projectgest.ui.sesion.UiSesion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

// Rutas type-safe de Navigation Compose.
@Serializable data object RutaInicio
@Serializable data object RutaClientes
@Serializable data object RutaCobros
@Serializable data object RutaReportes
@Serializable data object RutaMas
@Serializable data class RutaCliente(val id: String)
@Serializable data class RutaReporte(val id: String)
@Serializable data object RutaPagos
@Serializable data object RutaNotificaciones
@Serializable data object RutaPerfil

/** Acciones globales que cualquier pantalla puede disparar. */
class AccionesApp(
    val repository: Repository,
    private val snackbar: SnackbarHostState,
    private val scope: CoroutineScope,
    val registrarPago: (Suscripcion) -> Unit,
) {
    fun mostrar(mensaje: String) {
        scope.launch { snackbar.showSnackbar(mensaje) }
    }

    fun refrescar() {
        scope.launch { repository.refrescar() }
    }
}

val LocalAcciones = staticCompositionLocalOf<AccionesApp> { error("AccionesApp no provisto") }

private data class Pestana(
    val ruta: Any,
    val etiqueta: String,
    val icono: ImageVector,
    val iconoActivo: ImageVector,
)

private val pestanas = listOf(
    Pestana(RutaInicio, "Inicio", Icons.Outlined.Home, Icons.Filled.Home),
    Pestana(RutaClientes, "Clientes", Icons.Outlined.Groups, Icons.Filled.Groups),
    Pestana(RutaCobros, "Cobros", Icons.Outlined.Payments, Icons.Filled.Payments),
    Pestana(RutaReportes, "Reportes", Icons.AutoMirrored.Outlined.ReceiptLong, Icons.AutoMirrored.Filled.ReceiptLong),
    Pestana(RutaMas, "Más", Icons.Outlined.Menu, Icons.Filled.Menu),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    repository: Repository,
    sesion: UiSesion,
    onCambiarBiometria: (Boolean) -> Unit,
    onCerrarSesion: () -> Unit,
) {
    val navController = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val datos by repository.estado.collectAsStateWithLifecycle()
    var suscripcionAPagar by remember { mutableStateOf<Suscripcion?>(null) }

    val acciones = remember(repository) {
        AccionesApp(repository, snackbar, scope) { suscripcionAPagar = it }
    }

    // Primera carga al entrar a la app.
    LaunchedEffect(Unit) {
        if (!repository.estado.value.cargadoAlgunaVez) repository.refrescar()
    }

    val entrada by navController.currentBackStackEntryAsState()
    val destino = entrada?.destination
    val pestanaActual = pestanas.firstOrNull { p -> destino?.hasRoute(p.ruta::class) == true }

    val titulo = when {
        pestanaActual != null -> pestanaActual.etiqueta
        destino?.hasRoute(RutaCliente::class) == true -> "Cliente"
        destino?.hasRoute(RutaReporte::class) == true -> "Reporte de pago"
        destino?.hasRoute(RutaPagos::class) == true -> "Historial de pagos"
        destino?.hasRoute(RutaNotificaciones::class) == true -> "Notificaciones"
        destino?.hasRoute(RutaPerfil::class) == true -> "Mi perfil"
        else -> ""
    }

    fun abrirCliente(id: String) = navController.navigate(RutaCliente(id))

    CompositionLocalProvider(LocalAcciones provides acciones) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (pestanaActual?.ruta == RutaInicio) "projectGest" else titulo) },
                    navigationIcon = {
                        if (pestanaActual == null) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
            bottomBar = {
                if (pestanaActual != null) {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        pestanas.forEach { p ->
                            val seleccionada = pestanaActual == p
                            NavigationBarItem(
                                selected = seleccionada,
                                onClick = {
                                    navController.navigate(p.ruta) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    val pendientes = datos.reportesPendientes
                                    if (p.ruta == RutaReportes && pendientes > 0) {
                                        BadgedBox(badge = { Badge { Text(pendientes.toString()) } }) {
                                            Icon(if (seleccionada) p.iconoActivo else p.icono, contentDescription = p.etiqueta)
                                        }
                                    } else {
                                        Icon(if (seleccionada) p.iconoActivo else p.icono, contentDescription = p.etiqueta)
                                    }
                                },
                                label = { Text(p.etiqueta) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ),
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = RutaInicio,
                modifier = Modifier.padding(padding),
                enterTransition = { fadeIn(tween(200)) + slideInHorizontally(tween(250)) { it / 12 } },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(200)) },
                popExitTransition = { fadeOut(tween(150)) + slideOutHorizontally(tween(250)) { it / 12 } },
            ) {
                composable<RutaInicio> {
                    InicioScreen(
                        datos = datos,
                        nombre = datos.perfil?.nombre ?: sesion.nombre,
                        onVerReportes = {
                            navController.navigate(RutaReportes) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                            }
                        },
                        onVerCliente = ::abrirCliente,
                    )
                }
                composable<RutaClientes> { ClientesScreen(datos, ::abrirCliente) }
                composable<RutaCobros> { CobrosScreen(datos, ::abrirCliente) }
                composable<RutaReportes> { ReportesScreen(datos) { navController.navigate(RutaReporte(it)) } }
                composable<RutaMas> {
                    MasScreen(
                        datos = datos,
                        nombre = sesion.nombre,
                        email = sesion.email,
                        biometriaActivada = sesion.biometriaActivada,
                        onCambiarBiometria = onCambiarBiometria,
                        onPagos = { navController.navigate(RutaPagos) },
                        onNotificaciones = { navController.navigate(RutaNotificaciones) },
                        onPerfil = { navController.navigate(RutaPerfil) },
                        onCerrarSesion = onCerrarSesion,
                    )
                }
                composable<RutaCliente> { ClienteDetalleScreen(datos, it.toRoute<RutaCliente>().id) }
                composable<RutaReporte> {
                    ReporteDetalleScreen(datos, it.toRoute<RutaReporte>().id) { navController.popBackStack() }
                }
                composable<RutaPagos> { PagosScreen(datos, ::abrirCliente) }
                composable<RutaNotificaciones> { NotificacionesScreen(datos) }
                composable<RutaPerfil> { PerfilScreen(datos) }
            }
        }

        suscripcionAPagar?.let { s ->
            HojaRegistrarPago(
                suscripcion = s,
                nombreCliente = datos.nombreCliente(s.clienteId),
                repository = repository,
                onCerrar = { suscripcionAPagar = null },
                onRegistrado = {
                    suscripcionAPagar = null
                    acciones.mostrar("Pago de ${datos.nombreCliente(s.clienteId)} registrado")
                },
            )
        }
    }
}
