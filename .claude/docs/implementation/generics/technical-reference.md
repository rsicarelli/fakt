# IrTypeSubstitutor - Technical Reference

> **Purpose**: Deep dive into Kotlin IR type substitution APIs
> **Audience**: Developers implementing generic support
> **Source**: kotlin/compiler/ir/backend.common/

## 🎯 Core Concepts

### What is Type Substitution?

Type substitution is the process of replacing type parameters with concrete types in generic code.

```kotlin
// Before substitution (generic):
interface Repository<T> {
    fun save(item: T): T
}

// After substitution (concrete):
interface UserRepository : Repository<User> {
    fun save(item: User): User // T → User
}
```

In Fakt's case, we don't create concrete implementations manually—we generate them at compile time using IR APIs.

---

## 📚 Key Kotlin IR APIs

### 1. IrTypeSubstitutor

**Location**: `org.jetbrains.kotlin.ir.types.IrTypeSubstitutor`

**Purpose**: Substitutes type parameters in IR types

**Construction**:
```kotlin
val substitutor = IrTypeSubstitutor(
    typeParameters: List<IrTypeParameterSymbol>,  // Type params to substitute
    typeArguments: List<IrTypeArgument>,          // Concrete types to use
    irBuiltIns: IrBuiltIns                        // Access to built-in types
)
```

**Usage**:
```kotlin
// Given: interface Repository<T> { fun save(item: T): T }
// Want: Repository<User> where T → User

val originalFunction: IrSimpleFunction // save(item: T): T
val substitutor = createSubstitutor(mapOf(T -> User))

val substitutedReturnType = substitutor.substitute(originalFunction.returnType)
// Result: User (was T)

val substitutedParamType = substitutor.substitute(param.type)
// Result: User (was T)
```

**Key Methods**:
- `substitute(irType: IrType): IrType` - Main substitution method
- Handles nested types recursively
- Preserves nullability, variance

---

### 2. IrTypeParameterRemapper

**Location**: `org.jetbrains.kotlin.ir.util.IrTypeParameterRemapper`

**Purpose**: Remaps type parameters when copying declarations (for method-level generics)

**Construction**:
```kotlin
val remapper = IrTypeParameterRemapper(
    oldToNewTypeParameters: Map<IrTypeParameter, IrTypeParameter>
)
```

**Use Case**: When a function has both class-level and method-level generics

```kotlin
interface Service<T> {
    fun <R> transform(item: T): R
    //   ^^^^^          ^^^     ^^^
    //   Method-level   Class   Method
}

// Process:
// 1. Use IrTypeSubstitutor for T (class-level)
// 2. Use IrTypeParameterRemapper for R (method-level)
```

**Example**:
```kotlin
// Original method: fun <R> transform(item: T): R
val newTypeParameter = irFactory.createTypeParameter(...)
val remapper = IrTypeParameterRemapper(mapOf(oldR -> newTypeParameter))

val remappedType = remapper.remapType(substitutedType)
```

---

### 3. IrTypeArgument & IrTypeProjection

**Purpose**: Represents actual type arguments in generic types

```kotlin
// List<String>
//      ^^^^^^ - IrTypeProjection

when (val argument: IrTypeArgument) {
    is IrTypeProjection -> {
        val type: IrType = argument.type // The actual type (String)
        val variance: Variance = argument.variance // INVARIANT/OUT/IN
    }
    is IrStarProjection -> {
        // List<*> - wildcard type
    }
}
```

**Variance Types**:
- `INVARIANT` - `List<T>` (exact type)
- `OUT_VARIANCE` - `Producer<out T>` (covariant)
- `IN_VARIANCE` - `Consumer<in T>` (contravariant)

---

## 🔧 Implementation Patterns

### Pattern 1: Class-Level Generics Only

```kotlin
// Input: interface Repository<T>
// Output: class FakeRepositoryImpl<T> : Repository<T>

fun generateGenericFake(irClass: IrClass): IrClass {
    // Step 1: Extract type parameters from interface
    val typeParameters = irClass.typeParameters

    // Step 2: Create substitution map (T -> T, keep generic)
    // For generation, we DON'T substitute - we preserve!

    // Step 3: Generate class with same type parameters
    val fakeClass = irFactory.buildClass {
        name = Name.identifier("Fake${irClass.name}Impl")
        kind = ClassKind.CLASS
        // Copy type parameters to fake class
        this.typeParameters = typeParameters.map { copyTypeParameter(it) }
    }

    // Step 4: Add supertype with type parameters
    val superType = irClass.defaultType.withTypeParameters(typeParameters)
    fakeClass.superTypes += superType

    return fakeClass
}
```

