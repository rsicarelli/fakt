// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.shadow) apply false
}

java {
  targetCompatibility = JavaVersion.VERSION_21
  sourceCompatibility = JavaVersion.VERSION_21
}

dependencies {
  implementation(libs.kotlin.compilerEmbeddable)
  implementation(libs.autoService)

  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlin.testJunit5)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.coroutines.test)
}

tasks.jar.configure { enabled = false }

val shadowJar = tasks.register("shadowJar", ShadowJar::class.java) {
  from(sourceSets.main.map { it.output })
  configurations.add(project.configurations["compileClasspath"])

  dependencies {
    exclude(dependency("org.jetbrains:.*"))
    exclude(dependency("org.intellij:.*"))
    exclude(dependency("org.jetbrains.kotlin:.*"))
  }

  relocate("com.google.auto.service", "dev.rsicarelli.ktfake.shaded.autoservice")
}

tasks {
  // Configure test task
  test {
    // Compiler tests need more memory
    jvmArgs("-Xmx2g", "-XX:MaxMetaspaceSize=512m")

    // Extended timeout for compiler operations
    systemProperty("junit.jupiter.execution.timeout.default", "60s")

    // Limited parallelism for resource-intensive compiler tests
    systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
    systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "2")
  }
}

for (c in arrayOf("apiElements", "runtimeElements")) {
  configurations.named(c) { artifacts.removeIf { true } }
  artifacts.add(c, shadowJar)
}
