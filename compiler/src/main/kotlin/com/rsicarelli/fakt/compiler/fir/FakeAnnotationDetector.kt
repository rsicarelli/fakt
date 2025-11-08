// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Detector for @Fake annotations in FIR declarations.
 *
 * ## FIR Phase Role
 *
 * This class operates in the FIR (Frontend Intermediate Representation) phase of Kotlin compilation,
 * where annotation detection and type resolution are most accurate. It provides the foundation
 * for FakeInterfaceChecker and FakeClassChecker to identify @Fake annotated declarations.
 *
 * ## Capabilities
 *
 * **Current (MVP)**:
 * - ✅ Detecting presence of @Fake annotations using stable FIR APIs
 * - ✅ Detecting presence of @FakeConfig annotations (reserved for future use)
 * - ✅ Providing default annotation parameters for basic functionality
 *
 * **Future (Post-MVP)**:
 * - 🚧 Extracting annotation parameter values (trackCalls, concurrent, etc.)
 * - 🚧 Validating annotation parameter types and ranges
 * - 🚧 Supporting custom annotation parameter combinations
 *
 * ## MVP Limitations
 *
 * **Why No Parameter Extraction**:
 * FIR annotation parameter extraction APIs are marked as unstable and vary significantly
 * between Kotlin compiler versions (1.9.x vs 2.0.x). To ensure stability and compatibility,
 * the MVP uses sensible defaults rather than attempting extraction.
 *
 * **Current Approach**:
 * - All @Fake declarations use default parameters:
 *   - `trackCalls = false` (no call tracking overhead)
 *   - `concurrent = true` (thread-safe by default)
 *   - `scope = "test"` (test source set only)
 *
 * **Migration Path**:
 * When FIR annotation APIs stabilize (expected: Kotlin 2.1+):
 * 1. Implement `toAnnotationClassId()` using stable FIR APIs
 * 2. Extract parameter values from `FirAnnotation.argumentMapping`
 * 3. Validate parameter types match annotation declaration
 * 4. Update default fallback behavior
 *
 * ## Thread Safety
 *
 * All methods are stateless and thread-safe. FIR phase may process files concurrently,
 * and this detector handles concurrent calls safely.
 *
 * @see com.rsicarelli.fakt.compiler.fir.FakeInterfaceChecker for usage in interface validation
 * @see com.rsicarelli.fakt.compiler.fir.FakeClassChecker for usage in class validation
 */
class FakeAnnotationDetector {
    companion object {
        private val FAKE_ANNOTATION_CLASS_ID =
            ClassId.topLevel(
                FqName("com.rsicarelli.fakt.Fake"),
            )

        private val FAKE_CONFIG_ANNOTATION_CLASS_ID =
            ClassId.topLevel(
                FqName("com.rsicarelli.fakt.FakeConfig"),
            )
    }

    /**
     * Check if a declaration has @Fake annotation.
     *
     * Uses stable FIR API `hasAnnotation()` for reliable detection across Kotlin versions.
     *
     * **Performance**: O(a) where a = number of annotations (typically < 5)
     * Typical cost: < 10μs per declaration
     *
     * @param declaration FIR class declaration to check (interface or class)
     * @param session FIR session for annotation resolution
     * @return true if declaration is annotated with @Fake, false otherwise
     */
    fun hasFakeAnnotation(
        declaration: FirRegularClass,
        session: FirSession,
    ): Boolean {
        return declaration.hasAnnotation(FAKE_ANNOTATION_CLASS_ID, session)
    }

    /**
     * Check if a declaration has @FakeConfig annotation (reserved for future use).
     *
     * **Future Enhancement**: @FakeConfig will provide global configuration for generated fakes
     * at the module or package level. Currently unused but API is prepared.
     *
     * @param declaration FIR class declaration to check
     * @param session FIR session for annotation resolution
     * @return true if declaration is annotated with @FakeConfig, false otherwise
     */
    fun hasFakeConfigAnnotation(
        declaration: FirRegularClass,
        session: FirSession,
    ): Boolean = declaration.hasAnnotation(FAKE_CONFIG_ANNOTATION_CLASS_ID, session)

    /**
     * Extract @Fake annotation parameters (MVP: returns defaults only).
     *
     * ## Current Behavior (MVP)
     *
     * This method currently returns default parameters for all @Fake declarations:
     * - `trackCalls = false` - No call tracking overhead
     * - `builder = false` - No builder pattern generation
     * - `dependencies = []` - No dependency injection
     * - `concurrent = true` - Thread-safe implementations
     * - `scope = "test"` - Generate in test source sets only
     *
     * ## Why Defaults Only
     *
     * FIR annotation parameter extraction APIs are unstable and vary between Kotlin versions.
     * Attempting extraction would:
     * - Break compatibility across Kotlin 1.9.x → 2.0.x → 2.1.x
     * - Require complex version-specific code paths
     * - Risk compilation failures when Kotlin compiler updates
     *
     * **Production Decision**: Use defaults for MVP stability, implement extraction post-1.0
     * when FIR APIs stabilize (expected: Kotlin 2.1+).
     *
     * ## Future Enhancement
     *
     * When implemented, this method will:
     * 1. Extract parameter values from `FirAnnotation.argumentMapping`
     * 2. Validate parameter types (Boolean, String, Array)
     * 3. Apply user-specified values or fall back to defaults
     * 4. Report errors for invalid parameter combinations
     *
     * **Example Future Usage**:
     * ```kotlin
     * @Fake(trackCalls = true, concurrent = false)
     * interface UserService
     * // Will extract: trackCalls=true, concurrent=false
     * ```
     *
     * **Performance**: O(1) - returns pre-allocated defaults
     *
     * @param declaration FIR class declaration with @Fake annotation
     * @param session FIR session for annotation resolution (reserved for future use)
     * @return FakeAnnotationParameters with default values (MVP) or extracted values (future)
     *
     * @see FakeAnnotationParameters for parameter descriptions
     */
    fun extractFakeParameters(
        declaration: FirRegularClass,
        session: FirSession,
    ): FakeAnnotationParameters {
        val annotation =
            declaration.annotations.find { annotation ->
                annotation.toAnnotationClassId(session) == FAKE_ANNOTATION_CLASS_ID
            }

        return if (annotation != null) {
            extractParametersFromAnnotation(annotation, session)
        } else {
            FakeAnnotationParameters() // Use defaults
        }
    }

