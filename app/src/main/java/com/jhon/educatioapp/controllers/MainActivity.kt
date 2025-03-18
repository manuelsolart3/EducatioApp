package com.jhon.educatioapp.controllers

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jhon.educatioapp.R
import com.jhon.educatioapp.apiservice.ApiService
import com.jhon.educatioapp.apiservice.WebServices
import com.jhon.educatioapp.databinding.ActivityMainBinding
import com.jhon.educatioapp.fragments.Hojavida
import com.jhon.educatioapp.fragments.HomeFragment
import com.jhon.educatioapp.fragments.MisNotificacionesFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var isNotificationsFragmentVisible = false
    private val homeFragment = HomeFragment()
    private val notificationsFragment = MisNotificacionesFragment()
    private lateinit var apiService: ApiService
    private lateinit var navView: NavigationView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        initUI()
        loadServices()


        supportFragmentManager.beginTransaction()
            .replace(R.id.contenedor_fragment, homeFragment)
            .commit()

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.Home_Fragment,
                R.id.solicitar_clase,
                R.id.mis_clases,
                R.id.perfil_usuario,
                R.id.postulacion_docente,
                R.id.mis_clases_docente,
                R.id.perfil_docente
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        // Llamada a la función obtenerTipoUsuarioDesdeAPI dentro de un bloque de corutina
        lifecycleScope.launch {
            val result = obtenerTipoUsuarioDesdeAPI()
            configurarMenu(navView.menu, result)
        }
        // Detectar clics en los ítems del menú
        navView.setNavigationItemSelectedListener { menuItem ->
            // Realizar la lógica de cambio de opciones según el ítem del menú seleccionado
            when (menuItem.itemId) {
                R.id.ser_docente -> {
                    lifecycleScope.launch {
                        val tipoUsuario = obtenerTipoUsuarioDesdeAPI()
                        if (tipoUsuario == "usuario") {
                            // Mostrar el Fragment Hojavida
                            val hojaVidaFragment = Hojavida()
                            hojaVidaFragment.show(supportFragmentManager, "hojavida_dialog")
                        } else if (tipoUsuario == "docente") {

                            // Cambiar a las opciones del tipo de usuario 1 (docente)
                            navController.navigate(R.id.Home_Fragment)
                            configurarMenu(navView.menu, "docente") // Actualizar el menú
                        }
                    }
                    true
                }

                R.id.mis_clases_docente -> {
                    // Navegar al fragmento de "mis_clases"
                    navController.navigate(R.id.mis_clases_docente)
                    true
                }

                R.id.perfil_docente -> {
                    // Navegar al fragmento de "mis_clases"
                    navController.navigate(R.id.perfil_docente)
                    true
                }

                R.id.postulacion_docente -> {
                    // Navegar al fragmento de "mis_clases"
                    navController.navigate(R.id.postulacion_docente)
                    true
                }

                R.id.Home_Fragment -> {
                    // Navegar al fragmento de "mis_clases"
                    navController.navigate(R.id.Home_Fragment)
                    true
                }

                R.id.ser_usuario -> {
                    // Cambiar a las opciones del tipo de usuario 0 (estudiante)
                    // Por ejemplo, navegar a un fragmento específico
                    navController.navigate(R.id.Home_Fragment)
                    configurarMenu(navView.menu, "usuario") // Actualizar el menú
                    true
                }

                R.id.solicitar_clase -> {
                    // Navegar al fragmento de "solicitar_clase"
                    navController.navigate(R.id.solicitar_clase)
                    true
                }

                R.id.mis_clases -> {
                    // Navegar al fragmento de "mis_clases"
                    navController.navigate(R.id.mis_clases)
                    true
                }

                R.id.perfil_usuario -> {
                    // Navegar al fragmento de "perfil_usuario"
                    navController.navigate(R.id.perfil_usuario)
                    true
                }
                // Agregar más casos según sea necesario para otros items del menú
                else -> false
            }
        }
    }

    private fun initUI() {
        setupListener()
    }

    private fun setupListener() {
        binding.appBarMain.fab.setOnClickListener { view ->
            val fragmentManager = supportFragmentManager
            val currentFragment =
                fragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)

            if (currentFragment != null && currentFragment is MisNotificacionesFragment) {
                // Cerrar el fragmento de notificaciones
                fragmentManager.beginTransaction().remove(currentFragment).commit()
            } else {
                // Abrir el fragmento de notificaciones
                val fragmentTransaction = fragmentManager.beginTransaction()
                fragmentTransaction.replace(
                    R.id.nav_host_fragment_content_main,
                    notificationsFragment
                )
                fragmentTransaction.addToBackStack(null)
                fragmentTransaction.commit()
            }
        }
    }


    private fun loadServices() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://bdeducatio.vercel.app/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Crear la instancia de ApiService
        apiService = retrofit.create(ApiService::class.java)
    }

    private fun loadToken(): String? {
        val editor = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return editor.getString("token", null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.cerrar_sesion -> {
                // Aquí puedes agregar la lógica para cerrar sesión
                cerrarSesion()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private suspend fun obtenerTipoUsuarioDesdeAPI(): String {
        val token = loadToken()
        Log.e("TAG", "${token}")
        try {
            val response = WebServices.web.obtenerProfile(token!!)
            return response.rol
        } catch (e: Exception) {
            Log.e("TAG", "${e}")
            return "" // O un valor por defecto en caso de error
        }

    }

    private fun cerrarSesion() {
        // Limpiar el token en SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("token")
        editor.apply()

        // Redirigir al usuario a la pantalla de inicio de sesión
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish() // Finalizar MainActivity
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }


    private fun configurarMenu(menu: Menu, tipoUsuario: String) {

        menu.findItem(R.id.ser_docente)?.isVisible = (tipoUsuario == "usuario")
        menu.findItem(R.id.solicitar_clase)?.isVisible = (tipoUsuario == "usuario")
        menu.findItem(R.id.mis_clases)?.isVisible = (tipoUsuario == "usuario")
        menu.findItem(R.id.perfil_usuario)?.isVisible = (tipoUsuario == "usuario")

        menu.findItem(R.id.ser_usuario)?.isVisible = (tipoUsuario == "docente")
        menu.findItem(R.id.postulacion_docente)?.isVisible = (tipoUsuario == "docente")
        menu.findItem(R.id.mis_clases_docente)?.isVisible = (tipoUsuario == "docente")
        menu.findItem(R.id.perfil_docente)?.isVisible = (tipoUsuario == "docente")
    }
}
