// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.cache

import com.rsicarelli.fakt.compiler.api.FirMetadataCache
import com.rsicarelli.fakt.compiler.api.SerializableAnnotationArgument
import com.rsicarelli.fakt.compiler.api.SerializableAnnotationInfo
import com.rsicarelli.fakt.compiler.api.SerializableFakeClass
import com.rsicarelli.fakt.compiler.api.SerializableFakeInterface
import com.rsicarelli.fakt.compiler.api.SerializableFunctionInfo
import com.rsicarelli.fakt.compiler.api.SerializableParameterInfo
import com.rsicarelli.fakt.compiler.api.SerializablePropertyInfo
import com.rsicarelli.fakt.compiler.api.SerializableTypeParameterInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirAnnotationArgument
import com.rsicarelli.fakt.compiler.fir.metadata.FirAnnotationInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirCallHistoryMode
import com.rsicarelli.fakt.compiler.fir.metadata.FirFunctionInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirMutabilityMode
import com.rsicarelli.fakt.compiler.fir.metadata.FirParameterInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirPropertyInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirSourceLocation
import com.rsicarelli.fakt.compiler.fir.metadata.FirTypeParameterInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeClass
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeInterface
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Serializes and deserializes FIR metadata cache for cross-compilation caching.
 *
 * ## Purpose
 *
 * Enables KMP projects to cache FIR analysis results from metadata compilation so platform
 * compilations can skip redundant analysis.
 *
 * ## Thread Safety
 *
 * Uses atomic file operations (write to temp, then rename) for safe concurrent access.
 *
 * ## Usage
 *
 * ```kotlin
 * // Producer mode (metadata compilation)
 * val serializable = MetadataCacheSerializer.toSerializable(validatedInterface)
 * val cache = FirMetadataCache(cacheSignature = sig, interfaces = listOf(serializable), ...)
 * MetadataCacheSerializer.serialize(cache, "/path/to/cache.json")
 *
 * // Consumer mode (platform compilation)
 * val cache = MetadataCacheSerializer.deserialize("/path/to/cache.json")
 * val validated = cache?.interfaces?.map { MetadataCacheSerializer.toValidated(it) }
 * ```
 */
object MetadataCacheSerializer {
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ========================================================================
    // Serialization: ValidatedFakeInterface → SerializableFakeInterface
    // ========================================================================

    /**
     * Convert ValidatedFakeInterface to serializable format.
     *
     * Converts ClassId to string format "package.name/RelativeClassName".
     *
     * @param validated Validated interface from FIR phase
     * @return Serializable representation
     */
    fun toSerializable(validated: ValidatedFakeInterface): SerializableFakeInterface {
        val sourceFileSignature = computeFileSignature(validated.sourceLocation.filePath)

        return SerializableFakeInterface(
            classIdString = validated.classId.asString(),
            simpleName = validated.simpleName,
            packageName = validated.packageName,
            typeParameters = validated.typeParameters.map { it.toSerializable() },
            properties = validated.properties.map { it.toSerializable() },
            functions = validated.functions.map { it.toSerializable() },
            inheritedProperties = validated.inheritedProperties.map { it.toSerializable() },
            inheritedFunctions = validated.inheritedFunctions.map { it.toSerializable() },
            annotations = validated.annotations.map { it.toSerializable() },
            sourceFilePath = validated.sourceLocation.filePath,
            sourceFileSignature = sourceFileSignature,
            validationTimeNanos = validated.validationTimeNanos,
            visibility = validated.visibility.name,
            callHistoryMode = validated.callHistoryMode.name,
            mutabilityMode = validated.mutabilityMode.name,
        )
    }

    /** Convert ValidatedFakeClass to serializable format. */
    fun toSerializable(validated: ValidatedFakeClass): SerializableFakeClass {
        val sourceFileSignature = computeFileSignature(validated.sourceLocation.filePath)

        return SerializableFakeClass(
            classIdString = validated.classId.asString(),
            simpleName = validated.simpleName,
            packageName = validated.packageName,
            typeParameters = validated.typeParameters.map { it.toSerializable() },
            abstractProperties = validated.abstractProperties.map { it.toSerializable() },
            openProperties = validated.openProperties.map { it.toSerializable() },
            abstractMethods = validated.abstractMethods.map { it.toSerializable() },
            openMethods = validated.openMethods.map { it.toSerializable() },
            annotations = validated.annotations.map { it.toSerializable() },
            sourceFilePath = validated.sourceLocation.filePath,
            sourceFileSignature = sourceFileSignature,
            validationTimeNanos = validated.validationTimeNanos,
            visibility = validated.visibility.name,
            callHistoryMode = validated.callHistoryMode.name,
            mutabilityMode = validated.mutabilityMode.name,
        )
    }

