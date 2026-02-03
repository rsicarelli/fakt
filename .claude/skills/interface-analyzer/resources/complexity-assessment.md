# Interface Complexity Assessment Guide

Objective scoring system for interface generation complexity.

## Complexity Scoring System

### Overall Complexity = Method Complexity + Generic Complexity + Type Complexity

**Scale**: LOW (1-3) | MEDIUM (4-6) | HIGH (7-8) | VERY HIGH (9-10)

---

## Method Complexity Scoring

### Score Per Method

**1 point (Simple)**:
- Primitive parameters (String, Int, Boolean, etc.)
- Primitive return types
- No modifiers
- Example: `fun getName(): String`

**2 points (Moderate)**:
- Complex type parameters (User, Data, etc.)
- Nullable types
- Suspend modifier
- Example: `suspend fun getUser(id: String): User?`

**3 points (Complex)**:
- Generic return types (Result<T>, List<T>)
- Function type parameters
- Multiple parameters
- Example: `suspend fun process(data: Data, callback: (Result) -> Unit): Result<Data>`

**4+ points (Very Complex)**:
- Method-level generics
- Complex function types
- Multiple generic constraints
- Example: `fun <T, R> transform(data: T, fn: (T) -> R): R`

### Aggregate Method Complexity

```
Average method complexity:
- 1.0-2.0 → LOW
- 2.1-3.0 → MEDIUM
- 3.1-4.0 → HIGH
- 4.0+    → VERY HIGH
```

---

## Generic Complexity Scoring

### No Generics: 0 points
```kotlin
interface SimpleService {
    fun getData(): Data
}
```

### Interface-Level Generics: +3 points
```kotlin
interface Repository<T> {
    fun save(item: T): T
}
```

**Reason**: Requires Phase 2B or type erasure

### Method-Level Generics: +4 points
```kotlin
interface Processor {
    fun <T> process(data: T): T
}
```

**Reason**: Scoping challenge, requires Phase 2A

### Mixed Generics: +6 points
```kotlin
interface Cache<K, V> {
    fun <R : V> compute(key: K, fn: (K) -> R): R
}
```

**Reason**: Requires both Phase 2A and 2B

### Complex Constraints: +2 points (additional)
```kotlin
interface Bounded<T : Comparable<T>> {  // +2 for constraint
    fun sort(items: List<T>): List<T>
}
```

### Generic Complexity Scale
```
0 points  → No impact
3 points  → MEDIUM impact (Phase 2B)
4 points  → HIGH impact (Phase 2A)
6+ points → VERY HIGH impact (Phase 2A + 2B)
```

---

## Type Complexity Scoring

### Per Unique Type

**0 points (Primitives)**:
- String, Int, Long, Boolean, etc.
- kotlin.* built-ins

**1 point (Simple Custom)**:
- User-defined data classes
- Simple domain objects
- Example: `User`, `Order`, `Product`

**2 points (Generic Standard Library)**:
- List<T>, Set<T>, Map<K,V>
- Result<T>, Pair<A, B>
- Example: `List<User>`, `Result<Data>`

**3 points (Complex)**:
- Nested generics: `Map<String, List<User>>`
- Function types: `(Event) -> Unit`, `suspend (Data) -> Result<T>`

**4+ points (Very Complex)**:
- Higher-order types: `F<A>`
- Complex function types: `(T) -> (R) -> S`

### Aggregate Type Complexity

```
Count unique complex types (2+ points):
- 0-2 types  → LOW
- 3-5 types  → MEDIUM
- 6-8 types  → HIGH
- 9+ types   → VERY HIGH
```

---

## Special Pattern Modifiers

### Suspend Functions: No penalty
**Reason**: Fully supported in Phase 1
```kotlin
suspend fun fetchData(): Data  // Same complexity as non-suspend
```

### Function Types: No penalty
**Reason**: Fully supported in Phase 1
```kotlin
fun onClick(handler: (Event) -> Unit)  // Same as simple method
```

### Nullable Types: No penalty
**Reason**: Fully supported, safe defaults
```kotlin
fun findUser(id: String): User?  // Same complexity as non-null
```

### Collections: +0.5 points
**Reason**: Slightly more complex defaults
```kotlin
fun getUsers(): List<User>  // Minimal added complexity
```

---

## Complexity Calculation Examples

### Example 1: Simple CRUD Service
```kotlin
@Fake
interface UserService {
    fun create(user: User): User                   // 2 points (complex type)
    fun read(id: String): User?                    // 2 points (nullable)
    fun update(user: User): Boolean                // 2 points
    fun delete(id: String): Boolean                // 1 point (primitive)
}
```

**Calculation**:
- Method complexity: (2+2+2+1)/4 = **1.75** (LOW)
- Generic complexity: 0 (no generics) = **0**
- Type complexity: User (1 point), String/Boolean (0) = **1** (LOW)
- **Total**: 1.75 + 0 + 1 = **2.75 (LOW)**

**Overall**: LOW complexity, Phase 1 ✅

---

