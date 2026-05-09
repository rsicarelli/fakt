// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.types

import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.kotlinFqName

/**
 * Computes IR-type semantic flags and the [RenderedType] FQN side-channel.
 *
 * Introduced in 3.1.d.1 to hoist two semantic decisions out of the renderers and into a dedicated,
 * testable unit:
 * 1. **`isTypeParameter`** — whether a type is a type-parameter placeholder (T, K, V …). Previously
 *    checked inline inside [TypeRenderer.handleComplexType].
 * 2. **`requiresCollectionErasure`** — whether a type is a stdlib collection whose type arguments
 *    must be erased to `Any` under the NoGenerics pattern. Previously evaluated inside
 *    [GenericTypeHandler.renderGenericType].
 *
 * Both flags are pre-computed during FIR→IR transformation and stored on [IrPropertyMetadata],
 * [IrParameterMetadata], and [IrFunctionMetadata] so that codegen-runtime never needs to traverse
 * [IrType] trees.
 *
 * The [buildRenderedType] method bundles a pre-rendered short name with the set of FQNs referenced
 * by that type — the data that [com.rsicarelli.fakt.compiler.core.context.ImportResolver] will
 * consume in 3.1.d.5.
 *
 * @param typeRenderer Renderer used to produce the short-name half of [RenderedType].
 */
internal class IrTypeSemantics(private val typeRenderer: TypeRenderer) {

    /** Memoise [RenderedType] results to avoid re-computing FQN sets. */
    private val renderedTypeCache = ConcurrentHashMap<Pair<IrType, Boolean>, RenderedType>()

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Returns `true` when [irType] is a type-parameter placeholder (T, K, V …).
     *
     * Mirrors the `is IrTypeParameter` check previously inside [TypeRenderer.handleComplexType].
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun isTypeParameter(irType: IrType): Boolean =
        irType is IrSimpleType && irType.classifier.owner is IrTypeParameter

    /**
     * Returns `true` when [irType] is a stdlib collection whose type arguments must be erased to
     * `Any` under the NoGenerics pattern.
     *
     * Mirrors the erasure rules in [GenericTypeHandler.renderGenericType].
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun requiresCollectionErasure(irType: IrType): Boolean =
        irType is IrSimpleType &&
            irType.arguments.isNotEmpty() &&
            irType.getClass()?.let { irClass ->
                isErasableCollectionClass(
                    irClass.name.asString(),
                    irClass.kotlinFqName.parent().asString(),
                )
            } == true

    /**
     * Renders [irType] and returns a [RenderedType] bundling the short name with all FQNs
     * referenced by that string.
     *
     * Results are memoised in [renderedTypeCache].
     */
    fun buildRenderedType(irType: IrType, preserveTypeParameters: Boolean): RenderedType =
        renderedTypeCache.getOrPut(irType to preserveTypeParameters) {
            val shortName = typeRenderer.render(irType, preserveTypeParameters)
            val fqns = collectFqns(irType, preserveTypeParameters)
            RenderedType(shortName = shortName, fqns = fqns)
        }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /** Returns `true` when [className]/[packageName] identifies a stdlib erasable collection. */
    private fun isErasableCollectionClass(className: String, packageName: String): Boolean =
        when (packageName) {
            "kotlin.collections" -> className in ERASABLE_COLLECTION_CLASSES
            "kotlin" -> className == "Result" || className == "Array"
            else -> false
        }

    /**
     * Recursively collects all FQNs referenced by [irType].
     *
     * Skips primitives, type parameters, and kotlin.* builtins. Descends into type arguments for
     * generic and function types.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun collectFqns(irType: IrType, preserveTypeParameters: Boolean): Set<String> {
        if (typeRenderer.isPrimitive(irType) || isTypeParameter(irType)) return emptySet()
        val baseType = if (irType.isMarkedNullable()) irType.makeNotNull() else irType
        val result = mutableSetOf<String>()
        collectFqnsInto(baseType, preserveTypeParameters, result)
        return result
    }

    /**
     * Fills [into] with FQNs for [irType] and all nested type arguments.
     *
     * Split from [collectFqns] to honour detekt's [ReturnCount] threshold.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun collectFqnsInto(
        irType: IrType,
        preserveTypeParameters: Boolean,
        into: MutableSet<String>,
    ) {
        // Function types: skip the outer FunctionN class — only recurse into arg types
        if (isFunctionType(irType)) {
            collectTypeArgumentFqns(irType, preserveTypeParameters, into)
            return
        }

        if (irType is IrSimpleType) {
            irType.getClass()?.let { irClass ->
                val fqn = irClass.kotlinFqName.asString()
                if (shouldIncludeFqn(fqn)) into.add(fqn)
            }
            if (irType.arguments.isNotEmpty()) {
                collectTypeArgumentFqns(irType, preserveTypeParameters, into)
            }
        }
    }

    /** Collects FQNs from all [IrTypeProjection] arguments of [irType]. */
    private fun collectTypeArgumentFqns(
        irType: IrType,
        preserveTypeParameters: Boolean,
        into: MutableSet<String>,
    ) {
        if (irType !is IrSimpleType) return
        irType.arguments.forEach { arg ->
            if (arg is IrTypeProjection) {
                into.addAll(collectFqns(arg.type, preserveTypeParameters))
            }
        }
    }

    /** Returns `true` when [irType] is a kotlin function or suspend-function type. */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun isFunctionType(irType: IrType): Boolean =
        irType is IrSimpleType &&
            irType.getClass()?.let { irClass ->
                val className = irClass.name.asString()
                val pkg = irClass.kotlinFqName.parent().asString()
                (pkg == "kotlin" && className.startsWith("Function")) ||
                    (pkg == "kotlin.coroutines" && className.startsWith("SuspendFunction"))
            } == true

    /**
     * Returns `true` when [fqn] should appear in the import set.
     *
     * Excludes kotlin.* and kotlin.collections.* stdlib types (auto-imported).
     */
    private fun shouldIncludeFqn(fqn: String): Boolean {
        val pkg = fqn.substringBeforeLast('.', "")
        return pkg.isNotEmpty() && pkg != "kotlin" && !pkg.startsWith("kotlin.")
    }

    private companion object {
        private val ERASABLE_COLLECTION_CLASSES =
            setOf("List", "MutableList", "Set", "MutableSet", "Map", "MutableMap", "Collection")
    }
}
