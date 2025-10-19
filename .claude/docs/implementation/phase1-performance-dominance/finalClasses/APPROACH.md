# Final Class Faking: Technical Approach

> **Feature**: Compile-time fake generation for final Kotlin classes
> **Priority**: HIGH
> **Complexity**: Medium (2-4 weeks)
> **Target Performance**: Same as interface faking (<10ms overhead)

## Problem Statement

### The "Final by Default" Challenge

Kotlin classes and their methods are **final by default** by design. This promotes immutability and safer inheritance hierarchies, but creates a fundamental conflict with traditional mocking frameworks that rely on creating subclasses (proxies) at runtime.

**Example Problem:**

```kotlin
// Production code - final by default
class UserService {
    fun getUser(id: String): User {
        // Real implementation
    }
}

// Test code - this FAILS with Mockito
val mock = mock(UserService::class.java) // ❌ MockitoException: Cannot mock final class
```

### Current Solutions & Pain Points

| Solution | Approach | Pain Points | Performance |
|----------|---------|-------------|-------------|
| **Manual `open`** | Add `open` keyword to classes/methods | ❌ Pollutes production code<br>❌ Violates Kotlin principles | ✅ Fast |
| **all-open plugin** | Annotation-based `open` generation | ❌ Still requires production annotations<br>❌ Test-specific concerns in prod | ✅ Fast |
| **mockito-inline** | Runtime bytecode manipulation | ❌ **3x slower** test suites<br>❌ Complex setup | ❌ Slow |
| **MockK default** | Bytecode transformation on-the-fly | ❌ Performance overhead<br>❌ Runtime complexity | ⚠️ Moderate |

### Research Evidence

