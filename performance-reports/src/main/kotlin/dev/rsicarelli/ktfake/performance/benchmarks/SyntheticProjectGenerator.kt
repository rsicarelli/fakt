// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.performance.benchmarks

import kotlin.random.Random

/**
 * Generates synthetic large projects for performance benchmarking.
 *
 * Creates realistic interface structures with:
 * - Varying complexity (simple to complex generics)
 * - Different patterns (data access, services, repositories)
 * - Cross-interface dependencies
 * - Real-world naming conventions
 */
class SyntheticProjectGenerator {

    private val random = Random(42) // Fixed seed for reproducible benchmarks

    // Real-world interface patterns
    private val servicePatterns = listOf("Service", "Repository", "Handler", "Manager", "Controller", "Gateway")
    private val domainNames = listOf("User", "Order", "Product", "Payment", "Analytics", "Notification", "Auth", "Cache", "Search", "Report")
    private val commonMethods = listOf("get", "find", "create", "update", "delete", "save", "load", "process", "handle", "validate")
    private val commonProperties = listOf("isEnabled", "count", "size", "status", "lastUpdated", "configuration", "metadata")

    /**
     * Generate a synthetic project with realistic interfaces.
     */
    fun generateProject(config: BenchmarkConfig): SyntheticProject {
        val interfaces = mutableListOf<SyntheticInterface>()

        // Generate different categories of interfaces for realism
        val categoryDistribution = mapOf(
            InterfaceCategory.SIMPLE_SERVICE to 0.4,
            InterfaceCategory.REPOSITORY to 0.3,
            InterfaceCategory.COMPLEX_GENERIC to 0.2,
            InterfaceCategory.EVENT_HANDLER to 0.1
        )

        categoryDistribution.forEach { (category, percentage) ->
            val count = (config.interfaceCount * percentage).toInt()
            repeat(count) { index ->
                interfaces.add(generateInterface(category, index, config))
            }
        }

        // Add dependencies between interfaces (10% of interfaces depend on others)
        addInterfaceDependencies(interfaces)

        return SyntheticProject(interfaces)
    }

    /**
     * Generate a single interface based on category and configuration.
     */
    private fun generateInterface(
        category: InterfaceCategory,
        index: Int,
        config: BenchmarkConfig
    ): SyntheticInterface {
        val domainName = domainNames.random(random)
        val patternName = servicePatterns.random(random)

        return when (category) {
            InterfaceCategory.SIMPLE_SERVICE -> generateSimpleService(domainName, patternName, index, config)
            InterfaceCategory.REPOSITORY -> generateRepository(domainName, index, config)
            InterfaceCategory.COMPLEX_GENERIC -> generateComplexGeneric(domainName, patternName, index, config)
            InterfaceCategory.EVENT_HANDLER -> generateEventHandler(domainName, index, config)
        }
    }

    private fun generateSimpleService(
        domainName: String,
        patternName: String,
        index: Int,
        config: BenchmarkConfig
    ): SyntheticInterface {
        val className = "${domainName}${patternName}${index}"
        val fullyQualifiedName = "com.benchmark.${domainName.lowercase()}.${className}"

        val methods = generateMethods(config.avgMethodsPerInterface, complexityLevel = 1)
        val properties = generateProperties(config.avgPropertiesPerInterface, complexityLevel = 1)

        return SyntheticInterface(
            fullyQualifiedName = fullyQualifiedName,
            simpleName = className,
            typeParameters = emptyList(),
            methods = methods,
            properties = properties,
            dependencies = emptyList(),
            category = InterfaceCategory.SIMPLE_SERVICE
        )
    }

