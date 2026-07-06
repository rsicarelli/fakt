// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.types

import com.rsicarelli.fakt.compiler.fir.metadata.FirRenderedType
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeFlexibleType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.ConeTypeParameterType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * Renders [ConeKotlinType]s to Kotlin source strings plus the FQN import side-channel — the FIR
 * counterpart of the IR stack (`TypeRenderer`/`GenericTypeHandler`/`FunctionTypeHandler`/
 * `IrTypeSemantics`).
 *
 * **Parity contract**: output must be byte-identical to the IR renderer for the same source type
 * (locked by `FirIrEmissionParityTest`). Notable shared semantics:
 * - typealiases render fully expanded ([fullyExpandedType] — IR types arrive pre-expanded)
 * - nullable (suspend) function types are parenthesized: `((Int) -> Unit)?`
 * - star projections render `*` inside generic arguments but `Any` inside function-type
 *   parameter/return positions
 * - `in`/`out` projection keywords are never emitted
 * - under NoGenerics (preserveTypeParameters = false) type parameters erase to `Any` and stdlib
 *   collections erase per the fixed table (`List<Any>`, `Map<Any, Any>`, …)
 * - collected FQNs exclude primitives, type parameters, the outer `FunctionN` class, and
 *   `kotlin`/`kotlin.*` packages
 *
 * No memoization: instances are cheap, sessions vary per compilation, and FIR extraction visits
 * each member exactly once (unlike the IR path, which re-renders across transform stages).
 *
 * Defensive behavior (shapes the IR path never sees in valid code): flexible (Java platform) types
 * render their lower bound; error types fall back to the diagnostic-free simple name.
 */
internal object FirTypeRenderer {

    /**
     * Renders [coneType] and bundles the string with its FQN set and semantic flags.
     *
     * @param coneType Type to render (may be a typealias abbreviation; expanded internally)
     * @param session Use-site session for typealias expansion
     * @param preserveTypeParameters Whether type parameters render by name (`T`) or erase to `Any`
     */
    fun buildRendered(
        coneType: ConeKotlinType,
        session: FirSession,
        preserveTypeParameters: Boolean,
    ): FirRenderedType =
        FirRenderedType(
            shortName = render(coneType, session, preserveTypeParameters),
            fqns = collectFqns(coneType, session),
            isTypeParameter = isTypeParameter(coneType, session),
            requiresCollectionErasure = requiresCollectionErasure(coneType, session),
        )

    /** Renders [coneType] to its Kotlin source string. */
    fun render(
        coneType: ConeKotlinType,
        session: FirSession,
        preserveTypeParameters: Boolean,
    ): String {
        val expanded = coneType.expand(session)
        val base = renderNotNull(expanded, session, preserveTypeParameters)

        return if (expanded.isMarkedNullable) {
            // Function types need parentheses when nullable: ((Int, Int) -> Unit)?
            if (expanded.isFunctionType() || expanded.isSuspendFunctionType()) "($base)?"
            else "$base?"
        } else {
            base
        }
    }

    /** Returns `true` when [coneType] is a type-parameter placeholder (T, K, V …). */
    fun isTypeParameter(coneType: ConeKotlinType, session: FirSession): Boolean =
        coneType.expand(session) is ConeTypeParameterType

