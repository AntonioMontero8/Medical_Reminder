package com.example.medicalreminder.ui.link

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.LinkViewModel
import com.example.medicalreminder.data.LinkInfo
import com.example.medicalreminder.data.LinkPermissions
import com.example.medicalreminder.data.PatientCard
import com.example.medicalreminder.data.parseLinkCode
import com.example.medicalreminder.ui.auth.AuthTextField
import com.example.medicalreminder.ui.auth.PrimaryButton
import com.example.medicalreminder.ui.home.HomeCard
import com.example.medicalreminder.ui.home.HomeItem
import com.example.medicalreminder.ui.theme.MRColors
import kotlinx.coroutines.delay

/** "Mis pacientes": pacientes vinculados y solicitudes enviadas que esperan respuesta. */
@Composable
fun CaregiverPatientsScreen(
    vm: LinkViewModel,
    onBack: () -> Unit,
    onAddPatient: () -> Unit,
    onOpenPanel: (Int) -> Unit
) {
    // Se actualiza solo: así el cuidador ve cuándo el paciente acepta
    LaunchedEffect(Unit) {
        while (true) {
            vm.refreshLinks(silent = vm.linksLoaded)
            delay(5000)
        }
    }
    var cancelTarget by remember { mutableStateOf<LinkInfo?>(null) }

    LinkScaffold(
        title = "Mis pacientes",
        onBack = onBack,
        bottom = { PrimaryButton("VINCULAR PACIENTE", onClick = onAddPatient) }
    ) {
        BodyText("Toca un paciente para ver su tablero.")
        when {
            !vm.linksLoaded -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            vm.links.isEmpty() -> InfoCard(
                "Aún no tienes pacientes",
                "Toca «Vincular paciente» y escanea el código QR que te muestre."
            )
            else -> vm.links.forEach { l ->
                LinkListItem(
                    name = l.fullName,
                    subtitle = if (l.pendiente) "Esperando a que acepte tu solicitud" else "Toca para ver su tablero",
                    chipText = if (l.pendiente) "Pendiente" else "Vinculado",
                    chipColor = if (l.pendiente) MRColors.Orange else MRColors.MintStrong,
                    onClick = { if (l.pendiente) cancelTarget = l else onOpenPanel(l.idVinculacion) }
                )
            }
        }
    }

    cancelTarget?.let { l ->
        ConfirmDialog(
            title = "¿Retirar la solicitud?",
            text = "Dejarás de esperar la respuesta de ${l.fullName}.",
            confirmText = "Retirar solicitud",
            onConfirm = { cancelTarget = null; vm.unlink(l.idVinculacion) {} },
            onDismiss = { cancelTarget = null }
        )
    }
}

