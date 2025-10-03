// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package kmp.test.ios

import com.rsicarelli.fakt.Fake

/**
 * RULE TEST 5: iosX64Main → iosX64Test
 * These interfaces should generate fakes in iosX64Test source set
 * FALLBACK TEST: iosX64Test → appleTest → nativeTest → commonTest
 */

@Fake
interface IosDeviceService {
    val deviceModel: String
    val systemVersion: String
    val isSimulator: Boolean

    fun getBatteryLevel(): Float

    suspend fun requestPermissions(permissions: List<String>): Map<String, Boolean>

    fun <T> onMainQueue(action: () -> T): T
}

@Fake
interface IosLocationService<TCoordinate> {
    val isLocationEnabled: Boolean
    val accuracy: Double

    suspend fun getCurrentLocation(): TCoordinate?

    suspend fun startTracking(accuracy: Double): Boolean

    fun stopTracking()

    fun <R> withLocationPermission(action: () -> R): R?
}

@Fake
interface IosNotificationService {
    val isAuthorized: Boolean
    val badgeCount: Int

    suspend fun requestAuthorization(): Boolean

    suspend fun scheduleNotification(
        title: String,
        body: String,
        delay: Long,
    ): String

    fun cancelNotification(id: String)

    fun <T> handleNotificationResponse(
        response: Any,
        handler: (Any) -> T,
    ): T
}

// iOS-specific data classes
data class IosDeviceInfo(
    val model: String,
    val systemName: String,
    val systemVersion: String,
    val identifierForVendor: String?,
)

data class IosLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val accuracy: Double,
    val timestamp: Long,
)

data class IosNotification(
    val identifier: String,
    val title: String,
    val body: String,
    val badge: Int?,
    val sound: String?,
    val userInfo: Map<String, Any>,
)
