package com.example.projectgest.ui.clientes

import android.content.Intent
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.projectgest.data.EstadoDatos
import com.example.projectgest.data.Pago
import com.example.projectgest.ui.LocalAcciones
import com.example.projectgest.ui.cobros.TarjetaSuscripcion
import com.example.projectgest.ui.cobros.rememberCompartirLink
import com.example.projectgest.ui.components.Avatar
import com.example.projectgest.ui.components.EstadoVacio
import com.example.projectgest.ui.components.FilaDato
import com.example.projectgest.ui.components.PantallaConDatos
import com.example.projectgest.ui.components.Tarjeta
import com.example.projectgest.ui.components.TituloSeccion
import com.example.projectgest.ui.formatFecha
import com.example.projectgest.ui.formatMonto
import com.example.projectgest.ui.theme.EstiloCifra
import com.example.projectgest.ui.theme.ProjectGestTheme
import kotlinx.coroutines.launch

@Composable
fun ClienteDetalleScreen(datos: EstadoDatos, clienteId: String) {
    val acciones = LocalAcciones.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val compartir = rememberCompartirLink(acciones.repository, acciones::mostrar)

    val cliente = datos.cliente(clienteId)
    val suscripciones = remember(datos.suscripciones, clienteId) {
        datos.suscripciones.filter { it.clienteId == clienteId }.sortedBy { it.proximoCobro }
    }
    val pagos = remember(datos.pagos, clienteId) { datos.pagos.filter { it.clienteId == clienteId } }

    PantallaConDatos(
        cargadoAlgunaVez = datos.cargadoAlgunaVez,
        cargando = datos.cargando,
        error = datos.error,
        onRefrescar = acciones::refrescar,
    ) {
        if (cliente == null) {
            EstadoVacio(Icons.Outlined.PersonOff, "Cliente no encontrado", "Puede que lo hayan eliminado desde la web.")
            return@PantallaConDatos
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(Modifier.padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(cliente.nombre, Modifier.size(56.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(cliente.nombre, style = MaterialTheme.typography.headlineSmall)
                        Text(cliente.empresa, style = MaterialTheme.typography.bodyMedium, color = ProjectGestTheme.extra.textoSecundario)
                    }
                }
            }
            item {
                Tarjeta(Modifier.fillMaxWidth()) {
                    FilaDato("Email", cliente.email, onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, "mailto:${cliente.email}".toUri())
                        runCatching { context.startActivity(intent) }
                    })
                    FilaDato("Total pagado", formatMonto(pagos.sumOf { it.monto }))
                    FilaDato("Pagos registrados", pagos.size.toString())
                }
            }

            item { TituloSeccion("Suscripciones", Modifier.padding(horizontal = 4.dp)) }
            if (suscripciones.isEmpty()) {
                item {
                    Text(
                        "Sin suscripciones activas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ProjectGestTheme.extra.textoSecundario,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            } else {
                items(suscripciones, key = { it.id }) { s ->
                    TarjetaSuscripcion(
                        suscripcion = s,
                        nombreCliente = null,
                        onRegistrarPago = { acciones.registrarPago(s) },
                        onCompartirLink = { scope.launch { compartir.compartir(s, cliente.nombre) } },
                        compartiendo = compartir.enCurso == s.id,
                    )
                }
            }

            item { TituloSeccion("Historial de pagos", Modifier.padding(horizontal = 4.dp)) }
            if (pagos.isEmpty()) {
                item {
                    Text(
                        "Todavía no hay pagos registrados.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ProjectGestTheme.extra.textoSecundario,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            } else {
                items(pagos, key = { it.id }) { FilaPagoCliente(it) }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun FilaPagoCliente(pago: Pago) {
    Tarjeta(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pago.servicio, style = MaterialTheme.typography.titleSmall)
                Text(
                    buildString {
                        append(formatFecha(pago.fechaPago))
                        if (!pago.referencia.isNullOrBlank()) append(" · ref: ${pago.referencia}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ProjectGestTheme.extra.textoSecundario,
                )
                if (!pago.descripcion.isNullOrBlank()) {
                    Text(pago.descripcion, style = MaterialTheme.typography.bodySmall, color = ProjectGestTheme.extra.textoSecundario)
                }
            }
            Text(formatMonto(pago.monto), style = MaterialTheme.typography.titleSmall.merge(EstiloCifra), color = ProjectGestTheme.extra.exito)
        }
    }
}
