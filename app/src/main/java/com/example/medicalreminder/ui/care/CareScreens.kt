package com.example.medicalreminder.ui.care

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.CareTeamViewModel
import com.example.medicalreminder.data.ChatMessage
import com.example.medicalreminder.data.Conversation
import com.example.medicalreminder.data.TeamInfo
import com.example.medicalreminder.data.formatMessageTime
import com.example.medicalreminder.ui.link.Avatar
import com.example.medicalreminder.ui.link.BackButton
import com.example.medicalreminder.ui.link.BodyText
import com.example.medicalreminder.ui.link.InfoCard
import com.example.medicalreminder.ui.link.LinkScaffold
import com.example.medicalreminder.ui.link.StatusChip
import com.example.medicalreminder.ui.link.WireCard
import com.example.medicalreminder.ui.medication.MedicationCard
import com.example.medicalreminder.ui.profile.fieldColors
import com.example.medicalreminder.ui.theme.MRColors
import kotlinx.coroutines.delay

/* ------------------------- BOTIQUÍN DEL PACIENTE (solo lectura) ------------------------- */

@Composable
fun PatientMedicationsScreen(vm: CareTeamViewModel, linkId: Int, patientName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    // Se actualiza solo: si el paciente cambia su inventario o los permisos, se ve en segundos
    LaunchedEffect(linkId) {
        vm.clearPatientMeds()
        vm.loadPatientMeds(linkId)
        while (true) {
            delay(8000)
            vm.loadPatientMeds(linkId, silent = true)
        }
    }
    LaunchedEffect(vm.error) {
        vm.error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); vm.error = null }
    }

    LinkScaffold(title = "Botiquín", caption = patientName, onBack = onBack) {
        StatusChip("Solo lectura · se actualiza solo", MRColors.SkyBlue, Icons.Outlined.Visibility)
        when {
            !vm.patientMedsLoaded -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = MRColors.Blue)
            vm.patientMedsDenied -> InfoCard(
                "Inventario no compartido",
                "$patientName no te dio permiso para ver su inventario. Puede cambiarlo cuando quiera.",
                icon = Icons.Outlined.Lock
            )
            vm.patientMeds.isEmpty() -> InfoCard(
                "Aún no hay medicamentos",
                "$patientName todavía no ha registrado medicamentos en su botiquín.",
                icon = Icons.Outlined.Inventory2
            )
            else -> vm.patientMeds.forEach { MedicationCard(it) }
        }
    }
}

/* ------------------------- EQUIPO DE CUIDADORES (se muestra en el Tablero) ------------------------- */

@Composable
fun TeamCard(team: TeamInfo?, onOpenChat: (Int, String) -> Unit) {
    WireCard(thin = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Groups, contentDescription = null, tint = MRColors.Blue, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(10.dp))
                Text("Cuidadores de este paciente", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MRColors.Ink)
            }
            if (team == null) {
                Text("Cargando…", fontSize = 16.sp, color = MRColors.Slate)
            } else {
                if (team.iAmMaster) StatusChip("Tú eres el cuidador maestro", MRColors.Blue, Icons.Outlined.Star)
                if (team.others.isEmpty()) {
                    Text("Eres el único cuidador de este paciente por ahora.", fontSize = 16.sp, color = MRColors.Slate)
                } else {
                    team.others.forEach { m ->
                        WireCard(onClick = { onOpenChat(m.idUsuario, m.fullName) }, thin = true, borderColor = MRColors.Slate) {
                            Row(Modifier.padding(12.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                                Avatar(m.fullName, size = 44.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(m.fullName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MRColors.Ink)
                                    Text("Toca para escribirle", fontSize = 14.sp, color = MRColors.Slate)
                                }
                                if (m.maestro) {
                                    Spacer(Modifier.width(8.dp))
                                    StatusChip("Maestro", MRColors.Blue, Icons.Outlined.Star)
                                }
                                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MRColors.Ink)
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------- MENSAJES: LISTA DE COMPAÑEROS ------------------------- */

@Composable
fun MessagesScreen(vm: CareTeamViewModel, onBack: () -> Unit, onOpenChat: (Int, String) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        while (true) {
            vm.refreshConversations(silent = vm.conversationsLoaded)
            delay(5000)
        }
    }
    LaunchedEffect(vm.error) {
        vm.error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); vm.error = null }
    }

    LinkScaffold(title = "Mensajes", caption = "Cuidadores", onBack = onBack) {
        BodyText("Escríbeles a los cuidadores que acompañan a tus mismos pacientes.")
        when {
            !vm.conversationsLoaded -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = MRColors.Blue)
            vm.conversations.isEmpty() -> InfoCard(
                "Aún no tienes compañeros",
                "Cuando otro cuidador se vincule con alguno de tus pacientes, aparecerá aquí y podrás escribirle.",
                icon = Icons.Outlined.Groups
            )
            else -> vm.conversations.forEach { c -> ConversationRow(c) { onOpenChat(c.idUsuario, c.fullName) } }
        }
    }
}

@Composable
private fun ConversationRow(c: Conversation, onClick: () -> Unit) {
    val unread = c.noLeidos > 0
    WireCard(onClick = onClick) {
        Row(Modifier.padding(16.dp).heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(c.fullName)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(c.fullName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MRColors.Ink)
                Text(
                    when {
                        c.ultimoTexto == null -> "Aún sin mensajes"
                        c.ultimoMio -> "Tú: ${c.ultimoTexto}"
                        else -> c.ultimoTexto
                    },
                    fontSize = 16.sp, color = if (unread) MRColors.Ink else MRColors.Slate,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Cuida a: " + c.pacientes.joinToString(", ") { it.fullName },
                    fontSize = 14.sp, color = MRColors.Slate, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (c.esMaestro || unread) {
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (c.esMaestro) StatusChip("Maestro", MRColors.SkyBlue, Icons.Outlined.Star)
                        if (unread) StatusChip(if (c.noLeidos == 1) "1 nuevo" else "${c.noLeidos} nuevos", MRColors.Blue)
                    }
                }
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MRColors.Ink)
        }
    }
}

