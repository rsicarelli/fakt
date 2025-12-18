// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.api

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * BDD tests for FirMetadataCache serialization.
 *
 * Following GIVEN-WHEN-THEN pattern for cross-compilation caching feature.
 * Tests verify that FIR metadata can be serialized/deserialized for KMP caching.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FirMetadataCacheSerializationTest {
    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    // ========================================================================
    // Basic Serialization Tests
    // ========================================================================

    @Test
    fun `GIVEN valid interface metadata WHEN serialized to JSON THEN produces valid JSON`() = runTest {
        // GIVEN
        val cache = FirMetadataCache(
            version = FirMetadataCache.CURRENT_VERSION,
            cacheSignature = "abc123",
            interfaces = listOf(
                SerializableFakeInterface(
                    classIdString = "com.example/UserService",
                    simpleName = "UserService",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    properties = listOf(
                        SerializablePropertyInfo(
                            name = "userId",
                            type = "String",
                            isMutable = false,
                            isNullable = false,
                        ),
                    ),
                    functions = listOf(
                        SerializableFunctionInfo(
                            name = "getUser",
                            parameters = emptyList(),
                            returnType = "User",
                            isSuspend = true,
                            isInline = false,
                            typeParameters = emptyList(),
                            typeParameterBounds = emptyMap(),
                        ),
                    ),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceFilePath = "src/commonMain/kotlin/UserService.kt",
                    sourceFileSignature = "d41d8cd98f00b204e9800998ecf8427e",
                    validationTimeNanos = 12345L,
                ),
            ),
            classes = emptyList(),
        )

        // WHEN
        val jsonString = json.encodeToString(cache)

        // THEN
        assertTrue(jsonString.isNotBlank(), "JSON string should not be blank")
        assertTrue(jsonString.contains("UserService"), "JSON should contain interface name")
        assertTrue(jsonString.contains("abc123"), "JSON should contain cache signature")
    }

    @Test
    fun `GIVEN JSON string WHEN deserialized THEN restores original data`() = runTest {
        // GIVEN
        val original = FirMetadataCache(
            version = FirMetadataCache.CURRENT_VERSION,
            cacheSignature = "sig123",
            interfaces = listOf(
                SerializableFakeInterface(
                    classIdString = "com.example/Repository",
                    simpleName = "Repository",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    properties = emptyList(),
                    functions = emptyList(),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceFilePath = "/path/to/file.kt",
                    sourceFileSignature = "abc123",
                    validationTimeNanos = 0L,
                ),
            ),
            classes = emptyList(),
        )

        // WHEN
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<FirMetadataCache>(jsonString)

        // THEN
        assertEquals(original, decoded)
        assertEquals(1, decoded.interfaces.size)
        assertEquals("Repository", decoded.interfaces[0].simpleName)
    }

    // ========================================================================
    // Complex Generic Tests
    // ========================================================================

    @Test
    fun `GIVEN complex generics WHEN roundtrip THEN preserves type parameters`() = runTest {
        // GIVEN: Interface with complex generics like Repository<T : Entity, K : Comparable<K>>
        val original = FirMetadataCache(
            version = FirMetadataCache.CURRENT_VERSION,
            cacheSignature = "generic-sig",
            interfaces = listOf(
                SerializableFakeInterface(
                    classIdString = "com.example/Repository",
                    simpleName = "Repository",
                    packageName = "com.example",
                    typeParameters = listOf(
                        SerializableTypeParameterInfo(
                            name = "T",
                            bounds = listOf("Entity"),
                        ),
                        SerializableTypeParameterInfo(
                            name = "K",
                            bounds = listOf("Comparable<K>"),
                        ),
                    ),
                    properties = emptyList(),
                    functions = listOf(
                        SerializableFunctionInfo(
                            name = "findById",
                            parameters = listOf(
                                SerializableParameterInfo(
                                    name = "id",
                                    type = "K",
                                    hasDefaultValue = false,
                                    defaultValueCode = null,
                                    isVararg = false,
                                ),
                            ),
                            returnType = "T?",
                            isSuspend = true,
                            isInline = false,
                            typeParameters = emptyList(),
                            typeParameterBounds = emptyMap(),
                        ),
                    ),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceFilePath = "Repository.kt",
                    sourceFileSignature = "abc",
                    validationTimeNanos = 0L,
                ),
            ),
            classes = emptyList(),
        )

        // WHEN
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<FirMetadataCache>(jsonString)

        // THEN
        assertEquals(2, decoded.interfaces[0].typeParameters.size)
        assertEquals("T", decoded.interfaces[0].typeParameters[0].name)
        assertEquals(listOf("Entity"), decoded.interfaces[0].typeParameters[0].bounds)
        assertEquals("K", decoded.interfaces[0].typeParameters[1].name)
        assertEquals(listOf("Comparable<K>"), decoded.interfaces[0].typeParameters[1].bounds)
    }

    @Test
    fun `GIVEN method-level type parameters WHEN roundtrip THEN preserves bounds`() = runTest {
        // GIVEN: Function with method-level generics like <R : TValue> transform(): R
        val original = FirMetadataCache(
            version = FirMetadataCache.CURRENT_VERSION,
            cacheSignature = "method-generic-sig",
            interfaces = listOf(
                SerializableFakeInterface(
                    classIdString = "com.example/Transformer",
                    simpleName = "Transformer",
                    packageName = "com.example",
                    typeParameters = listOf(
                        SerializableTypeParameterInfo("TValue", emptyList()),
                    ),
                    properties = emptyList(),
                    functions = listOf(
                        SerializableFunctionInfo(
                            name = "transform",
                            parameters = emptyList(),
                            returnType = "R",
                            isSuspend = false,
                            isInline = true,
                            typeParameters = listOf(
                                SerializableTypeParameterInfo("R", listOf("TValue")),
                            ),
                            typeParameterBounds = mapOf("R" to "TValue"),
                        ),
                    ),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceFilePath = "Transformer.kt",
                    sourceFileSignature = "def",
                    validationTimeNanos = 0L,
                ),
            ),
            classes = emptyList(),
        )

        // WHEN
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<FirMetadataCache>(jsonString)

        // THEN
        val function = decoded.interfaces[0].functions[0]
        assertEquals(1, function.typeParameters.size)
        assertEquals("R", function.typeParameters[0].name)
        assertEquals(listOf("TValue"), function.typeParameters[0].bounds)
        assertEquals("TValue", function.typeParameterBounds["R"])
        assertTrue(function.isInline)
    }

    // ========================================================================
    // Inherited Members Tests
    // ========================================================================

    @Test
    fun `GIVEN inherited members WHEN serialized THEN includes all members`() = runTest {
        // GIVEN: Interface inheriting from parent interface
        val original = FirMetadataCache(
            version = FirMetadataCache.CURRENT_VERSION,
            cacheSignature = "inherit-sig",
            interfaces = listOf(
                SerializableFakeInterface(
                    classIdString = "com.example/ChildService",
                    simpleName = "ChildService",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    properties = listOf(
                        SerializablePropertyInfo("childProp", "String", false, false),
                    ),
                    functions = listOf(
                        SerializableFunctionInfo(
                            name = "childMethod",
                            parameters = emptyList(),
                            returnType = "Unit",
                            isSuspend = false,
                            isInline = false,
                            typeParameters = emptyList(),
                            typeParameterBounds = emptyMap(),
                        ),
                    ),
                    inheritedProperties = listOf(
                        SerializablePropertyInfo("parentProp", "Int", true, false),
                    ),
                    inheritedFunctions = listOf(
                        SerializableFunctionInfo(
                            name = "parentMethod",
                            parameters = emptyList(),
                            returnType = "Boolean",
                            isSuspend = true,
                            isInline = false,
                            typeParameters = emptyList(),
                            typeParameterBounds = emptyMap(),
                        ),
                    ),
                    sourceFilePath = "ChildService.kt",
                    sourceFileSignature = "child-sig",
                    validationTimeNanos = 0L,
                ),
            ),
            classes = emptyList(),
        )

        // WHEN
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<FirMetadataCache>(jsonString)

        // THEN
        val iface = decoded.interfaces[0]
        assertEquals(1, iface.properties.size, "Should have 1 direct property")
        assertEquals(1, iface.functions.size, "Should have 1 direct function")
        assertEquals(1, iface.inheritedProperties.size, "Should have 1 inherited property")
        assertEquals(1, iface.inheritedFunctions.size, "Should have 1 inherited function")
        assertEquals("parentProp", iface.inheritedProperties[0].name)
        assertEquals("parentMethod", iface.inheritedFunctions[0].name)
    }

    // ========================================================================
    // Class Metadata Tests
    // ========================================================================

    @Test
    fun `GIVEN abstract class metadata WHEN roundtrip THEN preserves abstract and open members`() = runTest {
        // GIVEN
        val original = FirMetadataCache(
            version = FirMetadataCache.CURRENT_VERSION,
            cacheSignature = "class-sig",
            interfaces = emptyList(),
            classes = listOf(
                SerializableFakeClass(
                    classIdString = "com.example/AbstractRepository",
                    simpleName = "AbstractRepository",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    abstractProperties = listOf(
                        SerializablePropertyInfo("id", "Long", false, false),
                    ),
                    openProperties = listOf(
                        SerializablePropertyInfo("name", "String", true, true),
                    ),
                    abstractMethods = listOf(
                        SerializableFunctionInfo(
                            name = "save",
                            parameters = emptyList(),
                            returnType = "Unit",
                            isSuspend = true,
                            isInline = false,
                            typeParameters = emptyList(),
                            typeParameterBounds = emptyMap(),
                        ),
                    ),
                    openMethods = listOf(
                        SerializableFunctionInfo(
                            name = "validate",
                            parameters = emptyList(),
                            returnType = "Boolean",
                            isSuspend = false,
                            isInline = false,
                            typeParameters = emptyList(),
                            typeParameterBounds = emptyMap(),
                        ),
                    ),
                    sourceFilePath = "AbstractRepository.kt",
                    sourceFileSignature = "repo-sig",
                    validationTimeNanos = 0L,
                ),
            ),
        )

        // WHEN
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<FirMetadataCache>(jsonString)

        // THEN
        assertEquals(1, decoded.classes.size)
        val clazz = decoded.classes[0]
        assertEquals("AbstractRepository", clazz.simpleName)
        assertEquals(1, clazz.abstractProperties.size)
        assertEquals(1, clazz.openProperties.size)
        assertEquals(1, clazz.abstractMethods.size)
        assertEquals(1, clazz.openMethods.size)
    }

    // ========================================================================
    // Parameter Tests
    // ========================================================================

    @Test
    fun `GIVEN parameters with defaults WHEN roundtrip THEN preserves default values`() = runTest {
        // GIVEN
        val original = FirMetadataCache(
            version = FirMetadataCache.CURRENT_VERSION,
            cacheSignature = "param-sig",
            interfaces = listOf(
                SerializableFakeInterface(
                    classIdString = "com.example/HttpClient",
                    simpleName = "HttpClient",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    properties = emptyList(),
                    functions = listOf(
                        SerializableFunctionInfo(
                            name = "request",
                            parameters = listOf(
                                SerializableParameterInfo(
                                    name = "url",
                                    type = "String",
                                    hasDefaultValue = false,
                                    defaultValueCode = null,
                                    isVararg = false,
                                ),
                                SerializableParameterInfo(
                                    name = "method",
                                    type = "String",
                                    hasDefaultValue = true,
                                    defaultValueCode = "\"GET\"",
                                    isVararg = false,
                                ),
                                SerializableParameterInfo(
                                    name = "timeout",
                                    type = "Long",
                                    hasDefaultValue = true,
                                    defaultValueCode = "30000L",
                                    isVararg = false,
                                ),
                            ),
                            returnType = "Response",
                            isSuspend = true,
                            isInline = false,
                            typeParameters = emptyList(),
                            typeParameterBounds = emptyMap(),
                        ),
                    ),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceFilePath = "HttpClient.kt",
                    sourceFileSignature = "http-sig",
                    validationTimeNanos = 0L,
                ),
            ),
            classes = emptyList(),
        )

        // WHEN
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<FirMetadataCache>(jsonString)

        // THEN
        val params = decoded.interfaces[0].functions[0].parameters
        assertEquals(3, params.size)
        assertEquals(false, params[0].hasDefaultValue)
        assertEquals(null, params[0].defaultValueCode)
        assertEquals(true, params[1].hasDefaultValue)
        assertEquals("\"GET\"", params[1].defaultValueCode)
        assertEquals(true, params[2].hasDefaultValue)
        assertEquals("30000L", params[2].defaultValueCode)
    }

    @Test
    fun `GIVEN vararg parameter WHEN roundtrip THEN preserves vararg flag`() = runTest {
        // GIVEN
        val original = FirMetadataCache(
            version = FirMetadataCache.CURRENT_VERSION,
            cacheSignature = "vararg-sig",
            interfaces = listOf(
                SerializableFakeInterface(
                    classIdString = "com.example/Logger",
                    simpleName = "Logger",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    properties = emptyList(),
                    functions = listOf(
                        SerializableFunctionInfo(
                            name = "log",
                            parameters = listOf(
                                SerializableParameterInfo(
                                    name = "messages",
                                    type = "String",
                                    hasDefaultValue = false,
                                    defaultValueCode = null,
                                    isVararg = true,
                                ),
                            ),
                            returnType = "Unit",
                            isSuspend = false,
                            isInline = false,
                            typeParameters = emptyList(),
                            typeParameterBounds = emptyMap(),
                        ),
                    ),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceFilePath = "Logger.kt",
                    sourceFileSignature = "log-sig",
                    validationTimeNanos = 0L,
                ),
            ),
            classes = emptyList(),
        )

        // WHEN
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<FirMetadataCache>(jsonString)

        // THEN
        val param = decoded.interfaces[0].functions[0].parameters[0]
        assertTrue(param.isVararg, "Vararg flag should be preserved")
    }

    // ========================================================================
    // Version and Signature Tests
    // ========================================================================

    @Test
    fun `GIVEN cache with version WHEN roundtrip THEN preserves version`() = runTest {
        // GIVEN
        val original = FirMetadataCache(
            version = 42,
            cacheSignature = "test-sig",
            interfaces = emptyList(),
            classes = emptyList(),
        )

        // WHEN
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<FirMetadataCache>(jsonString)

        // THEN
        assertEquals(42, decoded.version)
    }

    @Test
    fun `GIVEN cache with timestamp WHEN roundtrip THEN preserves generatedAt`() = runTest {
        // GIVEN
        val timestamp = 1703123456789L
        val original = FirMetadataCache(
            version = FirMetadataCache.CURRENT_VERSION,
            cacheSignature = "time-sig",
            interfaces = emptyList(),
            classes = emptyList(),
            generatedAt = timestamp,
        )

        // WHEN
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<FirMetadataCache>(jsonString)

        // THEN
        assertEquals(timestamp, decoded.generatedAt)
    }

    // ========================================================================
    // ClassId String Format Tests
    // ========================================================================

    @Test
    fun `GIVEN nested class WHEN serialized THEN uses correct classIdString format`() = runTest {
        // GIVEN: Nested class like com.example.Outer.Inner
        val original = FirMetadataCache(
            version = FirMetadataCache.CURRENT_VERSION,
            cacheSignature = "nested-sig",
            interfaces = listOf(
                SerializableFakeInterface(
                    classIdString = "com.example/Outer.Inner",
                    simpleName = "Inner",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    properties = emptyList(),
                    functions = emptyList(),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceFilePath = "Outer.kt",
                    sourceFileSignature = "nested-file-sig",
                    validationTimeNanos = 0L,
                ),
            ),
            classes = emptyList(),
        )

        // WHEN
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<FirMetadataCache>(jsonString)

        // THEN
        assertEquals("com.example/Outer.Inner", decoded.interfaces[0].classIdString)
        assertEquals("Inner", decoded.interfaces[0].simpleName)
    }

    // ========================================================================
    // Multiple Interfaces Test
    // ========================================================================

    @Test
    fun `GIVEN multiple interfaces WHEN roundtrip THEN preserves all interfaces`() = runTest {
        // GIVEN
        val original = FirMetadataCache(
            version = FirMetadataCache.CURRENT_VERSION,
            cacheSignature = "multi-sig",
            interfaces = listOf(
                SerializableFakeInterface(
                    classIdString = "com.example/ServiceA",
                    simpleName = "ServiceA",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    properties = emptyList(),
                    functions = emptyList(),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceFilePath = "ServiceA.kt",
                    sourceFileSignature = "sig-a",
                    validationTimeNanos = 100L,
                ),
                SerializableFakeInterface(
                    classIdString = "com.example/ServiceB",
                    simpleName = "ServiceB",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    properties = emptyList(),
                    functions = emptyList(),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceFilePath = "ServiceB.kt",
                    sourceFileSignature = "sig-b",
                    validationTimeNanos = 200L,
                ),
                SerializableFakeInterface(
                    classIdString = "com.other/ServiceC",
                    simpleName = "ServiceC",
                    packageName = "com.other",
                    typeParameters = emptyList(),
                    properties = emptyList(),
                    functions = emptyList(),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceFilePath = "ServiceC.kt",
                    sourceFileSignature = "sig-c",
                    validationTimeNanos = 300L,
                ),
            ),
            classes = emptyList(),
        )

        // WHEN
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<FirMetadataCache>(jsonString)

        // THEN
        assertEquals(3, decoded.interfaces.size)
        assertEquals("ServiceA", decoded.interfaces[0].simpleName)
        assertEquals("ServiceB", decoded.interfaces[1].simpleName)
        assertEquals("ServiceC", decoded.interfaces[2].simpleName)
        assertEquals("com.other", decoded.interfaces[2].packageName)
    }

    // ========================================================================
    // Size Test (Command Line Compatibility)
    // ========================================================================

    @Test
    fun `GIVEN typical cache WHEN serialized THEN JSON size is reasonable`() = runTest {
        // GIVEN: Typical project with 5 interfaces
        val interfaces = (1..5).map { i ->
            SerializableFakeInterface(
                classIdString = "com.example/Service$i",
                simpleName = "Service$i",
                packageName = "com.example",
                typeParameters = if (i % 2 == 0) listOf(SerializableTypeParameterInfo("T", emptyList())) else emptyList(),
                properties = listOf(
                    SerializablePropertyInfo("prop$i", "String", false, false),
                ),
                functions = listOf(
                    SerializableFunctionInfo(
                        name = "method$i",
                        parameters = listOf(
                            SerializableParameterInfo("param", "Int", false, null, false),
                        ),
                        returnType = "Unit",
                        isSuspend = i % 2 == 0,
                        isInline = false,
                        typeParameters = emptyList(),
                        typeParameterBounds = emptyMap(),
                    ),
                ),
                inheritedProperties = emptyList(),
                inheritedFunctions = emptyList(),
                sourceFilePath = "Service$i.kt",
                sourceFileSignature = "sig$i",
                validationTimeNanos = 1000L * i,
            )
        }

        val cache = FirMetadataCache(
            version = FirMetadataCache.CURRENT_VERSION,
            cacheSignature = "combined-sig",
            interfaces = interfaces,
            classes = emptyList(),
        )

        // WHEN
        val jsonString = json.encodeToString(cache)

        // THEN - Should be under 50KB for 5 interfaces (reasonable for file-based cache)
        assertTrue(
            jsonString.length < 50_000,
            "Cache JSON should be < 50KB for 5 interfaces. Got: ${jsonString.length} bytes",
        )
    }

    // ========================================================================
    // Empty Cache Test
    // ========================================================================

    @Test
    fun `GIVEN empty cache WHEN roundtrip THEN handles gracefully`() = runTest {
        // GIVEN
        val original = FirMetadataCache(
            version = FirMetadataCache.CURRENT_VERSION,
            cacheSignature = "empty-sig",
            interfaces = emptyList(),
            classes = emptyList(),
        )

        // WHEN
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<FirMetadataCache>(jsonString)

        // THEN
        assertEquals(original, decoded)
        assertTrue(decoded.interfaces.isEmpty())
        assertTrue(decoded.classes.isEmpty())
    }
}
