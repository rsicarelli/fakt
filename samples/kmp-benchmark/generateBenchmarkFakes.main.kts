#!/usr/bin/env kotlin
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

/**
 * Generates 10,000 @Fake interfaces for benchmark testing using parallel batch processing.
 *
 * Strategy:
 * - Source: All @Fake interfaces from kmp-single-module
 * - Batches: 100 batches × 100 random samples = 10,000 fakes
 * - Parallel: Coroutines for concurrent batch generation
 * - Naming: Unique names with path-based prefixes + numerical suffixes
 *
 * Usage: kotlin generateBenchmarkFakes.main.kts
 */

import java.io.File
import kotlinx.coroutines.*
import kotlin.system.exitProcess
import kotlin.random.Random

// ══════════════════════════════════════════════════════════════════════════════
// Configuration
// ══════════════════════════════════════════════════════════════════════════════

val TOTAL_BATCHES = 100
val SAMPLES_PER_BATCH = 100
val SOURCE_MODULE = "../kmp-single-module"
val SOURCE_PACKAGE = "com.rsicarelli.fakt.samples.kmpSingleModule"
val TARGET_PACKAGE = "com.rsicarelli.fakt.samples.kmpBenchmark"
val TARGET_DIR = "src/commonMain/kotlin/com/rsicarelli/fakt/samples/kmpBenchmark"

// Model types that should never be suffixed (they're in the models/ package)
val MODEL_TYPES = setOf("User", "Product", "Priority", "JobStatus", "HttpStatus", "ValidationStatus")

// ══════════════════════════════════════════════════════════════════════════════
// Data Models
// ══════════════════════════════════════════════════════════════════════════════

data class InterfaceFile(
    val file: File,
    val originalName: String,
    val originalPackage: String,
    val content: String,
)

// ══════════════════════════════════════════════════════════════════════════════
// Main Execution
// ══════════════════════════════════════════════════════════════════════════════

fun main() = runBlocking {
    println("🚀 Fakt Benchmark Fake Generator")
    println("════════════════════════════════════════════════════════════════")
    println()

    // 1. Discover all @Fake interfaces
    val sourceInterfaces = discoverFakeInterfaces()
    if (sourceInterfaces.isEmpty()) {
        println("❌ No @Fake interfaces found in $SOURCE_MODULE")
        exitProcess(1)
    }

    println("✅ Found ${sourceInterfaces.size} source files")
    println("📊 Generating $TOTAL_BATCHES batches × $SAMPLES_PER_BATCH samples = ${TOTAL_BATCHES * SAMPLES_PER_BATCH} fakes")
    println()

    // 2. Generate batches in parallel
    val jobs = (1..TOTAL_BATCHES).map { batchNumber ->
        async(Dispatchers.Default) {
            val batchName = "batch%03d".format(batchNumber)
            val batchDir = File(TARGET_DIR, batchName)
            batchDir.mkdirs()

            // Pick random samples for this batch
            val batchSamples = sourceInterfaces.shuffled(Random(batchNumber)).take(SAMPLES_PER_BATCH)

            for ((index, sourceInterface) in batchSamples.withIndex()) {
                val fileNumber = (batchNumber - 1) * SAMPLES_PER_BATCH + index + 1

                val transformedContent = transformInterface(
                    sourceInterface = sourceInterface,
                    batchName = batchName,
                    fileNumber = fileNumber,
                )

                val targetFile = File(batchDir, "${sourceInterface.originalName}$fileNumber.kt")
                targetFile.writeText(transformedContent)
            }

            batchName
        }
    }

    // Wait for all batches to complete and show progress
    jobs.forEachIndexed { index, deferred ->
        val batchName = deferred.await()
        if ((index + 1) % 10 == 0) {
            println("✓ Completed ${index + 1}/$TOTAL_BATCHES batches...")
        }
    }

    println()
    println("════════════════════════════════════════════════════════════════")
    println("✨ Generation Complete!")
    println("   Total Batches: $TOTAL_BATCHES")
    println("   Total Files: ${TOTAL_BATCHES * SAMPLES_PER_BATCH}")
    println("   Location: $TARGET_DIR")
    println("════════════════════════════════════════════════════════════════")
}

