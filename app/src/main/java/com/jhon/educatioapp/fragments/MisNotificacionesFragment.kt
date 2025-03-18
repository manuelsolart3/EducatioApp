package com.jhon.educatioapp.fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jhon.educatioapp.R
import com.jhon.educatioapp.apiservice.ApiService
import com.jhon.educatioapp.models.NotificationItem
import com.jhon.educatioapp.models.UserFoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

class MisNotificacionesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NotificationAdapter
    private val notificationList = mutableListOf<NotificationItem>()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var apiService: ApiService
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var view: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        view = inflater.inflate(R.layout.fragment_mis_notificaciones, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewNotificaciones)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = NotificationAdapter(notificationList)
        recyclerView.adapter = adapter

        // Inicializar Retrofit para la API
        val retrofit = Retrofit.Builder()
            .baseUrl("https://bdeducatio.vercel.app/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Crear la instancia de ApiService
        apiService = retrofit.create(ApiService::class.java)

        // Inicializar SharedPreferences
        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener el token de autenticación
        val token = sharedPreferences.getString("token", "")

        // Verificar si el token está vacío
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "Error: Token de autenticación vacío")
        } else {
            // Llamar al método para obtener los datos del perfil del usuario
            obtenerDatosPerfilUsuario(token)
        }

    }

    private fun obtenerDatosPerfilUsuario(token: String) {
        // Realizar la llamada asíncrona para obtener los datos del perfil del usuario
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val call = apiService.obtenerPerfilUsuario(token)
                val response = call.execute()

                if (response.isSuccessful) {
                    val currentUserProfile = response.body()!!
                    Log.d(TAG, "Datos del perfil del usuario obtenidos: $currentUserProfile")

                    // Aquí puedes usar los datos del perfil del usuario (currentUserProfile)
                    // para obtener las notificaciones del usuario autenticado
                    obtenerPostulacionesUsuarioLogueado(currentUserProfile.email)
                } else {
                    Log.e(TAG, "Error al obtener los datos del perfil del usuario: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener los datos del perfil del usuario", e)
            }
        }
    }

    private fun obtenerPostulacionesUsuarioLogueado(currentUserEmail: String) {
        db.collection("clases")
            .whereEqualTo("estado", "postulado")
            .whereEqualTo("email", currentUserEmail)
            .get()
            .addOnSuccessListener { querySnapshot ->
                notificationList.clear()
                for (document in querySnapshot.documents) {
                    val data = document.data
                    val id = document.id
                    val email = data?.get("email") as? String ?: ""
                    val materia = data?.get("materia") as? String ?: ""
                    val tema = data?.get("tema") as? String ?: ""
                    val fecha = (data?.get("fecha") as? com.google.firebase.Timestamp)?.toDate() ?: Date()
                    val horaInicio = data?.get("horaInicio") as? String ?: ""
                    val horaFin = data?.get("horaFin") as? String ?: ""
                    val modalidad = data?.get("modalidad") as? String ?: ""
                    val valorClase = data?.get("valorClase") as? String ?: ""
                    val aceptaciones = data?.get("aceptaciones") as? List<HashMap<String, Any>> ?: emptyList()

                    for (aceptacion in aceptaciones) {
                        val nombre = aceptacion["nombre"] as? String ?: "Nombre no disponible"
                        val correo = aceptacion["correo"] as? String ?: "Correo no disponible"
                        val valorContraoferta = aceptacion["valorContraoferta"] as? String ?: ": No hay contraoferta"

                        // Verificar si la clase cumple con los requisitos para ser una postulación
                        if (email.isNotEmpty() && materia.isNotEmpty() && tema.isNotEmpty() && valorClase.isNotEmpty()) {
                            // Verificar si hay una contraoferta en las aceptaciones
                            val contraOferta = aceptacion["contraOferta"] as? Boolean ?: false
                            val estadoPostulacion = aceptacion["estadopostulacion"] as? String ?: ""

                            // Obtener el valor de la contraoferta si es una contraofert

                            // Agregar todos los datos relevantes a la notificación
                            val notificationItem = NotificationItem(
                                fecha.toString(),
                                email,
                                nombre,
                                correo,
                                valorClase,
                                contraOferta,
                                materia,
                                tema,
                                horaInicio,
                                horaFin,
                                modalidad,
                                id,
                                "postulado",
                                estadoPostulacion,
                                valorContraoferta  // Agregar el valor de la contraoferta a la notificación
                            )
                            notificationList.add(notificationItem)
                        }
                    }
                }
                // Después de limpiar la lista de notificaciones si está vacía
                if (notificationList.isEmpty()) {
                    // Obtener una referencia al TextView con el ID textViewNoNotificaciones
                    val tvNoNotificaciones = view?.findViewById<TextView>(R.id.textViewNoNotificaciones)
                    // Mostrar el TextView y ocultar el RecyclerView
                    tvNoNotificaciones?.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    // Agregar un registro en el log para verificar si se envía el texto correctamente
                    Log.d(TAG, "No hay postulaciones nuevas")
                } else {
                    // Si hay notificaciones, mostrar el RecyclerView y ocultar el TextView
                    val tvNoNotificaciones = view?.findViewById<TextView>(R.id.textViewNoNotificaciones)
                    tvNoNotificaciones?.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al obtener las postulaciones", exception)
                Toast.makeText(requireContext(), "Error al obtener las postulaciones", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarEstadoDocumento(documentId: String, notification: NotificationItem) {
        val documentRef = db.collection("clases").document(documentId)

        // Verificar si la notificación es una contraoferta y tiene un valor de contraoferta
        if (notification.estadoPostulacion == "contraOferta" && !notification.valorContraoferta.isNullOrBlank()) {
            // Actualizar el valor de valorClase con el valor de valorContraoferta
            documentRef
                .update("estado", "aceptado", "valorClase", notification.valorContraoferta)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Documento actualizado correctamente", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error al actualizar el documento", exception)
                    Toast.makeText(requireContext(), "Error al actualizar el documento", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Si no es una contraoferta o no tiene valor de contraoferta, solo actualizar el estado
            documentRef
                .update("estado", "aceptado")
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Documento actualizado correctamente", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error al actualizar el documento", exception)
                    Toast.makeText(requireContext(), "Error al actualizar el documento", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun borrarDocumento(documentId: String) {
        val documentRef = db.collection("clases").document(documentId)
        documentRef
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Documento eliminado correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error al eliminar el documento", exception)
                Toast.makeText(requireContext(), "Error al eliminar el documento", Toast.LENGTH_SHORT).show()
            }
    }

    private inner class NotificationAdapter(private val notificationList: List<NotificationItem>) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notificaciones, parent, false)
            return NotificationViewHolder(view)
        }

        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            val notification = notificationList[position]
            holder.bind(notification)
        }

        override fun getItemCount(): Int {
            return notificationList.size
        }

        inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tituloTextView: TextView = itemView.findViewById(R.id.tvNotificacionTitulo)
            private val estadoTextView: TextView = itemView.findViewById(R.id.tvEstadoDocumento)
            private val fechaTextView: TextView = itemView.findViewById(R.id.textViewFecha)
            private val emailTextView: TextView = itemView.findViewById(R.id.textViewEmail)
            private val nombreTextView: TextView = itemView.findViewById(R.id.textViewName)
            private val valorclaseTextView: TextView = itemView.findViewById(R.id.textViewValor)
            private val materiaTextView: TextView = itemView.findViewById(R.id.textViewMateria)
            private val temaTextView: TextView = itemView.findViewById(R.id.textViewTema)
            private val horaInicioTextView: TextView = itemView.findViewById(R.id.textViewHoraInicio)
            private val horaFinTextView: TextView = itemView.findViewById(R.id.textViewHoraFin)
            private val modalidadTextView: TextView = itemView.findViewById(R.id.textViewModalidad)
            private val contraoferta: TextView = itemView.findViewById(R.id.textViewValorContraoferta)
            private val aceptarButton: Button = itemView.findViewById(R.id.btnAceptar)
            private val ignorarButton: Button = itemView.findViewById(R.id.btnIgnorar)


            fun bind(notification: NotificationItem) {
                estadoTextView.text = if (notification.estadoPostulacion == "contraOferta") "contraOferta" else "Aceptado"
                estadoTextView.setBackgroundResource(if (notification.estadoPostulacion == "contraOferta") R.color.colorContraoferta else R.color.colorAceptado)
                estadoTextView.visibility = View.VISIBLE
                contraoferta.text = "Valor de contraoferta ${notification.valorContraoferta ?: ""}"
                tituloTextView.text = if (notification.estadoPostulacion == "contraOferta") "Nueva Contraoferta" else "Nueva Postulación"
                fechaTextView.text = notification.fecha
                nombreTextView.text = "Nombre Docente: ${notification.nombre ?: "Nombre no disponible"}"
                emailTextView.text = "Correo Docente: ${notification.correo ?: "Correo no disponible"}"
                valorclaseTextView.text = "$: ${notification.valorClase ?: "No hay contraofertas"}"
                materiaTextView.text = "Materia: ${notification.materia ?: "Materia no disponible"}"
                temaTextView.text = "Tema: ${notification.tema ?: "Tema no disponible"}"
                horaInicioTextView.text = "Hora Inicio: ${notification.horaInicio ?: "Hora no disponible"}"
                horaFinTextView.text = "Hora Fin: ${notification.horaFin ?: "Hora no disponible"}"
                modalidadTextView.text = "Modalidad: ${notification.modalidad ?: "Modalidad no disponible"}"

                aceptarButton.setOnClickListener {
                    actualizarEstadoDocumento(notification.id, notification)
                }

                ignorarButton.setOnClickListener {
                    borrarDocumento(notification.id)
                }
            }
        }
    }

    companion object {
        private const val TAG = "MisNotificacionesFrag"
    }
}