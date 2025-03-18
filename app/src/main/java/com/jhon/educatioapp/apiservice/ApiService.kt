package com.jhon.educatioapp.apiservice
import com.jhon.educatioapp.models.LoginData
import com.jhon.educatioapp.models.UserData
import com.jhon.educatioapp.models.UserProfile
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Header
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Field
import retrofit2.http.GET

// Paso 2: Interfaz de Servicio
interface ApiService {

    data class ResponseApi(
        val message:ArrayList<String>
    )

    @GET("usuarios")
    suspend fun getUsers(): ApiClient.FotoResponse

    //@Body se usa para indicar que los datos que se enviarán en el cuerpo de la solicitud serán proporcionados por el parámetro
    @POST("register")
    suspend fun insertarDatos(@Body userList: UserData): ResponseApi//void es para no esperar ninguna respuesta

    //Interfaz de servicio para inicio de sesion
    @POST("login")
    fun login(@Body loginData: LoginData): Call<ApiClient.LoginResponse>

    @POST("hojaVida")
    fun enviarDatosAlServidor(
        @Header("Authorization") token: String,
        @Body datos: ApiClient.DatosAServidorRequest
    ): Call<ApiClient.ResponseApi>


    @GET("perfil")
    fun obtenerPerfilUsuario(@Header("Authorization") token: String): Call<UserProfile>
    @POST("foto")
    fun enviarUrlFoto(
        @Header("Authorization") token: String,
        @Body request: ApiClient.UrlFotoRequest

    ): Call<ApiClient.ResponseApi>
    @GET("perfil")
    suspend fun obtenerProfile(@Header("Authorization") token: String): UserProfile
    // cambio de rol
    @FormUrlEncoded
    @POST("/usuario/cambiar-rol")
    fun cambiarRol(
        @Header("Authorization") token: String,
        @Field("rol") nuevoRol: String
    ): Call<ApiClient.CambiarRolResponse>

}
object WebServices{
    val web by lazy {
        Retrofit.Builder()
            .baseUrl("https://bdeducatio.vercel.app/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}