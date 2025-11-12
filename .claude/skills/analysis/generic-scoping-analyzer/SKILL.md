---
name: generic-scoping-analyzer
description: Deep analysis of generic type parameter scoping challenges examining class-level vs method-level generics, scoping constraints, Phase 2A solutions with dynamic casting, Phase 2B generic class generation, and implementation roadmaps. Use when analyzing generics, understanding scoping issues, planning Phase 2 implementation, debugging generic errors, or when user mentions "generics", "type parameter", "scoping", "<T>", "generic challenge", "Phase 2A", "Phase 2B", or interface names with generic type parameters.
allowed-tools: [Read, Grep, Glob, Bash]
---

# Generic Type Scoping Challenge Analyzer

Comprehensive generic parameter analysis with Phase 2A/2B implementation roadmap for Fakt's core architectural challenge.

## Core Mission

Analyzes the structural and architectural challenges of generic type parameters in Fakt fake generation:
- Classifies generic patterns (class-level vs method-level vs mixed)
- Explains scoping constraints and limitations
- Provides Phase 2A solution (dynamic casting for method generics)
- Provides Phase 2B solution (generic fake classes)
- Recommends implementation strategy with timeline

**The Core Challenge**: Type parameters declared at method level (<T> in methods) are not accessible at class level (for behavior properties), creating a scoping mismatch.

## Instructions

### 1. Identify Analysis Target

**Extract from conversation:**
- Interface name with generics
- "all" for comprehensive analysis
- Specific generic pattern mentioned

**Look for patterns:**
- "analyze generics in Repository"
- "check scoping for <T>"
- "generic challenge in AsyncService"
- "all generic interfaces"

**If unclear:**
```
Ask: "Which interface would you like me to analyze for generic scoping?"
Options: Specific interface name | All @Fake interfaces | General analysis
```

### 2. Locate and Read Interface Definition

**Find interface:**
```bash
interface_name="Repository"  # from context

# Search for interface
find . -path "*/src/*/kotlin/*" -name "*.kt" -exec grep -l "interface ${interface_name}" {} \;
```

**Read interface:**
```bash
Read ${interface_file}
```

**Extract generic parameters:**
```kotlin
// Look for:
interface Repository<T> { ... }              // Class-level
interface Processor { fun <T> ... }          // Method-level
interface Cache<K, V> { fun <R> ... }        // Mixed
```

### 3. Classify Generic Pattern

**Classification matrix:**

**Pattern A: No Generics**
```kotlin
@Fake
interface SimpleService {
    fun getData(): Data
}
```

**Classification**: No generics
**Complexity**: N/A
**Phase Support**: Phase 1 ✅
**Action**: No generic analysis needed

---

**Pattern B: Interface-Level Generics Only**
```kotlin
@Fake
interface Repository<T> {
    fun save(item: T): T
    fun findById(id: String): T?
}
```

**Classification**: Interface-level generic
**Type Parameters**: T (class-level)
**Scope**: T accessible throughout class
**Methods Using T**: All (save, findById)

**Challenge**: T becomes Any in Phase 1 (type erasure)
**Phase 2B Solution**: Generate generic fake class `FakeRepository<T>`

---

**Pattern C: Method-Level Generics Only**
```kotlin
@Fake
interface Processor {
    fun <T> process(data: T): T
    fun <R> transform(input: String): R
}
```

**Classification**: Method-level generics
**Type Parameters**: T, R (method-level only)
**Scope**: T/R only accessible within their respective methods

**Challenge**: Cannot create behavior property with method-level <T>
```kotlin
// Problem:
class FakeProcessorImpl : Processor {
    private var processBehavior: (???) -> ???  // T not in scope here!

    override fun <T> process(data: T): T = ...  // T in scope here
}
```

**Phase 2A Solution**: Dynamic casting with identity function

---

**Pattern D: Mixed Generics (Both Levels)**
```kotlin
@Fake
interface Cache<K, V> {
    fun get(key: K): V?                              // Uses K, V
    fun <R : V> computeIfAbsent(key: K, fn: (K) -> R): R  // Uses K, V, R
}
```

**Classification**: Mixed (interface + method level)
**Interface Parameters**: K, V
**Method Parameters**: R (with constraint R : V)
**Scope**: K, V accessible throughout; R only in computeIfAbsent

**Challenge**: Most complex - combines both scoping challenges
**Phase 2A + 2B Solution**: Hybrid approach

---

### 4. Analyze Scoping Constraints

**For class-level generics (Pattern B):**

