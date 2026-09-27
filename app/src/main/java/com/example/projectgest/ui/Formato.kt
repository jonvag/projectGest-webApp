package com.example.projectgest.ui

import com.example.projectgest.data.Suscripcion
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Currency
import java.util.Locale

private val localeEs: Locale = Locale.forLanguageTag("es-AR")

// Mismo formato que la web: toLocaleString("es-AR", { currency: "USD" }).
private val formatoMoneda: NumberFormat = NumberFormat.getCurrencyInstance(localeEs).apply {
    currency = Currency.getInstance("USD")
}

private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val formatoFechaHora = DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")
private val formatoMes = DateTimeFormatter.ofPattern("MMMM yyyy", localeEs)
private val formatoDiaLargo = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", localeEs)

fun formatMonto(monto: Double): String = formatoMoneda.format(monto)

fun parseFecha(iso: String): LocalDate = LocalDate.parse(iso.take(10))

fun formatFecha(iso: String): String = parseFecha(iso).format(formatoFecha)

fun formatFecha(fecha: LocalDate): String = fecha.format(formatoFecha)

/** Para timestamps completos (creadoEn, enviadoEn): se muestran en hora local. */
fun formatFechaHora(isoInstant: String): String = runCatching {
    OffsetDateTime.parse(isoInstant).atZoneSameInstant(ZoneId.systemDefault()).format(formatoFechaHora)
}.getOrElse { formatFecha(isoInstant) }

fun formatMes(fecha: LocalDate): String = fecha.format(formatoMes).replaceFirstChar { it.uppercase() }

fun formatHoyLargo(): String = LocalDate.now().format(formatoDiaLargo).replaceFirstChar { it.uppercase() }

enum class Urgencia { Vencido, Pronto, Normal }

fun urgenciaDe(iso: String, hoy: LocalDate = LocalDate.now()): Urgencia {
    val dias = ChronoUnit.DAYS.between(hoy, parseFecha(iso))
    return when {
        dias < 0 -> Urgencia.Vencido
        dias <= 7 -> Urgencia.Pronto
        else -> Urgencia.Normal
    }
}

/** "Vencido hace 3 días", "Hoy", "Mañana", "En 5 días" o la fecha. */
fun vencimientoRelativo(iso: String, hoy: LocalDate = LocalDate.now()): String {
    val dias = ChronoUnit.DAYS.between(hoy, parseFecha(iso))
    return when {
        dias < -1 -> "Vencido hace ${-dias} días"
        dias == -1L -> "Vencido ayer"
        dias == 0L -> "Vence hoy"
        dias == 1L -> "Vence mañana"
        dias <= 7 -> "Vence en $dias días"
        else -> formatFecha(iso)
    }
}

fun etiquetaCiclo(ciclo: String): String = when (ciclo) {
    "mensual" -> "Mensual"
    "trimestral" -> "Trimestral"
    "anual" -> "Anual"
    else -> ciclo.replaceFirstChar { it.uppercase() }
}

/** Equivalente a calcularProximaFecha de la web (lib/suscripciones-store.ts). */
fun calcularProximaFecha(fechaActual: String, ciclo: String): LocalDate {
    val fecha = parseFecha(fechaActual)
    return when (ciclo) {
        "mensual" -> fecha.plusMonths(1)
        "trimestral" -> fecha.plusMonths(3)
        else -> fecha.plusYears(1)
    }
}

// Mismas reglas que el dashboard web (lib/mock-data.ts).
fun ingresosEsperadosDelMes(suscripciones: List<Suscripcion>, hoy: LocalDate = LocalDate.now()): Double =
    suscripciones
        .filter { parseFecha(it.proximoCobro).let { f -> f.year == hoy.year && f.month == hoy.month } }
        .sumOf { it.monto }

fun cobrosVencidos(suscripciones: List<Suscripcion>, hoy: LocalDate = LocalDate.now()): List<Suscripcion> =
    suscripciones.filter { parseFecha(it.proximoCobro) < hoy }.sortedBy { it.proximoCobro }

fun proximosCobrosDeLaSemana(suscripciones: List<Suscripcion>, hoy: LocalDate = LocalDate.now()): List<Suscripcion> {
    val limite = hoy.plusDays(7)
    return suscripciones
        .filter { parseFecha(it.proximoCobro).let { f -> f >= hoy && f <= limite } }
        .sortedBy { it.proximoCobro }
}

// El DatePicker de Material trabaja en milisegundos UTC a medianoche.
fun LocalDate.aMillisUtc(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun millisUtcAFecha(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

fun iniciales(nombre: String): String =
    nombre.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }
