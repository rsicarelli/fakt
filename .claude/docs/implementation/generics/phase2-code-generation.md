# Phase 2: Code Generation (Week 2)

> **Goal**: Update all generators to produce generic fake implementations
> **Duration**: 5-7 days
> **Prerequisites**: Phase 1 complete (GenericIrSubstitutor working)

## 📋 Tasks Breakdown

### Task 2.1: Update ImplementationGenerator.kt (Day 1-2)

**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/codegen/ImplementationGenerator.kt`

**Changes**:

1. **Generate generic class declaration** (lines 40-49):

```kotlin
fun generateImplementation(
    analysis: InterfaceAnalysis,
    fakeClassName: String,
): String = buildString {
    // NEW: Generate type parameters for the fake class
    val typeParameters = if (analysis.typeParameters.isNotEmpty()) {
        "<${analysis.typeParameters.joinToString(", ")}>"
    } else {
        ""
    }

    // NEW: Generate interface name with type parameters
    val interfaceWithGenerics = if (analysis.typeParameters.isNotEmpty()) {
        "${analysis.interfaceName}${typeParameters}"
    } else {
        analysis.interfaceName
    }

    appendLine("class $fakeClassName$typeParameters : $interfaceWithGenerics {")

    // Rest remains the same...
    append(generateBehaviorProperties(analysis))
    appendLine()
    append(generateMethodOverrides(analysis))
    appendLine()
    append(generateConfigMethods(analysis))
    append("}")
}
```

**Example Output**:
```kotlin
// Before (Type Erasure):
class FakeRepositoryImpl : Repository<Any> {
    private var saveBehavior: (Any) -> Any = { it }
}

// After (Full Generics):
class FakeRepositoryImpl<T> : Repository<T> {
    private var saveBehavior: (T) -> T = { it }
}
```

2. **Update behavior properties to use type parameters**:

No changes needed! The existing code in `generateBehaviorProperties()` already uses `preserveTypeParameters = true`, so it will automatically generate `(T) -> T` instead of `(Any) -> Any`.

3. **Verify default value generation**:

Update `generateTypeSafeDefault()` to handle type parameters gracefully:

```kotlin
private fun generateTypeSafeDefault(function: FunctionAnalysis): String {
    val returnType = typeResolver.irTypeToKotlinString(
        function.returnType,
        preserveTypeParameters = true
    )

    // For type parameters, generate identity function or throw
    val defaultValue = if (isTypeParameter(returnType)) {
        generateTypeParameterDefault(returnType, function)
    } else {
        generateKotlinStdlibDefault(returnType)
    }

    return if (function.parameters.isEmpty()) {
        "{ $defaultValue }"
    } else if (function.parameters.size == 1) {
        "{ _ -> $defaultValue }"
    } else {
        "{ ${function.parameters.joinToString(", ") { "_" }} -> $defaultValue }"
    }
}

private fun isTypeParameter(typeString: String): Boolean {
    // Type parameters are single capital letters or CamelCase without package
    return typeString.matches(Regex("^[A-Z][A-Za-z0-9]*$")) &&
           !typeString.startsWith("kotlin.") &&
           typeString !in setOf("String", "Int", "Long", "Boolean", "Double", "Float", "Unit", "Any")
}

private fun generateTypeParameterDefault(
    typeParam: String,
    function: FunctionAnalysis
): String {
    // For functions that take the type parameter and return it, use identity
    if (function.parameters.size == 1) {
        val paramType = typeResolver.irTypeToKotlinString(
            function.parameters[0].type,
            preserveTypeParameters = true
        )
        if (paramType == typeParam) {
            return "it" // Identity function: T -> T becomes { it }
        }
    }

    // Otherwise, require user to configure
    return """error("Configure behavior for generic type '$typeParam'")"""
}
```

**Test**:
```kotlin
@Test
fun `GIVEN generic interface WHEN generating implementation THEN should preserve type parameters`() = runTest {
    // Given
    val analysis = createTestAnalysis("Repository", typeParameters = listOf("T"))
    val generator = ImplementationGenerator(TypeResolver())

    // When
    val result = generator.generateImplementation(analysis, "FakeRepositoryImpl")

    // Then
    assertTrue(result.contains("class FakeRepositoryImpl<T> : Repository<T>"))
    assertTrue(result.contains("(T) -> T"))
}
```

---

### Task 2.2: Update FactoryGenerator.kt (Day 2-3)

**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/codegen/FactoryGenerator.kt`

**Complete Rewrite**:

```kotlin
package com.rsicarelli.fakt.compiler.codegen

import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis

internal class FactoryGenerator {

    fun generateFactoryFunction(
        analysis: InterfaceAnalysis,
        fakeClassName: String,
    ): String {
        val interfaceName = analysis.interfaceName
        val factoryFunctionName = "fake$interfaceName"
        val configClassName = "Fake${interfaceName}Config"

        return if (analysis.typeParameters.isEmpty()) {
            generateNonGenericFactory(
                factoryName = factoryFunctionName,
                interfaceName = interfaceName,
                fakeClassName = fakeClassName,
                configClassName = configClassName
            )
        } else {
            generateGenericFactory(
                factoryName = factoryFunctionName,
                interfaceName = interfaceName,
                fakeClassName = fakeClassName,
                configClassName = configClassName,
                typeParameters = analysis.typeParameters
            )
        }
    }

    private fun generateNonGenericFactory(
        factoryName: String,
        interfaceName: String,
        fakeClassName: String,
        configClassName: String
    ): String = buildString {
        appendLine("fun $factoryName(")
        appendLine("    configure: $configClassName.() -> Unit = {}")
        appendLine("): $interfaceName {")
        appendLine("    return $fakeClassName().apply {")
        appendLine("        $configClassName(this).configure()")
        appendLine("    }")
        appendLine("}")
    }

    private fun generateGenericFactory(
        factoryName: String,
        interfaceName: String,
        fakeClassName: String,
        configClassName: String,
        typeParameters: List<String>
    ): String = buildString {
        val typeParamsList = typeParameters.joinToString(", ")
        val reifiedTypeParams = typeParameters.joinToString(", ") { "reified $it" }

        appendLine("inline fun <$reifiedTypeParams> $factoryName(")
        appendLine("    configure: $configClassName<$typeParamsList>.() -> Unit = {}")
        appendLine("): $interfaceName<$typeParamsList> {")
        appendLine("    return $fakeClassName<$typeParamsList>().apply {")
        appendLine("        $configClassName(this).configure()")
        appendLine("    }")
        appendLine("}")
    }
}
```

**Example Output**:
```kotlin
// Non-generic:
fun fakeUserService(
    configure: FakeUserServiceConfig.() -> Unit = {}
): UserService {
    return FakeUserServiceImpl().apply {
        FakeUserServiceConfig(this).configure()
    }
}

// Generic:
inline fun <reified T> fakeRepository(
    configure: FakeRepositoryConfig<T>.() -> Unit = {}
): Repository<T> {
    return FakeRepositoryImpl<T>().apply {
        FakeRepositoryConfig(this).configure()
    }
}

// Multi-parameter generic:
inline fun <reified K, reified V> fakeCache(
    configure: FakeCacheConfig<K, V>.() -> Unit = {}
): Cache<K, V> {
    return FakeCacheImpl<K, V>().apply {
        FakeCacheConfig(this).configure()
    }
}
```

**Test**:
```kotlin
@Test
fun `GIVEN generic interface WHEN generating factory THEN should use reified type parameters`() = runTest {
    // Given
    val analysis = createTestAnalysis("Repository", typeParameters = listOf("T"))
    val generator = FactoryGenerator()

    // When
    val result = generator.generateFactoryFunction(analysis, "FakeRepositoryImpl")

    // Then
    assertTrue(result.contains("inline fun <reified T>"))
    assertTrue(result.contains("Repository<T>"))
    assertTrue(result.contains("FakeRepositoryConfig<T>"))
}
```

---

### Task 2.3: Update ConfigurationDslGenerator.kt (Day 3-4)

