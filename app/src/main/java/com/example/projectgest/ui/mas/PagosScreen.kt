package com.example.projectgest.ui.mas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.projectgest.data.EstadoDatos
import com.example.projectgest.data.Pago
import com.example.projectgest.ui.LocalAcciones
import com.example.projectgest.ui.components.EstadoVacio
import com.example.projectgest.ui.components.PantallaConDatos
import com.example.projectgest.ui.components.Tarjeta
import com.example.projectgest.ui.components.TituloSeccion
import com.example.projectgest.ui.formatFecha
import com.example.projectgest.ui.formatMes
import com.example.projectgest.ui.formatMonto
import com.example.projectgest.ui.parseFecha
import com.example.projectgest.ui.theme.EstiloCifra
import com.example.projectgest.ui.theme.ProjectGestTheme

@Composable
fun PagosScreen(datos: EstadoDatos, onAbrirCliente: (String) -> Unit) {
    val acciones = LocalAcciones.current
    // Agrupados por mes, del más reciente al más viejo.
    val porMes = remember(datos.pagos) {
        datos.pagos
            .groupBy { parseFecha(it.fechaPago).withDayOfMonth(1) }
            .toSortedMap(compareByDescending { it })
    }

    PantallaConDatos(
        cargadoAlgunaVez = datos.cargadoAlgunaVez,
        cargando = datos.cargando,
        error = datos.error,
        onRefrescar = acciones::refrescar,
    ) {
        if (porMes.isEmpty()) {
            EstadoVacio(Icons.Outlined.History, "Sin pagos registrados", "Los pagos que registres o apruebes aparecerán aquí.")
            return@PantallaConDatos
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            porMes.forEach { (mes, pagos) ->
                item(key = "mes-$mes") {
                    TituloSeccion(formatMes(mes), Modifier.padding(horizontal = 4.dp)) {
                        Text(
                            formatMonto(pagos.sumOf { it.monto }),
                            style = MaterialTheme.typography.titleSmall.merge(EstiloCifra),
                            color = ProjectGestTheme.extra.exito,
                        )
                    }
                }
                items(pagos.sortedByDescending { it.fechaPago }, key = { it.id }) { pago ->
                    FilaPago(pago, datos.nombreCliente(pago.clienteId)) { onAbrirCliente(pago.clienteId) }
                }
            }
        }
    }
}

@Composable
private fun FilaPago(pago: Pago, nombreCliente: String, onClick: () -> Unit) {
    Tarjeta(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(nombreCliente, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    pago.servicio,
                    style = MaterialTheme.typography.bodySmall,
                    color = ProjectGestTheme.extra.textoSecundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildString {
                        append(formatFecha(pago.fechaPago))
                        if (!pago.referencia.isNullOrBlank()) append(" · ref: ${pago.referencia}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ProjectGestTheme.extra.textoSecundario,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(formatMonto(pago.monto), style = MaterialTheme.typography.titleSmall.merge(EstiloCifra))
        }
    }
}
