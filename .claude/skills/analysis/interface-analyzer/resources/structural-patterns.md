# Common Interface Structural Patterns

Catalog of common @Fake interface patterns with analysis guidance.

## Service Layer Patterns

### 1. Simple CRUD Service
```kotlin
@Fake
interface UserService {
    fun create(user: User): User
    fun read(id: String): User?
    fun update(user: User): Boolean
    fun delete(id: String): Boolean
}
```

**Characteristics**:
- Basic CRUD operations
- Simple parameter/return types
- No generics
- No suspend functions

**Complexity**: LOW
**Phase Support**: Phase 1 ✅
**Generation**: Standard approach

---

### 2. Async Service Pattern
```kotlin
@Fake
interface AsyncUserService {
    suspend fun fetchUser(id: String): Result<User>
    suspend fun createUser(user: User): Result<User>
    suspend fun updateUser(user: User): Result<Boolean>
}
```

**Characteristics**:
- All methods suspend
- Result<T> return types
- Async/await pattern

**Complexity**: MEDIUM
**Phase Support**: Phase 1 ✅ (suspend fully supported)
**Generation**: Behavior properties must be suspend

---

### 3. Event Handler Pattern
```kotlin
@Fake
interface EventHandler {
    fun onEvent(event: Event, callback: (Result) -> Unit)
    fun subscribe(listener: EventListener)
    fun unsubscribe(listener: EventListener)
}
```

**Characteristics**:
- Function type parameters
- Listener/callback pattern
- Side-effect focused

**Complexity**: MEDIUM
**Phase Support**: Phase 1 ✅ (function types supported)
**Default behavior**: Empty lambdas `{ }`, no-op for void

---

## Data Access Patterns

### 4. Repository Pattern (Generic)
```kotlin
@Fake
interface Repository<T> {
    fun save(entity: T): T
    fun findById(id: String): T?
    fun findAll(): List<T>
    fun delete(entity: T)
}
```

**Characteristics**:
- Interface-level generic T
- CRUD operations on generic type
- Common in data layer

**Complexity**: MEDIUM
**Phase Support**: Phase 1 ⚠️ (T becomes Any)
**Phase 2B**: Generic fake class FakeRepository<T>

**Workaround for Phase 1**:
```kotlin
// Instead of Repository<User>, create concrete interface
@Fake
interface UserRepository {
    fun save(entity: User): User
    fun findById(id: String): User?
    // ...
}
```

---

### 5. DAO Pattern
```kotlin
@Fake
interface UserDao {
    suspend fun insert(user: User): Long
    suspend fun update(user: User): Int
    suspend fun delete(user: User): Int
    suspend fun query(sql: String, args: Array<Any>): List<User>
}
```

**Characteristics**:
- Database access pattern
- Suspend functions
- Numeric return types (row count, ID)

**Complexity**: MEDIUM
**Phase Support**: Phase 1 ✅
**Generation**: Standard suspend + primitive defaults

---

## Utility/Helper Patterns

### 6. Validator Pattern
```kotlin
@Fake
interface Validator<T> {
    fun validate(data: T): ValidationResult
    fun isValid(data: T): Boolean
}
```

**Characteristics**:
- Interface-level generic
- Boolean/result return types
- Pure functions (no side effects)

**Complexity**: MEDIUM
**Phase Support**: Phase 1 ⚠️ (generic T → Any)
**Phase 2B**: Full type safety

---

### 7. Mapper/Transformer Pattern
```kotlin
@Fake
interface Mapper<In, Out> {
    fun map(input: In): Out
    fun mapList(inputs: List<In>): List<Out>
}
```

**Characteristics**:
- Two generic type parameters
- Transformation logic
- Collection support

**Complexity**: MEDIUM
**Phase Support**: Phase 1 ⚠️ (In, Out → Any)
**Phase 2B**: Generic fake class

---

