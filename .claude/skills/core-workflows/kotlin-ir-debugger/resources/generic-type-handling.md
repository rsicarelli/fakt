# Generic Type Parameter Handling

> **Loaded on-demand** for debugging generic type issues

## Current Status: Phase 2 Challenge

### Working ✅
- **Method-level generics** with identity functions
- **Simple type parameters** (T, K, V)
- **Function types** with generics `(T) -> R`

### Phase 2 Challenge 🚧
- **Class-level generic scoping** (Repository<T>)
- **Variance annotations** (out T, in T)
- **Complex bounds** (T : Number, T : Comparable<T>)

## Generic Types in Fakt

### Method-Level Generics (Working)

```kotlin
@Fake
interface Transformer {
    fun <T> identity(value: T): T
    fun <T, R> map(input: T, transform: (T) -> R): R
}

// Generated (Phase 1 approach):
class FakeTransformerImpl : Transformer {
    private var identityBehavior: (Any?) -> Any? = { it }
    private var mapBehavior: (Any?, (Any?) -> Any?) -> Any? = { input, transform ->
        transform(input)
    }

    override fun <T> identity(value: T): T {
        @Suppress("UNCHECKED_CAST")
        return identityBehavior(value) as T
    }

    override fun <T, R> map(input: T, transform: (T) -> R): R {
        @Suppress("UNCHECKED_CAST")
        return mapBehavior(input, transform as (Any?) -> Any?) as R
    }
}
```

**Status**: ✅ Works with identity function pattern + safe casting

### Class-Level Generics (Phase 2 Challenge)

```kotlin
@Fake
interface Repository<T> {
    fun save(item: T)
    fun findAll(): List<T>
    fun findById(id: String): T?
}

// Current Phase 1 approach (workaround):
class FakeRepositoryImpl : Repository<Any?> {
    private var saveBehavior: (Any?) -> Unit = {}
    private var findAllBehavior: () -> List<Any?> = { emptyList() }
    private var findByIdBehavior: (String) -> Any? = { null }

    override fun save(item: Any?) = saveBehavior(item)
    override fun findAll(): List<Any?> = findAllBehavior()
    override fun findById(id: String): Any? = findByIdBehavior(id)
}

// ⚠️ ISSUE: Loses type safety at use-site
val repo: Repository<User> = fakeRepository<User>()  // Can't do this yet
val repo: Repository<Any?> = FakeRepositoryImpl()    // Current workaround
```

**Status**: 🚧 Phase 2 target - needs IrTypeSubstitutor integration

## Phase 2A Solution: Dynamic Casting with Identity Functions

### Strategy

```kotlin
// Target Phase 2A generation:
class FakeRepositoryImpl<T> : Repository<T> {  // Preserve generic parameter
    private var saveBehavior: (T) -> Unit = {}
    private var findAllBehavior: () -> List<T> = { emptyList() }

    override fun save(item: T) = saveBehavior(item)
    override fun findAll(): List<T> = findAllBehavior()
}

// Type-safe factory:
fun <T> fakeRepository(configure: FakeRepositoryConfig<T>.() -> Unit = {}): Repository<T> {
    return FakeRepositoryImpl<T>().apply {
        FakeRepositoryConfig(this).configure()
    }
}
```

### IrTypeSubstitutor Usage

```kotlin
// Metro-inspired type substitution
val substitutor = IrTypeSubstitutor(
    typeParameters = fakeInterface.typeParameters,
    typeArguments = factoryTypeArguments,
    pluginContext = context.pluginContext
)

// Substitute T in method signatures
val substitutedReturnType = substitutor.substitute(method.returnType)
```

## Debugging Generic Type Issues

### Symptoms of Generic Problems

1. **Compilation Error: Type mismatch**
   ```
   Error: Type mismatch: inferred type is Any? but User was expected
   ```
   → Indicates class-level generic T replaced with Any?

2. **Warning: Unchecked cast**
   ```
   Warning: Unchecked cast: Any? to T
   ```
   → Method-level generic using identity function (expected in Phase 1)

3. **Runtime ClassCastException**
   ```
   ClassCastException: Cannot cast String to Int
   ```
   → Type erasure issue, check generic bounds

### Diagnostic Steps

1. **Identify Generic Scope**
   ```kotlin
   // Is it class-level?
   interface Repository<T>  // YES - Phase 2 challenge

   // Or method-level?
   interface Transformer {
       fun <T> process(value: T): T  // YES - should work
   }
   ```

2. **Check Generated Code**
   ```bash
   # Find generated fake
   find build/generated/fakt -name "Fake*.kt"

   # Check for Any? replacement
   grep "Any?" build/generated/fakt/.../FakeRepositoryImpl.kt
   ```

3. **Validate Type Parameters**
   ```kotlin
   // In IR debugging:
   irClass.typeParameters.forEach { typeParam ->
       println("Type parameter: ${typeParam.name}")
       println("  Variance: ${typeParam.variance}")
       println("  Upper bounds: ${typeParam.superTypes}")
   }
   ```

## Workarounds for Phase 1

### Use Concrete Types

```kotlin
// Instead of:
@Fake
interface Repository<T> { ... }

// Use:
@Fake
interface UserRepository {
    fun save(user: User)
    fun findAll(): List<User>
}

@Fake
interface ProductRepository {
    fun save(product: Product)
    fun findAll(): List<Product>
}
```

### Accept Any? at Test Level

```kotlin
// In tests, accept the limitation:
val fakeRepo: Repository<Any?> = FakeRepositoryImpl()

// Configure with concrete types anyway:
fakeRepo.configureSave { user ->
    // user is Any? but you know it's User in this test
    val typedUser = user as User
    // test logic
}
```

## Phase 2 Roadmap

### Phase 2A: Method-Level Generics (Complete)
- ✅ Identity function pattern
- ✅ Safe casting with @Suppress
- ✅ Function types with generics

### Phase 2B: Class-Level Generics (In Progress)
- 🚧 IrTypeSubstitutor integration
- 🚧 Generic factory functions
- 🚧 Type-safe configuration DSL

### Phase 2C: Advanced Generics (Future)
- ⏳ Variance annotations (out/in)
- ⏳ Complex bounds (T : Comparable<T>)
- ⏳ Multiple type parameters with constraints

## References

- Generic Scoping Analysis: `.claude/docs/analysis/generic-scoping-analysis.md`
- Phase 2 Roadmap: `.claude/docs/implementation/generics/phase2-code-generation.md`
- IrTypeSubstitutor API: Consult with `/consult-kotlin-api IrTypeSubstitutor`
