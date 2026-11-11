package com.example.medicalreminder.ui.medication

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.MedicationViewModel
import com.example.medicalreminder.data.Medication
import com.example.medicalreminder.data.MedicationInput
import com.example.medicalreminder.data.formatQty
import com.example.medicalreminder.data.unitText
import com.example.medicalreminder.ui.auth.PrimaryButton
import com.example.medicalreminder.ui.link.BodyText
import com.example.medicalreminder.ui.link.ConfirmDialog
import com.example.medicalreminder.ui.link.InfoCard
import com.example.medicalreminder.ui.link.LinkScaffold
import com.example.medicalreminder.ui.link.SectionLabel
import com.example.medicalreminder.ui.link.WireCard
import com.example.medicalreminder.ui.link.SecondaryButton
import com.example.medicalreminder.ui.link.StatusChip
import com.example.medicalreminder.ui.link.time12
import com.example.medicalreminder.ui.profile.DropdownField
import com.example.medicalreminder.ui.profile.ProfileTextField
import com.example.medicalreminder.ui.profile.TimeDialog
import com.example.medicalreminder.ui.profile.display12h
import com.example.medicalreminder.ui.profile.fieldColors
import com.example.medicalreminder.ui.theme.MRColors
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/* ------------------------- OPCIONES ------------------------- */

/** Cada presentación define en qué se cuenta el inventario y una vía de administración sugerida. */
private data class Presentation(val label: String, val unit: String, val defaultVia: String?)

private val PRESENTATIONS = listOf(
    Presentation("Tableta", "tabletas", "Oral"),
    Presentation("Cápsula", "cápsulas", "Oral"),
    Presentation("Jarabe", "ml", "Oral"),
    Presentation("Gotas", "gotas", null),
    Presentation("Inyección", "ampolletas", "Inyectable"),
    Presentation("Crema o ungüento", "g", "Tópica"),
    Presentation("Parche", "parches", "Tópica"),
    Presentation("Inhalador", "disparos", "Inhalada"),
    Presentation("Otra", "unidades", null)
)

private val VIAS = listOf("Oral", "Sublingual", "Tópica", "Oftálmica", "Ótica", "Nasal", "Inhalada", "Inyectable", "Rectal", "Otra")

/** (texto en pantalla, valor que se guarda en la base de datos) */
private val FOOD = listOf(
    "Indiferente" to "indiferente",
    "Antes de comer" to "antes",
    "Con alimentos" to "con",
    "Después de comer" to "despues"
)

private fun foodLabel(code: String) = FOOD.firstOrNull { it.second == code }?.first ?: "Indiferente"

/* ------------------------- MI BOTIQUÍN (LISTA) ------------------------- */

@Composable
fun MedicationListScreen(vm: MedicationViewModel, onBack: () -> Unit, onAdd: () -> Unit, onEdit: (Int) -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.load() }
    LaunchedEffect(vm.error) {
        vm.error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); vm.error = null }
    }

    LinkScaffold(
        title = "Mi botiquín",
        caption = "Tu inventario",
        onBack = onBack,
        bottom = { PrimaryButton("REGISTRAR MEDICAMENTO", onClick = onAdd) }
    ) {
        when {
            !vm.loaded -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally), color = MRColors.Blue)
            vm.medications.isEmpty() -> InfoCard(
                "Aún no tienes medicamentos",
                "Toca «Registrar medicamento» para agregar el primero con su dosis, horarios e inventario.",
                icon = Icons.Outlined.Medication
            )
            else -> {
                val low = vm.medications.count { it.bajoInventario }
                BodyText(
                    "${vm.medications.size} " + (if (vm.medications.size == 1) "medicamento" else "medicamentos") +
                            (if (low > 0) " · $low con pocas dosis" else "") + ". Toca uno para editarlo o eliminarlo."
                )
                vm.medications.forEach { m -> MedicationCard(m) { onEdit(m.idMedicamento) } }
            }
        }
    }
}

