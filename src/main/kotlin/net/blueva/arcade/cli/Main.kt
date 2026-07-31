package net.blueva.arcade.cli

import net.blueva.arcade.cli.commands.BuildCommand
import net.blueva.arcade.cli.commands.CheckCommand
import net.blueva.arcade.cli.commands.InitCommand
import net.blueva.arcade.cli.commands.TestCommand
import java.io.File
import kotlin.system.exitProcess

/** Entry point for bacli. Run `bacli --help` for the full command list. */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        printUsage()
        exitProcess(1)
    }

    val exitCode = when (args[0]) {
        "init" -> runInit(args.drop(1))
        "check" -> CheckCommand.run(projectDirArg(args.drop(1)))
        "build" -> runBuild(args.drop(1))
        "test" -> TestCommand.run(projectDirArg(args.drop(1)))
        "-h", "--help", "help" -> {
            printUsage()
            0
        }
        else -> {
            System.err.println("error: unknown command '${args[0]}'")
            printUsage()
            1
        }
    }
    exitProcess(exitCode)
}

private fun runInit(args: List<String>): Int {
    val flags = parseFlags(args)
    val directory = args.firstOrNull { !it.startsWith("--") }?.let { File(it) } ?: File(".")
    val id = flags["id"] ?: run {
        System.err.println("error: --id is required")
        return 1
    }
    val type = (flags["type"] ?: "microgame").uppercase()
    val name = flags["name"] ?: id.replace('_', ' ').replaceFirstChar { it.uppercase() }
    val author = flags["author"] ?: System.getProperty("user.name") ?: "Unknown"

    return InitCommand.run(InitCommand.InitOptions(directory, id, type, name, author))
}

private fun runBuild(args: List<String>): Int {
    val flags = parseFlags(args)
    val projectDir = projectDirArg(args)
    val outDir = File(flags["out"] ?: "dist")
    return BuildCommand.run(projectDir, outDir)
}

private fun projectDirArg(args: List<String>): File =
    args.firstOrNull { !it.startsWith("--") }?.let { File(it) } ?: File(".")

private fun parseFlags(args: List<String>): Map<String, String> {
    val flags = mutableMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        val arg = args[i]
        if (arg.startsWith("--")) {
            val key = arg.removePrefix("--")
            val value = args.getOrNull(i + 1)
            if (value != null && !value.startsWith("--")) {
                flags[key] = value
                i += 2
            } else {
                flags[key] = "true"
                i += 1
            }
        } else {
            i += 1
        }
    }
    return flags
}

private fun printUsage() {
    println(
        """
        bacli - the .bamodule authoring CLI

        Usage:
          bacli init [dir] --id <id> [--type minigame|microgame] [--name <name>] [--author <author>]
          bacli check [dir]
          bacli build [dir] [--out <dir>]
          bacli test [dir]

        [dir] defaults to the current directory.
        """.trimIndent()
    )
}
