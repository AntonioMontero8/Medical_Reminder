package com.example.medicalreminder.ui.link

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.LinkViewModel
import com.example.medicalreminder.data.LinkPermissions
import com.example.medicalreminder.ui.auth.BrandHeader
import com.example.medicalreminder.ui.theme.MRColors

/** Estructura común: flecha atrás + logo, título, contenido con scroll y botones fijos abajo. */
@Composable
fun LinkScaffold(
    title: String,
    onBack: () -> Unit,
    bottom: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 24.dp)) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            IconButton(onClick = onBack, Modifier.align(Alignment.CenterStart).size(48.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = MRColors.Navy)
            }
            Box(Modifier.align(Alignment.Center)) { BrandHeader(compact = true) }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MRColors.Navy,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
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

@Composable
fun BodyText(text: String, modifier: Modifier = Modifier, center: Boolean = false) {
    Text(
        text, fontSize = 18.sp, color = MRColors.TextGray, modifier = modifier,
        textAlign = if (center) TextAlign.Center else TextAlign.Start
    )
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    val color = if (danger) MRColors.Error else MRColors.Navy
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
fun StatusChip(text: String, color: Color) {
    Surface(shape = CircleShape, color = color) {
        Text(
            text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MRColors.Navy,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun InfoCard(title: String, text: String) {
    Surface(shape = RoundedCornerShape(20.dp), color = MRColors.MintSoft, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MRColors.Navy)
            Text(text, fontSize = 17.sp, color = MRColors.TextGray, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun LinkListItem(name: String, subtitle: String, chipText: String, chipColor: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MRColors.FieldBg,
        border = BorderStroke(1.dp, MRColors.FieldBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MRColors.Navy)
                Text(subtitle, fontSize = 16.sp, color = MRColors.TextGray)
            }
            Spacer(Modifier.width(12.dp))
            StatusChip(chipText, chipColor)
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MRColors.Navy) },
        text = { Text(text, fontSize = 18.sp) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmText, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", fontSize = 18.sp) } }
    )
}

// ------------------------- Permisos -------------------------

private data class PermDef(
    val title: String,
    val desc: String,
    val get: (LinkPermissions) -> Boolean,
    val set: (LinkPermissions, Boolean) -> LinkPermissions
)

private val permDefs = listOf(
    PermDef("Ficha médica", "Alergias, padecimientos y datos de salud de tu perfil.",
        { it.ficha }, { p, v -> p.copy(ficha = v) }),
    PermDef("Inventario", "Ver cuántas dosis te quedan de cada medicamento.",
        { it.inventario }, { p, v -> p.copy(inventario = v) }),
    PermDef("Historial de tomas", "Ver si tomaste tus medicamentos a su hora.",
        { it.historial }, { p, v -> p.copy(historial = v) }),
    PermDef("Alertas", "Recibir avisos de reabastecimiento y de tomas omitidas.",
        { it.alertas }, { p, v -> p.copy(alertas = v) }),
    PermDef("Gestionar medicamentos", "Agregar o modificar tus medicamentos por ti.",
        { it.gestionar }, { p, v -> p.copy(gestionar = v) })
)

@Composable
fun PermissionList(perms: LinkPermissions, onChange: (LinkPermissions) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        permDefs.forEach { d ->
            PermissionRow(d.title, d.desc, d.get(perms)) { onChange(d.set(perms, it)) }
        }
    }
}

@Composable
private fun PermissionRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MRColors.FieldBg,
        border = BorderStroke(1.dp, MRColors.FieldBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MRColors.Navy)
                Text(desc, fontSize = 15.sp, color = MRColors.TextGray)
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = null, // el clic lo maneja toda la fila (zona táctil grande)
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MRColors.Navy,
                    checkedTrackColor = MRColors.Mint,
                    checkedBorderColor = MRColors.Navy,
                    uncheckedThumbColor = MRColors.FieldBorder,
                    uncheckedTrackColor = MRColors.FieldBg,
                    uncheckedBorderColor = MRColors.FieldBorder
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