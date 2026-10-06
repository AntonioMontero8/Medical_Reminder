package com.example.medicalreminder.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalreminder.ui.theme.MRColors
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.medicalreminder.R

enum class UserRole { PATIENT, CAREGIVER }
@Composable
fun BrandHeader(compact: Boolean = false) {
    val box = if (compact) 44.dp else 64.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.mascota2),
            contentDescription = "Logotipo de Medical Reminder",
            modifier = Modifier
                .size(box)
                .clip(RoundedCornerShape(148.dp))
        )
        Text(
            "MEDICAL\nREMINDER",
            fontSize = if (compact) 18.sp else 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MRColors.Navy,
            lineHeight = if (compact) 19.sp else 24.sp
        )
    }
}

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    errorText: String? = null
) {
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).semantics { contentDescription = placeholder },
            placeholder = { Text(placeholder, fontSize = 18.sp, color = MRColors.Navy) },
            leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null, tint = MRColors.Navy) } },
            trailingIcon = trailingIcon,
            singleLine = true,
            isError = errorText != null,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            shape = RoundedCornerShape(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 18.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MRColors.FieldBg,
                unfocusedContainerColor = MRColors.FieldBg,
                unfocusedBorderColor = MRColors.FieldBorder,
                focusedBorderColor = MRColors.Navy,
                errorContainerColor = MRColors.FieldBg
            )
        )
        if (errorText != null) {
            Text(errorText, color = MRColors.Error, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
        }
    }
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    withLockIcon: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    errorText: String? = null
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    AuthTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = modifier,
        leadingIcon = if (withLockIcon) Icons.Outlined.Lock else null,
        keyboardType = KeyboardType.Password,
        imeAction = imeAction,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        errorText = errorText,
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                    tint = MRColors.Navy
                )
            }
        }
    )
}

/** Devuelve 0..5 según la robustez de la contraseña. */
fun passwordStrength(p: String): Int {
    var s = 0
    if (p.length >= 8) s++
    if (p.length >= 12) s++
    if (p.any { it.isLowerCase() } && p.any { it.isUpperCase() }) s++
    if (p.any { it.isDigit() }) s++
    if (p.any { !it.isLetterOrDigit() }) s++
    return s
}

@Composable
fun StrengthBar(password: String, modifier: Modifier = Modifier) {
    val score = passwordStrength(password)
    val label = when (score) { 0, 1 -> "Débil"; 2, 3 -> "Media"; else -> "Fuerte" }
    Column(modifier.semantics { contentDescription = "Seguridad de la contraseña: $label" }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(5) { i ->
                val c by animateColorAsState(if (i < score) MRColors.StrengthOn else MRColors.StrengthOff, label = "bar")
                Box(Modifier.weight(1f).height(6.dp).clip(CircleShape).background(c))
            }
        }
        if (password.isNotEmpty()) {
            Text(label, fontSize = 14.sp, color = MRColors.TextGray, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = modifier.fillMaxWidth().height(60.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = MRColors.Mint, contentColor = MRColors.DarkTeal)
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(24.dp), color = MRColors.DarkTeal, strokeWidth = 3.dp)
        else Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun RoleSelector(selected: UserRole, onSelect: (UserRole) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .height(88.dp)
            .clip(RoundedCornerShape(20.dp))
            .selectableGroup(),
    ) {
        RoleOption("Soy Paciente", Icons.Outlined.FavoriteBorder, selected == UserRole.PATIENT, Modifier.weight(1f)) { onSelect(UserRole.PATIENT) }
        RoleOption("Soy Cuidador", Icons.Outlined.Shield, selected == UserRole.CAREGIVER, Modifier.weight(1f)) { onSelect(UserRole.CAREGIVER) }
    }
}

@Composable
private fun RoleOption(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) MRColors.MintStrong else MRColors.MintSoft, label = "role")
    Column(
        modifier
            .fillMaxHeight()
            .background(bg)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = MRColors.Navy, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 18.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = MRColors.Navy)
    }
}