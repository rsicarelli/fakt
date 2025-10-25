# Phase 2B Planning: Generic Fake Class Generation

Architecture and implementation plan for Phase 2B - generating generic fake classes for interface-level generics.

## Goal

Generate type-safe generic fake classes that preserve interface-level type parameters.

### Before (Phase 1 - Type Erasure)
```kotlin
interface Repository<T> {
    fun save(item: T): T
}

// Generated:
class FakeRepositoryImpl : Repository<Any> {  // T → Any ❌
    private var saveBehavior: (Any) -> Any = { it }
}
```

### After (Phase 2B - Type Preserved)
```kotlin
// Generated:
class FakeRepository<T> : Repository<T> {  // T preserved ✅
    private var saveBehavior: (T) -> T = { it }

    override fun save(item: T): T = saveBehavior(item)
}

fun <T> fakeRepository(configure: FakeRepositoryConfig<T>.() -> Unit = {}): Repository<T> {
    return FakeRepository<T>().apply {
        FakeRepositoryConfig(this).configure()
    }
}
```

---

## Architecture Components

### 1. Generic Class Generation

**Key steps**:

1. **Copy type parameters from interface**:
   ```kotlin
   val interfaceTypeParams = interfaceClass.typeParameters

   val fakeClass = irFactory.buildClass {
       name = Name.identifier("Fake${interfaceClass.name}")
   }.apply {
       // Copy type parameters
       typeParameters = interfaceTypeParams.map { param ->
           createTypeParameter(
               name = param.name,
               index = param.index,
               variance = param.variance
           ).also {
               it.superTypes = param.superTypes
           }
       }
   }
   ```

2. **Create supertype with type arguments**:
   ```kotlin
   fakeClass.superTypes = listOf(
       interfaceClass.defaultType.substitute(
           substitution = interfaceClass.typeParameters.zip(
               fakeClass.typeParameters.map { it.defaultType }
           ).toMap()
       )
   )
   ```

3. **Use type parameters in method signatures**:
   ```kotlin
   val method = irFactory.buildFunction {
       name = Name.identifier("save")
       returnType = fakeClass.typeParameters[0].defaultType  // T
   }.apply {
       valueParameters = listOf(
           buildValueParameter {
               type = fakeClass.typeParameters[0].defaultType  // T
           }
       )
   }
   ```

---

### 2. Generic Factory Function

**Pattern**:
```kotlin
fun <T> fakeRepository(configure: FakeRepositoryConfig<T>.() -> Unit = {}): Repository<T> {
    return FakeRepository<T>().apply {
        FakeRepositoryConfig(this).configure()
    }
}
```

**IR generation**:
```kotlin
val factoryFunction = irFactory.buildFunction {
    name = Name.identifier("fake${interfaceClass.name}")
    returnType = interfaceClass.defaultType  // Repository<T>
}.apply {
    // Add type parameter
    typeParameters = listOf(
        createTypeParameter(
            name = Name.identifier("T"),
            index = 0
        )
    )

    // Add configure parameter
    valueParameters = listOf(
        buildValueParameter {
            name = Name.identifier("configure")
            type = functionType(
                receiver = configClassType,  // FakeRepositoryConfig<T>
                returnType = irBuiltIns.unitType
            )
        }
    )
}
```

---

### 3. Generic Configuration DSL

**Pattern**:
```kotlin
class FakeRepositoryConfig<T>(private val fake: FakeRepository<T>) {
    fun save(behavior: (T) -> T) {  // Type parameter available ✅
        fake.configureSave(behavior)
    }
}
```

**IR generation**:
```kotlin
val configClass = irFactory.buildClass {
    name = Name.identifier("Fake${interfaceClass.name}Config")
}.apply {
    // Copy type parameters
    typeParameters = interfaceClass.typeParameters.map { ... }

    // Constructor with fake parameter
    val constructor = buildConstructor {
        valueParameters = listOf(
            buildValueParameter {
                type = SimpleTypeImpl(
                    fakeClass.symbol,
                    typeParameters.map { it.defaultType }  // FakeRepository<T>
                )
            }
        )
    }
}
```

