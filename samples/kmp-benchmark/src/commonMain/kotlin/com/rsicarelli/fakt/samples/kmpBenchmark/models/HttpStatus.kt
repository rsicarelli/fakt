// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.models

enum class HttpStatus(
    val code: Int,
    val message: String,
    val isSuccess: Boolean,
) {
    OK(200, "OK", true),
    CREATED(201, "Created", true),
    BAD_REQUEST(400, "Bad Request", false),
    UNAUTHORIZED(401, "Unauthorized", false),
    NOT_FOUND(404, "Not Found", false),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error", false),
    ;

    fun isClientError(): Boolean = code in 400..499

    fun isServerError(): Boolean = code in 500..599

    fun format(): String = "$code $message"
}
