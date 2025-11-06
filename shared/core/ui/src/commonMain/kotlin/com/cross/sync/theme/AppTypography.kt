package com.cross.sync.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

data class AppTypography(
    // Regular
    private val regular: TextStyle = TextStyle(
        fontWeight = FontWeight.Normal,
        fontStyle = FontStyle.Normal,
    ),

    // Regular 10
    val regular10: TextStyle = regular.copy(
        fontSize = 10.sp
    ),
    val regular10Center: TextStyle = regular10.copy(
        textAlign = TextAlign.Center
    ),

    // Regular 11
    val regular11: TextStyle = regular.copy(
        fontSize = 11.sp
    ),
    val regular11Center: TextStyle = regular11.copy(
        textAlign = TextAlign.Center
    ),

    // Regular 12
    val regular12: TextStyle = regular.copy(
        fontSize = 12.sp
    ),
    val regular12Center: TextStyle = regular12.copy(
        textAlign = TextAlign.Center
    ),

    // Regular 14
    val regular14: TextStyle = regular.copy(
        fontSize = 14.sp
    ),
    val regular14Italic: TextStyle = regular14.copy(
        fontStyle = FontStyle.Italic
    ),
    val regular14Center: TextStyle = regular14.copy(
        textAlign = TextAlign.Center
    ),

    // Regular 16
    val regular16: TextStyle = regular.copy(
        fontSize = 16.sp
    ),
    val regular16Center: TextStyle = regular16.copy(
        textAlign = TextAlign.Center
    ),

    // Regular 20
    val regular20: TextStyle = regular.copy(
        fontSize = 20.sp
    ),
    val regular20Center: TextStyle = regular20.copy(
        textAlign = TextAlign.Center
    ),

    // Regular 22
    val regular22: TextStyle = regular.copy(
        fontSize = 22.sp
    ),
    // Regular 28
    val regular28: TextStyle = regular.copy(
        fontSize = 28.sp
    ),

    // Medium
    val medium: TextStyle = TextStyle(
        fontWeight = FontWeight.Medium,
        fontStyle = FontStyle.Normal,
    ),

    // Medium 11
    val medium11: TextStyle = medium.copy(
        fontSize = 11.sp
    ),

    // Medium 12
    val medium12: TextStyle = medium.copy(
        fontSize = 12.sp
    ),

    // Medium 14
    val medium14: TextStyle = medium.copy(
        fontSize = 14.sp
    ),

    // Medium 16
    val medium16: TextStyle = medium.copy(
        fontSize = 16.sp
    ),

    // Medium 20
    val medium20: TextStyle = medium.copy(
        fontSize = 20.sp
    ),

    // Medium 20
    val medium20Center: TextStyle = medium.copy(
        fontSize = 20.sp,
        textAlign = TextAlign.Center
    ),

    val medium22: TextStyle = medium.copy(
        fontSize = 22.sp
    ),

    // Medium 20
    val medium26: TextStyle = medium.copy(
        fontSize = 26.sp
    ),

    val medium16Center: TextStyle = medium16.copy(
        textAlign = TextAlign.Center
    ),

    // Semi Bold
    val semiBold: TextStyle = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontStyle = FontStyle.Normal,
    ),

    // Semi Bold - 10
    val semiBold10: TextStyle = semiBold.copy(
        fontSize = 10.sp
    ),

    // Semi Bold - 12
    val semiBold12: TextStyle = semiBold.copy(
        fontSize = 12.sp
    ),

    // Semi Bold - 14
    val semiBold14: TextStyle = semiBold.copy(
        fontSize = 14.sp
    ),
    val semiBold14Center: TextStyle = semiBold14.copy(
        textAlign = TextAlign.Center
    ),

    // Semi Bold 16
    val semiBold16: TextStyle = semiBold.copy(
        fontSize = 16.sp
    ),

    val semiBold16Center: TextStyle = semiBold16.copy(
        textAlign = TextAlign.Center
    ),

    // Semi Bold 16
    val semiBold20: TextStyle = semiBold.copy(
        fontSize = 20.sp
    ),

    // Semi Bold 20
    val semiBold20Center: TextStyle = semiBold.copy(
        fontSize = 20.sp,
        textAlign = TextAlign.Center
    ),

    // Bold
    val bold: TextStyle = TextStyle(
        fontWeight = FontWeight.Bold,
        fontStyle = FontStyle.Normal,
    ),

    // Bold 12
    val bold12: TextStyle = bold.copy(
        fontSize = 12.sp
    ),

    val bold12Center: TextStyle = bold12.copy(
        textAlign = TextAlign.Center
    ),

    // Bold 14
    val bold14: TextStyle = bold.copy(
        fontSize = 14.sp
    ),
    val bold14Center: TextStyle = bold14.copy(
        textAlign = TextAlign.Center
    ),

    // Bold 16
    val bold16: TextStyle = bold.copy(
        fontSize = 16.sp
    ),
    val bold16Center: TextStyle = bold16.copy(
        textAlign = TextAlign.Center
    ),
    // Bold 20
    val bold20: TextStyle = bold.copy(
        fontSize = 20.sp
    ),
    val bold20Center: TextStyle = bold20.copy(
        textAlign = TextAlign.Center
    ),

    // Bold 22
    val bold22: TextStyle = bold.copy(
        fontSize = 22.sp
    ),

    // Bold 24
    val bold24: TextStyle = bold.copy(
        fontSize = 24.sp
    ),
    val bold24Center: TextStyle = bold24.copy(
        textAlign = TextAlign.Center
    ),
    // Bold 28
    val bold28: TextStyle = bold.copy(
        fontSize = 28.sp
    ),
    val bold28Center: TextStyle = bold28.copy(
        textAlign = TextAlign.Center
    ),

    // Bold 32
    val bold32: TextStyle = bold.copy(
        fontSize = 32.sp
    ),

    // Extra Bold
    val extraBold: TextStyle = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontStyle = FontStyle.Normal,
    ),

    // extrabold 20

    val extraBold20: TextStyle = extraBold.copy(
        fontSize = 20.sp
    ),

    val extraBoldCenter: TextStyle = extraBold.copy(
        textAlign = TextAlign.Center
    ),
)

val LocalAppTypography = staticCompositionLocalOf {
    AppTypography()
}