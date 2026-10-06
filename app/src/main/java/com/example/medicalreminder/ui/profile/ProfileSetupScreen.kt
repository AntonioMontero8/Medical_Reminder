package com.example.medicalreminder.ui.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.data.CaregiverProfile
import com.example.medicalreminder.data.PatientProfile
import com.example.medicalreminder.ui.auth.BrandHeader
import com.example.medicalreminder.ui.auth.PrimaryButton
import com.example.medicalreminder.ui.auth.UserRole
import com.example.medicalreminder.ui.theme.MRColors

private val SEX_OPTIONS = listOf("Masculino", "Femenino", "Otro")
private val BLOOD_OPTIONS = listOf("A+", "A−", "B+", "B−", "AB+", "AB−", "O+", "O−")

/** Punto de entrada: muestra el formulario que corresponde al rol elegido en el registro. */
@Composable
fun ProfileSetupScreen(
    role: UserRole,
    onBack: () -> Unit,
    onSavePatient: (PatientProfile) -> Unit,
    onSaveCaregiver: (CaregiverProfile) -> Unit,
    loading: Boolean = false
) {
    when (role) {
        UserRole.PATIENT -> PatientProfileForm(onBack, onSavePatient, loading)
        UserRole.CAREGIVER -> CaregiverProfileForm(onBack, onSaveCaregiver, loading)
    }
}

/* ------------------------------ PACIENTE ------------------------------ */

private data class PatientErrors(
    val age: String? = null, val sex: String? = null, val weight: String? = null,
    val height: String? = null, val blood: String? = null
) {
    fun any() = listOfNotNull(age, sex, weight, height, blood).isNotEmpty()
}

@Composable
private fun PatientProfileForm(onBack: () -> Unit, onSave: (PatientProfile) -> Unit, loading: Boolean) {
    var age by rememberSaveable { mutableStateOf("") }
    var sex by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var blood by rememberSaveable { mutableStateOf("") }
    var allergies by rememberSaveable { mutableStateOf("") }
    var conditions by rememberSaveable { mutableStateOf("") }
    var surgeries by rememberSaveable { mutableStateOf("") }
    var chronic by rememberSaveable { mutableStateOf("") }
    var breakfast by rememberSaveable { mutableStateOf<String?>(null) }
    var lunch by rememberSaveable { mutableStateOf<String?>(null) }
    var dinner by rememberSaveable { mutableStateOf<String?>(null) }
    var errors by remember { mutableStateOf(PatientErrors()) }

    fun submit() {
        val a = age.toIntOrNull()
        val w = weight.replace(',', '.').toFloatOrNull()
        val h = height.toIntOrNull()
        val e = PatientErrors(
            age = if (a == null || a !in 1..120) "Edad no válida" else null,
            sex = if (sex.isEmpty()) "Obligatorio" else null,
            weight = if (w == null || w < 2f || w > 400f) "Peso no válido" else null,
            height = if (h == null || h !in 30..250) "Estatura no válida" else null,
            blood = if (blood.isEmpty()) "Selecciona tu grupo sanguíneo" else null
        )
        errors = e
        if (e.any()) return
        onSave(
            PatientProfile(
                edad = a!!, sexo = sex, peso = w!!, estatura = h!!, grupoSanguineo = blood,
                alergias = allergies.trim(), padecimientos = conditions.trim(),
                cirugias = surgeries.trim(),
                enfermedadesCronicas = chronic.trim(),
                horarioDesayuno = breakfast, horarioComida = lunch, horarioCena = dinner
            )
        )
    }

    ProfileLayout(UserRole.PATIENT, onBack, onSubmit = ::submit, loading = loading) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            ProfileTextField(age, { age = it.filter(Char::isDigit).take(3); errors = errors.copy(age = null) },
                "Edad", Modifier.weight(1f), placeholder = "Ej. 34",
                keyboardType = KeyboardType.Number, error = errors.age)
            DropdownField("Sexo", sex, SEX_OPTIONS, { sex = it; errors = errors.copy(sex = null) },
                Modifier.weight(1f), error = errors.sex)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            ProfileTextField(weight, { weight = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6); errors = errors.copy(weight = null) },
                "Peso (kg)", Modifier.weight(1f), placeholder = "Ej. 70.5",
                keyboardType = KeyboardType.Decimal, error = errors.weight)
            ProfileTextField(height, { height = it.filter(Char::isDigit).take(3); errors = errors.copy(height = null) },
                "Estatura (cm)", Modifier.weight(1f), placeholder = "Ej. 165",
                keyboardType = KeyboardType.Number, error = errors.height)
        }
        DropdownField("Grupo Sanguíneo", blood, BLOOD_OPTIONS, { blood = it; errors = errors.copy(blood = null) },
            Modifier.fillMaxWidth(), error = errors.blood)
        ProfileTextField(allergies, { allergies = it }, "Alergias", Modifier.fillMaxWidth(),
            singleLine = false, minLines = 2, maxLines = 4)
        ProfileTextField(conditions, { conditions = it }, "Padecimientos", Modifier.fillMaxWidth(),
            singleLine = false, minLines = 2, maxLines = 4)
        ProfileTextField(surgeries, { surgeries = it }, "Cirugías", Modifier.fillMaxWidth(),
            placeholder = "Ej. Apendicectomía, cesárea", singleLine = false, minLines = 2, maxLines = 4)
        ProfileTextField(chronic, { chronic = it }, "Enfermedades Crónicas", Modifier.fillMaxWidth(),
            placeholder = "Ej. Diabetes, hipertensión", singleLine = false, minLines = 2, maxLines = 4)
        MealTimesCard(breakfast, lunch, dinner,
            onBreakfast = { breakfast = it }, onLunch = { lunch = it }, onDinner = { dinner = it })
    }
}

