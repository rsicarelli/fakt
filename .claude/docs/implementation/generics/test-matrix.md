# Generic Type Support - Comprehensive Test Matrix

> **Purpose**: Systematic validation of all generic type scenarios
> **Testing Framework**: kotlin-compile-testing with multi-stage validation
> **Standard**: GIVEN-WHEN-THEN pattern (THE ABSOLUTE STANDARD)

## 🎯 Testing Strategy

### Multi-Stage Validation

All tests follow this pattern:

```kotlin
@Test
fun `GIVEN [scenario] WHEN [action] THEN [outcome]`() = runTest {
    // Stage 1: Generation & Compilation
    val generationResult = compileWithFakt(sourceCode)
    assertEquals(KotlinCompilation.ExitCode.OK, generationResult.exitCode)

    // Stage 2: Structural Validation
    val fakeClass = generationResult.classLoader.loadClass("FakeImpl")
    // Assert structure is correct

    // Stage 3: Use-Site Type Safety (most critical!)
    val usageResult = compileUsageCode(generationResult.classpaths, usageCode)
    assertEquals(KotlinCompilation.ExitCode.OK, usageResult.exitCode)
}
```

### Why Multi-Stage?

1. **Stage 1**: Proves fake generates without compilation errors
2. **Stage 2**: Proves structure matches expectations
3. **Stage 3**: Proves type safety works at use-site (THE REAL TEST!)

---

## 📋 Priority 0: Basic Class-Level Generics (MUST PASS 100%)

### T0.1: Single Type Parameter

```kotlin
@Fake
interface Repository<T> {
    fun save(item: T): T
    fun findById(id: String): T?
    fun findAll(): List<T>
}

// Expected Generation:
class FakeRepositoryImpl<T> : Repository<T> {
    private var saveBehavior: (T) -> T = { it }
    private var findByIdBehavior: (String) -> T? = { null }
    private var findAllBehavior: () -> List<T> = { emptyList() }
}

inline fun <reified T> fakeRepository(
    configure: FakeRepositoryConfig<T>.() -> Unit = {}
): Repository<T>

// Use-Site Test:
val userRepo = fakeRepository<User> {
    save { user -> user }
}
val user: User = userRepo.save(User("123", "Test")) // TYPE CHECK!
```

**Test Cases**:
- [x] T0.1.1: Compilation success
- [x] T0.1.2: Class has 1 type parameter
- [x] T0.1.3: Factory function is reified
- [x] T0.1.4: Use-site type safety (User → User)
- [x] T0.1.5: Configuration DSL preserves types

---

### T0.2: Multiple Type Parameters

```kotlin
@Fake
interface Cache<K, V> {
    fun get(key: K): V?
    fun put(key: K, value: V): V?
    fun keys(): Set<K>
    fun values(): Collection<V>
}

// Expected Generation:
class FakeCacheImpl<K, V> : Cache<K, V>

inline fun <reified K, reified V> fakeCache(
    configure: FakeCacheConfig<K, V>.() -> Unit = {}
): Cache<K, V>

// Use-Site Test:
val cache = fakeCache<String, Int> {
    get { key -> 42 }
}
val result: Int? = cache.get("key") // TYPE CHECK!
```

**Test Cases**:
- [x] T0.2.1: Compilation success
- [x] T0.2.2: Class has 2 type parameters (K, V)
- [x] T0.2.3: Factory preserves parameter order
- [x] T0.2.4: Use-site type safety for both parameters
- [x] T0.2.5: Collection types use correct generics

---

### T0.3: Three Type Parameters

```kotlin
@Fake
interface TripleStore<K1, K2, V> {
    fun get(key1: K1, key2: K2): V?
    fun put(key1: K1, key2: K2, value: V): V?
}

// Use-Site Test:
val store = fakeTripleStore<String, Int, User> {
    get { k1, k2 -> User("123", "Test") }
}
```

**Test Cases**:
- [x] T0.3.1: Handles 3+ type parameters
- [x] T0.3.2: All parameters preserved in order

---

## 📋 Priority 1: Method-Level & Mixed Generics (MUST PASS 95%)

### T1.1: Method-Level Generics Only

