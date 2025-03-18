package com.jhon.educatioapp.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.jhon.educatioapp.R
import com.jhon.educatioapp.apiservice.ApiService
import com.jhon.educatioapp.databinding.FragmentPostulacionDocenteBinding
import com.jhon.educatioapp.models.Clase
import com.jhon.educatioapp.models.UserFoto
import com.jhon.educatioapp.models.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PostulacionDocenteFragment : Fragment() {

    private lateinit var binding: FragmentPostulacionDocenteBinding
    private val db = FirebaseFirestore.getInstance()
    private val clasesCollection = db.collection("clases")
    private val clasesList = mutableListOf<Clase>()
    private val adapter = ClaseAdapter(clasesList)
    private lateinit var apiService: ApiService
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var currentUserProfile: UserProfile
    private var isModifyingValue = false
    private var userFotoList: List<UserFoto>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPostulacionDocenteBinding.inflate(inflater, container, false)
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

        // Verificar si el token está vacío
        if (token.isNullOrEmpty()) {
            Log.e("SolicitarFragment", "Error: Token de autenticación vacío")
        } else {
            // Llamar al método para obtener los datos del perfil del usuario
            obtenerDatosPerfilUsuario(token)
        }

        // Obtener la lista de usuarios desde la API
        fetchUsers()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        obtenerClasesDesdeFirestore()
    }

    private fun setupRecyclerView() {
        binding.recyclerViewClases.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewClases.adapter = adapter
    }

    private fun obtenerDatosPerfilUsuario(token: String) {
        // Realizar la llamada asíncrona para obtener los datos del perfil del usuario
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val call = apiService.obtenerPerfilUsuario(token)
                val response = call.execute()

                if (response.isSuccessful) {
                    currentUserProfile = response.body()!!
                    Log.d(TAG, "Datos del perfil del usuario obtenidos: $currentUserProfile")
                } else {
                    Log.e("SolicitarFragment", "Error al obtener los datos del perfil del usuario: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SolicitarFragment", "Error al obtener los datos del perfil del usuario", e)
            }
        }
    }

    private fun obtenerClasesDesdeFirestore() {
        clasesCollection.whereIn("estado", listOf("postulado", "pendiente"))
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                clasesList.clear()
                for (document in querySnapshot.documents) {
                    val data = document.data
                    val estado = data?.get("estado") as? String ?: ""
                    if (estado in listOf("postulado", "pendiente")) {
                        val id = document.id
                        val email = data?.get("email") as? String ?: ""
                        val nombreCompleto = data?.get("nombreCompleto") as? String ?: ""
                        val materia = data?.get("materia") as? String ?: ""
                        val tema = data?.get("tema") as? String ?: ""
                        val fecha = (data?.get("fecha") as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                        val horaInicio = data?.get("horaInicio") as? String ?: ""
                        val horaFin = data?.get("horaFin") as? String ?: ""
                        val modalidad = data?.get("modalidad") as? String ?: ""
                        val valorClase = data?.get("valorClase") as? String ?: ""

                        // Verificar y castear aceptaciones como List<HashMap<String, Any>>
                        val aceptacionesObject = data?.get("aceptaciones")
                        val aceptaciones = if (aceptacionesObject is List<*>) {
                            aceptacionesObject.filterIsInstance<HashMap<String, Any>>()
                        } else {
                            emptyList()
                        }

                        val clase = Clase(
                            id,
                            nombreCompleto,
                            materia,
                            tema,
                            fecha,
                            horaInicio,
                            horaFin,
                            modalidad,
                            valorClase,
                            estado,
                            aceptaciones  // Agregar las aceptaciones a la clase
                        )

                        // Buscar el usuario correspondiente en la lista de usuarios y asignar la foto de perfil
                        val user = userFotoList?.find { it.email == email }
                        if (user != null) {
                            clase.foto = user.foto
                        }

                        clasesList.add(clase)
                    }
                }
                adapter.notifyDataSetChanged()
                for (clase in clasesList) {
                    Log.d(TAG, "Clase obtenida: $clase")
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al obtener las clases", exception)
                Toast.makeText(requireContext(), "Error al obtener las clases", Toast.LENGTH_SHORT).show()
            }
    }



    private inner class ClaseAdapter(private val clases: List<Clase>) :
        RecyclerView.Adapter<ClaseAdapter.ClaseViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClaseViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_postulacion_docente, parent, false)
            return ClaseViewHolder(view)
        }

        override fun onBindViewHolder(holder: ClaseViewHolder, position: Int) {
            val clase = clases[position]
            holder.bind(clase)
        }

        override fun getItemCount() = clases.size

        inner class ClaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val imageViewPerfil: ImageView = itemView.findViewById(R.id.imageViewPerfil)
            private val textViewEstado: TextView = itemView.findViewById(R.id.textViewEstado)
            private val tvListadoClasesName: TextView = itemView.findViewById(R.id.tvListadoClasesName)
            private val textViewMateria: TextView = itemView.findViewById(R.id.textViewMateria)
            private val textViewTema: TextView = itemView.findViewById(R.id.textViewTema)
            private val textViewFecha: TextView = itemView.findViewById(R.id.textViewFecha)
            private val textViewHoraInicio: TextView = itemView.findViewById(R.id.textViewHoraInicio)
            private val textViewHoraFin: TextView = itemView.findViewById(R.id.textViewHoraFin)
            private val textViewModalidad: TextView = itemView.findViewById(R.id.textViewModalidad)
            private val textViewValorClase: TextView = itemView.findViewById(R.id.textViewValorClase)
            private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
            private val btnCounteroffer: Button = itemView.findViewById(R.id.btnCounteroffer)

            init {
                btnAccept.setOnClickListener {
                    if (!isModifyingValue) {
                        val clase = clases[adapterPosition]
                        val currentUserEmail = currentUserProfile.email // Obtenemos el email del usuario del perfil
                        if (currentUserEmail != null) {
                            agregarAceptacion(clase, currentUserEmail)
                        } else {
                            Toast.makeText(itemView.context, "Usuario no logueado", Toast.LENGTH_SHORT).show()
                        }
                    }}

                btnCounteroffer.setOnClickListener {
                    val clase = clases[adapterPosition]
                    val currentUserEmail = currentUserProfile.email // Obtenemos el email del usuario del perfil
                    if (currentUserEmail != null) {
                        enviarContraoferta(clase, currentUserEmail)
                    } else {
                        Toast.makeText(itemView.context, "Usuario no logueado", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            fun bind(clase: Clase) {
                tvListadoClasesName.text = "El usuario ${clase.email}"
                textViewMateria.text = "Materia: ${clase.materia}"
                textViewTema.text = "Tema: ${clase.tema}"
                val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
                val fechaFormateada = dateFormat.format(clase.fecha)
                textViewFecha.text = fechaFormateada
                textViewHoraInicio.text = clase.horaInicio
                textViewHoraFin.text = clase.horaFin
                textViewModalidad.text = clase.modalidad
                textViewValorClase.text = "$${clase.valorClase}"

                // Cargar la foto de perfil utilizando Glide u otra biblioteca
                if (!clase.foto.isNullOrEmpty()) {
                    Log.d(TAG, "Cargando imagen desde ${clase.foto}")
                    Glide.with(imageViewPerfil.context)
                        .load(clase.foto)
                        .placeholder(R.drawable.perfil)
                        .error(R.drawable.dos)
                        .override(200, 200)
                        .into(imageViewPerfil)
                } else {
                    Log.d(TAG, "URL de la imagen vacía para ${clase.email}")
                }

            }
        }
    }

    private fun agregarAceptacion(clase: Clase, currentUserEmail: String) {
        val fechaFirestore = com.google.firebase.Timestamp(clase.fecha)
        val fechaActual = Date()

        // Actualizar el estado de la clase a "postulado" en la colección "clases"
        val claseRef = db.collection("clases").document(clase.id)
        claseRef.update("estado", "postulado")
            .addOnSuccessListener {
                Log.d(TAG, "Estado de la clase actualizado a 'postulado'")

                // Obtener el campo de aceptaciones y agregar los nuevos datos
                val aceptaciones = clase.aceptaciones.toMutableList()
                aceptaciones.add(
                    hashMapOf(
                        "correo" to currentUserProfile.email,
                        "nombre" to currentUserProfile.NomCompleto,
                        "telefono" to currentUserProfile.Telefono,
                        "fechaPostulacion" to fechaActual
                    )
                )

                // Actualizar el campo de aceptaciones en el documento de la clase
                claseRef.update("aceptaciones", aceptaciones)
                    .addOnSuccessListener {
                        Log.d(TAG, "Datos de aceptación actualizados correctamente")
                        val builder = AlertDialog.Builder(requireContext())
                        val inflater = requireActivity().layoutInflater
                        val dialogView = inflater.inflate(R.layout.popup_postular, null)

                        builder.setView(dialogView)
                        val dialog = builder.create()

                        dialogView.findViewById<Button>(R.id.buttonOK).setOnClickListener {
                            dialog.dismiss()
                        }

                        dialog.show()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al actualizar los datos de aceptación", e)
                        Toast.makeText(requireContext(), "Error al actualizar los datos de aceptación", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al actualizar el estado de la clase", e)
                Toast.makeText(requireContext(), "Error al actualizar el estado de la clase", Toast.LENGTH_SHORT).show()
            }
    }




    private fun enviarContraoferta(clase: Clase, currentUserEmail: String) {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.activity_splash, null)

        builder.setView(dialogView)
        val dialog = builder.create()

        val mensajeTextView = dialogView.findViewById<TextView>(R.id.mensajeTextView)
        val btnGuardarContra = dialogView.findViewById<Button>(R.id.btnGuardarContra)
        val editTextNewValue = dialogView.findViewById<EditText>(R.id.editTextNewValue)

        btnGuardarContra.setOnClickListener {
            val nuevoValor = editTextNewValue.text.toString()
            if (nuevoValor.isBlank() || nuevoValor.toIntOrNull()?.let { it < 15000 } == true) {
                mensajeTextView.text = "El valor de la contraoferta debe ser mayor o igual a 15000"
                mensajeTextView.visibility = View.VISIBLE
            } else {
                val fechaActual = Date()

                // Obtener las aceptaciones actuales
                val aceptaciones = clase.aceptaciones.toMutableList()

                // Agregar nueva información a las aceptaciones
                val nuevaAceptacion: HashMap<String, Any> = hashMapOf(
                    "correo" to currentUserProfile.email,
                    "nombre" to currentUserProfile.NomCompleto,
                    "telefono" to currentUserProfile.Telefono,
                    "fechaPostulacion" to fechaActual,
                    "estadopostulacion" to "contraOferta",
                    "valorContraoferta" to nuevoValor
                )
                aceptaciones.add(nuevaAceptacion)

                // Actualizar las aceptaciones en el documento de la clase
                val claseRef = db.collection("clases").document(clase.id)
                claseRef.update("aceptaciones", aceptaciones)
                    .addOnSuccessListener {
                        Log.d(TAG, "Datos del usuario que contraoferta agregados a las aceptaciones")

                        // Actualizar el estado a "postulado"
                        claseRef.update("estado", "postulado")
                            .addOnSuccessListener {
                                Log.d(TAG, "Estado de la clase actualizado a 'postulado'")

                                // Mostrar el popup de contraoferta exitosa
                                dialog.dismiss()
                                mostrarPopupContraofertaExitosa()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error al actualizar el estado de la clase", e)
                                Toast.makeText(requireContext(), "Error al actualizar el estado de la clase", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al actualizar las aceptaciones", e)
                        Toast.makeText(requireContext(), "Error al actualizar las aceptaciones", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        dialog.show()
    }


    private fun mostrarPopupContraofertaExitosa() {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.popup_contraofertar, null)

        builder.setView(dialogView)
        val dialog = builder.create()

        dialogView.findViewById<Button>(R.id.buttonOK).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    companion object {
        private const val TAG = "PostulacionDocente"
    }
    private fun fetchUsers() {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val apiResponse = withContext(Dispatchers.IO) {
                    apiService.getUsers()
                }
                val userProfileList = apiResponse.userProfiles
                Log.d(TAG, "Lista de usuarios obtenida: $userProfileList")
                userFotoList = userProfileList
                obtenerClasesDesdeFirestore()
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener los usuarios", e)
            }
        }
    }
}
