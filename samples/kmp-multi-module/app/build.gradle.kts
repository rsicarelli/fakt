// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("fakt-sample")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.fakt.runtime)

                // All feature dependencies
                implementation(projects.features.login)
                implementation(projects.features.order)
                implementation(projects.features.profile)
                implementation(projects.features.dashboard)
                implementation(projects.features.notifications)
                implementation(projects.features.settings)

                // Core dependencies (transitive, but explicit for clarity)
                implementation(projects.core.auth)
                implementation(projects.core.logger)
                implementation(projects.core.storage)
                implementation(projects.core.network)
                implementation(projects.core.analytics)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.test)

                // Use fakes from all modules for integration testing

                // Core fakes
                implementation(projects.core.authFakes)
                implementation(projects.core.loggerFakes)
                implementation(projects.core.storageFakes)
                implementation(projects.core.networkFakes)
                implementation(projects.core.analyticsFakes)

                // Feature fakes
                implementation(projects.features.loginFakes)
                implementation(projects.features.orderFakes)
                implementation(projects.features.profileFakes)
                implementation(projects.features.dashboardFakes)
                implementation(projects.features.notificationsFakes)
                implementation(projects.features.settingsFakes)
            }
        }
    }
}
