package de.kalorien.app.feature.scan

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Texterkennung auf dem Gerät (ML Kit, lateinische Schrift, offline). */
object TextFromImage {
    suspend fun read(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it.text) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        } finally {
            recognizer.close()
        }
    }

    /** Ziel-Datei für ein Kamerafoto (wird bei jedem Foto überschrieben, liegt im Cache). */
    fun newPhotoUri(context: Context): Uri {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "naehrwerte.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
    }
}
