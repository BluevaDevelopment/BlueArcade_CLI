package net.blueva.arcade.cli.format

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ManifestParserTest {

    private val validManifest = """
        schema = 1
        id = "chairs"
        name = "Chairs"
        version = "1.0.0"
        type = "MICROGAME"
        api = "^1.0"
        entry = "src/main.lua"
        authors = ["Blueva"]
        description = "Grab a seat when the music stops."
    """.trimIndent()

    @Test
    fun `parses a valid manifest`() {
        val manifest = ManifestParser.parse(validManifest)

        assertEquals("chairs", manifest.id)
        assertEquals("Chairs", manifest.name)
        assertEquals(ModuleType.MICROGAME, manifest.type)
        assertEquals("src/main.lua", manifest.entry)
        assertEquals(listOf("Blueva"), manifest.authors)
    }

    @Test
    fun `rejects an unsupported schema version`() {
        val exception = assertThrows(ManifestParseException::class.java) {
            ManifestParser.parse(validManifest.replace("schema = 1", "schema = 2"))
        }
        assert(exception.message!!.contains("schema"))
    }

    @Test
    fun `rejects an invalid module id`() {
        assertThrows(ManifestParseException::class.java) {
            ManifestParser.parse(validManifest.replace("\"chairs\"", "\"Chairs With Spaces\""))
        }
    }

    @Test
    fun `rejects an entry not under src or not a lua file`() {
        assertThrows(ManifestParseException::class.java) {
            ManifestParser.parse(validManifest.replace("\"src/main.lua\"", "\"main.lua\""))
        }
        assertThrows(ManifestParseException::class.java) {
            ManifestParser.parse(validManifest.replace("\"src/main.lua\"", "\"src/main.py\""))
        }
    }

    @Test
    fun `rejects an unknown module type`() {
        assertThrows(ManifestParseException::class.java) {
            ManifestParser.parse(validManifest.replace("\"MICROGAME\"", "\"BATTLEROYALE\""))
        }
    }

    @Test
    fun `rejects malformed TOML`() {
        assertThrows(ManifestParseException::class.java) {
            ManifestParser.parse("key = ")
        }
    }

    @Test
    fun `a manifest with no setup section parses with a null setup`() {
        assertNull(ManifestParser.parse(validManifest).setup)
    }

    @Test
    fun `parses a setup section`() {
        val withSetup = validManifest + "\n\n" + """
            [[setup.commands]]
            name = "musictime"
            usage = "musictime <seconds>"
            description = "Initial music duration"
            requiredHint = false
        """.trimIndent()

        val setup = ManifestParser.parse(withSetup).setup

        assertEquals(1, setup?.commands?.size)
        assertEquals("musictime", setup?.commands?.get(0)?.name)
    }

    @Test
    fun `rejects an install requirement with an unknown type`() {
        val withSetup = validManifest + "\n\n" + """
            [[setup.installRequirements]]
            id = "x"
            type = "NOT_A_REAL_TYPE"
            required = false
            displayName = "X"
            description = ""
        """.trimIndent()

        assertThrows(ManifestParseException::class.java) { ManifestParser.parse(withSetup) }
    }

    @Test
    fun `rejects an api range without a caret prefix`() {
        val manifest = validManifest.replace("api = \"^1.0\"", "api = \"1.0\"")

        assertThrows(ManifestParseException::class.java) { ManifestParser.parse(manifest) }
    }

    @Test
    fun `rejects an api range with a patch component`() {
        val manifest = validManifest.replace("api = \"^1.0\"", "api = \"^1.0.0\"")

        assertThrows(ManifestParseException::class.java) { ManifestParser.parse(manifest) }
    }
}
