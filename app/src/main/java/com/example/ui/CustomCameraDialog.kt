package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.os.Build
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.theme.FocusPink
import com.example.ui.theme.SecondaryPink
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.TextCharcoal
import com.example.ui.theme.TextGray
import java.io.File
import java.io.FileOutputStream
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCancellableCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { cameraProviderFuture ->
        cameraProviderFuture.addListener({
            continuation.resume(cameraProviderFuture.get())
        }, ContextCompat.getMainExecutor(this))
    }
}

@Composable
fun CustomCameraDialog(
    taskTitle: String,
    onDismiss: () -> Unit,
    onCapture: (String, String) -> Unit // returns (imageUri, completedAt)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Detect if we are running on an emulator or if the device has no camera hardware
    val hasCameraHardware = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    val defaultSimulationMode = remember {
        val brand = (Build.BRAND ?: "").lowercase()
        val device = (Build.DEVICE ?: "").lowercase()
        val fingerprint = (Build.FINGERPRINT ?: "").lowercase()
        val hardware = (Build.HARDWARE ?: "").lowercase()
        val model = (Build.MODEL ?: "").lowercase()
        val product = (Build.PRODUCT ?: "").lowercase()
        val manufacturer = (Build.MANUFACTURER ?: "").lowercase()

        !hasCameraHardware || 
        brand.startsWith("generic") || 
        brand.contains("google") && device.startsWith("generic") ||
        device.startsWith("generic") || 
        fingerprint.startsWith("generic") || 
        fingerprint.startsWith("unknown") || 
        fingerprint.contains("generic") ||
        hardware.contains("goldfish") || 
        hardware.contains("ranchu") || 
        model.contains("google_sdk") || 
        model.contains("emulator") || 
        model.contains("sdk") ||
        model.contains("android sdk built for x86") || 
        manufacturer.contains("genymotion") || 
        product.contains("sdk_google") || 
        product.contains("google_sdk") || 
        product.contains("sdk") || 
        product.contains("sdk_x86") || 
        product.contains("vbox86p") || 
        product.contains("emulator") || 
        product.contains("simulator") ||
        product.contains("gphone")
    }

    // Permissions State: if in fallback/simulation mode by default, we don't require camera permission
    var hasCameraPermission by remember {
        mutableStateOf(
            if (defaultSimulationMode) {
                true
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Trigger permission requests if not granted and not defaulting to simulation mode
    LaunchedEffect(Unit) {
        if (!defaultSimulationMode && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera Configuration States
    var isFrontCamera by remember { mutableStateOf(false) }
    var isCameraReady by remember { mutableStateOf(false) }
    var isSimulationMode by remember { mutableStateOf(defaultSimulationMode) }
    
    // Core CameraX structures
    val preview = remember { Preview.Builder().build() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // LaunchedEffect to bind Camera to Lifecycle cleanly
    LaunchedEffect(isFrontCamera, hasCameraPermission, isSimulationMode) {
        if (hasCameraPermission && !isSimulationMode) {
            try {
                // Safeguard against CameraX future hangs in virtual/web emulator environments
                val cameraProvider = kotlinx.coroutines.withTimeoutOrNull(1500) {
                    context.getCameraProvider()
                }
                if (cameraProvider != null) {
                    val cameraSelector = if (isFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    isCameraReady = true
                } else {
                    Log.w("CameraDialog", "Loading camera provider timed out. Switching to simulation mode.")
                    isSimulationMode = true
                }
            } catch (e: Throwable) {
                Log.e("CameraDialog", "Binding failed. Switching to mockup simulator.", e)
                isSimulationMode = true
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .windowInsetsPadding(WindowInsets.statusBars),
            contentAlignment = Alignment.Center
        ) {
            // Dismiss Button Top-Right
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .testTag("camera_close_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hủy bỏ chụp ảnh",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Header with target task
                Text(
                    text = "CHECK-IN HOÀN THÀNH ✨",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Nhiệm vụ: $taskTitle",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryPink,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Square Locket Viewfinder (1:1 ratio) with generous rounded corners
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(32.dp))
                        .border(3.dp, Color.White, RoundedCornerShape(32.dp))
                        .background(Color(0xFF222222))
                        .testTag("camera_viewfinder"),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission && !isSimulationMode) {
                        // Real CameraX preview container
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = { previewView ->
                                preview.setSurfaceProvider(previewView.surfaceProvider)
                            }
                        )
                    } else {
                        // Simulated / Cute Mock Up Camera Preview for Emulator Support
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFFFECF6),
                                            Color(0xFFE8D7FF)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                // Pulsing Cute Cow
                                val infiniteTransition = rememberInfiniteTransition(label = "sim_pulse")
                                val simScale by infiniteTransition.animateFloat(
                                    initialValue = 0.95f,
                                    targetValue = 1.05f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "simScale"
                                )

                                androidx.compose.foundation.Image(
                                    painter = painterResource(id = R.drawable.img_mascot_cow),
                                    contentDescription = "Simulated Viewfinder",
                                    modifier = Modifier
                                        .size(110.dp)
                                        .scale(simScale)
                                        .clip(RoundedCornerShape(16.dp))
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "📸 GƯƠNG THẦN BÒ BÉO",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextCharcoal
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (!hasCameraPermission) {
                                        "Chưa cấp quyền Camera. Hệ thống sẽ mô phỏng ảnh selfie cực dễ thương của cậu nhé! 💕"
                                    } else {
                                        "Đang kết nối camera thần kỳ... Bấm nút chụp để tạo ảnh check-in xinh đẹp! 🌸"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray,
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // Floating Time Tag Preview
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        val formatTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
                        Text(
                            text = "✨ Done lúc $formatTime",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Actions panel - Flip + Shutter + Simulated toggle
                Row(
                    modifier = Modifier.fillMaxWidth(0.95f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left element: Flip Camera (or placeholder if simulated)
                    IconButton(
                        onClick = {
                            if (!isSimulationMode && hasCameraPermission) {
                                isFrontCamera = !isFrontCamera
                            } else {
                                isSimulationMode = !isSimulationMode
                            }
                        },
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cached,
                            contentDescription = "Chuyển Camera hoặc Mô Phỏng",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Shutter Clicker
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable {
                                handleCapture(
                                    context = context,
                                    imageCapture = imageCapture,
                                    isSimulation = isSimulationMode || !hasCameraPermission,
                                    isFront = isFrontCamera,
                                    cameraExecutor = cameraExecutor,
                                    onCaptured = { savedUri, timeText ->
                                        onCapture(savedUri, timeText)
                                    }
                                )
                            }
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(3.dp, Color.Black, CircleShape)
                                .background(Brush.linearGradient(listOf(FocusPink, SecondaryPink)))
                        )
                    }

                    // Dummy matching item for aesthetic symmetry (or toggle simulated)
                    val label = if (isSimulationMode) "Real" else "Mock"
                    Button(
                        onClick = { isSimulationMode = !isSimulationMode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        ),
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .size(54.dp)
                            .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Logic to core-capturing & processing
private fun handleCapture(
    context: Context,
    imageCapture: ImageCapture,
    isSimulation: Boolean,
    isFront: Boolean,
    cameraExecutor: Executor,
    onCaptured: (String, String) -> Unit
) {
    val now = LocalTime.now()
    val formattedTime = now.format(DateTimeFormatter.ofPattern("HH:mm"))
    
    val outputDir = context.cacheDir
    val outputFile = File(outputDir, "checkin_${System.currentTimeMillis()}.jpg")

    if (isSimulation) {
        // Generate a cute pastel virtual card & save it as checking image representation
        try {
            val width = 600
            val height = 600
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = AndroidCanvas(bitmap)
            
            // Draw gradient sweet background
            val paint = Paint().apply { isAntiAlias = true }
            
            // Draw a cute background with nice colors
            canvas.drawColor(android.graphics.Color.parseColor("#FFE8D7")) // Pastel orange-peach color
            
            // Try to draw img_mascot_cow on the center of the simulated bitmap
            try {
                val mascotBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.img_mascot_cow)
                if (mascotBitmap != null) {
                    val scaledMascot = Bitmap.createScaledBitmap(mascotBitmap, 350, 350, true)
                    val left = (width - scaledMascot.width) / 2f
                    val top = (height - scaledMascot.height) / 2.2f
                    canvas.drawBitmap(scaledMascot, left, top, paint)
                }
            } catch (t: Throwable) {
                // Secondary fallback color circle
                paint.color = android.graphics.Color.parseColor("#FFCCF9")
                canvas.drawCircle(width/2f, height/2.2f, 150f, paint)
            }
            
            // Draw cute photo text border and labels
            paint.color = android.graphics.Color.parseColor("#444444")
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 28f
            paint.isFakeBoldText = true
            canvas.drawText("💖 CHEERS! CẬU ĐÃ RA TRẬN XUẤT SẮC 💖", width / 2f, height - 120f, paint)
            
            paint.textSize = 22f
            paint.isFakeBoldText = false
            canvas.drawText("Bò Béo chứng nhận - Hoàn thành lúc: $formattedTime", width / 2f, height - 75f, paint)

            // Compress to 70% quality and save to file
            val outputStream = FileOutputStream(outputFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            outputStream.flush()
            outputStream.close()
            
            onCaptured(Uri.fromFile(outputFile).toString(), formattedTime)
        } catch (e: Exception) {
            Log.e("CameraDialog", "Failed to generate mock capture image", e)
        }
    } else {
        // Core CameraX Image capturing options
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // Post-process image to correct orientation & compress to 70%
                    try {
                        val savedPath = outputFile.absolutePath
                        val originalBitmap = BitmapFactory.decodeFile(savedPath)
                        
                        // Rotates nicely if front camera or specific portrait EXIFs
                        val matrix = Matrix().apply {
                            if (isFront) {
                                postScale(-1f, 1f) // Mirror horizontal
                            }
                        }
                        
                        val rotatedBitmap = Bitmap.createBitmap(
                            originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true
                        )

                        // Write back compressed
                        val outputStream = FileOutputStream(outputFile)
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
                        outputStream.flush()
                        outputStream.close()
                        
                        ContextCompat.getMainExecutor(context).execute {
                            onCaptured(Uri.fromFile(outputFile).toString(), formattedTime)
                        }
                    } catch (e: Exception) {
                        Log.e("CameraDialog", "Post processing raw image failed", e)
                        // Fallback to basic Uri return
                        ContextCompat.getMainExecutor(context).execute {
                            onCaptured(Uri.fromFile(outputFile).toString(), formattedTime)
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraDialog", "takePicture error. Falling back to simulation.", exception)
                    // Auto retry with Simulation
                    ContextCompat.getMainExecutor(context).execute {
                        handleCapture(context, imageCapture, true, isFront, cameraExecutor, onCaptured)
                    }
                }
            }
        )
    }
}
