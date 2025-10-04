---
allowed-tools: Read, Grep, Glob, Bash(find:*), Bash(./gradlew:*), Bash(tree:*), TodoWrite, Task, WebFetch
argument-hint: [strategy] (optional - erasure|substitution|mvp|full)
description: Deep research and strategic planning for Kotlin IR generic type implementation with IrTypeSubstitutor
model: claude-opus-4-20250514
---

# 🎯 Generic Implementation Strategic Planner

**Comprehensive research, analysis, and implementation roadmap for Kotlin K2 IR generic type handling**

## 📚 Context Integration

**This command leverages:**
- Gemini Deep Search documentation on Kotlin IR generics
- Metro DI framework patterns for generic handling
- kotlin-serialization and Compose compiler case studies
- Current Fakt architecture and limitations analysis
- Production-ready implementation blueprints

**🏆 RESEARCH BASELINE:**
- IrTypeSubstitutor: Core API for type parameter substitution
- IrTypeParameterRemapper: For cross-scope type parameter handling
- Layered strategy: Method-level → Class-level generics
- Multi-stage validation: Compilation → Reflection → Type-safety

## Usage
```bash
/plan-generic-implementation
/plan-generic-implementation erasure
/plan-generic-implementation substitution
/plan-generic-implementation mvp
/plan-generic-implementation full
```

## What This Command Does

### 1. **Deep Architecture Analysis**
- Analyze current Fakt generic infrastructure
- Map existing components to IR substitution patterns
- Identify integration points for IrTypeSubstitutor
- Assess GenericPatternAnalyzer readiness

### 2. **Strategy Evaluation**
- Compare Type Erasure vs Full Substitution approaches
- Evaluate implementation complexity vs type safety
- Cost-benefit analysis for each strategy
- MVP vs production-ready timelines

### 3. **Implementation Roadmap**
- Sprint-based implementation plan
- File-by-file modification strategy
- Test-driven development approach
- Incremental deployment with validation gates

### 4. **Risk Assessment & Edge Cases**
- Identify critical edge cases (recursive generics, star projections)
- Document known Kotlin IR API limitations
- Plan error recovery strategies
- Performance profiling requirements

> **Related Research**: Kotlin IR Plugin Generic Type Handling (Gemini Deep Search)
> **Testing Standard**: [Testing Guidelines](../docs/validation/testing-guidelines.md)

## Strategy Options

### **1. Type Erasure MVP** (2-3 days)
```bash
/plan-generic-implementation erasure
```

**Approach:**
```kotlin
// Input
interface Repository<T> { fun save(item: T): T }

// Generated (Type Erasure)
class FakeRepositoryImpl : Repository<Any> {
    private var saveBehavior: (Any) -> Any = { it }
    override fun save(item: Any): Any = saveBehavior(item)
}

// Factory
fun fakeRepository(configure: FakeRepositoryConfig.() -> Unit = {}): Repository<Any>
```

**Pros:**
- ✅ Minimal code changes (remove generic filter)
- ✅ Fast implementation (2-3 days)
- ✅ Works with existing infrastructure
- ✅ Immediate value for simple cases

**Cons:**
- ❌ Type safety lost at compile time
- ❌ Developer must cast manually
- ❌ Not production-quality experience

**Implementation Steps:**
1. Remove generic filter in `UnifiedFaktIrGenerationExtension.kt:189`
2. Update `TypeResolver.kt` to use `Any` for type parameters
3. Test with `Repository<T>` interface
4. Validate compilation success

**Files to Modify:**
- `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/UnifiedFaktIrGenerationExtension.kt`
- `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/types/TypeResolver.kt`
- `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/codegen/ImplementationGenerator.kt`

---

### **2. Smart Defaults Strategy** (3-5 days)
```bash
/plan-generic-implementation mvp
```

**Approach:**
```kotlin
// Input
interface NumberService<T : Number> { fun compute(): T }

// Generated (Smart Defaults)
class FakeNumberServiceImpl : NumberService<Any> {
    // Smart default based on constraint
    private var computeBehavior: () -> Any = { 0 } // Number → 0
    override fun compute(): Any = computeBehavior()
}
```

**Pros:**
- ✅ Better defaults than random values
- ✅ Uses constraint information
- ✅ Reasonable developer experience
- ✅ Low implementation risk

**Cons:**
- ❌ Still uses type erasure
- ❌ Generic type info lost
- ❌ Limited to simple constraints

**Implementation Steps:**
1. Implement `GenericConstraintAnalyzer`
2. Create `SmartDefaultProvider` using bounds
3. Integrate with existing `TypeResolver`
4. Test constraint-based scenarios