    // ========================================================================
    // Deserialization: SerializableFakeInterface → ValidatedFakeInterface
    // ========================================================================

    /**
     * Convert SerializableFakeInterface back to ValidatedFakeInterface.
     *
     * Parses ClassId from string format "package.name/RelativeClassName".
     *
     * Note: validationTimeNanos is set to 0 for cache hits since no FIR analysis was performed in
     * this compilation. This ensures accurate telemetry reporting.
     *
     * @param serializable Serializable representation from cache
     * @return Validated interface for IR phase
     */
    fun toValidated(serializable: SerializableFakeInterface): ValidatedFakeInterface {
        val classId = parseClassId(serializable.classIdString)
        val sourceLocation =
            FirSourceLocation(
                filePath = serializable.sourceFilePath,
                startLine = 0,
                startColumn = 0,
                endLine = 0,
                endColumn = 0,
            )

        return ValidatedFakeInterface(
            classId = classId,
            simpleName = serializable.simpleName,
            packageName = serializable.packageName,
            qualifiedSourceName = classId.relativeClassName.asString(),
            typeParameters = serializable.typeParameters.map { it.toFir() },
            properties = serializable.properties.map { it.toFir() },
            functions = serializable.functions.map { it.toFir() },
            inheritedProperties = serializable.inheritedProperties.map { it.toFir() },
            inheritedFunctions = serializable.inheritedFunctions.map { it.toFir() },
            annotations = serializable.annotations.map { it.toFir() },
            sourceLocation = sourceLocation,
            // Cache hit: no FIR analysis performed in this compilation
            validationTimeNanos = 0L,
            // Mark as from cache to output to commonTest instead of platform test
            isFromCache = true,
            // Extract source set from cached file path (e.g., "commonMain" from
            // "src/commonMain/kotlin/...")
            sourceSourceSet = sourceLocation.extractSourceSetName(),
            // Restore visibility from cache for explicitApi() support
            visibility = FirVisibility.valueOf(serializable.visibility),
            // Restore call history mode from cache
            callHistoryMode = FirCallHistoryMode.valueOf(serializable.callHistoryMode),
            // Restore mutability mode from cache
            mutabilityMode = FirMutabilityMode.valueOf(serializable.mutabilityMode),
        )
    }

    /**
     * Convert SerializableFakeClass back to ValidatedFakeClass.
     *
     * Note: validationTimeNanos is set to 0 for cache hits since no FIR analysis was performed in
     * this compilation.
     */
    fun toValidated(serializable: SerializableFakeClass): ValidatedFakeClass {
        val classId = parseClassId(serializable.classIdString)
        val sourceLocation =
            FirSourceLocation(
                filePath = serializable.sourceFilePath,
                startLine = 0,
                startColumn = 0,
                endLine = 0,
                endColumn = 0,
            )

        return ValidatedFakeClass(
            classId = classId,
            simpleName = serializable.simpleName,
            packageName = serializable.packageName,
            qualifiedSourceName = classId.relativeClassName.asString(),
            typeParameters = serializable.typeParameters.map { it.toFir() },
            abstractProperties = serializable.abstractProperties.map { it.toFir() },
            openProperties = serializable.openProperties.map { it.toFir() },
            abstractMethods = serializable.abstractMethods.map { it.toFir() },
            openMethods = serializable.openMethods.map { it.toFir() },
            annotations = serializable.annotations.map { it.toFir() },
            sourceLocation = sourceLocation,
            // Cache hit: no FIR analysis performed in this compilation
            validationTimeNanos = 0L,
            // Mark as from cache to output to commonTest instead of platform test
            isFromCache = true,
            // Extract source set from cached file path (e.g., "commonMain" from
            // "src/commonMain/kotlin/...")
            sourceSourceSet = sourceLocation.extractSourceSetName(),
            // Restore visibility from cache for explicitApi() support
            visibility = FirVisibility.valueOf(serializable.visibility),
            // Restore call history mode from cache
            callHistoryMode = FirCallHistoryMode.valueOf(serializable.callHistoryMode),
            // Restore mutability mode from cache
            mutabilityMode = FirMutabilityMode.valueOf(serializable.mutabilityMode),
        )
    }

    // ========================================================================
    // File I/O
    // ========================================================================

