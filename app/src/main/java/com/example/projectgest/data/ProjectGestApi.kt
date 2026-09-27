package com.example.projectgest.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ProjectGestApi {
    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<ResponseBody>

    @GET("api/clientes")
    suspend fun clientes(): List<Cliente>

    @GET("api/suscripciones")
    suspend fun suscripciones(): List<Suscripcion>

    @PUT("api/suscripciones/{id}")
    suspend fun actualizarSuscripcion(
        @Path("id") id: String,
        @Body body: SuscripcionUpdate,
    ): Suscripcion

    @GET("api/suscripciones/{id}/link")
    suspend fun linkDePago(@Path("id") id: String): LinkResponse

    @GET("api/pagos")
    suspend fun pagos(): List<Pago>

    @POST("api/pagos")
    suspend fun registrarPago(@Body body: NuevoPago): Pago

    @GET("api/reportes-pago")
    suspend fun reportesPago(): List<ReportePago>

    @PATCH("api/reportes-pago/{id}")
    suspend fun revisarReporte(
        @Path("id") id: String,
        @Body body: RevisionReporte,
    ): ReportePago

    @GET("api/notificaciones")
    suspend fun notificaciones(): List<Notificacion>

    @GET("api/perfil")
    suspend fun perfil(): Perfil

    @PATCH("api/perfil")
    suspend fun actualizarPerfil(@Body body: PerfilUpdate): Perfil
}
