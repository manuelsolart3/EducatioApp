package com.jhon.educatioapp.models

import java.util.Date

data class Clase(
    val id: String,
    val email: String,
    val materia: String,
    val tema: String,
    val fecha: Date,
    val horaInicio: String,
    val horaFin: String,
    val modalidad: String,
    var valorClase: String,
    var nombreCompleto: String,  // Nuevo campo para el nombre completo
    var aceptaciones: List<HashMap<String, Any>> = emptyList(),
    var foto: String? = null // Inicializar como nula
) {
    // Campo adicional para almacenar el correo de aceptación
    val correoAceptacion: String
        get() {
            val aceptacion = aceptaciones.firstOrNull()
            return aceptacion?.get("correo") as? String ?: ""
        }

    // Propiedad ajustada para mostrar el nombre dentro de las aceptaciones
    val nombreAceptacion: String
        get() {
            val primeraAceptacion = aceptaciones.firstOrNull()
            return primeraAceptacion?.get("nombre") as? String ?: nombreCompleto
        }
}
