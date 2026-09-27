package com.example.projectgest.ui.mas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.projectgest.data.EstadoDatos
import com.example.projectgest.data.Notificacion
import com.example.projectgest.ui.LocalAcciones
import com.example.projectgest.ui.components.Etiqueta
import com.example.projectgest.ui.components.EstadoVacio
import com.example.projectgest.ui.components.PantallaConDatos
import com.example.projectgest.ui.components.Tarjeta
import com.example.projectgest.ui.formatFechaHora
import com.example.projectgest.ui.theme.ProjectGestTheme

@Composable
fun NotificacionesScreen(datos: EstadoDatos) {
    val acciones = LocalAcciones.current

    PantallaConDatos(
        cargadoAlgunaVez = datos.cargadoAlgunaVez,
        cargando = datos.cargando,
        error = datos.error,
        onRefrescar = acciones::refrescar,
    ) {
        if (datos.notificaciones.isEmpty()) {
            EstadoVacio(
                Icons.Outlined.NotificationsNone,
                "Sin notificaciones",
                "Los avisos automáticos de vencimiento que se envían a tus clientes aparecerán aquí.",
            )
            return@PantallaConDatos
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(datos.notificaciones, key = { it.id }) { FilaNotificacion(it) }
        }
    }
}

@Composable
private fun FilaNotificacion(n: Notificacion) {
    val extra = ProjectGestTheme.extra
    Tarjeta(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    n.cliente?.nombre ?: "Cliente",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(n.suscripcion?.servicio, n.canal.replaceFirstChar { it.uppercase() }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = extra.textoSecundario,
                )
            }
            Spacer(Modifier.width(8.dp))
            if (n.estado == "enviado") {
                Etiqueta("Enviado", extra.exitoFondo, extra.exito)
            } else {
                Etiqueta("Falló", extra.peligroFondo, extra.peligro)
            }
        }
        if (!n.detalle.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(n.detalle, style = MaterialTheme.typography.bodySmall, color = extra.textoSecundario)
        }
        Spacer(Modifier.height(6.dp))
        Text(formatFechaHora(n.enviadoEn), style = MaterialTheme.typography.labelSmall, color = extra.textoSecundario)
    }
}
