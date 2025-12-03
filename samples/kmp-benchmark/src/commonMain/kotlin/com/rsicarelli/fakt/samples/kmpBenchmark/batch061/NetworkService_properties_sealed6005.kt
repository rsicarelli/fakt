// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch061

import com.rsicarelli.fakt.Fake

@Fake
interface NetworkService_properties_sealed6005 {
    
    val currentState: NetworkService_properties_sealed6005_1<Unit>

    
    val lastResult: NetworkService_properties_sealed6005_1<String>?

    
    suspend fun fetchData(url: String): NetworkService_properties_sealed6005_1<String>

    
    fun handleResult(result: NetworkService_properties_sealed6005_1<String>): String

    
    fun <T, R> mapResult(
        result: NetworkService_properties_sealed6005_1<T>,
        transform: (T) -> R,
    ): NetworkService_properties_sealed6005_1<R>

    
    fun isSuccess(result: NetworkService_properties_sealed6005_1<*>): Boolean
}

sealed class NetworkService_properties_sealed6005_1<out T> {
    data class NetworkService_properties_sealed6005_2<T>(
        val data: T,
    ) : NetworkService_properties_sealed6005_1<T>()

    data class NetworkService_properties_sealed6005_3(
        val message: String,
        val code: Int,
    ) : NetworkService_properties_sealed6005_1<Nothing>()

    data object NetworkService_properties_sealed6005_4 : NetworkService_properties_sealed6005_1<Nothing>()

    data object NetworkService_properties_sealed6005_5 : NetworkService_properties_sealed6005_1<Nothing>()
}