**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/codegen/ConfigurationDslGenerator.kt`

**Changes**:

1. **Generate generic config class**:

```kotlin
fun generateConfigurationDsl(
    analysis: InterfaceAnalysis,
    fakeClassName: String,
): String = buildString {
    val interfaceName = analysis.interfaceName
    val configClassName = "Fake${interfaceName}Config"

    // NEW: Add type parameters to config class
    val typeParameters = if (analysis.typeParameters.isNotEmpty()) {
        "<${analysis.typeParameters.joinToString(", ")}>"
    } else {
        ""
    }

    appendLine("class $configClassName$typeParameters(")
    appendLine("    private val fake: $fakeClassName$typeParameters")
    appendLine(") {")

    // Generate configuration methods for functions
    for (function in analysis.functions) {
        append(generateFunctionConfigMethod(function, analysis.typeParameters))
    }

    // Generate configuration methods for properties
    for (property in analysis.properties) {
        append(generatePropertyConfigMethod(property, analysis.typeParameters))
    }

    appendLine("}")
}
```

2. **Update config methods to preserve type parameters**:

```kotlin
private fun generateFunctionConfigMethod(
    function: FunctionAnalysis,
    interfaceTypeParams: List<String>
): String = buildString {
    val functionName = function.name

    // Build parameter types (preserving generics)
    val parameterTypes = if (function.parameters.isEmpty()) {
        ""
    } else {
        function.parameters.joinToString(", ") { param ->
            val varargsPrefix = if (param.isVararg) "vararg " else ""
            val paramType = if (param.isVararg) {
                unwrapVarargsType(param)
            } else {
                typeResolver.irTypeToKotlinString(
                    param.type,
                    preserveTypeParameters = true
                )
            }
            varargsPrefix + paramType
        }
    }

    val returnType = typeResolver.irTypeToKotlinString(
        function.returnType,
        preserveTypeParameters = true
    )
    val suspendModifier = if (function.isSuspend) "suspend " else ""

    appendLine("    fun ${functionName}(")
    appendLine("        behavior: $suspendModifier($parameterTypes) -> $returnType")
    appendLine("    ) {")
    appendLine("        fake.configure${functionName.capitalize()}(behavior)")
    appendLine("    }")
}
```

**Example Output**:
```kotlin
// Non-generic:
class FakeUserServiceConfig(
    private val fake: FakeUserServiceImpl
) {
    fun getUser(behavior: (String) -> User) {
        fake.configureGetUser(behavior)
    }
}

// Generic:
class FakeRepositoryConfig<T>(
    private val fake: FakeRepositoryImpl<T>
) {
    fun save(behavior: (T) -> T) {
        fake.configureSave(behavior)
    }

    fun findById(behavior: (String) -> T?) {
        fake.configureFindById(behavior)
    }
}
```

**Test**:
```kotlin
@Test
fun `GIVEN generic interface WHEN generating config DSL THEN should preserve type parameters`() = runTest {
    // Given
    val analysis = createTestAnalysis("Repository", typeParameters = listOf("T"))
    val generator = ConfigurationDslGenerator(TypeResolver())

    // When
    val result = generator.generateConfigurationDsl(analysis, "FakeRepositoryImpl")

    // Then
    assertTrue(result.contains("class FakeRepositoryConfig<T>"))
    assertTrue(result.contains("private val fake: FakeRepositoryImpl<T>"))
    assertTrue(result.contains("(T) -> T"))
}
```

---

### Task 2.4: Integration with CodeGenerator (Day 4-5)

**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/codegen/CodeGenerator.kt`

**Changes**: Minimal, should work automatically since we're using the existing `generators` object.

Verify that:
1. Generated code uses new generator methods
2. Type parameters flow through correctly
3. Imports are resolved for generic types

**Integration Test**:
```kotlin
@Test
fun `GIVEN generic interface WHEN generating all code THEN should produce compilable output`() = runTest {
    // Given
    val analysis = createTestAnalysis("Repository", typeParameters = listOf("T"))
    val codeGenerator = CodeGenerator(/* dependencies */)

    // When
    codeGenerator.generateWorkingFakeImplementation(
        sourceInterface = mockInterface,
        analysis = analysis,
        moduleFragment = mockModuleFragment
    )

    // Then
    // Verify file was created
    val generatedFile = File("path/to/generated/FakeRepositoryImpl.kt")
    assertTrue(generatedFile.exists())

    // Verify content
    val content = generatedFile.readText()
    assertTrue(content.contains("class FakeRepositoryImpl<T>"))
    assertTrue(content.contains("inline fun <reified T> fakeRepository"))
    assertTrue(content.contains("class FakeRepositoryConfig<T>"))
}
```

---

## ✅ Phase 2 Completion Criteria

- [ ] ImplementationGenerator generates `class Fake<T> : Interface<T>`
- [ ] FactoryGenerator generates `inline fun <reified T> fakeFoo()`
- [ ] ConfigurationDslGenerator generates `class FakeConfig<T>`
- [ ] All unit tests pass for generators
- [ ] Integration test produces compilable generic code
- [ ] Code review: verify type parameter flow

## 📊 Progress Tracking

- Task 2.1: Update ImplementationGenerator
- Task 2.2: Update FactoryGenerator
- Task 2.3: Update ConfigurationDslGenerator
- Task 2.4: CodeGenerator integration

## 🔗 Next Steps

After Phase 2 completion, proceed to [Phase 3: Testing & Integration](./phase3-testing-integration.md)
