# Interface Generation Strategies

Decision tree and strategy guide for generating @Fake implementations based on interface complexity.

## Strategy Selection Decision Tree

```
Start: Analyze Interface
    |
    ├─ Has Generics?
    │   ├─ No ────────────────────────────────────────────> STRATEGY 1: Standard Generation
    │   │
    │   ├─ Interface-level only (Repository<T>)
    │   │   ├─ Can use concrete type? ──────────────────> STRATEGY 2: Concrete Type Substitution
    │   │   └─ Need generic type? ──────────────────────> STRATEGY 3: Phase 2B (Wait/Workaround)
    │   │
    │   ├─ Method-level only (<T> in methods)
    │   │   ├─ Can refactor to interface-level? ────────> STRATEGY 2: Refactor + Concrete
    │   │   └─ Need method generics? ───────────────────> STRATEGY 4: Phase 2A (Dynamic Casting)
    │   │
    │   └─ Mixed (interface + method generics) ─────────> STRATEGY 5: Phase 2A+2B Hybrid
    │
    └─ Check Special Features
        ├─ Suspend functions? ──────────────────────────> STRATEGY 1 (supported)
        ├─ Function types? ──────────────────────────────> STRATEGY 1 (supported)
        ├─ Nullable types? ──────────────────────────────> STRATEGY 1 (supported)
        └─ Collections? ─────────────────────────────────> STRATEGY 1 (supported)
```

---

## Strategy 1: Standard Generation (Phase 1)

### When to Use
- No generics OR
- Only suspend functions, function types, nullable, collections
- Complexity: LOW to MEDIUM

### Approach
Generate fake using current Phase 1 compiler plugin:

```bash
./gradlew :module:compileKotlinJvm
```

### Expected Output
```kotlin
// Generated: FakeUserServiceImpl.kt (immutable after construction)
class FakeUserServiceImpl(
    private val getUserBehavior: (String) -> User = { User("default") },
) : UserService {
    override fun getUser(id: String): User = getUserBehavior(id)
}

// Factory
fun fakeUserService(configure: FakeUserServiceConfig.() -> Unit = {}): UserService {
    val config = FakeUserServiceConfig().apply(configure)
    return FakeUserServiceImpl(
        getUserBehavior = config.getUserBehavior ?: { User("default") },
    )
}
```

### Success Criteria
- ✅ Compiles without errors
- ✅ All methods implemented
- ✅ Type-safe configuration DSL
- ✅ Smart defaults work

### Phase 1 Feature Support
| Feature | Supported | Default Behavior |
|---------|-----------|------------------|
| Suspend functions | ✅ | Suspend property |
| Function types | ✅ | Empty lambda `{ }` |
| Nullable types | ✅ | `null` |
| Collections | ✅ | `emptyList()` etc |
| Primitives | ✅ | `0`, `false`, `""` |

---

## Strategy 2: Concrete Type Substitution

### When to Use
- Interface has generics (interface or method level)
- Can use concrete types for your use case
- Complexity reduction acceptable

### Approach: Create Concrete Interface

**Original (generic)**:
```kotlin
@Fake
interface Repository<T> {
    fun save(item: T): T
    fun findById(id: String): T?
}
```

**Concrete version**:
```kotlin
@Fake
interface UserRepository {
    fun save(item: User): User
    fun findById(id: String): User?
}
```

### Benefits
- ✅ Works with Phase 1
- ✅ Full type safety
- ✅ No casting needed
- ✅ 100% compilation success

### Trade-offs
- ❌ Need separate interface per type (UserRepository, OrderRepository, etc.)
- ❌ Some code duplication
- ⚠️ More interfaces to maintain

### When Acceptable
- Small number of concrete types (1-3)
- Type-specific logic needed anyway
- Immediate Phase 1 implementation required

---

## Strategy 3: Phase 2B - Generic Fake Class (Future)

### When to Use
- Interface-level generics required
- Cannot/won't use concrete types
- Can wait for Phase 2B OR accept type erasure

### Option A: Wait for Phase 2B

**Timeline**: 2-3 months (estimated)

