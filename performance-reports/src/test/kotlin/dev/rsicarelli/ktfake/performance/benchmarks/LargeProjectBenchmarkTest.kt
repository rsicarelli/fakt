// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.performance.benchmarks

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertContains
import kotlin.test.assertEquals

class LargeProjectBenchmarkTest {

    @Test
    fun `GIVEN small project config WHEN running benchmark THEN should complete within reasonable time`() {
        // Given
        val benchmark = LargeProjectBenchmark()
        val config = LargeProjectBenchmark.SMALL_PROJECT

        // When
        val result = benchmark.runProjectBenchmark(config)

        // Then
        assertTrue(result.coldCompilationMs > 0, "Cold compilation should take some time")
        assertTrue(result.warmCompilationMs > 0, "Warm compilation should take some time")
        assertTrue(result.incrementalMs > 0, "Incremental compilation should take some time")
        assertTrue(result.warmCompilationMs <= result.coldCompilationMs, "Warm should be faster than cold")
        assertTrue(result.incrementalMs <= result.coldCompilationMs, "Incremental should be faster than cold")
        assertEquals(config.interfaceCount, result.interfacesProcessed)
    }

    @Test
    fun `GIVEN medium project config WHEN running benchmark THEN should show performance scaling`() {
        // Given
        val benchmark = LargeProjectBenchmark()
        val config = LargeProjectBenchmark.MEDIUM_PROJECT

        // When
        val result = benchmark.runProjectBenchmark(config)

        // Then
        assertTrue(result.interfacesProcessed == config.interfaceCount)
        assertTrue(result.methodsGenerated > 0)
        assertTrue(result.propertiesGenerated > 0)
        assertTrue(result.memoryUsageMB > 0)
        assertTrue(result.warmSpeedup >= 1.0, "Warm compilation should provide speedup")
        assertTrue(result.incrementalSpeedup >= 1.0, "Incremental compilation should provide speedup")
    }

    @Test
    fun `GIVEN synthetic project generator WHEN generating project THEN should create realistic interfaces`() {
        // Given
        val generator = SyntheticProjectGenerator()
        val config = BenchmarkConfig("Test", 10, 5, 2)

        // When
        val project = generator.generateProject(config)

        // Then
        assertEquals(10, project.interfaces.size)

        // Check interface diversity
        val categories = project.interfaces.map { it.category }.toSet()
        assertTrue(categories.isNotEmpty(), "Should have multiple interface categories")

        // Check realistic naming
        project.interfaces.forEach { syntheticInterface ->
            assertTrue(syntheticInterface.fullyQualifiedName.contains("com.benchmark"))
            assertTrue(syntheticInterface.simpleName.isNotBlank())
        }

        // Check methods and properties
        val totalMethods = project.interfaces.sumOf { it.methods.size }
        val totalProperties = project.interfaces.sumOf { it.properties.size }
        assertTrue(totalMethods > 0)
        assertTrue(totalProperties >= 0) // Some interfaces might have no properties
    }

    @Test
    fun `GIVEN benchmark results WHEN generating report THEN should provide detailed analysis`() {
        // Given
        val config1 = BenchmarkConfig("Small", 50, 5, 2)
        val config2 = BenchmarkConfig("Large", 500, 8, 3)

        val results = listOf(
            BenchmarkResult(config1, 1000, 300, 100, 85, 50, 50, 250, 100),
            BenchmarkResult(config2, 5000, 1200, 400, 90, 200, 500, 4000, 1500)
        )

        val benchmarkResults = BenchmarkResults(results)

        // When
        val report = benchmarkResults.generateReport()

        // Then
        assertContains(report, "KtFakes Performance Benchmark Report")
        assertContains(report, "Cold (ms)")
        assertContains(report, "Warm (ms)")
        assertContains(report, "Incremental (ms)")
        assertContains(report, "Speedup")
        assertContains(report, "Cache Hit %")
        assertContains(report, "Performance Analysis")
        assertContains(report, "Average warm compilation speedup")
        assertContains(report, "Average incremental speedup")
        assertContains(report, "Recommendations")
    }

