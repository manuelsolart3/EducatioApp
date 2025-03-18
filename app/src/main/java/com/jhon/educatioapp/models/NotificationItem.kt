package com.jhon.educatioapp.models

import java.io.Serializable

data class NotificationItem(
    val fecha: String,
    val email: String,
    val nombre: String?,
    val correo: String?,
    val valorClase: String?,
    val esContraoferta: Boolean,
    val materia: String?,
    val tema: String?,
    val horaInicio: String?,
    val horaFin: String?,
    val modalidad: String?,
    val id: String,
    val estado: String,  // Agregar el atributo estado
    val estadoPostulacion: String,  // Agregar el estado de la postulación
    val valorContraoferta: String? = null ,  // Nuevo campo para el valor de la contraoferta
    var foto: String? = null // Inicializar como nula
) : Serializable