**Expected output**:
```kotlin
// Phase 2B will generate (immutable after construction):
class FakeRepository<T>(
    private val saveBehavior: (T) -> T = { it },  // Type-safe!
) : Repository<T> {
    override fun save(item: T): T = saveBehavior(item)
}

fun <T> fakeRepository(configure: FakeRepositoryConfig<T>.() -> Unit = {}): Repository<T> {
    val config = FakeRepositoryConfig<T>().apply(configure)
    return FakeRepository<T>(
        saveBehavior = config.saveBehavior ?: { it },
    )
}
```

**Benefits**:
- ✅ Full type safety
- ✅ Generic reuse (one fake for all T)
- ✅ No casting

### Option B: Use Phase 1 with Type Erasure

**Accept limitations**:
```kotlin
// Phase 1 generates (immutable):
class FakeRepositoryImpl(
    private val saveBehavior: (Any) -> Any = { it },
) : Repository<Any> {  // T → Any
    override fun save(item: Any): Any = saveBehavior(item)
}
```

**Usage**:
```kotlin
val fake = fakeRepository<User>()  // Type declared at usage
fake.save(user)  // Must cast manually
```

**Benefits**:
- ✅ Works now (Phase 1)
- ✅ Minimal code duplication

**Trade-offs**:
- ⚠️ Type erasure (Any)
- ⚠️ Manual casting needed
- ⚠️ Reduced type safety

---

## Strategy 4: Phase 2A - Dynamic Casting (In Progress)

### When to Use
- Method-level generics only
- No interface-level generics
- Timeline: 2-3 weeks (Phase 2A)

### Current Problem (Phase 1)
```kotlin
@Fake
interface Processor {
    fun <T> process(data: T): T
}

// Cannot generate because:
class FakeProcessorImpl : Processor {
    private var processBehavior: (???) -> ???  // <T> not in scope!

    override fun <T> process(data: T): T = processBehavior(data)  // Type mismatch
}
```

### Phase 2A Solution
```kotlin
class FakeProcessorImpl(
    // Use Any? with identity function (immutable)
    private val processBehavior: (Any?) -> Any? = { it },
) : Processor {
    override fun <T> process(data: T): T {
        @Suppress("UNCHECKED_CAST")
        return processBehavior(data) as T  // Dynamic cast
    }
}
```

### Usage
```kotlin
val fake = fakeProcessor {
    process { data ->
        // data is Any? (type erased)
        // return must be Any?
        data  // Identity
    }
}

// At call site
val result: String = fake.process("test")  // T inferred as String
```

### Benefits
- ✅ Supports method-level generics
- ✅ Compiles successfully
- ✅ Identity function is safest default

### Trade-offs
- ⚠️ Type erasure in configuration
- ⚠️ @Suppress("UNCHECKED_CAST") needed
- ⚠️ Runtime cast (safe if used correctly)

---

## Strategy 5: Hybrid Phase 2A + 2B

### When to Use
- Mixed generics (interface + method level)
- Most complex scenario
- Example: `Cache<K, V>` with `<R : V>` methods

### Combined Approach

**Interface**:
```kotlin
@Fake
interface Cache<K, V> {
    fun get(key: K): V?
    fun <R : V> computeIfAbsent(key: K, fn: (K) -> R): R
}
```

**Phase 2A handles method generic R**:
```kotlin
override fun <R : V> computeIfAbsent(key: K, fn: (K) -> R): R {
    @Suppress("UNCHECKED_CAST")
    return computeIfAbsentBehavior(key, fn) as R
}
```

**Phase 2B handles interface generics K, V**:
```kotlin
class FakeCache<K, V> : Cache<K, V> {
    // K and V available throughout class
}
```

**Timeline**: Phase 2A (2-3 weeks) + Phase 2B (2-3 months)

---

## Strategy Selection Matrix

| Interface Pattern | Complexity | Best Strategy | Timeline | Type Safety |
|-------------------|------------|---------------|----------|-------------|
| No generics | LOW | Strategy 1 | Now | 100% ✅ |
| Suspend + Collections | MEDIUM | Strategy 1 | Now | 100% ✅ |
| Interface-level generic | MEDIUM | Strategy 2 (concrete) | Now | 100% ✅ |
| Interface-level generic | MEDIUM | Strategy 3 (Phase 2B) | 2-3 months | 100% ✅ |
| Method-level generic | HIGH | Strategy 4 (Phase 2A) | 2-3 weeks | Partial ⚠️ |
| Mixed generics | VERY HIGH | Strategy 5 (2A+2B) | 3-4 months | Partial → Full |

