package io.github.rim95dev.island.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import io.github.rim95dev.island.notification.IslandNotification

private val ExpandedCornerRadius = 28.dp
private const val ICON_SIZE_PX = 108

@Composable
internal fun IslandContent(state: IslandState, onTap: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val motion = state.motion
    LaunchedEffect(state.expanded) {
        if (state.expanded) {
            progress.animateTo(1f, tween(durationMillis = motion.expandMillis, easing = motion.style.expandEasing()))
        } else {
            progress.animateTo(0f, tween(durationMillis = motion.collapseMillis, easing = motion.style.collapseEasing()))
        }
    }
    val expandedCornerPx = with(LocalDensity.current) { ExpandedCornerRadius.toPx() }
    val geometry = state.geometry ?: return
    val contentFadeStart = motion.contentFadeStart

    // The animated value is read only in the layout and draw phases, so frames skip recomposition.
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val bounds = geometry.boundsAt(progress.value, motion)
                    val placeable = measurable.measure(Constraints.fixed(bounds.width, bounds.height))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(bounds.left, bounds.top)
                    }
                }
                .graphicsLayer {
                    val bounds = geometry.boundsAt(progress.value, motion)
                    shape = RoundedCornerShape(minOf(bounds.height / 2f, expandedCornerPx))
                    clip = true
                }
                .background(Color.Black)
                .clickable(interactionSource = null, indication = null, onClick = onTap),
        ) {
            state.notification?.let { notification ->
                NotificationRow(
                    notification = notification,
                    modifier = Modifier
                        // Lays the row out at its final place on screen. While the island is smaller than the
                        // card, the parts outside it are clipped by the island's shape.
                        .layout { measurable, _ ->
                            val bounds = geometry.boundsAt(progress.value, motion)
                            val content = geometry.content
                            val placeable = measurable.measure(Constraints.fixed(content.width, content.height))
                            layout(bounds.width, bounds.height) {
                                placeable.place(content.left - bounds.left, content.top - bounds.top)
                            }
                        }
                        .graphicsLayer {
                            alpha = ((progress.value - contentFadeStart) / (1f - contentFadeStart)).coerceIn(0f, 1f)
                        },
                )
            }
        }
    }
}

/** Style 1 pops out of the camera and settles, then speeds up as it is pulled back in. */
private fun IslandAnimationStyle.expandEasing(): Easing = when (this) {
    IslandAnimationStyle.STYLE_1 -> LinearOutSlowInEasing
    IslandAnimationStyle.STYLE_2 -> LinearEasing
}

/** Style 2 eases each step of its path on its own, so its overall timing stays linear to keep the pause even. */
private fun IslandAnimationStyle.collapseEasing(): Easing = when (this) {
    IslandAnimationStyle.STYLE_1 -> FastOutLinearInEasing
    IslandAnimationStyle.STYLE_2 -> LinearEasing
}

@Composable
private fun NotificationRow(notification: IslandNotification, modifier: Modifier = Modifier) {
    val icon = remember(notification.key, notification.icon) {
        notification.icon?.toBitmap(ICON_SIZE_PX, ICON_SIZE_PX)?.asImageBitmap()
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(38.dp))
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column {
            Text(
                text = notification.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            notification.text?.let {
                Text(
                    text = it,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
