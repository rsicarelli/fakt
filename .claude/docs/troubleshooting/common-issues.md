# Common Issues & Solutions - KtFakes Development

> **Purpose**: Quick solutions for common development issues with KtFakes compiler plugin
> **Audience**: Developers working on the unified IR-native architecture
> **Maintenance**: Update when new issues are discovered

## 🚨 **Compilation Issues**

### **Issue: "Cannot find @Fake annotation"**
```
Error: Unresolved reference: Fake
```

**Solution:**
```kotlin
// Ensure proper dependency in build.gradle.kts
dependencies {
    implementation("dev.rsicarelli.ktfake:annotations:$ktfakeVersion")
    testImplementation("dev.rsicarelli.ktfake:compiler:$ktfakeVersion")
}
```

**Root Cause**: Missing annotation dependency in module

### **Issue: "Fake generation not happening"**
```
// @Fake interface exists but no fakeXxx() function generated
```

**Solution:**
1. Verify test source set detection:
```bash
./gradlew :test-sample:jvmTest --info | grep "Source set"
```

2. Check module name contains "test":
```kotlin
// Module must be named with "test" or "sample"
// e.g., test-sample, integration-test, unit-test
```

**Root Cause**: Security constraint - fakes only generated in test modules

### **Issue: "Generated code doesn't compile"**
```
Type mismatch: inferred type is Any but String was expected
```

**Solution:**
1. Check interface method signatures:
```kotlin
@Test
fun `GIVEN interface with complex types WHEN generating THEN should handle types correctly`() = runTest {
    // Validate type resolution in UnifiedKtFakesIrGenerationExtensionTest
}
```

2. Verify type mapping:
```kotlin
// Check irTypeToKotlinString() method for type resolution
```

**Root Cause**: Type resolution issue in IR generation

## 🔧 **IDE Integration Issues**

### **Issue: "Generated fakes not visible in IDE"**
```
// fakeService() function exists but IDE shows red
```

**Solution:**
1. Refresh Gradle project:
```bash
./gradlew clean build
```

2. Invalidate IDE caches:
```
File > Invalidate Caches and Restart
```

**Root Cause**: IDE cache not updated with generated code

### **Issue: "Duplicate class errors"**
```
Class 'FakeServiceImpl' is defined multiple times
```

**Solution:**
1. Clean generated files:
```bash
rm -rf build/generated/ktfake/
./gradlew clean
```

2. Check for multiple @Fake annotations:
```kotlin
// Ensure interface has only one @Fake annotation
@Fake
interface Service { ... }
```

**Root Cause**: Multiple generation runs without cleanup

## ⚡ **Testing Issues**

### **Issue: "Tests fail with ClassNotFoundException"**
```
java.lang.ClassNotFoundException: FakeServiceImpl
```

**Solution:**
1. Verify test dependencies:
```kotlin
// In test module build.gradle.kts
dependencies {
    testImplementation("dev.rsicarelli.ktfake:compiler:$version")
}
```

2. Check compilation order:
```bash
./gradlew :test-sample:compileTestKotlin
./gradlew :test-sample:jvmTest
```

**Root Cause**: Test compilation before fake generation

### **Issue: "Suspend functions not working in fakes"**
```
// suspend fun in interface but generated fake doesn't preserve suspend
```

**Solution:**
1. Check current implementation status:
```kotlin
@Test
fun `GIVEN interface with suspend methods WHEN generating THEN should preserve suspend modifier`() = runTest {
    // Validate in UnifiedKtFakesIrGenerationExtensionTest
}
```

2. Verify behavior configuration:
```kotlin
val fake = fakeAsyncService {
    getUser { "test-user" } // Use suspend lambda
}
```

**Root Cause**: Known limitation - suspend function handling in Phase 2

## 🎯 **Generic Type Issues**

### **Issue: "Generic types cause compilation errors"**
```
Type parameter T is not accessible at class level
```

**Solution:**
1. **Current Workaround** (Phase 1):
```kotlin
// Use Any type with casting
interface GenericService<T> {
    fun process(data: T): T
}

// Generated (current approach):
private var processBehavior: (Any?) -> Any? = { it }
override fun <T> process(data: T): T {
    @Suppress("UNCHECKED_CAST")
    return processBehavior(data) as T
}
```

