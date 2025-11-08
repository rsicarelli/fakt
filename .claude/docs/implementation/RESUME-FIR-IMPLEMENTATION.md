# 🔄 Session Resume: FIR Full Feature Support

**Date**: 2025-01-04
**Phase**: 3B.3 - Design Proper FIR→IR Communication API
**Status**: Paused to fix architectural anti-pattern

---

## 📋 Quick Context

We are implementing full FIR mode support for Fakt compiler plugin to handle all 119 @Fake declarations in the kmp-single-module sample (95 interfaces + 24 classes).

**Detailed Plan**: See `.claude/docs/implementation/fir-full-support-plan.md`

---

## ✅ What We've Completed

### Phase 3A (Complete)
- ✅ FIR checkers created (`FakeInterfaceChecker`, `FakeClassChecker`)
- ✅ Metro-aligned validation patterns
- ✅ Metadata storage with thread-safe ConcurrentHashMap

### Phase 4.1-4.3 (Complete)
- ✅ FIR→IR wiring through `FaktSharedContext`
- ✅ `FirMetadataStorage` for validated interfaces/classes
- ✅ `generateFromFirMetadata()` skeleton in IR extension

### Phase 3B.1 (Complete) ✅
**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/FakeInterfaceChecker.kt`

Implemented proper type parameter bounds extraction:

```kotlin
// Lines 171-180: Class-level type parameters
val bounds = typeParam.bounds.map { boundRef ->
    boundRef.coneType.toString()
}

// Lines 283-302: Method-level type parameters
val typeParameters = function.typeParameters.map { typeParamRef ->
    val typeParam = typeParamRef.symbol.fir
    val bounds = typeParam.bounds.map { boundRef ->
        boundRef.coneType.toString()
    }
    FirTypeParameterInfo(
        name = typeParam.name.asString(),
        bounds = bounds,
    )
}
```

**Result**: All type constraints extracted (e.g., `T : Comparable<T>`)

### Phase 3B.2 (Complete) ✅
**File**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/FirFakeMetadata.kt`

Removed `GenericPattern` from FIR metadata:

```kotlin
// BEFORE (anti-pattern - GenericPattern requires IrTypeParameter):
data class ValidatedFakeInterface(
    // ...
    val genericPattern: GenericPattern,  // ❌ Uses IR types in FIR!
)

// AFTER (correct - will reconstruct in IR):
data class ValidatedFakeInterface(
    val classId: ClassId,
    val simpleName: String,
    val packageName: String,
    val typeParameters: List<FirTypeParameterInfo>,  // ✅ FIR data only
    val properties: List<FirPropertyInfo>,
    val functions: List<FirFunctionInfo>,
    val sourceLocation: FirSourceLocation,
)
```

**Result**: Clean FIR/IR separation - FIR has no IR dependencies

---

## 🚨 Critical Issue: Architectural Anti-Pattern

### The Problem

Current `generateFromFirMetadata()` implementation (lines 151-208):

```kotlin
private fun generateFromFirMetadata(
    moduleFragment: IrModuleFragment,
    pluginContext: IrPluginContext,
) {
    // Load FIR metadata ✅
    val validatedInterfaces = sharedContext.metadataStorage.getAllInterfaces()

    // Build IrClass map ✅
    val irClassMap = buildIrClassMap(moduleFragment)

    // Convert FIR metadata to IrClass instances ⚠️
    val interfacesToProcess = validatedInterfaces.mapNotNull { firInterface ->
        irClassMap[firInterface.classId]  // Get IrClass
    }

    // ❌ ANTI-PATTERN: Pass IrClass to existing pipeline
    // This causes IR phase to RE-ANALYZE what FIR already validated!
    processInterfaces(interfacesToProcess, moduleFragment)
    //                 ^^^^^^^^^^^^^^^^^^ IrClass instances
}
```

