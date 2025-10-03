---
allowed-tools: Read, Grep, Glob, Bash(find:*), Bash(./gradlew:*), TodoWrite, Task
argument-hint: [interface_name|all] (optional - specific interface or all generics analysis)
description: Deep analysis of generic type parameter scoping challenges with Phase 2A solutions
model: claude-sonnet-4-20250514
---

# 🔬 Generic Type Scoping Challenge Analyzer

**Comprehensive generic parameter analysis with Phase 2A implementation roadmap**

## 📚 Context Integration

**This command leverages:**
- `.claude/docs/analysis/generic-scoping-analysis.md` - Complete technical deep dive
- `.claude/docs/patterns/complex-generics-strategy.md` - Advanced generic handling patterns
- `.claude/docs/implementation/current-status.md` - Current Phase 1 achievements and Phase 2A planning
- `.claude/docs/validation/type-safety-validation.md` - Type system validation approaches
- Real interface examples with generic challenges for analysis

**🏆 GENERIC SCOPING BASELINE:**
- Phase 1: Method signatures preserve <T> parameters ✓
- Phase 2A Challenge: Class-level vs Method-level type parameter mismatch
- Solution Architecture: Dynamic casting with identity functions
- Implementation Timeline: 2-3 weeks for Phase 2A completion

## Usage
```bash
/analyze-generic-scoping [interface_name]
/analyze-generic-scoping AsyncDataService
/analyze-generic-scoping all
```

## What This Command Does

### 1. **Interface Analysis**
- Scan interfaces for generic type parameter patterns
- Classify class-level vs method-level generics
- Identify scoping constraints and limitations

### 2. **Scoping Challenge Assessment**
- Analyze type parameter accessibility
- Identify class/method scope mismatches
- Evaluate current generation approach limitations

### 3. **Solution Recommendations**
- Provide Phase 2A implementation guidance
- Suggest architectural improvements
- Estimate implementation complexity

### 4. **Evidence Collection**
- Show compilation errors and type mismatches
- Document current generation patterns
- Validate against real interface examples

> **Related Analysis**: [📋 Generic Scoping Analysis](.claude/docs/analysis/generic-scoping-analysis.md)
> **Testing Standard**: [📋 Testing Guidelines](.claude/docs/validation/testing-guidelines.md)

## Analysis Categories

### **1. Class-Level Generic Analysis**
```bash
/analyze-generic-scoping GenericRepository
```

**Interface Example:**
```kotlin
interface GenericRepository<T> {
    fun save(item: T): T
    fun findById(id: String): T?
}
```

**Output:**
```
🔍 ANALYZING GENERIC SCOPING: GenericRepository<T>

📋 Interface Classification:
- Type: Class-level generic interface
- Generic Parameters: T (class-level)
- Methods: 2 (save, findById)
- Method Generics: None

🏗️ Current Generation Approach:
class FakeGenericRepositoryImpl : GenericRepository<Any> {
    private var saveBehavior: (Any) -> Any = { it }
    private var findByIdBehavior: (String) -> Any? = { null }

    override fun save(item: Any): Any = saveBehavior(item)
    override fun findById(id: String): Any? = findByIdBehavior(id)
}

🚨 SCOPING ISSUES IDENTIFIED:
- ❌ Type parameter T becomes Any (type erasure)
- ❌ Loss of compile-time type safety
- ❌ Developer must handle Any casting manually

💡 RECOMMENDED SOLUTION:
- Phase 2B: Generate generic fake class
- Target: FakeGenericRepositoryImpl<T> : GenericRepository<T>
- Benefit: Full type safety restoration
```

### **2. Method-Level Generic Analysis**
```bash
/analyze-generic-scoping AsyncDataService
```

**Interface Example:**
```kotlin
interface AsyncDataService {
    suspend fun <T> processData(data: T): T
    suspend fun <R> transform(input: String): R
}
```

**Output:**
```
🔍 ANALYZING GENERIC SCOPING: AsyncDataService

📋 Interface Classification:
- Type: Method-level generic interface
- Generic Parameters: None (class-level)
- Methods: 2 (processData, transform)
- Method Generics: T, R (method-level)

🏗️ Current Generation Challenge:
class FakeAsyncDataServiceImpl : AsyncDataService {
    // ❌ SCOPING PROBLEM: <T> not accessible at class level
    private var processDataBehavior: suspend (Any) -> Any = { _ -> "" as Any }

    // ✅ METHOD SIGNATURE: <T> in scope here
    override suspend fun <T> processData(data: T): T = processDataBehavior(data)
    //                                              ^^^^^^^^^^^^^^^^
    //                                    TYPE MISMATCH: Any -> Any vs T -> T
}

🚨 CORE SCOPING CHALLENGE:
- ❌ Behavior properties cannot access method-level <T>
- ❌ Type bridge between Any and T unsafe
- ❌ Cannot maintain type safety across scope boundaries

💡 RECOMMENDED SOLUTION (Phase 2A):
class FakeAsyncDataServiceImpl : AsyncDataService {
    // Use Any? with identity function as safe default
    private var processDataBehavior: suspend (Any?) -> Any? = { it }

    override suspend fun <T> processData(data: T): T {
        @Suppress("UNCHECKED_CAST")
        return processDataBehavior(data) as T
    }
}

✅ BENEFITS:
- ✅ Identity function { it } is safest possible default
- ✅ Explicit casting with proper suppressions
- ✅ Minimal architecture changes required
- ✅ Can implement in 1-2 weeks
```