### 8. Predicate/Filter Pattern
```kotlin
@Fake
interface Predicate<T> {
    fun test(value: T): Boolean
    fun and(other: Predicate<T>): Predicate<T>
    fun or(other: Predicate<T>): Predicate<T>
}
```

**Characteristics**:
- Single generic parameter
- Boolean logic
- Combinator methods returning same type

**Complexity**: MEDIUM-HIGH
**Phase Support**: Phase 1 ⚠️ (complex for combinator methods)
**Recommendation**: Simplify for Phase 1

---

## Advanced Patterns

### 9. Method-Level Generic Pattern
```kotlin
@Fake
interface DataProcessor {
    fun <T> process(data: T): T
    fun <T, R> transform(input: T, fn: (T) -> R): R
}
```

**Characteristics**:
- Method-level generics
- No interface-level generics
- **Core scoping challenge**

**Complexity**: HIGH
**Phase Support**: Phase 1 ❌ (scoping issue)
**Phase 2A**: Identity function + dynamic casting

**Problem**:
```kotlin
// Behavior property can't access method-level <T>
private var processBehavior: (???) -> ???  // What type?

// Method has <T> in scope
override fun <T> process(data: T): T = processBehavior(data)  // Type mismatch
```

**Phase 2A Solution**:
```kotlin
private var processBehavior: (Any?) -> Any? = { it }  // Identity

override fun <T> process(data: T): T {
    @Suppress("UNCHECKED_CAST")
    return processBehavior(data) as T
}
```

---

### 10. Mixed Generic Pattern
```kotlin
@Fake
interface Cache<K, V> {
    fun get(key: K): V?
    fun put(key: K, value: V)
    fun <R : V> computeIfAbsent(key: K, fn: (K) -> R): R
}
```

**Characteristics**:
- Interface-level generics (K, V)
- Method-level generic with constraint (R : V)
- **Most complex pattern**

**Complexity**: VERY HIGH
**Phase Support**: Phase 1 ❌
**Phase 2A + 2B**: Hybrid solution

---

### 11. Higher-Order Type Parameters
```kotlin
@Fake
interface Wrapper<F<_>> {  // Rare but exists
    fun <A> wrap(value: A): F<A>
    fun <A, B> map(fa: F<A>, fn: (A) -> B): F<B>
}
```

**Characteristics**:
- Type constructor generics
- Category theory patterns
- Very rare in practice

**Complexity**: EXTREME
**Phase Support**: Phase 1 ❌, Phase 2 ❌
**Phase 3**: Potential future support

**Recommendation**: Avoid or use concrete wrapper type

---

## Property Patterns

### 12. Read-Only Properties
```kotlin
@Fake
interface Configuration {
    val appName: String
    val version: String
    val isDebug: Boolean
}
```

**Characteristics**:
- Only `val` properties
- No methods
- Configuration/settings pattern

**Complexity**: LOW
**Phase Support**: Phase 1 ✅
**Generation**: Property getters with defaults

---

### 13. Mutable State Pattern
```kotlin
@Fake
interface StateManager {
    var currentUser: User?
    var isAuthenticated: Boolean
    fun reset()
}
```

**Characteristics**:
- Mix of `var` and methods
- Mutable state
- Stateful interface

**Complexity**: LOW-MEDIUM
**Phase Support**: Phase 1 ✅
**Generation**: Mutable properties + method behaviors

---

### 14. Computed Properties
```kotlin
@Fake
interface UserProfile {
    val firstName: String
    val lastName: String
    val fullName: String  // Typically computed
    val isActive: Boolean
}
```

**Characteristics**:
- Properties that may depend on others
- No setters (read-only)

**Complexity**: LOW
**Phase Support**: Phase 1 ✅
**Note**: Generated properties are independent (not computed)

---

## Special Type Patterns

### 15. Nullable Types Pattern
```kotlin
@Fake
interface NullableService {
    fun findUser(id: String): User?
    fun findAll(): List<User>?
    val currentUser: User?
}
```

