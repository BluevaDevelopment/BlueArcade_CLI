package net.blueva.arcade.cli.commands

import net.blueva.arcade.cli.format.BamoduleContainer
import net.blueva.arcade.cli.format.ContainerValidationException
import net.blueva.arcade.cli.format.ManifestParseException
import net.blueva.arcade.cli.format.ManifestParser
import org.luaj.vm2.LuaError
import org.luaj.vm2.lib.jse.JsePlatform
import java.io.File

/**
 * Validates a project before `bacli build` packages it: manifest schema, the `src/`/`resources/`/
 * `assets/` file allow-list, and Lua syntax (compiled, never executed).
 *
 * Does not yet catch sandbox violations (forbidden globals), only a best-effort scan for
 * `require(...)` paths that look like they escape `src/`.
 */
object CheckCommand {

    class Finding(val severity: Severity, val message: String)
    enum class Severity { ERROR, WARNING }

    private val SUSPICIOUS_REQUIRE = Regex("""require\s*\(\s*["']([^"']+)["']\s*\)""")

    fun run(projectDir: File): Int {
        val findings = mutableListOf<Finding>()

        val manifestFile = File(projectDir, "module.toml")
        if (!manifestFile.isFile) {
            println("error: no module.toml found in ${projectDir.path}")
            return 1
        }

        val manifest = try {
            ManifestParser.parse(manifestFile.readText(Charsets.UTF_8))
        } catch (e: ManifestParseException) {
            println("error: ${e.message}")
            return 1
        }

        val entryFile = File(projectDir, manifest.entry)
        if (!entryFile.isFile) {
            findings += Finding(Severity.ERROR, "manifest entry '${manifest.entry}' does not exist")
        }

        for (topLevel in listOf("src", "resources", "assets", "META")) {
            val dir = File(projectDir, topLevel)
            if (!dir.isDirectory) continue
            checkDirectory(projectDir, dir, findings)
        }

        val luaFiles = File(projectDir, "src").walkTopDown().filter { it.isFile && it.name.endsWith(".lua") }
        for (file in luaFiles) {
            checkLuaSyntax(file, findings)
            checkRequirePaths(file, findings)
        }

        val errors = findings.filter { it.severity == Severity.ERROR }
        val warnings = findings.filter { it.severity == Severity.WARNING }

        for (finding in errors) println("error: ${finding.message}")
        for (finding in warnings) println("warning: ${finding.message}")

        if (errors.isEmpty()) {
            println("${manifest.id}: ${warnings.size} warning(s), 0 error(s)")
        } else {
            println("${manifest.id}: ${warnings.size} warning(s), ${errors.size} error(s)")
        }

        return if (errors.isEmpty()) 0 else 1
    }

    private fun checkDirectory(projectRoot: File, dir: File, findings: MutableList<Finding>) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                checkDirectory(projectRoot, file, findings)
                continue
            }
            val relativePath = projectRoot.toPath().relativize(file.toPath()).toString().replace('\\', '/')
            try {
                BamoduleContainer.validateEntryLocation(relativePath)
            } catch (e: ContainerValidationException) {
                findings += Finding(Severity.ERROR, e.message ?: "invalid entry: $relativePath")
            }
        }
    }

    private fun checkLuaSyntax(file: File, findings: MutableList<Finding>) {
        try {
            val globals = JsePlatform.standardGlobals()
            globals.load(file.readText(Charsets.UTF_8), file.name)
        } catch (e: LuaError) {
            findings += Finding(Severity.ERROR, "${file.name}: Lua syntax error: ${e.message}")
        }
    }

    private fun checkRequirePaths(file: File, findings: MutableList<Finding>) {
        val text = file.readText(Charsets.UTF_8)
        for (match in SUSPICIOUS_REQUIRE.findAll(text)) {
            val path = match.groupValues[1]
            if (path.startsWith("..") || path.startsWith("/") || path.contains(":")) {
                findings += Finding(
                    Severity.ERROR,
                    "${file.name}: require('$path') looks like it escapes src/ - only relative paths inside src/ are loadable at runtime",
                )
            }
        }
    }
}
