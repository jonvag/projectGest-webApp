package com.example.projectgest.ui.cobros

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.projectgest.data.Repository
import com.example.projectgest.data.Resultado
import com.example.projectgest.data.Suscripcion
import com.example.projectgest.ui.aMillisUtc
import com.example.projectgest.ui.calcularProximaFecha
import com.example.projectgest.ui.components.BotonPrincipal
import com.example.projectgest.ui.components.EtiquetaUrgencia
import com.example.projectgest.ui.components.FilaDato
import com.example.projectgest.ui.components.Tarjeta
import com.example.projectgest.ui.etiquetaCiclo
import com.example.projectgest.ui.formatFecha
import com.example.projectgest.ui.formatMonto
import com.example.projectgest.ui.millisUtcAFecha
import com.example.projectgest.ui.theme.EstiloCifra
import com.example.projectgest.ui.theme.ProjectGestTheme
import com.example.projectgest.ui.urgenciaDe
import com.example.projectgest.ui.vencimientoRelativo
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Tarjeta de una suscripción con sus dos acciones rápidas. */
@Composable
fun TarjetaSuscripcion(
    suscripcion: Suscripcion,
    nombreCliente: String?,
    onRegistrarPago: () -> Unit,
    onCompartirLink: () -> Unit,
    compartiendo: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Tarjeta(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                if (nombreCliente != null) {
                    Text(
                        nombreCliente,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    suscripcion.servicio,
                    style = if (nombreCliente != null) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                    color = if (nombreCliente != null) ProjectGestTheme.extra.textoSecundario else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatMonto(suscripcion.monto),
                    style = MaterialTheme.typography.titleMedium.merge(EstiloCifra),
                )
                Text(
                    etiquetaCiclo(suscripcion.ciclo),
                    style = MaterialTheme.typography.bodySmall,
                    color = ProjectGestTheme.extra.textoSecundario,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            EtiquetaUrgencia(
                vencimientoRelativo(suscripcion.proximoCobro),
                urgenciaDe(suscripcion.proximoCobro),
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onCompartirLink, enabled = !compartiendo) {
                if (compartiendo) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(Icons.Outlined.Share, contentDescription = "Compartir link de pago")
                }
            }
            FilledTonalButton(onClick = onRegistrarPago, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Outlined.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Registrar pago")
            }
        }
    }
}

/** Abre el menú nativo de compartir con el link público de reporte de pago. */
fun compartirLinkDePago(context: Context, url: String, nombreCliente: String, servicio: String) {
    val texto = "Hola $nombreCliente, por aquí puedes reportar tu pago de $servicio: $url"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texto)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir link de pago"))
}

/** Estado + acción para compartir el link de una suscripción desde cualquier pantalla. */
class CompartirLink(
    private val repository: Repository,
    private val context: Context,
    private val onError: (String) -> Unit,
) {
    var enCurso by mutableStateOf<String?>(null)
        private set

    suspend fun compartir(suscripcion: Suscripcion, nombreCliente: String) {
        enCurso = suscripcion.id
        when (val r = repository.linkDePago(suscripcion.id)) {
            is Resultado.Ok -> compartirLinkDePago(context, r.valor, nombreCliente, suscripcion.servicio)
            is Resultado.Error -> onError(r.mensaje)
        }
        enCurso = null
    }
}

@Composable
fun rememberCompartirLink(repository: Repository, onError: (String) -> Unit): CompartirLink {
    val context = LocalContext.current
    return remember(repository) { CompartirLink(repository, context, onError) }
}

/** Campo de solo lectura que abre un DatePicker de Material al tocarlo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(
    etiqueta: String,
    fecha: LocalDate,
    onCambio: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    ayuda: String? = null,
) {
    var abierto by remember { mutableStateOf(false) }
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    LaunchedEffect(presionado) { if (presionado) abierto = true }

    OutlinedTextField(
        value = formatFecha(fecha),
        onValueChange = {},
        readOnly = true,
        label = { Text(etiqueta) },
        supportingText = ayuda?.let { { Text(it) } },
        trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = "Elegir fecha") },
        interactionSource = interaccion,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth(),
    )

    if (abierto) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = fecha.aMillisUtc())
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { onCambio(millisUtcAFecha(it)) }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } },
        ) { DatePicker(state = estado) }
    }
}

/** Equivalente móvil del "Confirmar pago" de la web. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaRegistrarPago(
    suscripcion: Suscripcion,
    nombreCliente: String,
    repository: Repository,
    onCerrar: () -> Unit,
    onRegistrado: () -> Unit,
) {
    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var fecha by remember(suscripcion.id) {
        mutableStateOf(calcularProximaFecha(suscripcion.proximoCobro, suscripcion.ciclo))
    }
    var referencia by rememberSaveable(suscripcion.id) { mutableStateOf("") }
    var descripcion by rememberSaveable(suscripcion.id) { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = { if (!guardando) onCerrar() },
        sheetState = estadoHoja,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Registrar pago", style = MaterialTheme.typography.headlineSmall)
            Column {
                FilaDato("Cliente", nombreCliente)
                FilaDato("Servicio", suscripcion.servicio)
                FilaDato("Monto", formatMonto(suscripcion.monto))
                FilaDato("Cobro que se paga", formatFecha(suscripcion.proximoCobro))
            }
            CampoFecha(
                etiqueta = "Próximo cobro",
                fecha = fecha,
                onCambio = { fecha = it },
                ayuda = "Calculado según el ciclo ${etiquetaCiclo(suscripcion.ciclo).lowercase()}",
            )
            OutlinedTextField(
                value = referencia,
                onValueChange = { referencia = it },
                label = { Text("Referencia (opcional)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción (opcional)") },
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) {
                Text(error!!, color = ProjectGestTheme.extra.peligro, style = MaterialTheme.typography.bodySmall)
            }
            BotonPrincipal(
                texto = "Confirmar pago de ${formatMonto(suscripcion.monto)}",
                cargando = guardando,
                onClick = {
                    guardando = true
                    error = null
                    scope.launch {
                        when (val r = repository.registrarPago(suscripcion, fecha, referencia, descripcion)) {
                            is Resultado.Ok -> {
                                estadoHoja.hide()
                                onRegistrado()
                            }
                            is Resultado.Error -> error = r.mensaje
                        }
                        guardando = false
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}
