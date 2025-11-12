---
name: kotlin-api-consultant
description: Queries Kotlin compiler source code for API validation, compatibility checks, Metro pattern alignment, breaking change detection, and best practice recommendations. Use when validating Kotlin APIs, checking compiler compatibility, analyzing API usage patterns, detecting breaking changes, or when user mentions "Kotlin API", "compiler API", "IrGenerationExtension", "IrPluginContext", "API validation", "Metro API usage", or specific Kotlin compiler class/interface names.
allowed-tools: [Read, Grep, Glob, Bash, WebFetch]
---

# Kotlin API Oracle & Compatibility Validator

Automatic Kotlin compiler source consultation with Metro pattern alignment for production-quality compiler plugin development.

## Core Mission

Provides real-time Kotlin compiler API validation by consulting local Kotlin compiler source (`/kotlin/compiler/`), detecting breaking changes, analyzing Metro usage patterns, and ensuring API compatibility across Kotlin versions.

## Instructions

### 1. Identify Target API

**Extract from conversation:**
- API class/interface name from user's message
- Look for patterns: "check IrGenerationExtension", "validate IrPluginContext API", "how does Metro use X"
- Common APIs: IrGenerationExtension, IrPluginContext, IrFactory, IrClass, IrTypeParameter, CompilerPluginRegistrar

**If unclear or missing:**
```
Ask: "Which Kotlin compiler API would you like me to consult?"
Suggest: IrGenerationExtension | IrPluginContext | IrFactory | CompilerPluginRegistrar | Other
```

### 2. Locate API in Kotlin Compiler Source

**Search Kotlin source tree:**
```bash
# Primary location: /kotlin/compiler/
cd /kotlin/compiler/

# Find API definition
find . -name "*.kt" -type f -exec grep -l "interface ${API_NAME}\|class ${API_NAME}" {} \;

# Common locations:
# - ir/backend.common/src/.../extensions/ (IR APIs)
# - fir/ (FIR APIs)
# - plugin-api/src/ (Plugin APIs)
# - cli/cli-common/src/ (CLI APIs)
```

**If found multiple:**
- Prioritize `interface` over `class` (usually the API contract)
- Prefer `backend.common` over implementation modules
- Note all locations for completeness

**If not found:**
```
❌ API '${API_NAME}' not found in Kotlin compiler source

💡 Suggestions:
1. Check spelling (case-sensitive)
2. Try fuzzy search: grep -r "${PARTIAL_NAME}" /kotlin/compiler/
3. Consult Kotlin docs: https://kotlinlang.org/api/latest/
4. Check if it's an internal/experimental API
```

### 3. Read and Parse API Definition

**Read the source file:**
```bash
Read /kotlin/compiler/.../extensions/${API_NAME}.kt
```

**Extract key information:**
- [ ] Package and imports
- [ ] Interface/class declaration
- [ ] Generic type parameters
- [ ] Method signatures
- [ ] Annotations (@UnsafeApi, @FirIncompatiblePluginAPI, @Deprecated)
- [ ] KDoc comments
- [ ] Default implementations

**Analyze annotations:**
```kotlin
// Critical markers
@UnsafeApi                          // May change without notice
@FirIncompatiblePluginAPI          // K1 only, not K2
@Deprecated(message = "...", level = ERROR)  // Removal planned
@ExperimentalCompilerApi           // Unstable, may change
```

### 4. Check Metro Usage Patterns

**Search Metro codebase:**
```bash
# Find Metro usage of this API
grep -r "${API_NAME}" /metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/

# Look for:
# - How Metro implements/extends this API
# - Patterns Metro follows
# - Workarounds for known issues
```

**Common Metro files to check:**
```
metro/compiler/src/main/kotlin/dev/zacsweers/metro/compiler/
├── MetroCompilerPluginRegistrar.kt     # Plugin registration
├── ir/MetroIrGenerationExtension.kt    # IR generation
├── fir/MetroFirExtensionRegistrar.kt   # FIR phase
└── context/IrMetroContext.kt           # Context pattern
```

