package net.blueva.arcade.cli.commands

import java.io.File

/** Scaffolds a new `.bamodule` project: `module.toml`, a starter `src/main.lua`, and a language file. */
object InitCommand {

    class InitOptions(
        val directory: File,
        val id: String,
        val type: String,
        val name: String,
        val author: String,
    )

    fun run(options: InitOptions): Int {
        if (!ID_PATTERN.matches(options.id)) {
            System.err.println("error: --id '${options.id}' is invalid: must match [a-z0-9_]+")
            return 1
        }
        if (options.type != "MINIGAME" && options.type != "MICROGAME") {
            System.err.println("error: --type must be 'minigame' or 'microgame', got '${options.type}'")
            return 1
        }

        if (options.directory.exists() && (options.directory.listFiles()?.isNotEmpty() == true)) {
            System.err.println("error: ${options.directory.path} already exists and is not empty")
            return 1
        }

        val srcDir = File(options.directory, "src")
        val languageDir = File(options.directory, "resources/language")
        srcDir.mkdirs()
        languageDir.mkdirs()

        File(options.directory, "module.toml").writeText(manifestToml(options))
        File(srcDir, "main.lua").writeText(mainLuaStub(options))
        File(languageDir, "en.yml").writeText(languageYmlStub())

        println("Scaffolded '${options.id}' (${options.type}) in ${options.directory.path}")
        println("Next: bacli check, then bacli build")
        return 0
    }

    private fun manifestToml(options: InitOptions): String = """
        schema = 1
        id = "${options.id}"
        name = "${options.name}"
        version = "0.1.0"
        type = "${options.type}"
        api = "^1.0"
        entry = "src/main.lua"
        authors = ["${options.author}"]
        description = ""
    """.trimIndent() + "\n"

    private fun mainLuaStub(options: InitOptions): String = """
        local M = {}

        function M.onStart(session)
          -- Called when a match starts. `session` exposes scheduler, sounds, titles,
          -- scoreboard, messages, config, coreConfig, dataAccess, arena, world, entities and stats.
        end

        function M.onEnd(session, result)
          -- Called when a match ends. `result` has getWinner()/getPodium()/etc.
        end

        return M
    """.trimIndent() + "\n"

    private fun languageYmlStub(): String = """
        description:
          default:
            - "Describe your module here!"
    """.trimIndent() + "\n"

    private val ID_PATTERN = Regex("^[a-z0-9_]+$")
}
