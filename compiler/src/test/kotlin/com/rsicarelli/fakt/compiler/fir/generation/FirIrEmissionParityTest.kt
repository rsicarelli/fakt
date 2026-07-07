// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.generation

import com.rsicarelli.fakt.compiler.api.EmitPhase
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * THE PARITY GATE: every corpus fixture is compiled twice with the real Fakt plugin — once emitting
 * from the IR phase (legacy default) and once from the FIR phase (the metadata-driver path) — and
 * the generated `.kt` outputs are asserted **byte-identical**.
 *
 * A failure here means `FirTypeRenderer`/`FirToFakeDeclarationTranslator` drifted from the IrType
 * renderer stack. Maintenance rule: fix the FIR renderer and add a matrix row to
 * `FirTypeRendererTest` — never fork generated-output expectations per phase.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FirIrEmissionParityTest {

    @Test
    fun `GIVEN simple interface WHEN emitting from FIR and IR THEN outputs are byte-identical`() {
        assertParity(
            "simple",
            """
            package parity
            import com.rsicarelli.fakt.Fake

            @Fake
            interface UserService {
                val userCount: Int
                fun findUser(id: String): String?
                fun save(name: String, age: Int): Boolean
            }
            """,
        )
    }

    @Test
    fun `GIVEN nested generics and collections WHEN emitting from both phases THEN outputs are byte-identical`() {
        assertParity(
            "generics",
            """
            package parity
            import com.rsicarelli.fakt.Fake

            class User

            @Fake
            interface Repository {
                val cache: Map<String, List<User>>
                fun page(offset: Int): List<User>
                fun index(): Map<String, Set<Long>>
                fun result(): Result<String>
            }
            """,
        )
    }

    @Test
    fun `GIVEN class-level type parameters with bounds WHEN emitting from both phases THEN outputs are byte-identical`() {
        assertParity(
            "typeparams",
            """
            package parity
            import com.rsicarelli.fakt.Fake

            @Fake
            interface Store<K : Comparable<K>, V> {
                val size: Int
                fun get(key: K): V?
                fun put(key: K, value: V): Boolean
            }
            """,
        )
    }

    @Test
    fun `GIVEN suspend functions and default values WHEN emitting from both phases THEN outputs are byte-identical`() {
        assertParity(
            "suspend",
            """
            package parity
            import com.rsicarelli.fakt.Fake

            @Fake
            interface SessionApi {
                suspend fun refresh(token: String?): Boolean
                suspend fun load(id: Long, timeoutMs: Long = 30000L): String
                fun retryCount(attempts: Int = 3): Int
            }
            """,
        )
    }

    @Test
    fun `GIVEN function-typed members WHEN emitting from both phases THEN outputs are byte-identical`() {
        assertParity(
            "functiontypes",
            """
            package parity
            import com.rsicarelli.fakt.Fake

            @Fake
            interface CallbackRegistry {
                val onReady: () -> Unit
                val transform: (Int, String) -> Boolean
                val optionalHandler: ((Int) -> Unit)?
                val suspending: suspend (String) -> Int
                fun register(listener: (Long) -> Unit): Boolean
            }
            """,
        )
    }

    @Test
    fun `GIVEN star projections and variance WHEN emitting from both phases THEN outputs are byte-identical`() {
        assertParity(
            "projections",
            """
            package parity
            import com.rsicarelli.fakt.Fake

            class Box<T>

            @Fake
            interface ProjectionService {
                val anything: List<*>
                fun consume(box: Box<out Number>): Boolean
                fun produce(box: Box<in Int>): Int
            }
            """,
        )
    }

    @Test
    fun `GIVEN vararg parameters WHEN emitting from both phases THEN outputs are byte-identical`() {
        assertParity(
            "vararg",
            """
            package parity
            import com.rsicarelli.fakt.Fake

            @Fake
            interface Tagger {
                fun tag(vararg labels: String): Int
                fun weights(vararg values: Double): Double
            }
            """,
        )
    }

    @Test
    fun `GIVEN typealiases in signatures WHEN emitting from both phases THEN outputs are byte-identical`() {
        assertParity(
            "typealiases",
            """
            package parity
            import com.rsicarelli.fakt.Fake

            typealias Handler = (Int) -> String
            typealias Names = List<String>

            @Fake
            interface AliasService {
                val handler: Handler
                fun names(): Names
                fun register(h: Handler): Boolean
            }
            """,
        )
    }

    @Test
    fun `GIVEN extension receivers and operators WHEN emitting from both phases THEN outputs are byte-identical`() {
        assertParity(
            "extensions",
            """
            package parity
            import com.rsicarelli.fakt.Fake

            class Vector

            @Fake
            interface Calculator {
                operator fun plus(other: Int): Int
                fun Vector.magnitude(): Double
                fun String.shout(): String
            }
            """,
        )
    }

    @Test
    fun `GIVEN method-level generics WHEN emitting from both phases THEN outputs are byte-identical`() {
        assertParity(
            "methodgenerics",
            """
            package parity
            import com.rsicarelli.fakt.Fake

            @Fake
            interface Mapper {
                fun <T> identity(value: T): T
                fun <R : Comparable<R>> max(a: R, b: R): R
            }
            """,
        )
    }

    @Test
    fun `GIVEN inherited members WHEN emitting from both phases THEN outputs are byte-identical`() {
        assertParity(
            "inheritance",
            """
            package parity
            import com.rsicarelli.fakt.Fake

            interface Closeable2 {
                fun close(): Boolean
            }

            interface Named {
                val name: String
            }

            @Fake
            interface Resource : Closeable2, Named {
                fun open(): Boolean
            }
            """,
        )
    }

    @Test
    fun `GIVEN annotation propagation WHEN emitting from both phases THEN outputs are byte-identical`() {
        assertParity(
            "annotations",
            """
            package parity
            import com.rsicarelli.fakt.Fake

            @RequiresOptIn(message = "Experimental parity API")
            annotation class ExperimentalParity

            @Fake
            @ExperimentalParity
            interface FeatureService {
                fun enabled(): Boolean
            }
            """,
        )
    }

    @Test
    fun `GIVEN abstract class with constructor params WHEN emitting from both phases THEN outputs are byte-identical`() {
        assertParity(
            "abstractclass",
            """
            package parity
            import com.rsicarelli.fakt.Fake

            @Fake
            abstract class BaseRepository(val connection: String, val poolSize: Int = 10) {
                abstract fun query(sql: String): List<String>
                abstract val isConnected: Boolean
                open fun close(): Boolean = true
            }
            """,
        )
    }

    @Test
    fun `GIVEN java interop flexible types WHEN emitting from both phases THEN outputs are byte-identical`() {
        assertParity(
            "javainterop",
            """
            package parity
            import com.rsicarelli.fakt.Fake
            import java.time.Duration

            @Fake
            interface ClockService {
                val timeout: Duration
                fun elapsed(): Duration
                fun systemProperty(name: String): String?
            }
            """,
        )
    }

    @Test
    fun `GIVEN call history disabled WHEN emitting from both phases THEN outputs are byte-identical`() {
        assertParity(
            "nocallhistory",
            """
            package parity
            import com.rsicarelli.fakt.Fake

            @Fake
            interface QuietService {
                fun ping(): Boolean
            }
            """,
            enableCallHistory = false,
        )
    }

    // -------------------------------------------------------------------------
    // Harness
    // -------------------------------------------------------------------------

    private fun assertParity(
        fixtureName: String,
        source: String,
        enableCallHistory: Boolean = true,
    ) {
        val fixture = SourceFile.kotlin("Fixture.kt", source.trimIndent())

        val irDir = Files.createTempDirectory("parity-$fixtureName-ir").toFile()
        val firDir = Files.createTempDirectory("parity-$fixtureName-fir").toFile()

        try {
            val irOutcome =
                FullPluginCompilationHarness.compile(
                    fixtures = listOf(fixture),
                    emitPhase = EmitPhase.IR,
                    outputDir = irDir,
                    enableCallHistory = enableCallHistory,
                )
            val firOutcome =
                FullPluginCompilationHarness.compile(
                    fixtures = listOf(fixture),
                    emitPhase = EmitPhase.FIR,
                    outputDir = firDir,
                    enableCallHistory = enableCallHistory,
                )

            assertEquals(
                KotlinCompilation.ExitCode.OK,
                irOutcome.exitCode,
                "[$fixtureName] IR-phase compile failed:\n${irOutcome.messages}",
            )
            assertEquals(
                KotlinCompilation.ExitCode.OK,
                firOutcome.exitCode,
                "[$fixtureName] FIR-phase compile failed:\n${firOutcome.messages}",
            )

            assertTrue(
                irOutcome.generatedFiles.isNotEmpty(),
                "[$fixtureName] IR phase generated nothing — harness regression",
            )
            assertEquals(
                irOutcome.generatedFiles.keys,
                firOutcome.generatedFiles.keys,
                "[$fixtureName] phases generated different file sets",
            )

            irOutcome.generatedFiles.forEach { (path, irContent) ->
                assertEquals(
                    irContent,
                    firOutcome.generatedFiles.getValue(path),
                    "[$fixtureName] $path differs between IR and FIR emission",
                )
            }
        } finally {
            irDir.deleteRecursively()
            firDir.deleteRecursively()
        }
    }
}
