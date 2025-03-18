package com.jhon.educatioapp.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.google.firebase.firestore.FirebaseFirestore
import com.jhon.educatioapp.R
import com.jhon.educatioapp.apiservice.ApiService
import com.jhon.educatioapp.models.Clase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class MisClasesDocenteFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MisClasesAdapter
    private val clasesList = mutableListOf<Clase>()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var apiService: ApiService
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mis_clases_docente, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewMisClasesDocente)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = MisClasesAdapter(clasesList)
        recyclerView.adapter = adapter

        // Inicializar Retrofit para la API
        val retrofit = Retrofit.Builder()
            .baseUrl("https://bdeducatio.vercel.app/api/") // Reemplaza con la URL correcta de tu API
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
            Log.e(TAG, "Error: Token de autenticación vacío")
        } else {
            // Llamar al método para obtener el email del usuario desde la API
            obtenerEmailUsuario(token)
        }

        return view
    }

    private fun obtenerEmailUsuario(token: String) {
        // Realizar la llamada asíncrona para obtener el email del usuario
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val call = apiService.obtenerPerfilUsuario(token)
                val response = call.execute()

                if (response.isSuccessful) {
                    val emailUsuario = response.body()?.email ?: ""
                    Log.d(TAG, "Email del usuario obtenido: $emailUsuario")

                    // Consultar las clases aceptadas del usuario en Firestore
                    obtenerClasesAceptadas(emailUsuario)
                } else {
                    Log.e(TAG, "Error al obtener el email del usuario: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener el email del usuario", e)
            }
        }
    }

    private fun obtenerClasesAceptadas(emailUsuario: String) {
        db.collection("clases")
            .whereEqualTo("estado", "aceptado")
            .get()
            .addOnSuccessListener { querySnapshot ->
                clasesList.clear()
                for (document in querySnapshot.documents) {
                    val data = document.data
                    val aceptaciones = data?.get("aceptaciones") as? List<HashMap<String, Any>> ?: emptyList()
                    val correosAceptados = aceptaciones.mapNotNull { it["correo"] as? String }

                    if (emailUsuario in correosAceptados) {
                        val id = document.id
                        val emailClase = data?.get("email") as? String ?: ""
                        val nombreCompleto = data?.get("nombreCompleto") as? String ?: ""
                        val materia = data?.get("materia") as? String ?: ""
                        val tema = data?.get("tema") as? String ?: ""
                        val fechaTimestamp = data?.get("fecha") as? com.google.firebase.Timestamp
                        val fecha = fechaTimestamp?.toDate() ?: Date()
                        val horaInicio = data?.get("horaInicio") as? String ?: ""
                        val horaFin = data?.get("horaFin") as? String ?: ""
                        val modalidad = data?.get("modalidad") as? String ?: ""
                        val valorClase = data?.get("valorClase") as? String ?: ""

                        // Agregar los datos relevantes a la lista de clases
                        val claseItem = Clase(
                            id,
                            emailClase, // Utilizamos el emailClase en lugar del emailUsuario
                            materia,
                            tema,
                            fecha,
                            horaInicio,
                            horaFin,
                            modalidad,
                            valorClase,
                            nombreCompleto,  // Utilizamos el nombreAceptacion directamente como nombreCompleto
                            aceptaciones
                        )

                        // Agregamos la clase a la lista
                        clasesList.add(claseItem)
                    }
                }
                // Después de limpiar la lista de notificaciones si está vacía
                if (clasesList.isEmpty()) {
                    // Obtener una referencia al TextView con el ID textViewNoNotificaciones
                    val tvNoclases = view?.findViewById<TextView>(R.id.textViewNoclases)
                    // Mostrar el TextView y ocultar el RecyclerView
                    tvNoclases?.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    // Agregar un registro en el log para verificar si se envía el texto correctamente
                    Log.d(TAG, "No hay clases nuevas")
                } else {
                    // Si hay notificaciones, mostrar el RecyclerView y ocultar el TextView
                    val tvNoclases = view?.findViewById<TextView>(R.id.textViewNoclases)
                    tvNoclases?.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al obtener las clases aceptadas", exception)
                Toast.makeText(requireContext(), "Error al obtener las clases aceptadas", Toast.LENGTH_SHORT).show()
            }
    }


    inner class MisClasesAdapter(private val itemList: List<Clase>) :
        RecyclerView.Adapter<MisClasesAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val emailTextView: TextView = itemView.findViewById(R.id.textViewEmailClaseDocente)
            private val nombreTextView: TextView = itemView.findViewById(R.id.textViewNameClaseDocente)
            private val materiaTextView: TextView = itemView.findViewById(R.id.textViewMateriaClaseDocente)
            private val temaTextView: TextView = itemView.findViewById(R.id.textViewTemaClaseDocente)
            private val fechaTextView: TextView = itemView.findViewById(R.id.textViewFechaClaseDocente)
            private val horaInicioTextView: TextView = itemView.findViewById(R.id.textViewHoraInicioClaseDocente)
            private val horaFinTextView: TextView = itemView.findViewById(R.id.textViewHoraFinClaseDocente)
            private val modalidadTextView: TextView = itemView.findViewById(R.id.textViewModalidadClaseDocente)
            private val valorClaseTextView: TextView = itemView.findViewById(R.id.textViewValorClaseDocente)
            private val btnCancelarClase: Button = itemView.findViewById(R.id.btnCancelarClaseDocente)

            init {
                // Configurar el listener para el botón Cancelar Clase
                btnCancelarClase.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        val clase = itemList[position]
                        eliminarClase(clase)
                    }
                }
            }

            fun bind(item: Clase) {
                fechaTextView.text = SimpleDateFormat("dd MMM", Locale.getDefault()).format(item.fecha)
                emailTextView.text = "Correo del estudiante: ${item.email}"
                nombreTextView.text = "Nombre del estudiante: ${item.nombreCompleto}" // Mostrar el nombre completo
                materiaTextView.text = "Materia: ${item.materia}"
                temaTextView.text =  "Tema: ${item.tema}"
                horaInicioTextView.text = "Hora de inicio: ${item.horaInicio}"
                horaFinTextView.text = "Hora de finalización: ${item.horaFin}"
                modalidadTextView.text = item.modalidad
                valorClaseTextView.text = "$:${item.valorClase}"
            }
        }

        private fun eliminarClase(clase: Clase) {
            // Realizar la eliminación del documento en Firestore
            db.collection("clases")
                .document(clase.id)
                .delete()
                .addOnSuccessListener {
                    // Eliminación exitosa, mostrar mensaje o realizar alguna acción adicional si es necesario
                    Toast.makeText(requireContext(), "Clase cancelada correctamente", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error al cancelar la clase", exception)
                    Toast.makeText(requireContext(), "Error al cancelar la clase", Toast.LENGTH_SHORT).show()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mis_clases_docente, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(itemList[position])
        }

        override fun getItemCount(): Int = itemList.size
    }


    companion object {
        private const val TAG = "MisClasesDocenteFrag"
    }
}
