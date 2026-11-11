package com.example.medicalreminder.ui.link

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.LinkViewModel
import com.example.medicalreminder.data.LinkPermissions
import com.example.medicalreminder.ui.auth.BrandHeader
import com.example.medicalreminder.ui.theme.MRColors

/* ------------------------- ESTRUCTURA DE PANTALLA ------------------------- */

/** Botón de regreso: flecha dentro de un círculo con el mismo contorno que las tarjetas. */
@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onBack,
        shape = CircleShape,
        color = MRColors.White,
        border = BorderStroke(2.dp, MRColors.Ink),
        modifier = modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = MRColors.Ink)
        }
    }
}

/**
 * Estructura común de las pantallas interiores (estilo del wireframe «Cold Start»):
 * flecha atrás + logo, texto pequeño opcional (caption), título grande en mayúsculas,
 * contenido con scroll y botones fijos abajo.
 */
@Composable
fun LinkScaffold(
    title: String,
    onBack: () -> Unit,
    caption: String? = null,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(MRColors.White).systemBarsPadding().imePadding().padding(horizontal = 24.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onBack)
            Spacer(Modifier.weight(1f))
            BrandHeader(compact = true)
        }
        Spacer(Modifier.height(16.dp))
        if (caption != null) {
            Text(caption.uppercase(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MRColors.Slate)
            Spacer(Modifier.height(2.dp))
        }
        Text(
            title.uppercase(), fontSize = 30.sp, lineHeight = 34.sp,
            fontWeight = FontWeight.ExtraBold, color = MRColors.Ink
        )
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
        if (bottom != null) {
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = bottom)
        }
        Spacer(Modifier.height(16.dp))
    }
}

/** Muestra los errores del LinkViewModel como aviso. Se coloca una sola vez en la navegación. */
@Composable
fun LinkErrorToast(vm: LinkViewModel) {
    val context = LocalContext.current
    LaunchedEffect(vm.error) {
        vm.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.error = null
        }
    }
}

/** Encabezado pequeño para agrupar contenido («DATOS FÍSICOS», «HORARIOS»…). */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MRColors.Slate,
        letterSpacing = 1.sp, modifier = modifier.fillMaxWidth().padding(top = 8.dp)
    )
}

@Composable
fun BodyText(text: String, modifier: Modifier = Modifier, center: Boolean = false) {
    Text(
        text, fontSize = 18.sp, color = MRColors.Slate, modifier = modifier,
        textAlign = if (center) TextAlign.Center else TextAlign.Start
    )
}

/* ------------------------- TARJETAS ------------------------- */

/**
 * Tarjeta base del diseño: fondo blanco y contorno oscuro. [thin] = versión más ligera para
 * bloques dentro de formularios. Con [onClick] es un acceso; sin él, solo muestra información.
 */
@Composable
fun WireCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = MRColors.White,
    borderColor: Color = MRColors.Ink,
    thin: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(if (thin) 20.dp else 28.dp)
    val border = BorderStroke(if (thin) 1.5.dp else 2.5.dp, borderColor)
    if (onClick != null) {
        Surface(onClick = onClick, shape = shape, color = color, border = border, modifier = modifier.fillMaxWidth(), content = content)
    } else {
        Surface(shape = shape, color = color, border = border, modifier = modifier.fillMaxWidth(), content = content)
    }
}

/** Círculo con las iniciales de la persona. */
@Composable
fun Avatar(name: String, size: Dp = 52.dp, color: Color = MRColors.SkyBlue) {
    val initials = name.trim().split(" ").filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
    Box(Modifier.size(size).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(initials, fontSize = (size.value * 0.38f).sp, fontWeight = FontWeight.ExtraBold, color = MRColors.Ink)
    }
}

/** Etiqueta de estado. El color de la letra se elige solo según el fondo para mantener el contraste. */
@Composable
fun StatusChip(text: String, color: Color, icon: ImageVector? = null, borderColor: Color? = null) {
    val content = if (color.luminance() < 0.2f) MRColors.White else MRColors.Ink
    Surface(shape = CircleShape, color = color, border = borderColor?.let { BorderStroke(1.dp, it) }) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = content)
        }
    }
}

