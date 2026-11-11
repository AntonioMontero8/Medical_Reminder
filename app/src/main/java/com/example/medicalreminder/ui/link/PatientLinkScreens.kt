package com.example.medicalreminder.ui.link

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    val pending = vm.links.count { it.pendiente }

    LinkScaffold(
        title = "Mis cuidadores",
        caption = "Red de apoyo",
        onBack = onBack,
        bottom = {
            if (active < MAX_CAREGIVERS) {
                PrimaryButton("VINCULAR CUIDADOR", onClick = onAddCaregiver)
            } else {
                BodyText("Ya tienes $MAX_CAREGIVERS cuidadores. Desvincula a uno para agregar otro.", center = true)
            }
        }
    ) {
        SlotsCard(active, pending)
        BodyText("Tú decides qué puede ver cada cuidador y puedes cambiarlo cuando quieras.")
        when {
            !vm.linksLoaded -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = MRColors.Blue)
            vm.links.isEmpty() -> InfoCard(
                "Aún no tienes cuidadores",
                "Toca «Vincular cuidador» y muestra el código QR a la persona que te acompañará.",
                icon = Icons.Outlined.PersonAdd
            )
            else -> vm.links.forEach { l ->
                LinkListItem(
                    name = l.fullName,
                    subtitle = if (l.pendiente) "Quiere ser tu cuidador. Toca para responder." else "Toca para cambiar sus permisos",
                    chipText = if (l.pendiente) "Pendiente" else if (l.maestro) "Maestro" else "Activo",
                    // Pendiente = celeste (informativo), maestro = azul, activo = verde (vínculo en buen estado)
                    chipColor = if (l.pendiente) MRColors.SkyBlue else if (l.maestro) MRColors.Blue else MRColors.Green,
                    chipIcon = if (l.pendiente) Icons.Outlined.Schedule else if (l.maestro) Icons.Outlined.Star else Icons.Outlined.CheckCircle,
                    onClick = { onOpenLink(l.idVinculacion) }
                )
            }
        }
    }
}

