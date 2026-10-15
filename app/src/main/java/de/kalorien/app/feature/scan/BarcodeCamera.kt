package de.kalorien.app.feature.scan

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode as MlBarcode
import com.google.mlkit.vision.common.InputImage
import de.kalorien.app.core.domain.Barcode
import java.util.concurrent.Executors

/**
 * Kamera-Vorschau mit Barcode-Erkennung (ML Kit, läuft auf dem Gerät).
 * Meldet einen Code erst, wenn er zweimal hintereinander gleich gelesen wurde und die Prüfziffer stimmt.
 */
@Composable
fun BarcodeCamera(torchOn: Boolean, onCode: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val currentOnCode by rememberUpdatedState(onCode)

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(MlBarcode.FORMAT_EAN_13, MlBarcode.FORMAT_EAN_8, MlBarcode.FORMAT_UPC_A, MlBarcode.FORMAT_UPC_E)
                .build(),
        )
        val mainExecutor = ContextCompat.getMainExecutor(context)
        var last: String? = null
        var delivered = false
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy ->
                    analyze(proxy, scanner) { raw ->
                        val code = Barcode.clean(raw)
                        if (!delivered && Barcode.isValid(code)) {
                            if (code == last) {
                                delivered = true
                                mainExecutor.execute { currentOnCode(code) }
                            }
                            last = code
                        }
                    }
                }
                provider.unbindAll()
                camera = provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            } catch (e: Exception) {
                Log.e("BarcodeCamera", "Kamera konnte nicht gestartet werden", e)
            }
        }, mainExecutor)

        onDispose {
            runCatching { providerFuture.get().unbindAll() }
            executor.shutdown()
            scanner.close()
            camera = null
        }
    }

    LaunchedEffect(camera, torchOn) {
        camera?.let { if (it.cameraInfo.hasFlashUnit()) it.cameraControl.enableTorch(torchOn) }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

@OptIn(ExperimentalGetImage::class)
private fun analyze(proxy: ImageProxy, scanner: BarcodeScanner, onValue: (String) -> Unit) {
    val media = proxy.image
    if (media == null) {
        proxy.close()
        return
    }
    val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { list -> list.firstNotNullOfOrNull { it.rawValue }?.let(onValue) }
        .addOnCompleteListener { proxy.close() }
}
