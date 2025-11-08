// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertContains
import kotlin.test.assertTrue

/**
 * Tests for FIR error reporting constants and scenarios.
 *
 * **Testing Strategy:**
 * These tests validate that error constants exist and contain appropriate messages.
 * Actual error reporting behavior is validated through integration tests (E2E sample builds)
 * where we can verify error messages appear during compilation.
 *
 * **Phase 3B.4:** Error reporting is active in FakeInterfaceChecker and FakeClassChecker.
 * **Phase 3B.5:** Integration tests validate errors appear correctly.
 *
 * **Error Scenarios Covered:**
 *
 * FakeInterfaceChecker:
 * 1. Non-interface (class/object annotated with @Fake)
 * 2. Sealed interface
 * 3. Local interface
 *
 * FakeClassChecker:
 * 4. Non-class (object/enum annotated with @Fake)
 * 5. Non-abstract class
 * 6. Sealed class
 * 7. Local class
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FirErrorReportingTest {
    @Test
    fun `GIVEN error constants WHEN checking messages THEN all contain FAKT prefix`() {
        // GIVEN: All FIR error constants

        // WHEN: Checking message format

        // THEN: All messages have [FAKT] prefix for clear identification
        assertTrue(FirFaktErrors.FAKE_MUST_BE_INTERFACE.startsWith("[FAKT]"))
        assertTrue(FirFaktErrors.FAKE_CANNOT_BE_SEALED.startsWith("[FAKT]"))
        assertTrue(FirFaktErrors.FAKE_CANNOT_BE_LOCAL.startsWith("[FAKT]"))
        assertTrue(FirFaktErrors.FAKE_CLASS_MUST_BE_ABSTRACT.startsWith("[FAKT]"))
        assertTrue(FirFaktErrors.FAKE_CLASS_CANNOT_BE_SEALED.startsWith("[FAKT]"))
    }

    @Test
    fun `GIVEN FAKE_MUST_BE_INTERFACE error WHEN checking message THEN explains interface requirement`() {
        // GIVEN: Error for non-interface with @Fake

        // WHEN: Checking message content
        val message = FirFaktErrors.FAKE_MUST_BE_INTERFACE

        // THEN: Message clearly states interface requirement
        assertContains(message, "interface", ignoreCase = true)
        assertContains(message, "only", ignoreCase = true)
    }

    @Test
    fun `GIVEN FAKE_CANNOT_BE_SEALED error WHEN checking message THEN explains sealed restriction`() {
        // GIVEN: Error for sealed interface/class with @Fake

        // WHEN: Checking message content
        val message = FirFaktErrors.FAKE_CANNOT_BE_SEALED

        // THEN: Message clearly states sealed restriction
        assertContains(message, "sealed", ignoreCase = true)
        assertContains(message, "cannot", ignoreCase = true)
    }

    @Test
    fun `GIVEN FAKE_CANNOT_BE_LOCAL error WHEN checking message THEN explains local restriction`() {
        // GIVEN: Error for local class/interface with @Fake

        // WHEN: Checking message content
        val message = FirFaktErrors.FAKE_CANNOT_BE_LOCAL

        // THEN: Message clearly states local restriction
        assertContains(message, "local", ignoreCase = true)
        assertContains(message, "cannot", ignoreCase = true)
    }

    @Test
    fun `GIVEN FAKE_CLASS_MUST_BE_ABSTRACT error WHEN checking message THEN explains abstract requirement`() {
        // GIVEN: Error for non-abstract class with @Fake

        // WHEN: Checking message content
        val message = FirFaktErrors.FAKE_CLASS_MUST_BE_ABSTRACT

        // THEN: Message clearly states abstract requirement
        assertContains(message, "abstract", ignoreCase = true)
        assertContains(message, "must", ignoreCase = true)
    }

    @Test
    fun `GIVEN FAKE_CLASS_CANNOT_BE_SEALED error WHEN checking message THEN explains class sealed restriction`() {
        // GIVEN: Error for sealed class with @Fake

        // WHEN: Checking message content
        val message = FirFaktErrors.FAKE_CLASS_CANNOT_BE_SEALED

        // THEN: Message clearly states sealed class restriction
        assertContains(message, "class", ignoreCase = true)
        assertContains(message, "sealed", ignoreCase = true)
        assertContains(message, "cannot", ignoreCase = true)
    }

    /**
     * Integration Test Documentation:
     *
     * The following error scenarios are validated through E2E tests in Phase 3B.5:
     *
     * 1. **Non-interface with @Fake**:
     *    ```kotlin
     *    @Fake
     *    class NotAnInterface  // ERROR: [FAKT] @Fake can only be applied to interfaces
     *    ```
     *
     * 2. **Sealed interface with @Fake**:
     *    ```kotlin
     *    @Fake
     *    sealed interface SealedRepo  // ERROR: [FAKT] @Fake cannot be applied to sealed interfaces
     *    ```
     *
     * 3. **Local interface with @Fake**:
     *    ```kotlin
     *    fun test() {
     *        @Fake
     *        interface LocalRepo  // ERROR: [FAKT] @Fake cannot be applied to local classes or interfaces
     *    }
     *    ```
     *
     * 4. **Non-abstract class with @Fake**:
     *    ```kotlin
     *    @Fake
     *    class ConcreteClass  // ERROR: [FAKT] @Fake class must be abstract
     *    ```
     *
     * 5. **Sealed class with @Fake**:
     *    ```kotlin
     *    @Fake
     *    sealed class SealedBase  // ERROR: [FAKT] @Fake class cannot be sealed
     *    ```
     *
     * Verification:
     * - Run sample build with FIR mode enabled
     * - Check stderr for error messages
     * - Verify invalid @Fake usages are rejected
     * - Verify no fake code is generated for invalid declarations
     */
}
