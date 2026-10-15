package ch.piiwii.streamradar.data

import ch.piiwii.streamradar.Release
import java.time.LocalDate
import java.util.Locale

data class RecommendationItem(
    val release: Release,
    val score: Double,
    val reason: String
)

/**
 * Moteur local et déterministe de recommandations StreamRadar.
 * Aucun historique n'est envoyé sur Internet : tout est calculé depuis les interactions DataStore.
 */
object RecommendationEngine {
    fun recommend(releases: List<Release>, prefs: UserPreferences, limit: Int = 12): List<RecommendationItem> {
        if (releases.isEmpty()) return emptyList()
        val byId = releases.associateBy { it.id }
        val seedIds = (prefs.followedIds + prefs.interestedIds + prefs.viewedIds).filter { it in byId }
        val seeds = seedIds.mapNotNull(byId::get)

        val genreScores = mutableMapOf<String, Double>()
        val platformScores = mutableMapOf<String, Double>()
        val typeScores = mutableMapOf<String, Double>()

        seeds.forEach { release ->
            val weight = when (release.id) {
                in prefs.interestedIds -> 4.0
                in prefs.followedIds -> 3.4
                else -> 1.15
            }
            release.genres.forEach { genre ->
                val key = normalize(genre)
                if (key.isNotBlank()) genreScores[key] = (genreScores[key] ?: 0.0) + weight
            }
            normalize(release.platform).takeIf { it.isNotBlank() }?.let { key ->
                platformScores[key] = (platformScores[key] ?: 0.0) + weight * 0.75
            }
            val type = normalize(release.typeLabel())
            typeScores[type] = (typeScores[type] ?: 0.0) + weight * 0.55
        }

        val today = LocalDate.now()
        val coldStart = seeds.size < 3
        val candidates = releases.asSequence()
            .filter { it.id !in prefs.dismissedIds }
            .filter { it.id !in prefs.interestedIds }
            .filter { it.id !in prefs.followedIds }
            .map { release ->
                var score = 0.0
                var bestReason = "Sélection StreamRadar"
                var bestReasonScore = 0.0

                val matchingGenre = release.genres
                    .map { it to (genreScores[normalize(it)] ?: 0.0) }
                    .maxByOrNull { it.second }
                if (matchingGenre != null && matchingGenre.second > 0.0) {
                    val bonus = matchingGenre.second * 2.25
                    score += bonus
                    if (bonus > bestReasonScore) {
                        bestReasonScore = bonus
                        bestReason = "Parce que vous aimez ${matchingGenre.first.lowercase(Locale.FRENCH)}"
                    }
                }

                val platformScore = platformScores[normalize(release.platform)] ?: 0.0
                if (platformScore > 0.0) {
                    val bonus = platformScore * 1.35
                    score += bonus
                    if (bonus > bestReasonScore) {
                        bestReasonScore = bonus
                        bestReason = "Vous regardez souvent ${release.platform}"
                    }
                }

                val typeScore = typeScores[normalize(release.typeLabel())] ?: 0.0
                if (typeScore > 0.0) score += typeScore * 0.75

                release.voteAverage?.let { score += (it.coerceIn(0.0, 10.0) / 10.0) * if (coldStart) 7.0 else 3.0 }
                release.releaseDate?.let { raw ->
                    runCatching { LocalDate.parse(raw) }.getOrNull()?.let { date ->
                        val days = java.time.temporal.ChronoUnit.DAYS.between(today, date)
                        if (days in 0..31) score += if (coldStart) 5.0 else 2.2
                    }
                }
                if (!release.posterUrl.isNullOrBlank()) score += 0.7
                if (release.id in prefs.viewedIds) score -= 3.0
                RecommendationItem(release, score, bestReason)
            }
            .sortedByDescending { it.score }
            .toList()

        // Diversité : éviter une longue série de recommandations de la même plateforme / même genre.
        val selected = mutableListOf<RecommendationItem>()
        val platformCounts = mutableMapOf<String, Int>()
        val genreCounts = mutableMapOf<String, Int>()
        for (item in candidates) {
            if (selected.size >= limit) break
            val platform = normalize(item.release.platform)
            val mainGenre = item.release.genres.firstOrNull()?.let(::normalize).orEmpty()
            val platformCount = platformCounts[platform] ?: 0
            val genreCount = if (mainGenre.isBlank()) 0 else genreCounts[mainGenre] ?: 0
            val strict = selected.size < (limit * 2 / 3)
            if (strict && (platformCount >= 4 || genreCount >= 5)) continue
            selected += item
            platformCounts[platform] = platformCount + 1
            if (mainGenre.isNotBlank()) genreCounts[mainGenre] = genreCount + 1
        }

        // Catalogue très petit / filtres sévères : compléter sans doublon.
        if (selected.size < limit) {
            val used = selected.mapTo(mutableSetOf()) { it.release.id }
            candidates.forEach { if (selected.size < limit && used.add(it.release.id)) selected += it }
        }
        return selected
    }

    fun topPreferences(releases: List<Release>, prefs: UserPreferences): Triple<List<String>, List<String>, String> {
        val byId = releases.associateBy { it.id }
        val seedIds = (prefs.followedIds + prefs.interestedIds + prefs.viewedIds).filter { it in byId }
        val genres = mutableMapOf<String, Int>()
        val platforms = mutableMapOf<String, Int>()
        val types = mutableMapOf<String, Int>()
        seedIds.mapNotNull(byId::get).forEach { release ->
            release.genres.forEach { genres[it] = (genres[it] ?: 0) + 1 }
            platforms[release.platform] = (platforms[release.platform] ?: 0) + 1
            types[release.typeLabel()] = (types[release.typeLabel()] ?: 0) + 1
        }
        val topGenres = genres.entries.sortedByDescending { it.value }.take(3).map { it.key }
        val topPlatforms = platforms.entries.sortedByDescending { it.value }.take(2).map { it.key }
        val type = types.maxByOrNull { it.value }?.key ?: "Pas encore assez de données"
        return Triple(topGenres, topPlatforms, type)
    }

    private fun normalize(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9à-ÿ]+"), " ")
        .trim()
}
