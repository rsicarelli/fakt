# Pattern: Complex Generics Strategy

> **Purpose**: Advanced patterns for handling generic types in KtFakes generation
> **Complexity**: Advanced - requires understanding of Kotlin type system
> **Status**: Phase 2A implementation with known limitations
> **Testing Standard**: [📋 Testing Guidelines](../validation/testing-guidelines.md)

## 🎯 **Generic Handling Overview**

KtFakes provides different levels of generic type support:
- ✅ **Interface-level generics**: Fully supported
- ⚠️ **Method-level generics**: Phase 2A with casting workarounds
- 🔮 **Complex constraints**: Future Phase 2B implementation

## ✅ **Fully Supported: Interface-Level Generics**

### **Basic Generic Interfaces**
```kotlin
@Fake
interface Repository<T> {
    suspend fun save(entity: T): T
    suspend fun findById(id: String): T?
    suspend fun findAll(): List<T>
    fun delete(entity: T): Boolean
}
```

**Generated Code**:
```kotlin
class FakeRepositoryImpl : Repository<Any> {
    private var saveBehavior: suspend (Any) -> Any = { it }
    private var findByIdBehavior: suspend (String) -> Any? = { null }
    private var findAllBehavior: suspend () -> List<Any> = { emptyList() }
    private var deleteBehavior: (Any) -> Boolean = { false }

    override suspend fun save(entity: Any): Any = saveBehavior(entity)
    override suspend fun findById(id: String): Any? = findByIdBehavior(id)
    override suspend fun findAll(): List<Any> = findAllBehavior()
    override fun delete(entity: Any): Boolean = deleteBehavior(entity)
}
```

**Type-Safe Usage**:
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RepositoryTest {

    @Test
    fun `GIVEN generic repository WHEN using with specific type THEN should maintain type safety`() = runTest {
        // Given - Type-safe configuration for User entities
        val savedUsers = mutableListOf<User>()
        val userRepository: Repository<User> = fakeRepository<User> {
            save { user ->
                savedUsers.add(user as User)
                user
            }
            findById { id ->
                savedUsers.find { it.id == id }
            }
            findAll { savedUsers.toList() }
            delete { user ->
                savedUsers.remove(user as User)
            }
        }

        // When
        val user = User("123", "john@example.com", "John")
        val savedUser = userRepository.save(user)
        val foundUser = userRepository.findById("123")

        // Then
        assertEquals(user.id, savedUser.id)
        assertEquals(user, foundUser)
        assertEquals(1, savedUsers.size)
    }
}
```

### **Multiple Type Parameters**
```kotlin
@Fake
interface CacheService<K, V> {
    fun put(key: K, value: V): V?
    fun get(key: K): V?
    fun remove(key: K): V?
    fun containsKey(key: K): Boolean
    val size: Int
}
```

**Usage with Multiple Generics**:
```kotlin
@Test
fun `GIVEN cache service WHEN using multiple type parameters THEN should handle correctly`() = runTest {
    // Given - String keys, User values
    val cache = mutableMapOf<String, User>()
    val cacheService: CacheService<String, User> = fakeCacheService<String, User> {
        put { key, value ->
            cache.put(key as String, value as User)
        }
        get { key ->
            cache[key as String]
        }
        containsKey { key ->
            cache.containsKey(key as String)
        }
        size { cache.size }
    }

    // When
    val user = User("123", "john@example.com", "John")
    cacheService.put("user:123", user)

    // Then
    assertEquals(user, cacheService.get("user:123"))
    assertTrue(cacheService.containsKey("user:123"))
    assertEquals(1, cacheService.size)
}
```

## ⚠️ **Phase 2A: Method-Level Generics with Workarounds**

### **Current Challenge**
```kotlin
@Fake
interface GenericProcessor {
    fun <T> process(data: T): T                    // Method-level generic
    fun <T, R> transform(input: T): R              // Multiple method-level generics
    suspend fun <T> asyncProcess(data: T): Result<T>  // Suspend + generic
}
```

**Current Generated Code (Phase 2A)**:
```kotlin
class FakeGenericProcessorImpl : GenericProcessor {
    private var processBehavior: (Any?) -> Any? = { it }
    private var transformBehavior: (Any?) -> Any? = { it }
    private var asyncProcessBehavior: suspend (Any?) -> Result<Any?> = { Result.success(it) }