    private fun generateRepository(
        domainName: String,
        index: Int,
        config: BenchmarkConfig
    ): SyntheticInterface {
        val className = "${domainName}Repository${index}"
        val fullyQualifiedName = "com.benchmark.repository.${className}"

        val entityType = "${domainName}Entity"
        val idType = if (random.nextBoolean()) "String" else "Long"

        val methods = listOf(
            SyntheticMethod("findById", "suspend fun findById(id: $idType): $entityType?"),
            SyntheticMethod("findAll", "suspend fun findAll(): List<$entityType>"),
            SyntheticMethod("save", "suspend fun save(entity: $entityType): $entityType"),
            SyntheticMethod("deleteById", "suspend fun deleteById(id: $idType): Boolean"),
            SyntheticMethod("existsById", "suspend fun existsById(id: $idType): Boolean"),
            SyntheticMethod("count", "suspend fun count(): Long"),
            SyntheticMethod("findByFilter", "suspend fun findByFilter(filter: Map<String, Any>): List<$entityType>")
        ).take(config.avgMethodsPerInterface)

        val properties = listOf(
            SyntheticProperty("tableName", "String", false),
            SyntheticProperty("isTransactional", "Boolean", false),
            SyntheticProperty("cacheEnabled", "Boolean", false)
        ).take(config.avgPropertiesPerInterface)

        return SyntheticInterface(
            fullyQualifiedName = fullyQualifiedName,
            simpleName = className,
            typeParameters = listOf("T", "ID"),
            methods = methods,
            properties = properties,
            dependencies = emptyList(),
            category = InterfaceCategory.REPOSITORY
        )
    }

    private fun generateComplexGeneric(
        domainName: String,
        patternName: String,
        index: Int,
        config: BenchmarkConfig
    ): SyntheticInterface {
        val className = "${domainName}${patternName}${index}"
        val fullyQualifiedName = "com.benchmark.generic.${className}"

        val typeParameters = when (random.nextInt(3)) {
            0 -> listOf("T")
            1 -> listOf("T", "R")
            else -> listOf("T", "K", "V")
        }

        val methods = generateGenericMethods(config.avgMethodsPerInterface, typeParameters)
        val properties = generateProperties(config.avgPropertiesPerInterface, complexityLevel = 3)

        return SyntheticInterface(
            fullyQualifiedName = fullyQualifiedName,
            simpleName = className,
            typeParameters = typeParameters,
            methods = methods,
            properties = properties,
            dependencies = emptyList(),
            category = InterfaceCategory.COMPLEX_GENERIC
        )
    }

    private fun generateEventHandler(
        domainName: String,
        index: Int,
        config: BenchmarkConfig
    ): SyntheticInterface {
        val className = "${domainName}EventHandler${index}"
        val fullyQualifiedName = "com.benchmark.events.${className}"

        val methods = listOf(
            SyntheticMethod("handle", "suspend fun handle(event: ${domainName}Event): Result<Unit>"),
            SyntheticMethod("canHandle", "fun canHandle(eventType: String): Boolean"),
            SyntheticMethod("getPriority", "fun getPriority(): Int"),
            SyntheticMethod("onError", "suspend fun onError(event: ${domainName}Event, error: Throwable)")
        ).take(config.avgMethodsPerInterface)

        val properties = listOf(
            SyntheticProperty("handlerName", "String", false),
            SyntheticProperty("isAsync", "Boolean", false),
            SyntheticProperty("retryCount", "Int", false)
        ).take(config.avgPropertiesPerInterface)

        return SyntheticInterface(
            fullyQualifiedName = fullyQualifiedName,
            simpleName = className,
            typeParameters = listOf("T"),
            methods = methods,
            properties = properties,
            dependencies = emptyList(),
            category = InterfaceCategory.EVENT_HANDLER
        )
    }

    private fun generateMethods(count: Int, complexityLevel: Int): List<SyntheticMethod> {
        return (1..count).map { index ->
            val methodName = commonMethods.random(random) + if (complexityLevel > 1) index.toString() else ""
            val returnType = when (complexityLevel) {
                1 -> listOf("String", "Int", "Boolean", "Unit").random(random)
                2 -> listOf("List<String>", "Map<String, Any>", "Result<String>", "suspend Unit").random(random)
                else -> listOf("suspend Result<List<T>>", "Flow<T>", "suspend Pair<T, R>", "suspend Map<K, V>").random(random)
            }

            val isSuspend = returnType.contains("suspend")
            val signature = if (isSuspend) {
                "suspend fun $methodName(): $returnType"
            } else {
                "fun $methodName(): $returnType"
            }

            SyntheticMethod(methodName, signature, returnType)
        }
    }

