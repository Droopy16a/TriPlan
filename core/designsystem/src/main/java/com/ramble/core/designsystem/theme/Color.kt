package com.ramble.core.designsystem.theme

import androidx.compose.ui.graphics.Color

// New Branding Palette
val RambleGreen = Color(0xFF16C47F)
val RambleGreenDark = Color(0xFF0FA36B)
val RambleNavy = Color(0xFF2B2D42)
val RambleLight = Color(0xFFF2F4F7)

// Legacy Mappings (updated to match new palette)
val DeepGraphite = RambleNavy
val OffWhite = RambleLight
val CardBackground = Color.White
val UIBackgroundGray = Color(0xFFF9FAFB)
val UIBorderGray = Color(0xFFE5E7EB)
val SurfaceGray = Color(0xFFF3F4F6)
val LightGray = Color(0xFFD1D5DB)

val BrandDarkGreen = RambleGreenDark
val BrandLightGreen = RambleGreen
val SkyBlue = RambleGreen // Map sky blue to green for consistency
val SkyBlueLight = RambleGreen.copy(alpha = 0.1f)
val EmeraldGreen = RambleGreenDark

// Dark Theme specific
val DarkBackground = RambleNavy
val DarkSurface = Color(0xFF1E293B)
val DarkText = Color(0xFFF8FAFC)

// Functional Colors
val ErrorRed = Color(0xFFE63946)
val WarningYellow = Color(0xFFFFA303)

// Deprecated (to be removed)
@Deprecated("Use RambleGreen or MaterialTheme.colorScheme.primary")
val TravelRed = ErrorRed
@Deprecated("Use RambleGreenDark")
val TravelRedDark = Color(0xFFC1121F)
