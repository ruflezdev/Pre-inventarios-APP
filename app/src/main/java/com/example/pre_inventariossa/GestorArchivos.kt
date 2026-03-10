package com.example.pre_inventariossa

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun importarDesdeCSV(context: Context, uri: Uri): List<Producto> {
    val listaNueva = mutableListOf<Producto>()
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readLine() // Saltamos la primera línea (cabecera)
                var linea: String? = reader.readLine()
                while (linea != null) {
                    val datos = linea.split(",")
                    if (datos.size >= 5) {
                        listaNueva.add(
                            Producto(
                                id = datos[0].trim(),
                                nombre = datos[1].trim(),
                                seccion = datos[2].trim(),
                                unidad = datos[3].trim(),
                                stockNecesario = datos[4].trim().toDoubleOrNull() ?: 0.0,
                                enTienda = if (datos.size > 5) datos[5].trim().toDoubleOrNull()
                                    ?: 0.0 else 0.0,
                                pedidoFinal = if (datos.size > 6) datos[6].trim().toDoubleOrNull()
                                    ?: 0.0 else 0.0,
                                verificado = if (datos.size > 7) datos[7].trim()
                                    .toBooleanStrictOrNull() ?: false else false,
                                nota = if (datos.size > 8) datos[8].trim() else ""
                            )
                        )
                    }
                    linea = reader.readLine()
                }
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error al importar el Excel", Toast.LENGTH_SHORT).show()
    }
    return listaNueva
}

fun exportarACsv(context: Context, productos: List<Producto>) {
    if (productos.isEmpty()) {
        Toast.makeText(context, "El inventario está vacío", Toast.LENGTH_SHORT).show()
        return
    }

    val fecha = SimpleDateFormat("dd-MMM_HHmm", Locale.getDefault()).format(Date())
    val nombreArchivo = "Inventario_$fecha.csv"

    // Nombres de las columnas en el Excel
    val cabecera =
        "ID_Codigo,Nombre,Seccion,Unidad,Stock_Necesario,En_Tienda,Pedido_Final,Verificado,Nota\n"

    val contenido = productos.joinToString("\n") { p ->
        // Quitamos las comas de los textos para no romper el Excel
        val nombreLimpio = p.nombre.replace(",", "")
        val notaLimpia = p.nota.replace("\n", " ").replace(",", "")
        "${p.id},${nombreLimpio},${p.seccion},${p.unidad},${p.stockNecesario},${p.enTienda},${p.pedidoFinal},${p.verificado},${notaLimpia}"
    }

    try {
        val cacheFile = File(context.cacheDir, nombreArchivo)
        FileOutputStream(cacheFile).use { it.write((cabecera + contenido).toByteArray()) }

        val uri =
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cacheFile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Guardar Excel en:"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error al crear Excel", Toast.LENGTH_SHORT).show()
    }
}