From [Mocking Kotlin classes article](https://medium.com/21buttons-tech/mocking-kotlin-classes-with-mockito-the-fast-way-631824edd5ba):
- Projects using `mockito-inline` report **3x slower** test execution
- Developers resort to polluting production code to avoid performance hit
- Community consensus: Current solutions are unsatisfactory compromises

---

## Fakt's Solution: Compile-time Fake Generation

### Core Idea

Generate a separate **fake implementation class** at compile time that:
1. Extends or implements the final class structure
2. Provides configurable behavior (like interface fakes)
3. Requires **zero modifications** to production code
4. Incurs **zero runtime overhead**

### Example Output

```kotlin
// Production code (unchanged)
class UserService {
    fun getUser(id: String): User {
        return database.query(id)
    }

    fun saveUser(user: User) {
        database.save(user)
    }
}

// Generated fake (in test source set)
class FakeUserServiceImpl : UserService() {
    private var getUserBehavior: (String) -> User = { _ -> User.default() }
    private var saveUserBehavior: (User) -> Unit = { }

    override fun getUser(id: String): User = getUserBehavior(id)
    override fun saveUser(user: User) = saveUserBehavior(user)

    internal fun configureGetUser(behavior: (String) -> User) {
        getUserBehavior = behavior
    }

    internal fun configureSaveUser(behavior: (User) -> Unit) {
        saveUserBehavior = behavior
    }
}

// Factory function
fun fakeUserService(configure: FakeUserServiceConfig.() -> Unit = {}): UserService {
    return FakeUserServiceImpl().apply {
        FakeUserServiceConfig(this).configure()
    }
}

// Configuration DSL
class FakeUserServiceConfig(private val fake: FakeUserServiceImpl) {
    fun getUser(behavior: (String) -> User) {
        fake.configureGetUser(behavior)
    }

    fun saveUser(behavior: (User) -> Unit) {
        fake.configureSaveUser(behavior)
    }
}

// Usage in tests
val fake = fakeUserService {
    getUser { id -> User(id, "Test User") }
    saveUser { user -> println("Saved: ${user.name}") }
}
```

---

## Implementation Plan

### Phase 1: Analysis Extension (Week 1)

#### 1.1 Extend InterfaceAnalyzer

Currently, `InterfaceAnalyzer` only processes interfaces. We need to extend it to handle classes:

```kotlin
// Current
sealed class AnalyzedType {
    data class Interface(val declaration: IrClass, ...) : AnalyzedType()
}

// Extended
sealed class AnalyzedType {
    data class Interface(val declaration: IrClass, ...) : AnalyzedType()
    data class FinalClass(
        val declaration: IrClass,
        val constructors: List<IrConstructor>,
        val overridableFunctions: List<IrSimpleFunction>,
        val hasOpenMembers: Boolean
    ) : AnalyzedType()
}
```

#### 1.2 Class Detection Logic

```kotlin
fun IrClass.isFakableClass(): Boolean {
    return when {
        isInterface -> false // Already handled
        isAbstract -> false  // Phase 2 feature
        isSealed -> false    // Phase 2 feature
        isCompanion -> false // Not a fake target
        isInner -> false     // Complex, defer
        isData -> false      // Phase 2 - use builders instead
        isFinalClass -> true // ✅ This is our target!
        else -> false
    }
}

val IrClass.isFinalClass: Boolean
    get() = modality == Modality.FINAL && kind == ClassKind.CLASS
```

#### 1.3 Member Analysis

```kotlin
fun analyzeFinalClass(irClass: IrClass): AnalyzedType.FinalClass {
    val overridableFunctions = irClass.functions
        .filter { it.isFakeCandidate }
        .toList()

    val constructors = irClass.constructors.toList()

    return AnalyzedType.FinalClass(
        declaration = irClass,
        constructors = constructors,
        overridableFunctions = overridableFunctions,
        hasOpenMembers = overridableFunctions.isNotEmpty()
    )
}

val IrSimpleFunction.isFakeCandidate: Boolean
    get() = when {
        isFinal -> true    // Override final methods
        isOpen -> true     // Already overridable
        visibility == DescriptorVisibilities.PRIVATE -> false
        else -> true
    }
```

### Phase 2: Generation Extension (Week 2)

#### 2.1 Extend ImplementationGenerator

```kotlin
// In ImplementationGenerator.kt
fun generateFakeImplementation(analyzedType: AnalyzedType): IrClass {
    return when (analyzedType) {
        is AnalyzedType.Interface -> generateInterfaceFake(analyzedType)
        is AnalyzedType.FinalClass -> generateClassFake(analyzedType) // NEW
    }
}

private fun generateClassFake(classType: AnalyzedType.FinalClass): IrClass {
    val fakeClass = irFactory.buildClass {
        name = Name.identifier("Fake${classType.declaration.name}Impl")
        kind = ClassKind.CLASS
        modality = Modality.OPEN // Generated fake must be open for testability
    }

    // Add supertype (extends the original final class)
    fakeClass.superTypes = listOf(classType.declaration.defaultType)

    // Generate constructor (call super constructor)
    generateConstructor(fakeClass, classType.constructors)

    // Generate behavior properties
    classType.overridableFunctions.forEach { function ->
        generateBehaviorProperty(fakeClass, function)
    }

    // Generate override methods
    classType.overridableFunctions.forEach { function ->
        generateOverrideMethod(fakeClass, function)
    }

    // Generate configuration methods
    classType.overridableFunctions.forEach { function ->
        generateConfigMethod(fakeClass, function)
    }

    return fakeClass
}
```

#### 2.2 Constructor Handling

**Challenge**: Final classes may have non-default constructors with parameters.

**Solution**: Generate a constructor that calls the super constructor with default values:

```kotlin
private fun generateConstructor(
    fakeClass: IrClass,
    constructors: List<IrConstructor>
) {
    // Use primary constructor or first available
    val targetConstructor = constructors.firstOrNull { it.isPrimary }
        ?: constructors.firstOrNull()
        ?: return // No constructor needed (shouldn't happen)

    fakeClass.addConstructor {
        isPrimary = true
    }.apply {
        // Call super with default values for each parameter
        body = irFactory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET).apply {
            statements.add(
                IrDelegatingConstructorCallImpl(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    type = targetConstructor.returnType,
                    symbol = targetConstructor.symbol,
                    typeArgumentsCount = 0,
                    valueArgumentsCount = targetConstructor.valueParameters.size
                ).apply {
                    // For each parameter, provide a default value
                    targetConstructor.valueParameters.forEachIndexed { index, param ->
                        putValueArgument(index, param.type.defaultValue(irBuiltIns))
                    }
                }
            )
        }
    }
}

// Extension: Get default value for a type
fun IrType.defaultValue(irBuiltIns: IrBuiltIns): IrExpression {
    return when {
        isInt() -> IrConstImpl.int(UNDEFINED_OFFSET, UNDEFINED_OFFSET, this, 0)
        isString() -> IrConstImpl.string(UNDEFINED_OFFSET, UNDEFINED_OFFSET, this, "")
        isBoolean() -> IrConstImpl.boolean(UNDEFINED_OFFSET, UNDEFINED_OFFSET, this, false)
        isUnit() -> IrGetObjectValueImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            irBuiltIns.unitType, irBuiltIns.unitClass
        )
        // ... other primitives
        else -> IrConstImpl.constNull(UNDEFINED_OFFSET, UNDEFINED_OFFSET, irBuiltIns.nothingNType)
    }
}
```

### Phase 3: Edge Cases & Testing (Week 3-4)

#### 3.1 Edge Cases to Handle

| Case | Strategy |
|------|----------|
| **Generic classes** | Use existing generic support from Phase Generic implementation |
| **Suspend functions** | Already handled by interface generator |
| **Visibility modifiers** | Respect visibility, skip private members |
| **Inheritance hierarchies** | Only override members from the target class |
| **Companion objects** | Ignore (handled in Phase 1.2) |
| **Inner classes** | Defer to future (complex) |

#### 3.2 Test Coverage Strategy

**GIVEN-WHEN-THEN Test Matrix:**

```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FinalClassFakeGenerationTest {

    @Test
    fun `GIVEN final class with no-arg constructor WHEN generating fake THEN should compile`() = runTest {
        // Given
        val sourceCode = """
            class SimpleService {
                fun getValue(): Int = 42
            }
        """

        // When
        val result = compilationTestHelper.compile(sourceCode)

        // Then
        assertTrue(result.hasClass("FakeSimpleServiceImpl"))
        assertTrue(result.compiles())
    }

    @Test
    fun `GIVEN final class with constructor params WHEN generating fake THEN should call super with defaults`() = runTest {
        // Given
        val sourceCode = """
            class UserService(val apiUrl: String, val timeout: Int) {
                fun getUser(): User = TODO()
            }
        """

        // When
        val result = compilationTestHelper.compile(sourceCode)

        // Then
        assertTrue(result.hasConstructor("FakeUserServiceImpl"))
        assertTrue(result.callsSuperConstructor())
        assertTrue(result.compiles())
    }

    @Test
    fun `GIVEN final class with suspend function WHEN generating fake THEN should preserve suspend modifier`() = runTest {
        // Given
        val sourceCode = """
            class AsyncService {
                suspend fun fetchData(): Data = TODO()
            }
        """

        // When
        val result = compilationTestHelper.compile(sourceCode)

        // Then
        assertTrue(result.hasMethod("fetchData"))
        assertTrue(result.getMethod("fetchData").isSuspend)
        assertTrue(result.compiles())
    }

    @Test
    fun `GIVEN final class with generic type WHEN generating fake THEN should preserve type parameters`() = runTest {
        // Given
        val sourceCode = """
            class Repository<T> {
                fun save(item: T): T = item
            }
        """

        // When
        val result = compilationTestHelper.compile(sourceCode)

        // Then
        assertTrue(result.hasClass("FakeRepositoryImpl"))
        assertTrue(result.hasTypeParameter("T"))
        assertTrue(result.compiles())
    }
}
```

---

## Metro Alignment

### Relevant Metro Patterns

1. **Class Analysis**: Metro analyzes classes for dependency injection
   - Review: `metro/compiler/.../analysis/ClassAnalyzer.kt`
   - Pattern: Detect constructor parameters, member functions
   - Apply: Similar analysis for fakable classes

2. **Constructor Generation**: Metro generates DI constructors
   - Review: `metro/compiler/.../generation/ConstructorGenerator.kt`
   - Pattern: Super constructor delegation with provided values
   - Apply: Default value provision instead of DI

3. **Two-Phase FIR→IR**: Metro's compilation flow
   - Pattern: FIR detects annotations → IR generates code
   - Apply: Same flow, extend to detect `@Fake` on classes

### Consultation Checklist

Before implementation:
- [ ] `/consult-kotlin-api IrConstructor` - Verify constructor APIs
- [ ] `/consult-kotlin-api IrDelegatingConstructorCall` - Super call patterns
- [ ] `/validate-metro-alignment` - Check Metro's class handling
- [ ] Review Metro's error handling for missing constructors

---

## Performance Benchmarks

### Target Metrics

```kotlin
@Test
fun `GIVEN 1000 final class fake instantiations WHEN compared to mockito-inline THEN should be 3x faster`() {
    val iterations = 1000

    // Baseline: Mockito with inline mock maker
    val mockitoTime = measureTime {
        repeat(iterations) {
            val mock = mock(UserService::class.java)
            `when`(mock.getUser(any())).thenReturn(User.test())
        }
    }

    // Fakt: compile-time fake
    val faktTime = measureTime {
        repeat(iterations) {
            val fake = fakeUserService {
                getUser { User.test() }
            }
        }
    }

    val speedup = mockitoTime / faktTime
    assertTrue(speedup >= 3.0, "Expected 3x speedup, got ${speedup}x")
}
```

### Compilation Time Impact

**Baseline**: Measure current compilation time for 100 interfaces
**Target**: <5% increase when adding 100 final classes

```bash
# Before
./gradlew :samples:kmp-single-module:build --info | grep "Execution time"
# Baseline: 5.2s

# After adding 100 @Fake classes
./gradlew :samples:kmp-single-module:build --info | grep "Execution time"
# Target: <5.5s (less than 5% increase)
```

---

## Success Criteria

### Must Have (P0)
- ✅ Generate fakes for final classes with no-arg constructors
- ✅ Generate fakes for classes with parameterized constructors
- ✅ Preserve `suspend` modifier in functions
- ✅ Zero production code modifications required
- ✅ Compilation time <5% overhead
- ✅ Generated code passes all compilation checks

### Should Have (P1)
- ✅ Generic class support (existing generic infrastructure)
- ✅ Visibility modifier respect (public/internal only)
- ✅ Clear error messages for unsupported cases
- ✅ Comprehensive GIVEN-WHEN-THEN test coverage

### Nice to Have (P2)
- ⏳ Inheritance hierarchy handling (override only direct members)
- ⏳ Inner class support (defer if complex)
- ⏳ IDE integration (IntelliJ recognizes generated fakes)

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Constructor defaults don't work for all types | Medium | High | Comprehensive type default mapping, fallback to null |
| Performance claims don't hold | Low | Critical | Continuous benchmarking, realistic measurements |
| Edge cases break compilation | Medium | Medium | Extensive test matrix, dogfooding |
| Developers prefer existing solutions | Low | High | Clear migration guide, showcase performance wins |

---

## Next Steps

1. ✅ Review this approach document
2. 🎯 Create [EXAMPLES.md](./EXAMPLES.md) with usage patterns
3. 🎯 Implement Phase 1: Analysis Extension
4. 🎯 Test with simple final classes
5. 🎯 Implement Phase 2: Generation Extension
6. 🎯 Create comprehensive test suite
7. 🎯 Benchmark against Mockito-inline
8. 🎯 Document migration path from existing solutions

---

## References

- **Research**: [Mocking Kotlin classes](https://medium.com/21buttons-tech/mocking-kotlin-classes-with-mockito-the-fast-way-631824edd5ba)
- **Mockito-inline**: [Official Mockito docs](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html#mock-makers)
- **Phase 1 README**: [../README.md](../README.md)
- **Main Roadmap**: [../../roadmap.md](../../roadmap.md)

---

**Final Classes = Foundation for performance leadership.** 🎯
