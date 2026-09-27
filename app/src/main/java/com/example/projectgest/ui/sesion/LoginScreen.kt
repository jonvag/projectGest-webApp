package com.example.projectgest.ui.sesion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.projectgest.ui.components.AvisoError
import com.example.projectgest.ui.components.FondoAmbiental
import com.example.projectgest.ui.theme.ProjectGestTheme

// Gradiente de acento de .public-shell (claro / oscuro) en globals.css.
private val AcentoClaro = listOf(Color(0xFFFF9D6E), Color(0xFFC9A6FF))
private val AcentoOscuro = listOf(Color(0xFFC9855C), Color(0xFF8A5FC9))

private val FormaTarjetaVidrio = RoundedCornerShape(32.dp)

@Composable
fun LoginScreen(
    ui: UiSesion,
    mensaje: String?,
    onIngresar: (email: String, password: String, biometriaDisponible: Boolean) -> Unit,
) {
    val context = LocalContext.current
    val foco = LocalFocusManager.current
    var email by rememberSaveable { mutableStateOf(ui.email.orEmpty()) }
    var password by rememberSaveable { mutableStateOf("") }
    var verPassword by rememberSaveable { mutableStateOf(false) }

    val oscuro = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val acento = if (oscuro) AcentoOscuro else AcentoClaro

    fun enviar() {
        foco.clearFocus()
        onIngresar(email, password, biometriaDisponible(context))
    }

    // Entrada de la tarjeta como el motion.form de la web:
    // opacity 0→1, y 20→0, scale 0.96→1 con ease [0.16, 1, 0.3, 1].
    val entrada = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entrada.animateTo(1f, tween(600, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
    }

    FondoAmbiental {
        Box(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 32.dp),
            ) {
                TarjetaVidrio(
                    oscuro = oscuro,
                    modifier = Modifier.graphicsLayer {
                        alpha = entrada.value
                        translationY = (1f - entrada.value) * 20.dp.toPx()
                        val escala = 0.96f + 0.04f * entrada.value
                        scaleX = escala
                        scaleY = escala
                    },
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(Brush.linearGradient(acento)),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("projectGest", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Inicia sesión para continuar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ProjectGestTheme.extra.textoSecundario,
                    )
                    Spacer(Modifier.height(8.dp))

                    if (mensaje != null) AvisoError(mensaje)

                    val coloresCampo = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (oscuro) 0.35f else 0.6f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (oscuro) 0.5f else 0.85f),
                        unfocusedBorderColor = if (oscuro) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f),
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresCampo,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentType = ContentType.EmailAddress + ContentType.Username },
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = coloresCampo,
                        visualTransformation = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { verPassword = !verPassword }) {
                                Icon(
                                    if (verPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (verPassword) "Ocultar contraseña" else "Mostrar contraseña",
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { enviar() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentType = ContentType.Password },
                    )

                    if (ui.errorLogin != null) {
                        Text(ui.errorLogin, color = ProjectGestTheme.extra.peligro, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(Modifier.height(4.dp))
                    BotonGradiente(texto = "Ingresar", colores = acento, cargando = ui.ingresando, onClick = ::enviar)
                }

                Text(
                    "Solo tendrás que ingresar tu contraseña esta vez.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ProjectGestTheme.extra.textoSecundario,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 16.dp)
                        .graphicsLayer { alpha = entrada.value },
                )
            }
        }
    }
}

/** Tarjeta translúcida con borde claro, como el formulario de login web. */
@Composable
fun TarjetaVidrio(
    oscuro: Boolean,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit,
) {
    Surface(
        shape = FormaTarjetaVidrio,
        color = if (oscuro) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.65f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, if (oscuro) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.80f)),
        // Sin sombra de elevación: en una superficie translúcida se ve a
        // través del vidrio como un rectángulo gris.
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { contenido() }
    }
}

@Composable
fun BotonGradiente(texto: String, colores: List<Color>, cargando: Boolean, onClick: () -> Unit) {
    val forma = RoundedCornerShape(14.dp)
    Button(
        onClick = onClick,
        enabled = !cargando,
        shape = forma,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color.White,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(forma)
            .background(Brush.linearGradient(colores)),
    ) {
        if (cargando) {
            CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(20.dp))
        } else {
            Text(texto, style = MaterialTheme.typography.labelLarge)
        }
    }
}
