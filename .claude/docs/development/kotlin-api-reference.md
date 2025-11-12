# Kotlin API Reference Guide - Compiler Source Consultation

> **Purpose**: How to consult the Kotlin compiler source for technical validation
> **Location**: `/kotlin/compiler/` - Clone of Kotlin compiler for reference
> **Usage**: Validate APIs, understand patterns, verify compatibility

## 🎯 **When to Consult Kotlin Source**

### **✅ ALWAYS consult for:**
- **API validation** - Check if methods/classes still exist
- **Pattern verification** - How Kotlin internally resolves similar problems
- **Type system understanding** - How generics are handled internally
- **Breaking change detection** - Deprecated APIs or changes

### **🔍 Kotlin Compiler Source Structure**

```
/kotlin/compiler/
├── ir/                          # 🎯 IR system - our main reference
│   ├── backend.common/          # IrGenerationExtension, extensions
│   ├── ir.tree/                 # IrElement, IrClass, IrFunction hierarchy
│   └── ir.serialization.common/ # Cross-module serialization
├── fir/                         # 🎯 FIR system - frontend reference
│   ├── fir2ir/                  # FIR → IR conversion (critical for us)
│   ├── tree/                    # FirElement hierarchy
│   └── checkers/                # Validation and error reporting
├── backend.common/              # 🔧 Extension points we use
│   └── extensions/              # IrGenerationExtension interface
└── cli/                         # Command line processing
```

## 🔧 **Key API Reference Paths**

### **1. IrGenerationExtension - Our Main Extension Point**

**📍 Location:** `/kotlin/compiler/backend.common/src/org/jetbrains/kotlin/backend/common/extensions/IrGenerationExtension.kt`

**Key APIs to monitor:**
```kotlin
interface IrGenerationExtension {
    fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext)
    fun getPlatformIntrinsicExtension(loweringContext: LoweringContext): IrIntrinsicExtension?
}
```

**How to consult:**
1. Check method signatures haven't changed
2. Look for new methods added
3. Understand usage patterns in Kotlin's own implementations

### **2. IrPluginContext - Core Context for IR Manipulation**

**📍 Location:** `/kotlin/compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/extensions/IrPluginContext.kt`

**Critical APIs we use:**
```kotlin
interface IrPluginContext {
    val irFactory: IrFactory                    // Creating new IR elements
    val symbolTable: SymbolTable                // Symbol resolution
    val moduleDescriptor: ModuleDescriptor      // Module information
    fun referenceClass(classId: ClassId): IrClassSymbol?
    fun referenceFunction(callableId: CallableId): IrSimpleFunctionSymbol?
}
```

**Consultation checklist:**
```bash
# Always check these APIs are still available:
grep -n "val irFactory" /kotlin/compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/extensions/IrPluginContext.kt
grep -n "referenceClass" /kotlin/compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/extensions/IrPluginContext.kt
```

### **3. IR Tree Elements - For Code Generation**

**📍 Location:** `/kotlin/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/declarations/`

**Key classes for fake generation:**
```kotlin
// Interface definition structure
interface IrClass : IrDeclarationContainer, IrSymbolOwner<IrClassSymbol> {
    val name: Name
    val kind: ClassKind           // INTERFACE, CLASS, etc.
    val isInterface: Boolean
    val superTypes: List<IrType>
    // ... method and property collections
}

// Function definition structure
interface IrFunction : IrDeclaration, IrSymbolOwner<IrFunctionSymbol> {
    val name: Name
    val returnType: IrType
    val valueParameters: List<IrValueParameter>
    val isSuspend: Boolean
    val typeParameters: List<IrTypeParameter>  // Generic type parameters!
}
```

**Generic Type System Reference:**
```kotlin
// Critical for our Phase 2 generic scoping challenge
interface IrTypeParameter : IrDeclaration {
    val name: Name
    val index: Int
    val isReified: Boolean
    val variance: Variance
    val superTypes: List<IrType>
}

interface IrTypeParameterSymbol : IrBindableSymbol<IrTypeParameter>
```

## 🔍 **Specific API Consultation Patterns**

### **Pattern 1: Verify Extension Registration**

**Check how Kotlin registers extensions:**
```bash
# Look for extension registration patterns
grep -r "IrGenerationExtension.registerExtension" /kotlin/compiler/
grep -r "FirExtensionRegistrarAdapter" /kotlin/compiler/
```

**Understand Metro alignment:**
```kotlin
// Metro follows this exact pattern - verify it's still valid
IrGenerationExtension.registerExtension(
    MetroIrGenerationExtension(...)
)
```

### **Pattern 2: Type Resolution Validation**

**Check how Kotlin handles generic types:**
```bash
# Look for type parameter handling
grep -r "IrTypeParameterSymbol" /kotlin/compiler/ir/
grep -r "typeParameters" /kotlin/compiler/ir/ir.tree/
```

**Apply to our generic scoping challenge:**
```kotlin
// How Kotlin internally handles type parameters
when (val classifier = irType.classifier) {
    is IrTypeParameterSymbol -> {
        // This is how Kotlin resolves generic type parameters
        val typeParameter = classifier.owner
        val name = typeParameter.name.asString()
        // We can use this pattern for our generic scoping
    }
}
```

