package com.jhon.educatioapp.fragments


import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import com.jhon.educatioapp.R
import  com.jhon.educatioapp.R.layout.fragment_perfil_docente
import com.jhon.educatioapp.apiservice.ApiService
import com.jhon.educatioapp.models.UserProfile

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.jhon.educatioapp.apiservice.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.util.UUID

class PerfilDocente : Fragment() {
    private lateinit var apiService: ApiService
    private lateinit var sharedPreferences: SharedPreferences
    private val PICK_IMAGE_REQUEST = 1
    private val REQUEST_READ_EXTERNAL_STORAGE_PERMISSION = 2
    private lateinit var imageView: ImageView
    private lateinit var storage: FirebaseStorage
    private var imageUri: Uri? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_perfil_docente, container, false)

        // Inicializar Retrofit
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

        // Si el token está vacío, deberás implementar la lógica para obtenerlo (por ejemplo, iniciar sesión)
        if (token.isNullOrEmpty()) {
            // Aquí deberías iniciar sesión y obtener el token
            // Una vez que tengas el token, guárdalo en SharedPreferences
            with(sharedPreferences.edit()) {
                putString("token", "your_token_here")
                apply()
            }
        } else {
            // Llamar al método para obtener los datos del perfil del usuario
            obtenerDatosPerfilUsuario(view, token)
        }

        imageView = view.findViewById(R.id.imageView)
        val btnChangePhoto = view.findViewById<Button>(R.id.btnChangePhoto)
        btnChangePhoto.setOnClickListener {
            Log.d("PerfilUsuarioFragment", "Botón 'Cambiar Foto' presionado")
            openFileChooser()
        }
        storage = FirebaseStorage.getInstance("gs://cargar-imagenes-yt-e6d7a.appspot.com")

        return view
    }

    private fun obtenerDatosPerfilUsuario(view: View, token: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val call = apiService.obtenerPerfilUsuario(token)
                val response = call.execute()

                if (response.isSuccessful) {
                    val userProfile = response.body()
                    if (userProfile != null) {
                        Log.d("Respuesta del servidor", userProfile.toString())
                        with(sharedPreferences.edit()) {
                            putString("userEmail", userProfile.email) // Store the user's email
                            putString("user_image_url", userProfile.imageUrl)
                            apply()
                        }
                        actualizarInterfazUsuario(view, userProfile)
                        Log.d("exito", "Mensaje de depuración aquí con exito")
                    } else {
                        Log.e("PerfilUsuarioFragment", "Error: userProfile es nulo")
                    }
                } else {
                    Log.e(
                        "PerfilUsuarioFragment",
                        "Error al obtener los datos del perfil del usuario: ${response.code()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(
                    "PerfilUsuarioFragment",
                    "Error al obtener los datos del perfil del usuario",
                    e
                )
            }
        }
    }
    private fun actualizarInterfazUsuario(view: View, userProfile: UserProfile) {
        // Actualizar los elementos de la interfaz de usuario con los datos del perfil del usuario
        activity?.runOnUiThread {
            view.findViewById<TextView>(R.id.NomCom2).text = userProfile.NomCompleto
            view.findViewById<AppCompatButton>(R.id.appCompatButton3).text = userProfile.Telefono.toString()
            view.findViewById<AppCompatButton>(R.id.appCompatButton2).text = userProfile.Ciudad
            view.findViewById<AppCompatButton>(R.id.appCompatButton).text = userProfile.email
            // En el método actualizarInterfazUsuario
            val userEmail = sharedPreferences.getString("userEmail", "")
            val imageUrlKey = "imageUrl_$userEmail" // Use the user's email to construct the key
            val imageUrl = sharedPreferences.getString(imageUrlKey, "")
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(requireContext())
                    .load(imageUrl)
                    .into(imageView)
            } else {
                // Manejar el caso en que la URL de la imagen sea nula o vacía
                // Puedes mostrar una imagen predeterminada o un mensaje de error en este caso
                Log.e("PerfilDocenteFragment", "La URL de la imagen es nula o vacía")
            }
// Agregar un registro para verificar la URL de la imagen en SharedPreferences
            Log.d(
                "PerfilUsuarioFragment",
                "URL de la imagen obtenida de SharedPreferences: $imageUrl"
            )

        }
    }


    private fun openFileChooser() {
        val permission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_READ_EXTERNAL_STORAGE_PERMISSION
            )
        } else {
            val intent = Intent()
            intent.type = "image/*"
            intent.action = Intent.ACTION_GET_CONTENT
            startActivityForResult(
                Intent.createChooser(intent, "Select Picture"),
                PICK_IMAGE_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openFileChooser()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Se necesita el permiso para acceder a las imágenes",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            try {
                val bitmap =
                    MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
                imageView.setImageBitmap(bitmap)
                subirImagenFirebaseStorage(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Error al cargar la imagen", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun subirImagenFirebaseStorage(bitmap: Bitmap) {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()

        val storageRef = storage.reference
        val fileRef = storageRef.child("fotos_perfil/${UUID.randomUUID()}.jpg")
        val uploadTask = fileRef.putBytes(data)

        uploadTask.addOnSuccessListener { taskSnapshot ->
            taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                // Verificar que la URL de la imagen no sea nula o vacía
                if (!imageUrl.isNullOrEmpty()) {
                    // URL de la imagen obtenida con éxito, proceder a enviarla al servidor
                    enviarUrlFotoAlServidor(imageUrl)
                } else {
                    // Manejar el caso en que la URL de la imagen es nula o vacía
                    Toast.makeText(
                        requireContext(),
                        "La URL de la imagen es nula o vacía",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.addOnFailureListener { exception ->
            Toast.makeText(
                requireContext(),
                "Error al subir la imagen: ${exception.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun enviarUrlFotoAlServidor(imageUrl: String) {
        if (imageUrl.isNullOrEmpty()) {
            Toast.makeText(
                requireContext(),
                "La URL de la imagen es nula o vacía",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            val token = sharedPreferences.getString("token", "")

            if (!token.isNullOrEmpty()) {
                val apiService = ApiClient.createApiService()
                val request = ApiClient.UrlFotoRequest(token, imageUrl)
                apiService.enviarUrlFoto(token!!, request)
                    .enqueue(object : Callback<ApiClient.ResponseApi> {
                        override fun onResponse(
                            call: Call<ApiClient.ResponseApi>,
                            response: Response<ApiClient.ResponseApi>
                        ) {
                            if (response.isSuccessful) {
                                Log.d(
                                    "PerfilUsuarioFragment",
                                    "URL de la foto enviada correctamente al servidor"
                                )
                                with(sharedPreferences.edit()) {
                                    val userEmail = sharedPreferences.getString("userEmail", "")
                                    val imageUrlKey = "imageUrl_$userEmail" // Use the user's email to construct the key
                                    putString(imageUrlKey, imageUrl)
                                    apply()
                                }

                                Log.d(
                                    "PerfilUsuarioFragment",
                                    "Nueva URL de la imagen guardada en SharedPreferences: $imageUrl"
                                )
                            } else {
                                val errorBody = response.errorBody()?.string()
                                Log.e(
                                    "PerfilUsuarioFragment",
                                    "Error al enviar la URL de la foto al servidor: ${response.code()}, $errorBody"
                                )
                            }
                        }

                        override fun onFailure(call: Call<ApiClient.ResponseApi>, t: Throwable) {
                            Log.e(
                                "PerfilUsuarioFragment",
                                "Error al enviar la URL de la foto al servidor",
                                t
                            )
                        }
                    })
            }
        }
    }
}