---

## Complexity-Driven Strategy Recommendations

### LOW Complexity (Score 1-3)
**Recommendation**: Strategy 1 (Standard Generation)

```
✅ Use Phase 1 immediately
✅ Expect 100% success
✅ Full type safety
```

---

### MEDIUM Complexity (Score 4-6)

**If generics present**:
- **Prefer**: Strategy 2 (Concrete types)
- **Alternative**: Strategy 3 (Wait for 2B or accept erasure)

**If no generics** (just complex types):
- **Use**: Strategy 1 (Standard)

```
🎯 Decision point:
- Need multiple generic types? → Wait for Phase 2B
- 1-2 concrete types sufficient? → Use Strategy 2
- Complex but no generics? → Use Strategy 1
```

---

### HIGH Complexity (Score 7-8)

**If method-level generics**:
- **Wait**: Phase 2A (2-3 weeks)
- **Workaround**: Refactor to interface-level + concrete

**If interface-level generics**:
- **Wait**: Phase 2B (2-3 months)
- **Workaround**: Concrete types (Strategy 2)

```
⚠️ Phase 1 insufficient
💡 Options:
1. Simplify interface (reduce complexity)
2. Wait for appropriate phase
3. Accept limitations with workarounds
```

---

### VERY HIGH Complexity (Score 9+)

**Mixed generics**:
- **Wait**: Phase 2A + 2B (3-4 months)
- **Strongly recommend**: Simplify interface

```
🚨 Too complex for Phase 1
📋 Recommendation: Interface refactoring
- Split into multiple simpler interfaces
- Use concrete types
- Remove method-level generics if possible
```

---

## Decision Support Questions

### Q1: Can you wait for Phase 2?
- **Yes** → Use future strategies (3, 4, 5)
- **No** → Use current strategies (1, 2) or simplify

### Q2: Is type safety critical?
- **Yes** → Avoid type erasure (use concrete types)
- **No** → Accept erasure for faster delivery

### Q3: How many concrete types?
- **1-2** → Strategy 2 (concrete) is viable
- **3+** → Strategy 3 (generic) better long-term

### Q4: Can you refactor the interface?
- **Yes** → Simplify to reduce complexity
- **No** → Wait for appropriate phase

---

## Testing Strategy Per Approach

### Strategy 1 (Standard)
```kotlin
@Test
fun `GIVEN simple interface WHEN generating fake THEN should compile`() = runTest {
    val fake = fakeUserService {
        getUser { User("test") }
    }

    assertEquals(User("test"), fake.getUser("123"))
}
```

### Strategy 2 (Concrete)
```kotlin
@Test
fun `GIVEN concrete repository WHEN saving THEN should work`() = runTest {
    val fake = fakeUserRepository {
        save { user -> user.copy(id = "saved") }
    }

    val user = User(id = "", name = "Test")
    val saved = fake.save(user)
    assertEquals("saved", saved.id)
}
```

### Strategy 4 (Phase 2A)
```kotlin
@Test
fun `GIVEN method generic WHEN processing THEN should preserve type`() = runTest {
    val fake = fakeProcessor {
        process { it }  // Identity (Any? -> Any?)
    }

    val result: String = fake.process("test")  // T inferred
    assertEquals("test", result)
}
```

---

## Strategy Evolution Path

```
Phase 1 (Now):
├─ Strategy 1: Standard (no generics)
└─ Strategy 2: Concrete types

Phase 2A (2-3 weeks):
├─ Strategy 1: Still works
├─ Strategy 2: Still works
└─ Strategy 4: Method generics ✅

Phase 2B (2-3 months):
├─ Strategy 1: Still works
├─ Strategy 2: Still works (but less needed)
├─ Strategy 3: Interface generics ✅
├─ Strategy 4: Method generics ✅
└─ Strategy 5: Mixed generics ✅
```

---

## Summary Recommendations

1. **Start simple**: Use Strategy 1 when possible
2. **Prefer concrete over waiting**: Strategy 2 gets you moving
3. **Plan for phases**: Know which phase supports your needs
4. **Test complexity early**: Analyze before implementing
5. **Iterate**: Start with Strategy 2, migrate to 3 later

**Golden Rule**: Choose the simplest strategy that meets your immediate needs!