### **3. Mixed Generic Analysis**
```bash
/analyze-generic-scoping CacheService
```

**Interface Example:**
```kotlin
interface CacheService<K, V> {
    fun get(key: K): V?
    fun <R : V> computeIfAbsent(key: K, computer: (K) -> R): R
}
```

**Output:**
```
🔍 ANALYZING GENERIC SCOPING: CacheService<K, V>

📋 Interface Classification:
- Type: Mixed generic interface (class + method level)
- Class-level Generics: K, V
- Method-level Generics: R (with constraint R : V)
- Complexity: HIGH - Requires hybrid approach

🏗️ Current Generation Challenge:
class FakeCacheServiceImpl : CacheService<Any, Any> {
    // Class-level K, V become Any
    private var getBehavior: (Any) -> Any? = { null }

    // Method-level R requires constraint handling
    private var computeIfAbsentBehavior: (Any, (Any) -> Any) -> Any = { _, _ -> TODO() }

    override fun <R : Any> computeIfAbsent(key: Any, computer: (Any) -> Any): Any {
        // Multiple scoping challenges combined
    }
}

🚨 COMPLEX SCOPING ISSUES:
- ❌ Class-level K, V type erasure
- ❌ Method-level R constraint R : V loses meaning when V becomes Any
- ❌ Function type parameters become untyped
- ❌ Triple type safety violation

💡 RECOMMENDED HYBRID SOLUTION:
Phase 2A: Dynamic casting for method-level generics
Phase 2B: Generic class generation for class-level generics
Result: Best of both approaches combined
```

## Implementation Recommendations

### **Phase 2A: Dynamic Casting Solution (Immediate)**
```
🎯 IMPLEMENTATION TIMELINE: 2-3 weeks

Week 1: Core Changes
- Update irTypeToKotlinString() for Any? casting
- Implement identity function defaults
- Add @Suppress("UNCHECKED_CAST") generation

Week 2: Testing & Validation
- Test all method-level generic interfaces
- Validate compilation success rate
- Document casting patterns

Week 3: Polish & Documentation
- Developer experience improvements
- Error message enhancements
- Usage pattern documentation
```

### **Phase 2B: Generic Class Generation (Future)**
```
🎯 IMPLEMENTATION TIMELINE: 2-3 months

Month 1: Analysis System
- Interface classification logic
- Class vs method generic detection
- Generation strategy selection

Month 2: Generic Class Generation
- Generic fake class templates
- Factory function generation
- Type parameter preservation

Month 3: Hybrid Integration
- Combine Phase 2A and 2B approaches
- Seamless developer experience
- Performance optimization
```

## Error Scenarios

### **Scoping Analysis Failures**
```
❌ ERROR: Unable to analyze interface 'NonExistentService'
💡 TIP: Ensure interface exists and has @Fake annotation

❌ WARNING: Complex generic constraints detected
📋 Interface: CacheService<K, V> with method <R : V>
🔧 Recommendation: Phase 2B implementation required for full type safety
```

### **Generation Limitations**
```
🚨 CURRENT LIMITATION: Higher-order type parameters
Example: fun <F<_>> process(wrapper: F<String>): F<Int>
Status: Not supported in Phase 2A
Solution: Requires advanced type system analysis (Phase 3)
```

## Success Metrics

### **Analysis Completeness**
```
📊 SCOPING ANALYSIS RESULTS:

🔍 Interfaces Analyzed: 14/14
✅ Class-level Generics: 6 interfaces
✅ Method-level Generics: 8 interfaces
✅ Mixed Generics: 3 interfaces
⚠️ Complex Constraints: 2 interfaces

🎯 Phase 2A Applicability: 11/14 interfaces (78%)
🔮 Phase 2B Required: 6/14 interfaces (43%)
```

### **Implementation Readiness**
```
📋 PHASE 2A READINESS ASSESSMENT:

✅ Architecture: Ready for dynamic casting approach
✅ Testing: Infrastructure supports validation
✅ Examples: Clear implementation patterns identified
⚠️ Documentation: Developer guidance needs enhancement

Confidence Level: HIGH (ready to implement)
Estimated Success Rate: 95% compilation improvement
```

## Related Commands
- `/debug-ir-generation <interface>` - Debug specific generation issues
- `/check-implementation-status` - Monitor overall progress
- `/validate-metro-alignment` - Check architectural compliance

## Technical References
- **Generic Scoping Analysis**: [📋 Deep Analysis](.claude/docs/analysis/generic-scoping-analysis.md)
- **Implementation Roadmap**: [📋 Phase 2 Plan](.claude/docs/implementation/roadmap.md)
- **Kotlin Type System**: `/kotlin/compiler/ir/tree/src/org/jetbrains/kotlin/ir/types/`

---

**Use this command to understand the generic scoping challenge and plan Phase 2 implementation strategy.**