// Copyright (C) 2026 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package spike

import com.rsicarelli.fakt.Fake
import platform.UIKit.UIView
import platform.UIKit.UIViewController

/** C2: shared iosMain over UIKit. */
@Fake
interface IosViews {
    fun root(): UIViewController?
    fun inflate(name: String): UIView
}
