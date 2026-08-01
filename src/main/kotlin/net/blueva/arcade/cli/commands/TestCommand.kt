package net.blueva.arcade.cli.commands

import net.blueva.luak.LuaError
import net.blueva.luak.LuaTable
import net.blueva.luak.LuaValue
import net.blueva.luak.lib.ThreeArgFunction
import net.blueva.luak.lib.TwoArgFunction
import net.blueva.luak.lib.jvm.JvmPlatform
import java.io.File

/**
 * Runs the `.lua` files under `tests/` against a small built-in assertion library
 * (`assert_eq`/`assert_true`/`assert_false`). Each test file must `return` a table mapping test
 * name to a zero-argument function; every entry runs independently and is reported pass/fail.
 *
 * This is plain Lua unit testing of a module's own standalone functions, not a simulated
 * `ba.*`/`session` API, so lifecycle hooks like `M.onStart` cannot be exercised here directly.
 */
object TestCommand {

    class TestFailedException(message: String) : Exception(message)

    fun run(projectDir: File): Int {
        val testsDir = File(projectDir, "tests")
        if (!testsDir.isDirectory) {
            println("No tests/ directory found - nothing to run")
            return 0
        }

        val testFiles = testsDir.listFiles { f -> f.isFile && f.name.endsWith(".lua") }
            ?.sortedBy { it.name } ?: emptyList()

        if (testFiles.isEmpty()) {
            println("No .lua files found under tests/ - nothing to run")
            return 0
        }

        var passed = 0
        var failed = 0

        for (file in testFiles) {
            val results = runFile(file)
            for ((name, error) in results) {
                if (error == null) {
                    println("PASS  ${file.name} :: $name")
                    passed++
                } else {
                    println("FAIL  ${file.name} :: $name - $error")
                    failed++
                }
            }
        }

        println("$passed passed, $failed failed")
        return if (failed == 0) 0 else 1
    }

    /** @return list of (testName, errorMessageOrNull) pairs, one per exported test function. */
    private fun runFile(file: File): List<Pair<String, String?>> {
        val globals = JvmPlatform.standardGlobals()
        installAssertions(globals)

        val chunk = try {
            globals.load(file.readText(Charsets.UTF_8), file.name)!!
        } catch (e: LuaError) {
            return listOf(file.name to "Lua syntax error: ${e.message}")
        }

        val result = try {
            chunk.call()!!
        } catch (e: LuaError) {
            return listOf(file.name to "error while loading the test file: ${e.message}")
        }

        if (result !is LuaTable) {
            return listOf(file.name to "test file must `return` a table of test-name -> function")
        }

        val results = mutableListOf<Pair<String, String?>>()
        val keys = result.keys()
        for (keyRaw in keys) {
            val key = keyRaw!!
            val name = key.tojstring()!!
            val fn = result.get(key)!!
            if (!fn.isfunction()) continue
            try {
                fn.call()
                results += name to null
            } catch (e: LuaError) {
                results += name to (e.message ?: "assertion failed")
            }
        }
        return results
    }

    private fun installAssertions(globals: net.blueva.luak.Globals) {
        globals.set("assert_eq", object : ThreeArgFunction() {
            override fun call(actualRaw: LuaValue?, expectedRaw: LuaValue?, messageRaw: LuaValue?): LuaValue {
                val actual = actualRaw ?: LuaValue.NIL
                val expected = expectedRaw ?: LuaValue.NIL
                val message = messageRaw ?: LuaValue.NIL
                if (!actual.eq_b(expected)) {
                    val prefix = if (message.isnil()) "" else "${message.tojstring()!!}: "
                    throw LuaError("${prefix}expected ${expected.tojstring()!!}, got ${actual.tojstring()!!}")
                }
                return LuaValue.NIL
            }
        })
        globals.set("assert_true", object : TwoArgFunction() {
            override fun call(valueRaw: LuaValue?, messageRaw: LuaValue?): LuaValue {
                val value = valueRaw ?: LuaValue.NIL
                val message = messageRaw ?: LuaValue.NIL
                if (!value.toboolean()) {
                    val prefix = if (message.isnil()) "" else "${message.tojstring()!!}: "
                    throw LuaError("${prefix}expected true, got ${value.tojstring()!!}")
                }
                return LuaValue.NIL
            }
        })
        globals.set("assert_false", object : TwoArgFunction() {
            override fun call(valueRaw: LuaValue?, messageRaw: LuaValue?): LuaValue {
                val value = valueRaw ?: LuaValue.NIL
                val message = messageRaw ?: LuaValue.NIL
                if (value.toboolean()) {
                    val prefix = if (message.isnil()) "" else "${message.tojstring()!!}: "
                    throw LuaError("${prefix}expected false, got ${value.tojstring()!!}")
                }
                return LuaValue.NIL
            }
        })
    }
}