**Constraint Mapping:**
```kotlin
<T : Number> → 0
<T : CharSequence> → ""
<T : List<*>> → emptyList()
<T : Map<*, *>> → emptyMap()
<T : Any> → TODO("Implement")
```

---

### **3. IR Substitution Strategy** (1-2 weeks)
```bash
/plan-generic-implementation substitution
```

**Approach:**
```kotlin
// Input
interface Repository<T> { fun save(item: T): T }

// Generated (Full Substitution)
class FakeRepositoryImpl<T> : Repository<T> {
    private var saveBehavior: (T) -> T = { it }
    override fun save(item: T): T = saveBehavior(item)
}

// Factory with reified type
inline fun <reified T> fakeRepository(
    configure: FakeRepositoryConfig<T>.() -> Unit = {}
): Repository<T> = FakeRepositoryImpl<T>().apply {
    FakeRepositoryConfig(this).configure()
}
```

**Core Implementation:**
```kotlin
class GenericIrSubstitutor(private val pluginContext: IrPluginContext) {

    fun createSubstitutionMap(
        originalInterface: IrClass,
        superType: IrSimpleType
    ): Map<IrTypeParameterSymbol, IrTypeArgument> {
        return originalInterface.typeParameters
            .zip(superType.arguments)
            .associate { (param, arg) -> param.symbol to arg }
    }

    fun substituteFunction(
        originalFunction: IrSimpleFunction,
        classLevelSubstitutor: IrTypeSubstitutor
    ): IrSimpleFunction {
        val irFactory = pluginContext.irFactory

        // 1. Create new function
        val newFunction = irFactory.createSimpleFunction(
            returnType = classLevelSubstitutor.substitute(originalFunction.returnType),
            name = originalFunction.name,
            // ... other properties
        )

        // 2. Handle method-level generics
        val newTypeParameters = originalFunction.typeParameters.map { oldTp ->
            irFactory.createTypeParameter(
                name = oldTp.name,
                variance = oldTp.variance,
                index = oldTp.index,
                isReified = oldTp.isReified
            ).also { newTp ->
                newTp.parent = newFunction
                // Apply class-level substitution to constraints
                newTp.superTypes = oldTp.superTypes.map {
                    classLevelSubstitutor.substitute(it)
                }
            }
        }
        newFunction.typeParameters = newTypeParameters

        // 3. Create remapper for method-level type parameters
        val remapper = IrTypeParameterRemapper(
            originalFunction.typeParameters.zip(newTypeParameters).toMap()
        )

        // 4. Apply both substitution and remapping to value parameters
        newFunction.valueParameters = originalFunction.valueParameters.map { oldVp ->
            val substitutedType = classLevelSubstitutor.substitute(oldVp.type)
            val remappedType = remapper.remapType(substitutedType)

            irFactory.createValueParameter(
                name = oldVp.name,
                type = remappedType,
                index = oldVp.index
            ).also { it.parent = newFunction }
        }

        return newFunction
    }
}
```

**Pros:**
- ✅ Full type safety preserved
- ✅ Production-quality experience
- ✅ Follows Kotlin IR best practices
- ✅ Aligns with Metro patterns

**Cons:**
- ❌ Complex implementation
- ❌ Higher testing burden
- ❌ More edge cases to handle
- ❌ Longer development time

**Implementation Steps:**

**Week 1: Core Infrastructure**
1. Create `GenericIrSubstitutor.kt`
2. Implement substitution map builder
3. Test basic `IrTypeSubstitutor` usage
4. Validate with simple `Repository<T>`

**Week 2: Method-Level Generics**
1. Implement `IrTypeParameterRemapper` integration
2. Handle mixed class + method generics
3. Test `Processor<T> { fun <R> transform() }`
4. Validate constraint propagation

**Week 3: Integration & Polish**
1. Integrate with `UnifiedFaktIrGenerationExtension`
2. Update `ImplementationGenerator` templates
3. Handle edge cases (recursive, star projection)
4. Comprehensive test matrix

**Files to Create:**
- `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/GenericIrSubstitutor.kt`
- `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/TypeParameterScope.kt`

**Files to Modify:**
- `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/UnifiedFaktIrGenerationExtension.kt`
- `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/codegen/ImplementationGenerator.kt`
- `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/codegen/FactoryGenerator.kt`
- `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/codegen/ConfigurationDslGenerator.kt`

---

### **4. Full Production Strategy** (2-3 weeks)
```bash
/plan-generic-implementation full
```

**Complete Feature Set:**
- ✅ Class-level generics (`<T, K, V>`)
- ✅ Method-level generics (`fun <R> transform()`)
- ✅ Generic constraints (`<T : Number>`)
- ✅ Variance (`in/out`)
- ✅ Nested generics (`List<Map<K, V>>`)
- ✅ Star projections (`List<*>`)
- ✅ Recursive generics (`Node<T : Node<T>>`)

