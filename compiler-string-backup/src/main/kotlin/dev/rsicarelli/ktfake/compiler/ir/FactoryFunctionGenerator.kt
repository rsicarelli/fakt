// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

/**
 * Generator for factory functions in IR phase.
 *
 * Creates factory functions that follow the thread-safe pattern:
 * ```kotlin
 * @Generated("ktfake")
 * fun fakeUserService(configure: FakeUserServiceConfig.() -> Unit = {}): UserService {
 *     return FakeUserServiceImpl().apply {
 *         FakeUserServiceConfig(this).configure()
 *     }
 * }
 * ```
 *
 * Factory functions are the primary public API for creating fake instances.
 * They ensure thread safety by creating new instances rather than sharing state.
 */
class FactoryFunctionGenerator {

    /**
     * Generate a factory function name from an interface name.
     *
     * @param interfaceName The original interface name (e.g., "UserService")
     * @return The factory function name (e.g., "fakeUserService")
     */
    fun generateFactoryName(interfaceName: String): String {
        return "fake${interfaceName}"
    }

    /**
     * Generate a factory function for a given interface.
     *
     * TODO: Implement actual IR generation when IR API integration is ready
     */
    fun generateFactory(interfaceName: String): String {
        val factoryName = generateFactoryName(interfaceName)
        val implName = "${interfaceName}Impl"
        val configName = "Fake${interfaceName}Config"

        return """
fun $factoryName(configure: $configName.() -> Unit = {}): $interfaceName {
    return Fake$implName().apply {
        $configName(this).configure()
    }
}
        """.trimIndent()
    }

    /**
     * Handle custom factory names from @FakeConfig annotation.
     */
    fun generateCustomFactory(customName: String, interfaceName: String): String {
        val implName = "${interfaceName}Impl"
        val configName = "Fake${interfaceName}Config"

        return """
        @Generated("ktfake")
        fun $customName(configure: $configName.() -> Unit = {}): $interfaceName {
            return Fake$implName().apply {
                $configName(this).configure()
            }
        }
        """.trimIndent()
    }
}
