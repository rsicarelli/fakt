#!/usr/bin/env kotlin

/**
 * Version Bump Script for Fakt
 *
 * Usage:
 *   kotlin bump-version.main.kts <bump_type> <release_type> [current_version]
 *
 * Arguments:
 *   bump_type: major, minor, or patch
 *   release_type: alpha, beta, or stable
 *   current_version: (optional) If not provided, reads from gradle.properties
 *
 * Examples:
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
    val preRelease: String? = null
) {
    override fun toString(): String = when {
        preRelease != null -> "$major.$minor.$patch-$preRelease"
        else -> "$major.$minor.$patch"
    }

    fun bump(bumpType: String, releaseType: String): SemanticVersion {
        val newPreRelease = when (releaseType.lowercase()) {
            "alpha" -> "alpha"
            "beta" -> "beta"
            "stable" -> null
            else -> throw IllegalArgumentException("Invalid release type: $releaseType. Must be alpha, beta, or stable.")
        }

        return when (bumpType.lowercase()) {
            "major" -> copy(major = major + 1, minor = 0, patch = 0, preRelease = newPreRelease)
            "minor" -> copy(minor = minor + 1, patch = 0, preRelease = newPreRelease)
            "patch" -> copy(patch = patch + 1, preRelease = newPreRelease)
            else -> throw IllegalArgumentException("Invalid bump type: $bumpType. Must be major, minor, or patch.")
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
                "Invalid version format: $version. Expected format: X.Y.Z, X.Y.Z-alpha, X.Y.Z-beta, or with -SNAPSHOT suffix"
            }

            return SemanticVersion(
                major = versionParts[0].toInt(),
                minor = versionParts[1].toInt(),
                patch = versionParts[2].toInt(),
                preRelease = versionAndPreRelease.second
            )
        }
    }
}

fun readCurrentVersion(gradlePropertiesFile: File): SemanticVersion {
    val content = gradlePropertiesFile.readText()
    val versionLine = content.lines().find { it.startsWith("VERSION_NAME=") }
        ?: error("VERSION_NAME not found in gradle.properties")

    val versionString = versionLine.substringAfter("VERSION_NAME=").trim()
    return SemanticVersion.parse(versionString)
}

fun updateGradleProperties(gradlePropertiesFile: File, newVersion: SemanticVersion) {
    val content = gradlePropertiesFile.readText()
    val updatedContent = content.lines().joinToString("\n") { line ->
        if (line.startsWith("VERSION_NAME=")) {
            "VERSION_NAME=$newVersion"
        } else {
            line
        }
    }
    gradlePropertiesFile.writeText(updatedContent)
}

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Error: Bump type and release type arguments required")
        println("Usage: kotlin bump-version.main.kts <major|minor|patch> <alpha|beta|stable> [current_version]")
        exitProcess(1)
    }

    val bumpType = args[0]
    val releaseType = args[1]
    val gradlePropertiesFile = File("gradle.properties")

    if (!gradlePropertiesFile.exists()) {
        println("Error: gradle.properties not found in current directory")
        exitProcess(1)
    }

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
