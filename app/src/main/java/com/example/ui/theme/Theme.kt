package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7FA185),
    onPrimary = Color(0xFF102013),
    primaryContainer = DeepSageDark,
    onPrimaryContainer = Color(0xFFE3EFE5),
    secondary = WarmSandGold,
    onSecondary = Color(0xFF231A08),
    secondaryContainer = Color(0xFF3E341D),
    onSecondaryContainer = WarmSandGoldSoft,
    tertiary = WarmSandGold,
    onTertiary = Color(0xFF231A08),
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF3D4B40),
    outlineVariant = Color(0xFF2B372D),
    error = Color(0xFFE57373),
    onError = Color(0xFF2B0B0B),
    errorContainer = Color(0xFF4A1D1D),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = DeepSage,
    onPrimary = PureWhite,
    primaryContainer = SoftSage,
    onPrimaryContainer = DeepForestCharcoal,
    secondary = SoftSageLight,
    onSecondary = DeepForestCharcoal,
    secondaryContainer = SoftSageLight,
    onSecondaryContainer = DeepForestCharcoal,
    tertiary = WarmSandGold,
    onTertiary = PureWhite,
    tertiaryContainer = WarmSandGoldSoft,
    onTertiaryContainer = DeepForestCharcoal,
    background = WarmCream,
    onBackground = DeepForestCharcoal,
    surface = PureWhite,
    onSurface = DeepForestCharcoal,
    surfaceVariant = SoftIvory,
    onSurfaceVariant = MutedOliveGray,
    outline = SageBorderStrong,
    outlineVariant = SageBorder,
    error = Color(0xFFBA3C3C),
    onError = PureWhite,
    errorContainer = Color(0xFFFCEAEA),
    onErrorContainer = Color(0xFF691818)
)

val OrganicShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Organic Minimalist Warm Cream theme by default, matching reference
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = OrganicShapes,
        content = content
    )
}
