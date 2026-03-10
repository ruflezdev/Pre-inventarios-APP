package com.example.pre_inventariossa

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InventarioViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = InventarioDatabase.getDatabase(application).productoDao()

    val listaSecciones: Flow<List<Seccion>> = dao.obtenerSecciones()
    val historialPedidos: Flow<List<HistorialPedido>> = dao.obtenerHistorialCompleto()

    // Variable conectada al DAO para el Excel
    val todosLosProductos: Flow<List<Producto>> = dao.obtenerTodosLosProductos()

    fun productosDeSeccion(seccion: String): Flow<List<Producto>> =
        dao.obtenerProductosPorSeccion(seccion)

    fun guardarSeccion(seccion: Seccion) = viewModelScope.launch { dao.insertarSeccion(seccion) }

    fun modificarSeccion(
        viejaSeccion: Seccion,
        nuevoNombre: String,
        nuevoColor: Long,
        nuevoIcono: String
    ) =
        viewModelScope.launch {
            dao.insertarSeccion(Seccion(nuevoNombre, nuevoColor, nuevoIcono))
            dao.actualizarNombreSeccionEnProductos(viejaSeccion.nombre, nuevoNombre)
            if (viejaSeccion.nombre != nuevoNombre) dao.eliminarSeccion(viejaSeccion)
        }

    fun eliminarSeccionCompleta(seccion: Seccion) = viewModelScope.launch {
        dao.eliminarProductosPorSeccion(seccion.nombre)
        dao.eliminarSeccion(seccion)
    }

    fun guardarProductos(productos: List<Producto>) =
        viewModelScope.launch { dao.insertarProductos(productos) }

    // --- FUNCIÓN PARA IMPORTAR ---
    fun importarYCrearSecciones(productos: List<Producto>) = viewModelScope.launch {
        if (productos.isEmpty()) return@launch

        // Busca qué secciones vienen en el Excel y las crea automáticamente
        val seccionesUnicas = productos.map { it.seccion }.distinct()
        val colores = listOf(
            0xFF5C6BC0,
            0xFF42A5F5,
            0xFF26C6DA,
            0xFF26A69A,
            0xFF66BB6A,
            0xFF9CCC65,
            0xFFFFCA28,
            0xFFFFA726
        )
        val iconos = listOf("📦", "🍎", "🧼", "🥛", "🍪", "🍖", "🥤", "🧂")

        seccionesUnicas.forEachIndexed { index, nombreSec ->
            val colorAsignado = colores[index % colores.size]
            val iconoAsignado = iconos[index % iconos.size]
            dao.insertarSeccion(Seccion(nombreSec, colorAsignado, iconoAsignado))
        }

        // Inserta todos los productos
        dao.insertarProductos(productos)
    }

    fun actualizarIndividual(producto: Producto) =
        viewModelScope.launch { dao.actualizarProducto(producto) }

    fun eliminar(producto: Producto) = viewModelScope.launch { dao.eliminarProducto(producto) }

    fun finalizarYReiniciar(seccionNombre: String, textoReporte: String) = viewModelScope.launch {
        val fechaActual = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
        dao.guardarEnHistorial(
            HistorialPedido(
                fecha = fechaActual,
                seccion = seccionNombre,
                reporteTexto = textoReporte
            )
        )
        dao.reiniciarInventario(seccionNombre)
    }

    fun limpiarHistorial() = viewModelScope.launch { dao.borrarHistorial() }

    // --- BUSCAR NOMBRE EN INTERNET (API OPEN FOOD FACTS - LIMPIEZA MÉXICO) ---
    suspend fun buscarNombreEnInternet(codigo: String): String? {
        // Ignorar códigos locales (menores a 7 dígitos)
        if (codigo.length < 7) return null

        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://world.openfoodfacts.org/api/v0/product/$codigo.json")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                if (connection.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    val json = JSONObject(response.toString())
                    if (json.optInt("status") == 1) {
                        val product = json.getJSONObject("product")

                        // 1. Obtener nombre base (prioridad español)
                        var nombreFinal = product.optString("product_name_es").ifBlank {
                            product.optString("product_name")
                        }.trim()

                        // 2. Obtener marca y limpiar (quitar "The Coca-Cola Company", etc)
                        var marca = product.optString("brands").split(",")[0].trim()
                        val empresaSufijos = listOf(
                            "company",
                            "corporation",
                            "the",
                            "mexico",
                            "s.a.",
                            "de c.v.",
                            "inc."
                        )

                        // Limpiar marca de nombres de corporativo largos
                        var marcaLimpia = marca
                        empresaSufijos.forEach { sufijo ->
                            if (marcaLimpia.lowercase().contains(sufijo)) {
                                // Si la marca es "The Coca-Cola Company", intentar dejar solo "Coca-Cola"
                                marcaLimpia =
                                    marcaLimpia.replace(Regex("(?i)\\b$sufijo\\b"), "").trim()
                            }
                        }

                        // 3. Obtener cantidad (ej: 1.5 L, 600 ml)
                        val cantidad = product.optString("quantity").trim()

                        // 4. Construir el nombre final sin repetir marca
                        var resultado = ""

                        // Si el nombre ya incluye la marca (ej: "Sprite Refresco..."), no la pegamos al inicio
                        if (marcaLimpia.isNotBlank() && !nombreFinal.contains(
                                marcaLimpia,
                                ignoreCase = true
                            )
                        ) {
                            resultado = "$marcaLimpia "
                        }

                        resultado += nombreFinal

                        if (cantidad.isNotBlank()) {
                            resultado += " $cantidad"
                        }

                        // Limpiar espacios dobles que hayan quedado
                        return@withContext resultado.replace(Regex("\\s+"), " ").trim().uppercase()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            null
        }
    }
}