**Key Insight**: For code generation, we DON'T substitute generics—we preserve them!

---

### Pattern 2: Mixed Generics (Class + Method)

```kotlin
// Input: interface Service<T> { fun <R> transform(item: T): R }
// Output: class FakeServiceImpl<T> { override fun <R> transform(item: T): R }

fun generateMethodWithMixedGenerics(
    originalFunction: IrSimpleFunction,
    classTy peParameters: List<IrTypeParameter>
): IrSimpleFunction {
    // Step 1: Create new function
    val newFunction = irFactory.buildFun { ... }

    // Step 2: Copy class-level type parameters (already available in scope)
    // No substitution needed - they're inherited from class

    // Step 3: Copy method-level type parameters
    val newMethodTypeParams = originalFunction.typeParameters.map { oldTp ->
        irFactory.createTypeParameter(
            name = oldTp.name,
            variance = oldTp.variance,
            index = oldTp.index,
            isReified = oldTp.isReified
        ).also { newTp ->
            newTp.parent = newFunction
            // Copy constraints
            newTp.superTypes = oldTp.superTypes
        }
    }
    newFunction.typeParameters += newMethodTypeParams

    // Step 4: Create remapper for method type parameters
    val remapper = IrTypeParameterRemapper(
        originalFunction.typeParameters.zip(newMethodTypeParams).toMap()
    )

    // Step 5: Copy parameters with remapping
    newFunction.valueParameters += originalFunction.valueParameters.map { oldParam ->
        val remappedType = remapper.remapType(oldParam.type)
        irFactory.createValueParameter(
            name = oldParam.name,
            type = remappedType,
            // ... other properties
        )
    }

    // Step 6: Remap return type
    newFunction.returnType = remapper.remapType(originalFunction.returnType)

    return newFunction
}
```

---

### Pattern 3: Handling Constraints

```kotlin
// Input: interface Service<T : Number>
// Output: Preserve constraint in generated code

fun copyTypeParameterWithConstraints(
    original: IrTypeParameter,
    parent: IrDeclaration
): IrTypeParameter {
    val newTp = irFactory.createTypeParameter(
        name = original.name,
        variance = original.variance,
        index = original.index,
        isReified = original.isReified
    )

    newTp.parent = parent

    // KEY: Copy all superTypes (constraints)
    newTp.superTypes = original.superTypes.map { constraint ->
        // If constraint references other type parameters, remap them
        remapConstraintIfNeeded(constraint)
    }

    return newTp
}
```

---

## 🚨 Common Pitfalls

### Pitfall 1: Forgetting to Set Parent

```kotlin
❌ val newTp = irFactory.createTypeParameter(...)
   // Missing: newTp.parent = ...
   // Result: Compiler crash!

✅ val newTp = irFactory.createTypeParameter(...).also {
       it.parent = newFunction
   }
```

**Why**: IR nodes form a tree. Every node needs a parent for context.

---

### Pitfall 2: Not Copying Constraints

```kotlin
❌ newTp.superTypes = emptyList()
   // Lost constraint: T : Number

✅ newTp.superTypes = oldTp.superTypes.map { ... }
```

---

### Pitfall 3: Mixing Up Substitution vs Preservation

```kotlin
// For ANALYSIS (understanding existing code):
val substitutor = IrTypeSubstitutor(...)
val concreteType = substitutor.substitute(genericType) // T -> User

// For GENERATION (creating new code):
// DON'T substitute - preserve generics!
val newClass = buildClass {
    typeParameters = original.typeParameters // Keep T as T
}
```

**Key Distinction**:
- **Analysis**: Substitute to understand concrete usage
- **Generation**: Preserve to maintain generics

---

## 📊 Decision Tree: When to Use What

```
Is this class-level generic? (interface Foo<T>)
├─ YES → Preserve type parameters in generated class
│         Don't use IrTypeSubstitutor for generation
│         Use for type string conversion only
│
└─ NO → Check for method-level generics
    ├─ YES → Use IrTypeParameterRemapper
    │         Copy method type parameters
    │         Remap parameter/return types
    │
    └─ NO → Simple case, no special handling

Does method have BOTH class and method generics?
└─ YES → Two-step process:
          1. Class-level: Already in scope
          2. Method-level: Use IrTypeParameterRemapper
```

