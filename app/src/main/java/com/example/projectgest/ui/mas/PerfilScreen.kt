package com.example.projectgest.ui.mas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.projectgest.data.EstadoDatos
import com.example.projectgest.data.Perfil
import com.example.projectgest.data.Resultado
import com.example.projectgest.ui.LocalAcciones
import com.example.projectgest.ui.components.BotonPrincipal
import com.example.projectgest.ui.components.FilaDato
import com.example.projectgest.ui.components.PantallaConDatos
import com.example.projectgest.ui.components.Tarjeta
import com.example.projectgest.ui.components.TituloSeccion
import com.example.projectgest.ui.theme.ProjectGestTheme
import kotlinx.coroutines.launch

@Composable
fun PerfilScreen(datos: EstadoDatos) {
    val acciones = LocalAcciones.current
    PantallaConDatos(
        cargadoAlgunaVez = datos.cargadoAlgunaVez,
        cargando = false,
        error = datos.error,
        onRefrescar = acciones::refrescar,
    ) {
        val perfil = datos.perfil
        if (perfil != null) FormularioPerfil(perfil)
    }
}

@Composable
private fun FormularioPerfil(perfil: Perfil) {
    val acciones = LocalAcciones.current
    val scope = rememberCoroutineScope()
    val foco = LocalFocusManager.current

    var banco by rememberSaveable { mutableStateOf(perfil.banco.orEmpty()) }
    var cuenta by rememberSaveable { mutableStateOf(perfil.numeroCuenta.orEmpty()) }
    var rif by rememberSaveable { mutableStateOf(perfil.rif.orEmpty()) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val hayCambios = banco != perfil.banco.orEmpty() || cuenta != perfil.numeroCuenta.orEmpty() || rif != perfil.rif.orEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Tarjeta(Modifier.fillMaxWidth()) {
            FilaDato("Nombre", perfil.nombre ?: "—")
            FilaDato("Email", perfil.email)
        }

        TituloSeccion("Datos bancarios", Modifier.padding(horizontal = 4.dp))
        Text(
            "Se muestran a tus clientes en la página donde reportan sus pagos.",
            style = MaterialTheme.typography.bodySmall,
            color = ProjectGestTheme.extra.textoSecundario,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        CampoPerfil("Banco", banco, { banco = it })
        CampoPerfil("Número de cuenta", cuenta, { cuenta = it }, KeyboardType.Number)
        CampoPerfil("RIF", rif, { rif = it }, imeAction = ImeAction.Done)

        if (error != null) {
            Text(error!!, color = ProjectGestTheme.extra.peligro, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(4.dp))
        BotonPrincipal(
            texto = "Guardar cambios",
            enabled = hayCambios,
            cargando = guardando,
            onClick = {
                foco.clearFocus()
                guardando = true
                error = null
                scope.launch {
                    when (val r = acciones.repository.actualizarPerfil(banco, cuenta, rif)) {
                        is Resultado.Ok -> acciones.mostrar("Datos bancarios guardados")
                        is Resultado.Error -> error = r.mensaje
                    }
                    guardando = false
                }
            },
        )
    }
}

@Composable
private fun CampoPerfil(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    tipo: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = tipo, imeAction = imeAction),
        modifier = Modifier.fillMaxWidth(),
    )
}
