// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.visibility

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class VisibilityPropagationTest {

    @Test
    fun `GIVEN public interface WHEN generating fake THEN fake is accessible as public`() {
        // GIVEN a public interface PublicService
        // WHEN generating fake implementation
        val fake = fakePublicService {}

        // THEN the fake is publicly accessible and works correctly
        assertNotNull(fake)
        assertEquals("", fake.doWork())
        assertEquals(false, fake.status)
    }

    @Test
    fun `GIVEN public interface WHEN configuring fake THEN configuration works`() {
        // GIVEN a public interface PublicService
        // WHEN configuring the fake
        val fake = fakePublicService {
            doWork { "configured" }
            status { true }
        }

        // THEN the configured behavior is applied
        assertEquals("configured", fake.doWork())
        assertEquals(true, fake.status)
    }

    @Test
    fun `GIVEN internal interface WHEN generating fake THEN fake is accessible as internal`() {
        // GIVEN an internal interface InternalService
        // WHEN generating fake implementation
        val fake = fakeInternalService {}

        // THEN the fake is internally accessible and works correctly
        assertNotNull(fake)
        assertEquals("", fake.doWork())
        assertEquals(false, fake.status)
    }

    @Test
    fun `GIVEN internal interface WHEN configuring fake THEN configuration works`() {
        // GIVEN an internal interface InternalService
        // WHEN configuring the fake
        val fake = fakeInternalService {
            doWork { "internal-configured" }
            status { true }
        }

        // THEN the configured behavior is applied
        assertEquals("internal-configured", fake.doWork())
        assertEquals(true, fake.status)
    }

    @Test
    fun `GIVEN public abstract class WHEN generating fake THEN fake is public`() {
        // GIVEN a public abstract class PublicAbstractClass
        // WHEN generating fake implementation and configuring
        val fake = fakePublicAbstractClass {
            compute { 42 }
        }

        // THEN the fake is publicly accessible and works correctly
        assertNotNull(fake)
        assertEquals(42, fake.compute())
    }

    @Test
    fun `GIVEN internal open class WHEN generating fake THEN fake is internal`() {
        // GIVEN an internal open class InternalOpenClass
        // WHEN generating fake implementation
        val fake = fakeInternalOpenClass {}

        // THEN the fake is internally accessible and works correctly
        assertNotNull(fake)
        // Open methods delegate to super by default
        assertEquals("", fake.process())
    }

    @Test
    fun `GIVEN internal open class WHEN configuring fake THEN configuration works`() {
        // GIVEN an internal open class InternalOpenClass
        // WHEN configuring the fake
        val fake = fakeInternalOpenClass {
            process { "overridden" }
        }

        // THEN the configured behavior overrides super
        assertEquals("overridden", fake.process())
    }
}