```kotlin
@Fake
interface Processor {
    fun <R> transform(input: String): R
    fun <T> identity(item: T): T
    fun <A, B> combine(first: A, second: B): Pair<A, B>
}

// Expected Generation:
class FakeProcessorImpl : Processor {
    private var transformBehavior: <R>(String) -> R = { error("Configure") }

    override fun <R> transform(input: String): R = transformBehavior(input)
}

// Use-Site Test:
val processor = fakeProcessor {
    transform { input -> input.length } // String -> Int
}
val length: Int = processor.transform("test") // TYPE CHECK!
```

**Test Cases**:
- [x] T1.1.1: Single method type parameter
- [x] T1.1.2: Multiple method type parameters
- [x] T1.1.3: Use-site type inference works
- [x] T1.1.4: Identity function preserves type

**Challenge**: Method-level type parameters in behavior lambdas

---

### T1.2: Mixed Generics (Class + Method)

```kotlin
@Fake
interface Service<T> {
    fun process(item: T): T
    fun <R> transform(item: T): R
    fun <R> mapAll(items: List<T>): List<R>
}

// Expected Generation:
class FakeServiceImpl<T> : Service<T> {
    private var processBehavior: (T) -> T = { it }
    private var transformBehavior: <R>(T) -> R = { error("Configure") }
}

inline fun <reified T> fakeService(
    configure: FakeServiceConfig<T>.() -> Unit = {}
): Service<T>

// Use-Site Test:
val service = fakeService<User> {
    process { user -> user }
    transform { user -> user.name } // User -> String
}
val user: User = service.process(User("123", "Test"))
val name: String = service.transform(user)
```

**Test Cases**:
- [x] T1.2.1: Both class and method generics work
- [x] T1.2.2: Method uses class type parameter (T)
- [x] T1.2.3: Method introduces new type parameter (R)
- [x] T1.2.4: Type safety at all levels

---

## 📋 Priority 2: Constraints & Variance (MUST PASS 90%)

### T2.1: Type Constraints (Upper Bounds)

```kotlin
@Fake
interface NumberService<T : Number> {
    fun compute(value: T): T
    fun sum(values: List<T>): Double
}

// Use-Site Test:
val service = fakeNumberService<Int> { // ✅ Int : Number
    compute { value -> value }
}
// val invalid = fakeNumberService<String> {} // ❌ Should NOT compile
```

**Test Cases**:
- [x] T2.1.1: Single constraint (T : Number)
- [x] T2.1.2: Multiple constraints (where clause)
- [x] T2.1.3: Constraint violation detected at compile time
- [x] T2.1.4: Constraint propagates to generated code

---

### T2.2: Variance Annotations

```kotlin
@Fake
interface Producer<out T> {
    fun produce(): T
}

@Fake
interface Consumer<in T> {
    fun consume(item: T)
}

@Fake
interface Transformer<in I, out O> {
    fun transform(input: I): O
}

// Use-Site Test:
val producer: Producer<String> = fakeProducer<String> {
    produce { "test" }
}
val result: String = producer.produce() // ✅ Covariant

val consumer: Consumer<Any> = fakeConsumer<String> {
    consume { item -> println(item) }
}
consumer.consume("test") // ✅ Contravariant
```

**Test Cases**:
- [x] T2.2.1: Covariance (out T) preserved
- [x] T2.2.2: Contravariance (in T) preserved
- [x] T2.2.3: Mixed variance works
- [x] T2.2.4: Variance affects subtyping

---

### T2.3: Complex Constraints

```kotlin
@Fake
interface ComparableService<T> where T : Comparable<T>, T : java.io.Serializable {
    fun sort(items: List<T>): List<T>
}
```

**Test Cases**:
- [x] T2.3.1: Multiple where constraints
- [x] T2.3.2: Self-referential constraints

---

## 📋 Priority 3: Advanced Edge Cases (MUST PASS 80%)

### T3.1: Star Projections

```kotlin
@Fake
interface Handler {
    fun process(items: List<*>)
    fun getAll(): List<*>
}

// Expected: Use Any as fallback for star projections
class FakeHandlerImpl : Handler {
    private var processBehavior: (List<*>) -> Unit = {}
}
```