    /**
     * Serialize cache to JSON file.
     *
     * Uses atomic write (temp file + rename) for safety.
     *
     * @param cache Cache data to serialize
     * @param outputPath Absolute path to output JSON file
     */
    fun serialize(cache: FirMetadataCache, outputPath: String) {
        val file = File(outputPath)
        file.parentFile?.mkdirs()

        // Atomic write: write to temp file, then rename
        val tempFile = File("$outputPath.tmp")
        tempFile.writeText(json.encodeToString(FirMetadataCache.serializer(), cache))
        tempFile.renameTo(file)
    }

    /**
     * Deserialize cache from JSON file.
     *
     * @param cachePath Absolute path to cache JSON file
     * @return Deserialized cache, or null if file doesn't exist or is invalid
     */
    fun deserialize(cachePath: String): FirMetadataCache? {
        val file = File(cachePath)
        if (!file.exists()) return null

        return try {
            val content = file.readText()
            if (content.isBlank()) return null
            json.decodeFromString(FirMetadataCache.serializer(), content)
        } catch (_: SerializationException) {
            // Invalid JSON format - cache will be regenerated
            null
        } catch (_: IOException) {
            // File read error - cache will be regenerated
            null
        }
    }

    // ========================================================================
    // Signature Computation
    // ========================================================================

    /**
     * Compute MD5 signature for source file.
     *
     * Used for cache invalidation when source files change.
     *
     * @param filePath Absolute path to source file
     * @return MD5 hex string, "missing" if file doesn't exist, "unknown" for special paths
     */
    fun computeFileSignature(filePath: String): String {
        if (filePath == "<unknown>") return "unknown"
        val file = File(filePath)
        if (!file.exists()) return "missing"
        return file.readBytes().md5()
    }

    /**
     * Combine multiple signatures into one cache signature.
     *
     * Sorts signatures before combining to ensure deterministic output regardless of input order.
     *
     * @param signatures List of individual file signatures
     * @return Combined MD5 signature
     */
    fun computeCombinedSignature(signatures: List<String>): String =
        signatures.sorted().joinToString("|").toByteArray().md5()

    // ========================================================================
    // Private Helpers
    // ========================================================================

    /**
     * Parse ClassId from string format produced by ClassId.asString().
     *
     * The format is "package/parts/ClassName" where:
     * - Package segments are separated by "/"
     * - The last segment after the final "/" is the class name (may contain "." for nested)
     *
     * Examples:
     * - "com/example/UserService" → ClassId(com.example, UserService)
     * - "com/example/Outer.Inner" → ClassId(com.example, Outer.Inner)
     * - "ClassName" → ClassId(ROOT, ClassName) (root package)
     */
    private fun parseClassId(classIdString: String): ClassId {
        val lastSlashIndex = classIdString.lastIndexOf('/')

        // Handle root package (no slash in ClassId string)
        if (lastSlashIndex == -1) {
            return ClassId(FqName.ROOT, FqName(classIdString), isLocal = false)
        }

        // Package is everything before the last slash, with "/" replaced by "."
        val packagePart = classIdString.substring(0, lastSlashIndex).replace('/', '.')
        // Class name is everything after the last slash (may contain "." for nested classes)
        val relativeClassName = classIdString.substring(lastSlashIndex + 1)

        val packageFqName = FqName(packagePart)
        val relativeClassFqName = FqName(relativeClassName)

        return ClassId(packageFqName, relativeClassFqName, isLocal = false)
    }

