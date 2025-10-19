// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler

import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Service Loader validation tests to prevent classpath issues.
 *
 * These tests ensure that META-INF/services files are always in sync with actual class locations.
 * This prevents runtime ClassNotFoundException errors that occur when:
 * - Files are moved to different packages during refactoring
 * - Service loader files are not updated accordingly
 *
 * Real bug prevented: ClassNotFoundException for FaktCommandLineProcessor after
 * moving it from com.rsicarelli.fakt.compiler to com.rsicarelli.fakt.compiler.config
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServiceLoaderValidationTest {
    private val resourcesDir = File("src/main/resources")
    private val servicesDir = File(resourcesDir, "META-INF/services")

    @Test
    fun `GIVEN CommandLineProcessor service file WHEN reading class name THEN class should exist in correct package`() {
        // Given
        val serviceFile =
            File(
                servicesDir,
                "org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor",
            )
        assertTrue(
            serviceFile.exists(),
            "Service file should exist: ${serviceFile.absolutePath}",
        )

        // When
        val className = serviceFile.readText().trim()

        // Then - Class should exist and be loadable
        val loadedClass =
            try {
                Class.forName(className)
            } catch (e: ClassNotFoundException) {
                fail(
                    "Service loader references non-existent class: $className. " +
                        "This usually happens when a class is moved to a different package but " +
                        "META-INF/services was not updated. Error: ${e.message}",
                )
            }

        assertNotNull(loadedClass, "Class $className should be loadable")
        assertTrue(
            loadedClass.name == className,
            "Loaded class name should match service file entry",
        )
    }

    @Test
    fun `GIVEN CompilerPluginRegistrar service file WHEN reading class name THEN class should exist and be public`() {
        // Given
        val serviceFile =
            File(
                servicesDir,
                "org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar",
            )
        assertTrue(
            serviceFile.exists(),
            "Service file should exist: ${serviceFile.absolutePath}",
        )

        // When
        val className = serviceFile.readText().trim()

        // Then - Class should exist, be public, and instantiable
        val loadedClass =
            try {
                Class.forName(className)
            } catch (e: ClassNotFoundException) {
                fail(
                    "Service loader references non-existent class: $className. " +
                        "Did you move FaktCompilerPluginRegistrar without updating META-INF/services? " +
                        "Error: ${e.message}",
                )
            }

        assertNotNull(loadedClass, "Class $className should be loadable")

        // Verify class is public (service loader requirement)
        val modifiers = loadedClass.modifiers
        assertTrue(
            java.lang.reflect.Modifier
                .isPublic(modifiers),
            "Service loader class must be public: $className",
        )

        // Verify class can be instantiated (has public no-arg constructor)
        val hasPublicNoArgConstructor =
            try {
                loadedClass.getDeclaredConstructor()
                true
            } catch (_: NoSuchMethodException) {
                false
            }

        assertTrue(
            hasPublicNoArgConstructor,
            "Service loader class must have public no-arg constructor: $className",
        )
    }

    @Test
    fun `GIVEN service loader files WHEN checking all entries THEN all referenced classes should exist`() {
        // Given
        val serviceFiles =
            listOf(
                "org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor",
                "org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar",
            )

        // When & Then - Check each service file
        serviceFiles.forEach { serviceFileName ->
            val serviceFile = File(servicesDir, serviceFileName)

            assertTrue(
                serviceFile.exists(),
                "Service file should exist: $serviceFileName",
            )

            val classNames =
                serviceFile
                    .readLines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.startsWith("#") }

            assertTrue(
                classNames.isNotEmpty(),
                "Service file should contain at least one class: $serviceFileName",
            )

            // Validate each class entry
            classNames.forEach { className ->
                try {
                    Class.forName(className)
                } catch (e: ClassNotFoundException) {
                    fail(
                        "Service file '$serviceFileName' references non-existent class: $className. " +
                            "This breaks plugin loading at runtime. " +
                            "Check if class was moved or renamed without updating META-INF/services. " +
                            "Error: ${e.message}",
                    )
                }
            }
        }
    }

    @Test
    fun `GIVEN service loader config WHEN validating package THEN FaktCommandLineProcessor should be in config package`() {
        // Given - This test documents the expected package structure
        val serviceFile =
            File(
                servicesDir,
                "org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor",
            )
        val className = serviceFile.readText().trim()

        // When
        val expectedPackage = "com.rsicarelli.fakt.compiler.config"

        // Then
        assertTrue(
            className.startsWith(expectedPackage),
            "FaktCommandLineProcessor should be in $expectedPackage package. " +
                "Found: $className. " +
                "If you moved this class, update META-INF/services/" +
                "org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor",
        )

        assertTrue(
            className == "com.rsicarelli.fakt.compiler.config.FaktCommandLineProcessor",
            "Exact class name should be com.rsicarelli.fakt.compiler.config.FaktCommandLineProcessor. " +
                "Found: $className",
        )
    }

    @Test
    fun `GIVEN service loader config WHEN validating package THEN FaktCompilerPluginRegistrar in root compiler package`() {
        // Given - This test documents the expected package structure
        val serviceFile =
            File(
                servicesDir,
                "org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar",
            )
        val className = serviceFile.readText().trim()

        // When
        val expectedPackage = "com.rsicarelli.fakt.compiler"

        // Then
        assertTrue(
            className.startsWith(expectedPackage),
            "FaktCompilerPluginRegistrar should be in $expectedPackage package. " +
                "Found: $className",
        )

        assertTrue(
            className == "com.rsicarelli.fakt.compiler.FaktCompilerPluginRegistrar",
            "Exact class name should be com.rsicarelli.fakt.compiler.FaktCompilerPluginRegistrar. " +
                "Found: $className. " +
                "If you renamed this class, update META-INF/services/" +
                "org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar",
        )
    }
}