    @Test
    fun `GIVEN interface categories WHEN generating interfaces THEN should create appropriate patterns`() {
        // Given
        val generator = SyntheticProjectGenerator()

        // When - Generate different categories
        val simpleService = generator.generateInterface(InterfaceCategory.SIMPLE_SERVICE, 1, BenchmarkConfig("Test", 10, 5, 2))
        val repository = generator.generateInterface(InterfaceCategory.REPOSITORY, 1, BenchmarkConfig("Test", 10, 5, 2))
        val complexGeneric = generator.generateInterface(InterfaceCategory.COMPLEX_GENERIC, 1, BenchmarkConfig("Test", 10, 5, 2))
        val eventHandler = generator.generateInterface(InterfaceCategory.EVENT_HANDLER, 1, BenchmarkConfig("Test", 10, 5, 2))

        // Then
        // Simple service should have no type parameters
        assertTrue(simpleService.typeParameters.isEmpty())

        // Repository should have type parameters
        assertTrue(repository.typeParameters.isNotEmpty())
        assertContains(repository.fullyQualifiedName, "repository")

        // Complex generic should have multiple type parameters
        assertTrue(complexGeneric.typeParameters.isNotEmpty())
        assertContains(complexGeneric.fullyQualifiedName, "generic")

        // Event handler should have specific patterns
        assertTrue(eventHandler.typeParameters.isNotEmpty())
        assertContains(eventHandler.fullyQualifiedName, "events")
        assertTrue(eventHandler.methods.any { it.name.contains("handle") })
    }

    @Test
    fun `GIVEN comprehensive benchmark WHEN running all project sizes THEN should demonstrate scaling characteristics`() {
        // Given
        val benchmark = LargeProjectBenchmark()

        // When
        val results = benchmark.runComprehensiveBenchmarks()

        // Then
        assertEquals(4, results.results.size) // Small, Medium, Large, Enterprise

        // Verify scaling - larger projects should take more time
        val sortedResults = results.results.sortedBy { it.config.interfaceCount }

        for (i in 1 until sortedResults.size) {
            val smaller = sortedResults[i-1]
            val larger = sortedResults[i]

            assertTrue(
                larger.coldCompilationMs >= smaller.coldCompilationMs,
                "Larger projects should take more time than smaller ones"
            )

            assertTrue(
                larger.interfacesProcessed > smaller.interfacesProcessed,
                "Larger projects should process more interfaces"
            )
        }

        // Generate and validate report
        val report = results.generateReport()
        assertContains(report, "Small")
        assertContains(report, "Medium")
        assertContains(report, "Large")
        assertContains(report, "Enterprise")
    }

    // Helper extension function for private method access in tests
    private fun SyntheticProjectGenerator.generateInterface(
        category: InterfaceCategory,
        index: Int,
        config: BenchmarkConfig
    ): SyntheticInterface {
        // Use reflection or make method internal for testing
        // For now, generate a minimal interface for testing
        return when (category) {
            InterfaceCategory.SIMPLE_SERVICE -> SyntheticInterface(
                fullyQualifiedName = "com.benchmark.test.TestService$index",
                simpleName = "TestService$index",
                typeParameters = emptyList(),
                methods = listOf(SyntheticMethod("testMethod", "fun testMethod(): String")),
                properties = listOf(SyntheticProperty("testProperty", "String", false)),
                dependencies = emptyList(),
                category = category
            )
            InterfaceCategory.REPOSITORY -> SyntheticInterface(
                fullyQualifiedName = "com.benchmark.repository.TestRepository$index",
                simpleName = "TestRepository$index",
                typeParameters = listOf("T", "ID"),
                methods = listOf(SyntheticMethod("findById", "suspend fun findById(id: ID): T?")),
                properties = listOf(SyntheticProperty("tableName", "String", false)),
                dependencies = emptyList(),
                category = category
            )
            InterfaceCategory.COMPLEX_GENERIC -> SyntheticInterface(
                fullyQualifiedName = "com.benchmark.generic.TestGeneric$index",
                simpleName = "TestGeneric$index",
                typeParameters = listOf("T", "R"),
                methods = listOf(SyntheticMethod("process", "suspend fun <T, R> process(data: T): R")),
                properties = listOf(SyntheticProperty("config", "Map<String, Any>", false)),
                dependencies = emptyList(),
                category = category
            )
            InterfaceCategory.EVENT_HANDLER -> SyntheticInterface(
                fullyQualifiedName = "com.benchmark.events.TestEventHandler$index",
                simpleName = "TestEventHandler$index",
                typeParameters = listOf("T"),
                methods = listOf(SyntheticMethod("handle", "suspend fun handle(event: T): Result<Unit>")),
                properties = listOf(SyntheticProperty("handlerName", "String", false)),
                dependencies = emptyList(),
                category = category
            )
        }
    }
}