package com.thelab.mediahub.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Extracted 3D Soft Light & Dark Depth Modifier
fun Modifier.neuDepth(
    lightShadow: Color = Color.White.copy(alpha = 0.8f),
    darkShadow: Color = Color.Black.copy(alpha = 0.25f),
    shadowRadius: Dp = 8.dp,
    offset: Dp = 4.dp
): Modifier = this.drawBehind {
    drawIntoCanvas { canvas ->
        val lightPaint = Paint().apply {
            asFrameworkPaint().color = lightShadow.toArgb()
            asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(
                shadowRadius.toPx(),
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }
        val darkPaint = Paint().apply {
            asFrameworkPaint().color = darkShadow.toArgb()
            asFrameworkPaint().maskFilter = android.graphics.BlurMaskFilter(
                shadowRadius.toPx(),
                android.graphics.BlurMaskFilter.Blur.NORMAL
            )
        }

        // Top-Left Light Glow Shadow
        canvas.drawRoundRect(
            left = -offset.toPx(),
            top = -offset.toPx(),
            right = size.width - offset.toPx(),
            bottom = size.height - offset.toPx(),
            radiusX = 16.dp.toPx(),
            radiusY = 16.dp.toPx(),
            paint = lightPaint
        )

        // Bottom-Right Dark Extrusion Shadow
        canvas.drawRoundRect(
            left = offset.toPx(),
            top = offset.toPx(),
            right = size.width + offset.toPx(),
            bottom = size.height + offset.toPx(),
            radiusX = 16.dp.toPx(),
            radiusY = 16.dp.toPx(),
            paint = darkPaint
        )
    }
}
