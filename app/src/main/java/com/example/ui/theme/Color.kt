package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Sleek Blue Brand Colors
val PrimaryBlue = Color(0xFF1976D2)        // Beautiful vibrant primary blue matching screenshot
val SecondaryBlue = Color(0xFF1156A0)      // Slate-blue gradient end
val HighlightBlue = Color(0xFFE3EDFD)      // Light soft blue container matching screenshot
val SleekNavy = Color(0xFF1A2530)          // Clean dark navy matching screenshot

// Backwards compatibility mappings for green variables used in the layout
val PrimaryGreen = PrimaryBlue             // Remapped to PrimaryBlue for Sleek Theme
val PrimaryLight = SecondaryBlue            // Remapped to SecondaryBlue
val SecondaryGreen = HighlightBlue          // Remapped to HighlightBlue
val AccentGold = Color(0xFFFFB4AB)         // Soft warm accent
val AccentGoldDark = Color(0xFFBA1A1A)     // Modern subtle red accent

// Light Theme Colors
val BackgroundLight = Color(0xFFF5F7FB)    // Extremely clean light grayish-blue background from screenshot
val SurfaceLight = Color(0xFFFFFFFF)       // Clean white surfaces from screenshot
val TextPrimaryLight = SleekNavy           // Navy for high contrast text
val TextSecondaryLight = Color(0xFF5C6B73) // Soft dark slate gray text

// Dark Theme Colors
val BackgroundDark = Color(0xFF0B121F)     // Sleek deep dark navy background
val SurfaceDark = Color(0xFF151D2A)        // Soft dark blue surfaces
val TextPrimaryDark = Color(0xFFE2E2E6)    // Soft off-white text
val TextSecondaryDark = Color(0xFF8C9199)  // Slate-gray text

// Sleek Blue Gradient Assets (retaining gradient variables used by screens)
val TealGradientStart = Color(0xFF1D8DF1)  // Vivid sky blue from screenshot
val TealGradientEnd = Color(0xFF1367C8)    // Vivid ocean blue from screenshot

val PurpleGradientStart = Color(0xFF4C8DF5)
val PurpleGradientEnd = Color(0xFF004174)

val GoldGradientStart = Color(0xFF1D619B)
val GoldGradientEnd = Color(0xFF004B80)

val BlueGradientStart = Color(0xFF569FF0)
val BlueGradientEnd = Color(0xFF003059)

