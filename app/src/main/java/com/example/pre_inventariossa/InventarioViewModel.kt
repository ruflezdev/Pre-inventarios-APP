package com.example.pre_inventariossa

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InventarioViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = InventarioDatabase.getDatabase(application).productoDao()

    val listaSecciones: Flow<List<Seccion>> = dao.obtenerSecciones()
    val historialPedidos: Flow<List<HistorialPedido>> = dao.obtenerHistorialCompleto()

    // Variable conectada al DAO para el Excel
    val todosLosProductos: Flow<List<Producto>> = dao.obtenerTodosLosProductos()

    fun productosDeSeccion(seccion: String): Flow<List<Producto>> = dao.obtenerProductosPorSeccion(seccion)

    fun guardarSeccion(seccion: Seccion) = viewModelScope.launch { dao.insertarSeccion(seccion) }

    fun modificarSeccion(viejaSeccion: Seccion, nuevoNombre: String, nuevoColor: Long) = viewModelScope.launch {
        dao.insertarSeccion(Seccion(nuevoNombre, nuevoColor))
        dao.actualizarNombreSeccionEnProductos(viejaSeccion.nombre, nuevoNombre)
        if (viejaSeccion.nombre != nuevoNombre) dao.eliminarSeccion(viejaSeccion)
    }

    fun eliminarSeccionCompleta(seccion: Seccion) = viewModelScope.launch {
        dao.eliminarProductosPorSeccion(seccion.nombre)
        dao.eliminarSeccion(seccion)
    }

    fun guardarProductos(productos: List<Producto>) = viewModelScope.launch { dao.insertarProductos(productos) }

    // --- NUEVO: FUNCIÓN MÁGICA PARA IMPORTAR ---
    fun importarYCrearSecciones(productos: List<Producto>) = viewModelScope.launch {
        if (productos.isEmpty()) return@launch

        // Busca qué secciones vienen en el Excel y las crea automáticamente
        val seccionesUnicas = productos.map { it.seccion }.distinct()
        val colores = listOf(0xFF5C6BC0, 0xFF42A5F5, 0xFF26C6DA, 0xFF26A69A, 0xFF66BB6A, 0xFF9CCC65, 0xFFFFCA28, 0xFFFFA726)

        seccionesUnicas.forEachIndexed { index, nombreSec ->
            val colorAsignado = colores[index % colores.size]
            dao.insertarSeccion(Seccion(nombreSec, colorAsignado))
        }

        // Inserta todos los productos
        dao.insertarProductos(productos)
    }

    fun actualizarIndividual(producto: Producto) = viewModelScope.launch { dao.actualizarProducto(producto) }
    fun eliminar(producto: Producto) = viewModelScope.launch { dao.eliminarProducto(producto) }

    fun finalizarYReiniciar(seccionNombre: String, textoReporte: String) = viewModelScope.launch {
        val fechaActual = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
        dao.guardarEnHistorial(HistorialPedido(fecha = fechaActual, seccion = seccionNombre, reporteTexto = textoReporte))
        dao.reiniciarInventario(seccionNombre)
    }

    fun limpiarHistorial() = viewModelScope.launch { dao.borrarHistorial() }
}