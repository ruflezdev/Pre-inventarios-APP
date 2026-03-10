package com.example.pre_inventariossa

import android.Manifest
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) Toast.makeText(this, "Se requiere la cámara", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }

            // LLAMAMOS AL TEMA QUE CREASTE EN Theme.kt
            AppTema(isDarkMode = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppNavegacion(isDarkMode) { isDarkMode = !isDarkMode }
                }
            }
        }
    }
}

enum class RutaPantalla { SECCIONES, INVENTARIO, AJUSTES, HISTORIAL }

@Composable
fun AppNavegacion(isDarkMode: Boolean, vm: InventarioViewModel = viewModel(), onToggleDarkMode: () -> Unit) {
    var pantallaActual by remember { mutableStateOf(RutaPantalla.SECCIONES) }
    var seccionActual by remember { mutableStateOf<Seccion?>(null) }

    when (pantallaActual) {
        RutaPantalla.SECCIONES -> PantallaSecciones(vm, onIrAjustes = { pantallaActual = RutaPantalla.AJUSTES }, onIrHistorial = { pantallaActual = RutaPantalla.HISTORIAL }, onSeccionClick = { seleccion -> seccionActual = seleccion; pantallaActual = RutaPantalla.INVENTARIO })
        RutaPantalla.INVENTARIO -> PantallaInventario(vm, seccionActual!!, onBack = { seccionActual = null; pantallaActual = RutaPantalla.SECCIONES })
        RutaPantalla.AJUSTES -> PantallaAjustes(vm, isDarkMode, onToggleDarkMode, onBack = { pantallaActual = RutaPantalla.SECCIONES })
        RutaPantalla.HISTORIAL -> PantallaHistorial(vm, onBack = { pantallaActual = RutaPantalla.SECCIONES })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaSecciones(vm: InventarioViewModel, onIrAjustes: () -> Unit, onIrHistorial: () -> Unit, onSeccionClick: (Seccion) -> Unit) {
    val secciones by vm.listaSecciones.collectAsState(initial = emptyList())
    var mostrarDialogo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Secciones", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, actionIconContentColor = MaterialTheme.colorScheme.onPrimary),
                actions = { IconButton(onClick = onIrHistorial) { Icon(Icons.Default.History, "Historial") }; IconButton(onClick = onIrAjustes) { Icon(Icons.Default.Settings, "Ajustes") } }
            )
        },
        floatingActionButton = { FloatingActionButton(onClick = { mostrarDialogo = true }, containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary, shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Add, "Nueva") } }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 16.dp)) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            items(secciones, key = { it.nombre }) { seccion ->
                Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onSeccionClick(seccion) }, colors = CardDefaults.cardColors(containerColor = Color(seccion.colorHex))) {
                    Text(seccion.nombre.uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, modifier = Modifier.padding(32.dp))
                }
            }
        }
        if (mostrarDialogo) DialogoSeccion(null, { mostrarDialogo = false }, { vm.guardarSeccion(it); mostrarDialogo = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaAjustes(vm: InventarioViewModel, isDarkMode: Boolean, onToggleDarkMode: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val secciones by vm.listaSecciones.collectAsState(initial = emptyList())
    val todosLosProductos by vm.todosLosProductos.collectAsState(initial = emptyList())

    var seccionAEditar by remember { mutableStateOf<Seccion?>(null) }
    var seccionABorrar by remember { mutableStateOf<Seccion?>(null) }

    val launcherImportar = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val productosImportados = importarDesdeCSV(context, it)
            if (productosImportados.isNotEmpty()) {
                vm.importarYCrearSecciones(productosImportados)
                Toast.makeText(context, "¡Catálogo importado con éxito!", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Ajustes", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary)) }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Modo Nocturno", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Switch(checked = isDarkMode, onCheckedChange = { onToggleDarkMode() }, colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.secondary, checkedTrackColor = MaterialTheme.colorScheme.primary))
                }
            }

            Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Base de Datos (Excel / CSV)", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { exportarACsv(context, todosLosProductos) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Upload, null); Spacer(modifier = Modifier.width(8.dp)); Text("Exportar Catálogo a Excel")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = { launcherImportar.launch("text/*") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Download, null); Spacer(modifier = Modifier.width(8.dp)); Text("Importar desde Excel")
                    }
                }
            }

            Text("Administrar Secciones", fontSize = 16.sp, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            LazyColumn {
                items(secciones, key = { it.nombre }) { seccion ->
                    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(seccion.colorHex)))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(seccion.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Row {
                                IconButton(onClick = { seccionAEditar = seccion }) { Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary) }
                                IconButton(onClick = { seccionABorrar = seccion }) { Icon(Icons.Default.Delete, "Borrar", tint = Color.Red) }
                            }
                        }
                    }
                }
            }
        }

        if (seccionAEditar != null) DialogoSeccion(seccionAEditar, { seccionAEditar = null }, { nueva -> vm.modificarSeccion(seccionAEditar!!, nueva.nombre, nueva.colorHex); seccionAEditar = null })
        if (seccionABorrar != null) {
            AlertDialog(
                shape = RoundedCornerShape(20.dp),
                onDismissRequest = { seccionABorrar = null }, title = { Text("⚠️ BORRAR SECCIÓN", color = Color.Red, fontWeight = FontWeight.Bold) },
                text = { Text("¿Seguro de borrar '${seccionABorrar!!.nombre}'? \n\n¡SE BORRARÁN SUS PRODUCTOS!") },
                confirmButton = { Button(onClick = { vm.eliminarSeccionCompleta(seccionABorrar!!); seccionABorrar = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("BORRAR TODO", color = Color.White) } },
                dismissButton = { TextButton(onClick = { seccionABorrar = null }) { Text("Cancelar") } }
            )
        }
    }
}

@Composable
fun DialogoSeccion(seccionEdicion: Seccion?, onDismiss: () -> Unit, onConfirm: (Seccion) -> Unit) {
    var nombre by remember { mutableStateOf(seccionEdicion?.nombre ?: "") }
    val colores = listOf(0xFFEF5350, 0xFFEC407A, 0xFFAB47BC, 0xFF7E57C2, 0xFF5C6BC0, 0xFF42A5F5, 0xFF26C6DA, 0xFF26A69A, 0xFF66BB6A, 0xFF9CCC65, 0xFFFFCA28, 0xFFFFA726, 0xFFFF7043, 0xFF8D6E63, 0xFF78909C)
    var colorSeleccionado by remember { mutableStateOf(seccionEdicion?.colorHex ?: colores[0]) }

    AlertDialog(shape = RoundedCornerShape(20.dp), onDismissRequest = onDismiss, title = { Text(if (seccionEdicion == null) "Crear Sección" else "Editar", fontWeight = FontWeight.Bold) }, text = {
        Column {
            OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(modifier = Modifier.height(20.dp))
            Text("Desliza para ver más colores:", fontSize = 12.sp, color = Color.Gray)
            LazyRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(colores) { color ->
                    val isSelected = colorSeleccionado == color
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(color)).clickable { colorSeleccionado = color }.border(width = if (isSelected) 3.dp else 0.dp, color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, shape = CircleShape))
                }
            }
        }
    },
        confirmButton = { Button(onClick = { if (nombre.isNotBlank()) onConfirm(Seccion(nombre, colorSeleccionado)) }) { Text("Guardar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaHistorial(vm: InventarioViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val historiales by vm.historialPedidos.collectAsState(initial = emptyList())
    Scaffold(topBar = { TopAppBar(title = { Text("Respaldo", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") } }, actions = { IconButton(onClick = { vm.limpiarHistorial() }) { Icon(Icons.Default.DeleteSweep, "Limpiar") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary, actionIconContentColor = MaterialTheme.colorScheme.onPrimary)) }) { padding ->
        if (historiales.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay pedidos guardados", color = Color.Gray, fontSize = 18.sp) }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                items(historiales) { historial ->
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), elevation = CardDefaults.cardElevation(4.dp)) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(historial.seccion.uppercase(), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp); Text(historial.fecha, fontSize = 12.sp, color = Color.Gray) }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Text(historial.reporteTexto, fontSize = 15.sp, lineHeight = 22.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { enviarWhatsAppDirecto(context, historial.reporteTexto) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Reenviar", fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }
    }
}

// ==============================================================================
// 4. PANTALLA INVENTARIO (CON ALERTA DE BORRADO Y LÁPIZ)
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInventario(vm: InventarioViewModel, seccion: Seccion, onBack: () -> Unit) {
    val context = LocalContext.current
    val productos by vm.productosDeSeccion(seccion.nombre).collectAsState(initial = emptyList())

    var busqueda by remember { mutableStateOf("") }
    var mostrarCamaraBusqueda by remember { mutableStateOf(false) }
    var alertaCodigoNoEncontrado by remember { mutableStateOf("") }

    var idParaNuevoProducto by remember { mutableStateOf("") }
    var mostrarDialogoFormulario by remember { mutableStateOf(false) }
    var productoAConfigurarInfo by remember { mutableStateOf<Producto?>(null) }
    var productoAEditarStock by remember { mutableStateOf<Producto?>(null) }

    var alertaFinalizar by remember { mutableStateOf(false) }
    var productoABorrar by remember { mutableStateOf<Producto?>(null) }

    Scaffold(
        topBar = { TopAppBar(title = { Text(seccion.nombre.uppercase(), fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Atrás") } }, actions = { IconButton(onClick = { idParaNuevoProducto = ""; productoAConfigurarInfo = null; mostrarDialogoFormulario = true }) { Icon(Icons.Default.Add, "Nuevo") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = MaterialTheme.colorScheme.onPrimary, navigationIconContentColor = MaterialTheme.colorScheme.onPrimary, actionIconContentColor = MaterialTheme.colorScheme.onPrimary)) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = { alertaFinalizar = true }, icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.onSecondary) }, text = { Text("Finalizar y Enviar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondary) }, containerColor = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(16.dp), elevation = FloatingActionButtonDefaults.elevation(8.dp)) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(value = busqueda, onValueChange = { busqueda = it }, label = { Text("Buscar o Escanear") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), trailingIcon = { IconButton(onClick = { mostrarCamaraBusqueda = true }) { Icon(Icons.Default.PhotoCamera, null, tint = MaterialTheme.colorScheme.primary) } }, colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, focusedLabelColor = MaterialTheme.colorScheme.primary))
                LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
                    val filtrados = productos.filter { it.nombre.contains(busqueda, true) || it.id == busqueda }.sortedBy { it.verificado }

                    items(filtrados, key = { it.id }) { producto ->
                        CardProducto(
                            producto = producto,
                            colorSeccion = Color(seccion.colorHex),
                            onCardClick = { productoAEditarStock = producto },
                            onEditInfoClick = { productoAConfigurarInfo = producto; mostrarDialogoFormulario = true },
                            onDelete = { prod -> productoABorrar = prod }
                        )
                    }
                }
            }

            if (mostrarCamaraBusqueda) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    CamaraPreview(onCodigoDetectado = { codigo -> reproducirBeep(); val encontrado = productos.find { it.id == codigo }; if (encontrado != null) productoAEditarStock = encontrado else alertaCodigoNoEncontrado = codigo; mostrarCamaraBusqueda = false })
                    Button(onClick = { mostrarCamaraBusqueda = false }, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)) { Text("Cerrar Cámara") }
                }
            }

            if (alertaCodigoNoEncontrado.isNotEmpty()) { AlertDialog(shape = RoundedCornerShape(20.dp), onDismissRequest = { alertaCodigoNoEncontrado = "" }, title = { Text("No encontrado", fontWeight = FontWeight.Bold) }, text = { Text("El código no existe. ¿Agregar al catálogo?") }, confirmButton = { Button(onClick = { idParaNuevoProducto = alertaCodigoNoEncontrado; alertaCodigoNoEncontrado = ""; productoAConfigurarInfo = null; mostrarDialogoFormulario = true }) { Text("Sí, agregar") } }, dismissButton = { TextButton(onClick = { alertaCodigoNoEncontrado = "" }) { Text("No") } }) }

            if (productoABorrar != null) {
                AlertDialog(shape = RoundedCornerShape(20.dp), onDismissRequest = { productoABorrar = null }, title = { Text("⚠️ Confirmar", fontWeight = FontWeight.Bold, color = Color.Red) }, text = { Text("¿Estás seguro de eliminar '${productoABorrar!!.nombre}'?") }, confirmButton = { Button(onClick = { vm.eliminar(productoABorrar!!); productoABorrar = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Eliminar", color = Color.White) } }, dismissButton = { TextButton(onClick = { productoABorrar = null }) { Text("Cancelar") } })
            }

            if (mostrarDialogoFormulario) {
                DialogoFormularioProducto(
                    productoEdicion = productoAConfigurarInfo,
                    idInicial = idParaNuevoProducto,
                    seccionFija = seccion.nombre,
                    productosExistentes = productos,
                    onDismiss = { mostrarDialogoFormulario = false; idParaNuevoProducto = ""; productoAConfigurarInfo = null },
                    onConfirm = { nuevoProd ->
                        if (productoAConfigurarInfo != null && productoAConfigurarInfo!!.id != nuevoProd.id) { vm.eliminar(productoAConfigurarInfo!!) }
                        vm.guardarProductos(listOf(nuevoProd))
                        mostrarDialogoFormulario = false
                        idParaNuevoProducto = ""
                        if (productoAConfigurarInfo == null) { productoAEditarStock = nuevoProd }
                        productoAConfigurarInfo = null
                    }
                )
            }

            if (productoAEditarStock != null) { DialogoEditarStock(producto = productoAEditarStock!!, onDismiss = { productoAEditarStock = null }, onSave = { productoModificado -> vm.actualizarIndividual(productoModificado); productoAEditarStock = null; busqueda = "" }) }

            if (alertaFinalizar) {
                val todosVerificados = productos.all { it.verificado }; val faltantes = productos.count { !it.verificado }
                AlertDialog(
                    shape = RoundedCornerShape(20.dp), onDismissRequest = { alertaFinalizar = false }, title = { Text(if (todosVerificados) "¡Inventario Completo!" else "⚠️ Faltan productos", fontWeight = FontWeight.Bold) },
                    text = { Text(if (todosVerificados) "Se guardará un respaldo de este pedido y los contadores se reiniciarán a 0." else "Aún te faltan $faltantes productos por verificar.\n\nSe guardará un respaldo solo de los productos contados y se reiniciarán todos los contadores a 0.") },
                    confirmButton = {
                        Button(onClick = {
                            val productosAPedir = productos.filter { it.pedidoFinal > 0.0 }
                            if (productosAPedir.isNotEmpty()) { val textoReporte = generarTextoPedido(productosAPedir, seccion.nombre); vm.finalizarYReiniciar(seccion.nombre, textoReporte); enviarWhatsAppDirecto(context, textoReporte) }
                            else { Toast.makeText(context, "No hay pedido. Reiniciado.", Toast.LENGTH_LONG).show(); vm.finalizarYReiniciar(seccion.nombre, "Sin pedido generado.") }
                            alertaFinalizar = false
                        }) { Text("Confirmar Cierre") }
                    }, dismissButton = { TextButton(onClick = { alertaFinalizar = false }) { Text("Revisar") } }
                )
            }
        }
    }
}

// ==============================================================================
// COMPONENTES DE ESTÉTICA MODERNA Y MATEMÁTICAS
// ==============================================================================

fun Double.fmt(): String = if (this % 1.0 == 0.0) this.toInt().toString() else this.toString()
fun parseCantidad(input: String): Double { val str = input.trim(); if (str.isEmpty()) return 0.0; return try { if (str.contains(" ")) { val parts = str.split(" "); val entero = parts[0].toDoubleOrNull() ?: 0.0; val fraccion = parts[1]; if (fraccion.contains("/")) { val fParts = fraccion.split("/"); val num = fParts[0].toDoubleOrNull() ?: 0.0; val den = fParts[1].toDoubleOrNull() ?: 1.0; entero + (if (den != 0.0) num / den else 0.0) } else { entero + (fraccion.toDoubleOrNull() ?: 0.0) } } else if (str.contains("/")) { val parts = str.split("/"); val num = parts[0].toDoubleOrNull() ?: 0.0; val den = parts[1].toDoubleOrNull() ?: 1.0; if (den != 0.0) num / den else 0.0 } else { str.toDoubleOrNull() ?: 0.0 } } catch (e: Exception) { 0.0 } }

@Composable
fun CardProducto(producto: Producto, colorSeccion: Color, onCardClick: () -> Unit, onEditInfoClick: () -> Unit, onDelete: (Producto) -> Unit) {
    val alphaColor = if (producto.verificado) 0.4f else 1f
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth().clickable { onCardClick() }, elevation = CardDefaults.cardElevation(if (producto.verificado) 1.dp else 6.dp)) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = alphaColor))) {
            Box(modifier = Modifier.fillMaxWidth().background(colorSeccion.copy(alpha = alphaColor)).padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {

                    // NOMBRE GRANDE, UNIDAD PEQUEÑA
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text("${producto.nombre.uppercase()} ", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Text(producto.unidad, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    }

                    // LÁPIZ Y BASURERO
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onEditInfoClick() }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Edit, "Editar Info", tint = Color.White) }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { onDelete(producto) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, "Borrar", tint = Color.White) }
                    }
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                // UNIDAD CHICA EN TEXTO GRIS
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("Necesario: ", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(producto.stockNecesario.fmt(), color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(" ${producto.unidad}", color = Color.Gray.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(bottom = 1.dp))
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    if (producto.enTienda < 0) {
                        Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) { Text("NO SE CONTÓ", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color(0xFFE65100), fontWeight = FontWeight.Black, fontSize = 12.sp) }
                    } else {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("En Tienda: ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(producto.enTienda.fmt(), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text(" ${producto.unidad}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Pedido: ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        Text(producto.pedidoFinal.fmt(), fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                        Text(" ${producto.unidad}", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), fontSize = 11.sp, modifier = Modifier.padding(bottom = 3.dp))
                    }
                }
                if (producto.nota.isNotEmpty()) { Card(modifier = Modifier.padding(top = 8.dp).fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Text("Nota: ${producto.nota}", modifier = Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) } }
                if (producto.verificado) { Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CheckCircle, contentDescription = "Listo", tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Verificado", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
            }
        }
    }
}

@Composable
fun DialogoFormularioProducto(productoEdicion: Producto?, idInicial: String, seccionFija: String, productosExistentes: List<Producto>, onDismiss: () -> Unit, onConfirm: (Producto) -> Unit) {
    val context = LocalContext.current
    var id by remember { mutableStateOf(productoEdicion?.id ?: idInicial) }
    var nombre by remember { mutableStateOf(productoEdicion?.nombre ?: "") }
    var necesarioStr by remember { mutableStateOf(productoEdicion?.stockNecesario?.fmt() ?: "") }
    var unidad by remember { mutableStateOf(productoEdicion?.unidad ?: "Pieza") }
    var menuUnidadExpandido by remember { mutableStateOf(false) }
    val isPieza = unidad.lowercase() == "pieza"; val isCaja = unidad.lowercase() == "caja"

    AlertDialog(shape = RoundedCornerShape(24.dp), onDismissRequest = onDismiss, title = { Text(if (productoEdicion == null) "Agregar a $seccionFija" else "Editar Producto", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(value = id, onValueChange = { id = it }, label = { Text("Código") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre") }, singleLine = true, shape = RoundedCornerShape(12.dp))
                Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    OutlinedButton(onClick = { menuUnidadExpandido = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Unidad: $unidad") }
                    DropdownMenu(expanded = menuUnidadExpandido, onDismissRequest = { menuUnidadExpandido = false }) { listOf("Pieza", "Caja", "Kg/g").forEach { opcion -> DropdownMenuItem(text = { Text(opcion) }, onClick = { unidad = opcion; menuUnidadExpandido = false; necesarioStr = "" }) } }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = necesarioStr, onValueChange = { nuevoInput -> necesarioStr = if (isPieza) nuevoInput.filter { it.isDigit() } else nuevoInput }, label = { Text("Stock Necesario") }, singleLine = true, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = if (isPieza) KeyboardType.Number else if (isCaja) KeyboardType.Text else KeyboardType.Decimal))
            }
        },
        confirmButton = {
            Button(onClick = {
                if(id.isNotBlank()) {
                    // FILTRO DE DUPLICADOS
                    val idExiste = productosExistentes.any { it.id == id && it.id != productoEdicion?.id }
                    if (idExiste) {
                        Toast.makeText(context, "Error: Este código ya está en uso", Toast.LENGTH_LONG).show()
                    } else {
                        val necDouble = parseCantidad(necesarioStr); onConfirm(Producto(id = id, nombre = nombre, seccion = seccionFija, unidad = unidad, stockNecesario = necDouble, enTienda = productoEdicion?.enTienda ?: 0.0, pedidoFinal = productoEdicion?.pedidoFinal ?: 0.0, verificado = productoEdicion?.verificado ?: false, nota = productoEdicion?.nota ?: ""))
                    }
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun DialogoEditarStock(producto: Producto, onDismiss: () -> Unit, onSave: (Producto) -> Unit) {
    var omitirConteo by remember { mutableStateOf(producto.enTienda < 0) }; var nuevoIngresoStr by remember { mutableStateOf("") }
    val isPieza = producto.unidad.lowercase() == "pieza"; val isCaja = producto.unidad.lowercase() == "caja"; val kbType = if (isPieza) KeyboardType.Number else if (isCaja) KeyboardType.Text else KeyboardType.Decimal; val focusRequester = remember { FocusRequester() }; LaunchedEffect(Unit) { delay(100); focusRequester.requestFocus() }
    val enTiendaBase = if (producto.enTienda < 0) 0.0 else producto.enTienda; val nuevoIngreso = parseCantidad(nuevoIngresoStr); val enTiendaTotal = if (omitirConteo) -1.0 else (enTiendaBase + nuevoIngreso)
    val sugerido = if (omitirConteo) 0.0 else (producto.stockNecesario - enTiendaTotal); var pedidoStr by remember { mutableStateOf(if (producto.pedidoFinal > 0) producto.pedidoFinal.fmt() else if (sugerido > 0) sugerido.fmt() else "0") }; var nota by remember { mutableStateOf(producto.nota) }

    LaunchedEffect(nuevoIngresoStr, omitirConteo) { if (!omitirConteo) { val st = producto.stockNecesario - (enTiendaBase + parseCantidad(nuevoIngresoStr)); pedidoStr = if (st > 0) st.fmt() else "0" } }
    val pedidoVal = parseCantidad(pedidoStr); val proyeccion = if (omitirConteo) -1.0 else (enTiendaTotal + pedidoVal); val diferencia = if (omitirConteo) 0.0 else (proyeccion - producto.stockNecesario); val difAbsoluta = if (diferencia < 0) diferencia * -1 else diferencia
    val colorProyeccion = when { omitirConteo -> Color.Gray; diferencia < 0 -> Color.Red; diferencia > 0 -> Color(0xFF1976D2); else -> Color(0xFF2E7D32) }
    val textoEstado = when { omitirConteo -> "SIN PROYECCIÓN"; diferencia < 0 -> "FALTAN ${difAbsoluta.fmt()}"; diferencia > 0 -> "EXCEDENTE DE ${difAbsoluta.fmt()}"; else -> "STOCK COMPLETO" }

    AlertDialog(shape = RoundedCornerShape(24.dp), onDismissRequest = onDismiss, title = { Text(producto.nombre.uppercase(), fontWeight = FontWeight.Black) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.Bottom) { Text("Ideal: ", color = Color.Gray, fontWeight = FontWeight.Bold); Text(producto.stockNecesario.fmt(), color = Color.Gray, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(" ${producto.unidad}", color = Color.Gray.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 1.dp)) }
                if (producto.enTienda > 0) { Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) { Text("Ya contaste antes: ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold); Text(producto.enTienda.fmt(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp); Text(" ${producto.unidad}", color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 1.dp)) } }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { omitirConteo = !omitirConteo }.padding(bottom = 12.dp)) { Checkbox(checked = omitirConteo, onCheckedChange = { omitirConteo = it }, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)); Text("Omitir conteo (Ingreso manual)", fontWeight = FontWeight.Bold) }
                OutlinedTextField(value = if (omitirConteo) "" else nuevoIngresoStr, onValueChange = { nuevoIngresoStr = if (isPieza) it.filter { char -> char.isDigit() } else it }, label = { Text(if (producto.unidad == "Kg/g") "Sumar (ej: 1.2 para 1 Kg 200g)" else "Sumar cantidad encontrada") }, singleLine = true, enabled = !omitirConteo, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = kbType), modifier = Modifier.fillMaxWidth().focusRequester(focusRequester))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = pedidoStr, onValueChange = { pedidoStr = if (isPieza) it.filter { char -> char.isDigit() } else it }, label = { Text("Pedido (Modificable)") }, singleLine = true, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(keyboardType = kbType), modifier = Modifier.fillMaxWidth())
                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = colorProyeccion.copy(alpha = 0.1f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(if (omitirConteo) "Total en tienda: No se contó" else "Total quedará en: ${enTiendaTotal.fmt()}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) { Row(verticalAlignment = Alignment.Bottom) { Text(if (omitirConteo) "?" else proyeccion.fmt(), fontSize = 22.sp, fontWeight = FontWeight.Black, color = colorProyeccion); Text(" ${producto.unidad}", fontSize = 12.sp, color = colorProyeccion.copy(alpha=0.7f), modifier = Modifier.padding(bottom = 3.dp)) }; Text(textoEstado, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = colorProyeccion) }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = nota, onValueChange = { nota = it }, label = { Text("Notas / Observaciones") }, singleLine = false, minLines = 2, maxLines = 3, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onSave(producto.copy(enTienda = enTiendaTotal, pedidoFinal = pedidoVal, nota = nota, verificado = true)) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Confirmar") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

fun reproducirBeep() { try { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100).startTone(ToneGenerator.TONE_PROP_BEEP, 200) } catch (e: Exception) { e.printStackTrace() } }
fun generarTextoPedido(productosConPedido: List<Producto>, seccionNombre: String): String { val builder = StringBuilder(); builder.append("📋 *PEDIDO: ${seccionNombre.uppercase()}*\n\n"); productosConPedido.forEach { p -> builder.append("- ${p.pedidoFinal.fmt()} ${p.unidad} de ${p.nombre}\n"); if (p.nota.isNotEmpty()) builder.append("      - Nota: \"${p.nota}\"\n") }; return builder.toString() }
fun enviarWhatsAppDirecto(context: android.content.Context, textoPedido: String) { if (textoPedido.isEmpty() || textoPedido == "Sin pedido generado.") { Toast.makeText(context, "Nada que enviar", Toast.LENGTH_SHORT).show(); return }; try { context.startActivity(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, textoPedido); `package` = "com.whatsapp" }) } catch (e: Exception) { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, textoPedido) }, "Compartir con:")) } }