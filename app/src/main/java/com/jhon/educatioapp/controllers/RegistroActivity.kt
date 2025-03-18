package com.jhon.educatioapp.controllers

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jhon.educatioapp.R
import com.jhon.educatioapp.apiservice.ApiClient
import com.jhon.educatioapp.apiservice.ApiManager
import com.jhon.educatioapp.databinding.ActivityRegistroBinding
import com.jhon.educatioapp.models.UserData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.text.ParseException

class RegistroActivity : AppCompatActivity() {

    private lateinit var apiManager: ApiManager
    private lateinit var binding: ActivityRegistroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiManager = ApiManager(ApiClient.createApiService())

        binding.botonRegistro.setOnClickListener {
            registrarUsuario()
        }

        // Agregar TextWatcher al EditText de la fecha de nacimiento
        binding.fechaN.addTextChangedListener(DateTextWatcher())
    }

    private fun registrarUsuario() {
        val email = binding.email.text.toString()
        val contrasena = binding.contrasena.text.toString()
        val confirmarContrasena = binding.confirmarContrasena.text.toString()
        val name = binding.name.text.toString()
        val telefono = binding.telefono.text.toString()

        // Validar el formato del correo electrónico
        if (!validarEmail(email)) {
            Toast.makeText(
                this@RegistroActivity,
                "Formato de correo electrónico inválido",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (contrasena.length < 6) {
            Toast.makeText(
                this@RegistroActivity,
                "La contraseña debe tener al menos 6 caracteres",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (contrasena != confirmarContrasena) {
            Toast.makeText(
                this@RegistroActivity,
                "Las contraseñas no coinciden",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val fechaNacimiento = binding.fechaN.text.toString()
        if (!validarFechaNacimiento(fechaNacimiento)) {
            Toast.makeText(
                this@RegistroActivity,
                "Formato de fecha de nacimiento inválido (debe ser dd/MM/yyyy)",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Llamamos a la función para insertar datos en el servidor
        insertarDatos(email, contrasena, name, telefono)
    }

    private fun validarFechaNacimiento(fecha: String): Boolean {
        val regexFecha = "\\d{2}/\\d{2}/\\d{4}".toRegex()
        return fecha.matches(regexFecha)
    }
    private fun validarEmail(email: String): Boolean {
        val regexEmail = "[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}".toRegex()
        return email.matches(regexEmail)
    }

    private fun insertarDatos(email: String, contrasena: String, name: String, telefono: String) {
        val identificacion = binding.identificacion.text.toString()
        val ciudad = binding.ciudad.text.toString()

        val data = UserData(
            email = email,
            password = contrasena,
            NomCompleto = name,
            Telefono = telefono,
            N_Identificacion = identificacion,
            Ciudad = ciudad
        )

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val result = apiManager.insertarDatos(data)
                Toast.makeText(
                    this@RegistroActivity,
                    "Registro exitoso",
                    Toast.LENGTH_SHORT
                ).show()

                val intent = Intent(this@RegistroActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@RegistroActivity,
                    "Error al insertar datos: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // TextWatcher para agregar "/" automáticamente en el EditText de la fecha de nacimiento
    private inner class DateTextWatcher : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            s?.let { editable ->
                if (editable.length == 3 || editable.length == 6) {
                    if (editable[editable.length - 1] != '/') {
                        editable.insert(editable.length - 1, "/")
                    }
                }
            }
        }
    }


    companion object {
        const val TAG = "RegistroActivity"
    }
}
