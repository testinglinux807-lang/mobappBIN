package com.example.brin.ui.screens.petugas

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.brin.data.BinData
import com.example.brin.data.MockData
import com.example.brin.data.repository.BinRepository
import com.example.brin.ui.theme.GreenLight
import com.example.brin.ui.theme.GreenPrimary
import com.example.brin.ui.theme.GreenSurface
import com.example.brin.ui.theme.StatusNormal
import com.example.brin.ui.theme.StatusNormalBg
import com.example.brin.ui.theme.TextPrimary
import com.example.brin.ui.theme.TextSecondary
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun PetugasScanScreen(onBinClick: (String) -> Unit = {}) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var scannedBinId   by remember { mutableStateOf<String?>(null) }
    var scannedBin     by remember { mutableStateOf<BinData?>(null) }
    var isScanning     by remember { mutableStateOf(true) }
    var mapsUrl        by remember { mutableStateOf<String?>(null) }
    val scope          = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            ScanCameraView(
                modifier = Modifier.fillMaxSize(),
                onBarcode = { raw ->
                    if (!isScanning) return@ScanCameraView
                    isScanning = false
                    // Google Maps or any HTTPS URL → open directly in browser
                    if (raw.startsWith("http://") || raw.startsWith("https://")) {
                        mapsUrl = raw
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(raw)))
                        return@ScanCameraView
                    }
                    // brin:// deep link → load bin detail
                    val uri   = runCatching { Uri.parse(raw) }.getOrNull()
                    val binId = uri?.getQueryParameter("binId")
                    if (binId != null) {
                        scannedBinId = binId
                        scope.launch {
                            BinRepository.getBinById(binId)
                                .onSuccess { scannedBin = it }
                                .onFailure { scannedBin = MockData.getBinById(binId) }
                        }
                    } else {
                        // Unknown format — reset
                        isScanning = true
                    }
                }
            )
            ScanUiOverlay(
                scannedBinId = scannedBinId,
                scannedBin   = scannedBin,
                onNavigate   = { scannedBinId?.let(onBinClick) },
                onReset      = { scannedBinId = null; scannedBin = null; mapsUrl = null; isScanning = true }
            )
        } else {
            NoCameraPermission(onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) })
        }
    }
}

@Composable
private fun ScanCameraView(modifier: Modifier, onBarcode: (String) -> Unit) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor       = remember { Executors.newSingleThreadExecutor() }
    val scanner        = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
        )
    }

    AndroidView(
        modifier = modifier,
        factory  = { ctx ->
            val previewView = PreviewView(ctx)
            val future      = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                val preview  = Preview.Builder().build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { ia ->
                        ia.setAnalyzer(executor) { imageProxy ->
                            processImageProxy(imageProxy, scanner, onBarcode)
                        }
                    }
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onBarcode: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes -> barcodes.firstOrNull()?.rawValue?.let(onBarcode) }
            .addOnCompleteListener { imageProxy.close() }
    } else {
        imageProxy.close()
    }
}

