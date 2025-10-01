// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.performance

import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Incremental compilation manager for KtFakes.
 *
 * Tracks interface changes and avoids regenerating unchanged fakes.
 * Inspired by Metro's incremental compilation patterns and Kotlin's own IC system.
 *
 * Key optimizations:
 * - Interface signature hashing to detect changes
 * - Dependency tracking for cascading invalidation
 * - Persistent cache across compilation sessions
 * - Smart invalidation strategies
 */
class IncrementalCompilationManager(
    private val cacheDir: File,
    private val performanceTracker: FaktPerformanceTracker? = null
) {
    companion object {
        private const val CACHE_VERSION = "1.0"
        private const val INTERFACE_CACHE_FILE = "interface-signatures.cache"
        private const val DEPENDENCY_CACHE_FILE = "dependencies.cache"
        private const val GENERATED_FILES_CACHE = "generated-files.cache"
    }

    // In-memory caches for fast access
    private val interfaceSignatures = ConcurrentHashMap<String, InterfaceSignature>()
    private val dependencyGraph = ConcurrentHashMap<String, Set<String>>()
    private val generatedFilesMap = ConcurrentHashMap<String, Set<String>>()

    // Cache files
    private val interfaceCacheFile = File(cacheDir, INTERFACE_CACHE_FILE)
    private val dependencyCacheFile = File(cacheDir, DEPENDENCY_CACHE_FILE)
    private val generatedFilesCacheFile = File(cacheDir, GENERATED_FILES_CACHE)

    init {
        cacheDir.mkdirs()
        loadCaches()
    }

    /**
     * Check if an interface needs regeneration based on signature changes.
     */
    fun needsRegeneration(interfaceInfo: InterfaceChangeInfo): RegenerationDecision {
        performanceTracker?.startPhase(CompilationPhase.INTERFACE_DISCOVERY)

        val interfaceFqName = interfaceInfo.fullyQualifiedName
        val currentSignature = computeInterfaceSignature(interfaceInfo)
        val cachedSignature = interfaceSignatures[interfaceFqName]

        val decision = when {
            cachedSignature == null -> {
                // New interface - needs generation
                RegenerationDecision.GENERATE_NEW("Interface not seen before")
            }

            cachedSignature.contentHash != currentSignature.contentHash -> {
                // Interface changed - needs regeneration
                val changedAspects = detectChangedAspects(cachedSignature, currentSignature)
                RegenerationDecision.REGENERATE_CHANGED("Interface changed: $changedAspects")
            }

            !generatedFilesExist(interfaceFqName) -> {
                // Generated files missing - needs regeneration
                RegenerationDecision.REGENERATE_MISSING("Generated files missing or deleted")
            }

            else -> {
                // No changes - can skip
                RegenerationDecision.SKIP_UNCHANGED("Interface unchanged since last compilation")
            }
        }

        performanceTracker?.endPhase(
            CompilationPhase.INTERFACE_DISCOVERY,
            mapOf(
                "interface" to interfaceFqName,
                "decision" to decision.type.name,
                "reason" to decision.reason
            )
        )

        return decision
    }

    /**
     * Record that an interface was successfully generated.
     */
    fun recordGeneration(
        interfaceInfo: InterfaceChangeInfo,
        generatedFiles: List<File>
    ) {
        val interfaceFqName = interfaceInfo.fullyQualifiedName
        val signature = computeInterfaceSignature(interfaceInfo)

        // Update caches
        interfaceSignatures[interfaceFqName] = signature
        generatedFilesMap[interfaceFqName] = generatedFiles.map { it.absolutePath }.toSet()

        // Update dependencies if interface has dependencies
        if (interfaceInfo.dependencies.isNotEmpty()) {
            dependencyGraph[interfaceFqName] = interfaceInfo.dependencies.toSet()
        }
    }

    /**
     * Find all interfaces that need regeneration due to dependency changes.
     */
    fun findDependentInterfaces(changedInterface: String): Set<String> {
        val dependents = mutableSetOf<String>()

        // Find direct dependents
        dependencyGraph.forEach { (interfaceName, dependencies) ->
            if (changedInterface in dependencies) {
                dependents.add(interfaceName)
            }
        }

        // Find transitive dependents (recursive)
        val transitivelyAffected = mutableSetOf<String>()
        val toProcess = dependents.toMutableSet()

        while (toProcess.isNotEmpty()) {
            val current = toProcess.first()
            toProcess.remove(current)

            if (current !in transitivelyAffected) {
                transitivelyAffected.add(current)

                // Find interfaces that depend on current
                dependencyGraph.forEach { (interfaceName, dependencies) ->
                    if (current in dependencies && interfaceName !in transitivelyAffected) {
                        toProcess.add(interfaceName)
                    }
                }
            }
        }

        return transitivelyAffected
    }

    /**
     * Clean up generated files for interfaces that are no longer present.
     */
    fun cleanupStaleFiles(currentInterfaces: Set<String>): List<File> {
        val staleInterfaces = interfaceSignatures.keys - currentInterfaces
        val deletedFiles = mutableListOf<File>()

        staleInterfaces.forEach { interfaceFqName ->
            val files = generatedFilesMap[interfaceFqName] ?: emptySet()
            files.forEach { filePath ->
                val file = File(filePath)
                if (file.exists() && file.delete()) {
                    deletedFiles.add(file)
                }
            }

            // Remove from caches
            interfaceSignatures.remove(interfaceFqName)
            generatedFilesMap.remove(interfaceFqName)
            dependencyGraph.remove(interfaceFqName)
        }

        return deletedFiles
    }

    /**
     * Save all caches to disk for persistence across compilations.
     */
    fun saveCaches() {
        try {
            // Save interface signatures
            interfaceCacheFile.writeText(
                buildString {
                    appendLine("# KtFakes Interface Signature Cache v$CACHE_VERSION")
                    interfaceSignatures.forEach { (fqName, signature) ->
                        appendLine("$fqName|${signature.contentHash}|${signature.timestamp}|${signature.memberCount}")
                    }
                }
            )

            // Save dependency graph
            dependencyCacheFile.writeText(
                buildString {
                    appendLine("# KtFakes Dependency Cache v$CACHE_VERSION")
                    dependencyGraph.forEach { (interfaceName, dependencies) ->
                        appendLine("$interfaceName|${dependencies.joinToString(",")}")
                    }
                }
            )

            // Save generated files mapping
            generatedFilesCacheFile.writeText(
                buildString {
                    appendLine("# KtFakes Generated Files Cache v$CACHE_VERSION")
                    generatedFilesMap.forEach { (interfaceName, files) ->
                        appendLine("$interfaceName|${files.joinToString(",")}")
                    }
                }
            )

        } catch (e: Exception) {
            // Don't fail compilation if cache save fails
            println("Warning: Could not save KtFakes incremental compilation cache: ${e.message}")
        }
    }

    /**
     * Get incremental compilation statistics for performance reporting.
     */
    fun getIncrementalStats(): IncrementalStats {
        val totalInterfaces = interfaceSignatures.size
        val cachedGeneratedFiles = generatedFilesMap.values.sumOf { it.size }
        val dependencyEdges = dependencyGraph.values.sumOf { it.size }

        return IncrementalStats(
            cachedInterfaces = totalInterfaces,
            generatedFiles = cachedGeneratedFiles,
            dependencyEdges = dependencyEdges,
            cacheHitPotential = if (totalInterfaces > 0) {
                // Estimate potential cache hits (interfaces that haven't changed)
                (totalInterfaces * 0.8).toInt() // Assume 80% don't change between builds
            } else 0
        )
    }

    private fun loadCaches() {
        try {
            // Load interface signatures
            if (interfaceCacheFile.exists()) {
                interfaceCacheFile.readLines()
                    .filter { it.startsWith("#").not() && it.isNotBlank() }
                    .forEach { line ->
                        val parts = line.split("|")
                        if (parts.size >= 4) {
                            val fqName = parts[0]
                            val hash = parts[1]
                            val timestamp = parts[2].toLongOrNull() ?: 0
                            val memberCount = parts[3].toIntOrNull() ?: 0

                            interfaceSignatures[fqName] = InterfaceSignature(
                                contentHash = hash,
                                timestamp = timestamp,
                                memberCount = memberCount
                            )
                        }
                    }
            }

            // Load dependency graph
            if (dependencyCacheFile.exists()) {
                dependencyCacheFile.readLines()
                    .filter { it.startsWith("#").not() && it.isNotBlank() }
                    .forEach { line ->
                        val parts = line.split("|", limit = 2)
                        if (parts.size == 2) {
                            val interfaceName = parts[0]
                            val dependencies = if (parts[1].isNotBlank()) {
                                parts[1].split(",").toSet()
                            } else emptySet()

                            dependencyGraph[interfaceName] = dependencies
                        }
                    }
            }

            // Load generated files mapping
            if (generatedFilesCacheFile.exists()) {
                generatedFilesCacheFile.readLines()
                    .filter { it.startsWith("#").not() && it.isNotBlank() }
                    .forEach { line ->
                        val parts = line.split("|", limit = 2)
                        if (parts.size == 2) {
                            val interfaceName = parts[0]
                            val files = if (parts[1].isNotBlank()) {
                                parts[1].split(",").toSet()
                            } else emptySet()

                            generatedFilesMap[interfaceName] = files
                        }
                    }
            }

        } catch (e: Exception) {
            // Don't fail compilation if cache load fails - just start fresh
            println("Warning: Could not load KtFakes incremental compilation cache: ${e.message}")
            interfaceSignatures.clear()
            dependencyGraph.clear()
            generatedFilesMap.clear()
        }
    }

    private fun computeInterfaceSignature(interfaceInfo: InterfaceChangeInfo): InterfaceSignature {
        val content = buildString {
            appendLine("interface:${interfaceInfo.fullyQualifiedName}")

            // Include type parameters
            interfaceInfo.typeParameters.sorted().forEach { typeParam ->
                appendLine("typeParam:$typeParam")
            }

            // Include methods with signatures
            interfaceInfo.methods.sortedBy { it.name }.forEach { method ->
                appendLine("method:${method.name}:${method.signature}")
            }

            // Include properties with types
            interfaceInfo.properties.sortedBy { it.name }.forEach { property ->
                appendLine("property:${property.name}:${property.type}:${property.isMutable}")
            }

            // Include dependencies
            interfaceInfo.dependencies.sorted().forEach { dependency ->
                appendLine("dependency:$dependency")
            }
        }

        val hash = MessageDigest.getInstance("SHA-256")
            .digest(content.toByteArray())
            .joinToString("") { "%02x".format(it) }

        return InterfaceSignature(
            contentHash = hash,
            timestamp = System.currentTimeMillis(),
            memberCount = interfaceInfo.methods.size + interfaceInfo.properties.size
        )
    }

    private fun detectChangedAspects(
        old: InterfaceSignature,
        new: InterfaceSignature
    ): List<String> {
        val changes = mutableListOf<String>()

        if (old.memberCount != new.memberCount) {
            changes.add("member count (${old.memberCount} → ${new.memberCount})")
        }

        // For more detailed change detection, we'd need to store more granular signatures
        // This is a simplified version that just detects that something changed
        changes.add("content signature")

        return changes
    }

    private fun generatedFilesExist(interfaceFqName: String): Boolean {
        val files = generatedFilesMap[interfaceFqName] ?: return false
        return files.all { File(it).exists() }
    }
}

