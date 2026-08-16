package com.ramble.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// "Organic and friendly"
// Cards: 24dp rounded corners.
// Buttons: Pill-shaped (CircleShape).
// Bottom Sheets: Extra rounded top corners.

val RambleShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp), // Used for Cards
    extraLarge = RoundedCornerShape(32.dp) // Used for Bottom sheets
)

// Reusable shapes
val PillShape = RoundedCornerShape(percent = 50)
val BottomSheetShape = RoundedCornerShape(
    topStart = 32.dp,
    topEnd = 32.dp,
    bottomStart = 0.dp,
    bottomEnd = 0.dp
)