**Metro alignment checklist:**
- [ ] How does Metro use this API?
- [ ] Any Metro-specific patterns?
- [ ] Workarounds Metro implements?
- [ ] Best practices Metro demonstrates?

### 5. Detect Breaking Changes

**Compare with documented version:**
```bash
# Check git history for changes
cd /kotlin/compiler/
git log -p --all -- **/${API_NAME}.kt | head -100

# Look for:
# - Method signature changes
# - Removed methods
# - Added required methods
# - Changed default implementations
```

**Breaking change indicators:**
- Removed methods (breaking)
- Changed method signatures (breaking)
- Added abstract methods to interface (breaking)
- Changed return types (breaking)
- New required type parameters (breaking)
- Deprecation with ReplaceWith (migration path)

**Version compatibility:**
```
🔍 API: ${API_NAME}
📅 Current Kotlin: 2.2.20 (or detected version)
📋 Changes since 2.0.0: [list]

⚠️ Breaking changes detected: [yes/no]
```

### 6. Analyze Current API Definition

**Generate API report:**

```
═══════════════════════════════════════════════════
🔍 KOTLIN API CONSULTATION: ${API_NAME}
═══════════════════════════════════════════════════

📍 LOCATION:
File: /kotlin/compiler/.../path/to/${API_NAME}.kt
Package: org.jetbrains.kotlin.backend.common.extensions
Module: backend.common

📋 INTERFACE DEFINITION (Kotlin ${VERSION}):
```kotlin
interface ${API_NAME} {
    // Methods extracted from source
    fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext)

    // Annotations and markers
    @FirIncompatiblePluginAPI
    val shouldAlsoBeAppliedInKaptStubGenerationMode: Boolean
}
```

🏗️ METRO USAGE PATTERN:
```kotlin
class MetroIrGenerationExtension(...) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val context = IrMetroContext(pluginContext, messageCollector, ...)
        context(context) { generateInner(moduleFragment) }
    }
}
```

✅ FAKT ALIGNMENT CHECKLIST:
- [✅/❌] Follows Metro pattern
- [✅/❌] Uses context pattern for organization
- [✅/❌] Handles K2 compatibility properly
- [✅/❌] Implements all required methods
- [✅/❌] Respects annotations and warnings

📚 BEST PRACTICES:
1. [Practice from KDoc or Metro usage]
2. [Practice 2]
3. [Practice 3]

⚠️ WARNINGS & CAVEATS:
- [Any @UnsafeApi warnings]
- [Deprecation notices]
- [Known issues or limitations]

═══════════════════════════════════════════════════
```

### 7. Provide Usage Recommendations

**For Fakt implementation:**

**If API is stable:**
```
✅ SAFE TO USE: ${API_NAME}

📋 Recommended usage:
1. Follow Metro pattern exactly
2. Use context pattern for organization
3. Handle errors with messageCollector
4. Support both K1 and K2 if applicable

📝 Example implementation:
[Code snippet showing recommended usage]
```

**If API has warnings:**
```
⚠️ USE WITH CAUTION: ${API_NAME}

📋 Warnings:
- @UnsafeApi: May change without notice
- Monitor Kotlin release notes for changes
- Consider abstraction layer for isolation

📋 Mitigation:
- Create wrapper interface
- Document API version dependency
- Add compatibility tests
```

**If API is deprecated:**
```
🚨 DEPRECATED: ${API_NAME}

📋 Status:
- Deprecated since: Kotlin ${VERSION}
- Removal planned: Kotlin ${FUTURE_VERSION}
- Replacement: ${REPLACEMENT_API}

📋 Migration path:
[Show ReplaceWith suggestion or manual migration steps]
```

### 8. Cross-Reference Related APIs

**Suggest related APIs:**
```
🔗 RELATED APIS:

Used together with ${API_NAME}:
- ${RELATED_API_1} - [Brief description]
- ${RELATED_API_2} - [Brief description]

See also:
- Kotlin docs: https://kotlinlang.org/api/compiler/
- Metro examples: /metro/compiler/src/.../
```

### 9. Provide Implementation Examples

**Real-world examples:**

