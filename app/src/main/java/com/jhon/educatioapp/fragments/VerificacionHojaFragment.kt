import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.jhon.educatioapp.controllers.MainActivity

class VerificacionHojaFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setMessage("Espere un momento, el administrador está verificando su hoja de vida.")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Navegar al menú principal aquí
        navigateToMainMenu()
    }

    private fun navigateToMainMenu() {
        // Aquí debes agregar el código para navegar al menú principal de tu aplicación
        // Puedes utilizar un Intent para iniciar la actividad del menú principal
        val intent = Intent(requireContext(), MainActivity::class.java)
        startActivity(intent)
        // También puedes usar métodos de navegación como NavHostFragment.findNavController() si estás utilizando Navigation Component
    }
}

