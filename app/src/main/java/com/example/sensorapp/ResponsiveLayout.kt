package com.example.sensorapp.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.roundToInt

@Composable
fun ResponsiveLayout() {
    val orientation = LocalConfiguration.current.orientation

    // Detect if the device is laid flat
    val isFlat = remember { mutableStateOf(false) }

    // Detect orientation based on sensor data
    val deviceOrientation = remember { mutableStateOf("Unknown") }

    // Maintain last 500 sensor values
    val accelerometerValues = remember { mutableStateListOf<FloatArray>() }

    // Bubble level angles
    val bubbleAngle = remember { mutableStateOf(0f) } // 1D angle
    val bubbleAngleX = remember { mutableStateOf(0f) } // 2D X angle
    val bubbleAngleY = remember { mutableStateOf(0f) } // 2D Y angle

    // Maximum and minimum values for X and Y
    val maxAngleX = remember { mutableStateOf(Float.MIN_VALUE) }
    val minAngleX = remember { mutableStateOf(Float.MAX_VALUE) }
    val maxAngleY = remember { mutableStateOf(Float.MIN_VALUE) }
    val minAngleY = remember { mutableStateOf(Float.MAX_VALUE) }

    DetectFlatDevice { flat -> isFlat.value = flat }
    DetectDeviceOrientation { orientation, values ->
        deviceOrientation.value = orientation

        // Add sensor values to the buffer
        if (accelerometerValues.size >= 500) {
            accelerometerValues.removeAt(0) // Remove oldest value when buffer is full
        }
        accelerometerValues.add(values)

        // Calculate 1D and 2D bubble angles
        bubbleAngle.value = calculateBubbleAngle(values) // 1D case
        val (angleX, angleY) = calculateBubbleAngles2D(values) // 2D case
        bubbleAngleX.value = angleX
        bubbleAngleY.value = angleY

        // Update max and min values for X and Y angles
        maxAngleX.value = maxOf(maxAngleX.value, angleX)
        minAngleX.value = minOf(minAngleX.value, angleX)
        maxAngleY.value = maxOf(maxAngleY.value, angleY)
        minAngleY.value = minOf(minAngleY.value, angleY)
    }

    if (isFlat.value) {
        // Device is laid flat
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Device is flat", style = MaterialTheme.typography.headlineMedium)
        }
    } else {
        // Show Bubble Levels for both 1D and 2D
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "1D Bubble Level",
                style = MaterialTheme.typography.headlineMedium
            )
            BubbleLevel(angle = bubbleAngle.value) // Display 1D bubble level
            Text(
                text = "1D Angle: ${bubbleAngle.value}°",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "2D Bubble Level",
                style = MaterialTheme.typography.headlineMedium
            )
            BubbleLevel2D(angleX = bubbleAngleX.value, angleY = bubbleAngleY.value) // Display 2D bubble level

            // Display the current X and Y angles of the 2D bubble
            Text(
                text = "2D Bubble Angles:",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "X-Axis: ${bubbleAngleX.value}°",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Y-Axis: ${bubbleAngleY.value}°",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Display the maximum and minimum values for X and Y
            Text(
                text = "Max & Min Values:",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Max X: ${maxAngleX.value}°, Min X: ${minAngleX.value}°",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "Max Y: ${maxAngleY.value}°, Min Y: ${minAngleY.value}°",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun BubbleLevel2D(angleX: Float, angleY: Float) {
    Canvas(modifier = Modifier.size(200.dp)) {
        val center = Offset(size.width / 2, size.height / 2)

        // Draw circle outline
        drawCircle(
            color = Color.Gray,
            center = center,
            radius = size.minDimension / 2,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
        )

        // Draw North indicator line
        val northLineLength = size.minDimension / 2 - 20.dp.toPx()
        drawLine(
            color = Color.Red,
            start = center,
            end = Offset(center.x, center.y - northLineLength), // Line pointing upwards
            strokeWidth = 4.dp.toPx()
        )

        // Draw bubble
        val maxRange = size.minDimension / 2 - 20.dp.toPx()
        val normalizedX = angleX.coerceIn(-10f, 10f) / 10f // Normalize X to range [-1, 1]
        val normalizedY = angleY.coerceIn(-10f, 10f) / 10f // Normalize Y to range [-1, 1]
        val bubbleOffset = Offset(
            x = center.x + normalizedX * maxRange,
            y = center.y - normalizedY * maxRange // Invert Y for correct screen mapping
        )
        drawCircle(
            color = Color.Blue,
            center = bubbleOffset,
            radius = 10.dp.toPx()
        )
    }
}


fun calculateBubbleAngles2D(values: FloatArray): Pair<Float, Float> {
    val x = values[0]
    val y = values[1]
    val z = values[2]

    // Calculate the angles using arctangent
    val angleX = atan2(x, z) * (180 / Math.PI).toFloat()
    val angleY = atan2(y, z) * (180 / Math.PI).toFloat()

    return Pair(angleX.roundToInt().toFloat(), angleY.roundToInt().toFloat())
}

@Composable
fun BubbleLevel(angle: Float) {
    Canvas(modifier = Modifier.size(200.dp)) {
        val center = Offset(size.width / 2, size.height / 2)

        // Draw circle outline
        drawCircle(
            color = Color.Gray,
            center = center,
            radius = size.minDimension / 2,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
        )

        // Draw bubble
        val maxRange = size.minDimension / 2 - 20.dp.toPx()
        val normalizedAngle = angle.coerceIn(-10f, 10f) / 10f // Normalize to range [-1, 1]
        val bubbleOffset = Offset(
            x = center.x + normalizedAngle * maxRange,
            y = center.y // Bubble only moves horizontally in 1D case
        )
        drawCircle(
            color = Color.Blue,
            center = bubbleOffset,
            radius = 10.dp.toPx()
        )
    }
}

fun calculateBubbleAngle(values: FloatArray): Float {
    val x = values[0]
    val z = values[2]

    // Calculate the angle using arctangent
    val angle = atan2(x, z) * (180 / Math.PI).toFloat()
    return angle.roundToInt().toFloat() // Round to integer for simplicity
}

@Composable
fun DetectFlatDevice(onFlatDetected: (Boolean) -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val flatThreshold = 9.8f // Approximate gravity value when flat

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // Check if the device is flat (close to gravity in Z-axis)
                    val isFlat = Math.abs(it.values[2]) >= flatThreshold
                    onFlatDetected(isFlat)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Register listener
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
}

@Composable
fun DetectDeviceOrientation(
    onOrientationDetected: (String, FloatArray) -> Unit
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]

                    // Determine orientation based on X and Y values
                    val orientation = when {
                        Math.abs(x) > Math.abs(y) -> "Landscape"
                        Math.abs(y) > Math.abs(x) -> "Portrait"
                        else -> "Unknown"
                    }

                    // Pass orientation and sensor values
                    onOrientationDetected(orientation, it.values)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        // Register listener
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BubbleLevelPreview() {
    BubbleLevel(angle = 5f)
}

@Preview(showBackground = true)
@Composable
fun ResponsiveLayoutPreview() {
    ResponsiveLayout()
}