/* ------------------------------ CUIDADOR ------------------------------ */

@Composable
private fun CaregiverProfileForm(onBack: () -> Unit, onSave: (CaregiverProfile) -> Unit, loading: Boolean) {
    var experience by rememberSaveable { mutableStateOf("") }
    var expError by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (experience.trim().length < 10) { expError = "Cuéntanos un poco más sobre tu experiencia"; return }
        onSave(CaregiverProfile(experience.trim()))
    }

    ProfileLayout(UserRole.CAREGIVER, onBack, onSubmit = ::submit, loading = loading) {
        ProfileTextField(
            experience, { experience = it; expError = null }, "Experiencia Profesional", Modifier.fillMaxWidth(),
            placeholder = "Escribe detalladamente tu trayectoria, experiencia en el cuidado de pacientes, adultos mayores o historial médico...",
            singleLine = false, minLines = 9, maxLines = 14, error = expError,
            imeAction = ImeAction.Done
        )
    }
}

/* ------------------------- ESTRUCTURA COMÚN ------------------------- */

@Composable
private fun ProfileLayout(
    role: UserRole,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    loading: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val focus = LocalFocusManager.current
    Column(Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 24.dp)) {
        ProfileHeader(role, onBack)
        // Formulario con scroll; el botón queda fijo abajo como en el mockup
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
        PrimaryButton("GUARDAR Y COMENZAR", onClick = { focus.clearFocus(); onSubmit() }, loading = loading)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileHeader(role: UserRole, onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            IconButton(onClick = onBack, Modifier.align(Alignment.CenterStart).size(48.dp)) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver", tint = MRColors.Navy)
            }
            Box(Modifier.align(Alignment.Center)) { BrandHeader(compact = true) }
        }
        Spacer(Modifier.height(8.dp))
        Text("Completar Perfil", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = MRColors.Navy)
        Spacer(Modifier.height(10.dp))
        FlowRowCenter {
            InfoChip("Paso 2 de 2", MRColors.ChipGray)
            if (role == UserRole.PATIENT) InfoChip("Rol: Paciente", MRColors.MintStrong)
            else InfoChip("Rol: Cuidador", MRColors.Orange)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowCenter(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) { content() }
}

@Composable
private fun InfoChip(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(shape = CircleShape, color = color) {
        Text(text, fontSize = 18.sp, color = MRColors.Navy, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

/* ------------------------- CAMPOS ------------------------- */

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MRColors.FieldBg,
    unfocusedContainerColor = MRColors.FieldBg,
    errorContainerColor = MRColors.FieldBg,
    unfocusedBorderColor = MRColors.FieldBorder,
    focusedBorderColor = MRColors.Navy,
    focusedLabelColor = MRColors.Navy,
    unfocusedLabelColor = MRColors.Navy
)

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    error: String? = null
) {
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
            label = { Text(label, fontSize = 16.sp) },
            placeholder = placeholder?.let { { Text(it, fontSize = 18.sp, color = MRColors.TextGray) } },
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            isError = error != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = if (singleLine) imeAction else if (imeAction == ImeAction.Done) ImeAction.Done else ImeAction.Default
            ),
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
            colors = fieldColors()
        )
        if (error != null) {
            Text(error, color = MRColors.Error, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }
    }
}

