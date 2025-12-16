---
name: compilation-error-analyzer
description: Systematic compilation error diagnostic and resolution for Fakt development analyzing error types, identifying root causes, providing targeted solutions, and systematic debugging workflows. Use when debugging compilation errors, analyzing build failures, troubleshooting generated code, resolving plugin issues, or when user mentions "compilation error", "build fails", "generated code error", "plugin error", error messages, stack traces, or interface names with error context.
allowed-tools: [Read, Grep, Bash, Glob]
---

# Compilation Error Diagnostic & Resolution Engine

Systematic error analysis with targeted solution recommendations for Fakt compiler plugin development.

## Core Mission

Analyzes compilation errors in Fakt development to:
- Identify root causes of compilation failures
- Classify error types by component (plugin, IR, FIR, generated code, dependencies)
- Provide specific solutions for common error patterns
- Guide through systematic debugging workflows
- Suggest preventive measures

## Instructions

### 1. Determine Analysis Scope

**Extract from conversation:**
- Error type from user message or context
- Specific interface name if mentioned
- Verbosity level (quick check vs detailed analysis)

**Look for patterns:**
- "compilation error for UserService"
- "plugin registration failed"
- "generated code doesn't compile"
- Stack traces or error messages in conversation

**Scope options:**
- `--type=<error_type>` - Focus on specific error category
- `--interface=<name>` - Analyze errors for specific interface
- `--verbose` - Detailed output with code inspection
- Default: Analyze recent compilation logs

**If unclear:**
```
Ask: "What kind of compilation error are you experiencing?"
Options: Plugin registration | IR generation | Generated code | General build failure
```

### 2. Locate and Read Compilation Logs

**Find recent compilation output:**
```bash
# Check if compilation log exists
if [ -f "compilation.log" ]; then
    Read compilation.log
else
    # Run compilation to generate fresh logs
    ./gradlew compileKotlinJvm 2>&1 | tee compilation.log
fi
```

**Alternative log locations:**
```bash
# Gradle build scan
cat build/reports/build-scan/build-scan.txt

# Recent Gradle output
cat .gradle/build-output.txt

# Test compilation logs
cat build/test-results/*/TEST-*.xml
```

**Extract error messages:**
```bash
# Find all "error:" lines
grep -n "error:" compilation.log

# Find exception stack traces
grep -A 10 "Exception" compilation.log
```

### 3. Classify Error Type

**Error classification patterns:**

**Plugin Registration Errors:**
```
Indicators:
- "KtFakeCompilerPluginRegistrar"
- "CompilerPluginRegistrar not found"
- "META-INF/services"
- ServiceLoader exceptions
```

**IR Generation Errors:**
```
Indicators:
- "IrGenerationExtension"
- "UnifiedKtFakesIrGenerationExtension"
- "IR generation failed"
- IrElement exceptions
```

**FIR Detection Errors:**
```
Indicators:
- "FirExtension"
- "@Fake annotation"
- "FIR phase"
- Symbol resolution errors
```

**Generated Code Compilation Errors:**
```
Indicators:
- "build/generated/ktfake"
- Path containing generated files
- "Fake*Impl.kt"
- Kotlin syntax errors in generated files
```

**Dependency Errors:**
```
Indicators:
- "ClassNotFoundException"
- "NoClassDefFoundError"
- "Cannot resolve"
- Dependency resolution failures
```

**Type Resolution Errors:**
```
Indicators:
- "Type mismatch"
- "Unresolved reference"
- "Cannot infer type"
- Generic type issues
```

### 4. Extract Error Details

**For each error found:**

```
📋 ERROR CLASSIFICATION

Type: ${ERROR_TYPE}
Location: ${file_path}:${line_number}
Component: ${plugin|ir|fir|generated|dependency}

Error Message:
${full_error_message}

Context:
${surrounding_code_or_log_context}
```

**Parse stack traces:**
```bash
# Extract stack trace
grep -A 20 "Exception:" compilation.log

# Identify entry point
# Look for:
# - com.rsicarelli.fakt.compiler.* (our code)
# - org.jetbrains.kotlin.* (Kotlin compiler)
```

### 5. Analyze Plugin Registration Errors

**Common patterns:**

**Pattern 1: Plugin JAR Not Built**
```
❌ ERROR: Cannot find KtFakeCompilerPluginRegistrar

Root Cause:
- Compiler plugin JAR not built or out of date
- Shadow JAR task not executed
- Build cache issues

Solution:
1. Rebuild compiler plugin:
   cd fakt
   ./gradlew :compiler:clean :compiler:shadowJar

2. Verify JAR exists:
   ls -la compiler/build/libs/compiler-*.jar

3. Check JAR contents:
   jar tf compiler/build/libs/compiler-*.jar | grep CompilerPluginRegistrar

4. Re-run compilation across platforms:
   ./gradlew :samples:jvm-single-module:build
   ./gradlew :samples:android-single-module:build
   ./gradlew :samples:kmp-single-module:compileKotlinJvm

Verification:
✅ compiler-*.jar exists in build/libs/
✅ JAR contains KtFakeCompilerPluginRegistrar.class
✅ META-INF/services/ has registrar entry
```

