package com.jhon.educatioapp.controllers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jhon.educatioapp.apiservice.ApiClient
import com.jhon.educatioapp.apiservice.ApiManager
import com.jhon.educatioapp.databinding.ActivityLoginBinding
import com.jhon.educatioapp.models.LoginData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var apiManager: ApiManager
    private lateinit var binding: ActivityLoginBinding
    private lateinit var enlaceRegistro: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enlaceRegistro = binding.enlaceRegistro
        enlaceRegistro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }

        apiManager = ApiManager(ApiClient.createApiService())

        // Verificar si ya existe un token guardado
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val token = sharedPreferences.getString("token", null)
        if (token != null) {
            // Si hay un token guardado, ir directamente a la MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish() // Finalizar LoginActivity para que no se pueda volver atrás
        }




        binding.bottonInicioDeSesion.setOnClickListener {
            val emailInicio = binding.editTextTextEmailAddress.text.toString()
            val passIncio = binding.editTextNumberPassword.text.toString()
            insertarLogin(emailInicio, passIncio)
        }
    }

    private fun insertarLogin(emailInicio: String, passIncio: String) {
        val data = LoginData(emailInicio, passIncio, "")
        apiManager.iniciarSesion(data, object : Callback<ApiClient.LoginResponse> {
            override fun onResponse(
                call: Call<ApiClient.LoginResponse>,
                response: Response<ApiClient.LoginResponse>
            ) {
                if (response.isSuccessful) {
                    val result = response.body()
                    val token = result?.token
                    val rolUsuario = result?.rol ?: ""
                    val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("token", token)
                    editor.putString("rolUsuario", rolUsuario)
                    editor.apply()

                    Toast.makeText(
                        this@LoginActivity,
                        "Inicio de sesión exitoso",
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    intent.putExtra("rolUsuario", rolUsuario)
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Error al iniciar sesión: ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ApiClient.LoginResponse>, t: Throwable) {
                Toast.makeText(
                    this@LoginActivity,
                    "Error al iniciar sesión: ${t.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}
