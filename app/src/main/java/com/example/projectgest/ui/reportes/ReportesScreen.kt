package com.example.projectgest.ui.reportes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.projectgest.data.EstadoDatos
import com.example.projectgest.data.ReportePago
import com.example.projectgest.ui.LocalAcciones
import com.example.projectgest.ui.components.AvisoError
import com.example.projectgest.ui.components.EntradaEscalonada
import com.example.projectgest.ui.components.EstadoVacio
import com.example.projectgest.ui.components.EtiquetaEstadoReporte
import com.example.projectgest.ui.components.PantallaConDatos
import com.example.projectgest.ui.components.Tarjeta
import com.example.projectgest.ui.formatFechaHora
import com.example.projectgest.ui.formatMonto
import com.example.projectgest.ui.theme.EstiloCifra
import com.example.projectgest.ui.theme.ProjectGestTheme

@Composable
fun ReportesScreen(datos: EstadoDatos, onAbrirReporte: (String) -> Unit) {
    val acciones = LocalAcciones.current
    var pestana by rememberSaveable { mutableIntStateOf(0) }

    val pendientes = remember(datos.reportes) { datos.reportes.filter { it.pendiente } }
    val revisados = remember(datos.reportes) {
        datos.reportes.filterNot { it.pendiente }.sortedByDescending { it.revisadoEn ?: it.creadoEn }
    }
    val visibles = if (pestana == 0) pendientes else revisados

    PantallaConDatos(
        cargadoAlgunaVez = datos.cargadoAlgunaVez,
        cargando = datos.cargando,
        error = datos.error,
        onRefrescar = acciones::refrescar,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf("Por revisar · ${pendientes.size}", "Revisados · ${revisados.size}").forEachIndexed { i, etiqueta ->
                        SegmentedButton(
                            selected = pestana == i,
                            onClick = { pestana = i },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                        ) { Text(etiqueta) }
                    }
                }
            }
            if (datos.error != null) item { AvisoError(datos.error) }

            if (visibles.isEmpty()) {
                item {
                    if (pestana == 0) {
                        EstadoVacio(Icons.Outlined.TaskAlt, "Todo revisado", "Cuando un cliente reporte un pago aparecerá aquí.")
                    } else {
                        EstadoVacio(Icons.Outlined.Inbox, "Sin reportes revisados")
                    }
                }
            } else {
                itemsIndexed(visibles, key = { _, r -> r.id }) { i, reporte ->
                    val monto = datos.suscripcion(reporte.suscripcionId)?.monto
                    EntradaEscalonada(i) { FilaReporte(reporte, monto) { onAbrirReporte(reporte.id) } }
                }
            }
        }
    }
}

@Composable
private fun FilaReporte(reporte: ReportePago, monto: Double?, onClick: () -> Unit) {
    Tarjeta(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(reporte.clienteNombre, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    reporte.servicio,
                    style = MaterialTheme.typography.bodySmall,
                    color = ProjectGestTheme.extra.textoSecundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (monto != null) {
                Text(formatMonto(monto), style = MaterialTheme.typography.titleSmall.merge(EstiloCifra))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            EtiquetaEstadoReporte(reporte.estado)
            Spacer(Modifier.width(8.dp))
            if (reporte.tieneComprobante) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = "Tiene comprobante",
                    tint = ProjectGestTheme.extra.textoSecundario,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                formatFechaHora(reporte.creadoEn),
                style = MaterialTheme.typography.bodySmall,
                color = ProjectGestTheme.extra.textoSecundario,
            )
        }
        if (!reporte.referencia.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Ref: ${reporte.referencia}",
                style = MaterialTheme.typography.bodySmall,
                color = ProjectGestTheme.extra.textoSecundario,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
