package com.example.medicalreminder.ui.link

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.LinkViewModel
import com.example.medicalreminder.data.LinkInfo
import com.example.medicalreminder.data.MAX_CAREGIVERS
import com.example.medicalreminder.ui.auth.PrimaryButton
import com.example.medicalreminder.ui.theme.MRColors
import kotlinx.coroutines.delay

/** Si llega una solicitud pendiente nueva, abre su pantalla de respuesta (una sola vez por solicitud). */
@Composable
fun PendingRequestWatcher(vm: LinkViewModel, onOpenLink: (Int) -> Unit) {
    LaunchedEffect(vm.links) {
        vm.links.firstOrNull { it.pendiente && vm.markPendingHandled(it.idVinculacion) }
            ?.let { onOpenLink(it.idVinculacion) }
    }
}

/** "Mis cuidadores": lista de cuidadores vinculados y solicitudes pendientes. */
@Composable
fun PatientLinksScreen(
    vm: LinkViewModel,
    onBack: () -> Unit,
    onAddCaregiver: () -> Unit,
    onOpenLink: (Int) -> Unit
) {
    // Se actualiza solo para ver solicitudes nuevas sin tener que salir y entrar
    LaunchedEffect(Unit) {
        while (true) {
            vm.refreshLinks(silent = vm.linksLoaded)
            delay(5000)
        }
    }
    PendingRequestWatcher(vm, onOpenLink)

    val active = vm.links.count { !it.pendiente }

    LinkScaffold(
        title = "Mis cuidadores",
        onBack = onBack,
        bottom = {
            if (active < MAX_CAREGIVERS) {
                PrimaryButton("VINCULAR CUIDADOR", onClick = onAddCaregiver)
            } else {
                BodyText("Ya tienes $MAX_CAREGIVERS cuidadores. Desvincula a uno para agregar otro.", center = true)
            }
        }
    ) {
        BodyText("Tú decides qué puede ver cada cuidador y puedes cambiarlo cuando quieras.")
        Text(
            "$active de $MAX_CAREGIVERS cuidadores vinculados",
            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MRColors.Navy
        )
        when {
            !vm.linksLoaded -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
            vm.links.isEmpty() -> InfoCard(
                "Aún no tienes cuidadores",
                "Toca «Vincular cuidador» y muestra el código QR a la persona que te acompañará."
            )
            else -> vm.links.forEach { l ->
                LinkListItem(
                    name = l.fullName,
                    subtitle = if (l.pendiente) "Quiere ser tu cuidador. Toca para responder." else "Toca para cambiar sus permisos",
                    chipText = if (l.pendiente) "Pendiente" else "Activo",
                    chipColor = if (l.pendiente) MRColors.Orange else MRColors.MintStrong,
                    onClick = { onOpenLink(l.idVinculacion) }
                )
            }
        }
    }
}

/** Módulo 1: el paciente muestra su código QR. */
@Composable
fun PatientQrScreen(vm: LinkViewModel, onBack: () -> Unit, onOpenLink: (Int) -> Unit) {
    LaunchedEffect(Unit) { vm.generateCode() }
    DisposableEffect(Unit) { onDispose { vm.clearCode() } }

    // Mientras se muestra el QR, revisa si el cuidador ya envió su solicitud
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            vm.refreshLinks(silent = true)
        }
    }
    PendingRequestWatcher(vm, onOpenLink)

    val code = vm.code
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(code) {
        if (code == null) return@LaunchedEffect
        while (true) {
            now = System.currentTimeMillis()
            if (now >= code.deadlineMillis) break
            delay(1000)
        }
    }
    val remaining = if (code == null) 0L else ((code.deadlineMillis - now) / 1000).coerceAtLeast(0)
    val expired = code != null && remaining == 0L

    LinkScaffold(
        title = "Vincular cuidador",
        onBack = onBack,
        bottom = {
            PrimaryButton("GENERAR NUEVO CÓDIGO", onClick = { vm.generateCode() }, loading = vm.busy)
        }
    ) {
        BodyText(
            "Pídele a tu cuidador que abra la app, elija «Vincular paciente» y escanee este código. " +
                    "Después podrás aceptar su solicitud y elegir qué puede ver.",
            center = true
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.dp, MRColors.FieldBorder),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Box(
                Modifier
                    .padding(20.dp)
                    .size(240.dp)
                    .semantics {
                        contentDescription = if (code != null && !expired)
                            "Código QR para vincular a tu cuidador. Código para escribir a mano: ${code.display}"
                        else "Código no disponible"
                    },
                contentAlignment = Alignment.Center
            ) {
                when {
                    code != null && !expired -> QrCodeImage(code.qrText, Modifier.fillMaxSize())
                    code != null -> Text(
                        "El código venció. Genera uno nuevo.",
                        fontSize = 18.sp, color = MRColors.Navy, textAlign = TextAlign.Center
                    )
                    vm.busy -> CircularProgressIndicator(color = MRColors.Navy)
                    else -> Text(
                        "No se pudo generar el código.",
                        fontSize = 18.sp, color = MRColors.Navy, textAlign = TextAlign.Center
                    )
                }
            }
        }
        if (code != null && !expired) {
            BodyText("Código para escribir a mano", center = true, modifier = Modifier.fillMaxWidth())
            Text(
                code.display,
                fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp,
                color = MRColors.Navy, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
            BodyText(
                "Vigente por %d:%02d".format(remaining / 60, remaining % 60),
                center = true, modifier = Modifier.fillMaxWidth()
            )
        }
        BodyText("Comparte este código solo con la persona de confianza que te cuidará.", center = true)
    }
}

