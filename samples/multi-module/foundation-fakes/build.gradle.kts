// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
}

kotlin {
    jvmToolchain(21)
    applyDefaultHierarchyTemplate()

    // Targets (must match foundation module)
    jvm()
    // TODO: Re-enable JS when foundation module JS is fixed
    // js(IR) {
    //     browser()
    //     nodejs()
    // }

}

// Configure Fakt plugin in COLLECTOR MODE
fakt {
    enabled.set(true)
    debug.set(true)

    // This is the key: collect fakes from foundation module
    collectFakesFrom(project(":samples:multi-module:foundation"))
}