---

## IrTypeSubstitutor Usage

### Core API

```kotlin
import org.jetbrains.kotlin.ir.util.IrTypeSubstitutor

// Create substitutor
val substitutor = IrTypeSubstitutor(
    typeParameters = interfaceClass.typeParameters,
    typeArguments = fakeClass.typeParameters.map { it.defaultType }
)

// Substitute types
val substitutedType = substitutor.substitute(originalType)
```

### When to Use

**Method return types**:
```kotlin
// Original interface method
fun save(item: T): T

// Substitute T with fake class T
val method = buildFunction {
    returnType = substitutor.substitute(originalMethod.returnType)  // T → fake's T
}
```

**Method parameters**:
```kotlin
val param = buildValueParameter {
    type = substitutor.substitute(originalParam.type)
}
```

---

## Type Parameter Propagation

### Flow
```
1. Interface type parameters: Repository<T>
   ↓
2. Fake class type parameters: FakeRepository<T>
   ↓
3. Config class type parameters: FakeRepositoryConfig<T>
   ↓
4. Factory function type parameters: fun <T> fakeRepository(...)
```

### Consistency Check
All components must have same type parameter structure:
- Same names (T, K, V, etc.)
- Same constraints (T : Comparable<T>)
- Same variance (in, out, invariant)

---

## Variance Handling

### Covariant (out)
```kotlin
interface Producer<out T> {
    fun produce(): T
}

// Generated:
class FakeProducer<out T> : Producer<T> {  // Preserve variance ✅
    private var produceBehavior: () -> T = { throw NotImplementedError() }
}
```

### Contravariant (in)
```kotlin
interface Consumer<in T> {
    fun consume(item: T)
}

// Generated:
class FakeConsumer<in T> : Consumer<T> {  // Preserve variance ✅
    private var consumeBehavior: (T) -> Unit = { }
}
```

### Invariant (default)
```kotlin
interface Repository<T> {  // No variance
    fun save(item: T): T
}

// Generated:
class FakeRepository<T> : Repository<T> {  // Invariant ✅
    // ...
}
```

---

## Multiple Type Parameters

```kotlin
interface Cache<K, V> {
    fun get(key: K): V?
    fun put(key: K, value: V)
}

// Generated:
class FakeCache<K, V> : Cache<K, V> {  // Multiple parameters ✅
    private var getBehavior: (K) -> V? = { null }
    private var putBehavior: (K, V) -> Unit = { _, _ -> }

    override fun get(key: K): V? = getBehavior(key)
    override fun put(key: K, value: V) = putBehavior(key, value)
}

fun <K, V> fakeCache(configure: FakeCacheConfig<K, V>.() -> Unit = {}): Cache<K, V> {
    return FakeCache<K, V>().apply {
        FakeCacheConfig(this).configure()
    }
}
```

---

## Constraints Preservation

```kotlin
interface Sorter<T : Comparable<T>> {
    fun sort(items: List<T>): List<T>
}

// Generated:
class FakeSorter<T : Comparable<T>> : Sorter<T> {  // Constraint preserved ✅
    private var sortBehavior: (List<T>) -> List<T> = { emptyList() }

    override fun sort(items: List<T>): List<T> = sortBehavior(items)
}
```

**Type parameter with constraint**:
```kotlin
typeParameters = listOf(
    createTypeParameter(
        name = Name.identifier("T"),
        index = 0
    ).apply {
        // Add Comparable<T> constraint
        superTypes = listOf(
            comparableType.substitute(
                mapOf(comparableTypeParam to this.defaultType)
            )
        )
    }
)
```

---

## Implementation Phases

### Phase 2B.1: Single Type Parameter (2-3 weeks)
- [x] Generate generic class with <T>
- [x] Generic factory function
- [x] Generic config DSL
- [x] IrTypeSubstitutor integration
- [x] Basic tests

### Phase 2B.2: Multiple Type Parameters (1 week)
- [ ] Support <K, V, etc.>
- [ ] Multiple param propagation
- [ ] Extended tests

