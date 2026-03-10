package com.example.pre_inventariossa

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "secciones")
data class Seccion(
    @PrimaryKey val nombre: String,
    val colorHex: Long,
    val icono: String = "📁" // Emoji o ID de icono por defecto
)

@Entity(tableName = "productos")
data class Producto(
    @PrimaryKey val id: String,
    val nombre: String,
    val seccion: String,
    val unidad: String,
    val stockNecesario: Double,
    var enTienda: Double = 0.0,
    var pedidoFinal: Double = 0.0,
    var verificado: Boolean = false,
    var nota: String = ""
)

// ¡NUEVA TABLA PARA RESPALDOS!
@Entity(tableName = "historial_pedidos")
data class HistorialPedido(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fecha: String,
    val seccion: String,
    val reporteTexto: String
)