**Architecture:**
```
┌─────────────────────────────────────────────────────────┐
│  PHASE 1: Analysis (EXISTING - GenericPatternAnalyzer) │
├─────────────────────────────────────────────────────────┤
│  • Detect ClassLevelGenerics<T>                         │
│  • Detect MethodLevelGenerics<R>                        │
│  • Extract constraints & variance                       │
│  • Identify transformation patterns                     │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  PHASE 2: IR Substitution (NEW - GenericIrSubstitutor) │
├─────────────────────────────────────────────────────────┤
│  • Build substitution map from supertype                │
│  • Create IrTypeSubstitutor for class-level             │
│  • Create IrTypeParameterRemapper for method-level      │
│  • Apply layered substitution strategy                  │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│  PHASE 3: Code Generation (ENHANCED)                    │
├─────────────────────────────────────────────────────────┤
│  • Generate generic fake classes                        │
│  • Preserve type parameters in signatures               │
│  • Create reified factory functions                     │
│  • Build type-safe configuration DSL                    │
└─────────────────────────────────────────────────────────┘
```

**Testing Strategy (kotlin-compile-testing):**

```kotlin
class GenericFakeGenerationTest {

    @Test
    fun `GIVEN interface with class-level generics WHEN generating fake THEN should compile successfully`() {
        val compilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin("Repository.kt", """
                    package test
                    @Fake
                    interface Repository<T> {
                        fun findById(id: String): T?
                        fun save(item: T): T
                    }
                """)
            )
            compilerPlugins = listOf(FaktCompilerPluginRegistrar())
            inheritClassPath = true
        }

        val result = compilation.compile()
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        // Stage 2: Structural validation
        val fakeClass = result.classLoader.loadClass("test.FakeRepositoryImpl")
        assertTrue(fakeClass.typeParameters.isNotEmpty())

        val findByIdMethod = fakeClass.getMethod("findById", String::class.java)
        assertNotNull(findByIdMethod)
    }

    @Test
    fun `GIVEN generic fake WHEN used with specific type THEN should maintain type safety`() {
        val generationResult = KotlinCompilation().apply {
            sources = listOf(sourceWithFakeAnnotation)
            compilerPlugins = listOf(FaktCompilerPluginRegistrar())
        }.compile()

        assertEquals(KotlinCompilation.ExitCode.OK, generationResult.exitCode)

        // Stage 3: Use-site type safety validation
        val clientCompilation = KotlinCompilation().apply {
            sources = listOf(
                SourceFile.kotlin("Client.kt", """
                    package test
                    fun testTypeSafety() {
                        val repo = fakeRepository<String> {}
                        val result: String? = repo.findById("123") // TYPE CHECK!
                        repo.save("value") // TYPE CHECK!
                    }
                """)
            )
            classpaths = generationResult.classpaths
        }.compile()

        assertEquals(KotlinCompilation.ExitCode.OK, clientCompilation.exitCode)
    }
}
```

**Test Matrix:**

| Scenario | Interface Example | Priority |
|----------|-------------------|----------|
| Simple class-level | `Repository<T>` | P0 |
| Multiple params | `Cache<K, V>` | P0 |
| Method-level | `fun <R> transform()` | P1 |
| Mixed generics | `Processor<T> { fun <R> map() }` | P1 |
| Constraints | `<T : Number>` | P2 |
| Variance | `Producer<out T>` | P2 |
| Nested | `List<Map<K, V>>` | P3 |
| Star projection | `List<*>` | P3 |
| Recursive | `Node<T : Node<T>>` | P3 |

---

## Edge Cases & Mitigation

### **1. Recursive Generics**
```kotlin
interface Node<T : Node<T>> {
    fun getChildren(): List<T>
}
```

**Challenge:** Infinite type recursion in substitution
**Solution:** Detect recursion, use upper bound as fallback

```kotlin
fun detectRecursiveGeneric(typeParam: IrTypeParameter): Boolean {
    return typeParam.superTypes.any { superType ->
        containsTypeParameter(superType, typeParam.symbol)
    }
}

fun handleRecursiveGeneric(typeParam: IrTypeParameter): IrType {
    // Use first non-recursive upper bound or Any
    return typeParam.superTypes.firstOrNull {
        !containsTypeParameter(it, typeParam.symbol)
    } ?: pluginContext.irBuiltIns.anyType
}
```

### **2. Star Projections**
```kotlin
interface Handler {
    fun process(items: List<*>)
}
```

**Challenge:** Unknown type argument
**Solution:** Use upper bound from variance