    private fun ByteArray.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(this).joinToString("") { "%02x".format(it) }
    }

    // Extension functions for FIR → Serializable conversion
    private fun FirTypeParameterInfo.toSerializable() = SerializableTypeParameterInfo(name, bounds)

    private fun FirPropertyInfo.toSerializable() =
        SerializablePropertyInfo(name, type, isMutable, isNullable)

    private fun FirParameterInfo.toSerializable() =
        SerializableParameterInfo(name, type, hasDefaultValue, defaultValueCode, isVararg)

    private fun FirFunctionInfo.toSerializable() =
        SerializableFunctionInfo(
            name = name,
            parameters = parameters.map { it.toSerializable() },
            returnType = returnType,
            isSuspend = isSuspend,
            isInline = isInline,
            typeParameters = typeParameters.map { it.toSerializable() },
            typeParameterBounds = typeParameterBounds,
        )

    // Extension functions for Serializable → FIR conversion
    private fun SerializableTypeParameterInfo.toFir() = FirTypeParameterInfo(name, bounds)

    private fun SerializablePropertyInfo.toFir() =
        FirPropertyInfo(name, type, isMutable, isNullable)

    private fun SerializableParameterInfo.toFir() =
        FirParameterInfo(name, type, hasDefaultValue, defaultValueCode, isVararg)

    private fun SerializableFunctionInfo.toFir() =
        FirFunctionInfo(
            name = name,
            parameters = parameters.map { it.toFir() },
            returnType = returnType,
            isSuspend = isSuspend,
            isInline = isInline,
            typeParameters = typeParameters.map { it.toFir() },
            typeParameterBounds = typeParameterBounds,
        )

    // ========================================================================
    // Annotation Serialization (FIR → Serializable)
    // ========================================================================

    private fun FirAnnotationInfo.toSerializable(): SerializableAnnotationInfo =
        SerializableAnnotationInfo(
            annotationClassId = annotationClassId,
            arguments = arguments.mapValues { (_, arg) -> arg.toSerializable() },
            isOptInMarker = isOptInMarker,
        )

    private fun FirAnnotationArgument.toSerializable(): SerializableAnnotationArgument =
        when (this) {
            is FirAnnotationArgument.ClassReference ->
                SerializableAnnotationArgument(
                    type = SerializableAnnotationArgument.ArgumentType.CLASS_REFERENCE,
                    classId = classId,
                )

            is FirAnnotationArgument.StringLiteral ->
                SerializableAnnotationArgument(
                    type = SerializableAnnotationArgument.ArgumentType.STRING,
                    stringValue = value,
                )

            is FirAnnotationArgument.NumberLiteral ->
                SerializableAnnotationArgument(
                    type = SerializableAnnotationArgument.ArgumentType.NUMBER,
                    stringValue = value,
                )

            is FirAnnotationArgument.BooleanLiteral ->
                SerializableAnnotationArgument(
                    type = SerializableAnnotationArgument.ArgumentType.BOOLEAN,
                    boolValue = value,
                )

            is FirAnnotationArgument.CharLiteral ->
                SerializableAnnotationArgument(
                    type = SerializableAnnotationArgument.ArgumentType.CHAR,
                    charValue = value.toString(),
                )

            is FirAnnotationArgument.EnumValue ->
                SerializableAnnotationArgument(
                    type = SerializableAnnotationArgument.ArgumentType.ENUM,
                    enumClassId = enumClassId,
                    enumEntryName = entryName,
                )

            is FirAnnotationArgument.ArrayValue ->
                SerializableAnnotationArgument(
                    type = SerializableAnnotationArgument.ArgumentType.ARRAY,
                    arrayElements = elements.map { it.toSerializable() },
                )

            is FirAnnotationArgument.NestedAnnotation ->
                SerializableAnnotationArgument(
                    type = SerializableAnnotationArgument.ArgumentType.NESTED_ANNOTATION,
                    nestedAnnotation = annotation.toSerializable(),
                )
        }

    // ========================================================================
    // Annotation Deserialization (Serializable → FIR)
    // ========================================================================

    private fun SerializableAnnotationInfo.toFir(): FirAnnotationInfo =
        FirAnnotationInfo(
            annotationClassId = annotationClassId,
            arguments = arguments.mapValues { (_, arg) -> arg.toFir() },
            isOptInMarker = isOptInMarker,
        )

    private fun SerializableAnnotationArgument.toFir(): FirAnnotationArgument =
        when (type) {
            SerializableAnnotationArgument.ArgumentType.CLASS_REFERENCE ->
                FirAnnotationArgument.ClassReference(classId = classId!!)

            SerializableAnnotationArgument.ArgumentType.STRING ->
                FirAnnotationArgument.StringLiteral(value = stringValue!!)

            SerializableAnnotationArgument.ArgumentType.NUMBER ->
                FirAnnotationArgument.NumberLiteral(value = stringValue!!)

            SerializableAnnotationArgument.ArgumentType.BOOLEAN ->
                FirAnnotationArgument.BooleanLiteral(value = boolValue!!)

            SerializableAnnotationArgument.ArgumentType.CHAR ->
                FirAnnotationArgument.CharLiteral(value = charValue!!.first())

            SerializableAnnotationArgument.ArgumentType.ENUM ->
                FirAnnotationArgument.EnumValue(
                    enumClassId = enumClassId!!,
                    entryName = enumEntryName!!,
                )

            SerializableAnnotationArgument.ArgumentType.ARRAY ->
                FirAnnotationArgument.ArrayValue(elements = arrayElements!!.map { it.toFir() })

            SerializableAnnotationArgument.ArgumentType.NESTED_ANNOTATION ->
                FirAnnotationArgument.NestedAnnotation(annotation = nestedAnnotation!!.toFir())
        }
}
