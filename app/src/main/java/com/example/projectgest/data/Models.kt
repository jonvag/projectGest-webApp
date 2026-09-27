package com.example.projectgest.data

import kotlinx.serialization.Serializable

// Espejo de las respuestas de la API de projectGest (app/api/**/route.ts en
// la web). Las fechas de columnas @db.Date llegan como "YYYY-MM-DD"; los
// timestamps (creadoEn, enviadoEn...) llegan como ISO-8601 completo.

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val dispositivo: String = "mobile",
)

@Serializable
data class LoginResponse(
    val ok: Boolean = false,
    val rol: String? = null,
    val token: String? = null,
    val nombre: String? = null,
    val email: String? = null,
)

@Serializable
data class ErrorResponse(val error: String? = null)

@Serializable
data class Cliente(
    val id: String,
    val nombre: String,
    val empresa: String,
    val contactId: String,
    val email: String,
)

@Serializable
data class Suscripcion(
    val id: String,
    val clienteId: String,
    val servicio: String,
    val monto: Double,
    val ciclo: String,
    val proximoCobro: String,
)

@Serializable
data class SuscripcionUpdate(
    val clienteId: String,
    val servicio: String,
    val monto: Double,
    val ciclo: String,
    val proximoCobro: String,
)

@Serializable
data class Pago(
    val id: String,
    val suscripcionId: String,
    val clienteId: String,
    val servicio: String,
    val monto: Double,
    val fechaPago: String,
    val fechaCobroAnterior: String,
    val fechaProximoCobroNueva: String,
    val descripcion: String? = null,
    val referencia: String? = null,
)

@Serializable
data class NuevoPago(
    val suscripcionId: String,
    val clienteId: String,
    val servicio: String,
    val monto: Double,
    val fechaPago: String,
    val fechaCobroAnterior: String,
    val fechaProximoCobroNueva: String,
    val descripcion: String? = null,
    val referencia: String? = null,
)

@Serializable
data class ReportePago(
    val id: String,
    val suscripcionId: String,
    val clienteId: String,
    val referencia: String? = null,
    val comprobanteTipo: String? = null,
    val estado: String,
    val creadoEn: String,
    val revisadoEn: String? = null,
    val clienteNombre: String,
    val servicio: String,
    val ciclo: String,
    val proximoCobro: String,
) {
    val pendiente: Boolean get() = estado == "pendiente"
    val tieneComprobante: Boolean get() = comprobanteTipo != null
}

@Serializable
data class RevisionReporte(
    val estado: String,
    val fechaProximoCobro: String? = null,
)

@Serializable
data class NombreRef(val nombre: String)

@Serializable
data class ServicioRef(val servicio: String)

@Serializable
data class Notificacion(
    val id: String,
    val suscripcionId: String,
    val clienteId: String,
    val canal: String,
    val estado: String,
    val detalle: String? = null,
    val enviadoEn: String,
    val cliente: NombreRef? = null,
    val suscripcion: ServicioRef? = null,
)

@Serializable
data class Perfil(
    val nombre: String? = null,
    val email: String,
    val rol: String? = null,
    val banco: String? = null,
    val numeroCuenta: String? = null,
    val rif: String? = null,
)

@Serializable
data class PerfilUpdate(
    val banco: String?,
    val numeroCuenta: String?,
    val rif: String?,
)

@Serializable
data class LinkResponse(val token: String)
