// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
@file:Mockable(
    UserRepository::class,
    AnalyticsService::class,
    ConfigProvider::class,
    ComplexApiService::class,
)

package benchmark

import benchmark.domain.AnalyticsService
import benchmark.domain.ComplexApiService
import benchmark.domain.ConfigProvider
import benchmark.domain.UserRepository
import io.mockative.Mockable

// The domain interfaces live in the :contracts module, so Mockative's KSP processor needs them
// registered as @Mockable to generate mock implementations for them in this module.
