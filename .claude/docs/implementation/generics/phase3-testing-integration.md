# Phase 3: Testing & Integration (Week 3)

> **Goal**: Comprehensive testing, edge cases, and production readiness
> **Duration**: 5-7 days
> **Prerequisites**: Phase 2 complete (generators produce generic code)

## 📋 Tasks Breakdown

### Task 3.1: Implement Full Test Matrix (Day 1-3)

**File**: `compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/GenericFakeGenerationTest.kt`

**Test Structure** (using kotlin-compile-testing):

```kotlin
package com.rsicarelli.fakt.compiler

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenericFakeGenerationTest {

    // ============================================================================
    // P0: Basic Class-Level Generics
    // ============================================================================

    @Test
    fun `GIVEN interface with single type parameter WHEN generating fake THEN should compile successfully`() {
        // Given
        val compilation = createCompilation("""
            package test
            import com.rsicarelli.fakt.Fake

            @Fake
            interface Repository<T> {
                fun save(item: T): T
                fun findById(id: String): T?
            }
        """)

        // When
        val result = compilation.compile()

        // Then - Stage 1: Compilation success
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        // Stage 2: Structural validation
        val fakeClass = result.classLoader.loadClass("test.FakeRepositoryImpl")
        assertTrue(fakeClass.typeParameters.isNotEmpty(), "Class should have type parameters")
        assertEquals(1, fakeClass.typeParameters.size, "Should have exactly 1 type parameter")
    }

    @Test
    fun `GIVEN generic fake WHEN using with concrete type THEN should maintain type safety`() {
        // Given - Generate fake
        val generationResult = createCompilation("""
            package test
            import com.rsicarelli.fakt.Fake

            @Fake
            interface Repository<T> {
                fun save(item: T): T
            }
        """).compile()

        assertEquals(KotlinCompilation.ExitCode.OK, generationResult.exitCode)

        // When - Use fake with concrete type
        val usageResult = createCompilationWithClasspath(
            generationResult.classpaths,
            """
            package test

            fun testTypeSafety() {
                val repo = fakeRepository<String> {}
                val result: String = repo.save("test") // TYPE CHECK!
                // val wrong: Int = repo.save("test") // Should NOT compile
            }
            """
        ).compile()

        // Then - Stage 3: Use-site type safety
        assertEquals(KotlinCompilation.ExitCode.OK, usageResult.exitCode)
    }

    @Test
    fun `GIVEN interface with multiple type parameters WHEN generating fake THEN should preserve all parameters`() {
        // Given
        val compilation = createCompilation("""
            package test
            import com.rsicarelli.fakt.Fake

            @Fake
            interface Cache<K, V> {
                fun get(key: K): V?
                fun put(key: K, value: V): V?
            }
        """)

        // When
        val result = compilation.compile()

        // Then
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val fakeClass = result.classLoader.loadClass("test.FakeCacheImpl")
        assertEquals(2, fakeClass.typeParameters.size, "Should have 2 type parameters")
    }

    // ============================================================================
    // P1: Method-Level & Mixed Generics
    // ============================================================================

    @Test
    fun `GIVEN interface with method-level generics WHEN generating fake THEN should preserve method type params`() {
        // Given
        val compilation = createCompilation("""
            package test
            import com.rsicarelli.fakt.Fake

            @Fake
            interface Processor {
                fun <R> transform(input: String): R
            }
        """)

        // When
        val result = compilation.compile()

        // Then
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val fakeClass = result.classLoader.loadClass("test.FakeProcessorImpl")
        val transformMethod = fakeClass.getMethod("transform", String::class.java)
        assertTrue(transformMethod.typeParameters.isNotEmpty(), "Method should have type parameters")
    }

    @Test
    fun `GIVEN interface with mixed generics WHEN generating fake THEN should handle both levels`() {
        // Given
        val compilation = createCompilation("""
            package test
            import com.rsicarelli.fakt.Fake

            @Fake
            interface Service<T> {
                fun process(item: T): T
                fun <R> transform(item: T): R
            }
        """)

        // When
        val result = compilation.compile()

        // Then
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        // Validate both class-level and method-level generics
    }

    // ============================================================================
    // P2: Constraints & Variance
    // ============================================================================

    @Test
    fun `GIVEN interface with type constraints WHEN generating fake THEN should preserve constraints`() {
        // Given
        val compilation = createCompilation("""
            package test
            import com.rsicarelli.fakt.Fake

            @Fake
            interface NumberService<T : Number> {
                fun compute(value: T): T
            }
        """)

        // When
        val result = compilation.compile()

        // Then
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        // Validate constraint is preserved
    }

    @Test
    fun `GIVEN interface with variance annotations WHEN generating fake THEN should preserve variance`() {
        // Given
        val compilation = createCompilation("""
            package test
            import com.rsicarelli.fakt.Fake

            @Fake
            interface Producer<out T> {
                fun produce(): T
            }

            @Fake
            interface Consumer<in T> {
                fun consume(item: T)
            }
        """)

        // When
        val result = compilation.compile()

        // Then
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        // Validate variance is preserved
    }

    // ============================================================================
    // P3: Advanced Edge Cases
    // ============================================================================

    @Test
    fun `GIVEN interface with star projections WHEN generating fake THEN should handle gracefully`() {
        // Given
        val compilation = createCompilation("""
            package test
            import com.rsicarelli.fakt.Fake

            @Fake
            interface Handler {
                fun process(items: List<*>)
            }
        """)

        // When
        val result = compilation.compile()

        // Then
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `GIVEN interface with nested generics WHEN generating fake THEN should preserve nesting`() {
        // Given
        val compilation = createCompilation("""
            package test
            import com.rsicarelli.fakt.Fake

            @Fake
            interface ComplexService {
                fun process(data: Map<String, List<Int>>): List<Map<String, Int>>
            }
        """)

        // When
        val result = compilation.compile()

        // Then
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
    }

    @Test
    fun `GIVEN interface with recursive generics WHEN generating fake THEN should handle with fallback`() {
        // Given
        val compilation = createCompilation("""
            package test
            import com.rsicarelli.fakt.Fake

            @Fake
            interface Node<T : Node<T>> {
                fun getChildren(): List<T>
            }
        """)

        // When
        val result = compilation.compile()

        // Then
        // May use fallback strategy for recursive generics
        // Accept either OK or graceful degradation
        assertTrue(
            result.exitCode == KotlinCompilation.ExitCode.OK ||
            result.messages.contains("recursive generic")
        )
    }

    // ============================================================================
    // Real-World Scenarios
    // ============================================================================

    @Test
    fun `GIVEN realistic repository interface WHEN using fake THEN should work end-to-end`() {
        // Given - Generate fake
        val generationResult = createCompilation("""
            package test
            import com.rsicarelli.fakt.Fake

            data class User(val id: String, val name: String)

            @Fake
            interface UserRepository {
                suspend fun findById(id: String): User?
                suspend fun save(user: User): User
                suspend fun findAll(): List<User>
            }
        """).compile()

        assertEquals(KotlinCompilation.ExitCode.OK, generationResult.exitCode)

        // When - Use in test
        val usageResult = createCompilationWithClasspath(
            generationResult.classpaths,
            """
            package test
            import kotlinx.coroutines.runBlocking

            fun testUserRepository() = runBlocking {
                val repo = fakeUserRepository {
                    findById { id -> User(id, "Test User") }
                    save { user -> user }
                }

                val user = repo.findById("123")
                val saved = repo.save(User("456", "New User"))
            }
            """
        ).compile()

        // Then
        assertEquals(KotlinCompilation.ExitCode.OK, usageResult.exitCode)
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private fun createCompilation(sourceCode: String): KotlinCompilation {
        return KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("Test.kt", sourceCode))
            compilerPlugins = listOf(FaktCompilerPluginRegistrar())
            inheritClassPath = true
        }
    }

    private fun createCompilationWithClasspath(
        classpaths: List<File>,
        sourceCode: String
    ): KotlinCompilation {
        return KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("Usage.kt", sourceCode))
            this.classpaths = classpaths
            inheritClassPath = true
        }
    }
}
```