**Characteristics**:
- Extensive use of nullable types
- Safe defaults (null)

**Complexity**: LOW
**Phase Support**: Phase 1 ✅
**Default**: null for all nullable types

---

### 16. Collection Types Pattern
```kotlin
@Fake
interface CollectionService {
    fun getUsers(): List<User>
    fun getActiveUsers(): Set<User>
    fun getUserMap(): Map<String, User>
}
```

**Characteristics**:
- Collection return types
- No mutations in interface

**Complexity**: LOW
**Phase Support**: Phase 1 ✅
**Defaults**: emptyList(), emptySet(), emptyMap()

---

### 17. Function Type Pattern
```kotlin
@Fake
interface CallbackManager {
    fun onClick(handler: (Event) -> Unit)
    fun onData(processor: (Data) -> Result)
    fun filter(predicate: (User) -> Boolean)
}
```

**Characteristics**:
- Function type parameters
- Event/callback pattern

**Complexity**: MEDIUM
**Phase Support**: Phase 1 ✅
**Defaults**: Empty lambdas `{ }` or `{ null }`

---

## Anti-Patterns to Avoid

### ❌ Complex Nested Generics
```kotlin
@Fake
interface ComplexService<T, R> {
    fun <U, V> process(
        data: Map<T, List<U>>,
        fn: (U) -> Map<V, R>
    ): List<Pair<T, V>>
}
```

**Why avoid**: Extreme complexity, hard to generate safely

**Better**:
```kotlin
@Fake
interface SimpleService {
    fun process(data: DataInput): DataOutput
}

data class DataInput(...)
data class DataOutput(...)
```

---

### ❌ Overloaded Methods (Same Name)
```kotlin
@Fake
interface OverloadedService {
    fun save(user: User): User
    fun save(user: User, validate: Boolean): User
    fun save(users: List<User>): List<User>
}
```

**Status**: Supported but complex

**Recommendation**: Use distinct names when possible
```kotlin
@Fake
interface ClearService {
    fun saveUser(user: User): User
    fun saveUserWithValidation(user: User, validate: Boolean): User
    fun saveUsers(users: List<User>): List<User>
}
```

---

### ❌ Sealed Interface (Kotlin 1.5+)
```kotlin
@Fake
sealed interface Command {
    data class Create(...) : Command
    data class Update(...) : Command
}
```

**Status**: Not applicable (sealed = closed hierarchy)

**Reason**: Cannot create fake of sealed interface

**Alternative**: Use regular interface

---

## Pattern Selection Guide

**Choose Simple CRUD** when:
- Basic data operations
- No async requirements
- Simple types only

**Choose Async Service** when:
- Suspend functions needed
- Result<T> error handling
- Coroutine-based

**Choose Generic Repository** when:
- Reusable data access
- Accept Phase 2B requirement OR use concrete types

**Avoid Complex Generics** when:
- Phase 1 immediate need
- Type safety critical
- Performance sensitive

---

## Pattern Complexity Quick Reference

| Pattern | Generic Level | Complexity | Phase 1 | Phase 2A | Phase 2B |
|---------|--------------|------------|---------|----------|----------|
| Simple CRUD | None | LOW | ✅ | - | - |
| Async Service | None | MEDIUM | ✅ | - | - |
| Event Handler | None | MEDIUM | ✅ | - | - |
| Generic Repository | Interface | MEDIUM | ⚠️ | - | ✅ |
| Method-level Generic | Method | HIGH | ❌ | ✅ | - |
| Mixed Generics | Both | VERY HIGH | ❌ | ✅ | ✅ |

---

## References

- **Phase Roadmap**: `.claude/docs/implementation/generics/ROADMAP.md`
- **Generic Scoping**: `.claude/docs/analysis/generic-scoping-analysis.md`
- **Testing Patterns**: `.claude/docs/validation/testing-guidelines.md`
