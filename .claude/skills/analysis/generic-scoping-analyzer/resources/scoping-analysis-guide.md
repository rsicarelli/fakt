# Deep Generic Type Scoping Analysis

Technical deep dive into the generic type parameter scoping challenge.

## The Core Problem

### Kotlin/JVM Type Parameter Scoping Rules

**Class-level type parameters**:
```kotlin
class Box<T>(val item: T) {
    // T is accessible:
    private var value: T = item        // ✅ In properties
    fun get(): T = value              // ✅ In methods
    fun set(newValue: T) { value = newValue }  // ✅ In parameters
}
```

**Method-level type parameters**:
```kotlin
class Processor {
    // T is NOT accessible here
    private var behavior: (T) -> T = ...  // ❌ ERROR: T unresolved

    fun <T> process(data: T): T {
        // T IS accessible here ✅
        return data
    }
}
```

**Why?** Method type parameters are local to that method scope only.

---

## Fakt's Specific Challenge

### What We Need to Generate

**For interface**:
```kotlin
@Fake
interface Repository<T> {
    fun save(item: T): T
}
```

**We want to generate**:
```kotlin
class FakeRepositoryImpl : Repository<T> {  // Need T here
    private var saveBehavior: (T) -> T       // And here
    override fun save(item: T): T = saveBehavior(item)  // And here
}
```

**Problem**: Cannot use T in class name in Phase 1!

---

### Scoping Matrix

| Type Parameter | Declared | Accessible At Class Level | Accessible In Methods |
|----------------|----------|----------------------------|----------------------|
| Interface `<T>` | Interface | ❌ No (in Phase 1) | ✅ Yes |
| Method `<T>` | Method | ❌ No | ✅ Yes (in that method only) |

---

## Why This Is Hard

### Challenge 1: Kotlin Compiler IR Doesn't Preserve Generic Types Easily

**At IR level**:
- Generic types are complex IrType nodes
- Type parameters have symbols and constraints
- Preserving them requires IrTypeSubstitutor
- Not straightforward in Phase 1

### Challenge 2: Behavior Properties Need Type at Class Level

**Our architecture**:
```kotlin
// We generate this pattern:
class FakeXImpl {
    private var methodBehavior: (ParamType) -> ReturnType = default

    override fun method(param: ParamType): ReturnType = methodBehavior(param)
}
```

**Problem**: `methodBehavior` is a class property, must know its type at class declaration time.

**For method-level generics**:
```kotlin
interface Processor {
    fun <T> process(data: T): T
}

// Cannot generate:
class FakeProcessorImpl {
    private var processBehavior: (T) -> T  // T not in scope! ❌
}
```

---

## Technical Solutions

### Solution 1: Type Erasure (Phase 1)

**Erase to Any**:
```kotlin
// Interface-level <T> → Any
class FakeRepositoryImpl : Repository<Any> {
    private var saveBehavior: (Any) -> Any = { it }
}
```

**Pros**:
- ✅ Works immediately
- ✅ Compiles successfully

**Cons**:
- ❌ Lost type safety
- ❌ Manual casting needed
- ❌ Not ideal developer experience

---

### Solution 2: Dynamic Casting (Phase 2A)

**For method-level generics**:
```kotlin
interface Processor {
    fun <T> process(data: T): T
}

// Generate:
class FakeProcessorImpl : Processor {
    // Use Any? (universal supertype)
    private var processBehavior: (Any?) -> Any? = { it }

    override fun <T> process(data: T): T {
        // T in scope here, can cast
        @Suppress("UNCHECKED_CAST")
        return processBehavior(data) as T
    }
}
```

**Why this works**:
1. **Any?** is supertype of all types (can hold any value)
2. **Identity function** `{ it }` preserves input
3. **Method has T in scope** - can cast there
4. **Call site is type-safe** - T inferred correctly

**Trade-off**: Configuration is type-erased, but safe at call site

---

### Solution 3: Generic Fake Class (Phase 2B)

**For interface-level generics**:
```kotlin
interface Repository<T> {
    fun save(item: T): T
}

// Generate generic class:
class FakeRepository<T> : Repository<T> {
    private var saveBehavior: (T) -> T = { it }  // T in scope ✅

    override fun save(item: T): T = saveBehavior(item)
}

fun <T> fakeRepository(configure: ...): Repository<T> {
    return FakeRepository<T>().apply { ... }
}
```

