# Phase-Specific Compilation Errors

Errors organized by Fakt implementation phase with phase-aware solutions.

## Phase 1: MAP Foundation (Current - Complete)

### What Phase 1 Supports ✅
- Basic interface fake generation
- Suspend functions
- Function type parameters
- Nullable types
- Collections (List, Set, Map)
- Smart defaults for all types
- Zero TODO compilation blockers

### Known Phase 1 Limitations ⚠️

**Limitation 1: Interface-Level Generic Type Erasure**
```kotlin
@Fake
interface Repository<T> {  // T becomes Any in generated code
    fun save(item: T): T
}
```

**Error**:
```
Warning: Type parameter T erased to Any
Generated: class FakeRepositoryImpl : Repository<Any>
```

**Status**: EXPECTED in Phase 1
**Solution**: Use concrete types OR wait for Phase 2B
**Impact**: Reduced type safety

---

**Limitation 2: Method-Level Generics Not Supported**
```kotlin
@Fake
interface Processor {
    fun <T> process(data: T): T  // Scoping challenge
}
```

**Error**:
```
Error: Cannot create behavior property for method-level type parameter T
Reason: T not accessible at class level
```

**Status**: Phase 2A feature
**Solution**:
1. Wait for Phase 2A (2-3 weeks)
2. OR refactor to interface-level generics
3. OR use concrete methods

---

**Limitation 3: Complex Generic Constraints**
```kotlin
@Fake
interface Bounded<T : Comparable<T>> {
    fun sort(items: List<T>): List<T>
}
```

**Error**:
```
Warning: Generic constraint 'T : Comparable<T>' not fully preserved
Generated type: T erased to Any
```

**Status**: Partial support
**Impact**: Constraint not enforced at generated code level

---

## Phase 2A: Method-Level Generics (In Progress)

### What Phase 2A Will Add ✅
- Method-level generic support
- Dynamic casting with @Suppress
- Identity function pattern
- Function types with generics

### Phase 2A Approach

**Before (Phase 1 - Error)**:
```kotlin
@Fake
interface Processor {
    fun <T> process(data: T): T
}

// Cannot generate - T not in scope
```

**After (Phase 2A - Works)**:
```kotlin
class FakeProcessorImpl : Processor {
    private var processBehavior: (Any?) -> Any? = { it }  // Identity

    override fun <T> process(data: T): T {
        @Suppress("UNCHECKED_CAST")
        return processBehavior(data) as T  // Dynamic cast
    }
}
```

### Expected Phase 2A Warnings ⚠️

**Warning: Unchecked Cast**
```
Warning: Unchecked cast: Any? to T
File: build/generated/fakt/.../FakeProcessorImpl.kt
```

**Status**: EXPECTED in Phase 2A
**Reason**: Dynamic casting used for method generics
**Safety**: @Suppress("UNCHECKED_CAST") added
**Impact**: Runtime type safety (safe if used correctly)

---

**Warning: Type Erasure in Configuration**
```
Usage:
val fake = fakeProcessor {
    process { data ->  // data is Any?, not T
        // Must handle Any? here
        data
    }
}
```

**Status**: EXPECTED (type erasure at configuration)
**Workaround**: Type-safe at call site
```kotlin
val result: String = fake.process("test")  // T inferred as String
```

---

## Phase 2B: Generic Fake Classes (Future)

### What Phase 2B Will Add ✅
- Generic fake class generation
- Full type safety for interface-level generics
- Type-safe configuration DSL
- No type erasure

### Phase 2B Approach

**Before (Phase 1 - Type Erasure)**:
```kotlin
class FakeRepositoryImpl : Repository<Any> {  // Any!
    private var saveBehavior: (Any) -> Any = { it }
}
```

**After (Phase 2B - Type Safe)**:
```kotlin
class FakeRepository<T> : Repository<T> {  // Generic class!
    private var saveBehavior: (T) -> T = { it }

    override fun save(item: T): T = saveBehavior(item)
}

fun <T> fakeRepository(configure: FakeRepositoryConfig<T>.() -> Unit = {}): Repository<T> {
    return FakeRepository<T>().apply {
        FakeRepositoryConfig(this).configure()
    }
}
```

### Expected Phase 2B Changes ✅

**Type-Safe Usage**:
```kotlin
val fake = fakeRepository<User> {  // Type parameter
    save { user -> user.copy(saved = true) }  // user is User, not Any!
}

val user = User(...)
val saved: User = fake.save(user)  // Full type safety
```

**No More Type Erasure Warnings**:
```
Before Phase 2B: Warning: T erased to Any
After Phase 2B: ✅ No warnings
```

---

## Phase 3: Advanced Generics (Theoretical)

### What Phase 3 Might Add 🔮
- Higher-order type parameters
- Complex type constraints
- Type constructor generics
- Category theory patterns

### Phase 3 Challenges

