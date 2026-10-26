package com.example.medicalreminder.data

const val MAX_CAREGIVERS = 3

/** Lo que el paciente permite ver o hacer al cuidador. Equivale a las columnas booleanas de `vinculacion`. */
data class LinkPermissions(
    val ficha: Boolean = true,
    val inventario: Boolean = true,
    val historial: Boolean = true,
    val alertas: Boolean = true,
    val gestionar: Boolean = false
)

/** Una vinculación vista desde cualquiera de los dos lados (nombre = el de la otra persona). */
data class LinkInfo(
    val idVinculacion: Int,
    val estado: String, // "pendiente" | "activa"
    val nombre: String,
    val apellido: String,
    val permisos: LinkPermissions
) {
    val pendiente: Boolean get() = estado == "pendiente"
    val fullName: String get() = "$nombre $apellido".trim()
}

/** Código temporal que se muestra como QR en el teléfono del paciente. */
data class LinkCode(val codigo: String, val deadlineMillis: Long) {
    val qrText: String get() = "medicalreminder://vincular?c=$codigo"
    val display: String get() = codigo.chunked(4).joinToString(" ")
}

data class PatientCard(
    val sexo: String?,
    val peso: Double?,
    val estatura: Int?,
    val grupoSanguineo: String?,
    val alergias: String?,
    val padecimientos: String?,
    val cirugias: String?,
    val enfermedadesCronicas: String?,
    val desayuno: String?,
    val comida: String?,
    val cena: String?
)

/** Datos del panel espejo. `ficha` es null si el paciente no dio ese permiso. */
data class PanelData(
    val nombre: String,
    val apellido: String,
    val edad: Int?,
    val permisos: LinkPermissions,
    val ficha: PatientCard?
)

/**
 * Obtiene el código de 8 caracteres de lo que leyó el escáner ("medicalreminder://vincular?c=ABCD2345")
 * o de lo que escribió el usuario ("abcd 2345"). Devuelve null si no tiene el formato esperado.
 */
fun parseLinkCode(raw: String): String? {
    val text = raw.trim()
    val fromUri = Regex("""^medicalreminder://vincular\?c=([A-Za-z0-9]{8})$""").find(text)?.groupValues?.get(1)
    val candidate = (fromUri ?: text).uppercase().replace(Regex("[\\s-]"), "")
    return candidate.takeIf { Regex("^[A-Z0-9]{8}$").matches(it) }
}