package com.richardlenin.momonastic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.richardlenin.momonastic.ui.theme.MomonasticTheme
import android.content.Intent
import android.provider.MediaStore

class GesterDetectionTest : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MomonasticTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val configuration = LocalConfiguration.current
                    val screenHeight = configuration.screenHeightDp.dp
                    val halfHeight = screenHeight / 2
                    val points = remember { mutableStateOf(listOf<Pair<Float, Float>>()) }
                    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        val gestureText = remember { mutableStateOf("No gesture detected") }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(halfHeight)
                                .background(Color(0xFFE0E0E0))
                                .pointerInput(Unit) {
                                   detectDragGestures {
                                        change, dragAmount ->
                                             val (x, y) = change.position
                                             points.value = points.value + Pair(x, y)
                                             gestureText.value = "Dragging at: x=$x, y=$y"
                                             change.consume()
                                       openMacroCamera()
                                   }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = gestureText.value, fontSize = 20.sp, color = Color.Black)
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(halfHeight)
                                .background(Color.White)
                        ) {
                            points.value.forEach { point ->
                               // Draw a small circle at each recorded point
                                drawCircle(
                                    color = Color.Blue,
                                    radius = 10f,
                                    center = androidx.compose.ui.geometry.Offset(point.first, point.second)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun openMacroCamera() {
        val cameraManager = getSystemService(CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        var macroCameraId: String? = null
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val minFocusDistance = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
                // Macro cameras typically have very small minimum focus distance (e.g., < 0.05 meters)
                if (minFocusDistance != null && minFocusDistance < 0.05f) {
                    macroCameraId = cameraId
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (macroCameraId != null) {
            // Launch custom CameraActivity with macro camera ID
//            val intent = android.content.Intent(this, CameraActivity::class.java)
//            intent.putExtra("cameraId", macroCameraId)
            startActivity(intent)
        } else {
            // Fallback: open default camera app
            val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            }
        }
    }
}
