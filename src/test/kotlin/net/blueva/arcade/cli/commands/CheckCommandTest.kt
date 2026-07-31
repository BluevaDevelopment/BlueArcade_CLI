package net.blueva.arcade.cli.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class CheckCommandTest {

    @TempDir
    lateinit var tempDir: Path

    private fun validProject(dir: File) {
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
    }

    @Test
    fun `passes a valid project with no findings`() {
        val dir = tempDir.resolve("valid").toFile()
        validProject(dir)

        assertEquals(0, CheckCommand.run(dir))
    }

    @Test
    fun `fails when module toml is missing entirely`() {
        val dir = tempDir.resolve("no-manifest").toFile()
        dir.mkdirs()

        assertEquals(1, CheckCommand.run(dir))
    }

    @Test
    fun `fails when the manifest entry file does not exist on disk`() {
        val dir = tempDir.resolve("missing-entry").toFile()
        validProject(dir)
        File(dir, "src/main.lua").delete()

        assertEquals(1, CheckCommand.run(dir))
    }

    @Test
    fun `fails on a disallowed file extension outside the allow-list`() {
        val dir = tempDir.resolve("bad-extension").toFile()
        validProject(dir)
        File(dir, "src/helper.py").writeText("print('nope')\n")

        assertEquals(1, CheckCommand.run(dir))
    }

    @Test
    fun `fails on a Lua syntax error`() {
        val dir = tempDir.resolve("bad-syntax").toFile()
        validProject(dir)
        File(dir, "src/main.lua").writeText("local M = {\nreturn M\n") // unclosed table

        assertEquals(1, CheckCommand.run(dir))
    }

    @Test
    fun `flags a require path that looks like it escapes src`() {
        val dir = tempDir.resolve("bad-require").toFile()
        validProject(dir)
        File(dir, "src/main.lua").writeText("local x = require('../secret')\nlocal M = {}\nreturn M\n")

        assertEquals(1, CheckCommand.run(dir))
    }

    @Test
    fun `does not flag a normal relative require path`() {
        val dir = tempDir.resolve("good-require").toFile()
        validProject(dir)
        File(dir, "src/helper.lua").writeText("return { x = 1 }\n")
        File(dir, "src/main.lua").writeText("local helper = require('helper')\nlocal M = {}\nreturn M\n")

        assertEquals(0, CheckCommand.run(dir))
    }
}
