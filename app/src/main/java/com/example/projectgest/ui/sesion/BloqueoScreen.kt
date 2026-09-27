package com.example.projectgest.ui.sesion

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.projectgest.ui.components.BotonPrincipal
import com.example.projectgest.ui.theme.ProjectGestTheme

@Composable
fun BloqueoScreen(
    nombre: String?,
    onDesbloqueado: () -> Unit,
    onUsarOtraCuenta: () -> Unit,
) {
    val activity = LocalContext.current as FragmentActivity
    var error by remember { mutableStateOf<String?>(null) }

    fun pedir() {
        if (!biometriaDisponible(activity)) {
            // Se quitó la huella/PIN del teléfono: no hay con qué verificar.
            onDesbloqueado()
            return
        }
        error = null
        pedirBiometria(
            activity = activity,
            titulo = "Desbloquear projectGest",
            onExito = onDesbloqueado,
            onError = { error = it },
        )
    }

    // Pedir la huella apenas aparece la pantalla, sin un toque extra.
    LaunchedEffect(Unit) { pedir() }

    // "Atrás" no debe llegar a la navegación de la app que está debajo.
    BackHandler { activity.moveTaskToBack(true) }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = 420.dp).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(72.dp).background(ProjectGestTheme.extra.acentoFondo, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Fingerprint,
                    contentDescription = null,
                    tint = ProjectGestTheme.extra.acento,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (nombre != null) "Hola de nuevo, ${nombre.substringBefore(' ')}" else "Hola de nuevo",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(
                "Confirma tu identidad para ver tus cobros.",
                style = MaterialTheme.typography.bodyMedium,
                color = ProjectGestTheme.extra.textoSecundario,
                textAlign = TextAlign.Center,
            )
            if (error != null) {
                Text(error!!, color = ProjectGestTheme.extra.peligro, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            BotonPrincipal(texto = "Desbloquear", onClick = ::pedir)
            TextButton(onClick = onUsarOtraCuenta) { Text("Usar otra cuenta") }
        }
    }
}
