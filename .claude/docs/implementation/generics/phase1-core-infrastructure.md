# Phase 1: Core Infrastructure (Week 1)

> **Goal**: Build GenericIrSubstitutor and integrate IrTypeSubstitutor APIs
> **Duration**: 5-7 days
> **Prerequisites**: Understanding of Kotlin IR type system

## 📋 Tasks Breakdown

### Task 1.1: Create GenericIrSubstitutor.kt (Day 1-2)

**Location**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/GenericIrSubstitutor.kt`

**Implementation**:

```kotlin
package com.rsicarelli.fakt.compiler.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.IrTypeParameterRemapper
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * Handles generic type substitution using Kotlin IR APIs.
 *
 * Core responsibilities:
 * 1. Build substitution maps from interface type parameters
 * 2. Apply IrTypeSubstitutor to class-level generics
 * 3. Use IrTypeParameterRemapper for method-level generics
 * 4. Handle mixed generics (class + method level)
 *
 * Metro pattern alignment: Similar to Metro's dependency injection type resolution
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
class GenericIrSubstitutor(private val pluginContext: IrPluginContext) {

    /**
     * Creates a substitution map from original interface to its instantiated supertype.
     *
     * Example:
     * ```
     * interface Repository<T>
     * class FakeRepositoryImpl : Repository<String>
     *
     * Map: T -> String
     * ```
     *
     * @param originalInterface The interface with type parameters
     * @param superType The concrete supertype with type arguments
     * @return Map of type parameter symbols to their substituted arguments
     */
    fun createSubstitutionMap(
        originalInterface: IrClass,
        superType: IrSimpleType
    ): Map<IrTypeParameterSymbol, IrTypeArgument> {
        require(originalInterface.typeParameters.size == superType.arguments.size) {
            "Type parameter count mismatch: ${originalInterface.typeParameters.size} vs ${superType.arguments.size}"
        }

        return originalInterface.typeParameters
            .zip(superType.arguments)
            .associate { (param, arg) -> param.symbol to arg }
    }

    /**
     * Creates an IrTypeSubstitutor for class-level generics.
     *
     * @param substitutionMap Map from createSubstitutionMap()
     * @return IrTypeSubstitutor that can substitute types
     */
    fun createClassLevelSubstitutor(
        substitutionMap: Map<IrTypeParameterSymbol, IrTypeArgument>
    ): IrTypeSubstitutor {
        return IrTypeSubstitutor(
            typeParameters = substitutionMap.keys.toList(),
            typeArguments = substitutionMap.values.toList(),
            pluginContext.irBuiltIns
        )
    }

    /**
     * Substitutes a function signature with class-level and method-level generics.
     *
     * Handles two-level substitution:
     * 1. Class-level: T from Repository<T>
     * 2. Method-level: R from fun <R> transform()
     *
     * @param originalFunction The function to substitute
     * @param classLevelSubstitutor Substitutor for class-level type parameters
     * @return New function with substituted types
     */
    fun substituteFunction(
        originalFunction: IrSimpleFunction,
        classLevelSubstitutor: IrTypeSubstitutor
    ): IrSimpleFunction {
        val irFactory = pluginContext.irFactory

        // Step 1: Create new function with substituted return type
        val substitutedReturnType = classLevelSubstitutor.substitute(originalFunction.returnType)

        val newFunction = irFactory.buildFun {
            name = originalFunction.name
            returnType = substitutedReturnType
            isSuspend = originalFunction.isSuspend
            isInline = originalFunction.isInline
            // Copy other properties
        }

        // Step 2: Handle method-level type parameters
        if (originalFunction.typeParameters.isNotEmpty()) {
            val newTypeParameters = originalFunction.typeParameters.map { oldTp ->
                irFactory.createTypeParameter(
                    startOffset = oldTp.startOffset,
                    endOffset = oldTp.endOffset,
                    origin = oldTp.origin,
                    name = oldTp.name,
                    symbol = oldTp.symbol, // Reuse symbol or create new
                    variance = oldTp.variance,
                    index = oldTp.index,
                    isReified = oldTp.isReified
                ).also { newTp ->
                    newTp.parent = newFunction

                    // Apply class-level substitution to method type parameter constraints
                    newTp.superTypes = oldTp.superTypes.map { constraint ->
                        classLevelSubstitutor.substitute(constraint)
                    }
                }
            }
            newFunction.typeParameters += newTypeParameters

            // Step 3: Create remapper for method-level type parameters
            val remapper = IrTypeParameterRemapper(
                originalFunction.typeParameters.zip(newTypeParameters).toMap()
            )

            // Step 4: Apply both substitution and remapping to value parameters
            newFunction.valueParameters += originalFunction.valueParameters.map { oldVp ->
                val substitutedType = classLevelSubstitutor.substitute(oldVp.type)
                val remappedType = remapper.remapType(substitutedType)

                irFactory.createValueParameter(
                    startOffset = oldVp.startOffset,
                    endOffset = oldVp.endOffset,
                    origin = oldVp.origin,
                    name = oldVp.name,
                    type = remappedType,
                    isAssignable = oldVp.isAssignable,
                    symbol = oldVp.symbol,
                    index = oldVp.index,
                    varargElementType = oldVp.varargElementType,
                    isCrossinline = oldVp.isCrossinline,
                    isNoinline = oldVp.isNoinline,
                    isHidden = oldVp.isHidden
                ).also { it.parent = newFunction }
            }
        } else {
            // No method-level generics, just substitute value parameters
            newFunction.valueParameters += originalFunction.valueParameters.map { oldVp ->
                val substitutedType = classLevelSubstitutor.substitute(oldVp.type)

                irFactory.createValueParameter(
                    startOffset = oldVp.startOffset,
                    endOffset = oldVp.endOffset,
                    origin = oldVp.origin,
                    name = oldVp.name,
                    type = substitutedType,
                    isAssignable = oldVp.isAssignable,
                    symbol = oldVp.symbol,
                    index = oldVp.index,
                    varargElementType = oldVp.varargElementType,
                    isCrossinline = oldVp.isCrossinline,
                    isNoinline = oldVp.isNoinline,
                    isHidden = oldVp.isHidden
                ).also { it.parent = newFunction }
            }
        }

        return newFunction
    }

