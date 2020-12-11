package mx.tecnm.tepic.ladm_u3_ejercicio5

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BaseDatos(
    context: Context?,
    name: String?,
    factory: SQLiteDatabase.CursorFactory?,
    version: Int
) : SQLiteOpenHelper(context, name, factory, version) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE EVENTO (ID INTEGER NOT NULL PRIMARY KEY, LUGAR VARCHAR(200), HORA TIME, FECHA DATE, DESCRIPCION VARCHAR(300))")
    }

    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {

    }
}