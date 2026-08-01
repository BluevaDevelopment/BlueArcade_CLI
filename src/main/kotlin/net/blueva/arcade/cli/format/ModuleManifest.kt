package net.blueva.arcade.cli.format

/** Mirrors `net.blueva.arcade.api.module.ModuleType` (kept independent so `cli/` never needs `BlueArcade-API`). */
enum class ModuleType {
    MINIGAME,
    MICROGAME;

    companion object {
        fun fromString(raw: String?): ModuleType? {
            if (raw == null) return null
            val normalized = raw.trim().uppercase()
            return entries.find { it.name == normalized }
        }
    }
}

/** Parsed, validated contents of a `.bamodule` archive's `module.toml` manifest. */
data class ModuleManifest(
    val schema: Int,
    val id: String,
    val name: String,
    val version: String,
    val type: ModuleType,
    val api: String,
    val entry: String,
    val authors: List<String>,
    val description: String,
    val website: String?,
    val license: String?,
    val permissions: List<String>,
    val platforms: Map<String, PlatformSupport>,
    val materials: Map<String, Map<String, String>>,
    val replaces: LegacyReplacement?,
    val dependencies: List<String>,
    val softDependencies: List<String>,
    val setup: ManifestSetupSpec?,
)

sealed class PlatformSupport {
    data class Supported(val versionRange: String) : PlatformSupport()
    object Unsupported : PlatformSupport()
}

data class LegacyReplacement(val legacyId: String, val minLegacyVersion: String?)

data class ManifestSetupSpec(
    val installRequirements: List<ManifestInstallRequirement>,
    val steps: List<ManifestSetupStep>,
    val commands: List<ManifestSetupCommand>,
)

data class ManifestInstallRequirement(
    val id: String,
    val type: String,
    val required: Boolean,
    val displayName: String,
    val description: String,
    val installHint: String?,
)

data class ManifestSetupStep(
    val id: String,
    val required: Boolean,
    val displayName: String,
    val description: String,
    val commandExamples: List<String>,
    val expectedInput: String?,
)

/** `requiredHint` is display-only, it does not gate whether an arena can be enabled. */
data class ManifestSetupCommand(
    val name: String,
    val usage: String,
    val description: String,
    val requiredHint: Boolean,
)
