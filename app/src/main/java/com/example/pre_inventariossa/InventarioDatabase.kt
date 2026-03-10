package com.example.pre_inventariossa

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// AÑADIMOS LA TABLA HistorialPedido::class
@Database(entities = [Producto::class, Seccion::class, HistorialPedido::class], version = 1)
abstract class InventarioDatabase : RoomDatabase() {
    abstract fun productoDao(): ProductoDao

    companion object {
        @Volatile
        private var INSTANCE: InventarioDatabase? = null

        fun getDatabase(context: Context): InventarioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    InventarioDatabase::class.java,
                    "inventario_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}