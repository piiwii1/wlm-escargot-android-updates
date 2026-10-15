package ch.piiwii.fannyarchive.importer

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import ch.piiwii.fannyarchive.data.AppDatabase
import ch.piiwii.fannyarchive.data.ContactEntity
import ch.piiwii.fannyarchive.data.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.zip.ZipInputStream

class WhatsAppZipImporter(
    private val resolver: ContentResolver,
    private val db: AppDatabase
) {
    data class Result(
        val imported: Int,
        val mediaOmitted: Int,
        val multiline: Int,
        val vcfFiles: Int,
        val sha256: String,
        val annotationsPreserved: Boolean
    )

    private val startRegexes = listOf(
        Regex("""^(?:\[)?(\d{1,2})\.(\d{1,2})\.(\d{2,4}),[\s\u00A0\u202F]+(\d{1,2}):(\d{2})(?::\d{2})?(?:\])?[\s\u00A0\u202F]*[-–—][\s\u00A0\u202F]*([^:]+):[\s\u00A0\u202F]?(.*)$"""),
        Regex("""^(?:\[)?(\d{1,2})/(\d{1,2})/(\d{2,4}),?[\s\u00A0\u202F]+(\d{1,2}):(\d{2})(?::\d{2})?(?:\])?[\s\u00A0\u202F]*[-–—][\s\u00A0\u202F]*([^:]+):[\s\u00A0\u202F]?(.*)$"""),
        Regex("""^\[(\d{1,2})\.(\d{1,2})\.(\d{2,4}),[\s\u00A0\u202F]+(\d{1,2}):(\d{2})(?::\d{2})?\][\s\u00A0\u202F]*([^:]+):[\s\u00A0\u202F]?(.*)$""")
    )

    private fun matchStartLine(raw: String): MatchResult? {
        val value = raw.trimStart('\uFEFF', '\u200E', '\u200F')
        return startRegexes.firstNotNullOfOrNull { it.matchEntire(value) }
    }

    suspend fun importZip(
        uri: Uri,
        previousSha256: String = "",
        onProgress: (Int) -> Unit = {}
    ): Result = withContext(Dispatchers.IO) {
        val sha256 = sha256(uri)
        val preserveAnnotations = previousSha256.isBlank() || previousSha256.equals(sha256, ignoreCase = true)
        val messageDao = db.messageDao()
        val contactDao = db.contactDao()
        var imported = 0
        var media = 0
        var multiline = 0
        var foundTxt = false
        var vcfFiles = 0
        val previousFavorites = if (preserveAnnotations) messageDao.favoriteOrdinals() else emptyList()
        val previousNotes = if (preserveAnnotations) {
            messageDao.annotatedMessages().associate { it.sourceOrdinal to it.localNote }
        } else emptyMap()

        db.withTransaction {
            messageDao.clear()
            contactDao.clear()

            val input = resolver.openInputStream(uri) ?: error("Impossible d'ouvrir le fichier")
            ZipInputStream(input).use { zis ->
                while (true) {
                    val entry = zis.nextEntry ?: break
                    if (entry.isDirectory) {
                        zis.closeEntry()
                        continue
                    }

                    when {
                        entry.name.lowercase().endsWith(".txt") && !foundTxt -> {
                            foundTxt = true
                            val txtBytes = zis.readBytes()
                            val reader = BufferedReader(InputStreamReader(txtBytes.inputStream(), Charsets.UTF_8))
                            val batch = ArrayList<MessageEntity>(500)
                            var current: MutableParsed? = null
                            var ordinal = 0

                            suspend fun flushBatch() {
                                if (batch.isEmpty()) return
                                messageDao.insertAll(batch.toList())
                                imported += batch.size
                                batch.clear()
                                onProgress(imported)
                            }

                            suspend fun emitCurrent() {
                                val c = current ?: return
                                ordinal++
                                val body = c.body.toString()
                                val omitted = body.trim().equals("<Médias omis>", ignoreCase = true)
                                if (omitted) media++
                                if (c.extraLines > 0) multiline++
                                batch.add(
                                    MessageEntity(
                                        timestamp = c.timestamp,
                                        sender = c.sender.trim(),
                                        body = body,
                                        isMediaOmitted = omitted,
                                        sourceOrdinal = ordinal,
                                        localNote = previousNotes[ordinal].orEmpty()
                                    )
                                )
                                current = null
                                if (batch.size >= 500) flushBatch()
                            }

                            var line: String?
                            var firstLine = true
                            while (reader.readLine().also { line = it } != null) {
                                var value = line ?: continue
                                if (firstLine) {
                                    value = value.removePrefix("\uFEFF")
                                    firstLine = false
                                }
                                val m = matchStartLine(value)
                                if (m != null) {
                                    emitCurrent()
                                    val (d, mo, y, h, mi, sender, body) = m.destructured
                                    val parsedYear = y.toInt()
                                    val fullYear = if (parsedYear < 100) 2000 + parsedYear else parsedYear
                                    val dt = LocalDateTime.of(fullYear, mo.toInt(), d.toInt(), h.toInt(), mi.toInt())
                                    current = MutableParsed(
                                        timestamp = dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                        sender = sender,
                                        body = StringBuilder(body)
                                    )
                                } else if (current != null) {
                                    current!!.body.append('\n').append(value)
                                    current!!.extraLines++
                                }
                            }
                            emitCurrent()
                            flushBatch()
                        }

                        entry.name.lowercase().endsWith(".vcf") -> {
                            vcfFiles++
                            val bytes = zis.readBytes()
                            val raw = bytes.toString(Charsets.UTF_8)
                            parseVcard(raw, entry.name)?.let { contactDao.insertAll(listOf(it)) }
                        }
                    }
                    zis.closeEntry()
                }
            }

            if (!foundTxt) error("Aucun export WhatsApp .txt trouvé dans le ZIP")
            if (imported == 0) error("Le fichier texte a été trouvé mais aucun message n'a été reconnu")
            if (previousFavorites.isNotEmpty()) {
                previousFavorites.chunked(500).forEach { messageDao.restoreFavorites(it) }
            }
        }

        Result(imported, media, multiline, vcfFiles, sha256, preserveAnnotations)
    }

    private fun parseVcard(raw: String, filename: String): ContactEntity? {
        val unfolded = raw.replace("\r\n", "\n").replace("\r", "\n")
            .split('\n')
            .fold(mutableListOf<String>()) { acc, line ->
                if ((line.startsWith(" ") || line.startsWith("\t")) && acc.isNotEmpty()) {
                    acc[acc.lastIndex] = acc.last() + line.trimStart()
                } else {
                    acc.add(line)
                }
                acc
            }

        fun valuesFor(prefix: String): List<String> = unfolded.mapNotNull { line ->
            val beforeColon = line.substringBefore(':', "")
            if (beforeColon.substringBefore(';').substringAfterLast('.').equals(prefix, ignoreCase = true)) {
                line.substringAfter(':', "").trim().takeIf { it.isNotEmpty() }
            } else null
        }

        val name = valuesFor("FN").firstOrNull()
            ?: filename.substringAfterLast('/').removeSuffix(".vcf").removeSuffix(".VCF").trim()
        if (name.isBlank()) return null

        val phones = valuesFor("TEL").distinct().joinToString(" • ")
        val emails = valuesFor("EMAIL").distinct().joinToString(" • ")
        val organization = valuesFor("ORG").firstOrNull()?.takeUnless { it.equals("null", true) }.orEmpty()

        return ContactEntity(
            displayName = unescapeVcard(name),
            phones = unescapeVcard(phones),
            emails = unescapeVcard(emails),
            organization = unescapeVcard(organization),
            sourceFilename = filename.substringAfterLast('/')
        )
    }

    private fun unescapeVcard(value: String): String = value
        .replace("\\n", "\n", ignoreCase = true)
        .replace("\\,", ",")
        .replace("\\;", ";")
        .replace("\\\\", "\\")
        .trim()

    private fun sha256(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(64 * 1024)
        val input = resolver.openInputStream(uri) ?: return ""
        input.use {
            while (true) {
                val read = it.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { b -> "%02x".format(b) }
    }

    private data class MutableParsed(
        val timestamp: Long,
        val sender: String,
        val body: StringBuilder,
        var extraLines: Int = 0
    )
}
