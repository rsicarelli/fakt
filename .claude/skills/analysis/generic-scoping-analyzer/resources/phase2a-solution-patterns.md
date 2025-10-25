# Phase 2A Solution Patterns: Dynamic Casting for Method-Level Generics

Implementation details for Phase 2A solution using dynamic casting with identity functions.

## Core Pattern

### Identity Function + Dynamic Casting

```kotlin
// For method-level generic:
interface Processor {
    fun <T> process(data: T): T
}

// Generated code:
class FakeProcessorImpl : Processor {
    // Use Any? (universal supertype)
    private var processBehavior: (Any?) -> Any? = { it }  // Identity function

    override fun <T> process(data: T): T {
        @Suppress("UNCHECKED_CAST")
        return processBehavior(data) as T  // Dynamic cast at method level
    }

    internal fun configureProcess(behavior: (Any?) -> Any?) {
        processBehavior = behavior
    }
}
```

---

## Why Any? and Not Any

**Any? is safer**:
```kotlin
// Any? can hold null
private var behavior: (Any?) -> Any? = { it }
fake.process(null)  // Works ✅

// Any cannot hold null
private var behavior: (Any) -> Any = { it }
fake.process(null)  // ClassCastException ❌
```

**Rule**: Always use Any? for maximum compatibility

---

## Identity Function Pattern

### Why { it } is the Safest Default

```kotlin
// Identity preserves input as output
val identity: (Any?) -> Any? = { it }

identity("test")  // Returns "test"
identity(42)      // Returns 42
identity(null)    // Returns null
identity(User())  // Returns User()
```

**Alternatives considered**:
```kotlin
// Null default (loses data)
{ null }

// Exception default (crashes)
{ throw NotImplementedError() }

// Empty/zero default (type-specific, complex)
{ "" }  // Only works for String
```

**Identity is best**: Preserves input, works for all types, no data loss

---

## Code Generation Template

### For Single Type Parameter

```kotlin
// Interface method:
fun <T> process(data: T): T

// Generated:
private var processBehavior: (Any?) -> Any? = { it }

override fun <T> process(data: T): T {
    @Suppress("UNCHECKED_CAST")
    return processBehavior(data) as T
}

internal fun configureProcess(behavior: (Any?) -> Any?) {
    processBehavior = behavior
}

// Configuration DSL:
class FakeProcessorConfig(private val fake: FakeProcessorImpl) {
    fun process(behavior: (Any?) -> Any?) {
        fake.configureProcess(behavior)
    }
}
```

---

### For Multiple Type Parameters

```kotlin
// Interface method:
fun <T, R> transform(input: T, fn: (T) -> R): R

// Generated:
private var transformBehavior: (Any?, (Any?) -> Any?) -> Any? = { _, fn -> fn(null) }

override fun <T, R> transform(input: T, fn: (T) -> R): R {
    @Suppress("UNCHECKED_CAST")
    return transformBehavior(
        input,
        fn as (Any?) -> Any?
    ) as R
}
```

---

### For Generic with Constraints

```kotlin
// Interface method:
fun <T : Comparable<T>> sort(items: List<T>): List<T>

// Generated (constraint not enforced in behavior):
private var sortBehavior: (List<Any?>) -> List<Any?> = { emptyList() }

override fun <T : Comparable<T>> sort(items: List<T>): List<T> {
    @Suppress("UNCHECKED_CAST")
    return sortBehavior(items as List<Any?>) as List<T>
}
```

**Note**: Constraint `T : Comparable<T>` not enforced in behavior property (limitation)

---

## @Suppress Usage

### Why @Suppress("UNCHECKED_CAST") is Safe

**Kotlin's warning**:
```
Warning: Unchecked cast: Any? to T
This cast can never succeed because type T is erased at runtime
```

**Why we can safely suppress**:

1. **Call site is type-safe**:
   ```kotlin
   val result: String = fake.process("test")  // T = String inferred
   ```

2. **Identity function preserves type**:
   ```kotlin
   { it }  // Input type = output type at runtime
   ```

3. **User controls behavior**:
   ```kotlin
   fake.process { data: String -> data.uppercase() }  // User ensures String
   ```

4. **Runtime safety**:
   - If user configures correctly: ✅ Safe
   - If user misconfigures: ❌ ClassCastException (their responsibility)

---