```kotlin
fun resolveStarProjection(
    typeParam: IrTypeParameter,
    variance: Variance
): IrType {
    return when (variance) {
        Variance.OUT_VARIANCE -> typeParam.superTypes.first() // out T → T's upper bound
        Variance.IN_VARIANCE -> pluginContext.irBuiltIns.nothingType // in T → Nothing
        else -> typeParam.superTypes.first() // invariant → T's upper bound
    }
}
```

### **3. Multiple Constraints**
```kotlin
fun <T> process() where T : Comparable<T>, T : Serializable
```

**Challenge:** Propagate all constraints
**Solution:** Apply substitution to each constraint

```kotlin
val newTypeParameter = irFactory.createTypeParameter(...).also { newTp ->
    newTp.superTypes = oldTp.superTypes.map { constraint ->
        classLevelSubstitutor.substitute(constraint)
    }
}
```

### **4. Generic Function Types**
```kotlin
interface Processor<T> {
    fun process(handler: (T) -> Unit)
}
```

**Challenge:** Substitute within function type arguments
**Solution:** IrTypeSubstitutor handles recursively

```kotlin
// IrTypeSubstitutor automatically handles:
// (T) -> Unit
//  ↓ (when T = String)
// (String) -> Unit
```

## Risk Assessment

### **High Risk Areas**

| Risk | Impact | Mitigation |
|------|--------|------------|
| IR API changes | Breaking changes in Kotlin versions | Test against multiple Kotlin versions |
| Type safety holes | Unsound generated code | Multi-stage validation with kotlin-compile-testing |
| Performance degradation | Slow compilation | Benchmark and optimize substitution |
| Edge case failures | Silent bugs | Comprehensive test matrix |

### **Known Limitations**

```
❌ Higher-kinded types: fun <F<_>> process()
   Status: Not supported in Kotlin type system

❌ Inline reified with constraints: inline fun <reified T : Number>
   Status: Requires special handling (Phase 4)

❌ Platform-specific generics: expect/actual with generics
   Status: KMP complexity (Phase 4)
```

## Success Metrics

### **Compilation Success Rate**
```
Target: 95% of generic interfaces compile successfully
Measurement: kotlin-compile-testing pass rate
```

### **Type Safety Preservation**
```
Target: 100% of use-site code type-checks correctly
Measurement: Client code compilation success
```

### **Performance Impact**
```
Target: <10% compilation time increase
Measurement: Gradle build benchmarks
```

### **Developer Experience**
```
Target: Zero manual casting for 90% of cases
Measurement: Generated code review
```

## Implementation Checklist

### **Phase 1: Type Erasure MVP (2-3 days)**
- [ ] Remove generic filter in `UnifiedFaktIrGenerationExtension`
- [ ] Update `TypeResolver` for type erasure
- [ ] Test `Repository<T>` interface
- [ ] Validate compilation success

### **Phase 2: Smart Defaults (3-5 days)**
- [ ] Implement `GenericConstraintAnalyzer`
- [ ] Create `SmartDefaultProvider`
- [ ] Map constraints to defaults
- [ ] Test constraint-based scenarios

### **Phase 3: IR Substitution (1-2 weeks)**
- [ ] Create `GenericIrSubstitutor`
- [ ] Implement class-level substitution
- [ ] Add method-level remapping
- [ ] Test mixed generic scenarios

### **Phase 4: Production Polish (1 week)**
- [ ] Handle all edge cases
- [ ] Comprehensive test matrix
- [ ] Performance optimization
- [ ] Documentation & examples

## Related Commands
- `/analyze-generic-scoping` - Analyze scoping challenges
- `/debug-ir-generation <interface>` - Debug IR issues
- `/validate-compilation` - Validate generated code
- `/consult-kotlin-api IrTypeSubstitutor` - API reference

## Technical References

### **Kotlin Compiler Source**
- `kotlin/compiler/ir/backend.common/src/org/jetbrains/kotlin/ir/util/IrTypeSubstitutor.kt`
- `kotlin/compiler/ir/backend.common/src/org/jetbrains/kotlin/ir/util/IrTypeParameterRemapper.kt`
- `kotlin/compiler/ir/tree/src/org/jetbrains/kotlin/ir/types/IrType.kt`

### **Case Studies**
- **kotlinx.serialization**: Generic data class serialization
- **Jetpack Compose**: Generic composable function handling
- **Metro DI**: Generic dependency injection

### **Testing Framework**
- `kotlin-compile-testing` documentation
- Multi-stage validation patterns
- Type safety testing approaches

---

**Use this command to develop a comprehensive, production-ready implementation strategy for Kotlin IR generic type handling in the Fakt compiler plugin.**
