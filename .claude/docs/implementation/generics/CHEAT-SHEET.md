# Generic Support - Developer Cheat Sheet

> **Purpose**: Quick reference during implementation
> **Use When**: Stuck? Need quick answer? Check here first!

## 🔥 Most Common Operations

### Create Type Parameter

```kotlin
val typeParam = irFactory.createTypeParameter(
    startOffset = UNDEFINED_OFFSET,
    endOffset = UNDEFINED_OFFSET,
    origin = IrDeclarationOrigin.DEFINED,
    name = Name.identifier("T"),
    symbol = IrTypeParameterSymbolImpl(),
    variance = Variance.INVARIANT,
    index = 0,
    isReified = false
).also {
    it.parent = parentClass // ⚠️ CRITICAL: Always set parent!
    it.superTypes = listOf(pluginContext.irBuiltIns.anyType) // Upper bound
}
```

### Create Substitution Map

```kotlin
fun createSubstitutionMap(
    originalInterface: IrClass,
    superType: IrSimpleType
): Map<IrTypeParameterSymbol, IrTypeArgument> {
    require(originalInterface.typeParameters.size == superType.arguments.size)

    return originalInterface.typeParameters
        .zip(superType.arguments)
        .associate { (param, arg) -> param.symbol to arg }
}
```

### Create IrTypeSubstitutor

```kotlin
val substitutor = IrTypeSubstitutor(
    typeParameters = substitutionMap.keys.toList(),
    typeArguments = substitutionMap.values.toList(),
    pluginContext.irBuiltIns
)

// Use it:
val substitutedType = substitutor.substitute(originalType)
```

### Create Type Parameter Remapper

```kotlin
val remapper = IrTypeParameterRemapper(
    originalTypeParams.zip(newTypeParams).toMap()
)

// Use it:
val remappedType = remapper.remapType(typeWithOldParams)
```

---

## 🎯 Common Patterns

### Pattern: Preserve Generics (for code generation)

```kotlin
// ✅ CORRECT: Preserve type parameters
val typeParams = if (analysis.typeParameters.isNotEmpty()) {
    "<${analysis.typeParameters.joinToString(", ")}>"
} else ""
appendLine("class $fakeClassName$typeParams : $interfaceName$typeParams")

// ❌ WRONG: Type erasure
appendLine("class $fakeClassName : $interfaceName<Any>")
```

### Pattern: Type Parameter in Function

```kotlin
// For: fun <R> transform(item: T): R

// Step 1: Create new type parameter
val newR = irFactory.createTypeParameter(...)
newR.parent = newFunction

// Step 2: Add to function
newFunction.typeParameters += newR

// Step 3: Use in signature
newFunction.returnType = newR.defaultType
```

### Pattern: Copy with Constraints

```kotlin
fun copyTypeParameterWithConstraints(
    original: IrTypeParameter,
    parent: IrDeclaration
): IrTypeParameter {
    return irFactory.createTypeParameter(...).also { newTp ->
        newTp.parent = parent
        newTp.superTypes = original.superTypes // Copy constraints!
    }
}
```

---

## 🚨 Common Mistakes & Fixes

### Mistake 1: Forgot to Set Parent

```kotlin
❌ val newTp = irFactory.createTypeParameter(...)
   // Result: Compiler crash!

✅ val newTp = irFactory.createTypeParameter(...).also {
       it.parent = newFunction
   }
```

### Mistake 2: Lost Constraints

```kotlin
❌ newTp.superTypes = emptyList()
   // Lost: T : Number

✅ newTp.superTypes = oldTp.superTypes.map { constraint ->
       classLevelSubstitutor.substitute(constraint)
   }
```

### Mistake 3: Wrong String Representation

```kotlin
❌ typeResolver.irTypeToKotlinString(irType, preserveTypeParameters = false)
   // Result: "Any" instead of "T"

✅ typeResolver.irTypeToKotlinString(irType, preserveTypeParameters = true)
   // Result: "T"
```