## Configuration Examples

### Simple Identity
```kotlin
val fake = fakeProcessor {
    process { it }  // Identity, safest
}

val result: String = fake.process("test")
assertEquals("test", result)
```

### Type-Specific Logic
```kotlin
val fake = fakeProcessor {
    process { data ->
        when (data) {
            is String -> data.uppercase()
            is Int -> data * 2
            else -> data
        } as Any?
    }
}

assertEquals("HELLO", fake.process("hello"))
assertEquals(20, fake.process(10))
```

### With State
```kotlin
val calls = mutableListOf<Any?>()
val fake = fakeProcessor {
    process { data ->
        calls.add(data)
        data
    }
}

fake.process("test")
assertEquals(listOf("test"), calls)
```

---

## Common Patterns

### Pattern 1: Transform
```kotlin
interface Transformer {
    fun <T, R> map(input: T, fn: (T) -> R): R
}

// Configuration:
val fake = fakeTransformer {
    map { input, fn ->
        @Suppress("UNCHECKED_CAST")
        (fn as (Any?) -> Any?)(input)
    }
}
```

---

### Pattern 2: Filter/Predicate
```kotlin
interface Filter {
    fun <T> select(items: List<T>, predicate: (T) -> Boolean): List<T>
}

// Configuration:
val fake = fakeFilter {
    select { items, predicate ->
        @Suppress("UNCHECKED_CAST")
        items.filter { predicate(it as T) } as List<Any?>
    }
}
```

---

## Testing Strategy

### Test Type Safety at Call Site
```kotlin
@Test
fun `GIVEN processor WHEN calling with String THEN should preserve type`() {
    val fake = fakeProcessor {
        process { it }  // Identity
    }

    val result: String = fake.process("test")  // Type inferred
    assertEquals("test", result)
}
```

### Test Different Types
```kotlin
@Test
fun `GIVEN processor WHEN calling with different types THEN should work`() {
    val fake = fakeProcessor { process { it } }

    assertEquals("test", fake.process("test"))
    assertEquals(42, fake.process(42))
    assertEquals(null, fake.process(null))
}
```

### Test Custom Behavior
```kotlin
@Test
fun `GIVEN processor with custom behavior WHEN calling THEN should apply logic`() {
    val fake = fakeProcessor {
        process { data ->
            when (data) {
                is String -> data.uppercase()
                else -> data
            } as Any?
        }
    }

    assertEquals("HELLO", fake.process("hello"))
}
```

---

## Migration from Phase 1

### Before (Phase 1 - Not Supported)
```kotlin
interface Processor {
    fun <T> process(data: T): T
}

// ERROR: Cannot generate
```

### After (Phase 2A - Supported)
```kotlin
// Generated code compiles ✅
class FakeProcessorImpl : Processor {
    private var processBehavior: (Any?) -> Any? = { it }

    override fun <T> process(data: T): T {
        @Suppress("UNCHECKED_CAST")
        return processBehavior(data) as T
    }
}

// Usage:
val fake = fakeProcessor { process { it } }
val result: String = fake.process("test")  // Type-safe ✅
```

---

## Limitations

### 1. Configuration Type-Erased
```kotlin
val fake = fakeProcessor {
    process { data ->  // data is Any?, not T
        // Must handle Any?
        data
    }
}
```

**Impact**: Developer doesn't get IDE autocomplete for specific type in configuration

---

### 2. Runtime Type Safety
```kotlin
val fake = fakeProcessor {
    process { data ->
        (data as String).uppercase()  // User cast needed
    }
}

fake.process(42)  // ClassCastException! ❌
```

**Mitigation**: Document that user must ensure type correctness

---

### 3. Constraints Not Enforced
```kotlin
fun <T : Comparable<T>> sort(items: List<T>): List<T>

// Constraint T : Comparable<T> not enforced in behavior
// User could configure non-Comparable type
```

---

## Performance

- **Compilation**: ~same as Phase 1 (minor overhead for @Suppress)
- **Runtime**: Slight overhead for cast (negligible)
- **Memory**: No additional allocation

---

## References

- **Kotlin Type System**: kotlinlang.org/docs/generics.html
- **Reified vs Erased**: kotlinlang.org/docs/inline-functions.html#reified-type-parameters
- **Metro Pattern**: metro/compiler/ (for comparison)
