package mx.tecnm.tepic.ladm_u3_ejercicio5

import android.content.ContentValues
import android.database.sqlite.SQLiteException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main2.*

class MainActivity2 : AppCompatActivity() {
    var baseDatos = BaseDatos(this, "AGENDA", null, 1)
    var id = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        var extra = intent.extras
        id = extra?.getString("idactualizar")!!

        textView.setText(textView.text.toString() + "${id}")
        try {
            var base = baseDatos.readableDatabase

            var respuesta = base.query("EVENTO", arrayOf("LUGAR","HORA","FECHA","DESCRIPCION"),"ID=?", arrayOf(id),null,null,null)

            if (respuesta.moveToFirst()) {
                editTextAct.setText(respuesta.getString(0))
                editTextActTime.setText(respuesta.getString(1))
                editTextActDate.setText(respuesta.getString(2))
                editTextAct2.setText(respuesta.getString(3))
            } else {
                mensaje("ERROR: NO SE ENCONTRO ID!!!")
            }
            base.close()

        } catch (e: SQLiteException) {
            mensaje(e.message!!)
        }

        button3.setOnClickListener {
            actualizar(id)
        }

        button4.setOnClickListener {
            finish()
        }

        editTextActTime.setOnClickListener {
            showTimePickerDialog()
        }

        editTextActDate.setOnClickListener {
            showDatePickerDialog()
        }

    }

    private fun actualizar(id: String) {
        try {
            var trans = baseDatos.writableDatabase
            var valores = ContentValues()

            valores.put("LUGAR",editTextAct.text.toString())
            valores.put("HORA",editTextActTime.text.toString())
            valores.put("FECHA",editTextActDate.text.toString())
            valores.put("DESCRIPCION",editTextAct2.text.toString())

            var res = trans.update("EVENTO", valores, "ID=?", arrayOf(id))
            if (res > 0) {
                mensaje("Se actualizó correctamente evento con ID: ${id}")
                finish()
            } else {
                mensaje("No se pudo actualizar ID")
            }
            trans.close()

        } catch (e:SQLiteException) {
            mensaje(e.message!!)
        }
    }

    private fun mensaje(s: String) {
        AlertDialog.Builder(this)
            .setTitle("ATENCION")
            .setMessage(s)
            .setPositiveButton("Ok") {d, i -> d.dismiss() }
    }

    private fun showTimePickerDialog() {
        val newFragment = TimePickerFragment.newInstance{ _, hour, minute ->
            var selectedTime = ""
            if (minute < 10) {
                selectedTime = "$hour:0$minute"
            } else {
                selectedTime = "$hour:$minute"
            }
            editTextActTime.setText(selectedTime)
        }

        newFragment.show(supportFragmentManager, "timePicker")
    }

    private fun showDatePickerDialog() {
        val newFragment = DatePickerFragment.newInstance { _, year, month, day ->
            // +1 because January is zero
            val selectedDate = day.toString() + "/" + (month + 1) + "/" + year.toString()
            editTextActDate.setText(selectedDate)
        }

        newFragment.show(supportFragmentManager, "datePicker")
    }
}