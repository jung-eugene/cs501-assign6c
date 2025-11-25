package com.example.soundmeter

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.log10

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SoundMeterApp()
        }
    }
}

@Composable
fun SoundMeterApp() {
    // Current dB level (relative, not calibrated)
    var dbLevel by remember { mutableStateOf(0f) }

    // Start AudioRecord and update dbLevel in a coroutine
    LaunchedEffect(Unit) {
        startSoundMeter { db ->
            dbLevel = db
        }
    }

    val threshold = 80f
    val clampedDb = dbLevel.coerceIn(0f, 100f)
    val progress = (clampedDb / 100f).coerceIn(0f, 1f)

    val barColor = when {
        clampedDb < 50f -> Color(0xFF4CAF50) // green
        clampedDb < 80f -> Color(0xFFFFC107) // yellow
        else            -> Color(0xFFF44336) // red
    }

    val bgColor = Color(0xFF101820)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = bgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = "Sound Meter",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = String.format("%.1f dB", clampedDb),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = barColor
            )

            Spacer(Modifier.height(16.dp))

            // Colored meter bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(Color(0xFF37474F))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(barColor)
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = when {
                    clampedDb < 50f -> "Quiet • Safe"
                    clampedDb < threshold -> "Moderate • Be mindful"
                    else -> "Too Loud! • Turn it down"
                },
                fontSize = 18.sp,
                color = Color.White
            )

            if (clampedDb >= threshold) {
                Text(
                    text = "⚠ Noise level exceeds $threshold dB!",
                    fontSize = 16.sp,
                    color = Color(0xFFFFCDD2),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Starts an AudioRecord loop and calls [onLevel] with decibel estimates.
 * This runs on the coroutine started by LaunchedEffect.
 */
suspend fun startSoundMeter(onLevel: (Float) -> Unit) {
    val sampleRate = 8000
    val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
        return
    }

    @SuppressLint("MissingPermission")
    val audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize
    )

    if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
        return
    }

    val buffer = ShortArray(bufferSize)

    audioRecord.startRecording()

    try {
        while (kotlinx.coroutines.currentCoroutineContext().isActive) {
            val read = audioRecord.read(buffer, 0, buffer.size)
            if (read > 0) {
                // Use peak amplitude
                var maxAmp = 0
                for (i in 0 until read) {
                    val v = abs(buffer[i].toInt())
                    if (v > maxAmp) maxAmp = v
                }

                val db = if (maxAmp > 0) {
                    // 16-bit audio max amplitude is 32767
                    (20 * log10(maxAmp / 32767.0)).toFloat() + 90f
                    // +90 to shift into a friendly range (rough, not calibrated)
                } else {
                    0f
                }

                onLevel(db)
            }
        }
    } finally {
        audioRecord.stop()
        audioRecord.release()
    }
}