/** Fecha de hoy como "AAAA-MM-DD" (se compara como texto con la fecha de caducidad). */
private fun todayIso(): String {
    val c = Calendar.getInstance()
    return "%04d-%02d-%02d".format(Locale.US, c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

/**
 * Tarjeta de un medicamento. Con onClick (paciente) es un acceso a editarlo; sin onClick (cuidador) es solo lectura.
 * El estado del inventario se ve en una etiqueta con icono y texto, no solo por color:
 * verde = suficiente, coral = pocas dosis, rojo = agotado o caducado.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MedicationCard(m: Medication, onClick: (() -> Unit)? = null) {
    val today = remember { todayIso() }
    val expired = m.fechaCaducidad?.let { it < today } == true
    val soldOut = m.existencia <= 0.0
    WireCard(onClick = onClick) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 18.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Medication, contentDescription = null, tint = MRColors.Blue, modifier = Modifier.size(44.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (m.concentracion.isBlank()) m.nombre else "${m.nombre} ${m.concentracion}",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MRColors.Ink
                )
                Text("${m.presentacion} · ${m.via}", fontSize = 16.sp, color = MRColors.Slate)
                if (m.dosisCantidad != null) {
                    val food = if (m.conAlimentos == "indiferente") "" else " · ${foodLabel(m.conAlimentos)}"
                    Text("Dosis: ${unitText(m.dosisCantidad, m.unidad)}$food", fontSize = 16.sp, color = MRColors.Ink)
                }
                if (m.horarios.isNotEmpty()) {
                    Text("Horarios: ${m.horarios.joinToString(", ") { time12(it) }}", fontSize = 16.sp, color = MRColors.Ink)
                }
                val left = m.dosisRestantes?.let { " ($it dosis)" } ?: ""
                Text("Existencia: ${unitText(m.existencia, m.unidad)}$left", fontSize = 16.sp, color = MRColors.Ink)
                m.fechaCaducidad?.let { Text("Caduca: ${displayDate(it)}", fontSize = 16.sp, color = MRColors.Slate) }
                if (m.indicaciones.isNotBlank()) Text(m.indicaciones, fontSize = 16.sp, color = MRColors.Slate)
                Spacer(Modifier.height(2.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    when {
                        soldOut -> StatusChip("Agotado", MRColors.Red, Icons.Outlined.ErrorOutline)
                        m.bajoInventario -> StatusChip("Pocas dosis", MRColors.Blush, Icons.Outlined.Warning, MRColors.Coral)
                        else -> StatusChip("Existencia suficiente", MRColors.Green, Icons.Outlined.CheckCircle)
                    }
                    if (expired) StatusChip("Caducado", MRColors.Red, Icons.Outlined.ErrorOutline)
                }
            }
            if (onClick != null) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MRColors.Ink, modifier = Modifier.align(Alignment.CenterVertically))
            }
        }
    }
}

/* ------------------------- REGISTRAR MEDICAMENTO (FORMULARIO) ------------------------- */

private data class FormErrors(
    val name: String? = null, val presentation: String? = null, val via: String? = null,
    val dose: String? = null, val stock: String? = null, val times: String? = null,
    val expires: String? = null
) {
    fun any() = listOfNotNull(name, presentation, via, dose, stock, times, expires).isNotEmpty()
}

/**
 * Registrar (medicationId = null) o editar un medicamento. Al editar, el formulario aparece con los datos actuales
 * y se puede eliminar el medicamento.
 */
@Composable
fun MedicationFormScreen(
    vm: MedicationViewModel,
    medicationId: Int? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit = {}
) {
    val original = medicationId?.let { id -> vm.medications.firstOrNull { it.idMedicamento == id } }
    if (medicationId != null && original == null) {
        // Aún no se cargó la lista (o el medicamento ya no existe)
        LaunchedEffect(Unit) { vm.load() }
        LinkScaffold(title = "Editar medicamento", onBack = onBack) {
            if (vm.loaded) InfoCard("No encontramos este medicamento", "Quizá ya fue eliminado. Vuelve a Mi botiquín.")
            else CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        return
    }
    MedicationFormContent(vm, original, onBack, onSaved, onDeleted)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MedicationFormContent(
    vm: MedicationViewModel,
    original: Medication?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit
) {
    val editing = original != null
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    LaunchedEffect(vm.error) {
        vm.error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); vm.error = null }
    }

    var name by rememberSaveable { mutableStateOf(original?.nombre ?: "") }
    var presentation by rememberSaveable { mutableStateOf(original?.presentacion ?: "") }
    var concentration by rememberSaveable { mutableStateOf(original?.concentracion ?: "") }
    var via by rememberSaveable { mutableStateOf(original?.via ?: "") }
    var dose by rememberSaveable { mutableStateOf(original?.dosisCantidad?.let { formatQty(it) } ?: "") }
    var stock by rememberSaveable { mutableStateOf(original?.let { formatQty(it.existencia) } ?: "") }
    var times by rememberSaveable { mutableStateOf(original?.horarios?.joinToString(",") ?: "") }          // "08:00,16:00" (texto: se guarda solo al girar la pantalla)
    var food by rememberSaveable { mutableStateOf(original?.conAlimentos ?: "indiferente") }
    var acquired by rememberSaveable { mutableStateOf(original?.fechaAdquisicion ?: "") }       // "AAAA-MM-DD" o vacío
    var expires by rememberSaveable { mutableStateOf(original?.fechaCaducidad ?: "") }
    var notes by rememberSaveable { mutableStateOf(original?.indicaciones ?: "") }
    var errors by remember { mutableStateOf(FormErrors()) }
    var addingTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val unit = PRESENTATIONS.firstOrNull { it.label == presentation }?.unit ?: original?.unidad
    val timeList = if (times.isEmpty()) emptyList() else times.split(",")

    fun submit() {
        val d = dose.replace(',', '.').toDoubleOrNull()
        val s = stock.replace(',', '.').toDoubleOrNull()
        val e = FormErrors(
            name = if (name.isBlank()) "Escribe el nombre del medicamento" else null,
            presentation = if (presentation.isEmpty()) "Elige la presentación" else null,
            via = if (via.isEmpty()) "Elige la vía de administración" else null,
            dose = if (d == null || d <= 0.0 || d > 9999.0) "Dosis no válida" else null,
            stock = if (s == null || s < 0.0 || s > 999999.0) "Existencia no válida" else null,
            times = if (timeList.isEmpty()) "Agrega al menos un horario de toma" else null,
            expires = if (acquired.isNotEmpty() && expires.isNotEmpty() && expires < acquired)
                "No puede ser anterior a la adquisición" else null
        )
        errors = e
        if (e.any()) return
        val input =
            MedicationInput(
                nombre = name.trim(), presentacion = presentation, concentracion = concentration.trim(),
                via = via, unidadInventario = unit ?: "unidades", existencia = s!!,
                fechaAdquisicion = acquired.ifEmpty { null }, fechaCaducidad = expires.ifEmpty { null },
                dosisCantidad = d!!, conAlimentos = food, indicaciones = notes.trim(),
                horarios = timeList.sorted()
            )
        if (original != null) vm.update(original.idMedicamento, input, onSaved) else vm.save(input, onSaved)
    }

    LinkScaffold(
        title = if (editing) "Editar medicamento" else "Registrar medicamento",
        onBack = { if (!vm.saving) onBack() },
        bottom = {
            PrimaryButton(
                if (editing) "GUARDAR CAMBIOS" else "GUARDAR MEDICAMENTO",
                onClick = { focus.clearFocus(); submit() }, loading = vm.saving
            )
            if (editing) {
                SecondaryButton("ELIMINAR MEDICAMENTO", onClick = { if (!vm.saving) confirmDelete = true }, danger = true)
            }
        }
    ) {
        SectionLabel("Medicamento", Modifier.padding(top = 0.dp))
        ProfileTextField(
            name, { name = it.take(120); errors = errors.copy(name = null) }, "Nombre del medicamento",
            Modifier.fillMaxWidth(), placeholder = "Ej. Paracetamol", error = errors.name
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            DropdownField(
                "Presentación", presentation, PRESENTATIONS.map { it.label },
                { sel ->
                    presentation = sel
                    // Sugiere la vía más común, pero solo si aún no se eligió una
                    if (via.isEmpty()) PRESENTATIONS.firstOrNull { it.label == sel }?.defaultVia?.let { via = it }
                    errors = errors.copy(presentation = null)
                },
                Modifier.weight(1f), error = errors.presentation
            )
            ProfileTextField(
                concentration, { concentration = it.take(40) }, "Concentración",
                Modifier.weight(1f), placeholder = "Ej. 500 mg"
            )
        }
        DropdownField(
            "Vía de administración", via, VIAS,
            { via = it; errors = errors.copy(via = null) },
            Modifier.fillMaxWidth(), error = errors.via
        )
        SectionLabel("Dosis y existencia")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            ProfileTextField(
                dose, { dose = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(7); errors = errors.copy(dose = null) },
                "Dosis por toma", Modifier.weight(1f), placeholder = "Ej. 1",
                keyboardType = KeyboardType.Decimal, error = errors.dose
            )
            ProfileTextField(
                stock, { stock = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(9); errors = errors.copy(stock = null) },
                "Existencia actual", Modifier.weight(1f), placeholder = "Ej. 30",
                keyboardType = KeyboardType.Decimal, error = errors.stock
            )
        }
        Text(
            if (unit != null) "La dosis y la existencia se cuentan en: $unit" else "Elige la presentación para saber en qué se cuenta la dosis.",
            fontSize = 15.sp, color = MRColors.Slate
        )

        // Horarios de toma: uno o varios
        SectionLabel("Horarios")
        WireCard(thin = true, borderColor = if (errors.times != null) MRColors.Red else MRColors.Slate) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Horarios de toma", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MRColors.Ink)
                if (timeList.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        timeList.sorted().forEach { t ->
                            TimeTag(t) { times = timeList.filter { it != t }.joinToString(","); errors = errors.copy(times = null) }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { addingTime = true },
                    modifier = Modifier.heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, MRColors.Blue),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MRColors.Blue)
                ) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Agregar horario", fontSize = 18.sp)
                }
                errors.times?.let { Text(it, color = MRColors.Red, fontSize = 14.sp) }
            }
        }

        SectionLabel("Indicaciones")
        DropdownField(
            "Con alimentos", foodLabel(food), FOOD.map { it.first },
            { label -> food = FOOD.first { it.first == label }.second },
            Modifier.fillMaxWidth()
        )
        DateField("Fecha de adquisición", acquired, { acquired = it; errors = errors.copy(expires = null) }, Modifier.fillMaxWidth())
        DateField("Fecha de caducidad", expires, { expires = it; errors = errors.copy(expires = null) }, Modifier.fillMaxWidth(), error = errors.expires)
        ProfileTextField(
            notes, { notes = it.take(1000) }, "Indicaciones", Modifier.fillMaxWidth(),
            placeholder = "Ej. Tomar con un vaso de agua", singleLine = false, minLines = 2, maxLines = 4
        )
    }

    if (confirmDelete && original != null) {
        ConfirmDialog(
            title = "¿Eliminar medicamento?",
            text = "Se quitará «${original.nombre}» de tu botiquín y se desactivarán sus alarmas. Tu historial de tomas se conserva.",
            confirmText = "Eliminar",
            danger = true,
            onConfirm = { confirmDelete = false; vm.delete(original.idMedicamento, onDeleted) },
            onDismiss = { confirmDelete = false }
        )
    }

    if (addingTime) {
        TimeDialog(
            title = "Hora de toma",
            initial = "08:00",
            onDismiss = { addingTime = false },
            onConfirm = { t ->
                if (t !in timeList) times = (timeList + t).sorted().joinToString(",")
                errors = errors.copy(times = null)
                addingTime = false
            }
        )
    }
}

