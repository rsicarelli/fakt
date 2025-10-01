// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package kmp.test.apple

import com.rsicarelli.fakt.Fake

/**
 * RULE TEST 4: appleMain → appleTest
 * These interfaces should generate fakes in appleTest source set
 * FALLBACK TEST: If appleTest doesn't exist, should fallback to nativeTest → commonTest
 */

@Fake
interface AppleFrameworkService {
    val bundleIdentifier: String
    val version: String

    fun initialize(): Boolean
    suspend fun loadFramework(name: String): Boolean
    fun <T> callFrameworkMethod(framework: String, method: String, args: Array<Any>): T?
}

@Fake
interface AppleUIService<TView> {
    val screenSize: Pair<Double, Double>
    val orientation: String

    fun createView(): TView
    fun updateView(view: TView, properties: Map<String, Any>)
    suspend fun animateView(view: TView, duration: Double): Boolean
    fun <R> withMainThread(action: () -> R): R
}

@Fake
interface AppleStorageService {
    val documentsPath: String
    val cachesPath: String

    fun save(key: String, data: ByteArray): Boolean
    fun load(key: String): ByteArray?
    suspend fun sync(): Boolean
    fun <T> withTransaction(action: () -> T): T
}

// Apple-specific data classes
data class AppleFrameworkInfo(
    val name: String,
    val version: String,
    val path: String,
    val dependencies: List<String>
)

data class AppleViewConfiguration(
    val frame: Map<String, Double>,
    val backgroundColor: String,
    val hidden: Boolean,
    val userInteractionEnabled: Boolean
)
