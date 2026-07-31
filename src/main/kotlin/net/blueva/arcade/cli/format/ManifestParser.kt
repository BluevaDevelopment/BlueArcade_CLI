package net.blueva.arcade.cli.format

import net.blueva.arcade.api.module.ModuleType
import net.blueva.foundation.config.ConfigFormat
import net.blueva.foundation.config.ConfigParseException
import net.blueva.foundation.config.ConfigSection
import net.blueva.foundation.config.Configs

class ManifestParseException(message: String) : Exception(message)

/** Parses and validates a `module.toml` manifest, using BlueFoundation's TOML config parser. */
object ManifestParser {

    private const val SUPPORTED_SCHEMA = 1
    private val ID_PATTERN = Regex("^[a-z0-9_]+$")
    private val REQUIREMENT_TYPES = setOf("PLUGIN", "MODULE", "PERMISSION", "WORLD_RESOURCE", "RESOURCE_PACK", "OTHER")

    fun parse(source: String): ModuleManifest {
        val document = try {
            Configs.parse(source, ConfigFormat.TOML)
        } catch (e: ConfigParseException) {
            throw ManifestParseException("module.toml is not valid TOML (line ${e.line()}, column ${e.column()}): ${e.message}")
        }
        return parseValidated(document.rootSection())
    }

    private fun parseValidated(root: ConfigSection): ModuleManifest {
        val schema = root.getInt("schema", -1)
        if (schema != SUPPORTED_SCHEMA) {
            throw ManifestParseException("Unsupported manifest schema: $schema (expected $SUPPORTED_SCHEMA)")
        }

        val id = requireString(root, "id")
        if (!ID_PATTERN.matches(id)) {
            throw ManifestParseException("Invalid module id '$id': must match [a-z0-9_]+")
        }

        val typeName = requireString(root, "type")
        val type = ModuleType.fromString(typeName)
            ?: throw ManifestParseException("Unknown module type '$typeName' (expected MINIGAME or MICROGAME)")

        val entry = requireString(root, "entry")
        if (!entry.startsWith("src/") || !entry.endsWith(".lua")) {
            throw ManifestParseException("entry must be a .lua file under src/, got '$entry'")
        }

        val api = requireString(root, "api")
        if (!UniversalApiRange.isValidFormat(api)) {
            throw ManifestParseException("Invalid api range '$api': must be a caret range like \"^1.0\" (major.minor only)")
        }

        return ModuleManifest(
            schema = schema,
            id = id,
            name = requireString(root, "name"),
            version = requireString(root, "version"),
            type = type,
            api = api,
            entry = entry,
            authors = root.getStringList("authors"),
            description = root.getString("description", ""),
            website = optionalString(root, "website"),
            license = optionalString(root, "license"),
            permissions = root.getStringList("permissions"),
            platforms = parsePlatforms(root),
            materials = parseMaterials(root),
            replaces = parseReplaces(root),
            dependencies = root.getStringList("dependencies"),
            softDependencies = root.getStringList("softDependencies"),
            setup = parseSetup(root),
        )
    }

    private fun requireString(section: ConfigSection, key: String): String {
        if (!section.contains(key)) throw ManifestParseException("module.toml is missing required field '$key'")
        val value = section.getString(key, "")
        if (value.isBlank()) throw ManifestParseException("module.toml field '$key' must not be blank")
        return value
    }

    private fun optionalString(section: ConfigSection, key: String): String? =
        if (section.contains(key)) section.getString(key) else null

    /** [ConfigSection.section] returns null when [path] doesn't exist at all, not an empty section. */
    private fun optionalSection(root: ConfigSection, path: String): ConfigSection? {
        val section = root.section(path)
        return if (section != null && section.exists()) section else null
    }

    private fun parsePlatforms(root: ConfigSection): Map<String, PlatformSupport> {
        val platforms = optionalSection(root, "platforms") ?: return emptyMap()
        return platforms.keys().associateWith { platform ->
            when (val value = platforms.get(platform)) {
                is Boolean -> if (value) PlatformSupport.Supported("*") else PlatformSupport.Unsupported
                is String -> PlatformSupport.Supported(value)
                else -> throw ManifestParseException(
                    "platforms.$platform must be a version range string or a boolean, got: $value"
                )
            }
        }
    }

