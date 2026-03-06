---
name: compilation
description: Validates compilation of generated Fakt fakes and diagnoses build failures. Covers type safety, smart defaults, DSL typing, error classification, and root cause analysis. Use when compilation fails, generated code has errors, validating type safety, verifying changes compile before committing, or checking if generated code is valid. Make sure to use this skill whenever a build error mentions generated fakes or the Fakt plugin — even if the root cause might be elsewhere, this skill's error classification pinpoints the actual source.
allowed-tools: Read, Bash, Grep, Glob, TaskCreate, TaskUpdate
---

# Compilation Validator & Error Analyzer

Validates generated Fakt fake code compiles correctly and diagnoses failures when it doesn't.

## Instructions

### 1. Determine Scope

**Extract from conversation:**
- Validation: "validate compilation", "does it compile?", "check generated code"
- Diagnosis: "compilation error", "build fails", "generated code error"
- Specific interface: "validate compilation for AsyncService"

**If unclear:**
```
Ask: "Would you like me to validate compilation or diagnose a specific error?"
Options: validate all generated fakes | diagnose build failure | validate specific interface
```

### 2. Build and Capture Output

```bash
# Clean and rebuild
./gradlew clean

# Compile across platforms (captures errors)
./gradlew :samples:jvm-single-module:build 2>&1 | tee compilation.log
./gradlew :samples:android-single-module:build 2>&1 | tee -a compilation.log
./gradlew :samples:kmp-single-module:compileKotlinJvm --no-build-cache 2>&1 | tee -a compilation.log

# For multi-module
./gradlew :samples:kmp-multi-module:app:compileKotlinJvm 2>&1 | tee -a compilation.log
```

### 3. Validate Generated Files

```bash
# Find generated files
find build/generated/fakt -name "Fake*.kt" -type f

# Count files
find build/generated/fakt -name "*.kt" | wc -l
```

**If no files found** — check:
1. Plugin applied in build.gradle.kts
2. Interfaces have @Fake annotation
3. Plugin enabled: `fakt { enabled = true }`
4. Compiler logs for generation phase: `./gradlew compileKotlinJvm --info 2>&1 | grep -i "fakt\|fake"`

### 4. Structure Validation

For each generated file, verify:

```bash
for file in build/generated/fakt/**/*.kt; do
    grep -q "class Fake.*Impl" "$file" || echo "Missing impl class in $file"
    grep -q "fun fake" "$file" || echo "Missing factory in $file"
    grep -q "class Fake.*Config" "$file" || echo "Missing config in $file"
    grep -n "TODO\|FIXME" "$file" && echo "TODO markers found in $file"
done
```

**Per interface checklist:**
- [ ] `FakeXxxImpl` class exists
- [ ] `fakeXxx {}` factory function exists
- [ ] `FakeXxxConfig` DSL class exists
- [ ] All interface methods implemented
- [ ] Behavior properties for each method
- [ ] No TODO markers
- [ ] No unresolved references

### 5. Type Safety Validation

**Check type preservation in generated code:**

- [ ] Parameter types match interface
- [ ] Return types match interface
- [ ] Nullable types preserved (`User?`)
- [ ] `suspend` modifier preserved
- [ ] Collection types correct (`List<User>` not `List<Any>`)
- [ ] Function type parameters correct

**Smart defaults:**
- [ ] Primitives: `0`, `""`, `false`
- [ ] Nullable: `null`
- [ ] Collections: `emptyList()`, `emptySet()`, `emptyMap()`
- [ ] Unit: `{}`
- [ ] Complex types: `Result.failure(NotImplementedError())`

**DSL type safety:**
- [ ] Factory function properly typed
- [ ] Config class is standalone (no fake reference)
- [ ] Config collects behaviors as nullable `internal var`
- [ ] Lambda types match method signatures
- [ ] Fake is immutable after construction

### 6. Error Classification (If Compilation Fails)

**Extract errors:**
```bash
grep -n "error:" compilation.log
grep -A 10 "Exception" compilation.log
```

**Classify by component:**

| Category | Indicators | Common Fix |
|----------|-----------|------------|
| **Plugin Registration** | `CompilerPluginRegistrar not found`, ServiceLoader errors | `make publish-local` |
| **IR Generation** | `IrGenerationExtension`, `IR generation failed` | Check `UnifiedFaktIrGenerationExtension` |
| **FIR Detection** | `FirExtension`, `@Fake annotation`, symbol resolution | Check `FaktFirExtensionRegistrar` |
| **Generated Code** | Path contains `build/generated/fakt`, syntax errors | Inspect generated .kt files |
| **Dependencies** | `ClassNotFoundException`, `Cannot resolve` | `make publish-local`, check versions |
| **Type Resolution** | `Type mismatch`, `Unresolved reference`, `Cannot infer type` | Check imports, generic handling |

### 7. Common Error Patterns & Solutions

**Plugin JAR not built:**
```bash
# Rebuild and publish
make publish-local
# Verify JAR
ls -la compiler/build/libs/compiler-*.jar
jar tf compiler/build/libs/compiler-*.jar | grep CompilerPluginRegistrar
```

**Type resolution failure (`Unresolved reference`):**
```bash
# Check if type is in same module
find . -name "User.kt"
# For cross-module, verify imports in generated file
grep "^import" build/generated/fakt/**/*.kt
```

**Generic type issues (`Type mismatch: expected X, found Any?`):**
- Class-level generics → type erasure (known limitation)
- Method-level generics → scoping challenge
- Workaround: use concrete types or interface-level generics

**Missing META-INF/services:**
```bash
jar tf compiler/build/libs/compiler-*.jar | grep META-INF/services
unzip -p compiler/build/libs/compiler-*.jar \
  META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
```

### 8. Systematic Debugging (If Error Not Identified)

**Phase 1 — Environment:**
```bash
./gradlew --version | grep "Kotlin version"   # Expected: 2.0.0+
ls -la compiler/build/libs/                     # compiler-*.jar exists?
ls -la ~/.m2/repository/com/rsicarelli/fakt/    # Published to Maven?
```

**Phase 2 — Clean rebuild:**
```bash
make full-rebuild
```

**Phase 3 — Incremental isolation:**
Start with simplest interface, gradually add complexity:
1. Simple methods → 2. Nullable types → 3. Suspend functions → 4. Generics

**Phase 4 — Verbose logging:**
```bash
make debug
# Or manually:
./gradlew compileKotlinJvm --info 2>&1 | grep -i "fakt\|fake"
```

### 9. Generate Report

```
COMPILATION VALIDATION REPORT

Generation: {count} files generated
Structure: {pass/fail} (impl + factory + config per interface)
Compilation: {Success/Failed} ({error_count} errors, {warning_count} warnings)
Type Safety: {pass/fail}
Smart Defaults: {pass/fail}
DSL Typing: {pass/fail}

{If errors:}
ERRORS:
1. [{category}] {description} — File: {path}:{line} — Fix: {solution}

NEXT STEPS:
- {If success}: Run tests with `make test-sample`
- {If errors}: Fix issues above, then re-validate
```

## Supporting Files

- **`resources/error-catalog.md`** — Known error patterns and solutions
- **`resources/troubleshooting-workflows.md`** — Step-by-step debugging procedures
- **`resources/phase-specific-errors.md`** — Errors by implementation phase

## Related Skills

- **`bdd-test-runner`** — Run tests after successful validation
- **`kotlin-api-consultant`** — Validate Kotlin API usage
- **`interface-analyzer`** — Analyze problematic interfaces
