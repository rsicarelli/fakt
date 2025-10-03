# Fakt - Compile-Time Type Safety: Real Examples

> **Goal**: Show concrete examples of how compile-time safe generation would work  
> **Focus**: Practical implementation that developers will love

## 🎯 **THE VISION: What We're Building**

```kotlin
// This is what developers will write - 100% type-safe, zero casting
val userRepo = fakeRepository<User, Long> {
    save { user -> 
        // 'user' is User, not Any - full IDE auto-completion!
        user.copy(id = 123L, updatedAt = Clock.now())
    }
}

val service = fakeAsyncService {
    // Each type gets its own configuration - compile-time checked!
    processData<User> { user -> user.copy(verified = true) }
    processData<Order> { order -> order.copy(status = PROCESSED) }
    // processData<User> { order: Order -> ... } // ❌ Compile error!
}
```

## 📦 **EXAMPLE 1: Repository Pattern (Class-Level Generics)**

### Source Interface
```kotlin
interface Repository<T : Entity, ID : Comparable<ID>> {
    fun save(entity: T): T
    fun findById(id: ID): T?
    fun findAll(): List<T>
    fun deleteById(id: ID): Boolean
    fun existsById(id: ID): Boolean
}

interface UserRepository : Repository<User, Long> {
    fun findByEmail(email: String): User?
}
```

### Generated Code - Full Type Safety!
```kotlin
// Generic base implementation
class FakeRepositoryImpl<T : Entity, ID : Comparable<ID>> : Repository<T, ID> {
    private var saveBehavior: (T) -> T = { it }
    private var findByIdBehavior: (ID) -> T? = { null }
    private var findAllBehavior: () -> List<T> = { emptyList() }
    private var deleteByIdBehavior: (ID) -> Boolean = { false }
    private var existsByIdBehavior: (ID) -> Boolean = { false }
    
    override fun save(entity: T): T = saveBehavior(entity)
    override fun findById(id: ID): T? = findByIdBehavior(id)
    override fun findAll(): List<T> = findAllBehavior()
    override fun deleteById(id: ID): Boolean = deleteByIdBehavior(id)
    override fun existsById(id: ID): Boolean = existsByIdBehavior(id)
    
    // Type-safe configuration
    class Config<T : Entity, ID : Comparable<ID>>(
        private val impl: FakeRepositoryImpl<T, ID>
    ) {
        fun save(behavior: (T) -> T) { impl.saveBehavior = behavior }
        fun findById(behavior: (ID) -> T?) { impl.findByIdBehavior = behavior }
        fun findAll(behavior: () -> List<T>) { impl.findAllBehavior = behavior }
        fun deleteById(behavior: (ID) -> Boolean) { impl.deleteByIdBehavior = behavior }
        fun existsById(behavior: (ID) -> Boolean) { impl.existsByIdBehavior = behavior }
    }
}

// Specialized UserRepository implementation
class FakeUserRepositoryImpl : UserRepository, Repository<User, Long> by FakeRepositoryImpl<User, Long>() {
    private var findByEmailBehavior: (String) -> User? = { null }
    
    override fun findByEmail(email: String): User? = findByEmailBehavior(email)
    
    fun configureFindByEmail(behavior: (String) -> User?) {
        findByEmailBehavior = behavior
    }
}

// Factory functions with perfect type inference
inline fun <reified T : Entity, reified ID : Comparable<ID>> fakeRepository(
    configure: FakeRepositoryImpl.Config<T, ID>.() -> Unit = {}
): Repository<T, ID> {
    return FakeRepositoryImpl<T, ID>().apply {
        Config(this).configure()
    }
}

fun fakeUserRepository(
    configure: FakeUserRepositoryConfig.() -> Unit = {}
): UserRepository {
    return FakeUserRepositoryImpl().apply {
        FakeUserRepositoryConfig(this).configure()
    }
}

// Usage - Beautiful and Type-Safe!
val userRepo = fakeRepository<User, Long> {
    save { user -> 
        // 'user' is User, not Any!
        user.copy(
            id = generateId(),
            createdAt = Instant.now(),
            version = user.version + 1
        )
    }
    
    findById { id -> 
        // 'id' is Long, not Any!
        testUsers[id]
    }
    
    findAll { 
        testUsers.values.toList()
    }
}

val specializedRepo = fakeUserRepository {
    findByEmail { email ->
        // Full type safety!
        testUsers.values.find { it.email == email }
    }
    
    save { user ->
        testUsers[user.id] = user
        user
    }
}
```

