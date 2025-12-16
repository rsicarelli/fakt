---
name: compilation-validator
description: Validates compilation of generated Fakt fake code ensuring zero errors, proper type safety, smart defaults working correctly, and configuration DSL properly typed. Use when validating compilation, checking generated code, verifying type safety, debugging compilation issues, or when user mentions "validate compilation", "check compilation", "generated code errors", "compile fakes", "compilation fails", interface names with "validate", or "type safety".
allowed-tools: [Read, Bash, Grep, Glob, TodoWrite]
---

# Compilation Validator & Type Safety Checker

Production-grade validation ensuring generated Fakt fake code compiles without errors with proper type safety.

## Core Mission

Validates that Fakt-generated fake implementations:
- Compile successfully without errors
- Maintain type safety throughout
- Smart defaults work for all types
- Configuration DSL is properly typed
- Zero TODO compilation blockers (Phase 1 achievement)

## Instructions

### 1. Determine Validation Scope

**Extract from conversation:**
- Specific interface: "validate compilation for AsyncService"
- All interfaces: "validate all generated fakes"
- Verbose mode: "detailed compilation report"
- Quick check: "does it compile?"

**Options:**
- `--interface=<name>` - Validate specific interface
- `--all` - Validate all generated fakes (default)
- `--verbose` - Detailed output with code inspection

**If unclear:**
```
Ask: "What would you like me to validate?"
Options: specific interface | all generated fakes | verbose report
```

### 2. Clean Previous Builds

**Ensure fresh compilation:**
```bash
# Navigate to project root
cd /Users/rsicarelli/Workspace/Personal/ktfakes-prototype/ktfake

# Clean build artifacts
./gradlew clean
```

**Why clean?**
- Removes cached artifacts
- Ensures fresh generation
- Catches stale code issues
- Verifies incremental compilation

### 3. Trigger Fresh Code Generation

**Compile to generate fakes:**
```bash
# Test across all platforms
./gradlew :samples:jvm-single-module:build
./gradlew :samples:android-single-module:build
./gradlew :samples:kmp-single-module:compileKotlinJvm --no-build-cache

# Or for specific module
./gradlew :samples:kmp-multi-module:app:compileKotlinJvm
```

**Capture compilation output:**
```bash
# Save to log for analysis
./gradlew compileKotlinJvm 2>&1 | tee compilation.log
```

**Check exit code:**
```bash
if [ $? -eq 0 ]; then
    echo "✅ Compilation successful!"
else
    echo "❌ Compilation failed!"
    # Proceed to error analysis
fi
```

### 4. Validate Generated Files Exist

**Locate generated directory:**
```bash
# Common locations:
# - build/generated/fakt/commonTest/kotlin/
# - build/generated/fakt/test/kotlin/
# - build/generated/source/fakt/

# Find generated files
find build/generated -name "Fake*.kt" -type f
```

**Count generated files:**
```bash
file_count=$(find build/generated/fakt -name "*.kt" | wc -l)
echo "📊 Found ${file_count} generated Kotlin files"
```

**If no files found:**
```
❌ ERROR: No generated files found

Possible causes:
1. Plugin not applied correctly
2. No @Fake annotated interfaces
3. Output directory misconfigured
4. Compilation stopped before generation

💡 Check:
- build.gradle.kts has fakt plugin applied
- Interfaces have @Fake annotation
- Check compiler logs for generation phase
```

### 5. Validate Specific Interface (If Requested)

**Find generated file:**
```bash
interface_name="AsyncService"  # from user request

# Search for generated fake
generated_file=$(find build/generated/fakt -name "*${interface_name}*.kt" | head -n 1)

if [ -z "$generated_file" ]; then
    echo "❌ No generated file for: ${interface_name}"
    exit 1
fi

echo "📄 Generated file: ${generated_file}"
```

**Read and inspect generated code:**
```bash
Read ${generated_file}
```

**Validate file structure:**
- [ ] Implementation class exists (Fake{Interface}Impl)
- [ ] Factory function exists (fake{Interface})
- [ ] Configuration DSL exists (Fake{Interface}Config)
- [ ] All interface methods implemented
- [ ] Behavior properties for each method
- [ ] No TODO markers
- [ ] No compilation errors (e.g., unresolved references)

**Check for common issues:**
```bash
# Check for TODO markers (should be zero)
grep -n "TODO" ${generated_file}

# Check for unresolved references
grep -n "Unresolved" ${generated_file}

# Check for type errors
grep -n "Type mismatch" ${generated_file}
```

### 6. Type Safety Validation

**Check type preservation:**