```
🔍 SCOPING ANALYSIS: Repository<T>

Interface Declaration:
interface Repository<T> {
    fun save(item: T): T
}

Generated Code (Phase 1):
class FakeRepositoryImpl : Repository<Any> {  // ❌ T → Any (type erasure)
    private var saveBehavior: (Any) -> Any = { it }

    override fun save(item: Any): Any = saveBehavior(item)
}

Scoping Status:
- T accessible at class level: YES
- T accessible in methods: YES
- T accessible in properties: YES

Problem:
- ❌ Type parameter T becomes Any
- ❌ Lost compile-time type safety
- ❌ Runtime casting required

Root Cause:
- Cannot generate: class FakeRepositoryImpl<T> in Phase 1
- Lacks IrTypeSubstitutor integration
- Generic class generation not implemented
```

---

**For method-level generics (Pattern C):**

```
🔍 SCOPING ANALYSIS: Processor (Method-Level)

Interface Declaration:
interface Processor {
    fun <T> process(data: T): T
}

Attempted Generation (Phase 1):
class FakeProcessorImpl : Processor {
    // ❌ SCOPING PROBLEM: <T> not accessible here
    private var processBehavior: (T) -> T = { it }  // ERROR: T unresolved

    // ✅ <T> in scope here
    override fun <T> process(data: T): T = processBehavior(data)
}

Scoping Status:
- T accessible at class level: NO ❌
- T accessible in method: YES ✅
- T accessible in properties: NO ❌

Problem:
- ❌ Behavior property cannot use method-level T
- ❌ Type bridge needed between class and method scope
- ❌ Cannot maintain type safety across boundary

Root Cause:
- Method type parameters are local to method
- Class-level code cannot reference them
- Fundamental Kotlin/JVM limitation
```

---

**For mixed generics (Pattern D):**

```
🔍 SCOPING ANALYSIS: Cache<K, V> with Method-Level R

Interface Declaration:
interface Cache<K, V> {
    fun get(key: K): V?
    fun <R : V> computeIfAbsent(key: K, fn: (K) -> R): R
}

Complex Scoping:
- K, V: Accessible at class level ✅
- R: Only accessible in computeIfAbsent method ❌
- Constraint: R : V (R must extend V)

Problems:
1. K, V type erasure (Phase 1)
2. R scoping issue (Phase 2A needed)
3. Constraint R : V not fully preserved
4. Function type (K) -> R adds complexity

Requires: Both Phase 2A AND Phase 2B
```

---

### 5. Explain Phase 2A Solution (Method-Level Generics)

**Strategy: Dynamic Casting with Identity Function**

**Problem Recap:**
```kotlin
interface Processor {
    fun <T> process(data: T): T
}

// Cannot generate:
private var processBehavior: (T) -> T  // T not in scope!
```

**Phase 2A Solution:**
```kotlin
class FakeProcessorImpl : Processor {
    // Use Any? with identity function as safe default
    private var processBehavior: (Any?) -> Any? = { it }

    override fun <T> process(data: T): T {
        @Suppress("UNCHECKED_CAST")
        return processBehavior(data) as T  // Dynamic cast
    }

    internal fun configureProcess(behavior: (Any?) -> Any?) {
        processBehavior = behavior
    }
}
```

**Why This Works:**

1. **Identity Function Default**: `{ it }` is safest possible default
   - Preserves input as output
   - Works for any type
   - No data transformation

2. **Any? Type**: Universal supertype
   - All types are subtypes of Any?
   - Can hold any value
   - Nullable covers null case

3. **Dynamic Cast**: `as T` at method level
   - T is in scope in method
   - Runtime cast (safe if used correctly)
   - @Suppress annotation acknowledges trade-off

**Type Safety:**
- Configuration: ⚠️ Type-erased (Any?)
- Call site: ✅ Type-safe (T inferred)
- Runtime: ✅ Safe (correct usage assumed)

**Usage Example:**
```kotlin
val fake = fakeProcessor {
    process { data ->  // data is Any?
        // Handle as Any?, return Any?
        data  // Identity
    }
}

// Call site is type-safe
val result: String = fake.process("test")  // T = String inferred
assertEquals("test", result)
```

---

### 6. Explain Phase 2B Solution (Interface-Level Generics)

**Strategy: Generic Fake Class Generation**

**Problem Recap:**
```kotlin
interface Repository<T> {
    fun save(item: T): T
}

// Phase 1 generates:
class FakeRepositoryImpl : Repository<Any> {  // Type erasure!
    private var saveBehavior: (Any) -> Any = { it }
}
```

