package com.example.pre_inventariossa

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM secciones")
    fun obtenerSecciones(): Flow<List<Seccion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarSeccion(seccion: Seccion)

    @Delete
    suspend fun eliminarSeccion(seccion: Seccion)

    @Query("UPDATE productos SET seccion = :nuevoNombre WHERE seccion = :viejoNombre")
    suspend fun actualizarNombreSeccionEnProductos(viejoNombre: String, nuevoNombre: String)

    @Query("DELETE FROM productos WHERE seccion = :seccionNombre")
    suspend fun eliminarProductosPorSeccion(seccionNombre: String)

    // --- NUEVO: Extraer todo el catálogo para el Excel ---
    @Query("SELECT * FROM productos ORDER BY seccion ASC, nombre ASC")
    fun obtenerTodosLosProductos(): Flow<List<Producto>>

    @Query("SELECT * FROM productos WHERE seccion = :seccionNombre ORDER BY nombre ASC")
    fun obtenerProductosPorSeccion(seccionNombre: String): Flow<List<Producto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarProductos(productos: List<Producto>)

    @Update
    suspend fun actualizarProducto(producto: Producto)

    @Delete
    suspend fun eliminarProducto(producto: Producto)

    @Query("UPDATE productos SET enTienda = 0.0, pedidoFinal = 0.0, verificado = 0, nota = '' WHERE seccion = :seccionNombre")
    suspend fun reiniciarInventario(seccionNombre: String)

    @Insert
    suspend fun guardarEnHistorial(historial: HistorialPedido)

    @Query("SELECT * FROM historial_pedidos ORDER BY id DESC")
    fun obtenerHistorialCompleto(): Flow<List<HistorialPedido>>

    @Query("DELETE FROM historial_pedidos")
    suspend fun borrarHistorial()
}