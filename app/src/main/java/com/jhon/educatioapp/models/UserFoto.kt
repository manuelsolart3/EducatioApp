package com.jhon.educatioapp.models

import com.google.gson.annotations.SerializedName

data class UserFoto(
    @SerializedName("_id") val id: String,
    @SerializedName("N_Identificacion") val identificacion: Long,
    @SerializedName("NomCompleto") val nombre: String,
    @SerializedName("Telefono") val telefono: Long,
    @SerializedName("Ciudad") val ciudad: String,
    @SerializedName("email") val email: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("foto") val foto: String
)