**Pattern 2: META-INF/services Missing**
```
❌ ERROR: ServiceLoader cannot find plugin

Root Cause:
- META-INF/services configuration incorrect
- Shadow JAR not merging services correctly

Solution:
1. Check META-INF/services in JAR:
   jar tf compiler/build/libs/compiler-*.jar | grep META-INF/services

2. Should contain:
   META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar

3. Verify content:
   unzip -p compiler/build/libs/compiler-*.jar \
     META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar

4. Should show:
   com.rsicarelli.fakt.compiler.FaktCompilerPluginRegistrar
```

### 6. Analyze IR Generation Errors

**Common patterns:**

**Pattern 1: Interface Discovery Failure**
```
❌ ERROR: No @Fake interfaces found

Root Cause:
- @Fake annotation not applied
- FIR phase not detecting annotation
- Wrong source set

Solution:
1. Verify @Fake annotation present:
   grep -r "@Fake" src/*/kotlin/

2. Check interface definition:
   @Fake
   interface UserService { ... }

3. Verify plugin enabled in build.gradle.kts:
   fakt {
       enabled = true
   }

4. Check source set configuration:
   - Annotation should be in main/commonMain
   - Generated fakes go to test/commonTest
```

**Pattern 2: Type Resolution Failure**
```
❌ ERROR: Cannot resolve type 'User'

Root Cause:
- irTypeToKotlinString() method issues
- Import not generated
- Type not accessible in IR phase

Solution:
1. Check if type is in same module:
   find . -name "User.kt"

2. If cross-module, check imports:
   # Generated file should have:
   import com.example.User

3. Debug IR type:
   # Add logging to irTypeToKotlinString()
   println("Resolving type: ${irType.render()}")

4. Workaround: Use fully qualified names
   # In interface:
   fun getUser(): com.example.User
```

**Pattern 3: Generic Type Handling**
```
❌ ERROR: Type parameter 'T' not accessible

Root Cause:
- Method-level generic scoping challenge (Phase 2A)
- Class-level generic type erasure (Phase 2B)

Solution:
1. Identify generic pattern:
   Use generic-scoping-analyzer Skill

2. Check Phase support:
   - Method-level: Phase 2A needed
   - Class-level: Phase 2B or use concrete types

3. Temporary workaround:
   - Remove method-level generics
   - Use interface-level generics
   - OR use concrete types

For details:
- Consult: generic-scoping-analyzer Skill
- Reference: .claude/docs/implementation/generics/technical-reference.md
```

### 7. Analyze Generated Code Errors

**Read generated file:**
```bash
# Find generated file
interface_name="UserService"  # from context
generated_file=$(find build/generated/fakt -name "*${interface_name}*.kt" | head -n 1)

# Read file
Read ${generated_file}
```

**Check for common issues:**

**Syntax Errors:**
```bash
# Check for syntax issues
grep -n "TODO\|FIXME\|ERROR" ${generated_file}

# Check for unresolved references
grep -n "Unresolved" ${generated_file}

# Check for malformed code
kotlinc -jvm-target 11 ${generated_file} 2>&1 | head -20
```

**Type Safety Issues:**
```kotlin
// Look for:
❌ Type mismatch: Expected User, found Any
❌ Unresolved reference: User
❌ Cannot infer type parameter T
```

**Generated Code Validation Checklist:**
- [ ] All imports present
- [ ] Class declaration correct
- [ ] All methods implemented
- [ ] Behavior properties type-correct
- [ ] Factory function present
- [ ] Configuration DSL correct
- [ ] No TODO markers
- [ ] No syntax errors

### 8. Analyze Dependency Errors

**Pattern: ClassNotFoundException**
```
❌ ERROR: ClassNotFoundException: com.rsicarelli.fakt.compiler.FaktCompilerPluginRegistrar

Root Cause:
- Compiler plugin not published to local Maven
- Dependency version mismatch
- Gradle cache corruption

Solution:
1. Publish compiler to local Maven:
   cd fakt
   ./gradlew :compiler:publishToMavenLocal :gradle-plugin:publishToMavenLocal

2. Check local Maven repository:
   ls -la ~/.m2/repository/com/rsicarelli/fakt/compiler/

3. Verify version in build.gradle.kts:
   plugins {
       id("com.rsicarelli.fakt") version "1.0.0-alpha01"
   }

4. Clear Gradle cache if needed:
   rm -rf ~/.gradle/caches/
   ./gradlew clean

5. Rebuild:
   ./gradlew compileKotlinJvm
```

### 9. Provide Targeted Solutions

**Generate solution report:**

