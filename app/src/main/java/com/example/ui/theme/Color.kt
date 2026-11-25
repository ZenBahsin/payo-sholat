package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// Organic Minimalist Design System ("Payo Shalat")
// =========================================================================

// Core Palette Tokens
val WarmCream = Color(0xFFF6F4EC)             // Main Background
val SoftIvory = Color(0xFFFCFBF7)             // Secondary Card Surface
val PureWhite = Color(0xFFFFFFFF)             // Pure White Card Surface
val DeepSage = Color(0xFF4A664F)              // Primary (Hero Card, CTA, Active Check)
val DeepSageDark = Color(0xFF39503D)          // Deep Sage Gradient End / Dark Bento
val DeepSageSurface = Color(0xFF56735B)       // Subtle Elevated Sage
val SoftSage = Color(0xFFDCE5D8)              // Secondary (Curved Header, Bento Card)
val SoftSageLight = Color(0xFFE8EFE5)         // Active Schedule Highlight, Soft Pill
val WarmSandGold = Color(0xFFC8A258)          // Accent (CTA "Lanjut", Active Date Pill, Sun Dot)
val WarmSandGoldDark = Color(0xFFA9843D)      // Accent Pressed / Deep Gold Text
val WarmSandGoldSoft = Color(0xFFF5EBD6)      // Streak Badge / Warm Highlight Pill
val DeepForestCharcoal = Color(0xFF1E2A20)    // Primary Text
val MutedOliveGray = Color(0xFF68766A)        // Secondary Text
val SageBorder = Color(0x1F4A664F)            // rgba(74, 102, 79, 0.12)
val SageBorderStrong = Color(0x384A664F)      // rgba(74, 102, 79, 0.22)
val SoftSageDivider = Color(0xFFECEAE0)       // Hairline Divider on Warm Cream

// Dark Theme Organic Forest Palette
val BackgroundDark = Color(0xFF131A15)        // Deep Forest Charcoal Dark
val SurfaceDark = Color(0xFF1C261E)           // Elevated Forest Surface
val SurfaceVariantDark = Color(0xFF263329)    // Muted Sage Surface Dark
val TextPrimaryDark = Color(0xFFF2F0E8)       // Warm Ivory Text
val TextSecondaryDark = Color(0xFFA4B0A6)     // Soft Olive Gray Dark

// Light Theme Aliases
val BackgroundLight = WarmCream
val SurfaceLight = PureWhite
val TextPrimaryLight = DeepForestCharcoal
val TextSecondaryLight = MutedOliveGray

// Semantic Remappings (Replaces all legacy blue references across secondary screens)
val PrimaryBlue = DeepSage
val SecondaryBlue = DeepSageDark
val HighlightBlue = SoftSageLight
val SleekNavy = DeepForestCharcoal

val PrimaryGreen = DeepSage
val PrimaryLight = DeepSageSurface
val SecondaryGreen = SoftSage
val AccentGold = WarmSandGold
val AccentGoldDark = WarmSandGoldDark

// Organic Sage & Gold Gradients (Replaces legacy blue gradients)
val TealGradientStart = Color(0xFF56735B)
val TealGradientEnd = Color(0xFF3E5642)

val PurpleGradientStart = Color(0xFF4A664F)
val PurpleGradientEnd = Color(0xFF354A39)

val GoldGradientStart = Color(0xFFC8A258)
val GoldGradientEnd = Color(0xFFA9843D)

val BlueGradientStart = Color(0xFF5E7B63)
val BlueGradientEnd = Color(0xFF3A503E)