    private fun generateGenericMethods(count: Int, typeParameters: List<String>): List<SyntheticMethod> {
        return (1..count).map { index ->
            val methodName = commonMethods.random(random) + index.toString()
            val usedTypeParam = typeParameters.random(random)

            val returnType = when (typeParameters.size) {
                1 -> "suspend Result<$usedTypeParam>"
                2 -> "suspend Result<${typeParameters[1]}>"
                else -> "suspend Map<${typeParameters[1]}, ${typeParameters[2]}>"
            }

            val signature = "suspend fun <${typeParameters.joinToString(", ")}> $methodName(data: $usedTypeParam): $returnType"

            SyntheticMethod(methodName, signature, returnType)
        }
    }

    private fun generateProperties(count: Int, complexityLevel: Int): List<SyntheticProperty> {
        return (1..count).map { index ->
            val propertyName = commonProperties.random(random) + if (complexityLevel > 1) index.toString() else ""
            val type = when (complexityLevel) {
                1 -> listOf("String", "Int", "Boolean").random(random)
                2 -> listOf("List<String>", "Map<String, Any>", "Optional<String>").random(random)
                else -> listOf("StateFlow<T>", "LiveData<T>", "Channel<T>").random(random)
            }

            val isMutable = random.nextBoolean()

            SyntheticProperty(propertyName, type, isMutable)
        }
    }

    /**
     * Add realistic dependencies between interfaces (repositories depend on entities, services depend on repositories, etc.)
     */
    private fun addInterfaceDependencies(interfaces: MutableList<SyntheticInterface>) {
        val repositories = interfaces.filter { it.category == InterfaceCategory.REPOSITORY }
        val services = interfaces.filter { it.category == InterfaceCategory.SIMPLE_SERVICE }

        // Services depend on repositories
        services.take(repositories.size).forEachIndexed { index, service ->
            if (index < repositories.size) {
                val repository = repositories[index]
                interfaces[interfaces.indexOf(service)] = service.copy(
                    dependencies = listOf(repository.fullyQualifiedName)
                )
            }
        }

        // Some complex generics depend on simple services
        val complexGenerics = interfaces.filter { it.category == InterfaceCategory.COMPLEX_GENERIC }
        complexGenerics.take(services.size / 2).forEachIndexed { index, generic ->
            if (index < services.size) {
                val service = services[index]
                interfaces[interfaces.indexOf(generic)] = generic.copy(
                    dependencies = listOf(service.fullyQualifiedName)
                )
            }
        }
    }
}

/**
 * Categories of interfaces for realistic benchmarking.
 */
enum class InterfaceCategory {
    SIMPLE_SERVICE,    // Basic business logic interfaces
    REPOSITORY,        // Data access layer interfaces
    COMPLEX_GENERIC,   // Interfaces with complex generic patterns
    EVENT_HANDLER      // Event-driven architecture interfaces
}

/**
 * Synthetic project containing generated interfaces.
 */
data class SyntheticProject(
    val interfaces: List<SyntheticInterface>
)

/**
 * Synthetic interface for benchmarking.
 */
data class SyntheticInterface(
    val fullyQualifiedName: String,
    val simpleName: String,
    val typeParameters: List<String>,
    val methods: List<SyntheticMethod>,
    val properties: List<SyntheticProperty>,
    val dependencies: List<String>,
    val category: InterfaceCategory
)

data class SyntheticMethod(
    val name: String,
    val signature: String,
    val returnType: String = "Unit"
)

data class SyntheticProperty(
    val name: String,
    val type: String,
    val isMutable: Boolean
)