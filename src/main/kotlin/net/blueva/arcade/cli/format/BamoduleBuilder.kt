package net.blueva.arcade.cli.format

import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Packages a project directory (`module.toml` + `src/` + `resources/` + optional `assets/`) into
 * a `.bamodule` archive, reusing [BamoduleContainer]'s own file allow-list.
 */
object BamoduleBuilder {

    class BuildResult(val outputFile: File, val manifest: ModuleManifest, val entryCount: Int)

    /**
     * @param projectDir root of the authored project, containing `module.toml` at its top level.
     * @param outputDir directory the `.bamodule` is written into (created if missing).
     */
    fun build(projectDir: File, outputDir: File): BuildResult {
        val manifestFile = File(projectDir, "module.toml")
        if (!manifestFile.isFile) {
            throw ContainerValidationException("No module.toml found in ${projectDir.path}")
        }
        val manifest = ManifestParser.parse(manifestFile.readText(Charsets.UTF_8))

        val entryFile = File(projectDir, manifest.entry)
        if (!entryFile.isFile) {
            throw ContainerValidationException("Manifest entry '${manifest.entry}' does not exist in ${projectDir.path}")
        }

        outputDir.mkdirs()
        val outputFile = File(outputDir, "${manifest.id}-${manifest.version}.bamodule")

        var entryCount = 0
        ZipOutputStream(Files.newOutputStream(outputFile.toPath())).use { zip ->
            zip.putNextEntry(ZipEntry(BamoduleContainer.MANIFEST_ENTRY))
            zip.write(manifestFile.readBytes())
            zip.closeEntry()
            entryCount++

            for (topLevel in listOf("src", "resources", "assets")) {
                val dir = File(projectDir, topLevel)
                if (!dir.isDirectory) continue
                entryCount += addDirectory(zip, projectDir, dir)
            }

            val metaDir = File(projectDir, "META")
            if (metaDir.isDirectory) {
                entryCount += addDirectory(zip, projectDir, metaDir)
            }
        }

        return BuildResult(outputFile, manifest, entryCount)
    }

    private fun addDirectory(zip: ZipOutputStream, projectRoot: File, dir: File): Int {
        var count = 0
        val files = dir.listFiles() ?: return 0
        for (file in files.sortedBy { it.name }) {
            if (file.isDirectory) {
                count += addDirectory(zip, projectRoot, file)
                continue
            }
            val relativePath = projectRoot.toPath().relativize(file.toPath()).toString().replace('\\', '/')
            // Reject before writing anything the runtime would refuse to load anyway.
            BamoduleContainer.validateEntryLocation(relativePath)

            zip.putNextEntry(ZipEntry(relativePath))
            zip.write(file.readBytes())
            zip.closeEntry()
            count++
        }
        return count
    }
}
