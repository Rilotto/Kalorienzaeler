package de.kalorien.app.core

import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import de.kalorien.app.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Fehlerprotokoll: Stürzt die App ab, wird der Fehler (ohne persönliche Daten, nur Technik)
 * in eine Datei im App-Speicher geschrieben. In den Einstellungen lässt er sich teilen oder löschen.
 * Es wird nichts automatisch verschickt.
 */
class CrashLog(private val context: Context) {
    private val file get() = File(context.filesDir, FILE)

    fun exists(): Boolean = file.exists() && file.length() > 0

    /** Zeitpunkt des letzten Absturzes (aus der ersten Zeile des neuesten Blocks). */
    fun lastCrash(): String? = if (!exists()) null else
        file.readLines().lastOrNull { it.startsWith(HEADER) }?.removePrefix(HEADER)?.trim()

    fun count(): Int = if (!exists()) 0 else file.readLines().count { it.startsWith(HEADER) }

    fun clear() { file.delete() }

    /** Kopie im Cache, die per „Teilen“ weitergegeben werden kann. */
    fun shareUri(): Uri {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val out = File(dir, "kalorienzaehler-fehlerprotokoll.txt")
        out.writeText(deviceInfo() + "\n\n" + (if (exists()) file.readText() else ""))
        return FileProvider.getUriForFile(context, "${context.packageName}.updates", out)
    }

    private fun write(thread: Thread, e: Throwable) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        val block = "$HEADER ${LocalDateTime.now().format(STAMP)}\nThread: ${thread.name}\n$sw\n"
        val old = if (file.exists()) file.readText() else ""
        // nur die letzten Abstürze behalten (Datei bleibt klein)
        file.writeText((old + block).takeLast(MAX_CHARS))
    }

    companion object {
        private const val FILE = "crash.log"
        private const val HEADER = "=== Absturz"
        private const val MAX_CHARS = 60_000
        private val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")

        fun deviceInfo(): String =
            "Kalorienzähler ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n" +
                "Gerät: ${Build.MANUFACTURER} ${Build.MODEL}\nAndroid ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        /** Beim App-Start einhängen. Der normale Absturz-Ablauf von Android läuft danach weiter. */
        fun install(context: Context) {
            val log = CrashLog(context.applicationContext)
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { t, e ->
                runCatching { log.write(t, e) }
                previous?.uncaughtException(t, e)
            }
        }
    }
}