**Why This Is Wrong**:
1. FIR already analyzed and extracted all metadata
2. Passing `IrClass` to `processInterfaces()` triggers IR analysis again
3. `InterfaceAnalyzer.analyze(irClass)` walks `irClass.declarations`
4. **Duplication**: Same analysis happens twice (FIR + IR)
5. **Violates Metro Pattern**: FIR analyzes, IR generates (NO IR analysis)

### The Correct Approach

**Principle**: FIR analyzes → IR generates (using ONLY FIR metadata)

```kotlin
// ✅ CORRECT: IR generation uses only FIR data
private fun generateFromFirMetadata(
    moduleFragment: IrModuleFragment,
    pluginContext: IrPluginContext,
) {
    val validatedInterfaces = sharedContext.metadataStorage.getAllInterfaces()

    // Map FIR data to IR generation metadata (pure data transformation)
    val mapper = FirToIrMapper()
    val irMetadata = validatedInterfaces.map { firInterface ->
        mapper.mapToIrMetadata(firInterface)  // Data-only conversion
    }

    // Generate code ONLY from metadata (no IrClass.declarations access)
    irMetadata.forEach { metadata ->
        generateFakeImplementation(metadata, moduleFragment)
        //                         ^^^^^^^^ Pure data, no IR analysis
    }
}
```

---

## 🎯 Phase 3B.3: Design Proper FIR→IR Communication API

### Goal
Create a **data-only API** for FIR→IR communication with **NO IR types**.

### Step 1: Write Tests First (TDD) 🧪

**Create**: `compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/fir/FirToIrMappingTest.kt`

