package com.jhon.educatioapp.apiservice
import com.google.gson.annotations.SerializedName
import com.jhon.educatioapp.models.UserFoto
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient {
    companion object {
        private const val BASE_URL = "https://bdeducatio.vercel.app/api/"

        // Creamos una función para crear una instancia de Retrofit.
        fun createApiService(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
    //Respuesta de Rol
    data class CambiarRolResponse(
        val message: String,
        val rol: String
    )
    // Respuestas de registro y login
    data class LoginResponse(
        val message:ArrayList<String>,
        val token: String?,
        val rol: String?
    )

    data class DatosAServidorRequest(
        val token: String,
        val archivoUrl: String
    )

    data class UrlFotoRequest(
        val token: String,
        val foto: String
    )

    data class ResponseApi(
        val message: String
    )
    data class FotoResponse(
        @SerializedName("usuarios")
        val userProfiles: List<UserFoto>
    )
}