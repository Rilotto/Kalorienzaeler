package de.kalorien.app.core.update

import android.content.Context
import de.kalorien.app.BuildConfig
import de.kalorien.app.core.domain.AppVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Eine veröffentlichte Version auf GitHub. */
data class ReleaseInfo(
    val tag: String,
    val version: AppVersion,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
)

/**
 * Fragt die GitHub-Releases dieses Repositorys ab und lädt die APK herunter.
 * Einzige Netzwerkverbindung neben Open Food Facts; läuft nur, wenn der Nutzer den Knopf drückt.
 */
class UpdateRepository(private val context: Context) {

    /** Neueste Version, die neuer ist als die installierte – oder null, wenn alles aktuell ist. */
    suspend fun findUpdate(includePreReleases: Boolean): ReleaseInfo? = withContext(Dispatchers.IO) {
        val current = AppVersion.parse(BuildConfig.VERSION_NAME) ?: AppVersion(listOf(0), "dev")
        val json = get("https://api.github.com/repos/$REPO/releases?per_page=15")
        val list = JSONArray(json)
        val releases = (0 until list.length()).mapNotNull { i ->
            val r = list.getJSONObject(i)
            if (r.optBoolean("draft")) return@mapNotNull null
            if (r.optBoolean("prerelease") && !includePreReleases) return@mapNotNull null
            val tag = r.optString("tag_name")
            val version = AppVersion.parse(tag) ?: return@mapNotNull null
            val assets = r.optJSONArray("assets") ?: return@mapNotNull null
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk") } ?: return@mapNotNull null
            ReleaseInfo(tag, version, r.optString("body"), apk.optString("browser_download_url"), apk.optLong("size"))
        }
        releases.maxByOrNull { it.version }?.takeIf { it.version > current }
    }

    /** Lädt die APK in den Cache. [onProgress] bekommt 0..1. */
    suspend fun download(release: ReleaseInfo, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // alte Downloads aufräumen
        val target = File(dir, "kalorienzaehler-${release.tag}.apk")
        val conn = open(release.apkUrl)
        try {
            val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
            conn.inputStream.use { input ->
                target.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        if (release.apkSize > 0 && target.length() != release.apkSize) {
            target.delete()
            throw IOException("Download unvollständig")
        }
        target
    }

    private fun get(url: String): String {
        val conn = open(url)
        try {
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "Kalorienzaehler-App/${BuildConfig.VERSION_NAME}")
        conn.setRequestProperty("Accept", "application/vnd.github+json, application/octet-stream")
        val code = conn.responseCode
        if (code !in 200..299) {
            conn.disconnect()
            throw IOException(if (code == 404) "Repository nicht erreichbar (privat?)" else "Server antwortet mit $code")
        }
        return conn
    }

    companion object {
        const val REPO = "Rilotto/Kalorienzaeler"
    }
}
