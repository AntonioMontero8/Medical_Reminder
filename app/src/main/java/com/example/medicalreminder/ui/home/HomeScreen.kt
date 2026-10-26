package com.example.medicalreminder.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.data.UserSession
import com.example.medicalreminder.ui.auth.BrandHeader
import com.example.medicalreminder.ui.auth.UserRole
import com.example.medicalreminder.ui.theme.MRColors

/**
 * Tarjeta de la pantalla principal. Con onClick es un acceso; sin onClick es un espacio "Próximamente";
 * con locked = true muestra un candado (el paciente no dio ese permiso).
 */
data class HomeItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: (() -> Unit)? = null,
    val locked: Boolean = false
)

/** Pantalla principal: muestra contenido distinto para paciente y cuidador. */
@Composable
fun HomeScreen(
    user: UserSession,
    onLogout: () -> Unit,
    onOpenCaregivers: () -> Unit,
    onOpenPatients: () -> Unit,
    onLinkPatient: () -> Unit,
    pendingRequests: Int = 0
) {
    var confirmLogout by remember { mutableStateOf(false) }
    val isPatient = user.role == UserRole.PATIENT
    val accent = if (isPatient) MRColors.MintStrong else MRColors.Orange
    val name = user.nombre.trim()

    val items = if (isPatient) listOf(
        HomeItem(Icons.Outlined.Alarm, "Próxima toma", "Aquí verás qué medicamento sigue y a qué hora."),
        HomeItem(Icons.Outlined.Medication, "Mis medicamentos", "Agrega tus recetas tomando una foto."),
        HomeItem(Icons.Outlined.Inventory2, "Mi inventario", "Te avisaremos cuando queden pocas dosis."),
        HomeItem(Icons.Outlined.Groups, "Mis cuidadores", "Vincula y administra a quienes te acompañan.", onOpenCaregivers)
    ) else listOf(
        HomeItem(Icons.Outlined.Groups, "Mis pacientes", "Las personas a las que acompañas.", onOpenPatients),
        HomeItem(Icons.Outlined.PersonAdd, "Vincular paciente", "Escanea el código QR de un paciente.", onLinkPatient),
        HomeItem(Icons.Outlined.History, "Historial de tomas", "Consulta si tus pacientes tomaron su medicamento."),
        HomeItem(Icons.Outlined.NotificationsActive, "Alertas de reabastecimiento", "Aviso cuando a un paciente le queden pocas dosis.")
    )

    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp)) {
        Box(Modifier.padding(top = 12.dp)) { BrandHeader(compact = true) }
        Spacer(Modifier.height(20.dp))

        Text(
            if (name.isEmpty()) "Hola" else "Hola, $name",
            fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MRColors.Navy,
            maxLines = 2, overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(10.dp))
        Surface(shape = CircleShape, color = accent) {
            Text(
                if (isPatient) "Rol: Paciente" else "Rol: Cuidador",
                fontSize = 18.sp, color = MRColors.Navy,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            if (isPatient) "Aquí tendrás a la mano tus medicamentos y tus próximas tomas."
            else "Aquí podrás acompañar y supervisar a tus pacientes.",
            fontSize = 18.sp, color = MRColors.TextGray
        )
        Spacer(Modifier.height(16.dp))

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isPatient && pendingRequests > 0) {
                HomeCard(
                    HomeItem(
                        Icons.Outlined.NotificationsActive,
                        if (pendingRequests == 1) "Tienes una solicitud de vinculación" else "Tienes $pendingRequests solicitudes de vinculación",
                        "Toca para aceptarla o rechazarla.",
                        onOpenCaregivers
                    ),
                    MRColors.Orange
                )
            }
            items.forEach { HomeCard(it, accent) }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { confirmLogout = true },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = CircleShape,
            border = BorderStroke(2.dp, MRColors.Navy),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MRColors.Navy)
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("CERRAR SESIÓN", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("¿Cerrar sesión?", color = MRColors.Navy) },
            text = { Text("Tendrás que iniciar sesión de nuevo para ver tu información.", fontSize = 18.sp) },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) {
                    Text("Cerrar sesión", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancelar", fontSize = 18.sp) }
            }
        )
    }
}

@Composable
fun HomeCard(item: HomeItem, accent: Color) {
    val click = item.onClick?.takeIf { !item.locked }
    val body: @Composable () -> Unit = {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(if (item.locked) MRColors.StrengthOff else accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (item.locked) Icons.Outlined.Lock else item.icon,
                    contentDescription = null, tint = MRColors.Navy, modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MRColors.Navy)
                Text(item.subtitle, fontSize = 16.sp, color = MRColors.TextGray, modifier = Modifier.padding(top = 2.dp))
                when {
                    item.locked -> Text(
                        "Sin permiso del paciente", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = MRColors.Navy, modifier = Modifier.padding(top = 6.dp)
                    )
                    click == null -> Text(
                        "Próximamente", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = MRColors.Navy, modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
            if (click != null) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MRColors.Navy)
            }
        }
    }
    if (click != null) {
        Surface(
            onClick = click,
            shape = RoundedCornerShape(20.dp),
            color = MRColors.FieldBg,
            border = BorderStroke(1.dp, MRColors.FieldBorder),
            modifier = Modifier.fillMaxWidth()
        ) { body() }
    } else {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MRColors.FieldBg,
            border = BorderStroke(1.dp, MRColors.FieldBorder),
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}
        ) { body() }
    }
}