/* ------------------------- CHAT ------------------------- */

@Composable
fun ChatScreen(vm: CareTeamViewModel, otherId: Int, onBack: () -> Unit) {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(otherId) {
        if (vm.chatPartner != otherId) {
            vm.openChat(otherId, vm.conversations.firstOrNull { it.idUsuario == otherId }?.fullName ?: "Chat")
        }
        vm.loadChat(otherId)
        while (true) {
            delay(3000)
            vm.loadChat(otherId, silent = true)
        }
    }
    DisposableEffect(otherId) { onDispose { vm.leaveChat(otherId) } }
    LaunchedEffect(vm.error) {
        vm.error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); vm.error = null }
    }
    val partner = vm.conversations.firstOrNull { it.idUsuario == otherId }

    Column(
        Modifier.fillMaxSize().background(MRColors.White).systemBarsPadding().imePadding().padding(horizontal = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Spacer(Modifier.width(12.dp))
            Avatar(vm.chatName, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    vm.chatName, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MRColors.Ink,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (partner?.esMaestro == true) "Cuidador maestro" else "Cuidador",
                    fontSize = 15.sp, color = MRColors.Slate
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = MRColors.SkyBlue, thickness = 1.5.dp)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                !vm.chatLoaded -> CircularProgressIndicator(Modifier.align(Alignment.Center), color = MRColors.Blue)
                vm.messages.isEmpty() -> Text(
                    "Aún no hay mensajes.\nEscribe el primero.",
                    fontSize = 18.sp, color = MRColors.Slate, textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(
                    reverseLayout = true, // lo más nuevo queda abajo y la lista se queda pegada a ello
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(vm.messages.asReversed(), key = { it.id }) { Bubble(it) }
                }
            }
        }

        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.Bottom) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(1000) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un mensaje", fontSize = 18.sp, color = MRColors.Slate) },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = fieldColors()
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    val t = text.trim()
                    if (t.isNotEmpty()) vm.send(otherId, t) { text = "" }
                },
                enabled = text.isNotBlank() && !vm.sending,
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MRColors.Blue, contentColor = MRColors.White,
                    disabledContainerColor = MRColors.IceBlue, disabledContentColor = MRColors.Slate
                )
            ) {
                Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "Enviar mensaje")
            }
        }
    }
}

@Composable
private fun Bubble(m: ChatMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.mio) Arrangement.End else Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp, topEnd = 20.dp,
                bottomStart = if (m.mio) 20.dp else 4.dp, bottomEnd = if (m.mio) 4.dp else 20.dp
            ),
            color = if (m.mio) MRColors.Blue else MRColors.IceBlue,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(m.contenido, fontSize = 18.sp, color = if (m.mio) MRColors.White else MRColors.Ink)
                Text(
                    formatMessageTime(m.enviadoMs), fontSize = 12.sp,
                    color = if (m.mio) MRColors.IceBlue else MRColors.Ink.copy(alpha = 0.75f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}