### Example 2: Async Generic Repository
```kotlin
@Fake
interface Repository<T> {
    suspend fun save(item: T): T                   // 2 points (suspend + generic)
    suspend fun findById(id: String): T?           // 2 points
    suspend fun findAll(): List<T>                 // 3 points (collection + generic)
}
```

**Calculation**:
- Method complexity: (2+2+3)/3 = **2.33** (MEDIUM)
- Generic complexity: Interface-level T = **+3** (MEDIUM)
- Type complexity: List<T> (2 points) = **2** (LOW)
- **Total**: 2.33 + 3 + 2 = **7.33 (HIGH)**

**Overall**: HIGH complexity, Phase 2B needed

---

### Example 3: Method-Level Generic Processor
```kotlin
@Fake
interface DataProcessor {
    fun <T> process(data: T): T                    // 4 points (method generic)
    fun <T, R> transform(input: T, fn: (T) -> R): R // 5 points (2 generics + fn type)
}
```

**Calculation**:
- Method complexity: (4+5)/2 = **4.5** (VERY HIGH)
- Generic complexity: Method-level T, R = **+4** (HIGH)
- Type complexity: Function type (3 points) = **3** (MEDIUM)
- **Total**: 4.5 + 4 + 3 = **11.5 (VERY HIGH)**

**Overall**: VERY HIGH complexity, Phase 2A required

---

## Decision Matrix

| Total Score | Complexity | Phase 1 | Action |
|-------------|------------|---------|--------|
| 1-3 | LOW | ✅ Full support | Generate immediately |
| 4-6 | MEDIUM | ⚠️ Partial (depends) | Check specific features |
| 7-8 | HIGH | ❌ Limited | Wait for Phase 2 OR simplify |
| 9+ | VERY HIGH | ❌ Not supported | Simplify OR wait for Phase 2A+2B |

---

## Automated Complexity Analysis

### Checklist for Quick Assessment

**Run through these questions:**

1. **Any generics?**
   - No → +0 points
   - Interface-level → +3 points
   - Method-level → +4 points
   - Both → +6 points

2. **Average method complexity?**
   - All simple (primitives) → 1.0-1.5 points
   - Mix (some complex types) → 2.0-3.0 points
   - Mostly complex → 3.0-4.0 points
   - Very complex → 4.0+ points

3. **Complex types count?**
   - 0-2 types → +1 point
   - 3-5 types → +2 points
   - 6+ types → +3 points

**Sum the points → Complexity level**

---

## Complexity Reduction Strategies

### High → Medium

**For Interface-Level Generics**:
```kotlin
// Before (HIGH complexity)
@Fake
interface Repository<T> {
    fun save(item: T): T
}

// After (LOW complexity)
@Fake
interface UserRepository {
    fun save(item: User): User
}
```

**Impact**: -3 points (removes generic complexity)

---

### High → Low

**For Method-Level Generics**:
```kotlin
// Before (VERY HIGH complexity)
@Fake
interface Processor {
    fun <T> process(data: T): T
}

// After (LOW complexity)
@Fake
interface Processor {
    fun processString(data: String): String
    fun processInt(data: Int): Int
}
```

**Impact**: -4 points (removes method generics)

---

### Complex Types → Simple

**Flatten nested generics**:
```kotlin
// Before (HIGH type complexity)
fun getData(): Map<String, List<User>>

// After (MEDIUM type complexity)
data class UserGroups(val data: Map<String, List<User>>)
fun getData(): UserGroups
```

**Impact**: Complexity moves to data class (easier to handle)

---

## Complexity Thresholds for Phase Support

### Phase 1 (Current)
- **Supports**: Total score ≤ 6
- **Limits**: No method-level generics, type erasure for interface generics

### Phase 2A (In Progress)
- **Supports**: Total score ≤ 8
- **Adds**: Method-level generic support with dynamic casting

### Phase 2B (Future)
- **Supports**: Total score ≤ 10
- **Adds**: Full generic fake class generation

### Phase 3 (Theoretical)
- **Supports**: Total score > 10
- **Adds**: Higher-order types, complex constraints

---

## Scoring Tool (Conceptual)

```kotlin
data class InterfaceComplexity(
    val methodComplexity: Double,
    val genericComplexity: Int,
    val typeComplexity: Int
) {
    val total: Double = methodComplexity + genericComplexity + typeComplexity

    val level: ComplexityLevel = when {
        total <= 3.0 -> ComplexityLevel.LOW
        total <= 6.0 -> ComplexityLevel.MEDIUM
        total <= 8.0 -> ComplexityLevel.HIGH
        else -> ComplexityLevel.VERY_HIGH
    }

    val phaseSupport: String = when (level) {
        ComplexityLevel.LOW -> "Phase 1 ✅"
        ComplexityLevel.MEDIUM -> "Phase 1 ⚠️ (check specifics)"
        ComplexityLevel.HIGH -> "Phase 2A/2B required"
        ComplexityLevel.VERY_HIGH -> "Phase 2A+2B required"
    }
}
```

---

## Summary

**Quick complexity assessment**:
1. Count generic levels (interface vs method)
2. Average method complexity
3. Count complex types
4. Sum scores
5. Check decision matrix
6. Recommend strategy

**Objective**: Make complexity assessment fast, objective, and actionable!
