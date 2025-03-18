package com.jhon.educatioapp.fragments

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.jhon.educatioapp.R
import com.jhon.educatioapp.apiservice.ApiService
import com.jhon.educatioapp.controllers.MainActivity
import com.jhon.educatioapp.databinding.FragmentSolicitarClaseBinding
import com.jhon.educatioapp.models.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class SolicitarFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var binding: FragmentSolicitarClaseBinding
    private lateinit var apiService: ApiService
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var currentUserProfile: UserProfile
    private lateinit var editTextHora: EditText
    private lateinit var editTextHorafin: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSolicitarClaseBinding.inflate(inflater, container, false)

        // Inicializar Retrofit para la API
        val retrofit = Retrofit.Builder()
            .baseUrl("https://bdeducatio.vercel.app/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Crear la instancia de ApiService
        apiService = retrofit.create(ApiService::class.java)

        // Inicializar SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        // Obtener el token de autenticación
        val token = sharedPreferences.getString("token", "")

        if (token.isNullOrEmpty()) {
            Log.e("SolicitarFragment", "Error: Token de autenticación vacío")
        } else {
            // Llamar al método para obtener los datos del perfil del usuario
            obtenerDatosPerfilUsuario(token)
        }

        val modalidad = arrayOf("Virtual", "Presencial")
        val adapterModalidad = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modalidad)
        adapterModalidad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerModalidad.adapter = adapterModalidad

        val materias = arrayOf("Matemáticas", "Física", "Química", "Biología", "Historia", "Geografía", "Literatura", "Inglés", "Economía")
        val adapterMaterias = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, materias)
        adapterMaterias.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMaterias.adapter = adapterMaterias

        binding.buttonGuardar.setOnClickListener {
            val materia = binding.spinnerMaterias.selectedItem.toString()
            val tema = binding.editTextTema.text.toString()
            val modalidad = binding.spinnerModalidad.selectedItem.toString()
            val fecha = getDateFromDatePicker(binding.datePickerFecha)
            val horaInicio = binding.editTextHora.text.toString()
            val horaFin = binding.editTextHorafin.text.toString()

            if (::currentUserProfile.isInitialized) {
                // Utilizar los datos del usuario actual
                val nombreCompleto = currentUserProfile.NomCompleto
                val email = currentUserProfile.email

                if (validarDuracionClase(horaInicio, horaFin)) {
                    val valorClase = binding.editTextValorClase.text.toString().toIntOrNull()

                    if (valorClase != null) {
                        if (valorClase < 15000) {
                            // Mostrar un mensaje de error si el valor de la clase es menor que 15000
                            Toast.makeText(requireContext(), "El valor de la clase no puede ser menor que 15000", Toast.LENGTH_SHORT).show()
                        } else if (valorClase > 50000) {
                            // Mostrar un mensaje de error si el valor de la clase es mayor que 50000
                            Toast.makeText(requireContext(), "El valor de la clase no puede ser mayor que 50000", Toast.LENGTH_SHORT).show()
                        } else {
                            // Guardar la clase en Firestore
                            guardarDatosEnFirestore(email, nombreCompleto, materia, tema, fecha, horaInicio, horaFin, modalidad, valorClase.toString())

                        }
                    } else {
                        // Mostrar un mensaje de error si el valor de la clase no es un número válido
                        Toast.makeText(requireContext(), "Por favor ingresa un valor válido para la clase", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Mostrar un mensaje de error si la duración no es válida
                    Toast.makeText(requireContext(), "La duración de la clase debe estar entre 1 y 3 horas", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Mensaje de error si no se obtuvieron los datos del perfil del usuario
                Toast.makeText(requireContext(), "Error al obtener los datos del perfil del usuario", Toast.LENGTH_SHORT).show()
            }
        }
        binding.editTextHora.addTextChangedListener(HoraTextWatcher(binding.editTextHora))
        binding.editTextHorafin.addTextChangedListener(HoraTextWatcher(binding.editTextHorafin))
        return binding.root
    }
    private inner class HoraTextWatcher(private val editText: EditText) : TextWatcher {
        private val horaPattern = Pattern.compile("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
        private var isUpdating = false

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            if (isUpdating) {
                return
            }
            isUpdating = true

            s?.let {
                val input = it.toString()
                val matcher = horaPattern.matcher(input)
                if (!matcher.matches()) {
                    editText.error = "Formato de hora inválido (HH:mm)"
                } else {
                    editText.error = null
                }
                if (input.length == 2 && !input.contains(":")) {
                    val newText = input.substring(0, 2) + ":" + input.substring(2)
                    editText.setText(newText)
                    editText.setSelection(newText.length)
                }
            }

            isUpdating = false
        }
    }



    private fun obtenerDatosPerfilUsuario(token: String) {
        // Realizar la llamada asíncrona para obtener los datos del perfil del usuario
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val call = apiService.obtenerPerfilUsuario(token)
                val response = call.execute()

                if (response.isSuccessful) {
                    val userProfile = response.body()
                    if (userProfile != null) {
                        // Imprimir la respuesta del servidor
                        Log.d("Respuesta del servidor", userProfile.toString())
                        // Guardar los datos del usuario actual
                        currentUserProfile = userProfile
                        // Llamar al método para actualizar la interfaz de usuario
                        actualizarInterfazUsuario(userProfile)
                    } else {
                        Log.e("SolicitarFragment", "Error: userProfile es nulo")
                    }
                } else {
                    Log.e("SolicitarFragment", "Error al obtener los datos del perfil del usuario: ${response.code()}")
                }
            } catch (e: Exception) {
                // Manejar errores de manera adecuada (por ejemplo, imprimir el error)
                Log.e("SolicitarFragment", "Error al obtener los datos del perfil del usuario", e)
            }
        }
    }

    private fun actualizarInterfazUsuario(userProfile: UserProfile) {
        // Actualizar los elementos de la interfaz de usuario con los datos del perfil del usuario
        activity?.runOnUiThread {
            binding.editTextEmail.setText(userProfile.email)
        }
    }

    private fun guardarDatosEnFirestore(email: String, nombreCompleto: String, materia: String, tema: String, fecha: Date, horainicio: String, horafin: String, modalidad: String, valorClase: String) {
        val clase = hashMapOf(
            "nombreCompleto" to nombreCompleto,
            "email" to email,
            "materia" to materia,
            "tema" to tema,
            "fecha" to fecha,
            "horaInicio" to horainicio, // Corregido el nombre del campo
            "horaFin" to horafin, // Corregido el nombre del campo
            "modalidad" to modalidad,
            "valorClase" to valorClase,
            "estado" to "pendiente", // Estado inicial
            "aceptaciones" to ArrayList<HashMap<String, Any>>() // Lista vacía para aceptaciones
        )

        // Subir la clase a Firestore
        db.collection("clases")
            .add(clase)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                mostrarPopupSolicitudExitosa() // Mostrar el popup al guardar exitosamente
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
    }

    private fun getDateFromDatePicker(datePicker: DatePicker): Date {
        val day = datePicker.dayOfMonth
        val month = datePicker.month
        val year = datePicker.year

        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        return calendar.time
    }

    private fun validarDuracionClase(horaInicio: String, horaFin: String): Boolean {
        val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())
        try {
            val horaInicioDate = formatoHora.parse(horaInicio)
            val horaFinDate = formatoHora.parse(horaFin)

            val diferenciaMilisegundos = horaFinDate.time - horaInicioDate.time
            val diferenciaHoras = TimeUnit.MILLISECONDS.toHours(diferenciaMilisegundos)

            return diferenciaHoras in 1..3
        } catch (e: ParseException) {
            Log.e(TAG, "Error al analizar las horas", e)
            return false
        }
    }

    private fun mostrarPopupSolicitudExitosa() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.popup_solicitar)

        val buttonOK = dialog.findViewById<Button>(R.id.buttonOK)
        buttonOK.setOnClickListener {
            dialog.dismiss()
            val navController = findNavController()
            // Navegar al HomeFragment
            navController.navigate(R.id.action_solicitarFragment_to_homeFragment)
        }

        dialog.show()
    }

    companion object {
        private const val TAG = "SolicitarFragment"
    }
}
