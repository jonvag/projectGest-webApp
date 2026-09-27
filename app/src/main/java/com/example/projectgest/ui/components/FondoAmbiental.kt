package com.example.projectgest.ui.components

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.provider.Settings
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.projectgest.R
import kotlin.math.max

/**
 * Fondo de las pantallas públicas, igual que el login de la web
 * (AmbientVideoBackground + .public-shell en globals.css): video ambiental
 * en loop, un velo del color de fondo para que el texto se lea, y tres
 * manchas de gradiente que derivan lentamente.
 */
@Composable
fun FondoAmbiental(modifier: Modifier = Modifier, contenido: @Composable BoxScope.() -> Unit) {
    val fondo = MaterialTheme.colorScheme.background
    val oscuro = fondo.luminance() < 0.5f

    Box(modifier.fillMaxSize().background(fondo)) {
        VideoEnLoop(Modifier.fillMaxSize())
        // El video es claro/pastel: en oscuro necesita mucho más velo.
        Box(Modifier.fillMaxSize().background(fondo.copy(alpha = if (oscuro) 0.86f else 0.55f)))
        ManchasDeGradiente(oscuro)
        // Fuera de un Scaffold/Surface el texto no toma el color del tema.
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            contenido()
        }
    }
}

@Composable
private fun ManchasDeGradiente(oscuro: Boolean) {
    val colores = if (oscuro) {
        listOf(Color(0xFF4A2F5C), Color(0xFF2F3F5C), Color(0xFF2F5C4A))
    } else {
        listOf(Color(0xFFFFDFC2), Color(0xFFD9C2FF), Color(0xFFC2F0D9))
    }
    val opacidad = if (oscuro) 0.28f else 0.35f

    // Mismo recorrido que @keyframes fondo-drift de la web (28 s).
    val transicion = rememberInfiniteTransition(label = "fondo-drift")
    val fase by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 28_000
                0f at 0 using FastOutSlowInEasing
                1f at 28_000
            },
            RepeatMode.Restart,
        ),
        label = "fase",
    )
    val (dx, dy, escala) = derivaEn(fase)

    Canvas(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = size.width * dx
                translationY = size.height * dy
                scaleX = escala * 1.16f // inset -8% por lado, como en la web
                scaleY = escala * 1.16f
                alpha = opacidad
            },
    ) {
        val centros = listOf(Offset(0.15f, 0.10f), Offset(0.85f, 0.90f), Offset(0.90f, 0.10f))
        val radios = listOf(0.45f, 0.45f, 0.40f)
        val diagonal = max(size.width, size.height)
        centros.forEachIndexed { i, c ->
            val centro = Offset(size.width * c.x, size.height * c.y)
            drawRect(
                Brush.radialGradient(
                    colors = listOf(colores[i], Color.Transparent),
                    center = centro,
                    radius = diagonal * radios[i],
                ),
            )
        }
    }
}

/** Interpola los keyframes 0% / 33% / 66% / 100% de fondo-drift. */
private fun derivaEn(fase: Float): Triple<Float, Float, Float> {
    val puntos = listOf(
        Triple(0f, 0f, 1f),
        Triple(0.03f, -0.025f, 1.04f),
        Triple(-0.025f, 0.03f, 1.02f),
        Triple(0f, 0f, 1f),
    )
    val tramo = (fase * 3).coerceIn(0f, 2.9999f)
    val i = tramo.toInt()
    val t = tramo - i
    val (x0, y0, s0) = puntos[i]
    val (x1, y1, s1) = puntos[i + 1]
    return Triple(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, s0 + (s1 - s0) * t)
}

private fun animacionesReducidas(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f

/**
 * Video en loop, sin sonido y recortado al centro (como `object-cover`).
 * Se pausa cuando la app pasa a segundo plano.
 */
@Composable
private fun VideoEnLoop(modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val reproductor = remember { ReproductorAmbiental(context.applicationContext, animacionesReducidas(context)) }

    DisposableEffect(lifecycleOwner) {
        val observador = LifecycleEventObserver { _, evento ->
            when (evento) {
                Lifecycle.Event.ON_RESUME -> reproductor.reanudar()
                Lifecycle.Event.ON_PAUSE -> reproductor.pausar()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observador)
            reproductor.liberar()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx -> TextureView(ctx).also(reproductor::conectar) },
    )
}

private class ReproductorAmbiental(
    private val context: Context,
    private val estatico: Boolean,
) : TextureView.SurfaceTextureListener {

    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var vista: TextureView? = null
    private var enPrimerPlano = true

    fun conectar(textureView: TextureView) {
        vista = textureView
        textureView.surfaceTextureListener = this
        textureView.surfaceTexture?.let { onSurfaceTextureAvailable(it, textureView.width, textureView.height) }
    }

    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        if (player != null) return
        val nuevaSurface = Surface(texture)
        surface = nuevaSurface
        player = MediaPlayer.create(context, R.raw.bg_ambient)?.apply {
            setSurface(nuevaSurface)
            isLooping = true
            setVolume(0f, 0f)
            setOnVideoSizeChangedListener { _, w, h -> recortarAlCentro(w, h) }
            recortarAlCentro(videoWidth, videoHeight)
            // Con "quitar animaciones" activado queda el primer cuadro fijo.
            if (!estatico && enPrimerPlano) start()
        }
    }

    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        player?.let { recortarAlCentro(it.videoWidth, it.videoHeight) }
    }

    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        liberar()
        return true
    }

    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit

    fun reanudar() {
        enPrimerPlano = true
        if (!estatico) player?.takeUnless { it.isPlaying }?.start()
    }

    fun pausar() {
        enPrimerPlano = false
        player?.takeIf { it.isPlaying }?.pause()
    }

    fun liberar() {
        player?.release()
        player = null
        surface?.release()
        surface = null
    }

    // TextureView estira el video al tamaño de la vista; esta matriz lo
    // escala de vuelta para llenar la pantalla sin deformarlo.
    private fun recortarAlCentro(anchoVideo: Int, altoVideo: Int) {
        val v = vista ?: return
        if (anchoVideo == 0 || altoVideo == 0 || v.width == 0 || v.height == 0) return
        val escala = max(v.width.toFloat() / anchoVideo, v.height.toFloat() / altoVideo)
        val sx = anchoVideo * escala / v.width
        val sy = altoVideo * escala / v.height
        v.setTransform(Matrix().apply { setScale(sx, sy, v.width / 2f, v.height / 2f) })
    }
}
