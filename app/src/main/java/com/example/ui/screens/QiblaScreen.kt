package com.example.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QiblaScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentLocation by viewModel.currentLocation.collectAsState()
    val qiblaBearing = remember(currentLocation.lat, currentLocation.lng) {
        viewModel.getQiblaBearing(currentLocation.lat, currentLocation.lng)
    }
    val locationDisplayName = currentLocation.widgetDisplayName()

    // Magnetic Sensor State
    var azimuth by remember { mutableStateOf(0f) }
    var lastAccelerometer = remember { FloatArray(3) }
    var lastMagnetometer = remember { FloatArray(3) }
    var hasAccelerometer by remember { mutableStateOf(false) }
    var hasMagnetometer by remember { mutableStateOf(false) }

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    val sensorListener = remember {
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
                    hasAccelerometer = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
                    hasMagnetometer = true
                }

                if (hasAccelerometer && hasMagnetometer) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, lastAccelerometer, lastMagnetometer)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        // orientation[0] is azimuth in radians (magnetic north)
                        val azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        // Convert to 0-360
                        azimuth = (azimuthDeg + 360f) % 360f
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
    }

    DisposableEffect(Unit) {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (accelerometer != null) {
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
        if (magnetometer != null) {
            sensorManager.registerListener(sensorListener, magnetometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(sensorListener)
        }
    }

    // Relative angle of the Kaaba from the current phone direction: (Qibla Angle - Azimuth Angle)
    val relativeAngle = (qiblaBearing.toFloat() - azimuth + 360f) % 360f

    // Track continuous unwrapped angles so 359° -> 1° rotates smoothly by +2° instead of -358°
    var continuousRelativeAngle by remember { mutableStateOf(relativeAngle) }
    LaunchedEffect(relativeAngle) {
        val currentMod = ((continuousRelativeAngle % 360f) + 360f) % 360f
        var delta = relativeAngle - currentMod
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        continuousRelativeAngle += delta
    }

    var continuousAzimuth by remember { mutableStateOf(-azimuth) }
    LaunchedEffect(azimuth) {
        val target = -azimuth
        val currentMod = ((continuousAzimuth % 360f) + 360f) % 360f
        val targetMod = ((target % 360f) + 360f) % 360f
        var delta = targetMod - currentMod
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        continuousAzimuth += delta
    }

    // Whether the device is accurately pointing to the Qibla (tolerance of 3 degrees)
    val isAligned = abs(relativeAngle) < 3f || abs(relativeAngle - 360f) < 3f

    // Smooth animated compass & Qibla arrow angles
    val animatedRelativeAngle by animateFloatAsState(targetValue = continuousRelativeAngle, label = "QiblaArrowAnim")
    val animatedCompassRotation by animateFloatAsState(targetValue = continuousAzimuth, label = "CompassHousingAnim")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Arah Kiblat", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Location Details
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Explore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = locationDisplayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Sudut Kiblat: ${String.format("%.1f", qiblaBearing)}° dari Utara",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Beautiful Rotating Compass Canvas
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .testTag("qibla_compass_dial"),
                contentAlignment = Alignment.Center
            ) {
                // Background ticks
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2 - 12.dp.toPx()
                    
                    // Draw outer circle rim
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        radius = radius + 6.dp.toPx(),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Draw ticks every 10 degrees
                    for (angle in 0 until 360 step 10) {
                        val angleRad = Math.toRadians(angle.toDouble())
                        val tickLength = if (angle % 30 == 0) 12.dp.toPx() else 6.dp.toPx()
                        val strokeWidth = if (angle % 30 == 0) 2.dp.toPx() else 1.dp.toPx()
                        
                        val startX = center.x + (radius - tickLength) * sin(angleRad).toFloat()
                        val startY = center.y - (radius - tickLength) * cos(angleRad).toFloat()
                        
                        val endX = center.x + radius * sin(angleRad).toFloat()
                        val endY = center.y - radius * cos(angleRad).toFloat()
                        
                        drawLine(
                            color = if (angle == 0) Color.Red else Color.Gray.copy(alpha = 0.6f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = strokeWidth
                        )
                    }
                }

                // Rotated compass housing (based on physical azimuth device rotation)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(animatedCompassRotation)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Compass directions: U (N), S, T (E), B (W)
                    Text(
                        text = "U",
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                    )
                    Text(
                        text = "S",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                    )
                    Text(
                        text = "T",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp)
                    )
                    Text(
                        text = "B",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 10.dp)
                    )
                }

                // Rotating Qibla pointing indicator arrow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(animatedRelativeAngle),
                    contentAlignment = Alignment.Center
                ) {
                    // Kaaba symbol marker + gold needle
                    Canvas(modifier = Modifier.fillMaxSize().padding(28.dp)) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val needleLength = size.width / 2 - 20.dp.toPx()

                        // Draw golden compass arrow pointing directly to Qibla
                        val path = Path().apply {
                            moveTo(center.x, center.y - needleLength)
                            lineTo(center.x - 8.dp.toPx(), center.y)
                            lineTo(center.x, center.y + 12.dp.toPx())
                            lineTo(center.x + 8.dp.toPx(), center.y)
                            close()
                        }
                        
                        drawPath(
                            path = path,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFBBF24), Color(0xFFD97706)) // Gold needle gradient
                            )
                        )

                        // Draw Kaaba representation box at the tip of Qibla arrow
                        val kaabaSize = 28.dp.toPx()
                        val kaabaX = center.x - kaabaSize / 2
                        val kaabaY = center.y - needleLength - 14.dp.toPx()

                        // Base Kaaba Cube (Black)
                        drawRect(
                            color = Color(0xFF1E293B),
                            topLeft = Offset(kaabaX, kaabaY),
                            size = Size(kaabaSize, kaabaSize)
                        )
                        // Kiswah gold belt (Middle-top strip)
                        drawRect(
                            color = Color(0xFFFBBF24),
                            topLeft = Offset(kaabaX, kaabaY + kaabaSize / 4),
                            size = Size(kaabaSize, kaabaSize / 8)
                        )
                    }
                }

                // Centered glowing anchor dot
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            if (isAligned) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.tertiary
                        )
                )
            }

            // Real-Time Directions Guidance Panel
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Large numeric bearing reading
                Text(
                    text = "${Math.round(relativeAngle)}°",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isAligned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                )

                // Aligned status vs adjustment hint
                AnimatedVisibility(
                    visible = isAligned,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 24.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tepat! Anda Menghadap Kiblat",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                if (!isAligned) {
                    val turnInstruction = if (relativeAngle > 180) {
                        "Putar ke Kiri ${Math.round(360f - relativeAngle)}°"
                    } else {
                        "Putar ke Kanan ${Math.round(relativeAngle)}°"
                    }
                    Text(
                        text = turnInstruction,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }

                // Calibration sensor helper notice
                Row(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Tips: Gerakkan ponsel membentuk angka 8 untuk kalibrasi kompas",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
