package com.jhon.educatioapp.models

// Paso 1: Modelo de Datos
// Definimos el modelo de datos.
data class UserData(
    val N_Identificacion: String,
    val NomCompleto: String,
    val Telefono: String,
    val Ciudad: String,
    val email: String,
    val password: String
)

