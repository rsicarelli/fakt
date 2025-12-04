#!/usr/bin/env kotlin

/**
 * Documentation Version Sync Script for Fakt
 *
 * Automatically updates all hardcoded Fakt versions in documentation files
 * to match the current VERSION_NAME from gradle.properties
 *
 * Usage:
 *   kotlin sync-docs-version.main.kts [version]
 *
 * Arguments:
 *   version: (optional) If not provided, reads from gradle.properties
 *
 * Examples:
 *   kotlin sync-docs-version.main.kts           # Use gradle.properties version
 *   kotlin sync-docs-version.main.kts 1.0.0     # Use specific version
 */

import java.io.File
import kotlin.system.exitProcess

fun readCurrentVersion(gradlePropertiesFile: File): String {
    val content = gradlePropertiesFile.readText()
    val versionLine = content.lines().find { it.startsWith("VERSION_NAME=") }
        ?: error("VERSION_NAME not found in gradle.properties")

    return versionLine.substringAfter("VERSION_NAME=").trim()
}

fun syncVersionCatalog(projectDir: File, newVersion: String) {
    val versionCatalogFile = File(projectDir, "gradle/libs.versions.toml")
    if (!versionCatalogFile.exists()) return

    var content = versionCatalogFile.readText()
    val pattern = Regex("""(fakt\s*=\s*)"[^"]*"""")

    if (pattern.containsMatchIn(content)) {
        content = content.replace(pattern, """$1"$newVersion"""")
        versionCatalogFile.writeText(content)
        println("✅ Updated gradle/libs.versions.toml")
    }
}

fun syncDocumentationVersions(projectDir: File, newVersion: String) {
    var filesUpdated = 0
    var totalReplacements = 0

    // First, sync version catalog
    syncVersionCatalog(projectDir, newVersion)

    // Files to update
    val filesToUpdate = mutableListOf<File>()

    // Documentation files
    projectDir.walkTopDown().filter { file ->
        file.isFile && file.name.endsWith(".md") &&
        !file.path.contains("/build/") && !file.path.contains("/.")
    }.forEach { filesToUpdate.add(it) }

    // Sample build files that still use hardcoded versions
    projectDir.walkTopDown().filter { file ->
        file.isFile && file.name == "build.gradle.kts" &&
        file.path.contains("/samples/")
    }.forEach { filesToUpdate.add(it) }

    // Get current versions from version catalog for consistent documentation
    val versionCatalogFile = File(projectDir, "gradle/libs.versions.toml")
    val versions = if (versionCatalogFile.exists()) {
        val content = versionCatalogFile.readText()
        mapOf(
            "kotlin" to (Regex("""kotlin\s*=\s*"([^"]+)"""").find(content)?.groupValues?.get(1) ?: "2.2.20"),
            "coroutines" to (Regex("""coroutines\s*=\s*"([^"]+)"""").find(content)?.groupValues?.get(1) ?: "1.10.2")
        )
    } else {
        mapOf("kotlin" to "2.2.20", "coroutines" to "1.10.2")
    }

    // Patterns to replace (use version catalog versions for consistency)
    val replacementPatterns = listOf(
        // Plugin version declarations
        Regex("""(id\("com\.rsicarelli\.fakt"\)\s+version\s+)"[^"]*"(\s*)""") to """$1"$newVersion"$2""",

        // Version catalog entries in docs
        Regex("""(fakt\s*=\s*)"[^"]*"(\s*)""") to """$1"$newVersion"$2""",
        Regex("""(kotlin\s*=\s*)"[^"]*"(\s*)""") to """$1"${versions["kotlin"]}"$2""",
        Regex("""(coroutines\s*=\s*)"[^"]*"(\s*)""") to """$1"${versions["coroutines"]}"$2""",

        // Maven coordinates (supports alpha/beta/stable formats)
        Regex("""(com\.rsicarelli\.fakt:[\w-]+:)"[^"]*"""") to """$1"$newVersion"""",
        Regex("""(com\.rsicarelli\.fakt:[\w-]+:)[^"\s)]+(?=\s|\)|$)""") to """$1$newVersion""",

        // Documentation examples (supports alpha/beta/stable + optional)
        Regex("""(- \*\*Fakt\*\*:\s+)[^\s+]+(\+?\s*)""") to """$1$newVersion+$2""",
        Regex("""(Using Fakt\s+)[^\s+]+(\+?\s*)""") to """$1$newVersion+$2""",

        // Multiplatform version examples
        Regex("""(kotlin\("multiplatform"\)\s+version\s+)"[^"]*"(\s*)""") to """$1"${versions["kotlin"]}"$2""",

        // Alpha/Beta/Stable specific patterns
        Regex("""(fakt.*version.*)"[^"]*"(\s*)""") to """$1"$newVersion"$2""",
        Regex("""(Fakt.*:\s*)[^\s,\]]+""") to """$1$newVersion""",
    )

    filesToUpdate.forEach { file ->
        var content = file.readText()
        var fileModified = false

        replacementPatterns.forEach { (pattern, replacement) ->
            val matches = pattern.findAll(content).toList()
            if (matches.isNotEmpty()) {
                content = content.replace(pattern, replacement)
                fileModified = true
                totalReplacements += matches.size
            }
        }

        if (fileModified) {
            file.writeText(content)
            filesUpdated++
            println("✅ Updated ${file.relativeTo(projectDir)}")
        }
    }

    println("\n📊 Summary:")
    println("  Files updated: $filesUpdated")
    println("  Total replacements: $totalReplacements")
    println("  New version: $newVersion")
}

fun main(args: Array<String>) {
    val projectDir = File(".").absoluteFile
    val gradlePropertiesFile = File("gradle.properties")

    if (!gradlePropertiesFile.exists()) {
        println("Error: gradle.properties not found in current directory")
        exitProcess(1)
    }

    val newVersion = if (args.isNotEmpty()) {
        args[0]
    } else {
        readCurrentVersion(gradlePropertiesFile)
    }

    println("🔄 Syncing documentation versions to: $newVersion")

    syncDocumentationVersions(projectDir, newVersion)

    println("\n✅ Documentation sync completed!")
}

main(args)