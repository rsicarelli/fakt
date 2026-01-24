// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.extraction

import com.rsicarelli.fakt.compiler.fir.metadata.FirAnnotationArgument
import com.rsicarelli.fakt.compiler.fir.metadata.FirAnnotationInfo
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.FirArrayLiteral
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.expressions.FirPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.expressions.FirVarargArgumentsExpression
import org.jetbrains.kotlin.fir.references.toResolvedEnumEntrySymbol
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Extracts annotations from FIR declarations for propagation to generated fakes.
 *
 * This extractor:
 * - Filters out the @Fake annotation itself (we don't want to mark generated code as @Fake)
 * - Extracts all annotation arguments (class refs, literals, enums, arrays, nested)
 * - Converts FIR annotation data to serializable [FirAnnotationInfo]
 *
 * **Thread Safety**: Stateless object, safe for concurrent access.
 *
 * **Metro Alignment**: Follows FIR-phase annotation extraction pattern.
 *
 * **API Reference** (Kotlin 2.0-2.1.x stable):
 * - FirAnnotation.argumentMapping.mapping: Map<Name, FirExpression>
 * - FirArrayLiteral.argumentList.arguments: List<FirExpression>
 * - FirGetClassCall.argument: FirExpression (singular!)
 * - FirVarargArgumentsExpression.arguments: List<FirExpression>
 */
object AnnotationExtractor {
    /**
     * The @Fake annotation ClassId - filtered out during extraction.
     */
    private val FAKE_ANNOTATION_CLASS_ID = ClassId.topLevel(FqName("com.rsicarelli.fakt.Fake"))

    /**
     * The @RequiresOptIn annotation ClassId - used to detect opt-in marker annotations.
     */
    private val REQUIRES_OPT_IN_CLASS_ID = ClassId.topLevel(FqName("kotlin.RequiresOptIn"))

    /**
     * Extract all annotations from a FIR class declaration.
     *
     * Filters out the @Fake annotation since generated fakes shouldn't be marked
     * as @Fake themselves.
     *
     * @param declaration The FIR class declaration to extract annotations from
     * @param session The FIR session for resolving annotation class IDs
     * @return List of extracted annotation info, excluding @Fake
     */
    fun extractAnnotations(
        declaration: FirClass,
        session: FirSession,
    ): List<FirAnnotationInfo> =
        declaration.annotations
            .filter { annotation ->
                // Filter out @Fake annotation
                annotation.toAnnotationClassIdSafe(session) != FAKE_ANNOTATION_CLASS_ID
            }.mapNotNull { annotation ->
                extractAnnotationInfo(annotation, session)
            }

    /**
     * Extract annotation info from a single FIR annotation.
     *
     * @param annotation The FIR annotation to extract
     * @param session The FIR session for type resolution
     * @return Extracted annotation info, or null if extraction fails
     */
    private fun extractAnnotationInfo(
        annotation: FirAnnotation,
        session: FirSession,
    ): FirAnnotationInfo? {
        // Get annotation class ID
        val annotationClassId =
            annotation.toAnnotationClassIdSafe(session)
                ?: return null

        // Extract arguments using argumentMapping.mapping (correct API!)
        val arguments = extractArguments(annotation.argumentMapping, session)

        // Check if this annotation is marked with @RequiresOptIn
        val isOptInMarker = isAnnotationOptInMarker(annotationClassId, session)

        return FirAnnotationInfo(
            annotationClassId = annotationClassId.asString(),
            arguments = arguments,
            isOptInMarker = isOptInMarker,
        )
    }

    /**
     * Check if an annotation class is marked with @RequiresOptIn.
     *
     * This is used to detect custom opt-in marker annotations like:
     * ```kotlin
     * @RequiresOptIn(level = RequiresOptIn.Level.ERROR)
     * annotation class InternalPlatformApi
     * ```
     *
     * When such an annotation is on the source interface/class, the generated fake
     * needs `@OptIn(InternalPlatformApi::class)` to compile.
     *
     * @param annotationClassId The class ID of the annotation to check
     * @param session The FIR session for resolving the annotation class
     * @return True if the annotation class has @RequiresOptIn
     */
    @OptIn(SymbolInternals::class)
    private fun isAnnotationOptInMarker(
        annotationClassId: ClassId,
        session: FirSession,
    ): Boolean {
        // Look up the annotation class symbol
        val annotationClassSymbol =
            session.symbolProvider
                .getClassLikeSymbolByClassId(annotationClassId) as? FirRegularClassSymbol
                ?: return false

        // Check if the annotation class has @RequiresOptIn
        return annotationClassSymbol.annotations.any { ann ->
            ann.toAnnotationClassIdSafe(session) == REQUIRES_OPT_IN_CLASS_ID
        }
    }

    /**
     * Extract named arguments from annotation argument mapping.
     *
     * @param mapping The argument mapping from FIR annotation
     * @param session The FIR session for type resolution
     * @return Map of argument names to their values
     */
    private fun extractArguments(
        mapping: FirAnnotationArgumentMapping,
        session: FirSession,
    ): Map<String, FirAnnotationArgument> {
        val result = mutableMapOf<String, FirAnnotationArgument>()

        // Use mapping.mapping which is Map<Name, FirExpression>
        mapping.mapping.forEach { (name, expression) ->
            val argValue = extractArgumentValue(expression, session)
            if (argValue != null) {
                result[name.asString()] = argValue
            }
        }

        return result
    }

    /**
     * Extract a single argument value from a FIR expression.
     *
     * Handles:
     * - Class references (Foo::class)
     * - String/number/boolean/char literals
     * - Enum values (EnumClass.ENTRY)
     * - Arrays ([a, b, c])
     * - Varargs (spread arrays)
     * - Nested annotations
     *
     * @param expression The FIR expression to extract
     * @param session The FIR session for type resolution
     * @return Extracted argument value, or null if not supported
     */
    private fun extractArgumentValue(
        expression: FirExpression,
        session: FirSession,
    ): FirAnnotationArgument? =
        when (expression) {
            // Class reference: Foo::class
            is FirGetClassCall -> extractClassReference(expression)

            // Literals: "string", 42, true, 'c'
            is FirLiteralExpression -> extractLiteral(expression)

            // Enum value: EnumClass.ENTRY
            is FirPropertyAccessExpression -> extractEnumValue(expression)

            // Array literal: [a, b, c]
            is FirArrayLiteral -> extractArrayValue(expression, session)

            // Vararg arguments (spread into array)
            is FirVarargArgumentsExpression -> extractVarargValue(expression, session)

            // Nested annotation: @Nested("value")
            is FirAnnotation -> extractNestedAnnotation(expression, session)

            else -> null // Unsupported expression type
        }

    /**
     * Extract class reference from FirGetClassCall.
     *
     * Handles `Foo::class` expressions.
     * Uses `argument` (singular!) property - the FirExpression being referenced.
     */
    private fun extractClassReference(expression: FirGetClassCall): FirAnnotationArgument.ClassReference? {
        // Get the argument of ::class using singular `argument` property
        val argument = expression.argument

        // For class literals, the argument is typically a FirResolvedQualifier
        if (argument is FirResolvedQualifier) {
            val classId = argument.classId ?: return null
            return FirAnnotationArgument.ClassReference(classId.asString())
        }

        // Fallback: try to get from the resolved type
        return try {
            val resolvedType = argument.resolvedType as? ConeClassLikeType ?: return null
            val classId = resolvedType.lookupTag.classId
            FirAnnotationArgument.ClassReference(classId.asString())
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extract literal value from FirLiteralExpression.
     *
     * Handles strings, numbers, booleans, and chars.
     */
    private fun extractLiteral(expression: FirLiteralExpression): FirAnnotationArgument? {
        val value = expression.value ?: return null

        return when (value) {
            is String -> FirAnnotationArgument.StringLiteral(value)
            is Boolean -> FirAnnotationArgument.BooleanLiteral(value)
            is Char -> FirAnnotationArgument.CharLiteral(value)
            is Number -> FirAnnotationArgument.NumberLiteral(value.toString())
            else -> null
        }
    }

    /**
     * Extract enum value from FirPropertyAccessExpression.
     *
     * Handles `EnumClass.ENTRY` expressions.
     */
    private fun extractEnumValue(expression: FirPropertyAccessExpression): FirAnnotationArgument.EnumValue? {
        val enumEntrySymbol = expression.calleeReference.toResolvedEnumEntrySymbol() ?: return null
        val enumClassId = enumEntrySymbol.callableId.classId ?: return null
        val entryName = enumEntrySymbol.name.asString()

        return FirAnnotationArgument.EnumValue(
            enumClassId = enumClassId.asString(),
            entryName = entryName,
        )
    }

    /**
     * Extract array value from FirArrayLiteral.
     *
     * Handles `[a, b, c]` expressions.
     * Uses `argumentList.arguments` (NOT direct `arguments` property!).
     */
    private fun extractArrayValue(
        expression: FirArrayLiteral,
        session: FirSession,
    ): FirAnnotationArgument.ArrayValue {
        // FirArrayLiteral implements FirCall, access via argumentList.arguments
        val elements =
            expression.argumentList.arguments.mapNotNull { element ->
                extractArgumentValue(element, session)
            }
        return FirAnnotationArgument.ArrayValue(elements)
    }

    /**
     * Extract vararg value from FirVarargArgumentsExpression.
     *
     * Handles vararg parameters spread as arrays.
     * Uses direct `arguments` property (unlike FirArrayLiteral!).
     */
    private fun extractVarargValue(
        expression: FirVarargArgumentsExpression,
        session: FirSession,
    ): FirAnnotationArgument.ArrayValue {
        // FirVarargArgumentsExpression has direct `arguments` property
        val elements =
            expression.arguments.mapNotNull { element ->
                extractArgumentValue(element, session)
            }
        return FirAnnotationArgument.ArrayValue(elements)
    }

    /**
     * Extract nested annotation from FirAnnotation.
     *
     * Handles `@Nested("value")` expressions.
     */
    private fun extractNestedAnnotation(
        expression: FirAnnotation,
        session: FirSession,
    ): FirAnnotationArgument.NestedAnnotation? {
        val annotationInfo = extractAnnotationInfo(expression, session) ?: return null
        return FirAnnotationArgument.NestedAnnotation(annotationInfo)
    }
}
