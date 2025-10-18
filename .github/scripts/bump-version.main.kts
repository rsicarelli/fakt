#!/usr/bin/env kotlin

/**
 * Version Bump Script for Fakt
 *
 * Usage:
 *   kotlin bump-version.main.kts <bump_type> [current_version]
 *
 * Arguments:
 *   bump_type: major, minor, or patch
 *   current_version: (optional) If not provided, reads from gradle.properties
 *
 * Examples:
 *   kotlin bump-version.main.kts minor           # Reads 1.2.0 from gradle.properties → 1.3.0
 *   kotlin bump-version.main.kts patch 1.2.3     # 1.2.3 → 1.2.4
 *   kotlin bump-version.main.kts major 1.2.3     # 1.2.3 → 2.0.0
 */

import java.io.File
import kotlin.system.exitProcess

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val suffix: String = ""
) {
    override fun toString(): String = "$major.$minor.$patch$suffix"

    fun bump(type: String): SemanticVersion = when (type.lowercase()) {
        "major" -> copy(major = major + 1, minor = 0, patch = 0, suffix = "")
        "minor" -> copy(minor = minor + 1, patch = 0, suffix = "")
        "patch" -> copy(patch = patch + 1, suffix = "")
        else -> throw IllegalArgumentException("Invalid bump type: $type. Must be major, minor, or patch.")
    }

    companion object {
        fun parse(version: String): SemanticVersion {
            // Remove SNAPSHOT suffix if present
            val cleanVersion = version.replace("-SNAPSHOT", "")
            val parts = cleanVersion.split(".")

            require(parts.size == 3) {
                "Invalid version format: $version. Expected format: X.Y.Z or X.Y.Z-SNAPSHOT"
            }

            return SemanticVersion(
                major = parts[0].toInt(),
                minor = parts[1].toInt(),
                patch = parts[2].toInt(),
                suffix = if (version.contains("-SNAPSHOT")) "-SNAPSHOT" else ""
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
    if (args.isEmpty()) {
        println("Error: Bump type argument required")
        println("Usage: kotlin bump-version.main.kts <major|minor|patch> [current_version]")
        exitProcess(1)
    }

    val bumpType = args[0]
    val gradlePropertiesFile = File("gradle.properties")

    if (!gradlePropertiesFile.exists()) {
        println("Error: gradle.properties not found in current directory")
        exitProcess(1)
    }

    val currentVersion = if (args.size > 1) {
        SemanticVersion.parse(args[1])
    } else {
        readCurrentVersion(gradlePropertiesFile)
    }

    val newVersion = currentVersion.bump(bumpType)

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