    /**
     * Substitutes a property signature with class-level generics.
     *
     * @param originalProperty The property to substitute
     * @param classLevelSubstitutor Substitutor for class-level type parameters
     * @return Property metadata with substituted type
     */
    fun substituteProperty(
        originalProperty: IrProperty,
        classLevelSubstitutor: IrTypeSubstitutor
    ): PropertySubstitutionResult {
        val originalType = originalProperty.getter?.returnType
            ?: originalProperty.backingField?.type
            ?: error("Property ${originalProperty.name} has no determinable type")

        val substitutedType = classLevelSubstitutor.substitute(originalType)

        return PropertySubstitutionResult(
            name = originalProperty.name.asString(),
            originalType = originalType,
            substitutedType = substitutedType,
            isMutable = originalProperty.isVar,
            isNullable = substitutedType.isMarkedNullable()
        )
    }

    /**
     * Detects if a type parameter has recursive constraints.
     * Example: interface Node<T : Node<T>>
     */
    fun isRecursiveGeneric(typeParam: IrTypeParameter): Boolean {
        return typeParam.superTypes.any { superType ->
            containsTypeParameter(superType, typeParam.symbol)
        }
    }

    /**
     * Checks if an IrType contains a reference to a specific type parameter.
     */
    private fun containsTypeParameter(
        irType: IrType,
        targetSymbol: IrTypeParameterSymbol
    ): Boolean {
        return when (irType) {
            is IrSimpleType -> {
                if (irType.classifier == targetSymbol) return true
                irType.arguments.any { arg ->
                    arg is IrTypeProjection && containsTypeParameter(arg.type, targetSymbol)
                }
            }
            else -> false
        }
    }

    /**
     * Handles recursive generics by using upper bound as fallback.
     */
    fun resolveRecursiveGeneric(typeParam: IrTypeParameter): IrType {
        // Use first non-recursive upper bound or Any
        return typeParam.superTypes.firstOrNull {
            !containsTypeParameter(it, typeParam.symbol)
        } ?: pluginContext.irBuiltIns.anyType
    }

    /**
     * Resolves star projections based on variance.
     */
    fun resolveStarProjection(
        typeParam: IrTypeParameter,
        variance: Variance
    ): IrType {
        return when (variance) {
            Variance.OUT_VARIANCE -> typeParam.superTypes.firstOrNull()
                ?: pluginContext.irBuiltIns.anyType
            Variance.IN_VARIANCE -> pluginContext.irBuiltIns.nothingType
            else -> typeParam.superTypes.firstOrNull()
                ?: pluginContext.irBuiltIns.anyType
        }
    }
}

/**
 * Result of property type substitution.
 */
