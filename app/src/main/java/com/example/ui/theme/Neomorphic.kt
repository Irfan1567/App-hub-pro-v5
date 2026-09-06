package com.example.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Adaptive Neomorphic Styling Tokens
 */
object NeomorphicDefaults {
    // Light Mode (Warm Matte Clay)
    val LightBg = Color(0xFFE9EEF4)
    val LightSurface = Color(0xFFF1F5F9)
    val LightSurfaceRaised = Color(0xFFF8FAFC)
    val LightHighlight = Color(0xFFFFFFFF)
    val LightShadow = Color(0xFFC7D2E0)
    val LightPrimary = Color(0xFF2563EB)
    val LightPrimarySoft = Color(0xFFDBEAFE)
    val LightTextPrimary = Color(0xFF0F172A)
    val LightTextSecondary = Color(0xFF475569)

    // Dark Mode (Matte Obsidian Clay)
    val DarkBg = Color(0xFF0F141E)
    val DarkSurface = Color(0xFF161D2A)
    val DarkSurfaceRaised = Color(0xFF1E283A)
    val DarkHighlight = Color(0xFF2B384F)
    val DarkShadow = Color(0xFF070A10)
    val DarkPrimary = Color(0xFF38BDF8)
    val DarkPrimarySoft = Color(0xFF0C4A6E)
    val DarkTextPrimary = Color(0xFFF8FAFC)
    val DarkTextSecondary = Color(0xFF94A3B8)
}

/**
 * Helper to get active theme-aware primary accent (soft on eyes in Day mode, glowing in Night mode)
 */
val MaterialTheme.accentColor: Color
    @Composable
    get() = colorScheme.primary

val MaterialTheme.accentSoft: Color
    @Composable
    get() = colorScheme.primaryContainer

val MaterialTheme.isDarkTheme: Boolean
    @Composable
    get() = colorScheme.background == NeomorphicDefaults.DarkBg || colorScheme.background.red < 0.2f

/**
 * Tactile Neomorphic Surface Box (Extruded / Raised)
 */
@Composable
fun NeomorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    elevation: Dp = 4.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.isDarkTheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val surfaceColor = if (isDark) {
        if (isPressed) Color(0xFF131924) else NeomorphicDefaults.DarkSurface
    } else {
        if (isPressed) Color(0xFFE2E8F0) else NeomorphicDefaults.LightSurface
    }

    val highlightColor = if (isDark) {
        NeomorphicDefaults.DarkHighlight.copy(alpha = if (isPressed) 0.3f else 0.7f)
    } else {
        NeomorphicDefaults.LightHighlight.copy(alpha = if (isPressed) 0.5f else 0.95f)
    }

    val shadowColor = if (isDark) {
        NeomorphicDefaults.DarkShadow.copy(alpha = 0.8f)
    } else {
        NeomorphicDefaults.LightShadow.copy(alpha = 0.8f)
    }

    val actualElevation = if (isPressed) 1.dp else elevation

    val clickableModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .shadow(
                elevation = actualElevation,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(surfaceColor)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        highlightColor,
                        shadowColor.copy(alpha = 0.3f)
                    )
                ),
                shape = shape
            )
            .then(clickableModifier),
        content = content
    )
}

/**
 * Tactile Inset / Recessed Surface Box (Debossed - for Search Bars, Code / Scratchpad, Displays)
 */
@Composable
fun NeomorphicInsetBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = MaterialTheme.isDarkTheme
    val insetColor = if (isDark) Color(0xFF0B0F17) else Color(0xFFDFE6EE)
    val shadowRim = if (isDark) Color(0xFF04060A) else Color(0xFFBAC7D5)
    val highlightRim = if (isDark) Color(0xFF222B3B) else Color(0xFFFFFFFF)

    Box(
        modifier = modifier
            .clip(shape)
            .background(insetColor)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        shadowRim.copy(alpha = 0.6f),
                        highlightRim.copy(alpha = 0.4f)
                    )
                ),
                shape = shape
            ),
        content = content
    )
}

/**
 * Tactile Neomorphic Button with tactile press & theme-calibrated styling
 */
@Composable
fun NeomorphicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    shape: Shape = RoundedCornerShape(14.dp),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.isDarkTheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundBrush = if (isPrimary) {
        if (isDark) {
            Brush.linearGradient(listOf(Color(0xFF0284C7), Color(0xFF0369A1)))
        } else {
            Brush.linearGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
        }
    } else {
        val baseColor = if (isDark) {
            if (isPressed) Color(0xFF141A26) else Color(0xFF1E2838)
        } else {
            if (isPressed) Color(0xFFDFE6F0) else Color(0xFFF1F5F9)
        }
        Brush.linearGradient(listOf(baseColor, baseColor))
    }

    val shadowColor = if (isDark) Color(0xFF05070D) else Color(0xFFBAC6D6)
    val highlightColor = if (isDark) Color(0xFF2E3D55) else Color(0xFFFFFFFF)
    val elevation = if (isPressed) 1.dp else (if (isPrimary) 4.dp else 3.dp)

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(shape)
            .background(backgroundBrush)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isPrimary) {
                        listOf(Color.White.copy(alpha = 0.4f), Color.Transparent)
                    } else {
                        listOf(highlightColor.copy(alpha = 0.8f), shadowColor.copy(alpha = 0.3f))
                    }
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
