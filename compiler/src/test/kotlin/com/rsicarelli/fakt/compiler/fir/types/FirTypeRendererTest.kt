// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.types

import com.rsicarelli.fakt.compiler.fir.metadata.FirRenderedType
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.processAllDeclarations
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * BDD matrix for [FirTypeRenderer] over real [org.jetbrains.kotlin.fir.types.ConeKotlinType]s.
 *
 * Harness: each test compiles a snippet with a probe FIR checker that renders every member type of
 * the class named `Probe` and records the result keyed by member name (`fn.param` for parameters,
 * `fn.<receiver>` for extension receivers).
 *
 * The expectations encode the IR renderer's exact semantics — a row that fails here after a
 * renderer change signals a parity break with `TypeRenderer`/`GenericTypeHandler`/
 * `FunctionTypeHandler`/`IrTypeSemantics`.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FirTypeRendererTest {

    @Test
    fun `GIVEN primitive property types WHEN rendering THEN primitive short names are used`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                interface Probe {
                    val a: String; val b: Int; val c: Boolean; val d: Long; val e: Float
                    val f: Double; val g: Char; val h: Byte; val i: Short
                    fun u(): Unit
                    fun n(): Nothing
                    fun any(): Any
                }
                """
            )

        // THEN
        assertEquals("String", rendered.getValue("a").shortName)
        assertEquals("Int", rendered.getValue("b").shortName)
        assertEquals("Boolean", rendered.getValue("c").shortName)
        assertEquals("Long", rendered.getValue("d").shortName)
        assertEquals("Float", rendered.getValue("e").shortName)
        assertEquals("Double", rendered.getValue("f").shortName)
        assertEquals("Char", rendered.getValue("g").shortName)
        assertEquals("Byte", rendered.getValue("h").shortName)
        assertEquals("Short", rendered.getValue("i").shortName)
        assertEquals("Unit", rendered.getValue("u").shortName)
        assertEquals("Nothing", rendered.getValue("n").shortName)
        assertEquals("Any", rendered.getValue("any").shortName)
    }

    @Test
    fun `GIVEN nullable primitive WHEN rendering THEN question mark is appended`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                interface Probe { val a: String? }
                """
            )

        // THEN
        assertEquals("String?", rendered.getValue("a").shortName)
    }

    @Test
    fun `GIVEN user class property WHEN rendering THEN short name and FQN are captured`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                class User
                interface Probe { val u: User }
                """
            )

        // THEN
        assertEquals("User", rendered.getValue("u").shortName)
        assertEquals(setOf("test.User"), rendered.getValue("u").fqns)
    }

    @Test
    fun `GIVEN nested generic type WHEN rendering THEN arguments recurse and FQNs are collected`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                class User
                interface Probe { val m: Map<String, List<User>> }
                """
            )

        // THEN: kotlin.* FQNs are excluded from imports; nested user types are kept
        assertEquals("Map<String, List<User>>", rendered.getValue("m").shortName)
        assertEquals(setOf("test.User"), rendered.getValue("m").fqns)
        assertTrue(rendered.getValue("m").requiresCollectionErasure)
    }

    @Test
    fun `GIVEN class-level type parameter WHEN rendering preserved THEN parameter name is kept`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                interface Probe<T> { val t: T }
                """
            )

        // THEN
        assertEquals("T", rendered.getValue("t").shortName)
        assertTrue(rendered.getValue("t").isTypeParameter)
        assertEquals(emptySet(), rendered.getValue("t").fqns)
    }

    @Test
    fun `GIVEN class-level type parameter WHEN rendering erased THEN Any is used`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                interface Probe<T> { val t: T }
                """,
                preserveTypeParameters = false,
            )

        // THEN
        assertEquals("Any", rendered.getValue("t").shortName)
    }

    @Test
    fun `GIVEN stdlib collections WHEN rendering erased THEN fixed erasure table applies`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                interface Probe<T> {
                    val a: List<T>; val b: MutableList<T>; val c: Set<T>; val d: MutableSet<T>
                    val e: Map<T, T>; val f: MutableMap<T, T>; val g: Collection<T>
                    val h: Result<T>; val i: Array<T>
                }
                """,
                preserveTypeParameters = false,
            )

        // THEN
        assertEquals("List<Any>", rendered.getValue("a").shortName)
        assertEquals("List<Any>", rendered.getValue("b").shortName)
        assertEquals("Set<Any>", rendered.getValue("c").shortName)
        assertEquals("Set<Any>", rendered.getValue("d").shortName)
        assertEquals("Map<Any, Any>", rendered.getValue("e").shortName)
        assertEquals("Map<Any, Any>", rendered.getValue("f").shortName)
        assertEquals("Collection<Any>", rendered.getValue("g").shortName)
        assertEquals("Result<Any>", rendered.getValue("h").shortName)
        assertEquals("Array<Any>", rendered.getValue("i").shortName)
    }

    @Test
    fun `GIVEN star projection WHEN rendering THEN star is preserved in generic arguments`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                interface Probe { val l: List<*> }
                """
            )

        // THEN
        assertEquals("List<*>", rendered.getValue("l").shortName)
    }

    @Test
    fun `GIVEN use-site variance WHEN rendering THEN in and out keywords are dropped`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                class Box<T>
                interface Probe {
                    val o: Box<out Number>
                    val i: Box<in Int>
                }
                """
            )

        // THEN: IR renderer parity — projections render their type, never the keyword
        assertEquals("Box<Number>", rendered.getValue("o").shortName)
        assertEquals("Box<Int>", rendered.getValue("i").shortName)
        assertEquals(setOf("test.Box"), rendered.getValue("o").fqns)
    }

    @Test
    fun `GIVEN function type WHEN rendering THEN arrow syntax is used`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                interface Probe {
                    val f: (Int, String) -> Boolean
                    val zero: () -> Unit
                }
                """
            )

        // THEN
        assertEquals("(Int, String) -> Boolean", rendered.getValue("f").shortName)
        assertEquals("() -> Unit", rendered.getValue("zero").shortName)
        assertEquals(emptySet(), rendered.getValue("f").fqns)
    }

    @Test
    fun `GIVEN suspend function type WHEN rendering THEN suspend prefix is emitted`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                interface Probe { val f: suspend (Int) -> Unit }
                """
            )

        // THEN
        assertEquals("suspend (Int) -> Unit", rendered.getValue("f").shortName)
    }

    @Test
    fun `GIVEN nullable function types WHEN rendering THEN parentheses wrap before question mark`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                interface Probe {
                    val f: ((Int) -> Unit)?
                    val s: (suspend (Int) -> Unit)?
                }
                """
            )

        // THEN
        assertEquals("((Int) -> Unit)?", rendered.getValue("f").shortName)
        assertEquals("(suspend (Int) -> Unit)?", rendered.getValue("s").shortName)
    }

    @Test
    fun `GIVEN extension function type WHEN rendering THEN receiver becomes first parameter`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                interface Probe { val e: Int.(String) -> Boolean }
                """
            )

        // THEN: IR renderer parity — FunctionN args are rendered flat
        assertEquals("(Int, String) -> Boolean", rendered.getValue("e").shortName)
    }

    @Test
    fun `GIVEN function type with user types WHEN rendering THEN outer FunctionN is not imported`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                class User
                interface Probe { val f: (User) -> User }
                """
            )

        // THEN: only argument FQNs are collected, never kotlin.FunctionN
        assertEquals("(User) -> User", rendered.getValue("f").shortName)
        assertEquals(setOf("test.User"), rendered.getValue("f").fqns)
    }

    @Test
    fun `GIVEN typealias in signature WHEN rendering THEN alias is fully expanded`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                typealias Handler = (Int) -> String
                typealias Names = List<String>
                interface Probe {
                    val h: Handler
                    val n: Names
                }
                """
            )

        // THEN: IR types arrive expanded, so FIR must expand for parity
        assertEquals("(Int) -> String", rendered.getValue("h").shortName)
        assertEquals("List<String>", rendered.getValue("n").shortName)
    }

    @Test
    fun `GIVEN vararg parameter WHEN rendering THEN array type is rendered`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                interface Probe { fun f(vararg names: String) }
                """
            )

        // THEN: FIR parameter types for vararg are the array form, and the implicit `out`
        // projection is dropped like every other variance keyword (IR renderer parity)
        assertEquals("Array<String>", rendered.getValue("f.names").shortName)
    }

    @Test
    fun `GIVEN extension receiver on member WHEN rendering THEN receiver type is captured`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                class User
                interface Probe { fun User.describe(): String }
                """
            )

        // THEN
        assertEquals("User", rendered.getValue("describe.<receiver>").shortName)
        assertEquals(setOf("test.User"), rendered.getValue("describe.<receiver>").fqns)
    }

    @Test
    fun `GIVEN flexible java type WHEN rendering THEN lower bound is used`() {
        // GIVEN: an inferred platform type (String!) from a Java API
        val rendered =
            renderMembers(
                """
                package test
                interface Probe { fun f() = System.getProperty("x") }
                """
            )

        // THEN: defensive — flexible types render their lower bound
        assertEquals("String", rendered.getValue("f").shortName)
    }

    @Test
    fun `GIVEN java interop class WHEN rendering THEN non-kotlin FQN is collected`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                import java.time.Duration
                interface Probe { val d: Duration }
                """
            )

        // THEN
        assertEquals("Duration", rendered.getValue("d").shortName)
        assertEquals(setOf("java.time.Duration"), rendered.getValue("d").fqns)
    }

    @Test
    fun `GIVEN nullable user generic WHEN rendering THEN nullability and arguments compose`() {
        // GIVEN
        val rendered =
            renderMembers(
                """
                package test
                class User
                interface Probe { fun find(id: String): List<User>? }
                """
            )

        // THEN
        assertEquals("List<User>?", rendered.getValue("find").shortName)
        assertEquals("String", rendered.getValue("find.id").shortName)
        assertEquals(setOf("test.User"), rendered.getValue("find").fqns)
    }

    // -------------------------------------------------------------------------
    // Harness
    // -------------------------------------------------------------------------

    /**
     * Compiles [source] with a probe FIR checker and returns the rendered types of every member of
     * the class named `Probe`, keyed by member name (`fn.param`, `fn.<receiver>`).
     */
    private fun renderMembers(
        source: String,
        preserveTypeParameters: Boolean = true,
    ): Map<String, FirRenderedType> {
        val sink = ConcurrentHashMap<String, FirRenderedType>()
        val result =
            KotlinCompilation()
                .apply {
                    sources = listOf(SourceFile.kotlin("Probe.kt", source.trimIndent()))
                    compilerPluginRegistrars = listOf(ProbeRegistrar(sink, preserveTypeParameters))
                    inheritClassPath = true
                    messageOutputStream = System.out
                }
                .compile()

        assertEquals(
            KotlinCompilation.ExitCode.OK,
            result.exitCode,
            "Probe snippet failed to compile:\n${result.messages}",
        )
        assertTrue(sink.isNotEmpty(), "Probe checker captured no members — harness regression")
        return sink
    }
}