data class PropertySubstitutionResult(
    val name: String,
    val originalType: IrType,
    val substitutedType: IrType,
    val isMutable: Boolean,
    val isNullable: Boolean
)
```

**Test File**: `compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/ir/GenericIrSubstitutorTest.kt`

```kotlin
package com.rsicarelli.fakt.compiler.ir

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenericIrSubstitutorTest {

    @Test
    fun `GIVEN interface with single type parameter WHEN creating substitution map THEN should map correctly`() {
        // Given - create test interface Repository<T>
        val mockInterface = createMockInterface("Repository", listOf("T"))
        val mockSuperType = createMockSuperType(listOf("String"))
        val substitutor = GenericIrSubstitutor(mockPluginContext)

        // When
        val substitutionMap = substitutor.createSubstitutionMap(mockInterface, mockSuperType)

        // Then
        assertEquals(1, substitutionMap.size)
        assertNotNull(substitutionMap.entries.first())
    }

    @Test
    fun `GIVEN function with class-level generic WHEN substituting THEN should preserve type`() {
        // Given
        val mockFunction = createMockFunction("save", paramType = "T", returnType = "T")
        val mockSubstitutor = createMockSubstitutor(mapOf("T" to "String"))
        val substitutor = GenericIrSubstitutor(mockPluginContext)

        // When
        val result = substitutor.substituteFunction(mockFunction, mockSubstitutor)

        // Then
        assertNotNull(result)
        // Validate result.returnType is String
    }

    // More tests following GIVEN-WHEN-THEN pattern
}
```

---

### Task 1.2: Enhance TypeResolver.kt (Day 2-3)

**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/types/TypeResolver.kt`

**Changes**:

1. Add new method:
```kotlin
/**
 * Converts IrType to Kotlin string with generic substitution support.
 */
fun irTypeToKotlinStringWithSubstitution(
    irType: IrType,
    substitutor: IrTypeSubstitutor?
): String {
    val substitutedType = substitutor?.substitute(irType) ?: irType
    return irTypeToKotlinString(substitutedType, preserveTypeParameters = true)
}
```

2. Update line 118-124 to preserve type parameters:
```kotlin
// Handle type parameters (T, K, V, etc.)
irType is IrSimpleType && irType.classifier.owner is IrTypeParameter -> {
    val typeParam = irType.classifier.owner as IrTypeParameter
    // Always preserve for generics support
    typeParam.name.asString()
}
```

---

### Task 1.3: Remove Generic Filter (Day 3)

**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/UnifiedFaktIrGenerationExtension.kt`

**Delete lines 189-193**:
```kotlin
// DELETE THIS BLOCK:
interfaceAnalyzer.checkGenericSupport(fakeInterface) != null -> {
    val genericError = interfaceAnalyzer.checkGenericSupport(fakeInterface)
    messageCollector?.reportInfo("Fakt: Skipping generic interface: $genericError")
    null
}
```

**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/analysis/InterfaceAnalyzer.kt`

**Update method to return null (allow generics)**:
```kotlin
fun checkGenericSupport(irClass: IrClass): String? = null // Allow all generics
```

---

### Task 1.4: Integration Test (Day 4-5)

**Test**: Compile a simple generic interface and verify it's not skipped

```kotlin
@Test
fun `GIVEN simple generic interface WHEN compiling THEN should generate fake`() = runTest {
    val compilation = KotlinCompilation().apply {
        sources = listOf(
            SourceFile.kotlin("Repository.kt", """
                package test
                import com.rsicarelli.fakt.Fake

                @Fake
                interface Repository<T> {
                    fun save(item: T): T
                }
            """)
        )
        compilerPlugins = listOf(FaktCompilerPluginRegistrar())
    }

    val result = compilation.compile()
    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
}
```

---

## ✅ Phase 1 Completion Criteria

- [ ] GenericIrSubstitutor.kt created with all methods
- [ ] Unit tests for GenericIrSubstitutor passing
- [ ] TypeResolver enhanced with substitution methods
- [ ] Generic filter removed (interfaces no longer skipped)
- [ ] Integration test passes (simple Repository<T> compiles)
- [ ] Code review with Metro pattern alignment check

## 📊 Progress Tracking

Track progress in todo list:
- Task 1.1: Create GenericIrSubstitutor
- Task 1.2: Enhance TypeResolver
- Task 1.3: Remove generic filter
- Task 1.4: Integration test

## 🔗 Next Steps

After Phase 1 completion, proceed to [Phase 2: Code Generation](./phase2-code-generation.md)