**Higher-Order Types**:
```kotlin
interface Wrapper<F<_>> {  // F is a type constructor
    fun <A> wrap(value: A): F<A>
}
```

**Status**: Not planned for near term
**Complexity**: EXTREME
**Recommendation**: Avoid these patterns or use concrete wrapper types

---

## Error Migration Across Phases

### Scenario: Repository<T> Over Time

**Phase 1 (Current)**:
```
Warning: Repository<T> → Repository<Any>
Status: Type erasure expected
Action: Use concrete types OR accept erasure
```

**Phase 2B (Future)**:
```
✅ Repository<T> → FakeRepository<T>
Status: Full type safety
Action: Migrate to generic fake class
```

**Migration Path**:
1. Phase 1: Use concrete types (UserRepository, OrderRepository)
2. Phase 2B: Migrate to FakeRepository<T>
3. Refactor: Remove concrete types, use generic

---

## Phase Detection in Errors

### How to Tell Which Phase an Error Relates To

**Phase 1 Error Indicators**:
- "Type erasure"
- "T becomes Any"
- "Zero TODO blockers" (achievement)
- "Smart defaults working"

**Phase 2A Error Indicators**:
- "Method-level generic"
- "Scoping challenge"
- "Dynamic casting"
- "@Suppress("UNCHECKED_CAST")"

**Phase 2B Error Indicators**:
- "Interface-level generic"
- "Generic fake class"
- "IrTypeSubstitutor"
- "Factory function with type parameter"

---

## Roadmap-Aware Error Solutions

### Error: Generic Type Not Supported

**Step 1: Identify Pattern**
```kotlin
// Is it interface-level?
interface Repo<T> { ... }  // Phase 2B

// Or method-level?
interface Proc { fun <T> ... }  // Phase 2A

// Or mixed?
interface Cache<K, V> { fun <R : V> ... }  // Phase 2A + 2B
```

**Step 2: Check Phase Status**
- Phase 1: ✅ Complete
- Phase 2A: 🚧 In progress (2-3 weeks)
- Phase 2B: ⏳ Future (2-3 months)

**Step 3: Choose Timeline**
- Need now? → Use concrete types (Phase 1)
- Can wait 2-3 weeks? → Wait for Phase 2A
- Can wait 2-3 months? → Wait for Phase 2B
- Very complex? → Simplify interface

---

## Phase Transition Warnings

### Moving from Phase 1 → Phase 2A

**Expect Breaking Changes**:
```
Before (Phase 1 - Not supported):
Error: Method-level generics not supported

After (Phase 2A - Supported with warnings):
Warning: Unchecked cast in generated code
```

**Migration**:
- Generated code changes
- @Suppress annotations added
- Configuration type changes (T → Any?)
- Call site remains type-safe

---

### Moving from Phase 1 → Phase 2B

**Expect API Changes**:
```
Before (Phase 1):
val fake: Repository<User> = fakeRepository()  // Type erasure

After (Phase 2B):
val fake: Repository<User> = fakeRepository<User> { ... }  // Generic parameter
```

**Migration**:
- Add type parameters to factory calls
- Update configuration lambdas (Any → T)
- Remove @Suppress if present
- Full type safety restored

---

## Current Status Quick Reference

| Feature | Phase 1 | Phase 2A | Phase 2B |
|---------|---------|----------|----------|
| Simple interfaces | ✅ | ✅ | ✅ |
| Suspend functions | ✅ | ✅ | ✅ |
| Function types | ✅ | ✅ | ✅ |
| Nullable types | ✅ | ✅ | ✅ |
| Collections | ✅ | ✅ | ✅ |
| Interface generics | ⚠️ Erasure | ⚠️ Erasure | ✅ Type-safe |
| Method generics | ❌ | ✅ Dynamic cast | ✅ Type-safe |
| Mixed generics | ❌ | ⚠️ Partial | ✅ Full |

---

## Timeline Expectations

### Phase 1 (Complete)
- Status: ✅ Production ready
- Timeline: Available now
- Success Rate: 85% compilation
- Known Issues: Generic type erasure

### Phase 2A (In Progress)
- Status: 🚧 Active development
- Timeline: 2-3 weeks
- Target: 95% compilation
- Focus: Method-level generics

### Phase 2B (Planned)
- Status: ⏳ Planning/design
- Timeline: 2-3 months
- Target: 98% compilation
- Focus: Interface-level generics

---

## How to Use This Guide

**When you encounter an error:**

1. **Identify the pattern** (interface-level generic? method-level?)
2. **Check which phase supports it** (table above)
3. **Choose your path**:
   - Work with current phase limitations
   - Wait for appropriate phase
   - Simplify to reduce complexity
4. **Apply phase-appropriate solution**

**Remember**: Each phase builds on previous phases. Phase 2A doesn't break Phase 1 code!
