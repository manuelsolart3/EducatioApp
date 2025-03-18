package com.jhon.educatioapp.fragments

import VerificacionHojaFragment
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.jhon.educatioapp.R
import com.jhon.educatioapp.apiservice.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*

class Hojavida : DialogFragment() {
    private val db = FirebaseFirestore.getInstance()
    private val PICK_FILE_REQUEST = 1
    private lateinit var storage: FirebaseStorage
    private var fileUri: Uri? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_hojavida, container, false)
        view.findViewById<ImageView>(R.id.imageViewClose).setOnClickListener { dismiss() }
        view.findViewById<Button>(R.id.subirHoja).setOnClickListener { checkPermissions() }
        storage = FirebaseStorage.getInstance("gs://cargar-imagenes-yt-e6d7a.appspot.com")
        return view
    }

    private fun checkPermissions() {
        // Aquí puedes agregar la lógica para verificar los permisos necesarios
        seleccionarArchivo()
    }

    private fun seleccionarArchivo() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*" // Aceptar cualquier tipo de archivo
        startActivityForResult(intent, PICK_FILE_REQUEST)
    }

    private fun subirArchivo() {
        if (fileUri != null) {
            val storageRef = storage.reference
            val fileRef = storageRef.child("hojas_vida/${UUID.randomUUID()}") // Nombre aleatorio para evitar colisiones de nombres de archivos
            fileRef.putFile(fileUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    // Archivo subido exitosamente
                    taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                        Log.d("Hojavida", "Archivo subido correctamente. URL: $uri")

                        // Obtener el token de autenticación desde SharedPreferences
                        val sharedPreferences = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                        val token = sharedPreferences.getString("token", null)

                        if (token != null) {
                            val apiService = ApiClient.createApiService()
                            val datos = ApiClient.DatosAServidorRequest(token, uri.toString())
                            apiService.enviarDatosAlServidor(token, datos).enqueue(object :
                                Callback<ApiClient.ResponseApi> {
                                override fun onResponse(call: Call<ApiClient.ResponseApi>, response: Response<ApiClient.ResponseApi>) {
                                    if (response.isSuccessful) {
                                        Log.d("Hojavida", "URL del archivo enviada al servidor correctamente")
                                        val dialog = VerificacionHojaFragment()
                                        dialog.show(requireActivity().supportFragmentManager, "VerificacionHojaFragment")
                                    } else {
                                        Log.e("Hojavida", "Error al enviar la URL del archivo al servidor: ${response.message()}")
                                    }
                                }

                                override fun onFailure(call: Call<ApiClient.ResponseApi>, t: Throwable) {
                                    Log.e("Hojavida", "Error al enviar la URL del archivo al servidor", t)
                                }
                            })
                        } else {
                            Log.e("Hojavida", "No se encontró el token de autenticación")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Manejar errores durante la subida
                    Log.e("Hojavida", "Error al subir el archivo: ${e.message}")
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            fileUri = data.data
            Log.d("Hojavida", "URI del archivo seleccionado: $fileUri")
            subirArchivo()
        }
    }
}