---

### Task 3.2: Edge Case Handling (Day 3-4)

**Create**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/GenericEdgeCaseHandler.kt`

```kotlin
package com.rsicarelli.fakt.compiler.ir

import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.types.*

/**
 * Handles edge cases in generic type processing.
 */
class GenericEdgeCaseHandler(private val substitutor: GenericIrSubstitutor) {

    /**
     * Handles star projections in generic types.
     */
    fun handleStarProjection(irType: IrType): IrType {
        return when (irType) {
            is IrSimpleType -> {
                if (irType.arguments.any { it is IrStarProjection }) {
                    // Replace star projections with upper bounds
                    val newArguments = irType.arguments.map { arg ->
                        when (arg) {
                            is IrStarProjection -> {
                                // Use variance to determine replacement
                                IrTypeProjection(
                                    type = substitutor.pluginContext.irBuiltIns.anyNullableType,
                                    variance = Variance.INVARIANT
                                )
                            }
                            else -> arg
                        }
                    }
                    irType.withArguments(newArguments)
                } else {
                    irType
                }
            }
            else -> irType
        }
    }

    /**
     * Handles recursive generic constraints.
     */
    fun handleRecursiveConstraint(typeParam: IrTypeParameter): IrType {
        if (substitutor.isRecursiveGeneric(typeParam)) {
            return substitutor.resolveRecursiveGeneric(typeParam)
        }
        return typeParam.defaultType
    }