## 🔄 **EXAMPLE 2: Async Service (Method-Level Generics)**

### Source Interface
```kotlin
interface AsyncDataService {
    suspend fun <T> processData(data: T): T
    suspend fun <T, R> transformData(data: T, transformer: (T) -> R): R
    suspend fun <T : Comparable<T>> sortData(items: List<T>): List<T>
}
```

### Generated Code - Smart Type Registry
```kotlin
// Type-safe builder with reified types
class FakeAsyncDataServiceBuilder {
    // Type registry for each method
    private val processDataHandlers = TypedHandlerRegistry<ProcessHandler<*>>()
    private val transformDataHandlers = TypedHandlerRegistry<TransformHandler<*, *>>()
    private val sortDataHandlers = TypedHandlerRegistry<SortHandler<*>>()
    
    // Reified inline functions capture types at compile time
    inline fun <reified T> processData(
        noinline handler: suspend (T) -> T
    ) {
        processDataHandlers.register(
            T::class,
            ProcessHandler(T::class, handler)
        )
    }
    
    inline fun <reified T, reified R> transformData(
        noinline handler: suspend (T, (T) -> R) -> R
    ) {
        transformDataHandlers.register(
            TypePair(T::class, R::class),
            TransformHandler(T::class, R::class, handler)
        )
    }
    
    inline fun <reified T : Comparable<T>> sortData(
        noinline handler: suspend (List<T>) -> List<T>
    ) {
        sortDataHandlers.register(
            T::class,
            SortHandler(T::class, handler)
        )
    }
    
    fun build(): AsyncDataService = FakeAsyncDataServiceImpl(
        processDataHandlers,
        transformDataHandlers,
        sortDataHandlers
    )
}

// Implementation with smart type dispatch
class FakeAsyncDataServiceImpl(
    private val processHandlers: TypedHandlerRegistry<ProcessHandler<*>>,
    private val transformHandlers: TypedHandlerRegistry<TransformHandler<*, *>>,
    private val sortHandlers: TypedHandlerRegistry<SortHandler<*>>
) : AsyncDataService {
    
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> processData(data: T): T {
        // Smart lookup by actual runtime type
        val handler = processHandlers.findHandler(data!!::class) as? ProcessHandler<T>
        return handler?.handler?.invoke(data) ?: data // Identity fallback
    }
    
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T, R> transformData(
        data: T, 
        transformer: (T) -> R
    ): R {
        // We know the types from registration
        val handler = transformHandlers.findHandler(
            TypePair(data!!::class, Any::class) // R type determined at runtime
        ) as? TransformHandler<T, R>
        
        return handler?.handler?.invoke(data, transformer) 
            ?: transformer(data) // Default: just apply transformer
    }
    
    @Suppress("UNCHECKED_CAST")
    override suspend fun <T : Comparable<T>> sortData(items: List<T>): List<T> {
        if (items.isEmpty()) return items
        
        val handler = sortHandlers.findHandler(
            items.first()!!::class
        ) as? SortHandler<T>
        
        return handler?.handler?.invoke(items) 
            ?: items.sorted() // Default: natural ordering
    }
}

// Type-safe wrappers
class ProcessHandler<T>(
    val type: KClass<*>,
    val handler: suspend (T) -> T
)

class TransformHandler<T, R>(
    val inputType: KClass<*>,
    val outputType: KClass<*>,
    val handler: suspend (T, (T) -> R) -> R
)

class SortHandler<T : Comparable<T>>(
    val type: KClass<*>,
    val handler: suspend (List<T>) -> List<T>
)

// Factory function
fun fakeAsyncDataService(
    configure: FakeAsyncDataServiceBuilder.() -> Unit = {}
): AsyncDataService {
    return FakeAsyncDataServiceBuilder().apply(configure).build()
}

// Usage - Type-Safe and Beautiful!
val service = fakeAsyncDataService {
    // Each type gets its own handler - compile-time checked!
    processData<User> { user ->
        user.copy(
            lastActive = Instant.now(),
            processedCount = user.processedCount + 1
        )
    }
    
    processData<Order> { order ->
        order.copy(
            status = OrderStatus.PROCESSED,
            processedAt = Instant.now()
        )
    }
    
    processData<String> { str ->
        str.trim().uppercase()
    }
    
    transformData<User, UserDto> { user, transformer ->
        // Validate before transforming
        require(user.email.isNotBlank()) { "Email required" }
        transformer(user.copy(validated = true))
    }
    
    transformData<Order, OrderSummary> { order, transformer ->
        // Add computed fields before transforming
        val enrichedOrder = order.copy(
            total = order.items.sumOf { it.price * it.quantity }
        )
        transformer(enrichedOrder)
    }
    
    sortData<User> { users ->
        // Custom sorting logic
        users.sortedWith(
            compareBy({ it.role.priority }, { it.name })
        )
    }
}

// In tests - everything just works!
class ServiceTest {
    @Test
    fun `processData handles different types correctly`() = runTest {
        val service = fakeAsyncDataService {
            processData<User> { it.copy(name = "Processed") }
            processData<Order> { it.copy(status = COMPLETED) }
        }
        
        val user = User(1, "John")
        val processedUser = service.processData(user)
        assertEquals("Processed", processedUser.name) // ✅ Type-safe!
        
        val order = Order(1, PENDING)
        val processedOrder = service.processData(order)
        assertEquals(COMPLETED, processedOrder.status) // ✅ Type-safe!
    }
}
```

