# Systematic Troubleshooting Workflows

Step-by-step procedures for diagnosing Fakt compilation errors.

## Workflow 1: First-Time Setup Failure

**Symptoms**: Plugin not loading, no generation happening

### Steps

1. **Verify Plugin Build**
   ```bash
   cd fakt
   ./gradlew :compiler:shadowJar
   ls -la compiler/build/libs/compiler-*.jar
   ```
   ✅ JAR should exist (~5-10MB)

2. **Publish to Maven Local**
   ```bash
   ./gradlew :compiler:publishToMavenLocal :gradle-plugin:publishToMavenLocal
   ls -la ~/.m2/repository/com/rsicarelli/fakt/
   ```
   ✅ Should see compiler/ and gradle-plugin/ directories

3. **Verify Project Configuration**
   ```kotlin
   // build.gradle.kts
   plugins {
       id("com.rsicarelli.fakt") version "1.0.0-alpha01"
   }

   fakt {
       enabled = true
   }
   ```

4. **Test with Simple Interface**
   ```kotlin
   @Fake
   interface SimpleTest {
       fun getValue(): String
   }
   ```

5. **Compile**
   ```bash
   ./gradlew compileKotlinJvm --no-build-cache
   ```

6. **Verify Generation**
   ```bash
   find build/generated/fakt -name "*.kt"
   ```
   ✅ Should see FakeSimpleTestImpl.kt

---

## Workflow 2: Regression After Changes

**Symptoms**: Was working, now broken after code changes

### Steps

1. **Identify What Changed**
   ```bash
   git diff HEAD~1
   ```
   Focus on:
   - Interface modifications
   - Compiler plugin code
   - Build configuration

2. **Isolate Change**
   ```bash
   git stash
   ./gradlew compileKotlinJvm  # Does it work now?
   git stash pop
   ```

3. **Incremental Restoration**
   - Revert one change at a time
   - Test after each revert
   - Identify breaking change

4. **Analyze Breaking Change**
   - What was modified?
   - Why does it break generation?
   - Is it expected (new feature) or bug?

5. **Apply Fix**
   - Fix compiler plugin OR
   - Adjust interface to supported pattern

---

## Workflow 3: Generated Code Doesn't Compile

**Symptoms**: Generation succeeds, but output has compilation errors

### Steps

1. **Locate Generated File**
   ```bash
   interface_name="UserService"
   generated=$(find build/generated/fakt -name "*${interface_name}*.kt")
   echo $generated
   ```

2. **Read Generated Code**
   ```bash
   Read $generated
   ```

3. **Identify Error Type**
   - [ ] Syntax error (missing semicolon, bracket, etc.)
   - [ ] Unresolved reference (import missing)
   - [ ] Type mismatch (wrong type generated)
   - [ ] Generic type issue

4. **Compare with Source Interface**
   ```bash
   # Read original interface
   find src -name "*${interface_name}.kt" -exec cat {} \;
   ```

5. **Determine Root Cause**
   - Is interface complex (generics)?
   - Are types accessible (cross-module)?
   - Is it a known limitation (Phase 2)?

6. **Apply Fix**
   - Simplify interface (if possible)
   - Fix compiler plugin generator
   - Wait for appropriate Phase support

---

## Workflow 4: No Errors But Nothing Generated

**Symptoms**: Compilation succeeds, but no fake files generated

### Steps

1. **Check Plugin Enabled**
   ```kotlin
   fakt {
       enabled = true  // Verify this is set
   }
   ```

2. **Verify @Fake Annotation**
   ```bash
   grep -r "@Fake" src/
   ```
   ✅ Should find annotated interfaces

3. **Check Source Set**
   ```bash
   # Is @Fake in correct source set?
   # Should be in main/commonMain, not test/commonTest
   ```

4. **Enable Verbose Logging**
   ```bash
   ./gradlew compileKotlinJvm --info 2>&1 | tee verbose.log
   grep -i "fakt" verbose.log
   ```

5. **Check FIR Detection**
   ```bash
   # Look for FIR phase output in logs
   grep "FirExtension\|@Fake" verbose.log
   ```

6. **Debug IR Phase**
   ```bash
   # Look for IR generation phase
   grep "IrGeneration" verbose.log
   ```

---

## Workflow 5: Generic Type Errors

**Symptoms**: Errors related to type parameters T, R, etc.

### Steps

1. **Identify Generic Pattern**
   ```kotlin
   // Interface-level?
   interface Repository<T> { ... }

   // Method-level?
   interface Processor {
       fun <T> process(data: T): T
   }

   // Mixed?
   interface Cache<K, V> {
       fun <R : V> compute(...): R
   }
   ```

2. **Check Phase Support**
   - Interface-level: Phase 2B needed OR use concrete
   - Method-level: Phase 2A needed OR refactor
   - Mixed: Both phases needed

3. **Use generic-scoping-analyzer Skill**
   ```
   Invoke: generic-scoping-analyzer ${interface_name}
   ```

4. **Choose Strategy**
   - **Option A**: Use concrete types (Phase 1)
   - **Option B**: Accept type erasure (Phase 1)
   - **Option C**: Wait for Phase 2A/2B

