//package com.richardlenin.momonastic
//
//import android.Manifest
//import android.app.Activity
//import android.content.pm.PackageManager
//import android.graphics.ImageFormat
//import android.graphics.SurfaceTexture
//import android.hardware.camera2.*
//import android.media.ImageReader
//import android.os.Bundle
//import android.os.Environment
//import android.util.Size
//import android.view.Surface
//import android.view.TextureView
//import android.widget.Button
//import android.widget.FrameLayout
//import android.widget.Toast
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import java.io.File
//import java.io.FileOutputStream
//import java.nio.ByteBuffer
//
//class CameraActivity : Activity() {
//    private lateinit var textureView: TextureView
//    private lateinit var captureButton: Button
//    private var cameraDevice: CameraDevice? = null
//    private var cameraCaptureSession: CameraCaptureSession? = null
//    private var imageReader: ImageReader? = null
//    private var cameraId: String? = null
//    private val REQUEST_CAMERA_PERMISSION = 1001
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        cameraId = intent.getStringExtra("cameraId")
//        textureView = TextureView(this)
//        captureButton = Button(this).apply { text = "Capture" }
//        val layout = FrameLayout(this)
//        layout.addView(textureView)
//        layout.addView(captureButton)
//        setContentView(layout)
//
//        captureButton.setOnClickListener {
//            takePicture()
//        }
//
//        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
//            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
//                openCamera()
//            }
//            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
//            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
//                closeCamera()
//                return true
//            }
//            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
//        }
//    }
//
//    private fun openCamera() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
//            return
//        }
//        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
//        try {
//            val id = cameraId ?: manager.cameraIdList[0]
//            val characteristics = manager.getCameraCharacteristics(id)
//            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
//            val previewSize = map?.getOutputSizes(SurfaceTexture::class.java)?.get(0) ?: Size(640, 480)
//            imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 1)
//            imageReader?.setOnImageAvailableListener({ reader ->
//                val image = reader.acquireLatestImage()
//                val buffer: ByteBuffer = image.planes[0].buffer
//                val bytes = ByteArray(buffer.remaining())
//                buffer.get(bytes)
//                saveImage(bytes)
//                image.close()
//            }, null)
//            manager.openCamera(id, object : CameraDevice.StateCallback() {
//                override fun onOpened(camera: CameraDevice) {
//                    cameraDevice = camera
//                    startPreview()
//                }
//                override fun onDisconnected(camera: CameraDevice) {
//                    camera.close()
//                    cameraDevice = null
//                }
//                override fun onError(camera: CameraDevice, error: Int) {
//                    camera.close()
//                    cameraDevice = null
//                    Toast.makeText(this@CameraActivity, "Camera error", Toast.LENGTH_SHORT).show()
//                }
//            }, null)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Toast.makeText(this, "Failed to open camera", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun startPreview() {
//        val texture = textureView.surfaceTexture ?: return
//        val previewSize = imageReader?.width ?: 640
//        texture.setDefaultBufferSize(previewSize, imageReader?.height ?: 480)
//        val surface = Surface(texture)
//        try {
//            cameraDevice?.createCaptureSession(listOf(surface, imageReader?.surface), object : CameraCaptureSession.StateCallback() {
//                override fun onConfigured(session: CameraCaptureSession) {
//                    cameraCaptureSession = session
//                    val previewRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
//                    previewRequestBuilder?.addTarget(surface)
//
//