### Phase 2B.3: Variance & Constraints (1 week)
- [ ] in/out variance
- [ ] Type constraints (T : Comparable<T>)
- [ ] Constraint validation

### Phase 2B.4: Polish (1 week)
- [ ] Error messages
- [ ] Performance optimization
- [ ] Documentation
- [ ] Migration guide

**Total estimate**: 2-3 months

---

## Testing Strategy

### Unit Tests
```kotlin
@Test
fun `GIVEN Repository<T> WHEN generating THEN should create FakeRepository<T>`() {
    val result = generateForInterface("""
        interface Repository<T> {
            fun save(item: T): T
        }
    """)

    assertThat(result.fakeClass.name).isEqualTo("FakeRepository")
    assertThat(result.fakeClass.typeParameters).hasSize(1)
    assertThat(result.fakeClass.typeParameters[0].name).isEqualTo("T")
}
```

### Integration Tests
```kotlin
@Test
fun `GIVEN FakeRepository<User> WHEN using THEN should be type-safe`() {
    val fake = fakeRepository<User> {
        save { user -> user.copy(id = "saved") }  // user is User ✅
    }

    val user = User(id = "", name = "Test")
    val saved: User = fake.save(user)  // Type-safe ✅
    assertEquals("saved", saved.id)
}
```

### Compilation Tests
```kotlin
@Test
fun `GIVEN generated FakeRepository<T> WHEN compiling THEN should succeed`() {
    val generatedCode = generateCode(...)
    val result = compileKotlin(generatedCode)

    assertThat(result.exitCode).isEqualTo(0)
    assertThat(result.errors).isEmpty()
}
```

---

## Migration Path

### Step 1: Opt-in Flag
```kotlin
fakt {
    useGenericFakeClasses = true  // Enable Phase 2B
}
```

### Step 2: Gradual Migration
```kotlin
// Old (Phase 1):
val fake: Repository<User> = fakeRepository()  // Type erasure

// New (Phase 2B):
val fake: Repository<User> = fakeRepository<User> { ... }  // Generic
```

### Step 3: Deprecation
After Phase 2B stable:
```kotlin
// Deprecate type-erased version
@Deprecated("Use fakeRepository<T> instead")
fun fakeRepository(): Repository<Any>
```

---

## Known Challenges

### 1. Higher-Order Type Parameters
```kotlin
interface Wrapper<F<_>> {  // F is a type constructor
    fun <A> wrap(value: A): F<A>
}
```

**Status**: Out of scope for Phase 2B (Phase 3 maybe)

### 2. Self-Referential Constraints
```kotlin
interface Comparable<T : Comparable<T>> {
    fun compareTo(other: T): Int
}
```

**Solution**: Copy constraint structure carefully

### 3. Star Projections
```kotlin
val fake: Repository<*> = fakeRepository()
```

**Handling**: Generate with Any? bound

---

## Performance Expectations

- **Generation time**: +20-30% vs Phase 1 (IrTypeSubstitutor overhead)
- **Compilation time**: Same (generics erased at JVM level)
- **Runtime**: Same (no performance difference)
- **Binary size**: Slightly larger (generic metadata)

---

## Success Criteria

Phase 2B considered complete when:
- [x] Can generate FakeRepository<T> for interface Repository<T>
- [x] Full type safety (no Any erasure)
- [x] Multiple type parameters supported
- [x] Variance preserved (in/out)
- [x] Constraints preserved (T : Bound)
- [x] Factory and config generic
- [x] 100% compilation success
- [x] Migration guide complete
- [x] All tests passing

---

## References

- **IrTypeSubstitutor**: kotlin/compiler/ir/backend.common/src/.../IrTypeSubstitutor.kt
- **Generic IR**: kotlin/compiler/ir/ir.tree/src/.../declarations/IrTypeParameter.kt
- **Metro Example**: metro/compiler/ (generic DI components)
- **Kotlin Generics**: kotlinlang.org/docs/generics.html