**For methods:**
```kotlin
// Original interface
interface UserService {
    fun getUser(id: String): User
}

// Generated should preserve types
override fun getUser(id: String): User = getUserBehavior(id)
```

**Validate:**
- [ ] Parameter types match interface
- [ ] Return types match interface
- [ ] Nullable types preserved (User?)
- [ ] Generic types handled (method-level working, class-level Phase 2)

**For properties:**
```kotlin
// Original
interface Repository {
    val currentUser: User?
}

// Generated
override val currentUser: User?
    get() = currentUserBehavior()
```

**Validate:**
- [ ] Property types match
- [ ] Mutability preserved (val/var)
- [ ] Nullability correct

**For suspend functions:**
```kotlin
// Original
interface AsyncService {
    suspend fun fetchData(): Result<Data>
}

// Generated must preserve suspend
override suspend fun fetchData(): Result<Data> = fetchDataBehavior()
```

**Validate:**
- [ ] suspend modifier preserved
- [ ] Coroutine-safe

### 7. Smart Defaults Validation

**Phase 1 Achievement: Zero TODO blockers**

**Check default values work:**

**For return types:**
```kotlin
// Primitives
private var getUserBehavior: (String) -> User = { User("default") }

// Nullable
private var findUserBehavior: (String) -> User? = { null }

// Collections
private var getAllBehavior: () -> List<User> = { emptyList() }

// Unit
private var deleteBehavior: (String) -> Unit = {}

// Complex types
private var getResultBehavior: () -> Result<Data> = { Result.failure(NotImplementedError()) }
```

**Validate each default:**
- [ ] Compiles without errors
- [ ] Type-safe (no Any? casting unless intentional)
- [ ] Safe defaults (null for nullable, empty for collections)
- [ ] No runtime exceptions on default execution

### 8. Configuration DSL Type Safety

**Validate DSL is properly typed:**

**Factory function:**
```kotlin
fun fakeUserService(configure: FakeUserServiceConfig.() -> Unit = {}): UserService {
    return FakeUserServiceImpl().apply {
        FakeUserServiceConfig(this).configure()
    }
}
```

**Config class:**
```kotlin
class FakeUserServiceConfig(private val fake: FakeUserServiceImpl) {
    fun getUser(behavior: (String) -> User) {
        fake.configureGetUser(behavior)
    }
}
```

**Validate:**
- [ ] Factory function properly typed
- [ ] Config class takes correct impl type
- [ ] DSL methods match interface signatures
- [ ] Lambda types match method signatures
- [ ] No unsafe casts in DSL

### 9. Multi-Stage Validation

**Stage 1: Generation Check**
```bash
# Verify files generated
ls -la build/generated/fakt/

# Count files
find build/generated/fakt -name "*.kt" | wc -l
```

**Stage 2: Structure Check**
```bash
# Check each generated file has required components
for file in build/generated/fakt/**/*.kt; do
    grep -q "class Fake.*Impl" "$file" || echo "❌ Missing impl class in $file"
    grep -q "fun fake" "$file" || echo "❌ Missing factory in $file"
    grep -q "class Fake.*Config" "$file" || echo "❌ Missing config in $file"
done
```

**Stage 3: Compilation Check**
```bash
# Compile generated code
./gradlew compileTestKotlinJvm

# Check for errors
if [ $? -ne 0 ]; then
    echo "❌ Compilation failed"
    # Analyze errors
else
    echo "✅ Compilation successful"
fi
```

**Stage 4: Type-Check (Optional, verbose mode)**
```bash
# Use kotlinc for detailed type checking
kotlinc -jvm-target 11 ${generated_file} -d /tmp/validation
```

### 10. Analyze Compilation Errors (If Any)

**If compilation fails:**

**Extract error messages:**
```bash
# From compilation log
grep -A 3 "error:" compilation.log

# Common error patterns:
# - "Unresolved reference"
# - "Type mismatch"
# - "Expecting a top level declaration"
```

**Categorize errors:**

**Missing imports:**
```
❌ Unresolved reference: User
💡 Fix: Cross-module import generation (Phase 2.4)
```

**Generic type issues:**
```
❌ Type mismatch: expected User, found Any?
💡 Cause: Class-level generics (Phase 2 challenge)
```

**Suspend function issues:**
```
❌ Suspend function must be called from coroutine
💡 Check: Behavior property should be suspend
```

**For each error:**
- Identify category
- Provide specific fix
- Reference troubleshooting docs

### 11. Generate Validation Report

**Summary report:**

