// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import com.vanniktech.maven.publish.DeploymentValidation
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
    // Check if already configured
    val alreadyConfigured = extensions.findByName("faktPublishingConfigured")
    if (alreadyConfigured != null) {
        logger.info("Fakt: Publishing already configured for ${project.name}")
        return
    }

    // Mark as configured
    extensions.add("faktPublishingConfigured", true)

    // Apply vanniktech plugin and configure with coordination (using withPlugin)
    pluginManager.apply("com.vanniktech.maven.publish")

    // Configure after plugin is applied and available
    pluginManager.withPlugin("com.vanniktech.maven.publish") {
        configureMavenCentralPublishing()
    }

    logger.info("Fakt: Applied publishing convention")
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
    val isSnapshot = project.version.toString().endsWith("-SNAPSHOT")
    val skipSigning = findProperty("SKIP_SIGNING")?.toString()?.toBoolean() ?: false
    val isCI = System.getenv("IS_CI")?.toBoolean() ?: false

    // Configure using the mavenPublishing DSL extension
    extensions
        .findByType(com.vanniktech.maven.publish.MavenPublishBaseExtension::class.java)
        ?.apply {
            // Pin PUBLISHED explicitly: since the plugin's 0.36 release the default is
            // VALIDATED, which returns once the Portal validates the deployment rather than
            // once it is actually published. The release workflow tags the repo and cuts the
            // GitHub Release right after this task, so it must wait for the real thing.
            publishToMavenCentral(
                automaticRelease = isReleaseMode,
                validateDeployment = DeploymentValidation.PUBLISHED,
            )

            // Signing logic:
            // - If IS_CI is absent (false) → skip signing (local development)
            // - If SKIP_SIGNING flag is set → skip signing
            // - If IS_CI is true AND not skipped AND not snapshot → sign
            // This avoids requiring GPG credentials for local publishToMavenLocal
            if (isCI && !skipSigning && !isSnapshot) {
                signAllPublications()
            }

            coordinates(
                groupId = project.group.toString(),
                artifactId = project.name,
                version = project.version.toString(),
            )

            pom {
                name.set(project.description ?: project.name)
                description.set(project.description ?: "Fakt module ${project.name}")
                url.set(findProperty("POM_URL") as String? ?: "https://github.com/rsicarelli/fakt")

                licenses {
                    license {
                        name.set(
                            findProperty("POM_LICENCE_NAME") as String? ?: "Apache License 2.0",
                        )
                        url.set(
                            findProperty("POM_LICENCE_URL") as String?
                                ?: "https://www.apache.org/licenses/LICENSE-2.0",
                        )
                        distribution.set(findProperty("POM_LICENCE_DIST") as String? ?: "repo")
                    }
                }

                developers {
                    developer {
                        id.set(findProperty("POM_DEVELOPER_ID") as String? ?: "rsicarelli")
                        name.set(
                            findProperty("POM_DEVELOPER_NAME") as String? ?: "Rodrigo Sicarelli",
                        )
                        email.set("rodrigo.sicarelli@gmail.com")
                        url.set(
                            findProperty("POM_DEVELOPER_URL") as String?
                                ?: "https://github.com/rsicarelli",
                        )
                    }
                }

                scm {
                    connection.set(
                        findProperty("POM_SCM_CONNECTION") as String?
                            ?: "scm:git:git://github.com/rsicarelli/fakt.git",
                    )
                    developerConnection.set(
                        findProperty("POM_SCM_DEV_CONNECTION") as String?
                            ?: "scm:git:ssh://git@github.com/rsicarelli/fakt.git",
                    )
                    url.set(
                        findProperty("POM_SCM_URL") as String?
                            ?: "https://github.com/rsicarelli/fakt",
                    )
                }
            }
        }

    logger.info("Fakt: Configured Maven Central publishing - releaseMode=$isReleaseMode")
}
