package net.blueva.arcade.cli.commands

import net.blueva.arcade.cli.format.BamoduleBuilder
import net.blueva.arcade.cli.format.ContainerValidationException
import net.blueva.arcade.cli.format.ManifestParseException
import java.io.File

/** Runs [CheckCommand] first, then packages the project into a `.bamodule` via [BamoduleBuilder]. */
object BuildCommand {

    fun run(projectDir: File, outDir: File): Int {
        val checkResult = CheckCommand.run(projectDir)
        if (checkResult != 0) {
            println("build aborted: fix the errors above first (or see 'bacli check')")
            return checkResult
        }

        return try {
            val result = BamoduleBuilder.build(projectDir, outDir)
            println("Built ${result.outputFile.path} (${result.entryCount} entries)")
            0
        } catch (e: ManifestParseException) {
            println("error: ${e.message}")
            1
        } catch (e: ContainerValidationException) {
            println("error: ${e.message}")
            1
        }
    }
}
