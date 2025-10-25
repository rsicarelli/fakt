# Interface Analysis Patterns

> **Loaded on-demand** when detailed interface analysis is needed

## Comprehensive Interface Metadata Extraction

### Method Analysis

```kotlin
// Regular methods
fun getUser(id: String): User

Analysis checklist:
- Name: getUser
- Parameters: [(name: id, type: String)]
- Return type: User
- Modifiers: none
- Suspend: no
- Generics: no

// Suspend methods
suspend fun fetchData(): Result<Data>

Analysis checklist:
- Name: fetchData
- Parameters: []
- Return type: Result<Data>
- Modifiers: suspend
- Suspend: yes
- Generics: method-level <Data>

// Generic methods
fun <T> transform(input: T): T

Analysis checklist:
- Name: transform
- Parameters: [(name: input, type: T)]
- Return type: T
- Modifiers: none
- Suspend: no
- Generics: method-level <T> (SCOPED TO METHOD)
```

### Property Analysis

```kotlin
// Val property
val currentUser: User

Analysis:
- Name: currentUser
- Type: User
- Mutable: no (val)
- Nullable: no
- Backing field: likely yes

// Var nullable property
var cache: Map<String, Any>?

Analysis:
- Name: cache
- Type: Map<String, Any>?
- Mutable: yes (var)
- Nullable: yes
- Backing field: likely yes
```

### Generic Type Parameters

```kotlin
// Class-level generics (Phase 2 challenge)
interface Repository<T> {
    fun save(item: T)
    fun findAll(): List<T>
}

Analysis:
- Class-level generic: <T>
- Scoping: T is available across ALL methods
- Current approach: Replace T with Any? in generated code
- Phase 2 target: Preserve type safety

// Method-level generics (Working in Phase 1)
interface Transformer {
    fun <T> process(input: T): T
}

Analysis:
- Method-level generic: <T> scoped to process()
- Current approach: Identity function preservation
- Status: ✅ Working
```

### Annotation Parameters

```kotlin
@Fake(trackCalls = true, generateDsl = false)
interface Service

Extraction:
- trackCalls: Boolean = true
- generateDsl: Boolean = false
```

## IR-Level Analysis Patterns

### Using InterfaceAnalyzer

```kotlin
val analyzer = InterfaceAnalyzer()
val metadata = analyzer.analyze(irClass)

// Extracted metadata includes:
metadata.methods.forEach { method ->
    println("Method: ${method.name}")
    println("  Parameters: ${method.parameters}")
    println("  Return type: ${method.returnType}")
    println("  Is suspend: ${method.isSuspend}")
    println("  Type params: ${method.typeParameters}")
}
```

### Common Patterns to Identify

1. **Simple interfaces** (no generics, all regular methods)
2. **Async interfaces** (suspend functions)
3. **Generic interfaces** (class-level or method-level generics)
4. **Complex interfaces** (mixed: properties + methods + generics + suspend)

## Debugging Checklist

When analyzing an interface for IR generation:

- [ ] Interface name extracted correctly
- [ ] Package name identified
- [ ] All methods counted and analyzed
- [ ] All properties counted and analyzed
- [ ] Generic type parameters identified (class vs method level)
- [ ] Suspend modifiers detected
- [ ] @Fake annotation parameters extracted
- [ ] Cross-module dependencies identified
- [ ] KMP source set verified (commonMain, commonTest, etc.)

## Edge Cases

### Interfaces with Companion Objects

```kotlin
interface Service {
    companion object {
        const val VERSION = "1.0"
    }
    fun execute()
}

⚠️ NOTE: Companion objects are ignored in fake generation
Focus only on interface members
```

### Interfaces with Default Implementations

```kotlin
interface Calculator {
    fun add(a: Int, b: Int): Int = a + b  // Default implementation
    fun multiply(a: Int, b: Int): Int     // Abstract
}

Analysis:
- Default implementations: Can be preserved or overridden
- Abstract methods: Must be implemented in fake
```

### Nested Types

```kotlin
interface Processor {
    data class Config(val timeout: Int)
    fun process(config: Config)
}

Analysis:
- Nested types: Preserve in generated code
- Import handling: Ensure nested types are accessible
```