// ══════════════════════════════════════════════════════════════════════════════
// Interface Discovery
// ══════════════════════════════════════════════════════════════════════════════

fun discoverFakeInterfaces(): List<InterfaceFile> {
    val sourceDir = File(SOURCE_MODULE, "src/commonMain/kotlin")
    if (!sourceDir.exists()) {
        println("❌ Source directory not found: ${sourceDir.absolutePath}")
        exitProcess(1)
    }

    val scenariosDir = File(sourceDir, "com/rsicarelli/fakt/samples/kmpSingleModule/scenarios")
    val interfaces = mutableListOf<InterfaceFile>()

    sourceDir.walkTopDown()
        .filter { it.isFile && it.extension == "kt" }
        .filter { !it.path.contains("/models/") } // Skip model files to avoid duplication
        .filter { it.path.contains("/scenarios/") } // Only include files from scenarios dir
        .forEach { file ->
            val content = file.readText()

            // Only include files with @Fake annotation
            if (!content.contains("@Fake")) {
                return@forEach
            }

            // Create unique name by incorporating subdirectory path
            val relativePath = file.relativeTo(scenariosDir).path
            val parts = relativePath.split("/")
            val fileName = parts.last().removeSuffix(".kt")
            val dirPath = parts.dropLast(1).joinToString("_")
            val primaryName = if (dirPath.isNotEmpty()) "${fileName}_${dirPath}" else fileName

            val packageName = extractPackageName(content)

            if (packageName != null) {
                interfaces.add(
                    InterfaceFile(
                        file = file,
                        originalName = primaryName,
                        originalPackage = packageName,
                        content = content,
                    )
                )
            }
        }

    return interfaces.sortedBy { it.originalName }
}

fun extractPackageName(content: String): String? {
    val packageRegex = """package\s+([\w.]+)""".toRegex()
    return packageRegex.find(content)?.groupValues?.get(1)
}

// ══════════════════════════════════════════════════════════════════════════════
// Content Transformation
// ══════════════════════════════════════════════════════════════════════════════

fun stripDocumentation(content: String): String {
    // Preserve copyright header (first 3 lines)
    val lines = content.lines()
    val copyrightLines = lines.take(3).takeWhile {
        it.startsWith("//") && (it.contains("Copyright") || it.contains("SPDX"))
    }

    var cleaned = content

    // Remove multi-line comments (/* */ and /** */)
    cleaned = cleaned.replace(Regex("""/\*[\s\S]*?\*/"""), "")

    // Remove single-line comments (// ...)
    cleaned = cleaned.replace(Regex("""//.*"""), "")

    // Clean up excessive blank lines (keep max 2 consecutive)
    cleaned = cleaned.replace(Regex("""\n{3,}"""), "\n\n")

    // Add copyright back at the top if it was removed
    if (copyrightLines.isNotEmpty() && !cleaned.startsWith("// Copyright")) {
        cleaned = copyrightLines.joinToString("\n") + "\n" + cleaned.trimStart()
    }

    return cleaned
}

