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
 * Source principale FC Sion : calendrier officiel football.ch / SFL.
 * Le site FC Sion reste en secours. Le prochain coup d'envoi est mémorisé
 * localement puis le moteur d'événements prend le relais hors-ligne.
 */
object FcSionOfficialSchedule {
    private const val URL_FOOTBALL_CH = "https://club.football.ch/fr/club/equipes/team/calendrier-equipe/%26v%3D777833%26t%3D52463"
    private const val URL_FC_SION = "https://www.fcsion.ch/fr"
    private const val MATCH_DURATION_MS = 2L * 60L * 60L * 1000L + 30L * 60L * 1000L
    private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ROOT)
    private val footballDateRegex = Regex("(?:Lu|Ma|Me|Je|Ve|Sa|Di)\\s+(\\d{2}\\.\\d{2}\\.\\d{4})\\s+(\\d{2}:\\d{2})", RegexOption.IGNORE_CASE)
    private val fcSionDateRegex = Regex("(\\d{2}\\.\\d{2}\\.\\d{4})\\s*[-–—]\\s*(\\d{2}:\\d{2})")

    data class SyncResult(val ok: Boolean, val message: String)
    private data class Fixture(val kickoffMs: Long, val label: String)

    fun sync(context: Context): SyncResult {
        val repo = EventRepository.get(context)
        val config = repo.load()
        val event = config.events.firstOrNull { it.id == DefaultEvents.FC_SION_ID }
            ?: return SyncResult(false, "Événement FC Sion absent")
        if (!event.enabled) return SyncResult(false, "FC Sion automatique désactivé")

        val primary = runCatching { fetchFootballCh() }
        val fallback = if (primary.isFailure) runCatching { fetchFcSionHome() } else null
        val fixture = primary.getOrNull() ?: fallback?.getOrNull()
        val source = if (primary.isSuccess) "football.ch / SFL" else "fcsion.ch (secours)"

        if (fixture == null) {
            val details = buildList {
                primary.exceptionOrNull()?.message?.let { add("football.ch: $it") }
                fallback?.exceptionOrNull()?.message?.let { add("fcsion.ch: $it") }
            }.joinToString(" · ").ifBlank { "aucun horaire exploitable" }
            val now = System.currentTimeMillis()
            config.settings.fcSionLastDetectedAt = now
            config.settings.fcSionLastSignal = "Synchronisation FC Sion impossible : $details"
            repo.save(config)
            return SyncResult(false, config.settings.fcSionLastSignal)
        }

        val end = fixture.kickoffMs + MATCH_DURATION_MS
        val now = System.currentTimeMillis()
        config.settings.fcSionActiveSince = fixture.kickoffMs
        config.settings.fcSionActiveUntil = end
        config.settings.fcSionLastDetectedAt = now
        config.settings.fcSionLastSignal = "$source : ${fixture.label}"
        repo.save(config)
        return SyncResult(true, config.settings.fcSionLastSignal)
    }

    private fun fetchFootballCh(): Fixture {
        val text = htmlToText(httpGet(URL_FOOTBALL_CH))
        val zone = ZoneId.of("Europe/Zurich")
        val now = System.currentTimeMillis()
        val matches = footballDateRegex.findAll(text).toList()
        if (matches.isEmpty()) error("aucune date de match trouvée")

        val candidates = matches.mapNotNull { match ->
            val start = match.range.first
            val end = minOf(text.length, start + 520)
            val block = text.substring(start, end)
            if (!block.contains("FC Sion", ignoreCase = true)) return@mapNotNull null
            if (Regex("FC Sion\\s*(?:M-|U-|1ère|2|3)", RegexOption.IGNORE_CASE).containsMatchIn(block)) return@mapNotNull null
            val local = LocalDateTime.parse("${match.groupValues[1]} ${match.groupValues[2]}", formatter)
            val kickoff = local.atZone(zone).toInstant().toEpochMilli()
            val label = block.replace(Regex("\\s+"), " ").take(180).trim()
            Fixture(kickoff, label)
        }.sortedBy { it.kickoffMs }

        val active = candidates.firstOrNull {
            it.kickoffMs <= now && now < it.kickoffMs + MATCH_DURATION_MS
        }
        val future = candidates.firstOrNull { it.kickoffMs > now }
        return active ?: future ?: error("aucun match actuel ou futur")
    }

    private fun fetchFcSionHome(): Fixture {
        val text = htmlToText(httpGet(URL_FC_SION))
        val anchor = text.indexOf("Prochain match", ignoreCase = true)
        if (anchor < 0) error("bloc Prochain match introuvable")
        val block = text.substring(anchor, minOf(text.length, anchor + 1400))
        val match = fcSionDateRegex.find(block) ?: error("date du prochain match introuvable")
        val local = LocalDateTime.parse("${match.groupValues[1]} ${match.groupValues[2]}", formatter)
        val kickoff = local.atZone(ZoneId.of("Europe/Zurich")).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        if (kickoff + MATCH_DURATION_MS <= now) error("match affiché déjà terminé")
        return Fixture(kickoff, "${match.groupValues[1]} à ${match.groupValues[2]}")
    }

    private fun httpGet(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            instanceFollowRedirects = true
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
            setRequestProperty("Accept-Language", "fr-CH,fr;q=0.9,de;q=0.7")
            setRequestProperty("Accept-Encoding", "identity")
            setRequestProperty("Cache-Control", "no-cache")
        }
        return try {
            val code = conn.responseCode
            if (code !in 200..299) error("HTTP $code")
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
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
        .replace("&#39;", "'")
        .replace(Regex("\\s+"), " ")
        .trim()
}
