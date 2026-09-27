package com.example.projectgest.ui.clientes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.projectgest.data.Cliente
import com.example.projectgest.data.EstadoDatos
import com.example.projectgest.data.Suscripcion
import com.example.projectgest.ui.LocalAcciones
import com.example.projectgest.ui.Urgencia
import com.example.projectgest.ui.components.Avatar
import com.example.projectgest.ui.components.AvisoError
import com.example.projectgest.ui.components.EntradaEscalonada
import com.example.projectgest.ui.components.EstadoVacio
import com.example.projectgest.ui.components.EtiquetaUrgencia
import com.example.projectgest.ui.components.PantallaConDatos
import com.example.projectgest.ui.components.Tarjeta
import com.example.projectgest.ui.theme.ProjectGestTheme
import com.example.projectgest.ui.urgenciaDe
import com.example.projectgest.ui.vencimientoRelativo
import java.text.Normalizer

@Composable
fun ClientesScreen(datos: EstadoDatos, onAbrirCliente: (String) -> Unit) {
    val acciones = LocalAcciones.current
    var busqueda by rememberSaveable { mutableStateOf("") }

    val suscripcionesPorCliente = remember(datos.suscripciones) { datos.suscripciones.groupBy { it.clienteId } }
    val filtrados = remember(datos.clientes, busqueda) {
        val q = normalizar(busqueda)
        if (q.isBlank()) datos.clientes
        else datos.clientes.filter {
            normalizar(it.nombre).contains(q) || normalizar(it.empresa).contains(q) || normalizar(it.email).contains(q)
        }
    }

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
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar por nombre, empresa o email") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (busqueda.isNotEmpty()) {
                            IconButton(onClick = { busqueda = "" }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedBorderColor = ProjectGestTheme.extra.borde,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (datos.error != null) item { AvisoError(datos.error) }

            if (filtrados.isEmpty()) {
                item {
                    if (busqueda.isBlank()) {
                        EstadoVacio(Icons.Outlined.Groups, "Todavía no tienes clientes", "Puedes crearlos desde la web de projectGest.")
                    } else {
                        EstadoVacio(Icons.Outlined.Search, "Sin resultados", "No hay clientes que coincidan con \"$busqueda\".")
                    }
                }
            } else {
                item {
                    Text(
                        "${filtrados.size} cliente${if (filtrados.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ProjectGestTheme.extra.textoSecundario,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                itemsIndexed(filtrados, key = { _, c -> c.id }) { i, cliente ->
                    EntradaEscalonada(i) {
                        FilaCliente(cliente, suscripcionesPorCliente[cliente.id].orEmpty()) { onAbrirCliente(cliente.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilaCliente(cliente: Cliente, suscripciones: List<Suscripcion>, onClick: () -> Unit) {
    val masUrgente = suscripciones.minByOrNull { it.proximoCobro }
    Tarjeta(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(cliente.nombre)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(cliente.nombre, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    cliente.empresa,
                    style = MaterialTheme.typography.bodySmall,
                    color = ProjectGestTheme.extra.textoSecundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${suscripciones.size} servicio${if (suscripciones.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ProjectGestTheme.extra.textoSecundario,
                )
                if (masUrgente != null && urgenciaDe(masUrgente.proximoCobro) != Urgencia.Normal) {
                    Spacer(Modifier.height(4.dp))
                    EtiquetaUrgencia(vencimientoRelativo(masUrgente.proximoCobro), urgenciaDe(masUrgente.proximoCobro))
                }
            }
        }
    }
}

/** Búsqueda sin distinguir mayúsculas ni tildes ("jose" encuentra "José"). */
private fun normalizar(texto: String): String =
    Normalizer.normalize(texto.lowercase().trim(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
