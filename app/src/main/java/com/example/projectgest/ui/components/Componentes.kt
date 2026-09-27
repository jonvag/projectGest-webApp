package com.example.projectgest.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.projectgest.ui.Urgencia
import com.example.projectgest.ui.iniciales
import com.example.projectgest.ui.theme.ProjectGestTheme

val FormaTarjeta = RoundedCornerShape(16.dp)

/** Tarjeta plana con borde fino, como las cards de la web. */
@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit,
) {
    val colores = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val borde = BorderStroke(1.dp, ProjectGestTheme.extra.borde)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = FormaTarjeta, colors = colores, border = borde) {
            Column(Modifier.padding(16.dp), content = contenido)
        }
    } else {
        Card(modifier = modifier, shape = FormaTarjeta, colors = colores, border = borde) {
            Column(Modifier.padding(16.dp), content = contenido)
        }
    }
}

@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier, accion: (@Composable RowScope.() -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        accion?.invoke(this)
    }
}

@Composable
fun Etiqueta(texto: String, fondo: Color, color: Color, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        color = color,
        maxLines = 1,
        modifier = modifier
            .background(fondo, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun EtiquetaUrgencia(texto: String, urgencia: Urgencia, modifier: Modifier = Modifier) {
    val extra = ProjectGestTheme.extra
    when (urgencia) {
        Urgencia.Vencido -> Etiqueta(texto, extra.peligroFondo, extra.peligro, modifier)
        Urgencia.Pronto -> Etiqueta(texto, extra.acentoFondo, extra.acento, modifier)
        Urgencia.Normal -> Etiqueta(texto, extra.chip, extra.textoSecundario, modifier)
    }
}

@Composable
fun EtiquetaEstadoReporte(estado: String, modifier: Modifier = Modifier) {
    val extra = ProjectGestTheme.extra
    when (estado) {
        "pendiente" -> Etiqueta("Pendiente", extra.acentoFondo, extra.acento, modifier)
        "verificado" -> Etiqueta("Verificado", extra.exitoFondo, extra.exito, modifier)
        "rechazado" -> Etiqueta("Rechazado", extra.peligroFondo, extra.peligro, modifier)
        else -> Etiqueta(estado, extra.chip, extra.textoSecundario, modifier)
    }
}

@Composable
fun Avatar(nombre: String, modifier: Modifier = Modifier) {
    val extra = ProjectGestTheme.extra
    Box(
        modifier = modifier.size(40.dp).background(extra.acentoFondo, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(iniciales(nombre), style = MaterialTheme.typography.labelLarge, color = extra.acento)
    }
}

/** Fila "etiqueta ........ valor" para fichas de detalle. */
@Composable
fun FilaDato(etiqueta: String, valor: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = ProjectGestTheme.extra.textoSecundario)
        Spacer(Modifier.width(16.dp))
        Text(
            valor,
            style = MaterialTheme.typography.bodyMedium,
            color = if (onClick != null) ProjectGestTheme.extra.acento else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun EstadoVacio(icono: ImageVector, titulo: String, detalle: String? = null, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icono, contentDescription = null, tint = ProjectGestTheme.extra.textoSecundario, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (detalle != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                detalle,
                style = MaterialTheme.typography.bodyMedium,
                color = ProjectGestTheme.extra.textoSecundario,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun EstadoError(mensaje: String, onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = ProjectGestTheme.extra.textoSecundario, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(mensaje, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onReintentar) { Text("Reintentar") }
    }
}

/** Aviso fino arriba de una lista cuando falló un refresco pero hay datos viejos. */
@Composable
fun AvisoError(mensaje: String, modifier: Modifier = Modifier) {
    val extra = ProjectGestTheme.extra
    Text(
        mensaje,
        style = MaterialTheme.typography.bodySmall,
        color = extra.peligro,
        modifier = modifier
            .fillMaxWidth()
            .background(extra.peligroFondo, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
fun CargandoPantalla(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
    }
}

/**
 * Contenedor estándar de pantallas con datos del servidor: pull-to-refresh,
 * spinner en la primera carga y error con reintento si nunca se pudo cargar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConDatos(
    cargadoAlgunaVez: Boolean,
    cargando: Boolean,
    error: String?,
    onRefrescar: () -> Unit,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit,
) {
    when {
        !cargadoAlgunaVez && error != null -> EstadoError(error, onRefrescar, modifier.fillMaxSize())
        !cargadoAlgunaVez -> CargandoPantalla(modifier)
        else -> PullToRefreshBox(
            isRefreshing = cargando,
            onRefresh = onRefrescar,
            modifier = modifier.fillMaxSize(),
        ) { contenido() }
    }
}

/** Entrada escalonada de items de lista (equivalente al motion.li de la web). */
@Composable
fun EntradaEscalonada(indice: Int, contenido: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220, delayMillis = (indice.coerceAtMost(8)) * 35)) +
            slideInVertically(tween(260, delayMillis = (indice.coerceAtMost(8)) * 35)) { it / 6 },
    ) { contenido() }
}

@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cargando: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !cargando,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        if (cargando) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}