## 🎨 **EXAMPLE 3: Complex Generic Constraints**

### Source Interface
```kotlin
interface CacheService<K : Any, V : Any> {
    fun get(key: K): V?
    fun put(key: K, value: V): V?
    fun <R : V> computeIfAbsent(key: K, mappingFunction: (K) -> R): R
    fun <R> getOrElse(key: K, defaultValue: () -> R): Any where R : V
}
```

### Generated Code - Handling Constraints
```kotlin
class FakeCacheServiceImpl<K : Any, V : Any> : CacheService<K, V> {
    private var getBehavior: (K) -> V? = { null }
    private var putBehavior: (K, V) -> V? = { _, _ -> null }
    private var computeIfAbsentBehavior: (K, (K) -> V) -> V = { k, f -> f(k) }
    private var getOrElseBehavior: (K, () -> V) -> V = { _, default -> default() }
    
    override fun get(key: K): V? = getBehavior(key)
    override fun put(key: K, value: V): V? = putBehavior(key, value)
    
    override fun <R : V> computeIfAbsent(
        key: K, 
        mappingFunction: (K) -> R
    ): R {
        // The constraint R : V is enforced at compile time!
        @Suppress("UNCHECKED_CAST")
        return computeIfAbsentBehavior(key, mappingFunction) as R
    }
    
    override fun <R> getOrElse(
        key: K, 
        defaultValue: () -> R
    ): Any where R : V {
        return getOrElseBehavior(key, defaultValue as () -> V)
    }
    
    // Type-safe configuration
    fun configureGet(behavior: (K) -> V?) { getBehavior = behavior }
    fun configurePut(behavior: (K, V) -> V?) { putBehavior = behavior }
}

// Factory with constraints preserved
inline fun <reified K : Any, reified V : Any> fakeCacheService(
    configure: FakeCacheServiceImpl<K, V>.() -> Unit = {}
): CacheService<K, V> {
    return FakeCacheServiceImpl<K, V>().apply(configure)
}

// Usage - Constraints enforced!
val cache = fakeCacheService<String, User> {
    val storage = mutableMapOf<String, User>()
    
    configureGet { key -> 
        storage[key]
    }
    
    configurePut { key, value ->
        storage.put(key, value)
    }
}

// This works - AdminUser : User
cache.computeIfAbsent("admin") { AdminUser("admin", Role.ADMIN) }

// This won't compile - Order is not a subtype of User
// cache.computeIfAbsent("order") { Order(1) } // ❌ Compile error!
```

