package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Defined beautiful cute shapes with large border radius: 16dp, 20dp, 24dp for softness
val CuteShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),      // 16px/dp rounded corner
    large = RoundedCornerShape(20.dp),       // 20px/dp rounded corner
    extraLarge = RoundedCornerShape(24.dp)   // 24px/dp rounded corner
)

private val CuteLightColorScheme = lightColorScheme(
    primary = PrimaryLavender,            // Gentle pastel violet from design
    onPrimary = TextCharcoal,
    primaryContainer = PrimaryLavender, // Sweet light purple
    onPrimaryContainer = TextCharcoal,
    secondary = SecondaryPink,           // Soft pastel pink
    onSecondary = TextCharcoal,
    secondaryContainer = FocusPink,
    onSecondaryContainer = TextCharcoal,
    background = BackgroundCream,      // Warm off-white
    onBackground = TextCharcoal,
    surface = BackgroundCard,          // Clean pure white for list items
    onSurface = TextCharcoal,
    surfaceVariant = WarningPastel,     // Pastel yellow for moved/snoozed highlights
    onSurfaceVariant = TextCharcoal,
    error = ErrorCoral,                 // Coral for delete
    onError = TextCharcoal,
    tertiary = SuccessMint,             // Mint green for completed task state
    onTertiary = TextCharcoal,
    tertiaryContainer = SuccessMint
)

// Dark scheme keeping soft, comfortable tones
private val CuteDarkColorScheme = darkColorScheme(
    primary = PrimaryLavender,
    onPrimary = TextCharcoal,
    primaryContainer = PrimaryLavender,
    onPrimaryContainer = TextCharcoal,
    secondary = SecondaryPink,
    onSecondary = TextCharcoal,
    background = TextCharcoal,
    onBackground = BackgroundCream,
    surface = Color(0xFF1E1E1E),
    onSurface = BackgroundCream,
    error = ErrorCoral,
    onError = TextCharcoal,
    tertiary = SuccessMint,
    onTertiary = TextCharcoal,
    tertiaryContainer = SuccessMint
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Force playful, cozy light theme by default as per visual brand specs
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CuteDarkColorScheme else CuteLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            var context = view.context
            while (context is android.content.ContextWrapper) {
                if (context is Activity) break
                val base = context.baseContext
                if (base === context || base == null) break
                context = base
            }
            if (context is Activity) {
                try {
                    val window = context.window
                    window.statusBarColor = colorScheme.background.toArgb()
                    window.navigationBarColor = colorScheme.background.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CuteShapes,
        content = content
    )
}