**Phase 2B Solution:**
```kotlin
// Generate generic fake class
class FakeRepository<T> : Repository<T> {  // Generic parameter!
    private var saveBehavior: (T) -> T = { it }  // T in scope ✅

    override fun save(item: T): T = saveBehavior(item)

    internal fun configureSave(behavior: (T) -> T) {
        saveBehavior = behavior
    }
}

// Generic factory function
fun <T> fakeRepository(configure: FakeRepositoryConfig<T>.() -> Unit = {}): Repository<T> {
    return FakeRepository<T>().apply {
        FakeRepositoryConfig(this).configure()
    }
}

// Generic configuration DSL
class FakeRepositoryConfig<T>(private val fake: FakeRepository<T>) {
    fun save(behavior: (T) -> T) {
        fake.configureSave(behavior)
    }
}
```

**Why This Works:**

1. **Generic Class**: `class FakeRepository<T>`
   - T available throughout class
   - Type-safe behavior properties
   - No type erasure

2. **Type Parameter Propagation**:
   - Factory: `<T> fakeRepository`
   - Config: `FakeRepositoryConfig<T>`
   - Impl: `FakeRepository<T>`

3. **Full Type Safety**:
   - Configuration: ✅ Type-safe (T)
   - Properties: ✅ Type-safe (T)
   - Methods: ✅ Type-safe (T)

**Usage Example:**
```kotlin
val fake = fakeRepository<User> {  // Type parameter
    save { user ->  // user is User, not Any!
        user.copy(id = "saved")
    }
}

val user = User(id = "", name = "Test")
val saved: User = fake.save(user)  // Full type safety ✅
assertEquals("saved", saved.id)
```

**Implementation Requirements:**
- IrTypeSubstitutor usage
- Generic class IR generation
- Type parameter preservation
- Generic factory pattern

**Timeline**: 2-3 months (complex)

---

### 7. Hybrid Solution (Phase 2A + 2B)

**For mixed generics:**

```kotlin
@Fake
interface Cache<K, V> {
    fun get(key: K): V?
    fun <R : V> computeIfAbsent(key: K, fn: (K) -> R): R
}
```

**Combined approach:**

**Phase 2B handles K, V**:
```kotlin
class FakeCache<K, V> : Cache<K, V> {  // K, V in scope ✅
    private var getBehavior: (K) -> V? = { null }

    override fun get(key: K): V? = getBehavior(key)  // Type-safe!
}
```

**Phase 2A handles method-level R**:
```kotlin
class FakeCache<K, V> : Cache<K, V> {
    // ...

    private var computeIfAbsentBehavior: (K, (K) -> Any?) -> Any? = { k, _ -> null }

    override fun <R : V> computeIfAbsent(key: K, fn: (K) -> R): R {
        @Suppress("UNCHECKED_CAST")
        return computeIfAbsentBehavior(key, fn as (K) -> Any?) as R
    }
}
```

**Result**: Best of both phases combined

---

### 8. Generate Comprehensive Analysis Report

```
═══════════════════════════════════════════════════
🔬 GENERIC SCOPING ANALYSIS: ${INTERFACE_NAME}
═══════════════════════════════════════════════════

📋 INTERFACE CLASSIFICATION:

Pattern: ${PATTERN_TYPE}
- Class-level generics: ${list} | none
- Method-level generics: ${list} | none
- Complexity: ${LOW|MEDIUM|HIGH|VERY HIGH}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🎯 SCOPING ANALYSIS:

${For each type parameter}:
- Name: ${T|R|K|V}
- Level: ${class|method}
- Scope: ${description}
- Accessible at class level: ${yes|no}
- Accessible in methods: ${yes|no}

Scoping Constraints:
- ${constraint_1}
- ${constraint_2}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🏗️ GENERATION CHALLENGES:

Phase 1 Status: ${status}
- ${challenge_1}
- ${challenge_2}

Current Limitations:
- ${limitation_1}
- ${limitation_2}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

💡 PHASE 2A SOLUTION (Method-Level):
${if_applicable}

Strategy: Dynamic casting with identity function
Timeline: 2-3 weeks
Type Safety: Partial (call-site safe, config type-erased)

Generated Pattern:
private var ${method}Behavior: (Any?) -> Any? = { it }

override fun <T> ${method}(data: T): T {
    @Suppress("UNCHECKED_CAST")
    return ${method}Behavior(data) as T
}

Benefits:
✅ Supports method-level generics
✅ Identity function safest default
✅ Type-safe at call site

Trade-offs:
⚠️ Configuration type-erased (Any?)
⚠️ Runtime cast (safe if used correctly)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

💡 PHASE 2B SOLUTION (Interface-Level):
${if_applicable}

Strategy: Generic fake class generation
Timeline: 2-3 months
Type Safety: Full (100%)

Generated Pattern:
class Fake${INTERFACE}<T> : ${INTERFACE}<T> {
    private var ${method}Behavior: (T) -> T = { it }

    override fun ${method}(item: T): T = ${method}Behavior(item)
}

fun <T> fake${INTERFACE}(configure: ...) : ${INTERFACE}<T> {
    return Fake${INTERFACE}<T>().apply { ... }
}

Benefits:
✅ Full type safety
✅ Generic reuse (one fake for all T)
✅ No casting needed

Implementation:
- IrTypeSubstitutor integration
- Generic class IR generation
- Type parameter preservation

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🎯 RECOMMENDED STRATEGY:

Complexity: ${complexity_score}
Best Approach: ${strategy}
Timeline: ${timeline}

Immediate Actions:
1. ${action_1}
2. ${action_2}
3. ${action_3}

Long-term Plan:
- ${plan_1}
- ${plan_2}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📚 REFERENCES:
- Technical Reference: .claude/docs/implementation/generics/technical-reference.md
- Generic Strategies: .claude/docs/implementation/generics/complex-generics-strategy.md
- Implementation Guide: .claude/docs/implementation/generics/

═══════════════════════════════════════════════════
```

