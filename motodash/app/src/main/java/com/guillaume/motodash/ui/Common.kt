package com.guillaume.motodash.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val Bg = Color(0xFF07070F)
val Card = Color(0xFF1A1A24)          // fonds opaques (dialogues, boutons sur la carte)
val CardHi = Color(0x1AFFFFFF)        // tuiles en verre dépoli
val Accent = Color(0xFFFF7A00)
val TextDim = Color(0xB3FFFFFF)
val GoBlue = Color(0xFF1A73E8)
val MicRed = Color(0xFFE53935)
val Lilac = Color(0xFFC9B8FF)

@Composable
fun MotoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent, background = Bg, surface = Card,
            onPrimary = Color.Black, onBackground = Color.White, onSurface = Color.White,
        ),
        content = content,
    )
}

/** Fond d'écran dégradé violet / rouge / bleu, façon CarPlay. */
fun Modifier.wallpaper(): Modifier = this.background(Bg).drawBehind {
    val d = size.maxDimension
    drawRect(Brush.radialGradient(listOf(Color(0xCC5E1A8C), Color.Transparent), Offset(size.width * 0.55f, size.height * 0.45f), d * 0.55f))
    drawRect(Brush.radialGradient(listOf(Color(0xDDB0183F), Color.Transparent), Offset(size.width * 0.9f, size.height * 0.05f), d * 0.5f))
    drawRect(Brush.radialGradient(listOf(Color(0xDD2440C8), Color.Transparent), Offset(size.width * 0.1f, size.height * 1.0f), d * 0.55f))
}

/** Carte en verre dépoli. */
fun Modifier.glass(radius: Dp = 26.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this.clip(shape).background(Color.White.copy(alpha = 0.09f)).border(1.dp, Color.White.copy(alpha = 0.14f), shape)
}

/** Gros bouton rond, utilisable avec des gants. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoundButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    active: Boolean = false,
    background: Color = Card.copy(alpha = 0.9f),
    tint: Color = Color.White,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(if (active) Accent else background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = if (active) Color.Black else tint, modifier = Modifier.size(size * 0.5f))
    }
}