@OptIn(ExperimentalCompilerApi::class)
private class ProbeRegistrar(
    private val sink: MutableMap<String, FirRenderedType>,
    private val preserve: Boolean,
) : CompilerPluginRegistrar() {
    override val pluginId: String = "com.rsicarelli.fakt.test.fir-type-renderer-probe"
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(
            object : FirExtensionRegistrar() {
                @Suppress("ktlint:standard:curly-spacing")
                override fun ExtensionRegistrarContext.configurePlugin() =
                    +{ session: FirSession -> ProbeCheckersExtension(session, sink, preserve) }
            }
        )
    }
}

private class ProbeCheckersExtension(
    session: FirSession,
    sink: MutableMap<String, FirRenderedType>,
    preserve: Boolean,
) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers =
        object : DeclarationCheckers() {
            override val classCheckers: Set<FirClassChecker> = setOf(ProbeChecker(sink, preserve))
        }
}

private class ProbeChecker(
    private val sink: MutableMap<String, FirRenderedType>,
    private val preserve: Boolean,
) : FirClassChecker(MppCheckerKind.Common) {
    @OptIn(SymbolInternals::class)
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirClass) {
        if (declaration.classId.shortClassName.asString() != "Probe") return
        val session = context.session

        declaration.processAllDeclarations(session = declaration.moduleData.session) { symbol ->
            when (symbol) {
                is FirPropertySymbol -> {
                    val property = symbol.fir
                    sink[property.name.asString()] =
                        FirTypeRenderer.buildRendered(
                            property.returnTypeRef.coneType,
                            session,
                            preserve,
                        )
                }
                is FirFunctionSymbol<*> -> {
                    val function = symbol.fir
                    val fnName = symbol.name.asString()
                    sink[fnName] =
                        FirTypeRenderer.buildRendered(
                            function.returnTypeRef.coneType,
                            session,
                            preserve,
                        )
                    function.valueParameters.forEach { param ->
                        sink["$fnName.${param.name.asString()}"] =
                            FirTypeRenderer.buildRendered(
                                param.returnTypeRef.coneType,
                                session,
                                preserve,
                            )
                    }
                    function.receiverParameter?.typeRef?.coneType?.let { receiverType ->
                        sink["$fnName.<receiver>"] =
                            FirTypeRenderer.buildRendered(receiverType, session, preserve)
                    }
                }
                else -> Unit
            }
        }
    }
}