### **Pattern 3: Code Generation Validation**

**Check IR building patterns:**
```bash
# Look for class building patterns
grep -r "irFactory.buildClass" /kotlin/compiler/
grep -r "buildFun" /kotlin/compiler/
```

**Modern API usage:**
```kotlin
// Verify this pattern is still current
val generatedClass = irFactory.buildClass {
    name = Name.identifier("FakeUserServiceImpl")
    kind = ClassKind.CLASS
    // ... continue building
}.apply {
    // Post-creation setup
}
```

## 🚨 **API Stability Monitoring**

### **APIs We Depend On (Monitor These):**

1. **IrGenerationExtension interface**
   - `generate()` method signature
   - Extension registration mechanism

2. **IrPluginContext APIs**
   - `irFactory` property availability
   - `symbolTable` for symbol resolution
   - Reference methods (`referenceClass`, `referenceFunction`)

3. **IR Tree Building**
   - `irFactory.buildClass` method
   - `buildFun` for method generation
   - Type parameter handling APIs

4. **FIR Extension System**
   - `FirExtensionRegistrarAdapter` usage
   - Extension registration patterns

### **Breaking Change Detection Checklist:**

```bash
# Run these commands to detect changes:

# 1. Check IrGenerationExtension changes
diff -u previous_kotlin_version/IrGenerationExtension.kt current_kotlin_version/IrGenerationExtension.kt

# 2. Check IrPluginContext API changes
grep -n "interface IrPluginContext" /kotlin/compiler/ir/backend.common/src/org/jetbrains/kotlin/backend/common/extensions/IrPluginContext.kt

# 3. Check IR building APIs
grep -A 10 "class IrFactory" /kotlin/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/IrFactory.kt

# 4. Check type parameter APIs
grep -A 5 "interface IrTypeParameter" /kotlin/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/declarations/IrTypeParameter.kt
```

## 📚 **Common Consultation Workflows**

### **Workflow 1: Implementing New Feature**

1. **Research Phase:**
   ```bash
   # Find similar functionality in Kotlin compiler
   grep -r "similar_feature" /kotlin/compiler/
   ```

2. **API Validation:**
   ```bash
   # Check APIs we plan to use
   grep -n "target_api" /kotlin/compiler/path/to/api/
   ```

3. **Pattern Application:**
   ```kotlin
   // Apply Kotlin patterns to KtFakes
   // Follow exactly how Kotlin does it internally
   ```

### **Workflow 2: Debugging Compilation Issues**

1. **Error Analysis:**
   ```bash
   # Look for similar error handling in Kotlin
   grep -r "error_message_text" /kotlin/compiler/
   ```

2. **Resolution Pattern:**
   ```kotlin
   // See how Kotlin resolves the same issue
   // Apply same pattern to KtFakes
   ```

### **Workflow 3: Performance Optimization**

1. **Kotlin Performance Patterns:**
   ```bash
   # Look for optimization patterns
   grep -r "performance\|optimization\|cache" /kotlin/compiler/
   ```

2. **Apply to KtFakes:**
   ```kotlin
   // Use Kotlin's own optimization strategies
   ```

## 🎯 **Metro + Kotlin Source Alignment**

### **Best Practice Workflow:**

1. **Metro Pattern First** - Check if Metro already solved this
2. **Kotlin Source Validation** - Verify APIs and patterns are current
3. **Combined Application** - Apply Metro pattern with validated Kotlin APIs
4. **Testing** - Ensure compatibility with both Metro approach and Kotlin APIs

### **Example: Generic Type Scoping Solution**

```kotlin
// 1. Metro pattern research
// How does Metro handle complex types?

// 2. Kotlin source consultation
// /kotlin/compiler/ir/ir.tree/src/.../IrTypeParameter.kt
// How does Kotlin handle type parameter scoping?

// 3. Combined solution
context(context: IrKtFakeContext)
private fun resolveGenericType(irType: IrType): String {
    // Use Kotlin's own type parameter resolution + Metro patterns
    return when (irType) {
        is IrSimpleType -> {
            when (val classifier = irType.classifier) {
                is IrTypeParameterSymbol -> {
                    // Kotlin-validated + Metro-inspired approach
                    classifier.owner.name.asString()
                }
                // ... rest of Kotlin-pattern-based resolution
            }
        }
        // ... follow Kotlin internal patterns
    }
}
```

## ⚡ **Quick Reference Commands**

### **Common API Lookups:**
```bash
# IrGenerationExtension current signature
grep -A 10 "interface IrGenerationExtension" /kotlin/compiler/backend.common/src/org/jetbrains/kotlin/backend/common/extensions/IrGenerationExtension.kt

# IrFactory methods
grep -A 20 "class IrFactory" /kotlin/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/IrFactory.kt

# Type parameter handling
grep -A 15 "IrTypeParameterSymbol" /kotlin/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/symbols/IrSymbol.kt

# Extension registration
grep -r "registerExtension" /kotlin/compiler/cli/
```

---

**Always validate with Kotlin source before implementing. APIs change, patterns evolve, and our implementation must stay compatible.**