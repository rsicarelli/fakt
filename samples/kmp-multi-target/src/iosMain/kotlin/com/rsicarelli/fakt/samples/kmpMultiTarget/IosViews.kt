// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import com.rsicarelli.fakt.Fake
import platform.UIKit.UIView
import platform.UIKit.UIViewController

/** UIKit types in iosMain. */
@Fake
interface IosViews {
    fun createView(): UIView

    fun controller(): UIViewController?
}