```kotlin
package com.rsicarelli.fakt.compiler.fir

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FirToIrMappingTest {

    @Test
    fun `GIVEN simple interface WHEN mapping to IR metadata THEN all basic fields preserved`() {
        // GIVEN
        val firInterface = ValidatedFakeInterface(
            classId = ClassId(FqName("com.example"), FqName("UserRepository"), false),
            simpleName = "UserRepository",
            packageName = "com.example",
            typeParameters = emptyList(),
            properties = listOf(
                FirPropertyInfo(
                    name = "currentUser",
                    type = "String",
                    isMutable = false,
                    isNullable = true
                )
            ),
            functions = listOf(
                FirFunctionInfo(
                    name = "findById",
                    parameters = listOf(
                        FirParameterInfo("id", "Int", false, false)
                    ),
                    returnType = "User?",
                    isSuspend = false,
                    isInline = false,
                    typeParameters = emptyList(),
                    typeParameterBounds = emptyMap()
                )
            ),
            sourceLocation = FirSourceLocation.UNKNOWN
        )

        // WHEN
        val mapper = FirToIrMapper()
        val irMetadata = mapper.mapToIrMetadata(firInterface)

        // THEN
        assertEquals("com.example.UserRepository", irMetadata.fqName)
        assertEquals("UserRepository", irMetadata.simpleName)
        assertEquals("com.example", irMetadata.packageName)
        assertEquals(1, irMetadata.properties.size)
        assertEquals(1, irMetadata.functions.size)
        assertEquals(GenericPatternMetadata.NoGenerics, irMetadata.genericPattern)
    }

    @Test
    fun `GIVEN generic interface WHEN mapping THEN type parameters preserved`() {
        // GIVEN
        val firInterface = ValidatedFakeInterface(
            classId = ClassId(FqName("com.example"), FqName("Repository"), false),
            simpleName = "Repository",
            packageName = "com.example",
            typeParameters = listOf(
                FirTypeParameterInfo("T", emptyList())
            ),
            properties = emptyList(),
            functions = listOf(
                FirFunctionInfo(
                    name = "save",
                    parameters = listOf(
                        FirParameterInfo("item", "T", false, false)
                    ),
                    returnType = "T",
                    isSuspend = false,
                    isInline = false,
                    typeParameters = emptyList(),
                    typeParameterBounds = emptyMap()
                )
            ),
            sourceLocation = FirSourceLocation.UNKNOWN
        )

        // WHEN
        val mapper = FirToIrMapper()
        val irMetadata = mapper.mapToIrMetadata(firInterface)

        // THEN
        assertEquals(1, irMetadata.typeParameters.size)
        assertEquals("T", irMetadata.typeParameters[0].name)
        assertNotNull(irMetadata.genericPattern as? GenericPatternMetadata.ClassLevel)
    }

    @Test
    fun `GIVEN interface with constrained generics WHEN mapping THEN bounds rendered`() {
        // GIVEN
        val firInterface = ValidatedFakeInterface(
            classId = ClassId(FqName("com.example"), FqName("SortedRepo"), false),
            simpleName = "SortedRepo",
            packageName = "com.example",
            typeParameters = listOf(
                FirTypeParameterInfo("T", listOf("Comparable<T>"))
            ),
            properties = emptyList(),
            functions = emptyList(),
            sourceLocation = FirSourceLocation.UNKNOWN
        )

        // WHEN
        val mapper = FirToIrMapper()
        val irMetadata = mapper.mapToIrMetadata(firInterface)

        // THEN
        val classLevel = irMetadata.genericPattern as GenericPatternMetadata.ClassLevel
        assertEquals(listOf("Comparable<T>"), classLevel.bounds["T"])
    }

    @Test
    fun `GIVEN interface with method generics WHEN mapping THEN classified as MethodLevel`() {
        // GIVEN
        val firInterface = ValidatedFakeInterface(
            classId = ClassId(FqName("com.example"), FqName("Processor"), false),
            simpleName = "Processor",
            packageName = "com.example",
            typeParameters = emptyList(),
            properties = emptyList(),
            functions = listOf(
                FirFunctionInfo(
                    name = "transform",
                    parameters = listOf(
                        FirParameterInfo("input", "T", false, false)
                    ),
                    returnType = "T",
                    isSuspend = false,
                    isInline = false,
                    typeParameters = listOf(
                        FirTypeParameterInfo("T", emptyList())
                    ),
                    typeParameterBounds = emptyMap()
                )
            ),
            sourceLocation = FirSourceLocation.UNKNOWN
        )

        // WHEN
        val mapper = FirToIrMapper()
        val irMetadata = mapper.mapToIrMetadata(firInterface)

        // THEN
        assertNotNull(irMetadata.genericPattern as? GenericPatternMetadata.MethodLevel)
    }

    @Test
    fun `GIVEN interface with mixed generics WHEN mapping THEN classified as Mixed`() {
        // GIVEN
        val firInterface = ValidatedFakeInterface(
            classId = ClassId(FqName("com.example"), FqName("GenericRepo"), false),
            simpleName = "GenericRepo",
            packageName = "com.example",
            typeParameters = listOf(
                FirTypeParameterInfo("T", emptyList())
            ),
            properties = emptyList(),
            functions = listOf(
                FirFunctionInfo(
                    name = "map",
                    parameters = listOf(
                        FirParameterInfo("item", "T", false, false)
                    ),
                    returnType = "R",
                    isSuspend = false,
                    isInline = false,
                    typeParameters = listOf(
                        FirTypeParameterInfo("R", emptyList())
                    ),
                    typeParameterBounds = emptyMap()
                )
            ),
            sourceLocation = FirSourceLocation.UNKNOWN
        )

        // WHEN
        val mapper = FirToIrMapper()
        val irMetadata = mapper.mapToIrMetadata(firInterface)

        // THEN
        val mixed = irMetadata.genericPattern as GenericPatternMetadata.Mixed
        assertEquals(listOf("T"), mixed.classTypeParameterNames)
        assertEquals(listOf("map"), mixed.methodsWithGenerics)
    }
}
```

### Step 2: Create Metadata API 📦

**Create**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/IrGenerationMetadata.kt`

```kotlin
package com.rsicarelli.fakt.compiler.fir

