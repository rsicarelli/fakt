# IR Generation Troubleshooting Guide

> **Loaded on-demand** for diagnosing and fixing IR generation issues

## Common Issues and Solutions

### 1. Interface Not Found

**Symptom:**
```
❌ ERROR: Interface 'MyService' not found in module
```

**Causes:**
- Interface name typo
- Missing `@Fake` annotation
- Interface in wrong source set
- Module not compiled yet

**Solutions:**
1. **Verify interface exists:**
   ```bash
   grep -r "interface MyService" samples/
   ```

2. **Check @Fake annotation:**
   ```kotlin
   @Fake  // Must be present
   interface MyService
   ```

3. **Verify source set:**
   ```
   KMP: commonMain/ for production, commonTest/ for testing
   JVM: main/ or test/
   ```

4. **Rebuild module:**
   ```bash
   ./gradlew :samples:kmp-single-module:clean build
   ```

---

### 2. Generated Code Doesn't Compile

**Symptom:**
```
❌ ERROR: Unresolved reference: User
```

**Causes:**
- Missing imports for cross-module types
- Generic type parameter issues
- Suspend function modifier missing

**Solutions:**

**Missing Imports (Phase 2.4 priority):**
```kotlin
// Current limitation: Cross-module imports not auto-generated
// Workaround: Define interfaces in same module as tests
```

**Generic Issues:**
```kotlin
// Check if class-level generics (Phase 2 challenge)
interface Repository<T>  // → Replace T with Any? in Phase 1

// Or method-level generics (should work)
fun <T> identity(value: T): T  // → Identity function pattern
```

**Suspend Modifier:**
```kotlin
// Ensure suspend preserved in generated code
override suspend fun fetchData(): Result<Data> {
    return fetchDataBehavior()
}
```

---

### 3. Compilation Timeout

**Symptom:**
```
⏰ TIMEOUT: Compilation did not complete within 60s
```

**Causes:**
- Infinite loop in IR generation
- Circular dependency in types
- Too many interfaces processed

**Solutions:**

1. **Enable verbose logging:**
   ```kotlin
   fakt {
       logLevel.set(LogLevel.DEBUG)
   }
   ```

2. **Check for circular dependencies:**
   ```kotlin
   interface A {
       fun getB(): B
   }
   interface B {
       fun getA(): A
   }
   ```

3. **Reduce scope:**
   ```bash
   # Test one interface at a time
   ./gradlew build --info | grep "Generating fake for"
   ```

---

### 4. Generic Type Mismatch

**Symptom:**
```
❌ Type mismatch: inferred type is Any? but User was expected
```

**Cause:**
- Class-level generic T replaced with Any? (Phase 2 challenge)

**Solution:**

**Current Phase 1 workaround:**
```kotlin
// Accept Any? at declaration
val repo: Repository<Any?> = FakeRepositoryImpl()

// Cast at use-site in tests
repo.configureSave { item ->
    val user = item as User
    // test logic
}
```

**Phase 2 target:**
```kotlin
// Future type-safe version:
val repo: Repository<User> = fakeRepository<User> {
    save { user -> /* already typed as User */ }
}
```

---

### 5. FIR Phase Detection Failure

**Symptom:**
```
⚠️ WARNING: @Fake annotation not detected in FIR phase
```

**Causes:**
- FaktFirExtensionRegistrar not registered
- Annotation processing order issue
- Kotlin compiler plugin not applied

**Solutions:**

1. **Verify plugin applied:**
   ```kotlin
   // In build.gradle.kts
   plugins {
       id("com.rsicarelli.fakt") version "1.0.0-alpha01"
   }
   ```

2. **Check FIR registration:**
   ```bash
   ./gradlew build --info | grep "FaktFirExtensionRegistrar"
   ```

3. **Validate annotation import:**
   ```kotlin
   import com.rsicarelli.fakt.annotation.Fake  // Correct import

   @Fake
   interface MyService
   ```

---

### 6. Factory Function Not Generated

**Symptom:**
```
❌ Unresolved reference: fakeMyService
```

**Causes:**
- Factory generation disabled
- Output directory issue
- Name collision

**Solutions:**

1. **Check factory generation:**
   ```bash
   find build/generated/fakt -name "*Factory.kt"
   # Or check in same file as FakeXxxImpl
   ```

