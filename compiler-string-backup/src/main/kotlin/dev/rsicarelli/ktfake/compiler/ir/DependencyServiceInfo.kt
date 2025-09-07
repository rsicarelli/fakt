// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

/**
 * Data class representing dependency information for a service.
 *
 * Contains all metadata needed for cross-module dependency injection and resolution.
 */
data class DependencyServiceInfo(
    val serviceName: String,
    val dependencies: List<String>,
    val moduleName: String,
    val packageName: String,
    val isPublic: Boolean = true
)

/**
 * Data class representing versioned service information.
 */
data class VersionedServiceInfo(
    val serviceName: String,
    val version: String,
    val dependencies: List<String>,
    val moduleName: String
)

/**
 * Data class representing dependency graph analysis results.
 */
data class DependencyAnalysis(
    val hasCircularDependencies: Boolean,
    val circularPaths: List<List<String>>
)

/**
 * Data class representing aggregated metadata from multiple modules.
 */
data class AggregatedMetadata(
    val services: List<DependencyServiceInfo>
) {
    fun hasService(serviceName: String): Boolean =
        services.any { it.serviceName == serviceName }

    fun hasDependency(serviceName: String, dependencyName: String): Boolean =
        services.find { it.serviceName == serviceName }
            ?.dependencies
            ?.contains(dependencyName) ?: false
}

/**
 * Data class representing dependency validation results.
 */
data class DependencyValidation(
    val missingDependencies: List<String>
)

/**
 * Data class representing metadata change detection results.
 */
data class MetadataChangeDetection(
    val hasChanges: Boolean,
    val affectedServices: List<String>,
    val newServices: List<String>
)

/**
 * Data class representing cross-module metadata.
 */
data class CrossModuleMetadata(
    val services: List<DependencyServiceInfo>
) {
    fun containsService(serviceName: String): Boolean =
        services.any { it.serviceName == serviceName }
}

/**
 * Data class representing version compatibility information.
 */
data class VersionCompatibility(
    val hasBreakingChanges: Boolean,
    val recommendedVersion: String,
    val migrationRequired: Boolean
)
