package net.blueva.arcade.cli.commands

import net.blueva.arcade.cli.format.BamoduleContainer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class BuildCommandTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `builds a loadable archive for a valid project`() {
        val dir = tempDir.resolve("valid").toFile()
        dir.mkdirs()
        File(dir, "module.toml").writeText(
            """
            schema = 1
            id = "chairs"
            name = "Chairs"
            version = "1.0.0"
            type = "MICROGAME"
            api = "^1.0"
            entry = "src/main.lua"
            authors = ["Blueva"]
            """.trimIndent()
        )
        File(dir, "src").mkdirs()
        File(dir, "src/main.lua").writeText("local M = {}\nreturn M\n")
        val outDir = tempDir.resolve("dist").toFile()

        assertEquals(0, BuildCommand.run(dir, outDir))

        val built = File(outDir, "chairs-1.0.0.bamodule")
        assertTrue(built.exists())
        assertEquals("chairs", BamoduleContainer.open(built).manifest.id)
    }

    @Test
    fun `refuses to build a project that fails check`() {
        val dir = tempDir.resolve("broken").toFile()
        dir.mkdirs()
        File(dir, "module.toml").writeText(
            """
            schema = 1
            id = "chairs"
            name = "Chairs"
            version = "1.0.0"
            type = "MICROGAME"
            api = "^1.0"
            entry = "src/main.lua"
            authors = ["Blueva"]
            """.trimIndent()
        )
        // No src/main.lua on disk - check should catch this before build tries to package anything.
        val outDir = tempDir.resolve("dist2").toFile()

        assertEquals(1, BuildCommand.run(dir, outDir))
        assertFalse(File(outDir, "chairs-1.0.0.bamodule").exists())
    }
}
