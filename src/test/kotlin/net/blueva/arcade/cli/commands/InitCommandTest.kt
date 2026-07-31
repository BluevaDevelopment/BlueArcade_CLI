package net.blueva.arcade.cli.commands

import net.blueva.arcade.cli.format.ManifestParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class InitCommandTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `scaffolds a valid project that the manifest parser accepts`() {
        val dir = tempDir.resolve("chairs").toFile()

        val exitCode = InitCommand.run(
            InitCommand.InitOptions(dir, "chairs", "MICROGAME", "Chairs", "Blueva")
        )

        assertEquals(0, exitCode)
        assertTrue(File(dir, "module.toml").exists())
        assertTrue(File(dir, "src/main.lua").exists())
        assertTrue(File(dir, "resources/language/en.yml").exists())

        val manifest = ManifestParser.parse(File(dir, "module.toml").readText())
        assertEquals("chairs", manifest.id)
        assertEquals("Chairs", manifest.name)
        assertEquals("src/main.lua", manifest.entry)
    }

    @Test
    fun `rejects an invalid id`() {
        val dir = tempDir.resolve("bad-id").toFile()

        val exitCode = InitCommand.run(
            InitCommand.InitOptions(dir, "Not Valid", "MICROGAME", "X", "Blueva")
        )

        assertEquals(1, exitCode)
        assertTrue(!File(dir, "module.toml").exists())
    }

    @Test
    fun `rejects an invalid type`() {
        val dir = tempDir.resolve("bad-type").toFile()

        val exitCode = InitCommand.run(
            InitCommand.InitOptions(dir, "chairs", "BATTLEROYALE", "X", "Blueva")
        )

        assertEquals(1, exitCode)
    }

    @Test
    fun `refuses to scaffold into a non-empty directory`() {
        val dir = tempDir.resolve("occupied").toFile()
        dir.mkdirs()
        File(dir, "existing.txt").writeText("already here")

        val exitCode = InitCommand.run(
            InitCommand.InitOptions(dir, "chairs", "MICROGAME", "Chairs", "Blueva")
        )

        assertEquals(1, exitCode)
        assertTrue(!File(dir, "module.toml").exists())
    }
}
