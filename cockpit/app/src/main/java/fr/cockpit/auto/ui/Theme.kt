package fr.cockpit.auto.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Couleurs de l'interface voiture : un seul thème sombre, contraste fort. */
object K {
    val bg = Color(0xFF0A0F14)
    val rail = Color(0xFF05080B)
    val panel = Color(0xFF131B24)
    val panel2 = Color(0xFF1B2632)
    val line = Color(0xFF24313E)
    val text = Color(0xFFECF1F5)
    val dim = Color(0xFF8696A6)
    val accent = Color(0xFF3DD6C4)
    val accentInk = Color(0xFF032A26)
    val warn = Color(0xFFF5A524)
}

/** Dégradés des tuiles d'apps. */
object Tile {
    private fun g(a: Long, b: Long) = Brush.linearGradient(listOf(Color(a), Color(b)))
    val nav = g(0xFF34C9B6, 0xFF127A86)
    val music = g(0xFFFF7A59, 0xFFD9345B)
    val phone = g(0xFF5FD37A, 0xFF1F8F49)
    val msg = g(0xFF57A6FF, 0xFF2F5BD8)
    val claude = g(0xFFF2B48E, 0xFFC2603C)
    val voice = g(0xFF7FF3E4, 0xFF0A5F57)
    val car = g(0xFFFFC24D, 0xFFE07B12)
    val settings = g(0xFF8A9AAB, 0xFF4A5866)
    val cal = g(0xFFFF6B6B, 0xFFC23A3A)
    val weather = g(0xFF6FC3FF, 0xFF2A7FC8)
    val art = Brush.linearGradient(listOf(Color(0xFFF26B3A), Color(0xFFF7C548), Color(0xFF2EC4B6), Color(0xFF3A4BD9)))
}

@Composable
fun CockpitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = K.accent,
            onPrimary = K.accentInk,
            background = K.bg,
            onBackground = K.text,
            surface = K.panel,
            onSurface = K.text,
        ),
        content = content,
    )
}