    private fun extractParametersFromAnnotation(
        annotation: FirAnnotation,
        session: FirSession,
    ): FakeAnnotationParameters {
        // Extract parameter values from annotation arguments
        // For MVP, return sensible defaults - parameter extraction is complex and not critical for basic functionality
        // Real parameter extraction will be implemented when more stable FIR APIs are available

        return FakeAnnotationParameters(
            trackCalls = false, // Default: no call tracking
            builder = false, // Default: no builder pattern
            dependencies = emptyList(), // Default: no dependencies
            concurrent = true, // Default: thread-safe
            scope = "test", // Default: test scope
        )
    }

    /**
     * Extension function to extract ClassId from FirAnnotation.
     *
     * ## MVP Limitation
     *
     * **Current**: Returns null - parameter extraction not implemented due to unstable FIR APIs.
     *
     * **Why Null**:
     * FIR annotation type resolution APIs vary significantly between Kotlin compiler versions:
     * - Kotlin 1.9.x: Uses `annotationTypeRef.coneType.classId`
     * - Kotlin 2.0.x: Uses `annotationTypeRef.coneType.toSymbol()?.classId`
     * - Kotlin 2.1+: May use different approach entirely
     *
     * Attempting to use version-specific logic would:
     * - Require conditional compilation or runtime version checks
     * - Break when Kotlin compiler updates
     * - Complicate maintenance significantly
     *
     * **Fallback Strategy**:
     * The calling code handles null gracefully by using default parameters.
     * This ensures @Fake generation works reliably across all Kotlin versions.
     *
     * **Future Implementation**:
     * When FIR annotation APIs stabilize (Kotlin 2.1+):
     * ```kotlin
     * private fun FirAnnotation.toAnnotationClassId(session: FirSession): ClassId? {
     *     return annotationTypeRef.coneType.classId  // or stable equivalent
     * }
     * ```
     *
     * Then extract parameters from `argumentMapping`:
     * ```kotlin
     * val trackCalls = argumentMapping.mapping[Name.identifier("trackCalls")]?.extractBoolean()
     * ```
     *
     * @param session FIR session for type resolution (reserved for future use)
     * @return ClassId of the annotation, currently always null (MVP limitation)
     *
     * @see extractFakeParameters for context on MVP parameter handling
     */
    @Suppress("FunctionOnlyReturningConstant")
    private fun FirAnnotation.toAnnotationClassId(session: FirSession): ClassId? = null
}

/**
 * Data class representing @Fake annotation parameters.
 *
 * ## Parameter Descriptions
 *
 * These parameters control fake generation behavior. Currently all default to sensible MVP values.
 * Future versions will support user-specified values via @Fake annotation parameters.
 *
 * @property trackCalls Enable call tracking for verification (default: false)
 *   - `true`: Generate code to track method calls, parameters, and call counts
 *   - `false`: No tracking overhead (better performance)
 *   - **Future**: `@Fake(trackCalls = true) interface Foo`
 *
 * @property builder Generate builder pattern for fake configuration (default: false)
 *   - `true`: Generate `FakeFoo.Builder` for fluent configuration
 *   - `false`: Use simple configuration DSL only
 *   - **Future**: `@Fake(builder = true) interface Foo`
 *
 * @property dependencies Specify dependency injection for generated fakes (default: empty)
 *   - Allows injecting other fakes as dependencies
 *   - **Future**: `@Fake(dependencies = [UserRepository::class]) interface UserService`
 *
 * @property concurrent Generate thread-safe implementations (default: true)
 *   - `true`: Use thread-safe collections and synchronization
 *   - `false`: No thread safety overhead (use in single-threaded tests only)
 *   - **Future**: `@Fake(concurrent = false) interface Foo`
 *
 * @property scope Target source set for generated code (default: "test")
 *   - `"test"`: Generate in test source set only
 *   - `"main"`: Generate in main source set (production code)
 *   - `"commonTest"`: Generate in commonTest (KMP)
 *   - **Future**: `@Fake(scope = "commonTest") interface Foo`
 */
data class FakeAnnotationParameters(
    val trackCalls: Boolean = false,
    val builder: Boolean = false,
    val dependencies: List<ClassId> = emptyList(),
    val concurrent: Boolean = true,
    val scope: String = "test",
)
