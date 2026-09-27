package com.example.projectgest.ui.reportes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.SubcomposeAsyncImage
import com.example.projectgest.data.EstadoDatos
import com.example.projectgest.data.ReportePago
import com.example.projectgest.data.Resultado
import com.example.projectgest.ui.LocalAcciones
import com.example.projectgest.ui.calcularProximaFecha
import com.example.projectgest.ui.cobros.CampoFecha
import com.example.projectgest.ui.components.BotonPrincipal
import com.example.projectgest.ui.components.EstadoVacio
import com.example.projectgest.ui.components.EtiquetaEstadoReporte
import com.example.projectgest.ui.components.FilaDato
import com.example.projectgest.ui.components.PantallaConDatos
import com.example.projectgest.ui.components.Tarjeta
import com.example.projectgest.ui.etiquetaCiclo
import com.example.projectgest.ui.formatFecha
import com.example.projectgest.ui.formatFechaHora
import com.example.projectgest.ui.formatMonto
import com.example.projectgest.ui.theme.ProjectGestTheme
import kotlinx.coroutines.launch

@Composable
fun ReporteDetalleScreen(datos: EstadoDatos, reporteId: String, onVolver: () -> Unit) {
    val acciones = LocalAcciones.current
    val reporte = datos.reportes.find { it.id == reporteId }

    PantallaConDatos(
        cargadoAlgunaVez = datos.cargadoAlgunaVez,
        cargando = datos.cargando,
        error = datos.error,
        onRefrescar = acciones::refrescar,
    ) {
        if (reporte == null) {
            EstadoVacio(Icons.Outlined.SearchOff, "Reporte no encontrado")
        } else {
            DetalleReporte(datos, reporte, onVolver)
        }
    }
}

@Composable
private fun DetalleReporte(datos: EstadoDatos, reporte: ReportePago, onVolver: () -> Unit) {
    val acciones = LocalAcciones.current
    val scope = rememberCoroutineScope()
    val suscripcion = datos.suscripcion(reporte.suscripcionId)

    var fecha by remember(reporte.id) { mutableStateOf(calcularProximaFecha(reporte.proximoCobro, reporte.ciclo)) }
    var procesando by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmarRechazo by remember { mutableStateOf(false) }
    var verComprobante by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(reporte.clienteNombre, style = MaterialTheme.typography.headlineSmall)
                Text(reporte.servicio, style = MaterialTheme.typography.bodyMedium, color = ProjectGestTheme.extra.textoSecundario)
            }
            EtiquetaEstadoReporte(reporte.estado)
        }

        if (reporte.tieneComprobante) {
            Comprobante(
                url = acciones.repository.urlComprobante(reporte.id),
                modifier = Modifier.clickable { verComprobante = true },
            )
        } else {
            Tarjeta(Modifier.fillMaxWidth()) {
                Text("El cliente no adjuntó comprobante.", style = MaterialTheme.typography.bodyMedium, color = ProjectGestTheme.extra.textoSecundario)
            }
        }

        Tarjeta(Modifier.fillMaxWidth()) {
            if (suscripcion != null) FilaDato("Monto", formatMonto(suscripcion.monto))
            FilaDato("Referencia", reporte.referencia?.ifBlank { null } ?: "—")
            FilaDato("Ciclo", etiquetaCiclo(reporte.ciclo))
            FilaDato("Cobro reportado", formatFecha(reporte.proximoCobro))
            FilaDato("Enviado", formatFechaHora(reporte.creadoEn))
            if (reporte.revisadoEn != null) FilaDato("Revisado", formatFechaHora(reporte.revisadoEn))
        }

        if (reporte.pendiente) {
            CampoFecha(
                etiqueta = "Próximo cobro al aprobar",
                fecha = fecha,
                onCambio = { fecha = it },
                ayuda = "Al aprobar se registra el pago y la suscripción pasa a esta fecha.",
            )
            if (error != null) {
                Text(error!!, color = ProjectGestTheme.extra.peligro, style = MaterialTheme.typography.bodySmall)
            }
            BotonPrincipal(
                texto = "Aprobar pago",
                cargando = procesando == "aprobar",
                enabled = procesando == null,
                onClick = {
                    procesando = "aprobar"
                    error = null
                    scope.launch {
                        when (val r = acciones.repository.aprobarReporte(reporte, fecha)) {
                            is Resultado.Ok -> {
                                acciones.mostrar("Pago de ${reporte.clienteNombre} aprobado")
                                onVolver()
                            }
                            is Resultado.Error -> error = r.mensaje
                        }
                        procesando = null
                    }
                },
            )
            OutlinedButton(
                onClick = { confirmarRechazo = true },
                enabled = procesando == null,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                if (procesando == "rechazar") {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text("Rechazar", color = ProjectGestTheme.extra.peligro)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (confirmarRechazo) {
        AlertDialog(
            onDismissRequest = { confirmarRechazo = false },
            title = { Text("¿Rechazar este reporte?") },
            text = { Text("El pago de ${reporte.clienteNombre} quedará como rechazado y no se registrará en el historial.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarRechazo = false
                    procesando = "rechazar"
                    error = null
                    scope.launch {
                        when (val r = acciones.repository.rechazarReporte(reporte)) {
                            is Resultado.Ok -> {
                                acciones.mostrar("Reporte rechazado")
                                onVolver()
                            }
                            is Resultado.Error -> error = r.mensaje
                        }
                        procesando = null
                    }
                }) { Text("Rechazar", color = ProjectGestTheme.extra.peligro) }
            },
            dismissButton = { TextButton(onClick = { confirmarRechazo = false }) { Text("Cancelar") } },
        )
    }

    if (verComprobante) {
        VisorComprobante(acciones.repository.urlComprobante(reporte.id)) { verComprobante = false }
    }
}

@Composable
private fun Comprobante(url: String, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = "Comprobante de pago",
        contentScale = ContentScale.Crop,
        loading = {
            Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        },
        error = {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.BrokenImage, contentDescription = "No se pudo cargar el comprobante", tint = ProjectGestTheme.extra.textoSecundario)
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ProjectGestTheme.extra.chip),
    )
}

/** Comprobante a pantalla completa con zoom por pellizco. */
@Composable
private fun VisorComprobante(url: String, onCerrar: () -> Unit) {
    var escala by remember { mutableFloatStateOf(1f) }
    var desplazamiento by remember { mutableStateOf(Offset.Zero) }

    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        escala = (escala * zoom).coerceIn(1f, 5f)
                        desplazamiento = if (escala == 1f) Offset.Zero else desplazamiento + pan
                    }
                },
        ) {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = "Comprobante de pago",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = escala
                        scaleY = escala
                        translationX = desplazamiento.x
                        translationY = desplazamiento.y
                    },
            )
            IconButton(
                onClick = onCerrar,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp),
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Cerrar", tint = Color.White)
            }
        }
    }
}