### Mistake 4: Type Erasure in Generation

```kotlin
❌ // Substituting generics when generating (wrong!)
   val substitutor = IrTypeSubstitutor(...)
   val concreteType = substitutor.substitute(T) // T -> Any

✅ // Preserve generics when generating
   val typeParams = analysis.typeParameters // Keep as ["T"]
```

---

## 📋 Quick Checklist

### Before Writing Code
- [ ] Read [QUICK-START.md](./QUICK-START.md)?
- [ ] Understand current task?
- [ ] Test exists (TDD)?

### While Writing Code
- [ ] Parent set on all IR nodes?
- [ ] Type parameters preserved (not erased)?
- [ ] Constraints copied?
- [ ] Test follows GIVEN-WHEN-THEN?

### Before Committing
- [ ] All tests pass?
- [ ] Code formatted (spotlessApply)?
- [ ] No compilation warnings?
- [ ] Metro pattern aligned?

---

## 🧪 Test Template

```kotlin
@Test
fun `GIVEN [scenario] WHEN [action] THEN [expected]`() = runTest {
    // Given - Create isolated instances
    val analysis = createTestAnalysis("Repository", typeParameters = listOf("T"))
    val generator = ImplementationGenerator(TypeResolver())

    // When
    val result = generator.generateImplementation(analysis, "FakeRepositoryImpl")

    // Then
    assertTrue(result.contains("class FakeRepositoryImpl<T>"))
    assertTrue(result.contains("(T) -> T"))
}
```

### Multi-Stage Validation Template

```kotlin
@Test
fun `GIVEN generic interface WHEN using fake THEN type safe`() {
    // Stage 1: Generation
    val genResult = KotlinCompilation().apply {
        sources = listOf(SourceFile.kotlin("Test.kt", """
            @Fake interface Repository<T> { fun save(item: T): T }
        """))
    }.compile()
    assertEquals(KotlinCompilation.ExitCode.OK, genResult.exitCode)

    // Stage 2: Structure validation
    val fakeClass = genResult.classLoader.loadClass("FakeRepositoryImpl")
    assertEquals(1, fakeClass.typeParameters.size)

    // Stage 3: Use-site type safety (CRITICAL!)
    val useResult = compileWithClasspath(genResult.classpaths, """
        val repo = fakeRepository<User> {}
        val user: User = repo.save(User("test"))
    """)
    assertEquals(KotlinCompilation.ExitCode.OK, useResult.exitCode)
}
```

---

## 🔍 Debug Commands

### Check Generated Code

```bash
# View generated file
cat build/generated/fakt/test/kotlin/FakeRepositoryImpl.kt

# Check if generics preserved
grep "class Fake.*<" build/generated/fakt/test/kotlin/FakeRepositoryImpl.kt

# Check factory function
grep "inline fun <reified" build/generated/fakt/test/kotlin/FakeRepositoryImpl.kt
```

### Compilation Logs

```bash
# Enable Fakt debug logging
make debug

# Or with Gradle:
./gradlew :samples:single-module:build --info | grep Fakt
```

### Validate with Commands

```bash
# Check Kotlin API compatibility
/consult-kotlin-api IrTypeSubstitutor

# Verify Metro alignment
/validate-metro-alignment

# Run specific tests
./gradlew :compiler:test --tests "*Generic*"
```

---

## 📊 File Locations Quick Reference

### Source Files
```
compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/
├── ir/
│   ├── UnifiedFaktIrGenerationExtension.kt  # Line 189: Remove filter
│   ├── GenericIrSubstitutor.kt              # NEW: Create this
│   └── analysis/
│       ├── InterfaceAnalyzer.kt             # Update checkGenericSupport
│       └── GenericPatternAnalyzer.kt        # Already exists ✅
├── codegen/
│   ├── ImplementationGenerator.kt           # Update: lines 40-49
│   ├── FactoryGenerator.kt                  # Rewrite: reified params
│   └── ConfigurationDslGenerator.kt         # Update: generic DSL
└── types/
    └── TypeResolver.kt                       # Update: lines 118-124
```

