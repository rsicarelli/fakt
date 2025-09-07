// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

/**
 * Manager for dependency metadata exchange across modules.
 *
 * Handles serialization, deserialization, and aggregation of dependency information
 * to support cross-module dependency injection and incremental compilation.
 */
class DependencyMetadataManager {

    /**
     * Serialize dependency information to portable format.
     */
    fun serializeDependencyInfo(serviceInfo: DependencyServiceInfo): String {
        return """
        {
            "serviceName": "${serviceInfo.serviceName}",
            "dependencies": [${serviceInfo.dependencies.joinToString(", ") { "\"$it\"" }}],
            "moduleName": "${serviceInfo.moduleName}",
            "packageName": "${serviceInfo.packageName}",
            "isPublic": ${serviceInfo.isPublic}
        }
        """.trimIndent()
    }

    /**
     * Deserialize dependency information from portable format.
     */
    fun deserializeDependencyInfo(serialized: String): DependencyServiceInfo {
        // Simple JSON-like parsing for test purposes
        val serviceName = extractJsonValue(serialized, "serviceName")
        val moduleName = extractJsonValue(serialized, "moduleName")
        val packageName = extractJsonValue(serialized, "packageName")
        val isPublic = extractJsonValue(serialized, "isPublic").toBoolean()
        val dependencies = extractJsonArray(serialized, "dependencies")

        return DependencyServiceInfo(
            serviceName = serviceName,
            dependencies = dependencies,
            moduleName = moduleName,
            packageName = packageName,
            isPublic = isPublic
        )
    }

    /**
     * Aggregate metadata from multiple modules.
     */
    fun aggregateModuleMetadata(metadataList: List<DependencyServiceInfo>): AggregatedMetadata {
        return AggregatedMetadata(services = metadataList)
    }

    /**
     * Validate dependencies against available services.
     */
    fun validateDependencies(
        serviceInfo: DependencyServiceInfo,
        availableServices: Set<String>
    ): DependencyValidation {
        val missing = serviceInfo.dependencies.filter { dependency ->
            !availableServices.contains(dependency)
        }
        return DependencyValidation(missingDependencies = missing)
    }

    /**
     * Detect metadata changes for incremental compilation.
     */
    fun detectMetadataChanges(
        original: List<DependencyServiceInfo>,
        updated: List<DependencyServiceInfo>
    ): MetadataChangeDetection {
        val originalServices = original.map { it.serviceName }.toSet()
        val updatedServices = updated.map { it.serviceName }.toSet()

        val newServices = (updatedServices - originalServices).toList()
        val removedServices = (originalServices - updatedServices).toList()

        val changedServices = updated.filter { updatedService ->
            original.find { it.serviceName == updatedService.serviceName }?.let { originalService ->
                originalService.dependencies != updatedService.dependencies
            } ?: false
        }.map { it.serviceName }

        val affectedServices = (changedServices + newServices + removedServices).distinct()

        return MetadataChangeDetection(
            hasChanges = affectedServices.isNotEmpty(),
            affectedServices = affectedServices,
            newServices = newServices
        )
    }

    /**
     * Determine build order from service metadata.
     */
    fun determineBuildOrder(services: List<DependencyServiceInfo>): List<String> {
        val dependencies = services.associate { service ->
            service.serviceName to service.dependencies
        }

        return resolveBuildOrder(dependencies)
    }

    /**
     * Persist metadata for incremental builds.
     */
    fun persistMetadataForIncremental(services: List<DependencyServiceInfo>): String {
        val timestamp = kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
        val servicesJson = services.joinToString(",\n        ") { service ->
            serializeDependencyInfo(service).replace("\n", "")
        }

        val checksum = servicesJson.hashCode().toString()

        return """
        {
            "timestamp": $timestamp,
            "checksum": "$checksum",
            "dependencies": [
                $servicesJson
            ]
        }
        """.trimIndent()
    }

    /**
     * Generate cross-module metadata respecting visibility.
     */
    fun generateCrossModuleMetadata(
        services: List<DependencyServiceInfo>,
        targetModule: String
    ): CrossModuleMetadata {
        val publicServices = services.filter { it.isPublic }
        return CrossModuleMetadata(services = publicServices)
    }

    /**
     * Check version compatibility between service metadata.
     */
    fun checkVersionCompatibility(
        v1Metadata: VersionedServiceInfo,
        v2Metadata: VersionedServiceInfo
    ): VersionCompatibility {
        val v1Parts = v1Metadata.version.split(".").map { it.toInt() }
        val v2Parts = v2Metadata.version.split(".").map { it.toInt() }

        // Simple semantic versioning check
        val majorChange = v2Parts[0] > v1Parts[0]
        val dependencyChange = v1Metadata.dependencies != v2Metadata.dependencies

        return VersionCompatibility(
            hasBreakingChanges = majorChange || dependencyChange,
            recommendedVersion = v2Metadata.version,
            migrationRequired = majorChange
        )
    }

    // Helper methods for simple JSON parsing
    private fun extractJsonValue(json: String, key: String): String {
        val regex = """"$key":\s*"([^"]*)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1) ?: ""
    }

    private fun extractJsonArray(json: String, key: String): List<String> {
        val regex = """"$key":\s*\[([^\]]*)\]""".toRegex()
        val arrayContent = regex.find(json)?.groupValues?.get(1) ?: ""
        return if (arrayContent.isEmpty()) {
            emptyList()
        } else {
            arrayContent.split(",").map { it.trim().removeSurrounding("\"") }
        }
    }

    private fun resolveBuildOrder(dependencies: Map<String, List<String>>): List<String> {
        val visited = mutableSetOf<String>()
        val result = mutableListOf<String>()

        fun topologicalSort(service: String) {
            if (visited.contains(service)) return
            visited.add(service)

            // Visit dependencies first (they need to be built before this service)
            dependencies[service]?.forEach { dependency ->
                topologicalSort(dependency)
            }

            // Add current service after its dependencies
            result.add(service)
        }

        dependencies.keys.forEach { service ->
            topologicalSort(service)
        }

        // Dependencies are added first, so no need to reverse
        return result
    }
}