5. **Implement Chosen Strategy**
   (See generation-strategies.md in interface-analyzer)

---

## Workflow 6: Cross-Module Type Resolution

**Symptoms**: Types from other modules not found in generated code

### Steps

1. **Verify Type Accessibility**
   ```kotlin
   // Type must be public
   public data class User(...)  // ✅

   // Not internal
   internal data class User(...)  // ❌
   ```

2. **Check Import Generation**
   ```bash
   # Read generated file
   Read $generated_file

   # Should have import
   import com.example.User
   ```

3. **Debug ImportResolver**
   - Check ImportResolver.kt in compiler plugin
   - Verify cross-module import logic

4. **Workaround: Fully Qualified Names**
   ```kotlin
   @Fake
   interface UserService {
       fun getUser(): com.example.User  // Explicit package
   }
   ```

---

## Workflow 7: Plugin Version Mismatch

**Symptoms**: "Plugin version X expected, found Y"

### Steps

1. **Check versions**
   ```bash
   # In build.gradle.kts
   grep "com.rsicarelli.fakt" build.gradle.kts

   # In Maven Local
   ls ~/.m2/repository/com/rsicarelli/fakt/compiler/
   ```

2. **Ensure Consistency**
   ```kotlin
   // All should be same version
   id("com.rsicarelli.fakt") version "1.0.0-alpha01"
   ```

3. **Re-publish if Needed**
   ```bash
   ./gradlew publishToMavenLocal --rerun-tasks
   ```

4. **Clear Gradle Cache**
   ```bash
   rm -rf ~/.gradle/caches/modules-2/files-2.1/com.rsicarelli.fakt/
   ```

---

## Workflow 8: Incremental Compilation Issues

**Symptoms**: Changes not reflected, stale output

### Steps

1. **Disable Incremental**
   ```bash
   ./gradlew compileKotlinJvm --no-build-cache --rerun-tasks
   ```

2. **Clean Generated Files**
   ```bash
   rm -rf build/generated/fakt/
   ```

3. **Full Clean Rebuild**
   ```bash
   ./gradlew clean
   ./gradlew :compiler:shadowJar
   ./gradlew publishToMavenLocal
   ./gradlew compileKotlinJvm
   ```

4. **Verify Fresh Output**
   ```bash
   # Check modification time
   ls -lt build/generated/fakt/test/kotlin/
   ```

---

## Quick Decision Tree

```
Problem?
    |
    ├─ Plugin not loading?
    │   └─ Workflow 1: First-Time Setup
    │
    ├─ Was working, now broken?
    │   └─ Workflow 2: Regression
    │
    ├─ Generated code error?
    │   └─ Workflow 3: Generated Code
    │
    ├─ Nothing generated?
    │   └─ Workflow 4: No Generation
    │
    ├─ Generic type error?
    │   └─ Workflow 5: Generics
    │
    ├─ Cross-module error?
    │   └─ Workflow 6: Cross-Module
    │
    ├─ Version mismatch?
    │   └─ Workflow 7: Versions
    │
    └─ Stale output?
        └─ Workflow 8: Incremental
```

---

## Emergency "Nuclear Option"

**When all else fails:**

```bash
# 1. Clean everything
./gradlew clean
rm -rf build/ */build/ .gradle/
rm -rf ~/.gradle/caches/
rm -rf ~/.m2/repository/com/rsicarelli/fakt/

# 2. Rebuild from scratch
cd fakt
./gradlew :compiler:shadowJar
./gradlew :compiler:publishToMavenLocal
./gradlew :gradle-plugin:publishToMavenLocal

# 3. Test in clean project
cd ..
mkdir test-project
cd test-project
gradle init
# Add plugin
# Test with simple interface

# 4. If works: issue was in your project
# 5. If fails: issue is in plugin itself
```

---

## Logging Strategy

**Progressive verbosity:**

1. **Normal** (first try):
   ```bash
   ./gradlew compileKotlinJvm
   ```

2. **Info** (if issues):
   ```bash
   ./gradlew compileKotlinJvm --info 2>&1 | tee info.log
   ```

3. **Debug** (deep investigation):
   ```bash
   ./gradlew compileKotlinJvm --debug 2>&1 | tee debug.log
   ```

4. **Stacktrace** (exceptions):
   ```bash
   ./gradlew compileKotlinJvm --stacktrace
   ```

---

## Prevention

**Best practices to avoid errors:**

1. **Test incrementally** - Add complexity gradually
2. **Commit working states** - Easy rollback
3. **Clean rebuild regularly** - Catch cache issues
4. **Verify each change** - Compile after modifications
5. **Use simple interfaces first** - Prove plugin works
6. **Check Phase support** - Know what's supported when

---

## When to Ask for Help

**Escalate if:**
- [ ] Followed appropriate workflow
- [ ] Nuclear option attempted
- [ ] Minimal reproduction created
- [ ] Logs captured
- [ ] Still failing

**Provide:**
- Error messages (full)
- Interface definition
- Build configuration
- Kotlin version
- Steps to reproduce
