#!/usr/bin/env kotlin

/**
 * Version Bump Script for Fakt
 *
 * Usage:
 *   kotlin bump-version.main.kts snapshot
 *   kotlin bump-version.main.kts <bump_type> <release_type> [current_version]
 *
 * SNAPSHOT Mode:
 *   snapshot: Adds -SNAPSHOT suffix to current version without commit
 *
 * Bump Mode Arguments:
 *   bump_type: major, minor, or patch
 *   release_type: alpha, beta, or stable
 *   current_version: (optional) If not provided, reads from gradle.properties
 *
 * Examples:
 *   kotlin bump-version.main.kts snapshot         # 1.0.0-alpha01 → 1.0.0-alpha01-SNAPSHOT
 *   kotlin bump-version.main.kts minor alpha     # 1.0.0-alpha → 1.1.0-alpha
 *   kotlin bump-version.main.kts patch beta      # 1.0.0-alpha → 1.0.0-beta
 *   kotlin bump-version.main.kts patch stable    # 1.0.0-beta → 1.0.0
 *   kotlin bump-version.main.kts major stable    # 1.0.0 → 2.0.0
 */

import java.io.File
import kotlin.system.exitProcess

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: String? = null,
    val increment: Int? = null
) {
    override fun toString(): String = when {
        preRelease != null && increment != null -> "$major.$minor.$patch-$preRelease${increment.toString().padStart(2, '0')}"
        preRelease != null -> "$major.$minor.$patch-$preRelease"
        else -> "$major.$minor.$patch"
    }

    fun bump(bumpType: String, releaseType: String): SemanticVersion {
        // Validate bump type first
        if (bumpType.lowercase() !in listOf("major", "minor", "patch")) {
            throw IllegalArgumentException("Invalid bump type: $bumpType. Must be major, minor, or patch.")
        }

        return when (releaseType.lowercase()) {
            "alpha" -> {
                when {
                    // Current version has no pre-release (e.g., 1.0.0 -> 1.0.0-alpha01, 2.5.3 + minor -> 2.6.0-alpha01)
                    preRelease == null -> when (bumpType.lowercase()) {
                        "major" -> copy(major = major + 1, minor = 0, patch = 0, preRelease = "alpha", increment = 1)
                        "minor" -> copy(minor = minor + 1, patch = 0, preRelease = "alpha", increment = 1)
                        "patch" -> copy(preRelease = "alpha", increment = 1) // Keep current version frozen, add alpha01
                        else -> throw IllegalArgumentException("Invalid bump type: $bumpType. Must be major, minor, or patch.")
                    }

                    // Current is already alpha, increment the number (e.g., alpha01 -> alpha02)
                    preRelease == "alpha" -> copy(increment = (increment ?: 0) + 1)

                    // Current is beta, switch to alpha01 for next cycle
                    preRelease == "beta" -> when (bumpType.lowercase()) {
                        "major" -> copy(major = major + 1, minor = 0, patch = 0, preRelease = "alpha", increment = 1)
                        "minor" -> copy(minor = minor + 1, patch = 0, preRelease = "alpha", increment = 1)
                        "patch" -> copy(patch = patch + 1, preRelease = "alpha", increment = 1)
                        else -> throw IllegalArgumentException("Invalid bump type: $bumpType. Must be major, minor, or patch.")
                    }

                    else -> throw IllegalArgumentException("Invalid current pre-release: $preRelease")
                }
            }

            "beta" -> {
                when {
                    // Current version has no pre-release (e.g., 1.0.0 -> 1.0.0-beta01, 2.5.3 + minor -> 2.6.0-beta01)
                    preRelease == null -> when (bumpType.lowercase()) {
                        "major" -> copy(major = major + 1, minor = 0, patch = 0, preRelease = "beta", increment = 1)
                        "minor" -> copy(minor = minor + 1, patch = 0, preRelease = "beta", increment = 1)
                        "patch" -> copy(preRelease = "beta", increment = 1) // Keep current version frozen, add beta01
                        else -> throw IllegalArgumentException("Invalid bump type: $bumpType. Must be major, minor, or patch.")
                    }

                    // Current is alpha, switch to beta01
                    preRelease == "alpha" -> copy(preRelease = "beta", increment = 1)

                    // Current is already beta, increment the number (e.g., beta01 -> beta02)
                    preRelease == "beta" -> copy(increment = (increment ?: 0) + 1)

                    else -> throw IllegalArgumentException("Invalid current pre-release: $preRelease")
                }
            }

            "stable" -> {
                when {
                    // Release from pre-release (e.g., alpha03/beta02 -> 1.0.0)
                    preRelease != null -> copy(preRelease = null, increment = null)

                    // Normal version bump for stable releases
                    else -> when (bumpType.lowercase()) {
                        "major" -> copy(major = major + 1, minor = 0, patch = 0)
                        "minor" -> copy(minor = minor + 1, patch = 0)
                        "patch" -> copy(patch = patch + 1)
                        else -> throw IllegalArgumentException("Invalid bump type: $bumpType. Must be major, minor, or patch.")
                    }
                }
            }

            else -> throw IllegalArgumentException("Invalid release type: $releaseType. Must be alpha, beta, or stable.")
        }
    }

    companion object {
        fun parse(version: String): SemanticVersion {
            // Remove SNAPSHOT suffix if present
            val cleanVersion = version.replace("-SNAPSHOT", "")

            // Check for pre-release suffix (alpha, beta, etc.)
            val versionAndPreRelease = if (cleanVersion.contains("-")) {
                val parts = cleanVersion.split("-", limit = 2)
                parts[0] to parts[1]
            } else {
                cleanVersion to null
            }

            val versionParts = versionAndPreRelease.first.split(".")

            require(versionParts.size == 3) {
                "Invalid version format: $version. Expected format: X.Y.Z, X.Y.Z-alpha01, X.Y.Z-beta02, or with -SNAPSHOT suffix"
            }

            // Parse pre-release with optional increment (e.g., "alpha01", "beta02")
            var preRelease: String? = null
            var increment: Int? = null

            versionAndPreRelease.second?.let { preReleaseString ->
                // Check if it's exactly 2-digit increment format (e.g., "alpha01", "beta02")
                val incrementPattern = Regex("""^(alpha|beta)(\d{2})$""")
                val match = incrementPattern.find(preReleaseString)

                if (match != null) {
                    preRelease = match.groupValues[1]
                    increment = match.groupValues[2].toInt()
                } else {
                    // Legacy format without increment (e.g., "alpha", "beta")
                    // Validate that it's a supported pre-release type
                    if (preReleaseString !in listOf("alpha", "beta")) {
                        throw IllegalArgumentException("Invalid version format: $version. Unsupported pre-release type: $preReleaseString. Only 'alpha' and 'beta' are supported.")
                    }
                    preRelease = preReleaseString
                }
            }

            try {
                return SemanticVersion(
                    major = versionParts[0].toInt(),
                    minor = versionParts[1].toInt(),
                    patch = versionParts[2].toInt(),
                    preRelease = preRelease,
                    increment = increment
                )
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Invalid version format: $version. Version parts must be numeric.", e)
            }
        }
    }
}

