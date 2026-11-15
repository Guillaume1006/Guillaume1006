package com.guillaume.motodash.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val Bg = Color(0xFF0B0B0D)
val Card = Color(0xFF18181D)
val CardHi = Color(0xFF26262D)
val Accent = Color(0xFFFF7A00)
val TextDim = Color(0xFF9A9AA5)

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

/** Gros bouton rond, utilisable avec des gants. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoundButton(
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 60.dp,
    active: Boolean = false,
    background: Color = Card.copy(alpha = 0.92f),
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
        Icon(icon, contentDescription, tint = if (active) Color.Black else Color.White, modifier = Modifier.size(size * 0.5f))
    }
}

fun Modifier.card(): Modifier = this.clip(RoundedCornerShape(20.dp)).background(Card)
