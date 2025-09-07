// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

/**
 * Generator for cross-module dependency injection.
 *
 * Handles automatic dependency injection for interfaces marked with @Fake(dependencies = [...]).
 * Generates implementation classes with injected dependencies and configuration access.
 */
class CrossModuleDependencyGenerator {

    /**
     * Generate implementation with automatic dependency injection.
     */
    fun generateImplementationWithDependencies(
        interfaceName: String,
        dependencies: List<String>
    ): String {
        if (dependencies.isEmpty()) {
            return """
            internal class Fake${interfaceName}Impl : $interfaceName {
                // Implementation without dependencies
            }
            """.trimIndent()
        }

        val dependencyFields = dependencies.joinToString("\n    ") { dependency ->
            "private val ${dependency.replaceFirstChar { it.lowercase() }}: $dependency = fake$dependency()"
        }

        return """
        internal class Fake${interfaceName}Impl : $interfaceName {
            $dependencyFields
        }
        """.trimIndent()
    }

    /**
     * Generate configuration access methods for injected dependencies.
     */
    fun generateDependencyConfigurationAccess(
        interfaceName: String,
        dependencies: List<String>
    ): String {
        val accessMethods = dependencies.joinToString("\n    ") { dependency ->
            """
            fun get$dependency(): Fake${dependency}Impl =
                ${dependency.replaceFirstChar { it.lowercase() }} as Fake${dependency}Impl
            """.trimIndent()
        }

        return """
        // Configuration access for $interfaceName dependencies
        $accessMethods
        """.trimIndent()
    }

    /**
     * Generate dependency metadata for cross-module exchange.
     */
    fun generateDependencyMetadata(serviceInfos: Map<String, List<String>>): String {
        val metadataEntries = serviceInfos.map { (service, deps) ->
            """
            "$service": {
                "dependencies": [${deps.joinToString(", ") { "\"$it\"" }}]
            }
            """.trimIndent()
        }.joinToString(",\n    ")

        return """
        {
            "services": {
                $metadataEntries
            }
        }
        """.trimIndent()
    }

    /**
     * Analyze dependency graph for circular dependencies.
     */
    fun analyzeDependencyGraph(dependencies: Map<String, List<String>>): DependencyAnalysis {
        val circularPaths = mutableListOf<List<String>>()
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()

        fun detectCycles(node: String, path: MutableList<String>): Boolean {
            if (recursionStack.contains(node)) {
                // Found a cycle - extract the cycle from the path
                val cycleStart = path.indexOf(node)
                if (cycleStart >= 0) {
                    val cycle = path.subList(cycleStart, path.size) + listOf(node)
                    circularPaths.add(cycle)
                }
                return true
            }

            if (visited.contains(node)) {
                return false
            }

            visited.add(node)
            recursionStack.add(node)
            path.add(node)

            var hasCycle = false
            dependencies[node]?.forEach { dependency ->
                if (detectCycles(dependency, path)) {
                    hasCycle = true
                }
            }

            path.removeAt(path.size - 1)
            recursionStack.remove(node)

            return hasCycle
        }

        // Check for cycles starting from each unvisited node
        dependencies.keys.forEach { service ->
            if (!visited.contains(service)) {
                detectCycles(service, mutableListOf())
            }
        }

        return DependencyAnalysis(
            hasCircularDependencies = circularPaths.isNotEmpty(),
            circularPaths = circularPaths
        )
    }

    /**
     * Resolve build order using topological sort.
     */
    fun resolveBuildOrder(dependencies: Map<String, List<String>>): List<String> {
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

    /**
     * Generate factory function with dependency injection.
     */
    fun generateFactoryWithDependencyInjection(
        interfaceName: String,
        dependencies: List<String>
    ): String {
        return """
        fun fake$interfaceName(configure: Fake${interfaceName}Config.() -> Unit = {}): $interfaceName {
            return Fake${interfaceName}Impl().apply {
                Fake${interfaceName}Config(this).configure()
            }
        }
        """.trimIndent()
    }

    /**
     * Generate chainable configuration API for dependencies.
     */
    fun generateChainableConfigurationApi(
        interfaceName: String,
        dependencies: List<String>
    ): String {
        val configMethods = dependencies.joinToString("\n    ") { dependency ->
            """
            fun ${dependency.replaceFirstChar { it.lowercase() }}(configure: Fake${dependency}Config.() -> Unit) = apply {
                get$dependency().apply { Fake${dependency}Config(this).configure() }
            }
            """.trimIndent()
        }

        return """
        class Fake${interfaceName}Config(private val impl: Fake${interfaceName}Impl) {
            $configMethods
        }
        """.trimIndent()
    }

    /**
     * Generate cross-module implementation with proper visibility.
     */
    fun generateCrossModuleImplementation(
        interfaceName: String,
        dependencies: List<String>,
        isExternalDependency: Boolean
    ): String {
        val dependencyFields = dependencies.joinToString("\n    ") { dependency ->
            val visibility = if (isExternalDependency) "internal" else "private"
            "$visibility val ${dependency.replaceFirstChar { it.lowercase() }}: $dependency = fake$dependency()"
        }

        return """
        internal class Fake${interfaceName}Impl : $interfaceName {
            $dependencyFields
        }

        public fun fake$interfaceName(configure: Fake${interfaceName}Config.() -> Unit = {}): $interfaceName {
            return Fake${interfaceName}Impl().apply {
                Fake${interfaceName}Config(this).configure()
            }
        }
        """.trimIndent()
    }

    /**
     * Generate versioned dependency resolution.
     */
    fun generateVersionedDependencyResolution(
        interfaceName: String,
        dependencies: Map<String, String>
    ): String {
        val versionComments = dependencies.map { (service, version) ->
            "// Compatible with $service $version"
        }.joinToString("\n")

        val dependencyFields = dependencies.keys.joinToString("\n    ") { dependency ->
            "private val ${dependency.replaceFirstChar { it.lowercase() }}: $dependency = fake$dependency()"
        }

        return """
        $versionComments
        internal class Fake${interfaceName}Impl : $interfaceName {
            init {
                checkCompatibility()
            }

            $dependencyFields

            private fun checkCompatibility() {
                // Version compatibility validation
            }
        }
        """.trimIndent()
    }
}
