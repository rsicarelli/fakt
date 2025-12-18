// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.cache

import com.rsicarelli.fakt.compiler.api.FirMetadataCache
import com.rsicarelli.fakt.compiler.api.SerializableFakeInterface
import com.rsicarelli.fakt.compiler.api.SerializableFunctionInfo
import com.rsicarelli.fakt.compiler.api.SerializableParameterInfo
import com.rsicarelli.fakt.compiler.api.SerializablePropertyInfo
import com.rsicarelli.fakt.compiler.api.SerializableTypeParameterInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirFunctionInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirParameterInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirPropertyInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirSourceLocation
import com.rsicarelli.fakt.compiler.fir.metadata.FirTypeParameterInfo
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeInterface
import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * BDD tests for MetadataCacheSerializer.
 *
 * Tests verify the serialization/deserialization of FIR metadata for cross-compilation caching.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetadataCacheSerializerTest {
    @TempDir
    lateinit var tempDir: File

    // ========================================================================
    // ValidatedFakeInterface → SerializableFakeInterface Conversion Tests
    // ========================================================================

    @Test
    fun `GIVEN ValidatedFakeInterface WHEN toSerializable THEN converts all fields correctly`() =
        runTest {
            // GIVEN
            val classId = ClassId.topLevel(FqName("com.example.UserService"))
            val validated =
                ValidatedFakeInterface(
                    classId = classId,
                    simpleName = "UserService",
                    packageName = "com.example",
                    typeParameters =
                        listOf(
                            FirTypeParameterInfo("T", listOf("Any")),
                        ),
                    properties =
                        listOf(
                            FirPropertyInfo("userId", "String", false, false),
                        ),
                    functions =
                        listOf(
                            FirFunctionInfo(
                                name = "getUser",
                                parameters =
                                    listOf(
                                        FirParameterInfo("id", "String", false, null, false),
                                    ),
                                returnType = "User",
                                isSuspend = true,
                                isInline = false,
                                typeParameters = emptyList(),
                                typeParameterBounds = emptyMap(),
                            ),
                        ),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceLocation =
                        FirSourceLocation(
                            filePath = "/path/to/UserService.kt",
                            startLine = 10,
                            startColumn = 0,
                            endLine = 50,
                            endColumn = 1,
                        ),
                    validationTimeNanos = 12345L,
                )

            // WHEN
            val serializable = MetadataCacheSerializer.toSerializable(validated)

            // THEN
            // ClassId.asString() uses slashes for package: "com/example/UserService"
            assertEquals("com/example/UserService", serializable.classIdString)
            assertEquals("UserService", serializable.simpleName)
            assertEquals("com.example", serializable.packageName)
            assertEquals(1, serializable.typeParameters.size)
            assertEquals("T", serializable.typeParameters[0].name)
            assertEquals(listOf("Any"), serializable.typeParameters[0].bounds)
            assertEquals(1, serializable.properties.size)
            assertEquals("userId", serializable.properties[0].name)
            assertEquals(1, serializable.functions.size)
            assertEquals("getUser", serializable.functions[0].name)
            assertTrue(serializable.functions[0].isSuspend)
            assertEquals("/path/to/UserService.kt", serializable.sourceFilePath)
            assertEquals(12345L, serializable.validationTimeNanos)
        }

    @Test
    fun `GIVEN nested class WHEN toSerializable THEN classIdString uses correct format`() =
        runTest {
            // GIVEN: Nested class com.example.Outer.Inner
            val packageFqName = FqName("com.example")
            val relativeClassName = FqName("Outer.Inner")
            val classId = ClassId(packageFqName, relativeClassName, isLocal = false)

            val validated =
                ValidatedFakeInterface(
                    classId = classId,
                    simpleName = "Inner",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    properties = emptyList(),
                    functions = emptyList(),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceLocation = FirSourceLocation.UNKNOWN,
                    validationTimeNanos = 0L,
                )

            // WHEN
            val serializable = MetadataCacheSerializer.toSerializable(validated)

            // THEN
            // ClassId.asString() uses slashes for package: "com/example/Outer.Inner"
            assertEquals("com/example/Outer.Inner", serializable.classIdString)
        }

    // ========================================================================
    // SerializableFakeInterface → ValidatedFakeInterface Conversion Tests
    // ========================================================================

    @Test
    fun `GIVEN SerializableFakeInterface WHEN toValidated THEN restores ClassId correctly`() =
        runTest {
            // GIVEN: ClassId.asString() format uses slashes for package
            val serializable =
                SerializableFakeInterface(
                    classIdString = "com/example/Repository",
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
                )

            // WHEN
            val validated = MetadataCacheSerializer.toValidated(serializable)

            // THEN
            assertEquals("com.example", validated.classId.packageFqName.asString())
            assertEquals("Repository", validated.classId.relativeClassName.asString())
            assertEquals("Repository", validated.simpleName)
            assertEquals("com.example", validated.packageName)
        }

    @Test
    fun `GIVEN nested class classIdString WHEN toValidated THEN parses correctly`() =
        runTest {
            // GIVEN: ClassId.asString() format uses slashes for package
            val serializable =
                SerializableFakeInterface(
                    classIdString = "com/example/Outer.Inner",
                    simpleName = "Inner",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    properties = emptyList(),
                    functions = emptyList(),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceFilePath = "/path/to/file.kt",
                    sourceFileSignature = "abc123",
                    validationTimeNanos = 0L,
                )

            // WHEN
            val validated = MetadataCacheSerializer.toValidated(serializable)

            // THEN
            assertEquals("com.example", validated.classId.packageFqName.asString())
            assertEquals("Outer.Inner", validated.classId.relativeClassName.asString())
        }

    @Test
    fun `GIVEN serializable with all field types WHEN toValidated THEN converts everything`() =
        runTest {
            // GIVEN: ClassId.asString() format uses slashes for package
            val serializable =
                SerializableFakeInterface(
                    classIdString = "com/example/ComplexService",
                    simpleName = "ComplexService",
                    packageName = "com.example",
                    typeParameters =
                        listOf(
                            SerializableTypeParameterInfo("T", listOf("Any")),
                            SerializableTypeParameterInfo("R", listOf("Comparable<R>")),
                        ),
                    properties =
                        listOf(
                            SerializablePropertyInfo("prop1", "String", false, false),
                            SerializablePropertyInfo("prop2", "Int?", true, true),
                        ),
                    functions =
                        listOf(
                            SerializableFunctionInfo(
                                name = "method1",
                                parameters =
                                    listOf(
                                        SerializableParameterInfo("p1", "String", false, null, false),
                                        SerializableParameterInfo("p2", "Int", true, "42", false),
                                    ),
                                returnType = "Unit",
                                isSuspend = true,
                                isInline = false,
                                typeParameters =
                                    listOf(
                                        SerializableTypeParameterInfo("U", emptyList()),
                                    ),
                                typeParameterBounds = mapOf("U" to "Any"),
                            ),
                        ),
                    inheritedProperties =
                        listOf(
                            SerializablePropertyInfo("inherited", "Boolean", false, false),
                        ),
                    inheritedFunctions =
                        listOf(
                            SerializableFunctionInfo(
                                name = "inheritedMethod",
                                parameters = emptyList(),
                                returnType = "String",
                                isSuspend = false,
                                isInline = false,
                                typeParameters = emptyList(),
                                typeParameterBounds = emptyMap(),
                            ),
                        ),
                    sourceFilePath = "/path/to/ComplexService.kt",
                    sourceFileSignature = "complex-sig",
                    validationTimeNanos = 99999L,
                )

            // WHEN
            val validated = MetadataCacheSerializer.toValidated(serializable)

            // THEN
            assertEquals(2, validated.typeParameters.size)
            assertEquals("T", validated.typeParameters[0].name)
            assertEquals("R", validated.typeParameters[1].name)
            assertEquals(listOf("Comparable<R>"), validated.typeParameters[1].bounds)

            assertEquals(2, validated.properties.size)
            assertEquals("prop1", validated.properties[0].name)
            assertEquals(false, validated.properties[0].isMutable)
            assertEquals("prop2", validated.properties[1].name)
            assertEquals(true, validated.properties[1].isMutable)
            assertEquals(true, validated.properties[1].isNullable)

            assertEquals(1, validated.functions.size)
            assertEquals("method1", validated.functions[0].name)
            assertEquals(true, validated.functions[0].isSuspend)
            assertEquals(2, validated.functions[0].parameters.size)
            assertEquals("42", validated.functions[0].parameters[1].defaultValueCode)
            assertEquals(1, validated.functions[0].typeParameters.size)

            assertEquals(1, validated.inheritedProperties.size)
            assertEquals(1, validated.inheritedFunctions.size)

            // Cache hits should have 0 validationTimeNanos since no FIR analysis was performed
            assertEquals(0L, validated.validationTimeNanos)
        }

    // ========================================================================
    // File Signature Tests
    // ========================================================================

    @Test
    fun `GIVEN source file WHEN computeFileSignature THEN returns MD5 hash`() =
        runTest {
            // GIVEN
            val testFile = File(tempDir, "TestFile.kt")
            testFile.writeText("interface TestInterface { fun test(): String }")

            // WHEN
            val signature = MetadataCacheSerializer.computeFileSignature(testFile.absolutePath)

            // THEN
            assertNotNull(signature)
            assertEquals(32, signature.length, "MD5 hash should be 32 hex characters")
            assertTrue(signature.matches(Regex("[a-f0-9]{32}")), "Should be valid hex string")
        }

    @Test
    fun `GIVEN same file content WHEN computeFileSignature twice THEN returns same hash`() =
        runTest {
            // GIVEN
            val testFile = File(tempDir, "Consistent.kt")
            testFile.writeText("interface Consistent { val x: Int }")

            // WHEN
            val signature1 = MetadataCacheSerializer.computeFileSignature(testFile.absolutePath)
            val signature2 = MetadataCacheSerializer.computeFileSignature(testFile.absolutePath)

            // THEN
            assertEquals(signature1, signature2, "Same content should produce same signature")
        }

    @Test
    fun `GIVEN different file content WHEN computeFileSignature THEN returns different hash`() =
        runTest {
            // GIVEN
            val file1 = File(tempDir, "File1.kt")
            file1.writeText("interface A")

            val file2 = File(tempDir, "File2.kt")
            file2.writeText("interface B")

            // WHEN
            val sig1 = MetadataCacheSerializer.computeFileSignature(file1.absolutePath)
            val sig2 = MetadataCacheSerializer.computeFileSignature(file2.absolutePath)

            // THEN
            assertTrue(sig1 != sig2, "Different content should produce different signatures")
        }

    @Test
    fun `GIVEN missing file WHEN computeFileSignature THEN returns missing marker`() =
        runTest {
            // GIVEN
            val missingPath = File(tempDir, "NonExistent.kt").absolutePath

            // WHEN
            val signature = MetadataCacheSerializer.computeFileSignature(missingPath)

            // THEN
            assertEquals("missing", signature)
        }

    @Test
    fun `GIVEN unknown path WHEN computeFileSignature THEN returns unknown marker`() =
        runTest {
            // GIVEN
            val unknownPath = "<unknown>"

            // WHEN
            val signature = MetadataCacheSerializer.computeFileSignature(unknownPath)

            // THEN
            assertEquals("unknown", signature)
        }

    // ========================================================================
    // Combined Signature Tests
    // ========================================================================

    @Test
    fun `GIVEN multiple signatures WHEN computeCombinedSignature THEN produces deterministic hash`() =
        runTest {
            // GIVEN
            val signatures = listOf("sig1", "sig2", "sig3")

            // WHEN
            val combined1 = MetadataCacheSerializer.computeCombinedSignature(signatures)
            val combined2 = MetadataCacheSerializer.computeCombinedSignature(signatures)

            // THEN
            assertEquals(combined1, combined2, "Same inputs should produce same combined signature")
            assertEquals(32, combined1.length, "Combined signature should be MD5 (32 chars)")
        }

    @Test
    fun `GIVEN signatures in different order WHEN computeCombinedSignature THEN produces same hash`() =
        runTest {
            // GIVEN: Signatures should be sorted before combining
            val signaturesA = listOf("z", "a", "m")
            val signaturesB = listOf("a", "m", "z")

            // WHEN
            val combinedA = MetadataCacheSerializer.computeCombinedSignature(signaturesA)
            val combinedB = MetadataCacheSerializer.computeCombinedSignature(signaturesB)

            // THEN
            assertEquals(combinedA, combinedB, "Order should not affect combined signature")
        }

    @Test
    fun `GIVEN empty signatures WHEN computeCombinedSignature THEN returns valid hash`() =
        runTest {
            // GIVEN
            val empty = emptyList<String>()

            // WHEN
            val combined = MetadataCacheSerializer.computeCombinedSignature(empty)

            // THEN
            assertEquals(32, combined.length, "Should still produce valid MD5 hash")
        }

    // ========================================================================
    // File I/O Tests
    // ========================================================================

    @Test
    fun `GIVEN cache data WHEN serialize to file THEN writes atomically`() =
        runTest {
            // GIVEN
            val cache =
                FirMetadataCache(
                    cacheSignature = "test-sig",
                    interfaces =
                        listOf(
                            SerializableFakeInterface(
                                classIdString = "com/example/Test",
                                simpleName = "Test",
                                packageName = "com.example",
                                typeParameters = emptyList(),
                                properties = emptyList(),
                                functions = emptyList(),
                                inheritedProperties = emptyList(),
                                inheritedFunctions = emptyList(),
                                sourceFilePath = "Test.kt",
                                sourceFileSignature = "sig",
                                validationTimeNanos = 0L,
                            ),
                        ),
                    classes = emptyList(),
                )
            val outputPath = File(tempDir, "cache.json").absolutePath

            // WHEN
            MetadataCacheSerializer.serialize(cache, outputPath)

            // THEN
            val outputFile = File(outputPath)
            assertTrue(outputFile.exists(), "Cache file should exist")
            assertTrue(outputFile.length() > 0, "Cache file should not be empty")

            // Verify no temp file left behind (atomic write cleans up)
            val tempFile = File("$outputPath.tmp")
            assertTrue(!tempFile.exists(), "Temp file should be cleaned up")
        }

    @Test
    fun `GIVEN serialized cache WHEN deserialize THEN returns original data`() =
        runTest {
            // GIVEN
            val original =
                FirMetadataCache(
                    cacheSignature = "roundtrip-sig",
                    interfaces =
                        listOf(
                            SerializableFakeInterface(
                                classIdString = "com/example/RoundTrip",
                                simpleName = "RoundTrip",
                                packageName = "com.example",
                                typeParameters =
                                    listOf(
                                        SerializableTypeParameterInfo("T", listOf("Any")),
                                    ),
                                properties =
                                    listOf(
                                        SerializablePropertyInfo("prop", "String", false, false),
                                    ),
                                functions =
                                    listOf(
                                        SerializableFunctionInfo(
                                            name = "method",
                                            parameters = emptyList(),
                                            returnType = "T",
                                            isSuspend = true,
                                            isInline = false,
                                            typeParameters = emptyList(),
                                            typeParameterBounds = emptyMap(),
                                        ),
                                    ),
                                inheritedProperties = emptyList(),
                                inheritedFunctions = emptyList(),
                                sourceFilePath = "RoundTrip.kt",
                                sourceFileSignature = "rt-sig",
                                validationTimeNanos = 12345L,
                            ),
                        ),
                    classes = emptyList(),
                )
            val cachePath = File(tempDir, "roundtrip.json").absolutePath
            MetadataCacheSerializer.serialize(original, cachePath)

            // WHEN
            val loaded = MetadataCacheSerializer.deserialize(cachePath)

            // THEN
            assertNotNull(loaded)
            assertEquals(original.cacheSignature, loaded.cacheSignature)
            assertEquals(original.interfaces.size, loaded.interfaces.size)
            assertEquals(original.interfaces[0].simpleName, loaded.interfaces[0].simpleName)
        }

    @Test
    fun `GIVEN missing file WHEN deserialize THEN returns null`() =
        runTest {
            // GIVEN
            val missingPath = File(tempDir, "nonexistent.json").absolutePath

            // WHEN
            val result = MetadataCacheSerializer.deserialize(missingPath)

            // THEN
            assertNull(result, "Should return null for missing file")
        }

    @Test
    fun `GIVEN corrupt JSON WHEN deserialize THEN returns null gracefully`() =
        runTest {
            // GIVEN
            val corruptFile = File(tempDir, "corrupt.json")
            corruptFile.writeText("{ this is not valid JSON }")

            // WHEN
            val result = MetadataCacheSerializer.deserialize(corruptFile.absolutePath)

            // THEN
            assertNull(result, "Should return null for corrupt JSON")
        }

    @Test
    fun `GIVEN empty file WHEN deserialize THEN returns null gracefully`() =
        runTest {
            // GIVEN
            val emptyFile = File(tempDir, "empty.json")
            emptyFile.writeText("")

            // WHEN
            val result = MetadataCacheSerializer.deserialize(emptyFile.absolutePath)

            // THEN
            assertNull(result, "Should return null for empty file")
        }

    @Test
    fun `GIVEN nested directory path WHEN serialize THEN creates parent directories`() =
        runTest {
            // GIVEN
            val cache =
                FirMetadataCache(
                    cacheSignature = "nested-sig",
                    interfaces = emptyList(),
                    classes = emptyList(),
                )
            val nestedPath = File(tempDir, "a/b/c/cache.json").absolutePath

            // WHEN
            MetadataCacheSerializer.serialize(cache, nestedPath)

            // THEN
            assertTrue(File(nestedPath).exists(), "Should create nested directories and file")
        }

    // ========================================================================
    // Full Roundtrip with ValidatedFakeInterface Tests
    // ========================================================================

    @Test
    fun `GIVEN ValidatedFakeInterface WHEN full roundtrip THEN preserves all data`() =
        runTest {
            // GIVEN
            val classId = ClassId.topLevel(FqName("com.example.FullRoundTrip"))
            val original =
                ValidatedFakeInterface(
                    classId = classId,
                    simpleName = "FullRoundTrip",
                    packageName = "com.example",
                    typeParameters =
                        listOf(
                            FirTypeParameterInfo("T", listOf("Comparable<T>")),
                        ),
                    properties =
                        listOf(
                            FirPropertyInfo("id", "Long", false, false),
                            FirPropertyInfo("name", "String?", true, true),
                        ),
                    functions =
                        listOf(
                            FirFunctionInfo(
                                name = "process",
                                parameters =
                                    listOf(
                                        FirParameterInfo("input", "T", false, null, false),
                                        FirParameterInfo("options", "Map<String, Any>", true, "emptyMap()", false),
                                    ),
                                returnType = "Result<T>",
                                isSuspend = true,
                                isInline = false,
                                typeParameters = emptyList(),
                                typeParameterBounds = emptyMap(),
                            ),
                        ),
                    inheritedProperties =
                        listOf(
                            FirPropertyInfo("inherited", "Boolean", false, false),
                        ),
                    inheritedFunctions =
                        listOf(
                            FirFunctionInfo(
                                name = "close",
                                parameters = emptyList(),
                                returnType = "Unit",
                                isSuspend = false,
                                isInline = false,
                                typeParameters = emptyList(),
                                typeParameterBounds = emptyMap(),
                            ),
                        ),
                    sourceLocation = FirSourceLocation("/src/FullRoundTrip.kt", 1, 0, 100, 1),
                    validationTimeNanos = 54321L,
                )

            // WHEN: Convert to serializable, serialize, deserialize, convert back
            val serializable = MetadataCacheSerializer.toSerializable(original)
            val cachePath = File(tempDir, "full-roundtrip.json").absolutePath
            val cache =
                FirMetadataCache(
                    cacheSignature = "full-sig",
                    interfaces = listOf(serializable),
                    classes = emptyList(),
                )
            MetadataCacheSerializer.serialize(cache, cachePath)
            val loadedCache = MetadataCacheSerializer.deserialize(cachePath)
            val restored = MetadataCacheSerializer.toValidated(loadedCache!!.interfaces[0])

            // THEN
            assertEquals(original.classId.asFqNameString(), restored.classId.asFqNameString())
            assertEquals(original.simpleName, restored.simpleName)
            assertEquals(original.packageName, restored.packageName)
            assertEquals(original.typeParameters.size, restored.typeParameters.size)
            assertEquals(original.typeParameters[0].name, restored.typeParameters[0].name)
            assertEquals(original.typeParameters[0].bounds, restored.typeParameters[0].bounds)
            assertEquals(original.properties.size, restored.properties.size)
            assertEquals(original.functions.size, restored.functions.size)
            assertEquals(original.functions[0].parameters.size, restored.functions[0].parameters.size)
            assertEquals(original.inheritedProperties.size, restored.inheritedProperties.size)
            assertEquals(original.inheritedFunctions.size, restored.inheritedFunctions.size)
            // validationTimeNanos is intentionally set to 0 for cache hits since no FIR analysis
            // was performed - this ensures accurate telemetry reporting in platform compilations
            assertEquals(0L, restored.validationTimeNanos)
        }
}