fun transformInterface(
    sourceInterface: InterfaceFile,
    batchName: String,
    fileNumber: Int,
): String {
    // 0. Strip all documentation and comments first
    var content = stripDocumentation(sourceInterface.content)

    // 1. Update package declaration
    val newPackage = "$TARGET_PACKAGE.$batchName"
    content = content.replace(
        sourceInterface.originalPackage,
        newPackage,
    )

    // 2. Extract ALL types from this file and create unique mappings
    val interfaceNames = extractAllInterfaceNames(content)
    val typeNames = extractAllTypeNames(content)
    val allTypes = (interfaceNames + typeNames).distinct()

    // Create a mapping for each type to a unique name
    val typeMapping = mutableMapOf<String, String>()
    allTypes.forEachIndexed { index, typeName ->
        if (typeName !in MODEL_TYPES) {
            // Each type in the file gets the base name + file number + sub-index
            val newName = if (index == 0) {
                "${sourceInterface.originalName}$fileNumber"
            } else {
                "${sourceInterface.originalName}${fileNumber}_$index"
            }
            typeMapping[typeName] = newName
        }
    }

    // 3. Replace all type names (sorted by length to avoid partial matches)
    val sortedTypes = typeMapping.keys.sortedByDescending { it.length }
    for (oldName in sortedTypes) {
        val newName = typeMapping[oldName]!!

        // Replace declarations
        content = content.replace(
            """(fun\s+interface|interface|open\s+class|final\s+class|abstract\s+class|enum\s+class|sealed\s+class|sealed\s+interface|data\s+class|data\s+object|class)\s+$oldName\b""".toRegex(),
            "$1 $newName"
        )

        // Replace references
        content = content.replace(
            """\b$oldName\b""".toRegex(),
            newName
        )
    }

    // 4. Update imports
    val importRegex = """import\s+${Regex.escape(SOURCE_PACKAGE)}\.[\w.]+""".toRegex()
    content = importRegex.replace(content) { matchResult ->
        matchResult.value.replace(SOURCE_PACKAGE, TARGET_PACKAGE)
            .replace(Regex("""\.scenarios\.[\w.]+"""), ".$batchName")
    }

    // 5. Add imports for model types that are referenced in the content
    val modelImports = MODEL_TYPES.filter { modelType ->
        content.contains(Regex("""\b$modelType\b"""))
    }.map { "import $TARGET_PACKAGE.models.$it" }

    // Find package declaration and add imports after it
    if (modelImports.isNotEmpty()) {
        val packageLine = content.lines().indexOfFirst { it.startsWith("package ") }
        if (packageLine != -1) {
            val lines = content.lines().toMutableList()

            // Find where to insert imports (after package, before first non-blank line)
            var insertPos = packageLine + 1
            while (insertPos < lines.size && lines[insertPos].isBlank()) {
                insertPos++
            }

            // Add model imports
            modelImports.forEach { import ->
                if (!lines.any { it == import }) { // Avoid duplicates
                    lines.add(insertPos, import)
                    insertPos++
                }
            }

            content = lines.joinToString("\n")
        }
    }

    // 6. Remove model type declarations (they're in models/ package)
    for (modelType in MODEL_TYPES) {
        // Remove data class declarations with primary constructor
        content = content.replace(
            Regex("""data\s+class\s+$modelType\s*\([^)]*\)\s*""", RegexOption.MULTILINE),
            ""
        )

        // Remove enum class declarations
        content = content.replace(
            Regex("""enum\s+class\s+$modelType\s*\{[^}]*\}\s*""", RegexOption.MULTILINE),
            ""
        )

        // Remove any remaining class declarations (with body)
        content = content.replace(
            Regex("""(?:data\s+)?class\s+$modelType\s*(?:\([^)]*\))?\s*\{[^}]*\}\s*""", RegexOption.MULTILINE),
            ""
        )
    }

    // Clean up excessive blank lines again after removal
    content = content.replace(Regex("""\n{3,}"""), "\n\n")

    return content
}

fun extractAllInterfaceNames(content: String): List<String> {
    val interfaceRegex = """(?:fun\s+)?interface\s+(\w+)""".toRegex()
    return interfaceRegex.findAll(content)
        .map { it.groupValues[1] }
        .toList()
}

fun extractAllTypeNames(content: String): List<String> {
    val typeRegex = """(?:open\s+class|final\s+class|abstract\s+class|enum\s+class|sealed\s+class|sealed\s+interface|data\s+class|data\s+object|class)\s+(\w+)""".toRegex()
    return typeRegex.findAll(content)
        .map { it.groupValues[1] }
        .toList()
}

// ══════════════════════════════════════════════════════════════════════════════
// Execute
// ══════════════════════════════════════════════════════════════════════════════

main()