## 💡 **KEY INNOVATION: Smart Pattern Detection**

```kotlin
// During compilation, analyze the interface pattern
fun analyzeInterfacePattern(irClass: IrClass): GenericPattern {
    val classTypeParams = irClass.typeParameters
    val methodTypeParams = irClass.functions.flatMap { it.typeParameters }
    
    return when {
        classTypeParams.isEmpty() && methodTypeParams.isEmpty() -> 
            GenericPattern.NoGenerics
            
        classTypeParams.isNotEmpty() && methodTypeParams.isEmpty() ->
            GenericPattern.ClassLevel(classTypeParams)
            
        classTypeParams.isEmpty() && methodTypeParams.isNotEmpty() ->
            GenericPattern.MethodLevel(methodTypeParams)
            
        else ->
            GenericPattern.Mixed(classTypeParams, methodTypeParams)
    }
}

// Generate different code based on pattern
fun generateFake(pattern: GenericPattern, irClass: IrClass): String {
    return when (pattern) {
        is GenericPattern.ClassLevel -> 
            generateGenericClassFake(irClass) // Full type parameters
            
        is GenericPattern.MethodLevel ->
            generateReifiedBuilderFake(irClass) // Type registry approach
            
        is GenericPattern.Mixed ->
            generateHybridFake(irClass) // Combination of both
            
        is GenericPattern.NoGenerics ->
            generateSimpleFake(irClass) // Current approach
    }
}
```

## 🚀 **THE RESULT: Developer Experience**

```kotlin
// What developers write - clean, type-safe, intuitive
class RepositoryTest {
    private val userRepo = fakeRepository<User, Long> {
        val users = mutableMapOf<Long, User>()
        
        save { user ->
            val saved = user.copy(id = users.size + 1L)
            users[saved.id] = saved
            saved
        }
        
        findById { id -> users[id] }
        
        findAll { users.values.toList() }
    }
    
    @Test
    fun `saves user with generated ID`() {
        val user = User(0, "John", "john@example.com")
        val saved = userRepo.save(user)
        
        assertNotEquals(0, saved.id)
        assertEquals("John", saved.name)
        assertNotNull(userRepo.findById(saved.id))
    }
}

class ServiceTest {
    private val service = fakeAsyncDataService {
        processData<User> { user ->
            // Full IDE support - 'user' is User, not Any!
            user.copy(verified = true)
        }
        
        transformData<User, UserDto> { user, transformer ->
            require(user.verified) { "User must be verified" }
            transformer(user)
        }
    }
    
    @Test
    fun `processes and transforms user`() = runTest {
        val user = User(1, "Jane", "jane@example.com")
        val processed = service.processData(user)
        
        assertTrue(processed.verified)
        
        val dto = service.transformData(processed) { u ->
            UserDto(u.id, u.name)
        }
        
        assertEquals(1, dto.id)
        assertEquals("Jane", dto.name)
    }
}
```

## 🎯 **CONCLUSION**

This approach provides:
- ✅ **100% Compile-Time Type Safety** - No runtime casting in user code
- ✅ **Perfect IDE Support** - Full auto-completion and type checking
- ✅ **Beautiful API** - As clean as hand-written mocks
- ✅ **Zero Learning Curve** - Intuitive for any Kotlin developer
- ✅ **Performance** - Minimal overhead, no reflection
- ✅ **Maintainability** - Generated code is readable and debuggable

Fakt becomes the **only** Kotlin mocking framework with true compile-time type safety!