package io.github.rim95dev.island.ui

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.rim95dev.island.R
import io.github.rim95dev.island.overlay.ScreenCamera
import io.github.rim95dev.island.overlay.detectScreenCamera
import io.github.rim95dev.island.settings.CameraCalibration
import io.github.rim95dev.island.settings.IslandSettings

private val CrosshairColor = Color(0xFFE53935)
private val StepSizes = listOf(-10, -1, 1, 10)

/**
 * Lets the user line up the island's circle with the front camera of the current screen.
 *
 * The camera hides the circle's center, so long dashed lines cross there to show where it is, and a ring
 * shows the circle's size so it can be fitted around the camera. Values change only with the controls in
 * the card, never by touching the background.
 */
@Composable
fun CameraCalibrationScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val settings = remember { IslandSettings.get(context) }
    val screen = remember(configuration) { detectScreenCamera(context) }
    var calibration by remember(screen.key) { mutableStateOf(savedOrDefault(settings, screen)) }
    var isCustom by remember(screen.key) { mutableStateOf(screen.key in settings.cameraCalibrations.value) }
    var origin by remember { mutableStateOf(Offset.Zero) }

    fun save(new: CameraCalibration) {
        val clamped = CameraCalibration(
            x = new.x.coerceIn(0, screen.width),
            y = new.y.coerceIn(0, screen.height),
            diameter = (new.diameter ?: screen.defaultCircleDiameter).coerceIn(MIN_DIAMETER, MAX_DIAMETER),
        )
        calibration = clamped
        isCustom = true
        settings.setCameraCalibration(screen.key, clamped)
    }

    FullScreenWithCutout()
    BackHandler(onBack = onDone)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .onGloballyPositioned { origin = it.positionOnScreen() }
            .drawBehind {
                val x = calibration.x - origin.x
                val y = calibration.y - origin.y
                val stroke = 1.5.dp.toPx()
                val dashes = PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(), 8.dp.toPx()))
                drawLine(CrosshairColor, Offset(0f, y), Offset(size.width, y), stroke, pathEffect = dashes)
                drawLine(CrosshairColor, Offset(x, 0f), Offset(x, size.height), stroke, pathEffect = dashes)
                val diameter = calibration.diameter ?: screen.defaultCircleDiameter
                drawCircle(CrosshairColor, radius = diameter / 2f, center = Offset(x, y), style = Stroke(stroke))
            }
            .imePadding(),
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .widthIn(max = 420.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(text = stringResource(R.string.calibration_title), style = MaterialTheme.typography.titleLarge)
                Text(
                    text = stringResource(R.string.calibration_screen, screen.width, screen.height),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(if (isCustom) R.string.calibration_custom else R.string.calibration_detected),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(text = stringResource(R.string.calibration_hint), style = MaterialTheme.typography.bodyMedium)
                AxisControl(
                    label = stringResource(R.string.calibration_horizontal),
                    value = calibration.x,
                    onValueChange = { save(calibration.copy(x = it)) },
                )
                AxisControl(
                    label = stringResource(R.string.calibration_vertical),
                    value = calibration.y,
                    onValueChange = { save(calibration.copy(y = it)) },
                )
                AxisControl(
                    label = stringResource(R.string.calibration_size),
                    value = calibration.diameter ?: screen.defaultCircleDiameter,
                    onValueChange = { save(calibration.copy(diameter = it)) },
                )
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            settings.setCameraCalibration(screen.key, null)
                            calibration = screen.defaultCalibration()
                            isCustom = false
                        },
                    ) {
                        Text(text = stringResource(R.string.calibration_reset))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = onDone) {
                        Text(text = stringResource(R.string.calibration_done))
                    }
                }
            }
        }
    }
}

/** A pixel value with step buttons on both sides and a number field in the middle. */
@Composable
private fun AxisControl(label: String, value: Int, onValueChange: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepSizes.take(2).forEach { step -> StepButton(step, Modifier.weight(1f)) { onValueChange(value + step) } }
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    text = input.filter(Char::isDigit).take(MAX_DIGITS)
                    text.toIntOrNull()?.let(onValueChange)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.weight(1.6f),
            )
            StepSizes.drop(2).forEach { step -> StepButton(step, Modifier.weight(1f)) { onValueChange(value + step) } }
        }
    }
}

@Composable
private fun StepButton(step: Int, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 4.dp), modifier = modifier) {
        Text(text = if (step > 0) "+$step" else "−${-step}")
    }
}

/** Hides the system bars and lets the window draw into the camera cutout, so the crosshair can reach it. */
@Composable
private fun FullScreenWithCutout() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity) {
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val previousCutoutMode = window.attributes.layoutInDisplayCutoutMode
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = previousCutoutMode }
        }
    }
}

private const val MAX_DIGITS = 5
private const val MIN_DIAMETER = 4
private const val MAX_DIAMETER = 400

private fun ScreenCamera.defaultCalibration() =
    CameraCalibration(detectedHole.centerX, detectedHole.centerY, defaultCircleDiameter)

private fun savedOrDefault(settings: IslandSettings, screen: ScreenCamera): CameraCalibration {
    val saved = settings.cameraCalibrations.value[screen.key] ?: return screen.defaultCalibration()
    return saved.copy(diameter = saved.diameter ?: screen.defaultCircleDiameter)
}
