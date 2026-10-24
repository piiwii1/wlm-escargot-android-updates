package ch.piiwii.events.data

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Fully portable offline backup. Images are copied into app-private storage on restore. */
class BackupManager(private val context: Context) {
    data class ImportPreview(
        val tempDir: File,
        val jsonFile: File,
        val eventCount: Int,
        val imageCount: Int
    )

    private val maxSingleFile = 100L * 1024 * 1024
    private val maxArchive = 512L * 1024 * 1024
    private val maxEntries = 1500

    fun export(uri: Uri) {
        val temporary = File.createTempFile("widgevent_export_", ".zip", context.cacheDir)
        try {
            val config = EventRepository.get(context).load().toJson()
            val entries = LinkedHashMap<String, File>()
            val seen = HashMap<String, String>()
            val events = config.getJSONArray("events")
            for (i in 0 until events.length()) {
                val event = events.getJSONObject(i)
                for (key in listOf("imagePath", "originalImagePath")) {
                    val path = event.optString(key)
                    if (path.isBlank()) continue
                    val file = File(path)
                    require(file.isFile && file.length() > 0) {
                        "Photo introuvable pour ${event.optString("name")}. Vérifiez les images avant la sauvegarde."
                    }
                    require(file.length() <= maxSingleFile) { "Photo trop volumineuse : ${file.name}" }
                    val canonical = file.canonicalPath
                    val insideZip = seen.getOrPut(canonical) {
                        val extension = file.extension.lowercase().takeIf {
                            it in setOf("png", "jpg", "jpeg", "webp", "gif")
                        } ?: "img"
                        "images/${UUID.randomUUID()}.$extension"
                    }
                    entries[insideZip] = file
                    event.put(key, insideZip)
                }
            }
            val metadata = JSONObject().apply {
                put("format", "widgevent-portable-backup")
                put("version", 2)
                put("createdAt", System.currentTimeMillis())
                put("events", events.length())
                put("images", entries.size)
                put("sha256", JSONObject().apply {
                    for ((name, file) in entries) put(name, sha256(file))
                })
            }
            val total = entries.values.sumOf { it.length() }
            require(total <= maxArchive) { "Photos trop volumineuses pour la sauvegarde (plus de 512 Mo)" }
            ZipOutputStream(FileOutputStream(temporary).buffered()).use { zip ->
                fun writeText(name: String, text: String) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(text.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
                writeText("config.json", config.toString(2))
                writeText("manifest.json", metadata.toString(2))
                for ((name, file) in entries) {
                    zip.putNextEntry(ZipEntry(name))
                    file.inputStream().buffered().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            context.contentResolver.openOutputStream(uri, "w").use { dest ->
                requireNotNull(dest) { "Impossible d'ouvrir le fichier de sauvegarde" }
                temporary.inputStream().buffered().use { it.copyTo(dest) }
                dest.flush()
            }
        } finally {
            temporary.delete()
        }
    }

    fun inspect(uri: Uri): ImportPreview {
        val folder = File(context.cacheDir, "widgevent_restore_${UUID.randomUUID()}")
        check(folder.mkdirs()) { "Impossible de préparer la restauration" }
        try {
            var count = 0
            var total = 0L
            context.contentResolver.openInputStream(uri).use { raw ->
                requireNotNull(raw) { "Impossible de lire la sauvegarde" }
                ZipInputStream(raw.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        count++
                        require(count <= maxEntries) { "Sauvegarde trop volumineuse" }
                        val name = entry.name
                        require(name == "config.json" || name == "manifest.json" ||
                            (name.startsWith("images/") && name.removePrefix("images/").isNotBlank() &&
                             !name.removePrefix("images/").contains('/'))) { "Chemin interdit dans la sauvegarde" }
                        require(!name.contains('\\') && !name.contains("..")) { "Chemin interdit dans la sauvegarde" }
                        require(!entry.isDirectory) { "Entrée de sauvegarde non reconnue" }
                        val target = File(folder, name)
                        require(!target.exists()) { "Entrée dupliquée : $name" }
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { out ->
                            val buffer = ByteArray(64 * 1024)
                            var size = 0L
                            while (true) {
                                val n = zip.read(buffer)
                                if (n < 0) break
                                size += n
                                total += n
                                require(size <= maxSingleFile && total <= maxArchive) {
                                    "Sauvegarde trop volumineuse ou corrompue"
                                }
                                out.write(buffer, 0, n)
                            }
                        }
                        zip.closeEntry()
                    }
                }
            }
            val json = File(folder, "config.json")
            require(json.isFile && json.length() > 0) { "config.json absent de la sauvegarde" }
            val root = JSONObject(json.readText())
            val events = root.optJSONArray("events") ?: error("Liste des événements absente")
            require(root.optJSONObject("settings") != null) { "Paramètres absents de la sauvegarde" }
            val manifestFile = File(folder, "manifest.json")
            if (manifestFile.isFile) {
                val manifest = JSONObject(manifestFile.readText())
                require(manifest.optString("format") == "widgevent-portable-backup" &&
                    manifest.optInt("version") == 2) { "Version de sauvegarde non reconnue" }
                require(manifest.optInt("events") == events.length()) { "Nombre d'événements incorrect" }
                val digests = manifest.getJSONObject("sha256")
                val keys = digests.keys()
                while (keys.hasNext()) {
                    val name = keys.next()
                    val image = File(folder, name)
                    require(name.startsWith("images/") && image.isFile &&
                        sha256(image).equals(digests.getString(name), ignoreCase = true)) {
                        "Photo endommagée dans la sauvegarde : $name"
                    }
                }
            }
            for (i in 0 until events.length()) {
                val event = events.getJSONObject(i)
                for (key in listOf("imagePath", "originalImagePath")) {
                    val value = event.optString(key)
                    if (value.isBlank()) continue
                    require(value.startsWith("images/") && !value.contains("..") &&
                        !value.removePrefix("images/").contains('/')) {
                        "La sauvegarde contient une ancienne référence de photo non portable pour ${event.optString("name")}"
                    }
                    require(File(folder, value).isFile) {
                        "Photo manquante dans la sauvegarde : ${event.optString("name")}"
                    }
                }
            }
            val imageCount = File(folder, "images").listFiles()?.count { it.isFile } ?: 0
            if (manifestFile.isFile) require(JSONObject(manifestFile.readText()).optInt("images") == imageCount) {
                "Nombre de photos incorrect"
            }
            return ImportPreview(folder, json, events.length(), imageCount)
        } catch (e: Exception) {
            folder.deleteRecursively()
            throw e
        }
    }

    fun apply(preview: ImportPreview) {
        val newImages = mutableListOf<File>()
        try {
            val root = JSONObject(preview.jsonFile.readText())
            val events = root.getJSONArray("events")
            val directory = ImageStorage(context).imageDirectory
            val copied = HashMap<String, String>()
            for (i in 0 until events.length()) {
                val event = events.getJSONObject(i)
                for (key in listOf("imagePath", "originalImagePath")) {
                    val relative = event.optString(key)
                    if (relative.isBlank()) continue
                    require(relative.startsWith("images/") && !relative.contains("..") &&
                        !relative.removePrefix("images/").contains('/')) { "Photo invalide" }
                    val source = File(preview.tempDir, relative)
                    require(source.isFile && source.length() > 0) { "Photo manquante : ${event.optString("name")}" }
                    val targetPath = copied.getOrPut(relative) {
                        val extension = source.extension.takeIf { it.matches(Regex("[A-Za-z0-9]{1,5}")) } ?: "png"
                        val target = File(directory, "restored_${UUID.randomUUID()}.$extension")
                        source.copyTo(target, overwrite = false)
                        newImages.add(target)
                        target.absolutePath
                    }
                    event.put(key, targetPath)
                }
            }
            EventRepository.get(context).importJsonString(root.toString())
        } catch (e: Exception) {
            newImages.forEach { it.delete() }
            throw e
        } finally {
            preview.tempDir.deleteRecursively()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buf)
                if (read < 0) break
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