/**
 * Complete metadata for IR code generation, derived from FIR analysis.
 *
 * This is a **data-only** representation with NO IR types. All type information
 * is represented as strings rendered from FIR ConeType.
 *
 * Following Metro pattern: FIR analyzes and packages data, IR generates code
 * from this data WITHOUT re-analyzing.
 *
 * @property fqName Fully qualified name (e.g., "com.example.UserRepository")
 * @property simpleName Simple class name (e.g., "UserRepository")
 * @property packageName Package name (e.g., "com.example")
 * @property typeParameters Class-level type parameters
 * @property properties All properties
 * @property functions All functions
 * @property imports Required imports (inferred from types)
 * @property genericPattern Classification of generic usage
 */
data class IrGenerationMetadata(
    val fqName: String,
    val simpleName: String,
    val packageName: String,
    val typeParameters: List<TypeParameterMetadata>,
    val properties: List<PropertyMetadata>,
    val functions: List<FunctionMetadata>,
    val imports: Set<String>,
    val genericPattern: GenericPatternMetadata,
)

/**
 * Type parameter metadata (no IrTypeParameter).
 *
 * @property name Type parameter name ("T", "K", "V")
 * @property bounds Upper bounds as strings (["Comparable<T>"])
 * @property variance Variance annotation (in/out/null)
 */
data class TypeParameterMetadata(
    val name: String,
    val bounds: List<String>,
    val variance: VarianceMetadata?,
)

enum class VarianceMetadata {
    IN,  // contravariant
    OUT, // covariant
}

/**
 * Property metadata.
 */
data class PropertyMetadata(
    val name: String,
    val type: String,
    val isMutable: Boolean,
    val isNullable: Boolean,
)

/**
 * Function metadata.
 */
data class FunctionMetadata(
    val name: String,
    val parameters: List<ParameterMetadata>,
    val returnType: String,
    val isSuspend: Boolean,
    val isInline: Boolean,
    val typeParameters: List<TypeParameterMetadata>,
)

/**
 * Parameter metadata.
 */
data class ParameterMetadata(
    val name: String,
    val type: String,
    val hasDefaultValue: Boolean,
    val isVararg: Boolean,
)

/**
 * Generic pattern classification (no IrTypeParameter).
 */
sealed class GenericPatternMetadata {
    object NoGenerics : GenericPatternMetadata()

    data class ClassLevel(
        val typeParameterNames: List<String>,
        val bounds: Map<String, List<String>>,
    ) : GenericPatternMetadata()

    data class MethodLevel(
        val methodsWithGenerics: List<String>,
    ) : GenericPatternMetadata()

    data class Mixed(
        val classTypeParameterNames: List<String>,
        val classBounds: Map<String, List<String>>,
        val methodsWithGenerics: List<String>,
    ) : GenericPatternMetadata()
}
```

### Step 3: Implement Mapper 🔄

**Create**: `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/FirToIrMapper.kt`

```kotlin
package com.rsicarelli.fakt.compiler.fir

/**
 * Maps FIR metadata to IR generation metadata.
 *
 * Pure data transformation with NO IR types or IR analysis.
 * All structural information comes from FIR phase analysis.
 *
 * Following Metro pattern: This is a simple data mapper, not an analyzer.
 */
class FirToIrMapper {

    fun mapToIrMetadata(validated: ValidatedFakeInterface): IrGenerationMetadata {
        val fqName = validated.classId.asFqNameString()

        val typeParameters = validated.typeParameters.map { mapTypeParameter(it) }
        val properties = validated.properties.map { mapProperty(it) }
        val functions = validated.functions.map { mapFunction(it) }

        val imports = inferImports(validated)
        val genericPattern = classifyGenericPattern(validated)

        return IrGenerationMetadata(
            fqName = fqName,
            simpleName = validated.simpleName,
            packageName = validated.packageName,
            typeParameters = typeParameters,
            properties = properties,
            functions = functions,
            imports = imports,
            genericPattern = genericPattern,
        )
    }

    private fun mapTypeParameter(fir: FirTypeParameterInfo): TypeParameterMetadata {
        return TypeParameterMetadata(
            name = fir.name,
            bounds = fir.bounds,
            variance = null, // TODO Phase 3D: Extract variance from FIR
        )
    }

