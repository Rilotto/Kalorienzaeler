package de.kalorien.app.feature.scan

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.mlkit.vision.common.InputImage
import de.kalorien.app.core.domain.OcrLayout
import de.kalorien.app.core.domain.OcrLine
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Texterkennung auf dem Gerät (ML Kit, lateinische Schrift, offline). */
object TextFromImage {
    /**
     * Liest den Text und gibt zwei Lesarten zurück:
     * 1. Tabellenzeilen nach Position neu zusammengesetzt (Beschriftung und Zahlen auf gleicher Höhe → eine Zeile),
     * 2. der Text, wie ML Kit ihn liefert.
     * Der [de.kalorien.app.core.domain.NutritionTextParser] nimmt die Lesart mit den meisten Werten.
     */
    suspend fun read(context: Context, uri: Uri): List<String> {
        val image = withContext(Dispatchers.IO) { loadScaled(context, uri) }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val result = try {
            suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resumeWithException(it) }
            }
        } finally {
            recognizer.close()
        }
        val lines = result.textBlocks.flatMap { it.lines }.mapNotNull { l ->
            l.boundingBox?.let { b -> OcrLine(l.text, b.left, b.top, b.right, b.bottom) }
        }
        return listOf(OcrLayout.rows(lines), result.text)
    }

    /**
     * Lädt das Bild verkleinert (längste Seite ≤ ca. 2400 px) und mit richtiger Drehung.
     * Volle 12–48-MP-Fotos würden sonst unnötig viel Speicher brauchen; für Text reicht das locker.
     */
    private fun loadScaled(context: Context, uri: Uri): InputImage {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= 1200) sample *= 2
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IllegalStateException("Bild konnte nicht gelesen werden")
        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { s ->
                when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        }.getOrNull() ?: 0
        return InputImage.fromBitmap(bitmap, rotation)
    }

    /** Ziel-Datei für ein Kamerafoto (wird bei jedem Foto überschrieben, liegt im Cache). */
    fun newPhotoUri(context: Context): Uri {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "naehrwerte.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
    }
}
