package com.example.medicalreminder.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.data.UserSession
import com.example.medicalreminder.ui.auth.UserRole
import com.example.medicalreminder.ui.theme.MRColors
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Tarjeta de uso general (también la usa el tablero del cuidador). Con onClick es un acceso;
 * sin onClick es un espacio "Próximamente"; con locked = true muestra un candado.
 */
data class HomeItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: (() -> Unit)? = null,
    val locked: Boolean = false
)

private val MONTHS = listOf(
    "ENERO", "FEBRERO", "MARZO", "ABRIL", "MAYO", "JUNIO",
    "JULIO", "AGOSTO", "SEPTIEMBRE", "OCTUBRE", "NOVIEMBRE", "DICIEMBRE"
)

/** "HOY, 21 DE OCTUBRE" (sin depender de java.time para no exigir API 26). */
private fun todayLabel(): String {
    val c = Calendar.getInstance()
    return "HOY, ${c.get(Calendar.DAY_OF_MONTH)} DE ${MONTHS[c.get(Calendar.MONTH)]}"
}

/** Una opción del menú lateral. Si onClick es null, avisa que aún no está disponible. */
private data class MenuEntry(val label: String, val onClick: (() -> Unit)?, val indented: Boolean = false)

/**
 * Pantalla principal con menú lateral. Paciente: saludo + 4 accesos. Cuidador: por ahora solo fecha y saludo.
 */
@Composable
fun HomeScreen(
    user: UserSession,
    onLogout: () -> Unit,
    onOpenCaregivers: () -> Unit,
    onOpenPatients: () -> Unit,
    onEditProfile: () -> Unit,
    onScanRecipe: () -> Unit,
    onComingSoon: (String) -> Unit,
    pendingRequests: Int = 0
) {
    val isPatient = user.role == UserRole.PATIENT
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var confirmLogout by remember { mutableStateOf(false) }
    val today = remember { todayLabel() }

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    // Cierra el menú y luego ejecuta la acción elegida
    fun go(action: () -> Unit) { scope.launch { drawerState.close() }; action() }
    fun soon(label: String): () -> Unit = { onComingSoon(label) }

    val entries: List<MenuEntry> = if (isPatient) listOf(
        MenuEntry("Perfil", onEditProfile),
        MenuEntry("Historia clínica", soon("Historia clínica")),
        MenuEntry("Tratamientos y recetas", soon("Tratamientos y recetas")),
        MenuEntry("Alarmas y recordatorios", soon("Alarmas y recordatorios")),
        MenuEntry("Mi botiquín", soon("Mi botiquín")),
        MenuEntry("Diario médico", soon("Diario médico")),
        MenuEntry("Configuración", soon("Configuración"))
    ) else listOf(
        MenuEntry("Perfil", onEditProfile),
        MenuEntry("Mis Pacientes", onOpenPatients),
        MenuEntry("Historias clínicas", soon("Historias clínicas"), indented = true),
        MenuEntry("Tratamientos y recetas", soon("Tratamientos y recetas"), indented = true),
        MenuEntry("Alarmas y recordatorios", soon("Alarmas y recordatorios"), indented = true),
        MenuEntry("Inventarios de medicamentos", soon("Inventarios de medicamentos"), indented = true),
        MenuEntry("Historial de tomas / Adherencia", soon("Historial de tomas"), indented = true),
        MenuEntry("Configuración", soon("Configuración"))
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = MRColors.Background) {
                Column(Modifier.fillMaxHeight().systemBarsPadding().verticalScroll(rememberScrollState())) {
                    DrawerHeader(user, isPatient)
                    Spacer(Modifier.height(8.dp))
                    entries.forEach { e ->
                        DrawerRow(e.label, e.indented) { go(e.onClick ?: {}) }
                    }
                    DrawerRow("Cerrar sesión", false) { scope.launch { drawerState.close() }; confirmLogout = true }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    ) {
        Column(Modifier.fillMaxSize().background(MRColors.Background).systemBarsPadding().padding(horizontal = 24.dp)) {
            // Encabezado: fecha + saludo a la izquierda, botón del menú a la derecha
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(today, fontSize = 16.sp, color = MRColors.Navy)
                    Spacer(Modifier.height(4.dp))
                    val name = if (isPatient) user.nombre.trim() else "${user.nombre} ${user.apellido}".trim()
                    Text(
                        if (name.isEmpty()) "HOLA" else "HOLA, ${name.uppercase()}",
                        fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MRColors.Navy,
                        maxLines = 3, overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { scope.launch { drawerState.open() } }, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Filled.Menu, contentDescription = "Abrir menú", tint = MRColors.Navy, modifier = Modifier.size(36.dp))
                }
            }
            Spacer(Modifier.height(20.dp))

            if (isPatient) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (pendingRequests > 0) {
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
                    ActionCard(
                        Icons.Outlined.DocumentScanner, "Escanear receta médica",
                        "Toma una foto de tu prescripción y crearemos tus alarmas automáticamente. También puedes hacerlo de forma manual.",
                        onScanRecipe
                    )
                    ActionCard(
                        Icons.Outlined.Medication, "Registrar un medicamento",
                        "Configura un fármaco manual con su dosis, horario e inventario de pastillas.",
                        soon("Registrar un medicamento")
                    )
                    ActionCard(
                        Icons.Outlined.Checklist, "Crear un tratamiento completo",
                        "Agrupa varios medicamentos, rutinas e indicaciones para una condición o periodo específico. Ej. Gripe de 7 días.",
                        soon("Crear un tratamiento completo")
                    )
                    ActionCard(
                        Icons.Outlined.VolunteerActivism, "Red de apoyo / Cuidadores",
                        "Invita hasta 3 personas de confianza que te ayuden a cumplir con tu tratamiento.",
                        onOpenCaregivers
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
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

/* ------------------------- MENÚ LATERAL ------------------------- */

@Composable
private fun DrawerHeader(user: UserSession, isPatient: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.AccountCircle, contentDescription = null, tint = MRColors.Navy, modifier = Modifier.size(64.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                "${user.nombre} ${user.apellido}".trim(),
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MRColors.Navy,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(if (isPatient) "PACIENTE" else "CUIDADOR", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MRColors.TextGray)
        }
    }
}

@Composable
private fun DrawerRow(label: String, indented: Boolean, onClick: () -> Unit) {
    Text(
        label,
        fontSize = if (indented) 17.sp else 20.sp,
        fontWeight = if (indented) FontWeight.Normal else FontWeight.Medium,
        color = MRColors.Navy,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (indented) 48.dp else 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = if (indented) 44.dp else 20.dp, end = 20.dp)
            .wrapContentHeight(Alignment.CenterVertically)
    )
}

/* ------------------------- TARJETAS ------------------------- */

/** Tarjeta de la pantalla principal del paciente: contorno marcado, icono a la izquierda, título y descripción. */
@Composable
private fun ActionCard(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MRColors.Background,
        border = BorderStroke(2.5.dp, MRColors.Navy),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MRColors.Navy, modifier = Modifier.size(48.dp))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MRColors.Navy)
                Text(subtitle, fontSize = 16.sp, color = MRColors.Navy, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** Tarjeta de uso general; el tablero del cuidador la usa con candado o con "Próximamente". */
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