/** Aviso informativo (celeste). Con color = Blush es una advertencia suave. */
@Composable
fun InfoCard(title: String, text: String, icon: ImageVector? = null, color: Color = MRColors.IceBlue) {
    val warning = color == MRColors.Blush
    Surface(
        shape = RoundedCornerShape(20.dp), color = color,
        border = if (warning) BorderStroke(1.5.dp, MRColors.Coral) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = if (warning) MRColors.Red else MRColors.Blue, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MRColors.Ink)
                Text(text, fontSize = 17.sp, color = MRColors.Ink, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** Fila de lista: avatar con iniciales, nombre, descripción y estado. */
@Composable
fun LinkListItem(
    name: String,
    subtitle: String,
    chipText: String,
    chipColor: Color,
    onClick: () -> Unit,
    chipIcon: ImageVector? = null
) {
    WireCard(onClick = onClick) {
        Row(Modifier.padding(16.dp).heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(name)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MRColors.Ink)
                Text(subtitle, fontSize = 16.sp, color = MRColors.Slate)
                Spacer(Modifier.height(4.dp))
                StatusChip(chipText, chipColor, chipIcon)
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MRColors.Ink)
        }
    }
}

/* ------------------------- BOTONES Y DIÁLOGOS ------------------------- */

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    val color = if (danger) MRColors.Red else MRColors.Ink
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(60.dp),
        shape = CircleShape,
        border = BorderStroke(2.dp, color),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MRColors.White,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title, color = MRColors.Ink, fontWeight = FontWeight.Bold) },
        text = { Text(text, fontSize = 18.sp, color = MRColors.Ink) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = if (danger) MRColors.Red else MRColors.Blue)
            ) { Text(confirmText, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = MRColors.Ink)) {
                Text("Cancelar", fontSize = 18.sp)
            }
        }
    )
}

// ------------------------- Permisos -------------------------

private data class PermDef(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val get: (LinkPermissions) -> Boolean,
    val set: (LinkPermissions, Boolean) -> LinkPermissions
)

private val permDefs = listOf(
    PermDef(Icons.Outlined.Description, "Ficha médica", "Alergias, padecimientos y datos de salud de tu perfil.",
        { it.ficha }, { p, v -> p.copy(ficha = v) }),
    PermDef(Icons.Outlined.Inventory2, "Inventario", "Ver cuántas dosis te quedan de cada medicamento.",
        { it.inventario }, { p, v -> p.copy(inventario = v) }),
    PermDef(Icons.Outlined.History, "Historial de tomas", "Ver si tomaste tus medicamentos a su hora.",
        { it.historial }, { p, v -> p.copy(historial = v) }),
    PermDef(Icons.Outlined.NotificationsActive, "Alertas", "Recibir avisos de reabastecimiento y de tomas omitidas.",
        { it.alertas }, { p, v -> p.copy(alertas = v) }),
    PermDef(Icons.Outlined.Edit, "Gestionar medicamentos", "Agregar o modificar tus medicamentos por ti.",
        { it.gestionar }, { p, v -> p.copy(gestionar = v) })
)

@Composable
fun PermissionList(perms: LinkPermissions, onChange: (LinkPermissions) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        permDefs.forEach { d ->
            PermissionRow(d.icon, d.title, d.desc, d.get(perms)) { onChange(d.set(perms, it)) }
        }
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    // Permiso concedido = contorno azul y fondo celeste; no concedido = contorno gris pizarra
    WireCard(
        thin = true,
        color = if (checked) MRColors.IceBlue else MRColors.White,
        borderColor = if (checked) MRColors.Blue else MRColors.Slate
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 76.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (checked) MRColors.Blue else MRColors.Slate, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MRColors.Ink)
                Text(desc, fontSize = 15.sp, color = if (checked) MRColors.Ink else MRColors.Slate)
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = null, // el clic lo maneja toda la fila (zona táctil grande)
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MRColors.White,
                    checkedTrackColor = MRColors.Blue,
                    checkedBorderColor = MRColors.Blue,
                    uncheckedThumbColor = MRColors.Slate,
                    uncheckedTrackColor = MRColors.White,
                    uncheckedBorderColor = MRColors.Slate
                )
            )
        }
    }
}

/** "14:30:00" -> "2:30 p. m." */
fun time12(value: String?): String {
    if (value.isNullOrBlank()) return "Sin registrar"
    val parts = value.split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return value
    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d %s".format(h12, m, if (h < 12) "a. m." else "p. m.")
}