package com.enchanted.app.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------------------
// Material‑3 Teal‑based palette (light & dark)
// ---------------------------------------------------------------------------
// Primary teal shades
val Primary = Color(0xFF006D77)          // teal‑700 – main brand color
val OnPrimary = Color.White               // text/icons on primary

// Secondary (soft mint) – used for accents, secondary actions
val Secondary = Color(0xFF83C5BE)        // teal‑300
val OnSecondary = Color.White

// Tertiary (warm amber) – for highlights, warnings, snackbars
val Tertiary = Color(0xFFFFCB77)         // amber‑200
val OnTertiary = Color.Black

// Background & surface colors
val BackgroundLight = Color(0xFFF5F5F5)   // light gray background
val SurfaceLight = Color.White           // cards, dialogs
val BackgroundDark = Color(0xFF121212)    // dark mode background
val SurfaceDark = Color(0xFF1E1E1E)        // dark surface

// Text colors – ensure contrast ≥ 4.5:1
val TextPrimaryLight = Color(0xFF212121)
val TextPrimaryDark = Color(0xFFF5F5F5)
val TextSecondaryLight = Color(0xFF8E8E93)
val TextSecondaryDark = Color(0xFF8E8E93)

// Message bubbles (chat UI)
val MessageBubbleUserLight = Primary
val MessageBubbleUserDark = Primary
val MessageBubbleAssistantLight = Color(0xFFE8E8ED)
val MessageBubbleAssistantDark = Color(0xFF2C2C2E)

// Semantic colors
val ErrorRed = Color(0xFFB00020)
val SuccessGreen = Color(0xFF34C759)
val WarningYellow = Color(0xFFFFCC00)

// Code block background (optional)
val CodeBackgroundLight = Color(0xFFF2F2F7)
val CodeBackgroundDark = Color(0xFF2C2C2E)