@Composable
private fun ScanUiOverlay(scannedBinId: String?, scannedBin: BinData?, onNavigate: () -> Unit, onReset: () -> Unit) {
    val circleR = 148.dp

    val infinite = rememberInfiniteTransition(label = "scan")
    val scanProgress by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "line"
    )
    val bracketPulse by infinite.animateFloat(
        initialValue = 0.45f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "bracket"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val circleCy = maxHeight * 0.43f

        // Semi-transparent overlay with circular hole cut out
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            val r  = circleR.toPx()
            val cx = size.width / 2f
            val cy = circleCy.toPx()
            drawRect(Color.Black.copy(alpha = 0.66f))
            drawCircle(color = Color.Transparent, radius = r, center = Offset(cx, cy), blendMode = BlendMode.Clear)
        }

        // Scan line + circle ring + corner brackets
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val r  = circleR.toPx()
            val cx = size.width / 2f
            val cy = circleCy.toPx()

            // Outer ring
            drawCircle(
                color  = if (scannedBinId != null) GreenLight else GreenLight.copy(alpha = 0.55f),
                radius = r,
                center = Offset(cx, cy),
                style  = Stroke(width = 1.8.dp.toPx())
            )

            // Animated sweep line (only when scanning)
            if (scannedBinId == null) {
                val lineY = cy - r + 2 * r * scanProgress
                clipPath(Path().apply { addOval(Rect(cx - r, cy - r, cx + r, cy + r)) }) {
                    drawPath(
                        path  = Path().apply { moveTo(cx - r, lineY); lineTo(cx + r, lineY) },
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                GreenPrimary.copy(alpha = 0.7f),
                                GreenLight,
                                GreenPrimary.copy(alpha = 0.7f),
                                Color.Transparent
                            ),
                            startX = cx - r, endX = cx + r
                        ),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
            }

            // L-shaped corner brackets at 45° points of the circle
            val bLen   = 26.dp.toPx()
            val bWidth = 3.dp.toPx()
            val sq     = r * 0.707f
            val bColor = GreenLight.copy(alpha = if (scannedBinId != null) 1f else bracketPulse)

            fun corner(px: Float, py: Float, dx: Float, dy: Float) {
                drawLine(bColor, Offset(px, py), Offset(px + dx * bLen, py), strokeWidth = bWidth, cap = StrokeCap.Round)
                drawLine(bColor, Offset(px, py), Offset(px, py + dy * bLen), strokeWidth = bWidth, cap = StrokeCap.Round)
            }
            corner(cx - sq, cy - sq,  1f,  1f)   // top-left
            corner(cx + sq, cy - sq, -1f,  1f)   // top-right
            corner(cx - sq, cy + sq,  1f, -1f)   // bottom-left
            corner(cx + sq, cy + sq, -1f, -1f)   // bottom-right
        }

        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 52.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SCAN QR BIN", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 2.sp)
            Spacer(Modifier.height(5.dp))
            Text(
                text      = "Arahkan kamera ke QR code tempat sampah",
                fontSize  = 13.sp,
                color     = Color.White.copy(alpha = 0.60f),
                textAlign = TextAlign.Center
            )
        }

        // Result / hint card — follows circle below, with gap
        val cardHPad = (maxWidth - circleR * 2) / 2
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = circleCy + circleR + 24.dp, start = cardHPad, end = cardHPad)
        ) {
            AnimatedContent(
                targetState  = scannedBinId,
                transitionSpec = {
                    (slideInVertically { it / 3 } + fadeIn()) togetherWith fadeOut()
                },
                label = "card"
            ) { binId ->
                if (binId == null) ScanHintCard() else ScanResultCard(binId, scannedBin, onNavigate, onReset)
            }
        }
    }
}

@Composable
private fun ScanHintCard() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.13f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = GreenLight, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Menunggu QR code...", fontSize = 13.sp, color = Color.White.copy(alpha = 0.80f))
        }
    }
}

@Composable
private fun ScanResultCard(binId: String, bin: BinData?, onNavigate: () -> Unit, onReset: () -> Unit) {

    Surface(
        shape         = RoundedCornerShape(20.dp),
        color         = Color.White,
        shadowElevation = 10.dp,
        modifier      = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(GreenSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("QR Terdeteksi", fontSize = 10.sp, color = TextSecondary)
                    Text(bin?.location ?: binId, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = StatusNormalBg) {
                    Text(binId, fontSize = 10.sp, color = StatusNormal, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onReset,
                    shape   = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Scan Lagi", fontSize = 13.sp)
                }
                Button(
                    onClick  = onNavigate,
                    colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape    = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Detail Bin", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun NoCameraPermission(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(18.dp))
        Text("Izin Kamera Diperlukan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Fitur Scan QR membutuhkan akses kamera untuk mendeteksi QR code bin.",
            fontSize = 13.sp, color = Color.White.copy(alpha = 0.60f), textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick  = onRequest,
            colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape    = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Izinkan Kamera", fontWeight = FontWeight.SemiBold)
        }
    }
}