2. **Phase 2A Enhancement** (upcoming):
```kotlin
// Identity function approach
private var processBehavior: (Any?) -> Any? = { it }
```

**Root Cause**: Method-level generics not accessible at class level - core architectural challenge

### **Issue: "Complex generic types not supported"**
```
interface Repository<T : Entity> {
    fun <R : Result<T>> process(): R
}
```

**Solution:**
1. **Current**: Use simplified interface:
```kotlin
@Fake
interface SimpleRepository {
    fun processEntity(): String // Use concrete types
}
```

2. **Future**: Wait for Phase 2B implementation

**Root Cause**: Complex generic constraints require Phase 2B implementation

## 🔍 **Debugging Issues**

### **Issue: "Can't debug IR generation"**
```
// No visibility into what's being generated
```

**Solution:**
1. Enable debug output:
```kotlin
// Add to compiler plugin configuration
ktfake {
    debug = true
}
```

2. Use debugging commands:
```bash
# Check generated IR structure
.claude/commands/debug-ir-generation.md

# Analyze specific interface
.claude/commands/analyze-interface-structure.md
```

**Root Cause**: Limited debugging tools for IR generation

### **Issue: "Error messages not helpful"**
```
// Generic "compilation failed" without details
```

**Solution:**
1. Check message collector output:
```kotlin
// In test, verify diagnostic messages
@Test
fun `GIVEN invalid interface WHEN generating THEN should report clear error`() = runTest {
    // Validate error message quality
}
```

2. Use verbose compilation:
```bash
./gradlew :test-sample:jvmTest --debug
```

**Root Cause**: Diagnostic reporting needs enhancement

## 📊 **Performance Issues**

### **Issue: "Compilation takes too long"**
```
// Build time significantly increased with KtFakes
```

**Solution:**
1. Profile compilation:
```bash
./gradlew :test-sample:jvmTest --profile
```

2. Check interface count:
```kotlin
// Limit @Fake interfaces per module
// Consider splitting large test modules
```

**Root Cause**: Each @Fake interface adds compilation overhead

### **Issue: "Memory issues during compilation"**
```
OutOfMemoryError during fake generation
```

**Solution:**
1. Increase heap size:
```bash
export GRADLE_OPTS="-Xmx4g -XX:MaxMetaspaceSize=1g"
```

2. Reduce batch size:
```kotlin
// Generate fewer interfaces per compilation unit
```

**Root Cause**: IR manipulation memory intensive

## 🔗 **Cross-Module Issues**

### **Issue: "Fakes not visible across modules"**
```
// Can't use fakes from different test modules
```

**Solution:**
1. **Current**: Copy interface to each test module:
```kotlin
// test-module-a/src/jvmTest/kotlin/
@Fake
interface SharedService { ... }

// test-module-b/src/jvmTest/kotlin/
@Fake
interface SharedService { ... } // Duplicate
```

2. **Future**: Wait for cross-module dependency feature

**Root Cause**: Cross-module fake coordination not implemented

## 🚀 **Quick Diagnostic Commands**

### **Check System Health**
```bash
# Verify all tests pass
./gradlew :compiler:test :test-sample:jvmTest

# Check generated code location
ls -la build/generated/ktfake/test/kotlin/

# Verify plugin registration
./gradlew :test-sample:dependencies | grep ktfake
```

### **Common Fixes**
```bash
# Nuclear reset
./gradlew clean
rm -rf build/generated/
./gradlew build

# IDE refresh
./gradlew --refresh-dependencies build
```

### **Get Help**
```bash
# Check latest documentation
cat .claude/docs/implementation/current-status.md

# Review testing guidelines
cat .claude/docs/validation/testing-guidelines.md

# Analyze specific issue
.claude/commands/analyze-compilation-error.md
```

## 🎯 **Phase 2 Known Limitations**

### **Not Yet Implemented**
- Complex generic type constraints
- Cross-module fake dependencies
- Call tracking (@Fake(trackCalls = true))
- Builder patterns (@Fake(builder = true))
- Advanced error diagnostics

### **Workarounds Available**
- Use concrete types instead of complex generics
- Duplicate interfaces across test modules
- Manual call verification
- Traditional object construction
- Check compiler output manually

---

**For complex issues not covered here, consult the technical documentation or create a test case following GIVEN-WHEN-THEN patterns to reproduce the issue.**