/** Módulo 3 (lado paciente): confirmar la solicitud eligiendo permisos, o ajustar los de un cuidador activo. */
@Composable
fun LinkResponseScreen(vm: LinkViewModel, id: Int, onBack: () -> Unit, onDone: () -> Unit) {
    val link = vm.links.firstOrNull { it.idVinculacion == id }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.refreshLinks(silent = true) }
    // Si el vínculo desapareció (otro lo canceló), se regresa; "finished" evita regresar dos veces
    LaunchedEffect(link == null, vm.linksLoaded, finished) {
        if (link == null && vm.linksLoaded && !finished) onBack()
    }

    if (link == null) {
        LinkScaffold(title = "Vinculación", onBack = onBack) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        return
    }
    LinkResponseContent(vm, link, onBack) { finished = true; onDone() }
}

@Composable
private fun LinkResponseContent(vm: LinkViewModel, link: LinkInfo, onBack: () -> Unit, onDone: () -> Unit) {
    var perms by remember(link.idVinculacion) { mutableStateOf(link.permisos) }
    var confirmReject by remember { mutableStateOf(false) }
    var confirmUnlink by remember { mutableStateOf(false) }

    if (link.pendiente) {
        LinkScaffold(
            title = "Solicitud de vinculación",
            onBack = onBack,
            bottom = {
                PrimaryButton(
                    "ACEPTAR Y VINCULAR",
                    onClick = { vm.respond(link.idVinculacion, true, perms, onDone) },
                    loading = vm.busy
                )
                SecondaryButton("RECHAZAR", onClick = { confirmReject = true }, danger = true)
            }
        ) {
            InfoCard(
                "${link.fullName} quiere ser tu cuidador",
                "Si aceptas, verá tu tablero, solo para leer, con lo que permitas abajo."
            )
            Text("¿Qué podrá ver o hacer?", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MRColors.Navy)
            PermissionList(perms) { perms = it }
            BodyText("Puedes cambiar estos permisos o desvincularlo cuando quieras desde «Mis cuidadores».")
        }
    } else {
        LinkScaffold(
            title = "Permisos de ${link.nombre}",
            onBack = onBack,
            bottom = {
                PrimaryButton(
                    "GUARDAR CAMBIOS",
                    onClick = { vm.savePermissions(link.idVinculacion, perms, onDone) },
                    loading = vm.busy
                )
                SecondaryButton("DESVINCULAR", onClick = { confirmUnlink = true }, danger = true)
            }
        ) {
            InfoCard(
                "${link.fullName} es tu cuidador",
                "Los cambios se aplican en su tablero en unos segundos."
            )
            PermissionList(perms) { perms = it }
        }
    }

    if (confirmReject) {
        ConfirmDialog(
            title = "¿Rechazar la solicitud?",
            text = "${link.fullName} no podrá ver tu información.",
            confirmText = "Rechazar",
            onConfirm = { confirmReject = false; vm.respond(link.idVinculacion, false, null, onDone) },
            onDismiss = { confirmReject = false }
        )
    }
    if (confirmUnlink) {
        ConfirmDialog(
            title = "¿Desvincular a ${link.nombre}?",
            text = "Dejará de ver tu tablero. Si quieres volver a vincularlo, tendrá que escanear un código nuevo.",
            confirmText = "Desvincular",
            onConfirm = { confirmUnlink = false; vm.unlink(link.idVinculacion, onDone) },
            onDismiss = { confirmUnlink = false }
        )
    }
}