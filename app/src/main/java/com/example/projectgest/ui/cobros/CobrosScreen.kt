package com.example.projectgest.ui.cobros

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.projectgest.data.EstadoDatos
import com.example.projectgest.data.Suscripcion
import com.example.projectgest.ui.LocalAcciones
import com.example.projectgest.ui.components.AvisoError
import com.example.projectgest.ui.components.EntradaEscalonada
import com.example.projectgest.ui.components.EstadoVacio
import com.example.projectgest.ui.components.PantallaConDatos
import com.example.projectgest.ui.formatMonto
import com.example.projectgest.ui.parseFecha
import com.example.projectgest.ui.theme.ProjectGestTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class FiltroCobros(val etiqueta: String) {
    Todos("Todos"),
    Vencidos("Vencidos"),
    Semana("Próximos 7 días"),
    Mes("Este mes"),
}

private fun FiltroCobros.aplica(s: Suscripcion, hoy: LocalDate): Boolean {
    val fecha = parseFecha(s.proximoCobro)
    return when (this) {
        FiltroCobros.Todos -> true
        FiltroCobros.Vencidos -> fecha < hoy
        FiltroCobros.Semana -> fecha >= hoy && fecha <= hoy.plusDays(7)
        FiltroCobros.Mes -> fecha.year == hoy.year && fecha.month == hoy.month
    }
}

@Composable
fun CobrosScreen(datos: EstadoDatos, onAbrirCliente: (String) -> Unit) {
    val acciones = LocalAcciones.current
    val scope = rememberCoroutineScope()
    val compartir = rememberCompartirLink(acciones.repository, acciones::mostrar)
    var filtro by rememberSaveable { mutableStateOf(FiltroCobros.Todos) }

    val hoy = LocalDate.now()
    val visibles = remember(datos.suscripciones, filtro) {
        datos.suscripciones.filter { filtro.aplica(it, hoy) }.sortedBy { it.proximoCobro }
    }

    PantallaConDatos(
        cargadoAlgunaVez = datos.cargadoAlgunaVez,
        cargando = datos.cargando,
        error = datos.error,
        onRefrescar = acciones::refrescar,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(FiltroCobros.entries) { f ->
                        val cantidad = datos.suscripciones.count { f.aplica(it, hoy) }
                        FilterChip(
                            selected = filtro == f,
                            onClick = { filtro = f },
                            label = { Text("${f.etiqueta} · $cantidad") },
                        )
                    }
                }
            }
            if (datos.error != null) item { AvisoError(datos.error, Modifier.padding(horizontal = 16.dp)) }

            if (visibles.isEmpty()) {
                item { EstadoVacio(Icons.Outlined.EventAvailable, "Nada por aquí", "No hay cobros en \"${filtro.etiqueta.lowercase()}\".") }
            } else {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Text(
                            "Total ${formatMonto(visibles.sumOf { it.monto })}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ProjectGestTheme.extra.textoSecundario,
                        )
                    }
                }
                itemsIndexed(visibles, key = { _, s -> s.id }) { i, s ->
                    val nombre = datos.nombreCliente(s.clienteId)
                    EntradaEscalonada(i) {
                        TarjetaSuscripcion(
                            suscripcion = s,
                            nombreCliente = nombre,
                            onRegistrarPago = { acciones.registrarPago(s) },
                            onCompartirLink = { scope.launch { compartir.compartir(s, nombre) } },
                            compartiendo = compartir.enCurso == s.id,
                            onClick = { onAbrirCliente(s.clienteId) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}