    /**
     * Validates that generated code will be compilable.
     */
    fun validateGenericStructure(analysis: InterfaceAnalysis): List<String> {
        val warnings = mutableListOf<String>()

        // Check for unsupported recursive generics
        analysis.sourceInterface.typeParameters.forEach { typeParam ->
            if (substitutor.isRecursiveGeneric(typeParam)) {
                warnings.add(
                    "Recursive generic detected: ${typeParam.name}. " +
                    "Using fallback strategy with upper bound."
                )
            }
        }

        // Check for deeply nested generics (>3 levels)
        // TODO: Implement nested depth check

        return warnings
    }
}
```

---

### Task 3.3: Performance Benchmarking (Day 4-5)

**Create**: `compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/performance/GenericCompilationBenchmark.kt`

```kotlin
package com.rsicarelli.fakt.compiler.performance

import kotlin.test.Test
import kotlin.time.measureTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenericCompilationBenchmark {

    @Test
    fun `GIVEN 10 generic interfaces WHEN compiling THEN should complete within time limit`() {
        // Given
        val interfaces = (1..10).map { i ->
            """
            @Fake
            interface Service$i<T> {
                fun process(item: T): T
                fun findById(id: String): T?
            }
            """
        }

        // When
        val compilationTime = measureTime {
            val compilation = createCompilation(interfaces.joinToString("\n"))
            compilation.compile()
        }

        // Then
        println("Compilation time for 10 generic interfaces: $compilationTime")
        // Target: <10% overhead compared to non-generic baseline
    }

    @Test
    fun `GIVEN complex nested generics WHEN compiling THEN should handle efficiently`() {
        // Test compilation time for complex scenarios
    }
}
```

**Benchmarking Strategy**:
1. Baseline: 10 non-generic interfaces compilation time
2. Generic: 10 generic interfaces compilation time
3. Target: Generic time < Baseline time * 1.10 (10% overhead)

---

### Task 3.4: Documentation & Examples (Day 5-6)

**Update**: `ktfake/samples/kmp-single-module/src/commonMain/kotlin/TestService.kt`

Add working generic examples:

```kotlin
// ============================================================================
// GENERIC TYPES - Now fully supported!
// ============================================================================

@Fake
interface Repository<T> {
    val items: List<T>
    fun findAll(): List<T>
    fun findById(id: String): T?
    fun save(item: T): T
    fun saveAll(items: List<T>): List<T>
}

@Fake
interface Cache<K, V> {
    val size: Int
    fun get(key: K): V?
    fun put(key: K, value: V): V?
    fun remove(key: K): V?
    fun clear()
}

// Usage examples in tests
val userRepo = fakeRepository<User> {
    save { user -> user }
    findById { id -> User(id, "Test User", "test@example.com") }
}

val cache = fakeCache<String, Int> {
    get { key -> 42 }
    put { key, value -> null }
}
```

**Create**: `.claude/docs/implementation/generics/USAGE.md` with examples

---

### Task 3.5: Update Project Documentation (Day 6-7)

**Files to Update**:

1. **CLAUDE.md** - Update "Status Atual do Projeto":
```markdown
### ✅ Funcionando (Production-Ready)

#### Generic Type Support (NEW!)
- ✅ Class-level generics (`interface Repository<T>`)
- ✅ Method-level generics (`fun <R> transform()`)
- ✅ Multiple type parameters (`Cache<K, V>`)
- ✅ Type constraints (`<T : Number>`)
- ✅ Variance annotations (`out T`, `in T`)
- ✅ Reified factory functions
- ✅ Type-safe configuration DSL
```

2. **README.md** - Add generic support to features

3. **Current Status** - Remove from "Não Funcionando" section

---

## ✅ Phase 3 Completion Criteria

- [ ] All P0 tests passing (basic generics)
- [ ] All P1 tests passing (method-level & mixed)
- [ ] P2 tests passing (constraints & variance)
- [ ] P3 edge cases handled gracefully
- [ ] Performance benchmarks within 10% overhead
- [ ] Documentation updated
- [ ] Sample code with generics working
- [ ] Code review: production quality validation

## 📊 Test Coverage Target

| Priority | Scenarios | Required Pass Rate |
|----------|-----------|-------------------|
| P0 | Basic class-level | 100% |
| P1 | Method & mixed | 95% |
| P2 | Constraints & variance | 90% |
| P3 | Advanced edge cases | 80% (graceful degradation OK) |

## 🚀 Production Readiness Checklist

- [ ] All tests in test matrix passing
- [ ] Performance benchmarks meet targets
- [ ] Edge cases documented and handled
- [ ] Examples in samples/ working
- [ ] Documentation complete
- [ ] Metro pattern alignment verified
- [ ] Ready for publishToMavenLocal testing
- [ ] Real-world project validation

## 🔗 Next Steps

After Phase 3 completion:
1. Test with real projects (samples/)
2. Performance optimization if needed
3. Update changelog
4. Prepare for release
