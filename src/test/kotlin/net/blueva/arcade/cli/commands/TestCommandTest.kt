package net.blueva.arcade.cli.commands

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class TestCommandTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `reports success with no tests directory`() {
        val dir = tempDir.resolve("no-tests").toFile()
        dir.mkdirs()

        assertEquals(0, TestCommand.run(dir))
    }

    @Test
    fun `runs a passing test file`() {
        val dir = tempDir.resolve("passing").toFile()
        File(dir, "tests").mkdirs()
        File(dir, "tests/example.lua").writeText(
            """
            return {
              ["adds numbers"] = function()
                assert_eq(1 + 1, 2)
              end,
            }
            """.trimIndent()
        )

        assertEquals(0, TestCommand.run(dir))
    }

    @Test
    fun `reports a failing assertion as a test failure`() {
        val dir = tempDir.resolve("failing").toFile()
        File(dir, "tests").mkdirs()
        File(dir, "tests/example.lua").writeText(
            """
            return {
              ["is wrong on purpose"] = function()
                assert_eq(1 + 1, 3)
              end,
            }
            """.trimIndent()
        )

        assertEquals(1, TestCommand.run(dir))
    }

    @Test
    fun `runs multiple named tests from the same file independently`() {
        val dir = tempDir.resolve("mixed").toFile()
        File(dir, "tests").mkdirs()
        File(dir, "tests/example.lua").writeText(
            """
            return {
              ["passes"] = function() assert_true(true) end,
              ["fails"] = function() assert_false(true) end,
            }
            """.trimIndent()
        )

        assertEquals(1, TestCommand.run(dir))
    }

    @Test
    fun `a Lua syntax error in a test file is reported as a failure, not a crash`() {
        val dir = tempDir.resolve("bad-syntax").toFile()
        File(dir, "tests").mkdirs()
        File(dir, "tests/example.lua").writeText("return {\n")

        assertEquals(1, TestCommand.run(dir))
    }
}