**Test Cases**:
- [x] T3.1.1: Star projection in parameter
- [x] T3.1.2: Star projection in return type
- [x] T3.1.3: Nested star projections

**Strategy**: Replace * with upper bound (Any?)

---

### T3.2: Nested Generics

```kotlin
@Fake
interface ComplexService {
    fun process(data: Map<String, List<Int>>): List<Map<String, Int>>
    fun nested(data: Map<String, Map<Int, List<User>>>): User?
}

// Use-Site Test:
val service = fakeComplexService {
    process { data -> listOf(mapOf("key" to 1)) }
}
val result: List<Map<String, Int>> = service.process(mapOf())
```

**Test Cases**:
- [x] T3.2.1: Two-level nesting (Map<K, List<V>>)
- [x] T3.2.2: Three-level nesting
- [x] T3.2.3: Mixed nesting with domain types

---

### T3.3: Recursive Generics

```kotlin
@Fake
interface Node<T : Node<T>> {
    fun getChildren(): List<T>
    fun getParent(): T?
}

// Challenge: T is bounded by itself!
// Strategy: Use fallback to upper bound or Any
```

**Test Cases**:
- [x] T3.3.1: Detect recursive constraint
- [x] T3.3.2: Apply fallback strategy
- [x] T3.3.3: Generate compilable code (even if type-erased)

**Expected**: May use type erasure for this edge case

---

### T3.4: Generic Function Types

```kotlin
@Fake
interface TransformService<T> {
    fun process(handler: (T) -> Unit)
    fun transform(mapper: (T) -> String): String
    fun <R> convert(converter: (T) -> R): R
}

// Use-Site Test:
val service = fakeTransformService<User> {
    process { handler -> handler(User("123", "Test")) }
}
service.process { user: User -> println(user.name) } // TYPE CHECK!
```

**Test Cases**:
- [x] T3.4.1: Function type with class generic
- [x] T3.4.2: Function type with method generic
- [x] T3.4.3: Nested function types

---

## 📋 Real-World Scenarios (Integration Tests)

### R1: Repository Pattern

```kotlin
@Fake
interface EntityRepository<T, ID> {
    suspend fun findById(id: ID): T?
    suspend fun save(entity: T): T
    suspend fun deleteById(id: ID): Boolean
    suspend fun findAll(): List<T>
}

// Usage:
val userRepo = fakeEntityRepository<User, String> {
    findById { id -> User(id, "Test User", "test@example.com") }
    save { user -> user }
}
```

---

### R2: Cache Pattern

```kotlin
@Fake
interface CacheManager<K, V> {
    fun get(key: K): V?
    fun put(key: K, value: V, ttl: Long = 0): V?
    fun <R> computeIfAbsent(key: K, computer: (K) -> R): R where R : V
}
```

---

### R3: Event Bus Pattern

```kotlin
@Fake
interface EventBus {
    fun <T> publish(event: T)
    fun <T> subscribe(eventType: Class<T>, handler: (T) -> Unit)
}
```

---

## 📊 Test Coverage Summary

| Priority | Category | Test Count | Required Pass Rate | Critical |
|----------|----------|------------|-------------------|----------|
| P0 | Basic Class-Level | 15 | 100% | ✅ YES |
| P1 | Method & Mixed | 10 | 95% | ✅ YES |
| P2 | Constraints & Variance | 8 | 90% | ⚠️ IMPORTANT |
| P3 | Edge Cases | 12 | 80% | ℹ️ NICE-TO-HAVE |
| R | Real-World | 5 | 90% | ✅ YES |
| **Total** | **All** | **50** | **92%** | |

---

## 🚨 Critical Test Failures

If any of these fail, **DO NOT PROCEED**:

1. **T0.1.4**: Basic use-site type safety (User → User)
2. **T0.2.4**: Multi-parameter type safety (K, V)
3. **T1.2.4**: Mixed generics type safety
4. **R1**: Repository pattern (most common use case)

These are the **absolute minimum** for production quality.

---

## 🔗 Related Documentation

- [Phase 3: Testing & Integration](./phase3-testing-integration.md)
- [Testing Guidelines](../../validation/testing-guidelines.md)
- [GenericFakeGenerationTest.kt](../../../compiler/src/test/kotlin/.../GenericFakeGenerationTest.kt)
