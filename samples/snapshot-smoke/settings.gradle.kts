// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// Self-contained proof that the published SNAPSHOT resolves from Maven Central Snapshots.
// No mavenLocal, no composite build — exactly what a downstream consumer would write.
rootProject.name = "snapshot-smoke"

pluginManagement {
    repositories {
        // 1. Snapshots repo MUST be here so the `plugins {}` block can find the plugin marker.
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            mavenContent { snapshotsOnly() }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        // 2. And here so the `com.rsicarelli.fakt:annotations` snapshot dependency resolves.
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
            mavenContent { snapshotsOnly() }
        }
        mavenCentral()
    }
}