```
═══════════════════════════════════════════════════
🛡️ COMPILATION VALIDATION REPORT
═══════════════════════════════════════════════════

📊 GENERATION METRICS:
- Generated files: {count}
- Interfaces processed: {count}
- Output directory: {path}

✅ COMPILATION STATUS:
- Status: {Success ✅ | Failed ❌}
- Duration: {time}
- Warnings: {count}
- Errors: {count}

🔍 VALIDATION RESULTS:

Structure Check:
✅ Implementation classes: {count}/{total}
✅ Factory functions: {count}/{total}
✅ Configuration DSL: {count}/{total}
✅ No TODO markers: {yes/no}

Type Safety Check:
✅ Parameter types preserved: {yes/no}
✅ Return types preserved: {yes/no}
✅ Nullable types correct: {yes/no}
✅ Suspend modifiers preserved: {yes/no}

Smart Defaults Check:
✅ All defaults compile: {yes/no}
✅ Safe default values: {yes/no}
✅ No runtime exceptions: {yes/no}

DSL Type Safety:
✅ Factory properly typed: {yes/no}
✅ Config class correct: {yes/no}
✅ Lambda types match: {yes/no}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

{If errors:}
❌ COMPILATION ERRORS ({count}):
1. {Error type}: {Description}
   File: {path}:{line}
   Fix: {Suggested fix}

{If warnings:}
⚠️  WARNINGS ({count}):
1. {Warning description}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🎯 OVERALL SCORE: {percentage}%

{If 100%:}
✅ ALL VALIDATIONS PASSED - Production Ready!

{If <100%:}
⚠️  ISSUES FOUND - See details above

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📚 REFERENCES:
- Validation Docs: .claude/docs/development/validation/compilation-validation.md
- Type Safety: .claude/docs/development/validation/type-safety-validation.md
- Troubleshooting: Use compilation-error-analyzer Skill

═══════════════════════════════════════════════════
```

### 12. Suggest Next Actions

**Based on results:**

**If successful:**
```
✅ Compilation validation passed!

Next steps:
1. Run tests: bdd-test-runner Skill
2. Validate Metro patterns: metro-pattern-validator
3. Continue development
```

**If errors found:**
```
❌ Compilation errors detected

Recommended actions:
1. Analyze errors: compilation-error-analyzer Skill
2. Fix specific issues (see report)
3. Re-validate after fixes

For generic-related errors:
- Consult: generic-scoping-analyzer Skill
- Reference: .claude/docs/implementation/generics/technical-reference.md
```

## Supporting Files

Progressive disclosure for validation patterns:

- **`resources/compilation-patterns.md`** - Common compilation patterns (loaded on-demand)
- **`resources/type-safety-guide.md`** - Type safety validation techniques (loaded on-demand)
- **`resources/error-catalog.md`** - Known compilation errors and fixes (loaded on-demand)
- **`scripts/validate-compilation.sh`** - Automated validation script

## Related Skills

This Skill composes with:
- **`compilation-error-analyzer`** - Analyze specific errors
- **`generic-scoping-analyzer`** - Debug generic type issues
- **`kotlin-ir-debugger`** - Debug IR generation
- **`bdd-test-runner`** - Run tests after validation

## Validation Checklist

Per interface:
- [ ] Generated file exists
- [ ] Implementation class present
- [ ] Factory function present
- [ ] Configuration DSL present
- [ ] All methods implemented
- [ ] Types preserved
- [ ] Smart defaults work
- [ ] No TODO markers
- [ ] Compiles without errors
- [ ] No warnings

## Best Practices

1. **Always clean before validate** - Fresh compilation catches issues
2. **Check both structure and compilation** - Multi-stage validation
3. **Validate type safety explicitly** - Don't assume
4. **Test smart defaults** - They must work without configuration
5. **Reference troubleshooting** - Link to error solutions

## Known Validation Targets

**Phase 1 Achievements:**
- ✅ Zero TODO compilation blockers
- ✅ 85% compilation success rate
- ✅ Smart default system working
- ✅ Function type generation perfect

**Phase 2 Known Issues:**
- ⚠️ Class-level generics lose type safety (use Any?)
- ⚠️ Cross-module imports not auto-generated
- ⚠️ Complex generic bounds need manual handling

## Performance Notes

- Clean build: ~30s
- Compilation: ~1-2 minutes (depends on project size)
- Validation: ~10-30s
- Total: ~2-4 minutes for full validation

## Success Criteria

**100% Validation Score**:
- All generated files exist
- All structure checks pass
- Zero compilation errors
- Zero warnings
- All type safety checks pass
- Smart defaults work
- DSL properly typed