### 9. Provide Implementation Guidance

**Based on analysis:**

**If no generics:**
```
✅ No generic scoping issues
→ Use Phase 1 standard generation
→ No special handling needed
```

**If interface-level generics only:**
```
🎯 STRATEGY: Choose Your Path

Option A: Immediate (Phase 1)
- Use concrete types: UserRepository, OrderRepository
- Timeline: Now
- Type Safety: 100% (for each concrete type)

Option B: Wait (Phase 2B)
- Generic fake class: FakeRepository<T>
- Timeline: 2-3 months
- Type Safety: 100% (generic reuse)

Option C: Accept Trade-off (Phase 1)
- Use FakeRepositoryImpl : Repository<Any>
- Timeline: Now
- Type Safety: Reduced (manual casting)

Recommendation: ${based_on_project_needs}
```

**If method-level generics:**
```
🎯 STRATEGY: Phase 2A Required

Status: In progress
Timeline: 2-3 weeks
Type Safety: Partial (call-site safe)

Workarounds (Phase 1):
1. Refactor to interface-level generics
2. Use concrete method names (processString, processInt)
3. Wait for Phase 2A

Recommendation: Wait for Phase 2A OR refactor
```

**If mixed generics:**
```
🎯 STRATEGY: Hybrid Solution

Required: Phase 2A + Phase 2B
Timeline: 3-4 months (full implementation)
Type Safety: Full (when complete)

Interim Options:
1. Simplify to single level (interface OR method)
2. Use concrete types throughout
3. Accept partial implementation (Phase 2A first)

Recommendation: Simplify interface OR plan phased rollout
```

## Supporting Files

Progressive disclosure for generic scoping:

- **`resources/scoping-analysis-guide.md`** - Deep technical dive into scoping (loaded on-demand)
- **`resources/phase2a-solution-patterns.md`** - Dynamic casting implementation details (loaded on-demand)
- **`resources/phase2b-planning.md`** - Generic class generation architecture (loaded on-demand)

## Related Skills

This Skill composes with:
- **`interface-analyzer`** - Identify generic patterns in interfaces
- **`compilation-error-analyzer`** - Debug generic-related compilation errors
- **`kotlin-api-consultant`** - Validate IrTypeParameter and IrTypeSubstitutor APIs
- **`implementation-tracker`** - Track Phase 2A/2B progress

## Generic Pattern Examples

### Simple: Interface-Level
```kotlin
interface Repository<T> {
    fun save(item: T): T
}
```
**Complexity**: MEDIUM (Phase 2B)

### Complex: Method-Level
```kotlin
interface Processor {
    fun <T> process(data: T): T
}
```
**Complexity**: HIGH (Phase 2A)

### Very Complex: Mixed
```kotlin
interface Cache<K, V> {
    fun <R : V> compute(key: K): R
}
```
**Complexity**: VERY HIGH (Phase 2A + 2B)

## Best Practices

1. **Analyze early** - Know scoping challenges before implementing
2. **Choose appropriate phase** - Match solution to timeline
3. **Prefer interface-level** - Easier than method-level
4. **Simplify when possible** - Concrete types avoid scoping issues
5. **Plan for phases** - Understand migration path

## Quick Scoping Check

```bash
# Count generic parameters
grep -E "<.*>" ${interface_file}

# Classify level
grep "interface.*<" ${interface_file}    # Interface-level
grep "fun.*<" ${interface_file}          # Method-level
```

## Performance Notes

- Interface read: ~1-2 seconds
- Pattern classification: ~2-3 seconds
- Scoping analysis: ~5-10 seconds
- Solution generation: ~5-10 seconds
- Total: ~15-30 seconds per interface

Comprehensive analysis for critical architectural decisions!