```
═══════════════════════════════════════════════════
🔧 COMPILATION ERROR ANALYSIS
═══════════════════════════════════════════════════

📋 ERROR TYPE: ${error_type}

❌ ERROR MESSAGE:
${error_message}

📍 LOCATION:
File: ${file_path}
Line: ${line_number}
Component: ${component}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔍 ROOT CAUSE ANALYSIS:

Primary cause: ${root_cause}
Contributing factors: ${factors}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

💡 SOLUTION:

Step 1: ${step_1}
Step 2: ${step_2}
Step 3: ${step_3}

Commands:
${command_1}
${command_2}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ VERIFICATION STEPS:

1. ${verification_1}
2. ${verification_2}
3. ${verification_3}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📚 REFERENCES:
- Troubleshooting Guide: ${doc_reference}
- Related Skill: ${skill_reference}
- Known Issue: ${known_issue_reference}

═══════════════════════════════════════════════════
```

### 10. Systematic Debugging Workflow

**If error not immediately identified:**

**Phase 1: Environment Validation**
```bash
# 1. Check Kotlin version
./gradlew --version | grep "Kotlin version"
# Expected: 2.0.0+

# 2. Check plugin build
ls -la fakt/compiler/build/libs/
# Should contain compiler-*.jar

# 3. Check local Maven
ls -la ~/.m2/repository/com/rsicarelli/fakt/
# Should have compiler and gradle-plugin
```

**Phase 2: Clean Rebuild**
```bash
# 1. Clean everything
./gradlew clean
rm -rf build/ */build/

# 2. Rebuild compiler
./gradlew :compiler:shadowJar

# 3. Publish to local Maven
./gradlew publishToMavenLocal

# 4. Retry compilation
./gradlew compileKotlinJvm --no-build-cache
```

**Phase 3: Incremental Debugging**
```bash
# 1. Test with simplest interface
@Fake
interface SimpleTest {
    fun getValue(): String
}

# 2. If works, gradually add complexity:
# - Add more methods
# - Add nullable types
# - Add suspend functions
# - Add generic types (last)

# 3. Identify breaking point
```

**Phase 4: Verbose Logging**
```bash
# Enable debug logging
./gradlew compileKotlinJvm --info 2>&1 | tee verbose.log

# Search for Fakt-specific output
grep -i "fakt\|fake" verbose.log

# Check IR generation phase
grep "IrGeneration" verbose.log
```

## Supporting Files

Progressive disclosure for error analysis:

- **`resources/error-catalog.md`** - Known error patterns and solutions (loaded on-demand)
- **`resources/troubleshooting-workflows.md`** - Step-by-step debugging procedures (loaded on-demand)
- **`resources/phase-specific-errors.md`** - Errors by implementation phase (loaded on-demand)

## Related Skills

This Skill composes with:
- **`compilation-validator`** - Validate after fixing errors
- **`generic-scoping-analyzer`** - For generic-related errors
- **`kotlin-ir-debugger`** - Debug IR generation issues
- **`interface-analyzer`** - Analyze problematic interfaces

## Error Categories

### By Severity
- **BLOCKER**: Cannot proceed (plugin not loading)
- **CRITICAL**: Generation fails (IR errors)
- **MAJOR**: Generated code doesn't compile
- **MINOR**: Warnings or non-blocking issues

### By Component
- **Plugin**: Registration and configuration
- **FIR**: Annotation detection
- **IR**: Code generation
- **Generated**: Output compilation
- **Environment**: Dependencies and setup

## Best Practices

1. **Read logs first** - Error messages contain most info
2. **Classify before solving** - Correct classification → faster solution
3. **Clean rebuild** - Eliminates cache issues
4. **Incremental debugging** - Start simple, add complexity
5. **Verify solution** - Ensure fix actually works

## Common Error Patterns

### Pattern: "Unresolved reference: Fake*Impl"
**Solution**: Generated code not in classpath, rebuild

### Pattern: "Type mismatch: Expected User, found Any"
**Solution**: Generic type erasure, use concrete types or wait for Phase 2

### Pattern: "Plugin not applied correctly"
**Solution**: Check build.gradle.kts plugin configuration

### Pattern: "Cannot find @Fake annotation"
**Solution**: Verify annotation import and FIR phase

## Quick Diagnostics

**One-liner checks:**
```bash
# Plugin JAR exists?
ls -la fakt/compiler/build/libs/compiler-*.jar

# Published to Maven?
ls -la ~/.m2/repository/com/rsicarelli/fakt/compiler/

# Generated code exists?
find build/generated/fakt -name "*.kt"

# Recent errors?
./gradlew compileKotlinJvm 2>&1 | grep -E "error:|ERROR|Exception"
```

## Performance Notes

- Error log analysis: ~5-10 seconds
- JAR inspection: ~2-5 seconds
- Clean rebuild: ~30-60 seconds
- Total debugging cycle: ~1-2 minutes

Fast iteration for error resolution!