**From Metro:**
```kotlin
// Metro's implementation of ${API_NAME}
[Extract relevant Metro code]
```

**Recommended for Fakt:**
```kotlin
// Adapted for Fakt's use case
[Show recommended implementation]
```

**Anti-patterns to avoid:**
```kotlin
// ❌ Don't do this:
[Show common mistakes]

// ✅ Do this instead:
[Show correct approach]
```

## Supporting Files

Progressive disclosure for API knowledge:

- **`resources/api-lookup-patterns.md`** - Strategies for finding APIs in Kotlin source (loaded on-demand)
- **`resources/metro-api-usage.md`** - Metro usage examples for common APIs (loaded on-demand)
- **`resources/breaking-changes-catalog.md`** - Known breaking changes across Kotlin versions (loaded on-demand)
- **`resources/api-best-practices.md`** - Best practices from Kotlin and Metro (loaded on-demand)

## Related Skills

This Skill composes with:
- **`kotlin-ir-debugger`** - Debug IR generation using validated APIs
- **`metro-pattern-validator`** - Validate Metro alignment for API usage
- **`generic-scoping-analyzer`** - Analyze IrTypeParameter API (Phase 2)
- **`fakt-docs-navigator`** - Access Metro alignment documentation

## Common API Consultations

**Frequently consulted APIs:**

### IrGenerationExtension
```bash
"Check IrGenerationExtension API"
"How does Metro use IrGenerationExtension?"
"Validate IR generation API"
```

### IrPluginContext
```bash
"Consult IrPluginContext"
"What's in IrPluginContext API?"
"How to use irFactory from IrPluginContext?"
```

### IrTypeParameter
```bash
"Check IrTypeParameter for generics"
"Generic type API validation"
"How to handle IrTypeParameterSymbol?"
```

### CompilerPluginRegistrar
```bash
"Validate CompilerPluginRegistrar"
"Check plugin registration API"
"Metro's CompilerPluginRegistrar pattern"
```

## Error Handling

### API Not Found
```
❌ API '${API_NAME}' not found

🔍 Attempting fuzzy search...
[Run grep with partial name]

💡 Did you mean:
- ${SIMILAR_API_1}
- ${SIMILAR_API_2}
```

### Multiple Matches
```
⚠️ Multiple definitions found for '${API_NAME}':

1. /kotlin/compiler/ir/.../IrFactory.kt (interface)
2. /kotlin/compiler/ir/.../IrFactory.kt (implementation class)

📋 Analyzing interface (primary API contract)...
```

### Deprecated API
```
🚨 DEPRECATED API DETECTED

API: ${API_NAME}
Status: Deprecated since Kotlin ${VERSION}
Replacement: ${NEW_API}

💡 Recommendation: Migrate to ${NEW_API}
Migration guide: [Link or steps]
```

## Best Practices

1. **Always check Metro first** - If Metro uses an API, it's battle-tested
2. **Respect @UnsafeApi** - Isolate behind abstraction layer
3. **Monitor Kotlin releases** - Breaking changes happen in minor versions
4. **Test across K1/K2** - API behavior may differ
5. **Document API versions** - Track which Kotlin version APIs target

## API Categories

| Category | Examples | Stability |
|----------|----------|-----------|
| Core IR | IrGenerationExtension, IrPluginContext | Stable |
| FIR Phase | FirExtensionRegistrar | K2 specific |
| Plugin System | CompilerPluginRegistrar | Stable |
| Type System | IrTypeParameter, IrType | Moderate |
| Experimental | Various @ExperimentalCompilerApi | Unstable |

## Validation Checklist

**Before using an API in Fakt:**
- [ ] API located in Kotlin source
- [ ] Current definition analyzed
- [ ] Metro usage pattern reviewed
- [ ] Breaking changes checked
- [ ] Deprecation status confirmed
- [ ] K1/K2 compatibility verified
- [ ] Implementation example created

## Performance Notes

- API lookup: ~5-10 seconds (file system search)
- Source reading: ~2-5 seconds per file
- Metro cross-reference: ~5 seconds
- Total consultation: ~20-30 seconds

Fast enough for real-time development guidance!
