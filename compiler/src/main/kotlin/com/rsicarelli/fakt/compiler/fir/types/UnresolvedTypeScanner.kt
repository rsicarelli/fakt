// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.types

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedError
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.ClassId

/**
 * Finds unresolved (error) types that a `@Fake` declaration depends on: its supertypes,
 * type-parameter bounds, member signatures and the same for every supertype it inherits from.
 *
 * Unresolved types appear when the code needs another compiler plugin that Fakt's own analysis does
 * not load (for example `kotlinx.serialization`). A fake built from such a signature would not
 * compile, so the checkers report a `[FAKT]` error instead of emitting it.
 */
internal object UnresolvedTypeScanner {

    /** Names of every unresolved type reached from [declaration], in first-seen order. */
    fun scan(declaration: FirClass, session: FirSession): List<String> {
        val found = linkedSetOf<String>()
        scanClass(declaration, session, mutableSetOf(), found)
        return found.toList()
    }

    private fun scanClass(
        declaration: FirClass,
        session: FirSession,
        visited: MutableSet<ClassId>,
        found: MutableSet<String>,
    ) {
        if (!visited.add(declaration.classId)) return
        declaration.typeParameters.forEach { ref -> scanTypeParameter(ref, found) }
        scanMembers(declaration, session, found)
        declaration.superTypeRefs.forEach { ref ->
            collect(ref.coneType, found)
            superClassOf(ref, session)?.let { scanClass(it, session, visited, found) }
        }
    }

    @OptIn(SymbolInternals::class)
    private fun scanTypeParameter(
        ref: org.jetbrains.kotlin.fir.declarations.FirTypeParameterRef,
        found: MutableSet<String>,
    ) {
        ref.symbol.fir.bounds.forEach { bound -> collect(bound.coneType, found) }
    }

    @OptIn(SymbolInternals::class)
    private fun scanMembers(declaration: FirClass, session: FirSession, found: MutableSet<String>) {
        declaration.processAllDeclarations(session = declaration.moduleData.session) { symbol ->
            when (symbol) {
                is FirPropertySymbol -> scanProperty(symbol.fir, found)
                is FirFunctionSymbol<*> -> scanFunction(symbol.fir, found)
                else -> Unit
            }
        }
    }

    private fun scanProperty(property: FirProperty, found: MutableSet<String>) {
        collect(property.returnTypeRef.coneType, found)
        property.receiverParameter?.typeRef?.let { collect(it.coneType, found) }
    }

    @OptIn(SymbolInternals::class)
    private fun scanFunction(function: FirFunction, found: MutableSet<String>) {
        collect(function.returnTypeRef.coneType, found)
        function.receiverParameter?.typeRef?.let { collect(it.coneType, found) }
        function.valueParameters.forEach { collect(it.returnTypeRef.coneType, found) }
        function.typeParameters.forEach { scanTypeParameter(it, found) }
    }

    private fun superClassOf(ref: FirTypeRef, session: FirSession): FirClass? {
        val type = ref.coneType as? ConeClassLikeType ?: return null
        return (type.lookupTag.toSymbol(session) as? FirClassSymbol<*>)?.fir()
    }

    @OptIn(SymbolInternals::class) private fun FirClassSymbol<*>.fir(): FirClass = fir

    private fun collect(type: ConeKotlinType, found: MutableSet<String>) {
        if (type is ConeErrorType) {
            found += type.unresolvedName()
            return
        }
        type.typeArguments.forEach { argument -> argument.type?.let { collect(it, found) } }
    }

    private fun ConeErrorType.unresolvedName(): String =
        (diagnostic as? ConeUnresolvedError)?.qualifier
            ?: toString().substringAfterLast('.').ifBlank { "<unknown>" }
}
