package com.example.projectgest.ui.inicio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.projectgest.data.EstadoDatos
import com.example.projectgest.data.Suscripcion
import com.example.projectgest.ui.LocalAcciones
import com.example.projectgest.ui.cobrosVencidos
import com.example.projectgest.ui.components.AvisoError
import com.example.projectgest.ui.components.EntradaEscalonada
import com.example.projectgest.ui.components.EtiquetaUrgencia
import com.example.projectgest.ui.components.FormaTarjeta
import com.example.projectgest.ui.components.PantallaConDatos
import com.example.projectgest.ui.components.Tarjeta
import com.example.projectgest.ui.components.TituloSeccion
import com.example.projectgest.ui.formatHoyLargo
import com.example.projectgest.ui.formatMonto
import com.example.projectgest.ui.ingresosEsperadosDelMes
import com.example.projectgest.ui.proximosCobrosDeLaSemana
import com.example.projectgest.ui.theme.EstiloCifra
import com.example.projectgest.ui.theme.ProjectGestTheme
import com.example.projectgest.ui.urgenciaDe
import com.example.projectgest.ui.vencimientoRelativo

@Composable
fun InicioScreen(
    datos: EstadoDatos,
    nombre: String?,
    onVerReportes: () -> Unit,
    onVerCliente: (String) -> Unit,
) {
    val acciones = LocalAcciones.current
    val vencidos = remember(datos.suscripciones) { cobrosVencidos(datos.suscripciones) }
    val proximos = remember(datos.suscripciones) { proximosCobrosDeLaSemana(datos.suscripciones) }
    val ingresosMes = remember(datos.suscripciones) { ingresosEsperadosDelMes(datos.suscripciones) }
    val totalVencido = vencidos.sumOf { it.monto }

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
                Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                    Text(formatHoyLargo(), style = MaterialTheme.typography.bodyMedium, color = ProjectGestTheme.extra.textoSecundario)
                    Text(
                        if (nombre != null) "Hola, ${nombre.substringBefore(' ')}" else "Hola",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
            }
            if (datos.error != null) item { AvisoError(datos.error) }

            if (datos.reportesPendientes > 0) {
                item { AvisoReportes(datos.reportesPendientes, onVerReportes) }
            }

            item {
                MetricaDestacada(
                    etiqueta = "Ingresos esperados este mes",
                    valor = ingresosMes,
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MetricaChica(
                        etiqueta = "Vencido",
                        valor = formatMonto(totalVencido),
                        detalle = "${vencidos.size} cobro${if (vencidos.size == 1) "" else "s"}",
                        color = if (totalVencido > 0) ProjectGestTheme.extra.peligro else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    MetricaChica(
                        etiqueta = "Esta semana",
                        valor = proximos.size.toString(),
                        detalle = formatMonto(proximos.sumOf { it.monto }),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item { TituloSeccion("Cobros vencidos", Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) }
            if (vencidos.isEmpty()) {
                item { SinPendientes("Nada vencido. Todo al día.") }
            } else {
                itemsIndexed(vencidos, key = { _, s -> "v-${s.id}" }) { i, s ->
                    EntradaEscalonada(i) { FilaCobro(s, datos.nombreCliente(s.clienteId)) { onVerCliente(s.clienteId) } }
                }
            }

            item { TituloSeccion("Próximos 7 días", Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) }
            if (proximos.isEmpty()) {
                item { SinPendientes("No hay cobros esta semana.") }
            } else {
                itemsIndexed(proximos, key = { _, s -> "p-${s.id}" }) { i, s ->
                    EntradaEscalonada(i) { FilaCobro(s, datos.nombreCliente(s.clienteId)) { onVerCliente(s.clienteId) } }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun AvisoReportes(cantidad: Int, onClick: () -> Unit) {
    val extra = ProjectGestTheme.extra
    Card(
        onClick = onClick,
        shape = FormaTarjeta,
        colors = CardDefaults.cardColors(containerColor = extra.acentoFondo, contentColor = extra.acento),
        border = BorderStroke(1.dp, extra.acentoBorde),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (cantidad == 1) "1 reporte de pago por revisar" else "$cantidad reportes de pago por revisar",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text("Tus clientes enviaron comprobantes", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

/** Cifra grande que "cuenta" hasta su valor al aparecer, como el MetricCard web. */
@Composable
private fun MetricaDestacada(etiqueta: String, valor: Double) {
    // Se anima el progreso (0..1) y no el monto, para que al terminar la
    // cifra sea exacta (un Float perdería centavos en montos grandes).
    val progreso = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progreso.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
    }
    Tarjeta(Modifier.fillMaxWidth()) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = ProjectGestTheme.extra.textoSecundario)
        Spacer(Modifier.height(6.dp))
        Text(
            formatMonto(if (progreso.value >= 1f) valor else valor * progreso.value),
            style = MaterialTheme.typography.displaySmall.merge(EstiloCifra),
        )
    }
}

@Composable
private fun MetricaChica(etiqueta: String, valor: String, detalle: String, color: Color, modifier: Modifier) {
    Tarjeta(modifier) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = ProjectGestTheme.extra.textoSecundario)
        Spacer(Modifier.height(4.dp))
        Text(valor, style = MaterialTheme.typography.titleLarge.merge(EstiloCifra), color = color, maxLines = 1)
        Text(detalle, style = MaterialTheme.typography.bodySmall, color = ProjectGestTheme.extra.textoSecundario, maxLines = 1)
    }
}

@Composable
private fun FilaCobro(suscripcion: Suscripcion, nombreCliente: String, onClick: () -> Unit) {
    Tarjeta(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(nombreCliente, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    suscripcion.servicio,
                    style = MaterialTheme.typography.bodySmall,
                    color = ProjectGestTheme.extra.textoSecundario,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                EtiquetaUrgencia(vencimientoRelativo(suscripcion.proximoCobro), urgenciaDe(suscripcion.proximoCobro))
            }
            Spacer(Modifier.width(12.dp))
            Text(formatMonto(suscripcion.monto), style = MaterialTheme.typography.titleMedium.merge(EstiloCifra))
        }
    }
}

@Composable
private fun SinPendientes(texto: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = ProjectGestTheme.extra.exito, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = ProjectGestTheme.extra.textoSecundario)
    }
}