**Why this works**:
1. **Generic class declaration** - T available throughout
2. **Type parameter propagation** - Factory and config also generic
3. **Full type safety** - No erasure, no casting

**Complexity**: Requires IrTypeSubstitutor, complex IR generation

---

## Implementation Details

### Phase 2A: IrTypeParameter Handling

**Key Kotlin APIs**:
```kotlin
when (val classifier = irType.classifier) {
    is IrTypeParameterSymbol -> {
        val typeParameter = classifier.owner
        val name = typeParameter.name.asString()  // "T"
        val index = typeParameter.index           // 0
        val isReified = typeParameter.isReified  // false

        // Generate Any? for behavior property
        val anyType = pluginContext.irBuiltIns.anyNType
    }
}
```

**Generation**:
```kotlin
// For method <T> process(T): T
// Generate:
private var processBehavior: (Any?) -> Any? = { it }

override fun <T> process(data: T): T {
    @Suppress("UNCHECKED_CAST")
    return processBehavior(data) as T
}
```

---

### Phase 2B: IrTypeSubstitutor Usage

**Key pattern**:
```kotlin
// Substitute type parameters
val typeSubstitutor = IrTypeSubstitutor(
    typeParameters = interfaceClass.typeParameters,
    typeArguments = fakeClass.typeParameters.map { it.defaultType }
)

// Apply substitution
val substitutedType = typeSubstitutor.substitute(originalType)
```

**Generate generic class**:
```kotlin
val fakeClass = irFactory.buildClass {
    name = Name.identifier("Fake${interfaceClass.name}")
    kind = ClassKind.CLASS
}.apply {
    // Copy type parameters from interface
    typeParameters = interfaceClass.typeParameters.map { param ->
        irFactory.createTypeParameter(
            name = param.name,
            index = param.index
        )
    }
}
```

---

## Scoping Analysis Algorithm

### Step 1: Collect All Type Parameters
```kotlin
data class GenericAnalysis(
    val interfaceTypeParams: List<IrTypeParameter>,   // <T> on interface
    val methodTypeParams: Map<IrFunction, List<IrTypeParameter>>  // <T> on methods
)
```

### Step 2: Classify Each Parameter
```kotlin
enum class TypeParamLevel {
    INTERFACE,  // Accessible at class level
    METHOD      // Only accessible in specific method
}
```

### Step 3: Determine Strategy
```kotlin
when {
    interfaceTypeParams.isEmpty() && methodTypeParams.isEmpty() ->
        Strategy.STANDARD  // No generics

    methodTypeParams.isEmpty() ->
        Strategy.PHASE_2B  // Interface-level only

    interfaceTypeParams.isEmpty() ->
        Strategy.PHASE_2A  // Method-level only

    else ->
        Strategy.HYBRID  // Both levels
}
```

---

## Edge Cases

### Reified Type Parameters
```kotlin
interface Processor {
    fun <reified T> process(): T
}
```

**Status**: Not supported (reified requires inline, can't be interface method)

### Type Parameter Constraints
```kotlin
interface Sorter<T : Comparable<T>> {
    fun sort(items: List<T>): List<T>
}
```

**Phase 1**: Constraint lost (T → Any)
**Phase 2B**: Constraint preserved in generic class

### Variance Annotations
```kotlin
interface Producer<out T> {
    fun produce(): T
}

interface Consumer<in T> {
    fun consume(item: T)
}
```

**Phase 2B**: Preserve variance in generic fake class

---

## Performance Implications

### Phase 1 (Type Erasure)
- Generation: Fast (no complex IR manipulation)
- Runtime: Fastest (no generic overhead)
- Type safety: Lowest

### Phase 2A (Dynamic Casting)
- Generation: Moderate (cast insertion)
- Runtime: Slight overhead (runtime cast)
- Type safety: Partial (call-site safe)

### Phase 2B (Generic Classes)
- Generation: Slowest (complex IR)
- Runtime: Fast (generics erased at runtime anyway)
- Type safety: Highest

---

## References

- **Kotlin Type System**: kotlin/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/types/
- **IrTypeParameter**: kotlin/compiler/ir/ir.tree/src/.../IrTypeParameter.kt
- **IrTypeSubstitutor**: kotlin/compiler/ir/backend.common/src/.../IrTypeSubstitutor.kt
- **Metro Example**: metro/compiler/ (production usage)
