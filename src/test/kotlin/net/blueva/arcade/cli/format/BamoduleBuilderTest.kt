package net.blueva.arcade.cli.format

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/** Covers the round trip: [BamoduleBuilder] packages a project, [BamoduleContainer] must accept it back. */
class BamoduleBuilderTest {

    @TempDir
    lateinit var tempDir: Path

    private fun writeProject(dir: File, entry: String = "src/main.lua") {
        dir.mkdirs()
        File(dir, "module.toml").writeText(
            """
            schema = 1
            id = "chairs"
            name = "Chairs"
            version = "1.0.0"
            type = "MICROGAME"
            api = "^1.0"
            entry = "$entry"
            authors = ["Blueva"]
            """.trimIndent()
        )
        File(dir, "src").mkdirs()
        File(dir, "src/main.lua").writeText("local M = {}\nreturn M\n")
        File(dir, "resources/language").mkdirs()
        File(dir, "resources/language/en.yml").writeText("key: value\n")
    }

    @Test
    fun `builds an archive the runtime's own reader accepts`() {
        val projectDir = tempDir.resolve("project").toFile()
        writeProject(projectDir)
        val outDir = tempDir.resolve("dist").toFile()

        val result = BamoduleBuilder.build(projectDir, outDir)

        assertTrue(result.outputFile.exists())
        assertEquals("chairs-1.0.0.bamodule", result.outputFile.name)

        val container = BamoduleContainer.open(result.outputFile)
        assertEquals("chairs", container.manifest.id)
        assertTrue(container.hasEntry("src/main.lua"))
        assertTrue(container.hasEntry("resources/language/en.yml"))
    }

    @Test
    fun `fails when module toml is missing`() {
        val projectDir = tempDir.resolve("empty").toFile()
        projectDir.mkdirs()

        assertThrows(ContainerValidationException::class.java) {
            BamoduleBuilder.build(projectDir, tempDir.resolve("dist2").toFile())
        }
    }

    @Test
    fun `fails when the manifest entry file does not exist`() {
        val projectDir = tempDir.resolve("missing-entry").toFile()
        writeProject(projectDir)
        File(projectDir, "src/main.lua").delete()

        assertThrows(ContainerValidationException::class.java) {
            BamoduleBuilder.build(projectDir, tempDir.resolve("dist3").toFile())
        }
    }

    @Test
    fun `rejects a disallowed file extension under src`() {
        val projectDir = tempDir.resolve("bad-src").toFile()
        writeProject(projectDir)
        File(projectDir, "src/helper.py").writeText("print('nope')\n")

        assertThrows(ContainerValidationException::class.java) {
            BamoduleBuilder.build(projectDir, tempDir.resolve("dist4").toFile())
        }
    }
}