/**
 * Information about an interface that might have changed.
 */
data class InterfaceChangeInfo(
    val fullyQualifiedName: String,
    val typeParameters: List<String>,
    val methods: List<MethodSignatureInfo>,
    val properties: List<PropertySignatureInfo>,
    val dependencies: List<String> // Other interfaces this depends on
)

data class MethodSignatureInfo(
    val name: String,
    val signature: String // Full method signature including types
)

data class PropertySignatureInfo(
    val name: String,
    val type: String,
    val isMutable: Boolean
)

/**
 * Compact signature of an interface for change detection.
 */
data class InterfaceSignature(
    val contentHash: String,
    val timestamp: Long,
    val memberCount: Int
)

/**
 * Decision about whether to regenerate a fake implementation.
 */
sealed class RegenerationDecision(val type: DecisionType, val reason: String) {
    class GenerateNew(reason: String) : RegenerationDecision(DecisionType.GENERATE_NEW, reason)
    class RegenerateChanged(reason: String) : RegenerationDecision(DecisionType.REGENERATE_CHANGED, reason)
    class RegenerateMissing(reason: String) : RegenerationDecision(DecisionType.REGENERATE_MISSING, reason)
    class SkipUnchanged(reason: String) : RegenerationDecision(DecisionType.SKIP_UNCHANGED, reason)

    companion object {
        fun GENERATE_NEW(reason: String) = GenerateNew(reason)
        fun REGENERATE_CHANGED(reason: String) = RegenerateChanged(reason)
        fun REGENERATE_MISSING(reason: String) = RegenerateMissing(reason)
        fun SKIP_UNCHANGED(reason: String) = SkipUnchanged(reason)
    }
}

enum class DecisionType {
    GENERATE_NEW,
    REGENERATE_CHANGED,
    REGENERATE_MISSING,
    SKIP_UNCHANGED
}

/**
 * Statistics about incremental compilation effectiveness.
 */
data class IncrementalStats(
    val cachedInterfaces: Int,
    val generatedFiles: Int,
    val dependencyEdges: Int,
    val cacheHitPotential: Int
)