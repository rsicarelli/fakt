// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import org.gradle.api.Project

/**
 * Publishing convention for Fakt modules.
 *
 * Applies project coordinates (group, version) from gradle.properties and
 * configures vanniktech plugin with centralized Maven Central publishing.
 *
 * **gradle.properties values:**
 * - GROUP=com.rsicarelli.fakt
 * - VERSION_NAME=1.0.0-SNAPSHOT
 *
 * **Environment Variables:**
 * - RELEASE_MODE=true: automatic release to Maven Central (release/hotfix workflows)
 * - RELEASE_MODE=false: SNAPSHOT publishing only (continuous-deploy workflow)
 *
 * **Maven Central Requirements:**
 * - Complete POM metadata (description, URL, licenses, developers)
 * - GPG signing for all artifacts
 * - Sources and Javadoc JARs
 *
 * See: https://vanniktech.github.io/gradle-maven-publish-plugin/central/
 */
fun Project.applyPublishingConvention() {
    // Read from gradle.properties (inherited from root project)
    val groupId = findProperty("GROUP") as String? ?: "com.rsicarelli.fakt"
    val versionName = findProperty("VERSION_NAME") as String? ?: "1.0.0-SNAPSHOT"

    // Apply to project
    group = groupId
    version = versionName

    // Apply and configure vanniktech plugin
    pluginManager.apply("com.vanniktech.maven.publish")

    // Use afterEvaluate to ensure plugin is fully configured
    afterEvaluate {
        configureMavenCentralPublishing()
    }

    logger.info("Fakt: Applied publishing convention - group=$group, version=$version")
}

/**
 * Configures vanniktech maven publishing plugin with centralized Maven Central setup.
 *
 * Uses RELEASE_MODE environment variable to control automatic release behavior:
 * - RELEASE_MODE=true: automaticRelease=true (for release/hotfix workflows)
 * - RELEASE_MODE=false: automaticRelease=false (for SNAPSHOT continuous deploy)
 */
private fun Project.configureMavenCentralPublishing() {
    val isReleaseMode = findProperty("RELEASE_MODE")?.toString()?.toBoolean() ?: false

    // Configure using the mavenPublishing DSL extension
    extensions.findByType(com.vanniktech.maven.publish.MavenPublishBaseExtension::class.java)
        ?.apply {
            try {
                publishToMavenCentral(automaticRelease = isReleaseMode)
                signAllPublications()
            } catch (e: IllegalStateException) {
                // Plugin already configured, skip
                logger.info("Fakt: Maven Central publishing already configured")
                return
            }

            coordinates(
                groupId = project.group.toString(),
                artifactId = project.name,
                version = project.version.toString()
            )

            pom {
                name.set(project.description ?: project.name)
                description.set(project.description ?: "Fakt module ${project.name}")
                url.set(findProperty("POM_URL") as String? ?: "https://github.com/rsicarelli/fakt")

                licenses {
                    license {
                        name.set(
                            findProperty("POM_LICENCE_NAME") as String? ?: "Apache License 2.0"
                        )
                        url.set(
                            findProperty("POM_LICENCE_URL") as String?
                                ?: "https://www.apache.org/licenses/LICENSE-2.0"
                        )
                        distribution.set(findProperty("POM_LICENCE_DIST") as String? ?: "repo")
                    }
                }

                developers {
                    developer {
                        id.set(findProperty("POM_DEVELOPER_ID") as String? ?: "rsicarelli")
                        name.set(
                            findProperty("POM_DEVELOPER_NAME") as String? ?: "Rodrigo Sicarelli"
                        )
                        email.set("rodrigo.sicarelli@gmail.com")
                        url.set(
                            findProperty("POM_DEVELOPER_URL") as String?
                                ?: "https://github.com/rsicarelli"
                        )
                    }
                }

                scm {
                    connection.set(
                        findProperty("POM_SCM_CONNECTION") as String?
                            ?: "scm:git:git://github.com/rsicarelli/fakt.git"
                    )
                    developerConnection.set(
                        findProperty("POM_SCM_DEV_CONNECTION") as String?
                            ?: "scm:git:ssh://git@github.com/rsicarelli/fakt.git"
                    )
                    url.set(
                        findProperty("POM_SCM_URL") as String?
                            ?: "https://github.com/rsicarelli/fakt"
                    )
                }
            }
        }

    logger.info("Fakt: Configured Maven Central publishing - releaseMode=$isReleaseMode")
}
