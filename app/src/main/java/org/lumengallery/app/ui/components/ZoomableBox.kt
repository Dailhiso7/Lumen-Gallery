package org.lumengallery.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    maxScale: Float = 4.0f,
    onDismiss: () -> Unit = {},
    content: @Composable () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Swipe down to dismiss state
    var dismissOffsetY by remember { mutableFloatStateOf(0f) }

    val animatedScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(stiffness = 500f),
        label = "scale"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(0, dismissOffsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        scale = if (scale > 1.2f) {
                            offsetX = 0f
                            offsetY = 0f
                            1f
                        } else {
                            // Zoom in towards 2.5x
                            2.5f
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(0.85f, maxScale)

                    if (scale <= 1.05f && pan.y > 20f && kotlin.math.abs(pan.x) < pan.y) {
                        // Drag down to dismiss gesture
                        dismissOffsetY += pan.y
                        if (dismissOffsetY > 250f) {
                            onDismiss()
                        }
                    } else {
                        scale = newScale
                        if (scale > 1f) {
                            val maxOffsetX = (size.width * (scale - 1f)) / 2f
                            val maxOffsetY = (size.height * (scale - 1f)) / 2f
                            offsetX = (offsetX + pan.x * scale).coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = (offsetY + pan.y * scale).coerceIn(-maxOffsetY, maxOffsetY)
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                            dismissOffsetY = 0f
                        }
                    }
                }
            }
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                translationX = offsetX
                translationY = offsetY
                // Fade out slightly when dragging down to dismiss
                alpha = if (dismissOffsetY > 0f) {
                    (1f - (dismissOffsetY / 500f)).coerceIn(0.2f, 1f)
                } else 1f
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
