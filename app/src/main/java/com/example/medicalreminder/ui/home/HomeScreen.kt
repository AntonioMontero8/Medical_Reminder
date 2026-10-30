package com.example.medicalreminder.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
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
import kotlinx.coroutines.launch

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

/** Pantalla principal: muestra contenido distinto para paciente y cuidador, e incluye el menú hamburguesa. */
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MRColors.Background,
                drawerContentColor = MRColors.Navy
            ) {
                PatientDrawerContent(
                    user = user,
                    onCloseDrawer = { scope.launch { drawerState.close() } },
                    onLogoutClick = {
                        scope.launch { drawerState.close() }
                        confirmLogout = true
                    }
                )
            }
        }
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { scope.launch { drawerState.open() } }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Abrir menú hamburguesa",
                        tint = MRColors.Navy,
                        modifier = Modifier.size(32.dp)
                    )
                }
                BrandHeader(compact = true)
            }

            Spacer(Modifier.height(16.dp))

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

/** Contenido del menú hamburguesa con todas las opciones solicitadas para Paciente. */
@Composable
fun PatientDrawerContent(
    user: UserSession,
    onCloseDrawer: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var selectedItem by remember { mutableStateOf("") }

    var cuidadoresExpanded by remember { mutableStateOf(true) }
    var alarmasExpanded by remember { mutableStateOf(true) }
    var recetasExpanded by remember { mutableStateOf(true) }
    var tratamientosExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MRColors.Background)
            .padding(vertical = 16.dp)
    ) {
        // Cabecera del Menú
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    text = "Menú hamburguesa paciente",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MRColors.Navy
                )
                if (user.nombre.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user.nombre,
                        fontSize = 15.sp,
                        color = MRColors.TextGray
                    )
                }
            }
        }

        HorizontalDivider(color = MRColors.FieldBorder, modifier = Modifier.padding(bottom = 8.dp))

        // Opciones del Menú
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // 1. Perfil paciente
            DrawerItemRow(
                title = "Perfil paciente",
                icon = Icons.Outlined.Person,
                selected = selectedItem == "Perfil paciente",
                onClick = { selectedItem = "Perfil paciente"; onCloseDrawer() }
            )

            // 2. Historia Clínica
            DrawerItemRow(
                title = "Historia Clínica",
                icon = Icons.Outlined.Description,
                selected = selectedItem == "Historia Clínica",
                onClick = { selectedItem = "Historia Clínica"; onCloseDrawer() }
            )

            // 3. Cuidadores
            DrawerExpandableGroupRow(
                title = "Cuidadores",
                icon = Icons.Outlined.Groups,
                expanded = cuidadoresExpanded,
                onToggleExpand = { cuidadoresExpanded = !cuidadoresExpanded },
                selected = selectedItem == "Cuidadores",
                onSelectGroup = { selectedItem = "Cuidadores"; onCloseDrawer() }
            )
            if (cuidadoresExpanded) {
                DrawerSubItemRow(
                    title = "Lista de cuidadores",
                    icon = Icons.AutoMirrored.Outlined.FormatListBulleted,
                    selected = selectedItem == "Lista de cuidadores",
                    onClick = { selectedItem = "Lista de cuidadores"; onCloseDrawer() }
                )
                DrawerSubItemRow(
                    title = "Vincular cuidador",
                    icon = Icons.Outlined.PersonAdd,
                    selected = selectedItem == "Vincular cuidador",
                    onClick = { selectedItem = "Vincular cuidador"; onCloseDrawer() }
                )
            }

            // 4. Alarmas
            DrawerExpandableGroupRow(
                title = "Alarmas",
                icon = Icons.Outlined.Alarm,
                expanded = alarmasExpanded,
                onToggleExpand = { alarmasExpanded = !alarmasExpanded },
                selected = selectedItem == "Alarmas",
                onSelectGroup = { selectedItem = "Alarmas"; onCloseDrawer() }
            )
            if (alarmasExpanded) {
                DrawerSubItemRow(
                    title = "Detalles de alarma",
                    icon = Icons.Outlined.Info,
                    selected = selectedItem == "Detalles de alarma",
                    onClick = { selectedItem = "Detalles de alarma"; onCloseDrawer() }
                )
            }

            // 5. Recetas
            DrawerExpandableGroupRow(
                title = "Recetas",
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                expanded = recetasExpanded,
                onToggleExpand = { recetasExpanded = !recetasExpanded },
                selected = selectedItem == "Recetas",
                onSelectGroup = { selectedItem = "Recetas"; onCloseDrawer() }
            )
            if (recetasExpanded) {
                DrawerSubItemRow(
                    title = "Detalles de recetas",
                    icon = Icons.AutoMirrored.Outlined.Article,
                    selected = selectedItem == "Detalles de recetas",
                    onClick = { selectedItem = "Detalles de recetas"; onCloseDrawer() }
                )
            }

            // 6. Tratamientos
            DrawerExpandableGroupRow(
                title = "Tratamientos",
                icon = Icons.Outlined.Healing,
                expanded = tratamientosExpanded,
                onToggleExpand = { tratamientosExpanded = !tratamientosExpanded },
                selected = selectedItem == "Tratamientos",
                onSelectGroup = { selectedItem = "Tratamientos"; onCloseDrawer() }
            )
            if (tratamientosExpanded) {
                DrawerSubItemRow(
                    title = "Detalles de tratamiento",
                    icon = Icons.Outlined.MedicalServices,
                    selected = selectedItem == "Detalles de tratamiento",
                    onClick = { selectedItem = "Detalles de tratamiento"; onCloseDrawer() }
                )
            }

            // 7. Inventario
            DrawerItemRow(
                title = "Inventario",
                icon = Icons.Outlined.Inventory2,
                selected = selectedItem == "Inventario",
                onClick = { selectedItem = "Inventario"; onCloseDrawer() }
            )

            // 8. Historial de medicamentos
            DrawerItemRow(
                title = "Historial de medicamentos",
                icon = Icons.Outlined.History,
                selected = selectedItem == "Historial de medicamentos",
                onClick = { selectedItem = "Historial de medicamentos"; onCloseDrawer() }
            )

            // 9. Configuración
            DrawerItemRow(
                title = "Configuración",
                icon = Icons.Outlined.Settings,
                selected = selectedItem == "Configuración",
                onClick = { selectedItem = "Configuración"; onCloseDrawer() }
            )
        }

        HorizontalDivider(color = MRColors.FieldBorder, modifier = Modifier.padding(vertical = 8.dp))

        // 10. Cerrar sesión
        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
            DrawerItemRow(
                title = "Cerrar sesión",
                icon = Icons.AutoMirrored.Outlined.Logout,
                selected = false,
                onClick = onLogoutClick,
                contentColor = Color(0xFFD32F2F)
            )
        }
    }
}

@Composable
fun DrawerItemRow(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    contentColor: Color = MRColors.Navy
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
        },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MRColors.MintSoft,
            unselectedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    )
}

@Composable
fun DrawerExpandableGroupRow(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    selected: Boolean,
    onSelectGroup: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = MRColors.Navy
                )
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "Expandir $title",
                        tint = MRColors.Navy,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MRColors.Navy,
                modifier = Modifier.size(22.dp)
            )
        },
        selected = selected,
        onClick = onSelectGroup,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MRColors.MintSoft,
            unselectedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    )
}

@Composable
fun DrawerSubItemRow(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = MRColors.Navy
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MRColors.Navy,
                modifier = Modifier.size(18.dp)
            )
        },
        selected = selected,
        onClick = onClick,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MRColors.MintSoft,
            unselectedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp)
            .height(40.dp)
    )
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