/** Una hora ya elegida; al tocarla se quita. */
@Composable
private fun TimeTag(time: String, onRemove: () -> Unit) {
    Surface(
        onClick = onRemove,
        shape = RoundedCornerShape(12.dp),
        color = MRColors.SkyBlue,
        modifier = Modifier.heightIn(min = 52.dp).semantics {
            contentDescription = "${display12h(time)}. Toca para quitar este horario"
        }
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MRColors.Navy, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            Text(display12h(time), fontSize = 18.sp, color = MRColors.Navy)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Close, contentDescription = null, tint = MRColors.Navy, modifier = Modifier.size(20.dp))
        }
    }
}

/* ------------------------- FECHAS ------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier) {
        Box {
            OutlinedTextField(
                value = if (value.isEmpty()) "" else displayDate(value),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                label = { Text(label, fontSize = 16.sp) },
                placeholder = { Text("Opcional", fontSize = 18.sp, color = MRColors.Slate) },
                trailingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = MRColors.Blue) },
                isError = error != null,
                shape = RoundedCornerShape(8.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                colors = fieldColors()
            )
            // Capa transparente: un campo readOnly no recibe el clic por sí solo
            Box(Modifier.matchParentSize().clickable(role = Role.Button, onClickLabel = "Elegir $label") { open = true })
        }
        if (value.isNotEmpty()) {
            TextButton(onClick = { onChange("") }, colors = ButtonDefaults.textButtonColors(contentColor = MRColors.Blue)) { Text("Quitar fecha", fontSize = 16.sp) }
        }
        if (error != null) {
            Text(error, color = MRColors.Red, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }
    }

    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = millisFromIso(value))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onChange(isoFromMillis(it)) }
                        open = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MRColors.Blue)
                ) { Text("Aceptar", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }, colors = ButtonDefaults.textButtonColors(contentColor = MRColors.Ink)) {
                    Text("Cancelar", fontSize = 18.sp)
                }
            }
        ) { DatePicker(state = state) }
    }
}

// El selector de fechas trabaja en milisegundos a las 00:00 UTC; se convierte con UTC para que no se corra un día.
private fun isoFromMillis(ms: Long): String {
    val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = ms }
    return "%04d-%02d-%02d".format(Locale.US, c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

private fun millisFromIso(iso: String): Long? {
    val p = iso.split("-")
    if (p.size != 3) return null
    val y = p[0].toIntOrNull() ?: return null
    val m = p[1].toIntOrNull() ?: return null
    val d = p[2].toIntOrNull() ?: return null
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(y, m - 1, d) }.timeInMillis
}

/** "2027-05-01" -> "01/05/2027" */
private fun displayDate(iso: String): String {
    val p = iso.split("-")
    return if (p.size == 3) "${p[2]}/${p[1]}/${p[0]}" else iso
}