---

## 🔍 Debugging Tips

### Tip 1: Inspect IR with --dump-ir

Add to compiler options:
```kotlin
-Xdump-ir=afterFaktGeneration
```

View generated IR structure to debug type parameter issues.

---

### Tip 2: Use TypeResolver for String Conversion

```kotlin
// For debugging: convert IrType to readable Kotlin string
val typeString = typeResolver.irTypeToKotlinString(
    irType,
    preserveTypeParameters = true
)
println("Type: $typeString") // "Repository<T>" not "Repository<Any>"
```

---

### Tip 3: Validate Parent Chain

```kotlin
fun validateIrNode(node: IrDeclaration) {
    var current: IrDeclaration? = node
    while (current != null) {
        println("${current::class.simpleName}: ${current.name}")
        current = current.parent as? IrDeclaration
    }
}
```

---

## 📚 Reference Implementation: Metro

Metro DI framework uses similar patterns for generic injection:

**File**: `metro/compiler/src/.../ir/MetroIrGenerationExtension.kt`

```kotlin
// Metro's approach to generic dependencies
private fun generateGenericProvider(
    dependency: IrClass,
    typeArguments: List<IrType>
) {
    val substitutor = IrTypeSubstitutor(
        typeParameters = dependency.typeParameters.map { it.symbol },
        typeArguments = typeArguments,
        irBuiltIns
    )

    // Substitute types in provider function
    val providerReturnType = substitutor.substitute(dependency.defaultType)
}
```

**Key Learnings**:
1. Metro uses IrTypeSubstitutor for dependency resolution
2. Validates type arguments at compile time
3. Generates type-safe provider functions

---

## 🔗 Official Documentation

### Kotlin Compiler Source (Local)
- `kotlin/compiler/ir/backend.common/src/org/jetbrains/kotlin/ir/types/IrTypeSubstitutor.kt`
- `kotlin/compiler/ir/backend.common/src/org/jetbrains/kotlin/ir/util/IrTypeParameterRemapper.kt`
- `kotlin/compiler/ir/tree/src/org/jetbrains/kotlin/ir/types/IrType.kt`

### Query Kotlin APIs
Use `/consult-kotlin-api IrTypeSubstitutor` to validate current API version.

---

## 💡 Quick Reference Cheat Sheet

```kotlin
// ============================================================================
// COMMON OPERATIONS
// ============================================================================

// 1. Create type parameter
val typeParam = irFactory.createTypeParameter(
    name = Name.identifier("T"),
    variance = Variance.INVARIANT,
    index = 0,
    isReified = false
).also {
    it.parent = parentClass
    it.superTypes = listOf(irBuiltIns.anyType) // Upper bound
}

// 2. Create substitution map
val substitutionMap = originalInterface.typeParameters
    .zip(concreteTypeArguments)
    .associate { (param, arg) -> param.symbol to arg }

// 3. Create substitutor
val substitutor = IrTypeSubstitutor(
    typeParameters = substitutionMap.keys.toList(),
    typeArguments = substitutionMap.values.toList(),
    irBuiltIns
)

// 4. Substitute a type
val substitutedType = substitutor.substitute(originalType)

// 5. Create type parameter remapper
val remapper = IrTypeParameterRemapper(
    oldTypeParams.zip(newTypeParams).toMap()
)

// 6. Remap a type
val remappedType = remapper.remapType(typeWithOldParams)

// 7. Check if type is generic
fun IrType.isGeneric(): Boolean =
    this is IrSimpleType && this.classifier.owner is IrTypeParameter

// 8. Get type arguments from IrSimpleType
val typeArgs: List<IrTypeArgument> = (irType as IrSimpleType).arguments
```

---

## 🎓 Learning Path

1. **Start**: Read this reference
2. **Explore**: Check Metro implementation
3. **Experiment**: Create simple GenericIrSubstitutor
4. **Test**: Write tests for single type parameter
5. **Expand**: Handle multiple parameters, constraints
6. **Master**: Mixed generics, edge cases

**Estimated Time**: 2-3 days to become proficient

---

This reference should be consulted throughout Phase 1 implementation!
