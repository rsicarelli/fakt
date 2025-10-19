package com.rsicarelli.fakt.samples.singlemodule.models

import com.rsicarelli.fakt.GeneratesFake

/**
 * Example of a custom annotation for fake generation.
 *
 * By annotating this with @GeneratesFake, the Fakt compiler will automatically
 * detect interfaces/classes annotated with @TestDouble and generate fakes for them.
 *
 * ## Benefits
 * - **Ownership**: Your annotation, your naming convention
 * - **Migration Safety**: Breaking changes in Fakt don't affect your annotation
 * - **Zero Configuration**: No Gradle setup required, meta-annotation does everything
 *
 * ## Usage
 * ```kotlin
 * @TestDouble
 * interface PaymentService {
 *     fun processPayment(amount: Double): Boolean
 * }
 *
 * // Generated automatically:
 * // - FakePaymentServiceImpl
 * // - fakePaymentService() factory
 * // - FakePaymentServiceConfig DSL
 * ```
 */
@GeneratesFake
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class CustomAnnotation