    override fun <T> process(data: T): T {
        @Suppress("UNCHECKED_CAST")
        return processBehavior(data) as T
    }

    override fun <T, R> transform(input: T): R {
        @Suppress("UNCHECKED_CAST")
        return transformBehavior(input) as R
    }

    override suspend fun <T> asyncProcess(data: T): Result<T> {
        @Suppress("UNCHECKED_CAST")
        return asyncProcessBehavior(data) as Result<T>
    }
}
```

### **Safe Usage Patterns for Method-Level Generics**
```kotlin
@Test
fun `GIVEN method-level generics WHEN using identity functions THEN should work safely`() = runTest {
    // Given - Identity function pattern (safest approach)
    val processor = fakeGenericProcessor {
        process { data -> data }  // Identity function - always safe
        transform { input -> input }  // Identity transformation
        asyncProcess { data -> Result.success(data) }  // Safe Result wrapping
    }

    // When - Using with known types
    val stringResult = processor.process("test-string")
    val intResult = processor.process(42)
    val asyncResult = processor.asyncProcess("async-test")

    // Then
    assertEquals("test-string", stringResult)
    assertEquals(42, intResult)
    assertTrue(asyncResult.isSuccess)
    assertEquals("async-test", asyncResult.getOrNull())
}
```

### **Type-Specific Behaviors**
```kotlin
@Test
fun `GIVEN method-level generics WHEN using type-specific logic THEN should handle correctly`() = runTest {
    // Given - Type-specific behavior (requires careful casting)
    val processor = fakeGenericProcessor {
        process { data ->
            when (data) {
                is String -> data.uppercase() as Any?
                is Int -> (data * 2) as Any?
                else -> data
            }
        }
        transform { input ->
            when (input) {
                is String -> input.length as Any?  // String -> Int transformation
                is Int -> input.toString() as Any?  // Int -> String transformation
                else -> input
            }
        }
    }

    // When
    val uppercaseResult = processor.process("hello")
    val doubleResult = processor.process(21)
    val lengthResult: Int = processor.transform("test")
    val stringResult: String = processor.transform(42)

    // Then
    assertEquals("HELLO", uppercaseResult)
    assertEquals(42, doubleResult)
    assertEquals(4, lengthResult)
    assertEquals("42", stringResult)
}
```

## 🔧 **Advanced Generic Patterns**

### **Bounded Type Parameters**
```kotlin
@Fake
interface BoundedRepository<T : Entity> {
    suspend fun save(entity: T): T
    suspend fun findById(id: String): T?
}

