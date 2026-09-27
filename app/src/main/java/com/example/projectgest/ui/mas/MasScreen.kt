package com.example.projectgest.ui.mas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.projectgest.BuildConfig
import com.example.projectgest.data.EstadoDatos
import com.example.projectgest.ui.components.Avatar
import com.example.projectgest.ui.components.Tarjeta
import com.example.projectgest.ui.sesion.biometriaDisponible
import com.example.projectgest.ui.sesion.pedirBiometria
import com.example.projectgest.ui.theme.ProjectGestTheme

@Composable
fun MasScreen(
    datos: EstadoDatos,
    nombre: String?,
    email: String?,
    biometriaActivada: Boolean,
    onCambiarBiometria: (Boolean) -> Unit,
    onPagos: () -> Unit,
    onNotificaciones: () -> Unit,
    onPerfil: () -> Unit,
    onCerrarSesion: () -> Unit,
) {
    val activity = LocalContext.current as FragmentActivity
    val hayBiometria = remember { biometriaDisponible(activity) }
    var confirmarSalida by remember { mutableStateOf(false) }
    val nombreMostrado = datos.perfil?.nombre ?: nombre ?: email.orEmpty()
    val notificacionesFallidas = datos.notificaciones.count { it.estado != "enviado" }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Tarjeta(Modifier.fillMaxWidth(), onClick = onPerfil) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(nombreMostrado, Modifier.size(52.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(nombreMostrado, style = MaterialTheme.typography.titleMedium)
                    Text(email.orEmpty(), style = MaterialTheme.typography.bodySmall, color = ProjectGestTheme.extra.textoSecundario)
                    Text("Empresa", style = MaterialTheme.typography.bodySmall, color = ProjectGestTheme.extra.textoSecundario)
                }
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = ProjectGestTheme.extra.textoSecundario)
            }
        }

        Tarjeta(Modifier.fillMaxWidth()) {
            ItemMenu(Icons.Outlined.History, "Historial de pagos", "${datos.pagos.size} pagos registrados", onPagos)
            HorizontalDivider(color = ProjectGestTheme.extra.borde)
            ItemMenu(
                Icons.Outlined.NotificationsNone,
                "Notificaciones enviadas",
                if (notificacionesFallidas > 0) "$notificacionesFallidas fallaron" else "Avisos de WhatsApp a tus clientes",
                onNotificaciones,
                detalleColor = if (notificacionesFallidas > 0) ProjectGestTheme.extra.peligro else null,
            )
            HorizontalDivider(color = ProjectGestTheme.extra.borde)
            ItemMenu(Icons.Outlined.AccountBalance, "Datos bancarios", "Los que ven tus clientes al pagar", onPerfil)
        }

        Tarjeta(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Desbloqueo con huella", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        if (hayBiometria) "Pedir huella o rostro al abrir la app" else "Configura una huella o PIN en el teléfono",
                        style = MaterialTheme.typography.bodySmall,
                        color = ProjectGestTheme.extra.textoSecundario,
                    )
                }
                Switch(
                    checked = biometriaActivada && hayBiometria,
                    enabled = hayBiometria,
                    onCheckedChange = { activar ->
                        if (activar) {
                            // Confirmar la huella antes de activarla, para no
                            // dejar al usuario afuera con una que no funciona.
                            pedirBiometria(activity, "Activar desbloqueo", onExito = { onCambiarBiometria(true) }, onError = {})
                        } else {
                            onCambiarBiometria(false)
                        }
                    },
                )
            }
        }

        Tarjeta(Modifier.fillMaxWidth()) {
            ItemMenu(
                Icons.AutoMirrored.Outlined.Logout,
                "Cerrar sesión",
                null,
                { confirmarSalida = true },
                tituloColor = ProjectGestTheme.extra.peligro,
                mostrarFlecha = false,
            )
        }

        Text(
            "projectGest ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = ProjectGestTheme.extra.textoSecundario,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
        )
        Spacer(Modifier.height(8.dp))
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tendrás que ingresar tu email y contraseña la próxima vez.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    onCerrarSesion()
                }) { Text("Cerrar sesión", color = ProjectGestTheme.extra.peligro) }
            },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun ItemMenu(
    icono: ImageVector,
    titulo: String,
    detalle: String?,
    onClick: () -> Unit,
    detalleColor: Color? = null,
    tituloColor: Color? = null,
    mostrarFlecha: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icono, contentDescription = null, tint = tituloColor ?: MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge, color = tituloColor ?: MaterialTheme.colorScheme.onSurface)
            if (detalle != null) {
                Text(detalle, style = MaterialTheme.typography.bodySmall, color = detalleColor ?: ProjectGestTheme.extra.textoSecundario)
            }
        }
        if (mostrarFlecha) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = ProjectGestTheme.extra.textoSecundario)
        }
    }
}
