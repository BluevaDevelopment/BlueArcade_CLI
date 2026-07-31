package net.blueva.arcade.cli.format

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ContainerValidationException(message: String) : Exception(message)

/**
 * Validated in-memory view of a `.bamodule` archive: a zip file, read and checked in a single
 * pass against path traversal, a directory/extension allow-list, and size/count caps.
 */
class BamoduleContainer private constructor(
    private val entries: Map<String, ByteArray>,
) {
    val manifest: ModuleManifest = ManifestParser.parse(
        readText(MANIFEST_ENTRY) ?: throw ContainerValidationException("Archive is missing $MANIFEST_ENTRY")
    )

    fun readText(path: String): String? = entries[path]?.toString(Charsets.UTF_8)

    fun readBytes(path: String): ByteArray? = entries[path]

    fun hasEntry(path: String): Boolean = entries.containsKey(path)

    fun entryPaths(): Set<String> = entries.keys

    companion object {
        const val MANIFEST_ENTRY = "module.toml"

        private const val MAX_ENTRY_COUNT = 10_000
        private const val MAX_TOTAL_UNCOMPRESSED_BYTES = 200L * 1024 * 1024
        private const val MAX_SINGLE_ENTRY_BYTES = 50L * 1024 * 1024

        val ALLOWED_ASSET_EXTENSIONS = setOf("png", "jpg", "jpeg", "schem", "nbt")

        @Throws(IOException::class)
        fun open(file: File): BamoduleContainer {
            val entries = LinkedHashMap<String, ByteArray>()
            var totalBytes = 0L
            var entryCount = 0

            ZipInputStream(file.inputStream()).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    entryCount++
                    if (entryCount > MAX_ENTRY_COUNT) {
                        throw ContainerValidationException("Archive has more than $MAX_ENTRY_COUNT entries")
                    }

                    val name = normalizeAndValidatePath(entry.name)
                    if (!entry.isDirectory) {
                        validateEntryLocation(name)

                        val bytes = readEntryBounded(zip, name)
                        totalBytes += bytes.size
                        if (totalBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
                            throw ContainerValidationException(
                                "Archive exceeds the $MAX_TOTAL_UNCOMPRESSED_BYTES byte uncompressed size cap"
                            )
                        }
                        entries[name] = bytes
                    }

                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            return BamoduleContainer(entries)
        }

        fun normalizeAndValidatePath(rawName: String): String {
            if (rawName.isBlank()) throw ContainerValidationException("Archive contains a blank entry name")
            if (rawName.startsWith("/") || rawName.contains("\\") || (rawName.length > 1 && rawName[1] == ':')) {
                throw ContainerValidationException("Archive entry has an absolute or platform-specific path: $rawName")
            }

            val normalized = Paths.get(rawName).normalize().toString().replace('\\', '/')
            if (normalized.startsWith("..") || normalized.startsWith("/")) {
                throw ContainerValidationException("Archive entry escapes the archive root: $rawName")
            }
            return normalized
        }

        fun validateEntryLocation(name: String) {
            when {
                name == MANIFEST_ENTRY -> return
                name.startsWith("META/") -> return
                name.startsWith("src/") -> {
                    if (!name.endsWith(".lua")) {
                        throw ContainerValidationException("Only .lua files are allowed under src/, got: $name")
                    }
                }
                name.startsWith("resources/") -> {
                    if (!name.endsWith(".yml") && !name.endsWith(".yaml")) {
                        throw ContainerValidationException("Only .yml/.yaml files are allowed under resources/, got: $name")
                    }
                }
                name.startsWith("assets/") -> {
                    val extension = name.substringAfterLast('.', "").lowercase()
                    if (extension !in ALLOWED_ASSET_EXTENSIONS) {
                        throw ContainerValidationException("File type not allowed under assets/: $name")
                    }
                }
                else -> throw ContainerValidationException("Unrecognized top-level entry: $name")
            }
        }

        private fun readEntryBounded(zip: ZipInputStream, name: String): ByteArray {
            val buffer = ByteArray(8192)
            val output = ByteArrayOutputStream()
            var totalRead = 0L
            var read: Int
            while (zip.read(buffer).also { read = it } != -1) {
                totalRead += read
                if (totalRead > MAX_SINGLE_ENTRY_BYTES) {
                    throw ContainerValidationException(
                        "Entry $name exceeds the $MAX_SINGLE_ENTRY_BYTES byte single-entry cap"
                    )
                }
                output.write(buffer, 0, read)
            }
            return output.toByteArray()
        }
    }
}
