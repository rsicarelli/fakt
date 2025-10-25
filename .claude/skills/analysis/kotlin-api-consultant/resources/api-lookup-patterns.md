# API Lookup Patterns

Strategies for efficiently finding Kotlin compiler APIs in the source tree.

## Directory Structure

```
/kotlin/compiler/
├── backend.common/src/          # ⭐ Most IR/backend APIs
│   └── org/jetbrains/kotlin/backend/common/
│       ├── extensions/          # IrGenerationExtension, etc.
│       ├── lower/               # Lowering passes
│       └── serialization/       # IR serialization
├── ir/
│   ├── ir.tree/src/            # ⭐ Core IR node definitions
│   │   └── org/jetbrains/kotlin/ir/
│   │       ├── declarations/   # IrClass, IrFunction, etc.
│   │       ├── expressions/    # IrCall, IrConst, etc.
│   │       ├── types/          # IrType, IrTypeParameter
│   │       └── symbols/        # IrClassSymbol, etc.
│   └── backend.jvm/src/        # JVM-specific IR
├── fir/                        # ⭐ Frontend IR (K2 compiler)
│   ├── fir2ir/                 # FIR → IR conversion
│   ├── resolve/                # Name resolution
│   └── checkers/               # Semantic checks
├── plugin-api/src/             # ⭐ Public plugin API
│   └── org/jetbrains/kotlin/compiler/plugin/
│       ├── CompilerPluginRegistrar.kt
│       └── ComponentRegistrar.kt
└── cli/cli-common/src/         # CLI and configuration
    └── org/jetbrains/kotlin/cli/common/
        └── config/             # CompilerConfiguration
```

## Search Strategies

### Strategy 1: Direct Name Search
**Best for**: Known API names

```bash
# Find interface/class definition
find /kotlin/compiler -name "*.kt" -exec grep -l "interface IrGenerationExtension\|class IrGenerationExtension" {} \;

# More specific
find /kotlin/compiler/backend.common -name "*Extension*.kt"
```

### Strategy 2: Package-Based Search
**Best for**: APIs in known packages

```bash
# Find all IR extension APIs
ls /kotlin/compiler/backend.common/src/org/jetbrains/kotlin/backend/common/extensions/

# List FIR extensions
ls /kotlin/compiler/fir/fir-extension-api/src/org/jetbrains/kotlin/fir/extensions/
```

### Strategy 3: Fuzzy/Partial Search
**Best for**: Exploring related APIs

```bash
# Find all APIs containing "Plugin"
grep -r "interface.*Plugin\|class.*Plugin" /kotlin/compiler/plugin-api/ --include="*.kt"

# Find type-related APIs
grep -r "interface.*Type\|class.*Type" /kotlin/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/types/ --include="*.kt"
```

### Strategy 4: Annotation-Based Search
**Best for**: Finding experimental/unsafe APIs

```bash
# Find @UnsafeApi marked APIs
grep -r "@UnsafeApi" /kotlin/compiler/ --include="*.kt" -A 3

# Find FIR-incompatible APIs
grep -r "@FirIncompatiblePluginAPI" /kotlin/compiler/ --include="*.kt" -A 3
```

## Common API Locations

### Plugin System APIs
```
📦 Package: org.jetbrains.kotlin.compiler.plugin
📁 Location: /kotlin/compiler/plugin-api/src/

Key APIs:
- CompilerPluginRegistrar
- ComponentRegistrar
- CliOption
```

### IR Generation APIs
```
📦 Package: org.jetbrains.kotlin.backend.common.extensions
📁 Location: /kotlin/compiler/backend.common/src/.../extensions/

Key APIs:
- IrGenerationExtension
- IrPluginContext
- IrIntrinsicExtension
```

### IR Node APIs
```
📦 Package: org.jetbrains.kotlin.ir.declarations
📁 Location: /kotlin/compiler/ir/ir.tree/src/.../declarations/

Key APIs:
- IrClass
- IrFunction
- IrProperty
- IrTypeParameter
```

### FIR Phase APIs
```
📦 Package: org.jetbrains.kotlin.fir.extensions
📁 Location: /kotlin/compiler/fir/fir-extension-api/src/

Key APIs:
- FirExtensionRegistrar
- FirSupertypeGenerationExtension
- FirDeclarationGenerationExtension
```

## Search Examples

### Example 1: Finding IrFactory
```bash
# Method 1: Direct search
grep -r "interface IrFactory\|class IrFactory" /kotlin/compiler/ir/

# Result:
# /kotlin/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/IrFactory.kt

# Method 2: Package search
ls /kotlin/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/IrFactory.kt
```

### Example 2: Finding Type Parameter APIs
```bash
# Search for IrTypeParameter
find /kotlin/compiler/ir -name "*TypeParameter*.kt"

# Results:
# - IrTypeParameter.kt (interface)
# - IrTypeParameterImpl.kt (implementation)
```

### Example 3: Finding Symbol APIs
```bash
# Find all symbol-related interfaces
grep -r "interface.*Symbol" /kotlin/compiler/ir/ir.tree/src/org/jetbrains/kotlin/ir/symbols/ --include="*.kt" | head -20

# Common patterns:
# - IrClassSymbol
# - IrFunctionSymbol
# - IrPropertySymbol
# - IrTypeParameterSymbol
```

## Tips for Efficient Lookup

### 1. Use Package Hints
If you know the category, start with the right directory:
- IR nodes → `ir/ir.tree/src/`
- Extensions → `backend.common/src/.../extensions/`
- FIR → `fir/`
- Plugin API → `plugin-api/src/`

### 2. Check Imports
When reading an API file, check imports to discover related APIs:
```kotlin
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
// → Related APIs: IrElement, IrElementVisitor
```

### 3. Follow Type Hierarchies
```kotlin
interface IrClass : IrDeclarationWithName, IrTypeParametersContainer
//                  ^^^^^^^^^^^^^^^^^^^^^^^^  ^^^^^^^^^^^^^^^^^^^^^^^
//                  Check these too!
```

### 4. Use IDE Navigation (if available)
```
Ctrl+Click on interface name → Jump to definition
Ctrl+H on interface → View type hierarchy
Alt+F7 on interface → Find usages
```

## Troubleshooting

### API Not Found
```
Problem: grep/find returns no results

Solutions:
1. Check spelling (case-sensitive)
2. Try partial name: grep -r "PluginContext" instead of "IrPluginContext"
3. Search in parent packages
4. Check if API is in a different Kotlin version
```

### Multiple Matches
```
Problem: Same name in multiple places

Strategy:
1. Prefer interface over implementation class
2. Prefer public API (`plugin-api/`) over internal
3. Check package name for correct module
```

### Deprecated API
```
Problem: Found API but marked deprecated

Action:
1. Look for @Deprecated annotation
2. Check ReplaceWith for new API name
3. Search for replacement API
```

## Quick Reference

**Most Used Fakt APIs:**
- IrGenerationExtension → `backend.common/src/.../extensions/`
- IrPluginContext → `backend.common/src/.../extensions/`
- IrClass → `ir/ir.tree/src/.../declarations/`
- IrTypeParameter → `ir/ir.tree/src/.../declarations/`
- CompilerPluginRegistrar → `plugin-api/src/`
