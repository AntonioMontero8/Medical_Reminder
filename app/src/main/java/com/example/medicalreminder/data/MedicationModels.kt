package com.example.medicalreminder.data

/** Lo que el paciente captura al registrar un medicamento (se guarda en medicamento + prescripcion + horario_toma). */
data class MedicationInput(
    val nombre: String,
    val presentacion: String,
    val concentracion: String,
    val via: String,
    val unidadInventario: String,
    val existencia: Double,
    val fechaAdquisicion: String?,   // "AAAA-MM-DD" o null
    val fechaCaducidad: String?,
    val dosisCantidad: Double,
    val conAlimentos: String,        // indiferente | antes | con | despues
    val indicaciones: String,
    val horarios: List<String>       // "HH:mm" en 24 h
)

/** Un medicamento del botiquín, tal como lo devuelve GET /api/medicamentos. */
data class Medication(
    val idMedicamento: Int,
    val nombre: String,
    val presentacion: String,
    val concentracion: String,
    val via: String,
    val unidad: String,
    val existencia: Double,
    val dosisCantidad: Double?,
    val conAlimentos: String,
    val indicaciones: String,
    val horarios: List<String>,
    val dosisRestantes: Int?,
    val bajoInventario: Boolean,
    val fechaAdquisicion: String?,
    val fechaCaducidad: String?
)

/** 1.0 -> "1", 2.5 -> "2.5" */
fun formatQty(d: Double): String =
    if (d % 1.0 == 0.0) d.toLong().toString() else "%.2f".format(java.util.Locale.US, d).trimEnd('0').trimEnd('.')

private fun singular(unit: String) = when {
    unit == "unidades" -> "unidad"
    unit.endsWith("s") -> unit.dropLast(1)
    else -> unit
}

/** "1 tableta", "2 tabletas", "2.5 ml" */
fun unitText(qty: Double, unit: String): String = "${formatQty(qty)} ${if (qty == 1.0) singular(unit) else unit}"