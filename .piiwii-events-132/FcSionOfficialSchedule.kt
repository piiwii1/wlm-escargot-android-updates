package ch.piiwii.events.service

import android.content.Context
import ch.piiwii.events.data.DefaultEvents
import ch.piiwii.events.data.EventRepository
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Source principale FC Sion : calendrier publié sur le site officiel du club.
 * Une synchronisation suffit à programmer l'intervalle du prochain match ;
 * le moteur d'événements et AlarmManager prennent ensuite le relais hors-ligne.
 */
object FcSionOfficialSchedule {
    private const val URL_OFFICIAL = "https://www.fcsion.ch/fr"
    private const val MATCH_DURATION_MS = 2L * 60L * 60L * 1000L + 30L * 60L * 1000L
    private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ROOT)
    private val dateRegex = Regex("(\\d{2}\\.\\d{2}\\.\\d{4})\\s*-\\s*(\\d{2}:\\d{2})")

    data class SyncResult(val ok: Boolean, val message: String)

    fun sync(context: Context): SyncResult {
        val repo = EventRepository.get(context)
        val config = repo.load()
        val event = config.events.firstOrNull { it.id == DefaultEvents.FC_SION_ID }
            ?: return SyncResult(false, "Événement FC Sion absent")
        if (!event.enabled) return SyncResult(false, "FC Sion automatique désactivé")

        return runCatching {
            val conn = (URL(URL_OFFICIAL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 7000
                readTimeout = 7000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) PiiWii-Evenements/1.3.2")
                setRequestProperty("Accept-Language", "fr-CH,fr;q=0.9")
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) error("HTTP $code")
                val html = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val text = htmlToText(html)
                val anchor = text.indexOf("Prochain match", ignoreCase = true)
                if (anchor < 0) error("bloc Prochain match introuvable")
                val block = text.substring(anchor, minOf(text.length, anchor + 1200))
                val match = dateRegex.find(block) ?: error("date du prochain match introuvable")
                val local = LocalDateTime.parse("${match.groupValues[1]} ${match.groupValues[2]}", formatter)
                val zone = ZoneId.of("Europe/Zurich")
                val kickoff = local.atZone(zone).toInstant().toEpochMilli()
                val end = kickoff + MATCH_DURATION_MS
                val now = System.currentTimeMillis()
                if (end + 30L * 60L * 1000L < now) error("horaire officiel déjà dépassé")

                config.settings.fcSionActiveSince = kickoff
                config.settings.fcSionActiveUntil = end
                config.settings.fcSionLastDetectedAt = now
                config.settings.fcSionLastSignal = "Calendrier officiel FC Sion : ${match.groupValues[1]} à ${match.groupValues[2]}"
                repo.save(config)
                SyncResult(true, config.settings.fcSionLastSignal)
            } finally {
                conn.disconnect()
            }
        }.getOrElse { error ->
            val now = System.currentTimeMillis()
            config.settings.fcSionLastDetectedAt = now
            config.settings.fcSionLastSignal = "Calendrier officiel indisponible : ${error.message ?: error.javaClass.simpleName}"
            repo.save(config)
            SyncResult(false, config.settings.fcSionLastSignal)
        }
    }

    private fun htmlToText(html: String): String = html
        .replace(Regex("(?is)<script.*?</script>"), " ")
        .replace(Regex("(?is)<style.*?</style>"), " ")
        .replace(Regex("(?s)<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&ndash;", "-")
        .replace("&mdash;", "-")
        .replace(Regex("\\s+"), " ")
        .trim()
}