    /**
     * Returns `true` when [coneType] is a stdlib collection whose type arguments must be erased to
     * `Any` under the NoGenerics pattern.
     */
    fun requiresCollectionErasure(coneType: ConeKotlinType, session: FirSession): Boolean {
        val expanded = coneType.expand(session)
        return expanded is ConeClassLikeType &&
            expanded.typeArguments.isNotEmpty() &&
            expanded.classId?.let { isErasableCollectionClass(it) } == true
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    /** Renders the not-null projection of an already-expanded [coneType]. */
    private fun renderNotNull(
        coneType: ConeKotlinType,
        session: FirSession,
        preserveTypeParameters: Boolean,
    ): String =
        when (coneType) {
            is ConeErrorType -> coneType.fallbackName()
            is ConeFlexibleType ->
                renderNotNull(coneType.lowerBound, session, preserveTypeParameters)
            is ConeTypeParameterType ->
                if (preserveTypeParameters) coneType.lookupTag.name.asString() else "Any"
            is ConeClassLikeType -> renderClassLike(coneType, session, preserveTypeParameters)
            else -> coneType.toString().substringAfterLast('.')
        }

    private fun renderClassLike(
        coneType: ConeClassLikeType,
        session: FirSession,
        preserveTypeParameters: Boolean,
    ): String {
        coneType.asPrimitiveName()?.let {
            return it
        }

        return when {
            coneType.isFunctionType() ->
                renderFunctionType(coneType, session, preserveTypeParameters)
            coneType.isSuspendFunctionType() ->
                "suspend ${renderFunctionType(coneType, session, preserveTypeParameters)}"
            coneType.typeArguments.isNotEmpty() ->
                renderGenericType(coneType, session, preserveTypeParameters)
            else -> coneType.simpleName()
        }
    }

    /** Renders `(A, B) -> R` from a `FunctionN`/`SuspendFunctionN` type (arity from class name). */
    private fun renderFunctionType(
        coneType: ConeClassLikeType,
        session: FirSession,
        preserveTypeParameters: Boolean,
    ): String {
        val className = coneType.simpleName()
        val arityString =
            when {
                className.startsWith("SuspendFunction") -> className.removePrefix("SuspendFunction")
                className.startsWith("Function") -> className.removePrefix("Function")
                else -> ""
            }
        val arity = arityString.toIntOrNull() ?: 0

        if (coneType.typeArguments.size == arity + 1) {
            // Star projections render "Any" in function-type positions (IR renderer parity)
            fun ConeTypeProjection.renderOrAny(): String =
                type?.let { render(it, session, preserveTypeParameters) } ?: "Any"

            val paramTypes = coneType.typeArguments.take(arity).map { it.renderOrAny() }
            val returnType = coneType.typeArguments.lastOrNull()?.renderOrAny() ?: "Unit"

            return if (paramTypes.isEmpty()) "() -> $returnType"
            else "(${paramTypes.joinToString(", ")}) -> $returnType"
        }

        // Fallback (malformed arity mismatch)
        return "() -> Unit"
    }

    private fun renderGenericType(
        coneType: ConeClassLikeType,
        session: FirSession,
        preserveTypeParameters: Boolean,
    ): String {
        val classId = coneType.classId ?: return "Any"
        val className = classId.shortClassName.asString()

        return when {
            preserveTypeParameters -> {
                val args =
                    coneType.typeArguments.map { projection ->
                        when (projection) {
                            is ConeStarProjection -> "*"
                            // Variance keywords (in/out) are dropped — IR renderer emits none
                            else ->
                                projection.type?.let { render(it, session, preserveTypeParameters) }
                                    ?: "*"
                        }
                    }
                "$className<${args.joinToString(", ")}>"
            }

            // NoGenerics pattern: fixed type-erasure rules for common stdlib types
            classId.packageMatches("kotlin.collections") &&
                className in listOf("List", "MutableList") -> "List<Any>"
            classId.packageMatches("kotlin.collections") &&
                className in listOf("Set", "MutableSet") -> "Set<Any>"
            classId.packageMatches("kotlin.collections") &&
                className in listOf("Map", "MutableMap") -> "Map<Any, Any>"
            classId.packageMatches("kotlin.collections") && className == "Collection" ->
                "Collection<Any>"
            classId.packageMatches("kotlin") && className == "Result" -> "Result<Any>"
            classId.packageMatches("kotlin") && className == "Array" -> "Array<Any>"
            else -> className
        }
    }

    // -------------------------------------------------------------------------
    // FQN collection (import side-channel)
    // -------------------------------------------------------------------------

    /**
     * Recursively collects all FQNs referenced by [coneType]. Skips primitives and type parameters;
     * for function types only the argument types contribute (the outer `FunctionN` class never
     * needs importing).
     */
    private fun collectFqns(coneType: ConeKotlinType, session: FirSession): Set<String> {
        val expanded = coneType.expand(session)
        if (expanded is ConeTypeParameterType) return emptySet()
        val result = mutableSetOf<String>()
        collectFqnsInto(expanded, session, result)
        return result
    }

    private fun collectFqnsInto(
        coneType: ConeKotlinType,
        session: FirSession,
        into: MutableSet<String>,
    ) {
        when (val expanded = coneType.expand(session)) {
            is ConeErrorType -> Unit
            is ConeFlexibleType -> collectFqnsInto(expanded.lowerBound, session, into)
            is ConeClassLikeType -> {
                if (expanded.isFunctionType() || expanded.isSuspendFunctionType()) {
                    // Function types: skip the outer FunctionN class — recurse into args only
                    collectTypeArgumentFqns(expanded, session, into)
                    return
                }
                expanded.classId?.let { classId ->
                    val fqn = classId.asSingleFqName().asString()
                    if (shouldIncludeFqn(fqn)) into.add(fqn)
                }
                if (expanded.typeArguments.isNotEmpty()) {
                    collectTypeArgumentFqns(expanded, session, into)
                }
            }
            else -> Unit
        }
    }

    private fun collectTypeArgumentFqns(
        coneType: ConeClassLikeType,
        session: FirSession,
        into: MutableSet<String>,
    ) {
        coneType.typeArguments.forEach { projection ->
            projection.type?.let { into.addAll(collectFqns(it, session)) }
        }
    }

    /**
     * Returns `true` when [fqn] should appear in the import set. Excludes kotlin.* stdlib types
     * (auto-imported). Mirrors `IrTypeSemantics.shouldIncludeFqn` exactly.
     */
    private fun shouldIncludeFqn(fqn: String): Boolean {
        val pkg = fqn.substringBeforeLast('.', "")
        return pkg.isNotEmpty() && pkg != "kotlin" && !pkg.startsWith("kotlin.")
    }

    // -------------------------------------------------------------------------
    // Classification helpers
    // -------------------------------------------------------------------------

    /** Expands typealiases; non-class-like types pass through unchanged. */
    private fun ConeKotlinType.expand(session: FirSession): ConeKotlinType =
        if (this is ConeClassLikeType) fullyExpandedType(session) else this

    private val PRIMITIVE_NAMES: Map<ClassId, String> =
        mapOf(
            StandardClassIds.String to "String",
            StandardClassIds.Int to "Int",
            StandardClassIds.Boolean to "Boolean",
            StandardClassIds.Unit to "Unit",
            StandardClassIds.Long to "Long",
            StandardClassIds.Float to "Float",
            StandardClassIds.Double to "Double",
            StandardClassIds.Char to "Char",
            StandardClassIds.Byte to "Byte",
            StandardClassIds.Short to "Short",
            StandardClassIds.Nothing to "Nothing",
            StandardClassIds.Any to "Any",
        )

    /** Returns the primitive short name, or null when [this] is not a primitive builtin. */
    private fun ConeClassLikeType.asPrimitiveName(): String? =
        if (typeArguments.isEmpty()) classId?.let(PRIMITIVE_NAMES::get) else null

    private fun ConeKotlinType.isFunctionType(): Boolean {
        val classId = (this as? ConeClassLikeType)?.classId ?: return false
        return classId.packageFqName.asString() == "kotlin" &&
            classId.shortClassName.asString().startsWith("Function")
    }

    private fun ConeKotlinType.isSuspendFunctionType(): Boolean {
        val classId = (this as? ConeClassLikeType)?.classId ?: return false
        return classId.packageFqName.asString() == "kotlin.coroutines" &&
            classId.shortClassName.asString().startsWith("SuspendFunction")
    }

    private fun isErasableCollectionClass(classId: ClassId): Boolean {
        val className = classId.shortClassName.asString()
        return when (classId.packageFqName.asString()) {
            "kotlin.collections" -> className in ERASABLE_COLLECTION_CLASSES
            "kotlin" -> className == "Result" || className == "Array"
            else -> false
        }
    }

    private fun ConeClassLikeType.simpleName(): String =
        classId?.shortClassName?.asString() ?: toString().substringAfterLast('.')

    private fun ClassId.packageMatches(pkg: String): Boolean = packageFqName.asString() == pkg

    private fun ConeErrorType.fallbackName(): String = toString().substringAfterLast('.')

    private val ConeClassLikeType.classId: ClassId?
        get() = lookupTag.classId

    private val ERASABLE_COLLECTION_CLASSES =
        setOf("List", "MutableList", "Set", "MutableSet", "Map", "MutableMap", "Collection")
}
