# API Lookup Patterns

Strategies for finding Kotlin compiler APIs in the source tree.

## Kotlin Compiler Source Structure

The Kotlin compiler source (https://github.com/JetBrains/kotlin) follows this layout:

```
compiler/
├── backend.common/src/          # IR/backend APIs
│   └── org/jetbrains/kotlin/backend/common/
│       ├── extensions/          # IrGenerationExtension, etc.
│       ├── lower/               # Lowering passes
│       └── serialization/       # IR serialization
├── ir/
│   ├── ir.tree/src/            # Core IR node definitions
│   │   └── org/jetbrains/kotlin/ir/
│   │       ├── declarations/   # IrClass, IrFunction, etc.
│   │       ├── expressions/    # IrCall, IrConst, etc.
│   │       ├── types/          # IrType, IrTypeParameter
│   │       └── symbols/        # IrClassSymbol, etc.
│   └── backend.jvm/src/        # JVM-specific IR
├── fir/                        # Frontend IR (K2 compiler)
│   ├── fir2ir/                 # FIR → IR conversion
│   ├── resolve/                # Name resolution
│   └── checkers/               # Semantic checks
├── plugin-api/src/             # Public plugin API
│   └── org/jetbrains/kotlin/compiler/plugin/
│       ├── CompilerPluginRegistrar.kt
│       └── ComponentRegistrar.kt
└── cli/cli-common/src/         # CLI and configuration
```

## Search Strategies

### Strategy 1: GitHub Code Search

Search https://github.com/JetBrains/kotlin for API definitions:
- `interface IrGenerationExtension` in `compiler/backend.common/`
- `class CompilerPluginRegistrar` in `compiler/plugin-api/`

### Strategy 2: Package-Based Navigation

Navigate the GitHub tree by known package locations:
- IR extensions: `compiler/backend.common/src/org/jetbrains/kotlin/backend/common/extensions/`
- FIR extensions: `compiler/fir/fir-extension-api/src/org/jetbrains/kotlin/fir/extensions/`
- Plugin API: `compiler/plugin-api/src/org/jetbrains/kotlin/compiler/plugin/`

### Strategy 3: Annotation-Based Discovery

Search for stability annotations in the Kotlin repo:
- `@UnsafeApi` - may change without notice
- `@FirIncompatiblePluginAPI` - K1 only, not K2
- `@ExperimentalCompilerApi` - unstable, may change

## Common API Locations

### Plugin System APIs
```
Package: org.jetbrains.kotlin.compiler.plugin
Path: compiler/plugin-api/src/

Key APIs:
- CompilerPluginRegistrar
- ComponentRegistrar
- CliOption
```

### IR Generation APIs
```
Package: org.jetbrains.kotlin.backend.common.extensions
Path: compiler/backend.common/src/.../extensions/

Key APIs:
- IrGenerationExtension
- IrPluginContext
- IrIntrinsicExtension
```

### IR Node APIs
```
Package: org.jetbrains.kotlin.ir.declarations
Path: compiler/ir/ir.tree/src/.../declarations/

Key APIs:
- IrClass
- IrFunction
- IrProperty
- IrTypeParameter
```

### FIR Phase APIs
```
Package: org.jetbrains.kotlin.fir.extensions
Path: compiler/fir/fir-extension-api/src/

Key APIs:
- FirExtensionRegistrar
- FirSupertypeGenerationExtension
- FirDeclarationGenerationExtension
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

## Quick Reference

**Most Used Fakt APIs:**
- IrGenerationExtension → `backend.common/src/.../extensions/`
- IrPluginContext → `backend.common/src/.../extensions/`
- IrClass → `ir/ir.tree/src/.../declarations/`
- IrTypeParameter → `ir/ir.tree/src/.../declarations/`
- CompilerPluginRegistrar → `plugin-api/src/`