    private fun mapProperty(fir: FirPropertyInfo): PropertyMetadata {
        return PropertyMetadata(
            name = fir.name,
            type = fir.type,
            isMutable = fir.isMutable,
            isNullable = fir.isNullable,
        )
    }

    private fun mapFunction(fir: FirFunctionInfo): FunctionMetadata {
        val parameters = fir.parameters.map { param ->
            ParameterMetadata(
                name = param.name,
                type = param.type,
                hasDefaultValue = param.hasDefaultValue,
                isVararg = param.isVararg,
            )
        }

        val typeParameters = fir.typeParameters.map { mapTypeParameter(it) }

        return FunctionMetadata(
            name = fir.name,
            parameters = parameters,
            returnType = fir.returnType,
            isSuspend = fir.isSuspend,
            isInline = fir.isInline,
            typeParameters = typeParameters,
        )
    }

    private fun inferImports(validated: ValidatedFakeInterface): Set<String> {
        // TODO: Parse type strings and extract package names
        // For now, return empty - existing code generation handles imports
        return emptySet()
    }

    private fun classifyGenericPattern(
        validated: ValidatedFakeInterface
    ): GenericPatternMetadata {
        val hasClassGenerics = validated.typeParameters.isNotEmpty()
        val methodsWithGenerics = validated.functions
            .filter { it.typeParameters.isNotEmpty() }
            .map { it.name }

        return when {
            !hasClassGenerics && methodsWithGenerics.isEmpty() -> {
                GenericPatternMetadata.NoGenerics
            }

            hasClassGenerics && methodsWithGenerics.isEmpty() -> {
                val bounds = validated.typeParameters.associate { param ->
                    param.name to param.bounds
                }
                GenericPatternMetadata.ClassLevel(
                    typeParameterNames = validated.typeParameters.map { it.name },
                    bounds = bounds,
                )
            }

            !hasClassGenerics && methodsWithGenerics.isNotEmpty() -> {
                GenericPatternMetadata.MethodLevel(
                    methodsWithGenerics = methodsWithGenerics,
                )
            }

            else -> { // hasClassGenerics && methodsWithGenerics.isNotEmpty()
                val bounds = validated.typeParameters.associate { param ->
                    param.name to param.bounds
                }
                GenericPatternMetadata.Mixed(
                    classTypeParameterNames = validated.typeParameters.map { it.name },
                    classBounds = bounds,
                    methodsWithGenerics = methodsWithGenerics,
                )
            }
        }
    }
}
```

### Step 4: Update IR Generation 🏗️

**Update**: `UnifiedFaktIrGenerationExtension.kt::generateFromFirMetadata()`

```kotlin
private fun generateFromFirMetadata(
    moduleFragment: IrModuleFragment,
    pluginContext: IrPluginContext,
) {
    logger.trace("Phase 3B.3: Generating code from FIR metadata (TDD approach)")

    // Load validated interfaces from FIR phase
    val validatedInterfaces = sharedContext.metadataStorage.getAllInterfaces()

    logger.trace("FIR metadata loaded: ${validatedInterfaces.size} interfaces")

    if (validatedInterfaces.isEmpty()) {
        logger.trace("No validated interfaces to generate")
        return
    }

    // Map FIR data to IR generation metadata (pure data transformation)
    val mapper = FirToIrMapper()
    val irMetadata = validatedInterfaces.map { firInterface ->
        mapper.mapToIrMetadata(firInterface)
    }

    logger.info("Phase 3B.3: Mapped ${irMetadata.size} interfaces to IR metadata")

    // Generate code ONLY from metadata (no IrClass analysis)
    irMetadata.forEach { metadata ->
        logger.trace("Generating fake for: ${metadata.fqName}")
        generateFakeFromMetadata(metadata, moduleFragment)
    }

    logGenerationCompletion(irMetadata.size, 0, moduleFragment)
}

/**
 * Generate fake implementation from pure metadata (no IR analysis).
 *
 * This is the core IR generation function that receives ONLY FIR-derived data.
 * NO access to IrClass.declarations or any IR structural analysis.
 *
 * @param metadata Complete structural information from FIR phase
 * @param moduleFragment Module for file creation
 */