/** «2 de 3 cuidadores»: un círculo por cada lugar (verde = vinculado, celeste = solicitud pendiente, vacío = libre). */
@Composable
private fun SlotsCard(active: Int, pending: Int) {
    WireCard(thin = true) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "$active de $MAX_CAREGIVERS cuidadores",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MRColors.Ink
                )
                Text(
                    if (pending > 0) "$pending solicitud" + (if (pending == 1) "" else "es") + " por responder" else "vinculados",
                    fontSize = 16.sp, color = MRColors.Slate
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(MAX_CAREGIVERS) { i ->
                    val fill = when {
                        i < active -> MRColors.Green
                        i < active + pending -> MRColors.SkyBlue
                        else -> MRColors.White
                    }
                    Box(
                        Modifier.size(22.dp).clip(CircleShape).background(fill)
                            .border(2.dp, if (fill == MRColors.White) MRColors.Slate else fill, CircleShape)
                    )
                }
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
        caption = "Red de apoyo",
        onBack = onBack,
        bottom = {
            PrimaryButton("GENERAR NUEVO CÓDIGO", onClick = { vm.generateCode() }, loading = vm.busy)
        }
    ) {
        InfoCard(
            "Cómo vincularlo",
            "Pídele a tu cuidador que abra la app, elija «Vincular paciente» y escanee este código. " +
                    "Después podrás aceptar su solicitud y elegir qué puede ver.",
            icon = Icons.Outlined.QrCode2
        )
        WireCard(modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 300.dp)) {
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
                        fontSize = 18.sp, color = MRColors.Ink, textAlign = TextAlign.Center
                    )
                    vm.busy -> CircularProgressIndicator(color = MRColors.Blue)
                    else -> Text(
                        "No se pudo generar el código.",
                        fontSize = 18.sp, color = MRColors.Ink, textAlign = TextAlign.Center
                    )
                }
            }
        }
        if (code != null && !expired) {
            BodyText("Código para escribir a mano", center = true, modifier = Modifier.fillMaxWidth())
            Text(
                code.display,
                fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 4.sp,
                color = MRColors.Blue, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
            // Verde mientras sobra tiempo; coral el último minuto
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (remaining > 60) {
                    StatusChip("Vigente por %d:%02d".format(remaining / 60, remaining % 60), MRColors.Green, Icons.Outlined.CheckCircle)
                } else {
                    StatusChip("Vence en %d s".format(remaining), MRColors.Blush, Icons.Outlined.Warning, MRColors.Coral)
                }
            }
        } else if (code != null) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                StatusChip("Código vencido", MRColors.Red, Icons.Outlined.ErrorOutline)
            }
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
    var confirmMaster by remember { mutableStateOf(false) }

    if (link.pendiente) {
        LinkScaffold(
            title = "Solicitud de vinculación",
            caption = "Red de apoyo",
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
            PersonBanner(link.fullName, "Quiere ser tu cuidador", "Si aceptas, verá tu tablero, solo para leer, con lo que permitas abajo.")
            SectionLabel("¿Qué podrá ver o hacer?")
            PermissionList(perms) { perms = it }
            BodyText("Puedes cambiar estos permisos o desvincularlo cuando quieras desde «Mis cuidadores».")
            if (vm.links.none { !it.pendiente && it.maestro }) {
                BodyText("El primer cuidador que aceptes será tu cuidador maestro; después podrás elegir a otro.")
            }
        }
    } else {
        LinkScaffold(
            title = "Permisos de ${link.nombre}",
            caption = "Mis cuidadores",
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
            PersonBanner(
                link.fullName, if (link.maestro) "Cuidador maestro" else "Cuidador activo",
                "Los cambios se aplican en su tablero en unos segundos.",
                chipColor = if (link.maestro) MRColors.Blue else MRColors.Green
            )
            if (link.maestro) {
                BodyText("Es el cuidador principal de tu equipo. Puedes elegir a otro cuando quieras.")
            } else {
                SecondaryButton("HACER CUIDADOR MAESTRO", onClick = { confirmMaster = true })
            }
            SectionLabel("Permisos")
            PermissionList(perms) { perms = it }
        }
    }

    if (confirmReject) {
        ConfirmDialog(
            title = "¿Rechazar la solicitud?",
            text = "${link.fullName} no podrá ver tu información.",
            confirmText = "Rechazar",
            danger = true,
            onConfirm = { confirmReject = false; vm.respond(link.idVinculacion, false, null, onDone) },
            onDismiss = { confirmReject = false }
        )
    }
    if (confirmMaster) {
        ConfirmDialog(
            title = "¿Hacer maestro a ${link.nombre}?",
            text = "Será el cuidador principal de tu equipo. Solo puede haber uno: el maestro actual dejará de serlo.",
            confirmText = "Hacer maestro",
            onConfirm = { confirmMaster = false; vm.setMaster(link.idVinculacion) {} },
            onDismiss = { confirmMaster = false }
        )
    }
    if (confirmUnlink) {
        ConfirmDialog(
            title = "¿Desvincular a ${link.nombre}?",
            text = "Dejará de ver tu tablero. Si quieres volver a vincularlo, tendrá que escanear un código nuevo.",
            confirmText = "Desvincular",
            danger = true,
            onConfirm = { confirmUnlink = false; vm.unlink(link.idVinculacion, onDone) },
            onDismiss = { confirmUnlink = false }
        )
    }
}

/** Tarjeta con el avatar de la persona, su estado y una explicación corta. */
@Composable
private fun PersonBanner(name: String, status: String, text: String, chipColor: Color = MRColors.SkyBlue) {
    WireCard {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(name, size = 56.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MRColors.Ink)
                StatusChip(status, chipColor)
                Text(text, fontSize = 16.sp, color = MRColors.Slate)
            }
        }
    }
}