fun readCurrentVersion(gradlePropertiesFile: File): SemanticVersion {
    val content = gradlePropertiesFile.readText()
    val versionLine = content.lines().find { it.startsWith("version=") }
        ?: error("version not found in gradle.properties")

    val versionString = versionLine.substringAfter("version=").trim()
    return SemanticVersion.parse(versionString)
}

fun updateGradleProperties(gradlePropertiesFile: File, newVersion: SemanticVersion) {
    val content = gradlePropertiesFile.readText()
    val updatedContent = content.lines().joinToString("\n") { line ->
        if (line.startsWith("version=")) {
            "version=$newVersion"
        } else {
            line
        }
    }
    gradlePropertiesFile.writeText(updatedContent)
}

fun updateGradlePropertiesWithString(gradlePropertiesFile: File, versionString: String) {
    val content = gradlePropertiesFile.readText()
    val updatedContent = content.lines().joinToString("\n") { line ->
        if (line.startsWith("version=")) {
            "version=$versionString"
        } else {
            line
        }
    }
    gradlePropertiesFile.writeText(updatedContent)
}

fun addSnapshotSuffix(version: SemanticVersion): String {
    val versionString = version.toString()
    return if (versionString.endsWith("-SNAPSHOT")) {
        versionString
    } else {
        "$versionString-SNAPSHOT"
    }
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Error: At least one argument required")
        println("Usage:")
        println("  kotlin bump-version.main.kts snapshot")
        println("  kotlin bump-version.main.kts <major|minor|patch> <alpha|beta|stable> [current_version]")
        exitProcess(1)
    }

    val gradlePropertiesFile = File("gradle.properties")
    if (!gradlePropertiesFile.exists()) {
        println("Error: gradle.properties not found in current directory")
        exitProcess(1)
    }

    // Handle SNAPSHOT mode
    if (args[0].lowercase() == "snapshot") {
        val currentVersion = readCurrentVersion(gradlePropertiesFile)
        val snapshotVersion = addSnapshotSuffix(currentVersion)

        // Update gradle.properties with SNAPSHOT version
        updateGradlePropertiesWithString(gradlePropertiesFile, snapshotVersion)

        // Output for GitHub Actions (if needed)
        println("VERSION_SNAPSHOT=$snapshotVersion")
        println("Current version converted to SNAPSHOT: $snapshotVersion")
        return
    }

    // Handle regular version bump mode
    if (args.size < 2) {
        println("Error: Bump type and release type arguments required")
        println("Usage: kotlin bump-version.main.kts <major|minor|patch> <alpha|beta|stable> [current_version]")
        exitProcess(1)
    }

    val bumpType = args[0]
    val releaseType = args[1]

    val currentVersion = if (args.size > 2) {
        SemanticVersion.parse(args[2])
    } else {
        readCurrentVersion(gradlePropertiesFile)
    }

    val newVersion = currentVersion.bump(bumpType, releaseType)

    // Update gradle.properties
    updateGradleProperties(gradlePropertiesFile, newVersion)

    // Output for GitHub Actions
    println("VERSION_CURRENT=$currentVersion")
    println("VERSION_NEW=$newVersion")
    println("TAG=v$newVersion")

    // Set GitHub Actions output if running in CI
    val githubOutput = System.getenv("GITHUB_OUTPUT")
    if (githubOutput != null) {
        File(githubOutput).appendText("""
            version_current=$currentVersion
            version_new=$newVersion
            tag=v$newVersion
        """.trimIndent())
    }
}

main(args)