private fun generateFakeFromMetadata(
    metadata: IrGenerationMetadata,
    moduleFragment: IrModuleFragment,
) {
    // TODO Phase 3B.3: Implement generation using only metadata
    // - Use metadata.properties for property generation
    // - Use metadata.functions for method generation
    // - Use metadata.genericPattern for generic handling
    // - Use metadata.typeParameters for class-level generics

    // NO calls to IrClass.declarations!
    // NO calls to InterfaceAnalyzer!
    // ONLY use metadata fields!

    logger.trace("TODO: Generate fake for ${metadata.simpleName} using metadata only")
}
```

---

## 🚀 How to Resume This Session

### 1. Load Context
```bash
# Read the full plan
cat .claude/docs/implementation/fir-full-support-plan.md

# Read this resume document
cat .claude/docs/implementation/RESUME-FIR-IMPLEMENTATION.md
```

### 2. Verify Current State
```bash
# Check what's been implemented
git status
git log --oneline -10

# Verify Phase 3B.1 + 3B.2 changes compiled
./gradlew :compiler:compileKotlin --no-daemon
```

### 3. Start Phase 3B.3 with TDD

**Command to give Claude**:
```
Continue Phase 3B.3 - Design Proper FIR→IR Communication API.

Start with TDD:
1. Create FirToIrMappingTest.kt with the tests from RESUME-FIR-IMPLEMENTATION.md
2. Run tests (they should fail - no implementation yet)
3. Create IrGenerationMetadata.kt with all data types
4. Create FirToIrMapper.kt with the mapping logic
5. Run tests again (should pass)
6. Update UnifiedFaktIrGenerationExtension.kt to use mapper
7. Verify no IrClass.declarations access in generation path

Follow the "FIR analyzes, IR generates" principle strictly.
NO shortcuts. NO IR analysis in generation.
```

### 4. Run Tests
```bash
# After implementing the test file
./gradlew :compiler:test --tests "*FirToIrMappingTest*" --no-daemon

# Verify all compiler tests still pass
./gradlew :compiler:test --no-daemon
```

---

## 📚 Key Reference Files

### Documentation
- **Full Plan**: `.claude/docs/implementation/fir-full-support-plan.md`
- **Metro Alignment**: `.claude/docs/development/metro-alignment.md`
- **Testing Guidelines**: `.claude/docs/validation/testing-guidelines.md`

### Code Files Modified
- ✅ `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/FakeInterfaceChecker.kt`
- ✅ `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/FirFakeMetadata.kt`

### Code Files to Create
- 🔜 `compiler/src/test/kotlin/com/rsicarelli/fakt/compiler/fir/FirToIrMappingTest.kt`
- 🔜 `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/IrGenerationMetadata.kt`
- 🔜 `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/fir/FirToIrMapper.kt`

### Code Files to Update
- 🔜 `compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/ir/UnifiedFaktIrGenerationExtension.kt`

---

## ✅ Success Criteria for Phase 3B.3

1. ✅ All tests in `FirToIrMappingTest.kt` pass
2. ✅ `IrGenerationMetadata` contains NO IR types (only strings)
3. ✅ `FirToIrMapper` performs pure data transformation
4. ✅ `generateFakeFromMetadata()` uses ONLY metadata (no IrClass access)
5. ✅ No calls to `IrClass.declarations` in generation path
6. ✅ No calls to `InterfaceAnalyzer.analyze()` in FIR mode
7. ✅ Generated code compiles (verify with sample)

---

## 🎯 Remember: TDD + Clean Architecture

1. **Write tests first** - Define expected behavior before implementation
2. **FIR analyzes** - Extract ALL structural info in FIR phase
3. **IR generates** - Use ONLY FIR metadata, NO re-analysis
4. **Data-only API** - No IR types in FIR→IR communication
5. **Follow Metro** - Proven patterns from production plugin

**Critical**: User explicitly rejected shortcuts. Build it right.