/** Módulo 2: el cuidador escanea el QR (o escribe el código) y envía la solicitud. */
@Composable
fun CaregiverScanScreen(vm: LinkViewModel, onBack: () -> Unit, onRequestSent: () -> Unit) {
    val context = LocalContext.current
    var manual by rememberSaveable { mutableStateOf("") }
    var candidate by remember { mutableStateOf<Pair<String, String>?>(null) } // código + nombre del paciente

    fun lookup(raw: String) {
        val code = parseLinkCode(raw)
        if (code == null) {
            vm.error = "El código no es válido. Debe tener 8 letras o números."
            return
        }
        vm.previewCode(code) { name -> candidate = code to name }
    }

    LinkScaffold(title = "Vincular paciente", onBack = onBack) {
        BodyText("Pídele al paciente que abra «Mis cuidadores», toque «Vincular cuidador» y escanea el código QR de su pantalla.")
        PrimaryButton(
            "ESCANEAR CÓDIGO QR",
            onClick = { scanQrCode(context, { lookup(it) }, { vm.error = it }) },
            loading = vm.busy
        )
        Text(
            "o escribe el código a mano",
            fontSize = 18.sp, color = MRColors.TextGray,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        AuthTextField(
            value = manual,
            onValueChange = { manual = it.take(12) },
            placeholder = "Código (8 caracteres)"
        )
        SecondaryButton("CONTINUAR", onClick = { lookup(manual) })
    }

    candidate?.let { (code, name) ->
        ConfirmDialog(
            title = "Vincular con $name",
            text = "Le enviaremos una solicitud. Quedarán vinculados cuando acepte y elija lo que puedes ver.",
            confirmText = "Enviar solicitud",
            onConfirm = { candidate = null; vm.requestLink(code, onRequestSent) },
            onDismiss = { candidate = null }
        )
    }
}

/** Módulo 3 (lado cuidador): tablero del paciente, solo lectura y según los permisos vigentes. */
@Composable
fun MirrorPanelScreen(vm: LinkViewModel, id: Int, onBack: () -> Unit, onUnlinked: () -> Unit) {
    var leaving by remember { mutableStateOf(false) }
    var confirmUnlink by remember { mutableStateOf(false) }

    // Panel y lista se refrescan solos: los cambios de permisos del paciente se ven en segundos
    LaunchedEffect(id) {
        vm.clearPanel()
        vm.loadPanel(id)
        while (true) {
            delay(8000)
            vm.loadPanel(id, silent = true)
            vm.refreshLinks(silent = true)
        }
    }
    // Si el paciente terminó la vinculación, se sale del panel
    LaunchedEffect(vm.links, vm.linksLoaded, leaving) {
        if (vm.linksLoaded && !leaving && vm.links.none { it.idVinculacion == id && !it.pendiente }) {
            vm.error = "Este paciente ya no está vinculado contigo."
            leaving = true
            onUnlinked()
        }
    }

    val panel = vm.panel
    LinkScaffold(
        title = "Tablero",
        onBack = onBack,
        bottom = {
            if (panel != null) {
                SecondaryButton("DESVINCULAR PACIENTE", onClick = { confirmUnlink = true }, danger = true)
            }
        }
    ) {
        if (panel == null) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        } else {
            Surface(shape = RoundedCornerShape(20.dp), color = MRColors.Orange, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Visibility, contentDescription = null, tint = MRColors.Navy)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Estás viendo el tablero de ${"${panel.nombre} ${panel.apellido}".trim()}",
                            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MRColors.Navy
                        )
                        Text("Solo lectura. Se actualiza solo.", fontSize = 15.sp, color = MRColors.Navy)
                    }
                }
            }
            val accent = MRColors.MintStrong
            HomeCard(HomeItem(Icons.Outlined.Alarm, "Próxima toma", "Aquí verás qué medicamento sigue y a qué hora."), accent)
            HomeCard(
                HomeItem(Icons.Outlined.Inventory2, "Inventario", "Dosis que le quedan de cada medicamento.",
                    locked = !panel.permisos.inventario), accent
            )
            HomeCard(
                HomeItem(Icons.Outlined.History, "Historial de tomas", "Si tomó sus medicamentos a su hora.",
                    locked = !panel.permisos.historial), accent
            )
            if (panel.ficha != null) {
                FichaCard(panel.edad, panel.ficha)
            } else {
                HomeCard(
                    HomeItem(Icons.Outlined.Lock, "Ficha médica", "Datos de salud del paciente.", locked = true), accent
                )
            }
            PermissionSummary(panel.permisos)
        }
    }

    if (confirmUnlink) {
        ConfirmDialog(
            title = "¿Desvincular paciente?",
            text = "Dejarás de ver su tablero. Para volver a vincularlos, tendrás que escanear un código nuevo.",
            confirmText = "Desvincular",
            onConfirm = {
                confirmUnlink = false
                vm.unlink(id) { leaving = true; onUnlinked() }
            },
            onDismiss = { confirmUnlink = false }
        )
    }
}

@Composable
private fun FichaCard(edad: Int?, f: PatientCard) {
    Surface(
        shape = RoundedCornerShape(20.dp), color = MRColors.FieldBg,
        border = BorderStroke(1.dp, MRColors.FieldBorder), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ficha médica", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MRColors.Navy)
            FichaRow("Edad", edad?.let { "$it años" })
            FichaRow("Sexo", f.sexo)
            FichaRow("Peso", f.peso?.let { if (it % 1.0 == 0.0) "${it.toInt()} kg" else "$it kg" })
            FichaRow("Estatura", f.estatura?.let { "$it cm" })
            FichaRow("Grupo sanguíneo", f.grupoSanguineo)
            FichaRow("Alergias", f.alergias)
            FichaRow("Padecimientos", f.padecimientos)
            FichaRow("Cirugías", f.cirugias)
            FichaRow("Enfermedades crónicas", f.enfermedadesCronicas)
            FichaRow("Desayuno", time12(f.desayuno))
            FichaRow("Comida", time12(f.comida))
            FichaRow("Cena", time12(f.cena))
        }
    }
}

@Composable
private fun FichaRow(label: String, value: String?) {
    Column {
        Text(label, fontSize = 14.sp, color = MRColors.TextGray)
        Text(value ?: "Sin registrar", fontSize = 18.sp, color = MRColors.Navy)
    }
}

@Composable
private fun PermissionSummary(p: LinkPermissions) {
    Surface(
        shape = RoundedCornerShape(20.dp), color = MRColors.MintSoft, modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Lo que el paciente te permite", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MRColors.Navy)
            listOf(
                "Ficha médica" to p.ficha,
                "Inventario" to p.inventario,
                "Historial de tomas" to p.historial,
                "Recibir alertas" to p.alertas,
                "Gestionar medicamentos" to p.gestionar
            ).forEach { (label, allowed) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (allowed) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = if (allowed) MRColors.Navy else MRColors.TextGray
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (allowed) label else "$label (sin permiso)",
                        fontSize = 17.sp, color = if (allowed) MRColors.Navy else MRColors.TextGray
                    )
                }
            }
        }
    }
}