package dev.nichidori.saku.core.composable

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

data class ThemeSwitcherRequest(val id: Long, val origin: Offset)

@Composable
fun MyThemeSwitcher(
    isDark: Boolean,
    request: ThemeSwitcherRequest?,
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float> = tween(durationMillis = 750, easing = FastOutSlowInEasing),
    content: @Composable (dark: Boolean) -> Unit,
) {
    var dark by remember { mutableStateOf(isDark) }
    var snapshotBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var revealOrigin by remember { mutableStateOf(Offset.Zero) }
    val revealProgress = remember { Animatable(1f) }
    val captureLayer = rememberGraphicsLayer()
    var lastHandledId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(request) {
        val req = request ?: return@LaunchedEffect
        if (req.id == lastHandledId) return@LaunchedEffect
        lastHandledId = req.id

        withFrameNanos { } // ensure captureLayer holds the latest frame
        snapshotBitmap = captureLayer.toImageBitmap()
        revealOrigin = req.origin
        dark = isDark

        revealProgress.snapTo(0f)
        revealProgress.animateTo(targetValue = 1f, animationSpec = animationSpec)
        snapshotBitmap = null
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    captureLayer.record { this@drawWithContent.drawContent() }
                    drawLayer(captureLayer)
                },
        ) {
            content(dark)
        }

        snapshotBitmap?.let {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val maxRadius = sqrt(
                    max(revealOrigin.x, size.width - revealOrigin.x).pow(2) +
                            max(revealOrigin.y, size.height - revealOrigin.y).pow(2),
                )
                val currentRadius = revealProgress.value * maxRadius

                if (currentRadius < maxRadius) {
                    val clipPath = Path().apply {
                        addRect(Rect(Offset.Zero, size))
                        addOval(Rect(center = revealOrigin, radius = currentRadius))
                        fillType = PathFillType.EvenOdd
                    }
                    clipPath(clipPath) {
                        drawImage(
                            image = it,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt())
                        )
                    }
                }
            }
        }
    }
}