/** Lista desplegable sin depender de ExposedDropdownMenuBox (su API cambia entre versiones de Material 3). */
@Composable
private fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier) {
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                label = { Text(label, fontSize = 16.sp) },
                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MRColors.Navy) },
                isError = error != null,
                shape = RoundedCornerShape(8.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
                colors = fieldColors()
            )
            // Capa transparente: un campo readOnly no recibe el clic por sí solo
            Box(
                Modifier.matchParentSize().clickable(role = Role.DropdownList, onClickLabel = "Elegir $label") { expanded = true }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, fontSize = 18.sp, color = MRColors.Navy) },
                        onClick = { onSelect(option); expanded = false },
                        modifier = Modifier.heightIn(min = 52.dp)
                    )
                }
            }
        }
        if (error != null) {
            Text(error, color = MRColors.Error, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }
    }
}

/* ------------------------- HORARIOS DE COMIDA ------------------------- */

private enum class Meal(val label: String, val defaultTime: String) {
    BREAKFAST("Desayuno", "08:00"), LUNCH("Comida", "14:00"), DINNER("Cena", "20:00")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealTimesCard(
    breakfast: String?, lunch: String?, dinner: String?,
    onBreakfast: (String) -> Unit, onLunch: (String) -> Unit, onDinner: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    var editing by rememberSaveable { mutableStateOf<Meal?>(null) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MRColors.FieldBg,
        border = BorderStroke(1.dp, MRColors.FieldBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    .clickable(role = Role.Button, onClickLabel = if (expanded) "Contraer" else "Expandir") { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Horarios de comida habituales", fontSize = 18.sp, color = MRColors.Navy, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null, tint = MRColors.Navy)
            }
            AnimatedVisibility(expanded) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Meal.entries.forEach { meal ->
                        val value = when (meal) { Meal.BREAKFAST -> breakfast; Meal.LUNCH -> lunch; Meal.DINNER -> dinner }
                        TimeChip(meal.label, value) { editing = meal }
                    }
                }
            }
        }
    }

    editing?.let { meal ->
        val current = when (meal) { Meal.BREAKFAST -> breakfast; Meal.LUNCH -> lunch; Meal.DINNER -> dinner }
        TimeDialog(
            title = "Hora de ${meal.label.lowercase()}",
            initial = current ?: meal.defaultTime,
            onDismiss = { editing = null },
            onConfirm = {
                when (meal) { Meal.BREAKFAST -> onBreakfast(it); Meal.LUNCH -> onLunch(it); Meal.DINNER -> onDinner(it) }
                editing = null
            }
        )
    }
}

@Composable
private fun TimeChip(label: String, time: String?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (time != null) MRColors.MintSoft else MRColors.Background,
        border = BorderStroke(1.dp, if (time != null) MRColors.MintStrong else MRColors.FieldBorder),
        modifier = Modifier.heightIn(min = 52.dp).semantics {
            contentDescription = if (time != null) "$label, ${display12h(time)}. Toca para cambiar" else "$label, sin hora. Toca para elegir"
        }
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MRColors.Navy, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (time != null) "$label ${display12h(time)}" else label, fontSize = 18.sp, color = MRColors.Navy)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(title: String, initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val (h, m) = initial.split(":").let { it[0].toInt() to it[1].toInt() }
    val state = rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MRColors.Navy) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm("%02d:%02d".format(state.hour, state.minute)) }) {
                Text("Aceptar", fontSize = 18.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", fontSize = 18.sp) } }
    )
}

/** "14:30" -> "2:30 p. m." (en la BD se guarda en 24 h) */
private fun display12h(hhmm: String): String {
    val (h, m) = hhmm.split(":").let { it[0].toInt() to it[1].toInt() }
    val h12 = if (h % 12 == 0) 12 else h % 12
    return "%d:%02d %s".format(h12, m, if (h < 12) "a. m." else "p. m.")
}