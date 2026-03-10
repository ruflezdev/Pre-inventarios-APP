package com.example.pre_inventariossa

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ==============================================================================
// 🎨 PALETA DE COLORES "MATERIAL PASTEL SUAVE" (MEJORADA)
// ==============================================================================

// Colores Base
val AzulSuave = Color(0xFF3949AB) // Azul principal para el día
val AzulClaroNoche = Color(0xFF5C6BC0) // Azul más luminoso para que se lea en la noche
val AmarilloMostaza = Color(0xFFFFCA28) // Color de acento para botones de acción (Día y Noche)
val RojoAlerta = Color(0xFFEF5350) // Rojo suave para errores y botones de borrar

// Fondos y Superficies (Día)
val BlancoPuro = Color(0xFFFFFFFF)
val GrisFondoLight = Color(0xFFF5F6F8)
val GrisCajasTextoLight = Color(0xFFEBEBEB) // Para resaltar los campos de texto

// Fondos y Superficies (Noche)
val FondoOscuro = Color(0xFF121212)
val GrisOscuroCard = Color(0xFF1E1E1E)
val GrisCajasTextoDark = Color(0xFF2C2C2C) // Para resaltar los campos de texto en la oscuridad

// ==============================================================================
// ☀️ REGLAS DEL MODO CLARO
// ==============================================================================
private val TemaClaro = lightColorScheme(
    primary = AzulSuave,
    onPrimary = BlancoPuro,

    secondary = AmarilloMostaza,
    onSecondary = Color.Black, // Letras negras sobre el botón amarillo

    background = GrisFondoLight,
    onBackground = Color.Black,

    surface = BlancoPuro,
    onSurface = Color.Black,

    surfaceVariant = GrisCajasTextoLight, // Da volumen a los textfields
    onSurfaceVariant = Color.DarkGray,

    error = RojoAlerta,
    onError = BlancoPuro
)

// ==============================================================================
// 🌙 REGLAS DEL MODO OSCURO
// ==============================================================================
private val TemaOscuro = darkColorScheme(
    primary = AzulClaroNoche, // Barra superior azul claro para no lastimar la vista
    onPrimary = BlancoPuro,

    secondary = AmarilloMostaza, // El botón FAB sigue siendo amarillo para resaltar
    onSecondary = Color.Black,

    background = FondoOscuro,
    onBackground = BlancoPuro,

    surface = GrisOscuroCard, // Las tarjetas son gris muy oscuro (no negro puro)
    onSurface = BlancoPuro,

    surfaceVariant = GrisCajasTextoDark, // Campos de texto más visibles
    onSurfaceVariant = Color.LightGray,

    error = RojoAlerta,
    onError = BlancoPuro
)

// ==============================================================================
// 🚀 INYECTOR DEL TEMA (Tu "CSS" principal)
// ==============================================================================
@Composable
fun AppTema(
    isDarkMode: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (isDarkMode) TemaOscuro else TemaClaro,
        typography = Typography(), // Dejamos la tipografía moderna por defecto de Android
        content = content
    )
}