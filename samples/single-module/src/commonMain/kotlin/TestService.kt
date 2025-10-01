// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import com.rsicarelli.fakt.Fake

// ============================================================================
// BASIC INTERFACES - Simple property and method faking
// ============================================================================

@Fake
interface TestService {
    val stringValue: String
    fun getValue(): String
    fun setValue(value: String)
}

@Fake(trackCalls = true)
interface AnalyticsService {
    fun track(event: String)
}

// ============================================================================
// SUSPEND FUNCTIONS - Async/coroutine support
// ============================================================================

@Fake
interface AsyncUserService {
    suspend fun getUser(id: String): String
    suspend fun updateUser(id: String, name: String): Boolean
    suspend fun deleteUser(id: String)
}

@Fake
interface AsyncDataService {
    suspend fun fetchData(): String
    suspend fun <T> processData(data: T): T
    suspend fun batchProcess(items: List<String>): List<String>
}

// ============================================================================
// DATA CLASSES & VALUE OBJECTS
// ============================================================================

data class User(
    val id: String,
    val name: String,
    val email: String,
    val age: Int = 18
)

data class Product(
    val id: Long,
    val name: String,
    val price: Double,
    val category: String
)

@Fake
interface UserRepository {
    val users: List<User>
    fun findById(id: String): User?
    fun save(user: User): User
    fun delete(id: String): Boolean
    fun findByAge(minAge: Int, maxAge: Int = 100): List<User>
}

@Fake
interface ProductService {
    suspend fun getProduct(id: Long): Product?
    suspend fun searchProducts(query: String, limit: Int = 10): List<Product>
    suspend fun updatePrice(id: Long, newPrice: Double): Product
}

// ============================================================================
// HIGHER-ORDER FUNCTIONS - Function types and lambdas
// ============================================================================

// Generic interface - NOT supported by KtFakes (will be skipped)
interface GenericEventProcessor<T> {
    fun process(item: T, processor: (T) -> String): String
    fun <R> transform(items: List<T>, transformer: (T) -> R): List<R>
}

// Type-safe alternative - SUPPORTED by KtFakes
@Fake
interface EventProcessor {
    fun processString(item: String, processor: (String) -> String): String
    fun processInt(item: Int, processor: (Int) -> String): String
    fun filter(items: List<String>, predicate: (String) -> Boolean): List<String>
    fun onComplete(callback: () -> Unit)
    fun onError(errorHandler: (Exception) -> Unit)
    suspend fun processAsync(item: String, processor: suspend (String) -> String): String
}


@Fake
interface WorkflowManager {
    fun <T> executeStep(step: () -> T): T
    fun <T> executeStepWithFallback(step: () -> T, fallback: () -> T): T
    suspend fun <T> executeAsyncStep(step: suspend () -> T): T
    fun chainSteps(steps: List<() -> Unit>)
}

// ============================================================================
// GENERIC TYPES - Collections, Result types, and custom generics
// ============================================================================

@Fake
interface GenericRepository<T> {
    val items: List<T>
    fun findAll(): List<T>
    fun findById(id: String): T?
    fun save(item: T): T
    fun saveAll(items: List<T>): List<T>
    fun <R> map(transformer: (T) -> R): List<R>
}

@Fake
interface ResultService {
    fun <T> tryOperation(operation: () -> T): Result<T>
    fun <T, R> mapResult(result: Result<T>, mapper: (T) -> R): Result<R>
    suspend fun <T> tryAsyncOperation(operation: suspend () -> T): Result<T>
    fun combineResults(results: List<Result<String>>): Result<List<String>>
}

@Fake
interface CollectionService {
    fun processStrings(items: List<String>): Set<String>
    fun processNumbers(items: Set<Int>): Map<Int, String>
    fun <K, V> transformMap(map: Map<K, V>, transformer: (K, V) -> String): Map<K, String>
    fun nestedCollections(data: Map<String, List<Set<Int>>>): List<Map<String, Int>>
}

// ============================================================================
// COMPLEX SCENARIOS - Multi-parameter, nullable, default values
// ============================================================================

@Fake
interface ComplexApiService {
    val baseUrl: String
    val timeout: Long
    val retryCount: Int?

    fun makeRequest(
        endpoint: String,
        method: String = "GET",
        headers: Map<String, String>? = null,
        body: String? = null,
        timeout: Long = 30000L
    ): String

    suspend fun makeBatchRequests(
        requests: List<Pair<String, Map<String, String>>>,
        parallel: Boolean = true,
        onProgress: ((Int, Int) -> Unit)? = null
    ): List<Result<String>>

    fun <T> parseResponse(
        response: String,
        parser: (String) -> T?,
        fallback: T? = null
    ): T?

    suspend fun <TRequest, TResponse> processWithRetry(
        request: TRequest,
        processor: suspend (TRequest) -> TResponse,
        retryCount: Int = 3,
        onRetry: ((Int, Exception) -> Unit)? = null
    ): Result<TResponse>
}

@Fake
interface AuthenticationService {
    val isLoggedIn: Boolean
    val currentUser: User?
    val permissions: Set<String>

    suspend fun login(username: String, password: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun refreshToken(): Result<String>
    fun hasPermission(permission: String): Boolean
    fun hasAnyPermissions(permissions: List<String>): Boolean
    fun hasAllPermissions(permissions: Collection<String>): Boolean
}

@Fake
interface CacheService<TKey, TValue> {
    val size: Int
    val maxSize: Int?

    fun get(key: TKey): TValue?
    fun put(key: TKey, value: TValue): TValue?
    fun remove(key: TKey): TValue?
    fun clear()
    fun containsKey(key: TKey): Boolean
    fun keys(): Set<TKey>
    fun values(): Collection<TValue>
    fun <R> computeIfAbsent(key: TKey, computer: (TKey) -> R): R where R : TValue
    suspend fun <R> asyncComputeIfAbsent(key: TKey, computer: suspend (TKey) -> R): R where R : TValue
}