### Test Files
```
compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/
├── GenericFakeGenerationTest.kt             # NEW: Create this
└── ir/
    └── GenericIrSubstitutorTest.kt          # NEW: Create this
```

### Documentation
```
.claude/docs/implementation/generics/
├── README.md                  # Index
├── QUICK-START.md            # Start here
├── ROADMAP.md                # Overview
├── CHEAT-SHEET.md            # This file
├── phase1-core-infrastructure.md
├── phase2-code-generation.md
├── phase3-testing-integration.md
├── test-matrix.md
├── technical-reference.md
└── CHANGELOG.md              # Track progress
```

---

## 💡 Quick Answers

### Q: How to check if type is generic?

```kotlin
fun IrType.isGeneric(): Boolean =
    this is IrSimpleType && this.classifier.owner is IrTypeParameter
```

### Q: How to get type parameter name?

```kotlin
val typeParam = irType.classifier.owner as IrTypeParameter
val name = typeParam.name.asString() // "T"
```

### Q: How to handle star projections?

```kotlin
when (val arg: IrTypeArgument) {
    is IrTypeProjection -> arg.type // Concrete type
    is IrStarProjection -> pluginContext.irBuiltIns.anyNullableType // Fallback
}
```

### Q: How to detect recursive generics?

```kotlin
fun isRecursiveGeneric(typeParam: IrTypeParameter): Boolean {
    return typeParam.superTypes.any { superType ->
        containsTypeParameter(superType, typeParam.symbol)
    }
}
```

### Q: What's the difference between substitution and remapping?

**Substitution** (`IrTypeSubstitutor`):
- For class-level generics
- Replaces type parameters with concrete types
- Example: T → User

**Remapping** (`IrTypeParameterRemapper`):
- For method-level generics
- Remaps old type parameter to new type parameter
- Example: old<R> → new<R>

### Q: When to use what?

```
Class-level only (Repository<T>)
└─> Preserve type params, no substitution

Method-level only (fun <R> transform())
└─> Use IrTypeParameterRemapper

Mixed (Service<T> { fun <R> map() })
└─> Preserve class-level + remapper for method-level
```

---

## 🎯 Success Indicators

### You're On Track If:
- ✅ Generic interfaces not skipped in logs
- ✅ Generated code contains `<T>` (not `<Any>`)
- ✅ Factory functions have `reified`
- ✅ Tests follow GIVEN-WHEN-THEN
- ✅ Multi-stage validation passing

### Red Flags:
- ❌ Generated code uses `Any` everywhere
- ❌ Type parameters disappeared
- ❌ Manual casting required
- ❌ Compiler crashes on generic interfaces
- ❌ Tests don't validate use-site type safety

---

## 🔗 Quick Links

| Need | Link |
|------|------|
| Start implementing | [QUICK-START.md](./QUICK-START.md) |
| API reference | [technical-reference.md](./technical-reference.md) |
| Test examples | [test-matrix.md](./test-matrix.md) |
| Phase details | [phase1](./phase1-core-infrastructure.md), [phase2](./phase2-code-generation.md), [phase3](./phase3-testing-integration.md) |
| Track progress | [CHANGELOG.md](./CHANGELOG.md) |
| Overview | [ROADMAP.md](./ROADMAP.md) |

---

## 📞 When Stuck

1. **Check this cheat sheet first** ✅ You're here!
2. **Consult technical-reference.md** - Deep API dive
3. **Look at Metro source** - `metro/compiler/src/`
4. **Run `/consult-kotlin-api`** - Validate API usage
5. **Check test-matrix.md** - See test examples
6. **Review CHANGELOG.md** - Learn from past issues

---

**Print this cheat sheet and keep it visible while coding!** 📄

Last Updated: January 2025