    private fun parseMaterials(root: ConfigSection): Map<String, Map<String, String>> {
        val materials = optionalSection(root, "materials") ?: return emptyMap()
        return materials.keys().associateWith { logicalName ->
            val overrides = optionalSection(materials, logicalName) ?: return@associateWith emptyMap()
            overrides.keys().associateWith { overrides.getString(it, "") }
        }
    }

    private fun parseReplaces(root: ConfigSection): LegacyReplacement? {
        val replaces = optionalSection(root, "replaces") ?: return null
        val legacyId = replaces.getString("legacyId", "")
        if (legacyId.isBlank()) {
            throw ManifestParseException("replaces.legacyId must not be blank when 'replaces' is present")
        }
        return LegacyReplacement(legacyId, optionalString(replaces, "minLegacyVersion"))
    }

    private fun parseSetup(root: ConfigSection): ManifestSetupSpec? {
        if (optionalSection(root, "setup") == null) return null

        val installRequirements = tableArray(root, "setup.installRequirements").map { parseInstallRequirement(it) }
        val steps = tableArray(root, "setup.steps").map { parseSetupStep(it) }
        val commands = tableArray(root, "setup.commands").map { parseSetupCommand(it) }

        return ManifestSetupSpec(installRequirements, steps, commands)
    }

    private fun tableArray(root: ConfigSection, path: String): List<Map<String, Any?>> =
        (root.getList(path) ?: emptyList()).map { entry ->
            @Suppress("UNCHECKED_CAST")
            (entry as? Map<String, Any?>) ?: throw ManifestParseException("$path entries must be tables, got: $entry")
        }

    private fun requireMapString(map: Map<String, Any?>, key: String, arrayName: String): String {
        val value = map[key] as? String
        if (value.isNullOrBlank()) throw ManifestParseException("setup.$arrayName[].$key must be a non-blank string")
        return value
    }

    private fun mapStringList(map: Map<String, Any?>, key: String): List<String> =
        (map[key] as? List<*>)?.map { it.toString() } ?: emptyList()

    private fun parseInstallRequirement(map: Map<String, Any?>): ManifestInstallRequirement {
        val type = requireMapString(map, "type", "installRequirements")
        if (type !in REQUIREMENT_TYPES) {
            throw ManifestParseException("setup.installRequirements[].type must be one of $REQUIREMENT_TYPES, got '$type'")
        }
        return ManifestInstallRequirement(
            id = requireMapString(map, "id", "installRequirements"),
            type = type,
            required = (map["required"] as? Boolean) ?: true,
            displayName = requireMapString(map, "displayName", "installRequirements"),
            description = (map["description"] as? String) ?: "",
            installHint = map["installHint"] as? String,
        )
    }

    private fun parseSetupStep(map: Map<String, Any?>): ManifestSetupStep = ManifestSetupStep(
        id = requireMapString(map, "id", "steps"),
        required = (map["required"] as? Boolean) ?: true,
        displayName = requireMapString(map, "displayName", "steps"),
        description = (map["description"] as? String) ?: "",
        commandExamples = mapStringList(map, "commandExamples"),
        expectedInput = map["expectedInput"] as? String,
    )

    private fun parseSetupCommand(map: Map<String, Any?>): ManifestSetupCommand = ManifestSetupCommand(
        name = requireMapString(map, "name", "commands"),
        usage = (map["usage"] as? String) ?: "",
        description = (map["description"] as? String) ?: "",
        requiredHint = (map["requiredHint"] as? Boolean) ?: false,
    )
}

/** Format-only caret-range check, e.g. `^1.0`. Actual compatibility is checked by the runtime at load time. */
private object UniversalApiRange {
    private val PATTERN = Regex("^\\^(\\d+)\\.(\\d+)$")
    fun isValidFormat(range: String): Boolean = PATTERN.matches(range)
}
