package com.example.projectgest.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate

/** Todo lo que la app muestra, cargado de una vez y compartido entre pestañas. */
data class EstadoDatos(
    val clientes: List<Cliente> = emptyList(),
    val suscripciones: List<Suscripcion> = emptyList(),
    val pagos: List<Pago> = emptyList(),
    val reportes: List<ReportePago> = emptyList(),
    val notificaciones: List<Notificacion> = emptyList(),
    val perfil: Perfil? = null,
    val cargadoAlgunaVez: Boolean = false,
    val cargando: Boolean = false,
    val error: String? = null,
) {
    fun cliente(id: String): Cliente? = clientes.find { it.id == id }
    fun nombreCliente(id: String): String = cliente(id)?.nombre ?: "Cliente desconocido"
    fun suscripcion(id: String): Suscripcion? = suscripciones.find { it.id == id }
    val reportesPendientes: Int get() = reportes.count { it.pendiente }
}

sealed interface Resultado<out T> {
    data class Ok<T>(val valor: T) : Resultado<T>
    data class Error(val mensaje: String) : Resultado<Nothing>
}

class Repository(
    private val api: ProjectGestApi,
    private val json: Json,
    private val baseUrl: String,
) {
    private val _estado = MutableStateFlow(EstadoDatos())
    val estado: StateFlow<EstadoDatos> = _estado.asStateFlow()

    suspend fun refrescar() {
        _estado.update { it.copy(cargando = true, error = null) }
        try {
            coroutineScope {
                val clientes = async { api.clientes() }
                val suscripciones = async { api.suscripciones() }
                val pagos = async { api.pagos() }
                val reportes = async { api.reportesPago() }
                val notificaciones = async { api.notificaciones() }
                val perfil = async { api.perfil() }
                _estado.value = EstadoDatos(
                    clientes = clientes.await().sortedBy { it.nombre.lowercase() },
                    suscripciones = suscripciones.await(),
                    pagos = pagos.await().sortedByDescending { it.fechaPago },
                    reportes = reportes.await(),
                    notificaciones = notificaciones.await(),
                    perfil = perfil.await(),
                    cargadoAlgunaVez = true,
                )
            }
        } catch (e: Exception) {
            _estado.update { it.copy(cargando = false, error = mensajeDe(e)) }
        }
    }

    fun limpiar() {
        _estado.value = EstadoDatos()
    }

    /**
     * Mismo flujo que "Confirmar pago" en la web: primero mueve el próximo
     * cobro de la suscripción y después registra el pago en el historial.
     */
    suspend fun registrarPago(
        suscripcion: Suscripcion,
        fechaProximoCobro: LocalDate,
        referencia: String,
        descripcion: String,
    ): Resultado<Unit> = ejecutar {
        val actualizada = api.actualizarSuscripcion(
            suscripcion.id,
            SuscripcionUpdate(
                clienteId = suscripcion.clienteId,
                servicio = suscripcion.servicio,
                monto = suscripcion.monto,
                ciclo = suscripcion.ciclo,
                proximoCobro = fechaProximoCobro.toString(),
            ),
        )
        val pago = api.registrarPago(
            NuevoPago(
                suscripcionId = suscripcion.id,
                clienteId = suscripcion.clienteId,
                servicio = suscripcion.servicio,
                monto = suscripcion.monto,
                fechaPago = LocalDate.now().toString(),
                fechaCobroAnterior = suscripcion.proximoCobro,
                fechaProximoCobroNueva = fechaProximoCobro.toString(),
                descripcion = descripcion.trim().ifEmpty { null },
                referencia = referencia.trim().ifEmpty { null },
            ),
        )
        _estado.update { estado ->
            estado.copy(
                suscripciones = estado.suscripciones.map { if (it.id == actualizada.id) actualizada else it },
                pagos = listOf(pago) + estado.pagos,
            )
        }
    }

    suspend fun aprobarReporte(reporte: ReportePago, fechaProximoCobro: LocalDate): Resultado<Unit> =
        ejecutar {
            api.revisarReporte(reporte.id, RevisionReporte("verificado", fechaProximoCobro.toString()))
            // Aprobar crea un pago y mueve la suscripción del lado del
            // servidor, así que se recarga todo para reflejarlo.
            refrescar()
        }

    suspend fun rechazarReporte(reporte: ReportePago): Resultado<Unit> = ejecutar {
        val actualizado = api.revisarReporte(reporte.id, RevisionReporte("rechazado"))
        _estado.update { estado ->
            estado.copy(reportes = estado.reportes.map { if (it.id == actualizado.id) actualizado else it })
        }
    }

    suspend fun linkDePago(suscripcionId: String): Resultado<String> = ejecutar {
        val token = api.linkDePago(suscripcionId).token
        "${baseUrl.trimEnd('/')}/reportar/$token"
    }

    suspend fun actualizarPerfil(banco: String, numeroCuenta: String, rif: String): Resultado<Unit> =
        ejecutar {
            val perfil = api.actualizarPerfil(
                PerfilUpdate(
                    banco = banco.trim().ifEmpty { null },
                    numeroCuenta = numeroCuenta.trim().ifEmpty { null },
                    rif = rif.trim().ifEmpty { null },
                ),
            )
            // El PATCH no devuelve `rol`: se conserva el que ya teníamos.
            _estado.update { it.copy(perfil = perfil.copy(rol = it.perfil?.rol ?: perfil.rol)) }
        }

    fun urlComprobante(reporteId: String): String =
        "${baseUrl.trimEnd('/')}/api/reportes-pago/$reporteId/comprobante"

    private suspend fun <T> ejecutar(bloque: suspend () -> T): Resultado<T> =
        try {
            Resultado.Ok(bloque())
        } catch (e: Exception) {
            Resultado.Error(mensajeDe(e))
        }

    fun mensajeDe(e: Throwable): String = when (e) {
        is HttpException -> {
            val cuerpo = e.response()?.errorBody()?.string()
            val delServidor = cuerpo?.let {
                runCatching { json.decodeFromString<ErrorResponse>(it).error }.getOrNull()
            }
            delServidor ?: "Error del servidor (${e.code()})."
        }
        is IOException -> "Sin conexión con el servidor. Revisa tu internet."
        else -> e.message ?: "Ocurrió un error inesperado."
    }
}
