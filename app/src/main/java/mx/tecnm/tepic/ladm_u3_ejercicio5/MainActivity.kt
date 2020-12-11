package mx.tecnm.tepic.ladm_u3_ejercicio5

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import java.time.Clock
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {
    var baseDatos = BaseDatos(this, "AGENDA", null, 1)
    var listaID = ArrayList<String>()
    var idSeleccionadoEnLista = -1

    // Firebase
    var baseRemota = FirebaseFirestore.getInstance()
    var datos = ArrayList<String>()
    var datos2 = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            insertar()
        }

        editTextDate.setOnClickListener {
            showDatePickerDialog()
        }

        editTextTime.setOnClickListener {
            showTimePickerDialog()
        }

        button2.setOnClickListener {
            sincronizar()
        }

        cargarEventos()
    }

    override fun onResume() {
        super.onResume()
        cargarEventos()
    }

    private fun showTimePickerDialog() {
        val newFragment = TimePickerFragment.newInstance{ _, hour, minute ->
            var selectedTime = ""
            if (minute < 10) {
                selectedTime = "$hour:0$minute"
            } else {
                selectedTime = "$hour:$minute"
            }
            editTextTime.setText(selectedTime)
        }

        newFragment.show(supportFragmentManager, "timePicker")
    }

    private fun showDatePickerDialog() {
        val newFragment = DatePickerFragment.newInstance { _, year, month, day ->
            // +1 because January is zero
            val selectedDate = day.toString() + "/" + (month + 1) + "/" + year
            editTextDate.setText(selectedDate)
        }

        newFragment.show(supportFragmentManager, "datePicker")
    }

    private fun insertar() {
        try {
            var trans = baseDatos.writableDatabase // Permite leer y escribir
            var variables = ContentValues()

            variables.put("ID", editText.text.toString().toInt())
            variables.put("LUGAR", editText2.text.toString())
            variables.put("HORA", editTextTime.text.toString())
            variables.put("FECHA", editTextDate.text.toString())
            variables.put("DESCRIPCION", editText3.text.toString())

            var respuesta = trans.insert("EVENTO", null, variables)
            if (respuesta == -1L) {
                mensaje("ERROR NO SE PUDO INSERTAR")
            } else {
                mensaje("SE INSERTÓ CON EXITO")
                limpiarCampos()
            }
            trans.close()

        } catch (e: SQLiteException) {
            mensaje(e.message!!)
        }
        cargarEventos()
    }

    private fun cargarEventos() {
        try {
            var trans = baseDatos.readableDatabase
            var eventos = ArrayList<String>()

            var respuesta = trans.query("EVENTO", arrayOf("*"), null, null, null, null, null)

            listaID.clear()
            if (respuesta.moveToFirst()) {
                do {
                    var concatenacion = "ID: ${respuesta.getInt(0)}\nLUGAR: ${respuesta.getString(1)}\n" +
                            "HORA: ${respuesta.getString(2)}\nFECHA: ${respuesta.getString(3)}\nDESCRIPCION: ${respuesta.getString(4)}"
                    eventos.add(concatenacion)
                    listaID.add(respuesta.getInt(0).toString())
                } while (respuesta.moveToNext())

            } else {
                eventos.add("NO HAY EVENTOS INSERTADOS")
            }
            listaAgenda.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, eventos)

            // ligando el menulist con listView
            this.registerForContextMenu(listaAgenda)

            listaAgenda.setOnItemClickListener { adapterView, view, i, l ->
                idSeleccionadoEnLista = i
                Toast.makeText(this,"Se seleccionó elemento", Toast.LENGTH_LONG)
                    .show()
            }
            trans.close()

        } catch (e:SQLiteException) {
            mensaje("ERROR: " + e.message!!)
        }
    }

    private fun limpiarCampos() {
        editText.setText("")
        editTextTime.setText("")
        editTextDate.setText("")
        editText2.setText("")
        editText3.setText("")
    }

    private fun mensaje(s: String) {
        AlertDialog.Builder(this)
            .setTitle("ATENCION")
            .setMessage(s)
            .setPositiveButton("Ok") {d, i -> d.dismiss()}
            .show()
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)

        var inflaterOB = menuInflater

        inflaterOB.inflate(R.menu.menulist, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {

        if (idSeleccionadoEnLista == -1) {
            mensaje("ERROR! Debes dar clic primero en un Item para ACTUALIZAR/ELIMINAR")
            return true
        }

        when (item.itemId) {
            R.id.itemActualizar -> {
                var ventana2 = Intent(this, MainActivity2::class.java)

                ventana2.putExtra("idactualizar", listaID.get(idSeleccionadoEnLista))
                startActivity(ventana2)
            }
            R.id.itemEliminar -> {
                var idEliminar = listaID.get(idSeleccionadoEnLista)
                AlertDialog.Builder(this)
                    .setTitle("ATENCIÓN")
                    .setMessage("¿ESTAS SEGURO DE QUE DESEAS ELIMINAR ID: "+idEliminar+"?")
                    .setPositiveButton("ELIMINAR") {d,i->
                        eliminar(idEliminar)
                    }
                    .setNeutralButton("NO") {d,i -> }
                    .show()
            }
        }
        idSeleccionadoEnLista = -1
        return true
    }

    private fun eliminar(idEliminar: String) {
        try {
            var trans = baseDatos.writableDatabase
            var resultado = trans.delete("EVENTO", "ID=?", arrayOf(idEliminar))

            if (resultado == 0) {
                mensaje("ERROR! NO SE PUDO ELIMINAR EVENTO")
            } else {
                mensaje("SE ELIMINÓ CON EXITO EVENTO CON ID ${idEliminar}")
            }
            trans.close()
            cargarEventos()

        } catch (e:SQLiteException) {
            mensaje(e.message!!)
        }
    }

    private fun sincronizar(){
        datos2.clear()
        baseRemota.collection("evento").addSnapshotListener { querySnapshot, firebaseFirestoreException ->
            if (firebaseFirestoreException != null) {
                mensaje("Error! No se pudo recuperar data desde FireBase")
                return@addSnapshotListener
            }
            var cadena = ""
            for (registro in querySnapshot!!) {
                cadena = registro.id
                datos2.add(cadena)
            }
            try {
                var trans = baseDatos.readableDatabase
                var respuesta = trans.query("EVENTO", arrayOf("*"), null, null, null, null, null)
                if (respuesta.moveToFirst()) {
                    do{
                        baseRemota.waitForPendingWrites()
                        if (datos2.any{respuesta.getString(0).toString()==it}) {
                            datos2.remove(respuesta.getString(0).toString())
                            baseRemota.collection("agenda")
                                .document(respuesta.getString(0))
                                .update("lugar",respuesta.getString(1),
                                "hora",respuesta.getString(2),
                                "fecha",respuesta.getString(3),"descripcion",respuesta.getString(4)
                                ).addOnSuccessListener {
                                    baseRemota.waitForPendingWrites()
                                }.addOnFailureListener {
                                    mensaje("No se pudo Actualizar!")
                                }
                        } else {
                            var datosInsertar = hashMapOf(
                                "lugar" to respuesta.getString(1),
                                "hora" to respuesta.getString(2),
                                "fecha" to respuesta.getString(3),
                                "descripcion" to respuesta.getString(4)
                            )
                            baseRemota.collection("agenda").document("${respuesta.getString(0)}")
                                .set(datosInsertar as Any).addOnSuccessListener {

                                }
                                .addOnFailureListener {
                                    mensaje("NO SE PUDO INSERTAR:\n${it.message!!}")
                                }
                        }
                    }while (respuesta.moveToNext())

                } else {
                    datos.add("NO TIENES EVENTOS")
                }
                trans.close()
            } catch (e: SQLiteException) {
                    mensaje("ERROR: " + e.message!!)
            }
            var el = datos2.subtract(listaID)
            if (el.isEmpty()) {

            } else {
                el.forEach {
                baseRemota.collection("evento")
                    .document(it)
                    .delete()
                    .addOnSuccessListener {}
                    .addOnFailureListener { mensaje("Error:No se elimino\n" + it.message!!) }
                }
            }

        }
        mensaje("Sincronizado con exito")
    }

}

class DatePickerFragment : DialogFragment() {

    private var listener: DatePickerDialog.OnDateSetListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        return DatePickerDialog(activity!!, listener, year, month, day)
    }

    companion object {
        fun newInstance(listener: DatePickerDialog.OnDateSetListener): DatePickerFragment {
            val fragment = DatePickerFragment()
            fragment.listener = listener
            return fragment
        }
    }

}

class TimePickerFragment : DialogFragment() {

    private var listener: TimePickerDialog.OnTimeSetListener? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val c = Calendar.getInstance()
        val hour = c.get(Calendar.HOUR)
        val minute = c.get(Calendar.MINUTE)

        return TimePickerDialog(activity, listener, hour, minute, true)
    }

    companion object {
        fun newInstance(listener: TimePickerDialog.OnTimeSetListener): TimePickerFragment {
            val fragment = TimePickerFragment()
            fragment.listener = listener
            return fragment
        }
    }

}