2. **Verify naming convention:**
   ```kotlin
   // For interface UserService:
   val fake = fakeUserService {  // camelCase with 'fake' prefix
       // configuration
   }
   ```

3. **Check for name collisions:**
   ```kotlin
   // If you have:
   fun fakeUserService() { }  // Custom function

   // And generated:
   fun fakeUserService(configure: ...) { }  // Collision!

   // Solution: Rename custom function or use qualified name
   ```

---

### 7. Configuration DSL Missing Methods

**Symptom:**
```
❌ Unresolved reference: configureGetUser
```

**Causes:**
- DSL generation failed
- Method name transformation issue
- Property vs method confusion

**Solutions:**

1. **Check generated Config class:**
   ```bash
   grep -A 10 "class FakeUserServiceConfig" build/generated/fakt/.../FakeUserServiceImpl.kt
   ```

2. **Verify naming pattern:**
   ```kotlin
   // For method: getUser()
   // DSL method: configureGetUser() or just getUser() in DSL

   fakeUserService {
       getUser { User("test") }  // Common pattern
   }
   ```

3. **Properties vs Methods:**
   ```kotlin
   // Property: val currentUser: User
   fakeService {
       currentUser = User("default")  // Property assignment
   }

   // Method: fun getUser(): User
   fakeService {
       getUser { User("test") }  // Method configuration
   }
   ```

---

### 8. KMP Multi-Module Issues

**Symptom:**
```
❌ Generated fakes appear in wrong source set
```

**Causes:**
- Source set detection failure
- commonTest vs jvmTest confusion
- Module dependency not configured

**Solutions:**

1. **Verify source set:**
   ```
   Expected for tests: commonTest/kotlin/
   Expected for main: commonMain/kotlin/
   ```

2. **Check module dependencies:**
   ```kotlin
   // In test module build.gradle.kts:
   kotlin {
       sourceSets {
           commonTest {
               dependencies {
                   implementation(projects.core.userFakes)  // Fakes module
               }
           }
       }
   }
   ```

3. **Use explicit output directory:**
   ```kotlin
   fakt {
       outputDir.set(layout.buildDirectory.dir("generated/fakt/commonTest/kotlin"))
   }
   ```

---

## Diagnostic Workflow

When debugging IR generation issues:

1. **Enable DEBUG logging:**
   ```kotlin
   fakt {
       logLevel.set(LogLevel.DEBUG)
   }
   ```

2. **Clean build:**
   ```bash
   ./gradlew clean build --info
   ```

3. **Check generated code:**
   ```bash
   ls -la build/generated/fakt/
   cat build/generated/fakt/.../FakeXxxImpl.kt
   ```

4. **Validate compilation:**
   ```bash
   ./gradlew compileTestKotlinJvm
   # or
   ./gradlew compileKotlinJvm
   ```

5. **Run tests:**
   ```bash
   ./gradlew test
   ```

## Getting Help

If issues persist:

1. **Consult related Skills:**
   - `compilation-validator` - Verify generated code compiles
   - `metro-pattern-validator` - Check architectural alignment
   - `kotlin-api-consultant` - Validate Kotlin APIs

2. **Check documentation:**
   - `.claude/docs/troubleshooting/common-issues.md`
   - `.claude/docs/development/metro-alignment.md`

3. **Enable TRACE logging:**
   ```kotlin
   fakt {
       logLevel.set(LogLevel.TRACE)
   }
   ```

4. **Create minimal reproduction:**
   ```kotlin
   // Isolate issue to simplest possible interface
   @Fake
   interface MinimalTest {
       fun simple(): String
   }
   ```

## Known Issues (Phase 1)

- ⚠️ Class-level generics lose type safety (Phase 2 target)
- ⚠️ Cross-module imports not auto-generated (Phase 2.4 priority)
- ⚠️ Variance annotations not fully supported
- ⚠️ Complex generic bounds need manual handling

## Metro Alignment Check

If debugging Metro pattern violations:

```bash
# Use metro-pattern-validator Skill
"Validate Metro alignment for UnifiedFaktIrGenerationExtension"

# Or consult Metro source directly:
/consult-kotlin-api MetroIrGenerationExtension
```