// Base entity interface
interface Entity {
    val id: String
}
```

**Current Workaround**:
```kotlin
@Test
fun `GIVEN bounded generics WHEN using entity types THEN should respect bounds`() = runTest {
    // Given - Work with concrete Entity types
    val repository: BoundedRepository<User> = fakeBoundedRepository<User> {
        save { entity ->
            // Type bounds ensure entity has id property
            println("Saving entity with id: ${(entity as Entity).id}")
            entity
        }
        findById { id ->
            User(id, "test@example.com", "Test User") as Any?
        }
    }

    // When
    val user = User("123", "john@example.com", "John")
    val savedUser = repository.save(user)

    // Then
    assertEquals("123", savedUser.id)
}
```

### **Generic Function Types**
```kotlin
@Fake
interface FunctionalProcessor {
    fun <T, R> map(items: List<T>, transform: (T) -> R): List<R>
    suspend fun <T> filter(items: List<T>, predicate: suspend (T) -> Boolean): List<T>
}
```

**Function Type Handling**:
```kotlin
@Test
fun `GIVEN functional processor WHEN using function types THEN should handle transformations`() = runTest {
    // Given
    val processor = fakeFunctionalProcessor {
        map { items, transform ->
            (items as List<Any?>).map { item ->
                @Suppress("UNCHECKED_CAST")
                (transform as (Any?) -> Any?)(item)
            }
        }
        filter { items, predicate ->
            val result = mutableListOf<Any?>()
            for (item in items as List<Any?>) {
                @Suppress("UNCHECKED_CAST")
                if ((predicate as suspend (Any?) -> Boolean)(item)) {
                    result.add(item)
                }
            }
            result
        }
    }

    // When
    val numbers = listOf(1, 2, 3, 4, 5)
    val doubled = processor.map(numbers) { it * 2 }
    val evens = processor.filter(numbers) { it % 2 == 0 }

    // Then
    assertEquals(listOf(2, 4, 6, 8, 10), doubled)
    assertEquals(listOf(2, 4), evens)
}
```

## 🎯 **Best Practices for Generic Fakes**

### **1. Prefer Interface-Level Generics**
```kotlin
// ✅ GOOD: Interface-level generics (fully supported)
@Fake
interface UserRepository : Repository<User> {
    suspend fun findByEmail(email: String): User?
}

// ⚠️ CHALLENGING: Method-level generics (requires workarounds)
@Fake
interface GenericService {
    fun <T> process(data: T): T
}
```

### **2. Use Identity Functions for Safety**
```kotlin
// ✅ SAFE: Identity function pattern
val processor = fakeGenericProcessor {
    process { data -> data }  // Always safe
}

// ⚠️ RISKY: Complex transformations
val processor = fakeGenericProcessor {
    process { data ->
        // Complex logic with casting - test thoroughly
        when (data) {
            is String -> data.uppercase() as Any?
            else -> data
        }
    }
}
```

### **3. Test with Concrete Types**
```kotlin
@Test
fun `GIVEN generic fake WHEN testing THEN use concrete types for safety`() = runTest {
    // ✅ GOOD: Test with specific, known types
    val stringProcessor: GenericProcessor = fakeGenericProcessor {
        process { data -> (data as String).uppercase() as Any? }
    }

    val result: String = stringProcessor.process("hello")
    assertEquals("HELLO", result)
}
```

### **4. Document Type Expectations**
```kotlin
@Test
fun `GIVEN complex generic fake WHEN configuring THEN document expected types`() = runTest {
    // Document expected input/output types
    val processor = fakeGenericProcessor {
        // Expects: String -> String (uppercase transformation)
        process { data ->
            require(data is String) { "Expected String, got ${data?.javaClass}" }
            data.uppercase() as Any?
        }

        // Expects: T -> List<T> (wrap in list)
        transform { input ->
            listOf(input) as Any?
        }
    }
}
```

## 🔮 **Future: Phase 2B Full Generic Support**

### **Planned Improvements**
```kotlin
// Future: Full type safety without casting
@Fake
interface FutureGenericService {
    fun <T> process(data: T): T
    fun <T, R> transform(input: T): R
}

// Future generated code (planned):
class FakeFutureGenericServiceImpl : FutureGenericService {
    // Type-safe generic handling without Any casting
    // Proper type parameter preservation
    // Zero unchecked cast warnings
}
```

## 🔗 **Related Patterns**

- **[📋 Basic Fake Generation](basic-fake-generation.md)** - Foundation patterns
- **[📋 Suspend Function Handling](suspend-function-handling.md)** - Async patterns
- **[📋 Generic Scoping Analysis](../analysis/generic-scoping-analysis.md)** - Technical deep dive
- **[📋 Type Safety Validation](../validation/type-safety-validation.md)** - Testing approach

---

**Complex generics in KtFakes require understanding current limitations and using appropriate workarounds. Interface-level generics work perfectly, while method-level generics require careful handling until Phase 2B implementation.**