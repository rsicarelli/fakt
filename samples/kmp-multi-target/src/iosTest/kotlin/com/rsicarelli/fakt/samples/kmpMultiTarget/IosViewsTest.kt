// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import platform.UIKit.UIView

class IosViewsTest {
    @Test
    fun `GIVEN IosViews fake WHEN creating a view THEN should return the configured UIView`() {
        // Given
        val view = UIView()
        val fake = fakeIosViews { createView { view } }

        // When
        val result = fake.createView()

        // Then
        assertEquals(view, result)
        assertEquals(1, fake.createViewCalls.value.size)
    }

    @Test
    fun `GIVEN IosViews fake WHEN asking for a controller THEN should return null by default`() {
        // Given
        val fake = fakeIosViews()

        // When
        val controller = fake.controller()

        // Then
        assertNull(controller)
        assertEquals(1, fake.controllerCalls.value.size)
    }
}
