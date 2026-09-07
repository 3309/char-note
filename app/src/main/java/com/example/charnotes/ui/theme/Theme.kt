package com.example.charnotes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// A small "notebook" palette: deep ink green for structure, warm parchment for the
// background, and a single muted gold accent — deliberately not the default Material
// purple, and not the cream+terracotta combo that's become an AI-generated cliché.
object CharNotesColors {
    val Ink = Color(0xFF1F3D2E)          // deep forest green — top bar, FAB, spine accents
    val InkLight = Color(0xFF335A45)     // lighter ink for pressed/secondary states
    val Paper = Color(0xFFF6F3EA)        // warm parchment background
    val Surface = Color(0xFFFDFBF5)      // slightly warm white for cards
    val Gold = Color(0xFFC9972A)         // the one accent color — attachments, highlights
    val TextInk = Color(0xFF241F18)      // near-black warm charcoal for body text
    val TextMuted = Color(0xFF6B6459)    // warm grey-brown for timestamps/labels
    val Hairline = Color(0xFFE3DDCB)     // subtle border color instead of drop shadows
}

private val LightColors = lightColorScheme(
    primary = CharNotesColors.Ink,
    onPrimary = CharNotesColors.Paper,
    secondary = CharNotesColors.Gold,
    onSecondary = CharNotesColors.TextInk,
    background = CharNotesColors.Paper,
    onBackground = CharNotesColors.TextInk,
    surface = CharNotesColors.Surface,
    onSurface = CharNotesColors.TextInk,
    surfaceVariant = CharNotesColors.Surface,
    onSurfaceVariant = CharNotesColors.TextMuted,
    outline = CharNotesColors.Hairline,
    error = Color(0xFFB3261E)
)

// Serif carries the "notebook / journal" personality for anything title-like; a plain
// sans face keeps note content and UI chrome easy to read at length. Both are generic
// system font families, so no bundled font files are needed.
private val Serif = FontFamily.Serif
private val Sans = FontFamily.SansSerif

val CharNotesTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 0.2.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.1.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    )
)

// A note card's shape: rounded like a folded page corner rather than the uniform
// SaaS-card radius — larger radius on the top-left/bottom-right, tighter on the others.
val NoteCardShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 6.dp,
    bottomStart = 6.dp,
    bottomEnd = 18.dp
)

@Composable
fun CharNotesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = CharNotesTypography,
        content = content
    )
}
