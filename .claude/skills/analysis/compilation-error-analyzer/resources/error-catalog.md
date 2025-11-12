# Compilation Error Catalog

Known error patterns with solutions for Fakt development.

## Plugin Registration Errors

### ERROR-001: CompilerPluginRegistrar Not Found
```
Error: java.util.ServiceConfigurationError: org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar:
Provider com.rsicarelli.fakt.compiler.FaktCompilerPluginRegistrar not found
```

**Cause**: Plugin JAR not built or not in classpath
**Solution**: `./gradlew :compiler:shadowJar`
**Verification**: `jar tf compiler/build/libs/compiler-*.jar | grep FaktCompilerPluginRegistrar`

---

### ERROR-002: Duplicate Plugin Registration
```
Error: Multiple CompilerPluginRegistrar implementations found
```

**Cause**: Conflicting plugin versions in classpath
**Solution**: Clear Gradle cache + clean rebuild
```bash
rm -rf ~/.gradle/caches/
./gradlew clean :compiler:shadowJar
```

---

### ERROR-003: Plugin Not Applied
```
Error: Plugin 'com.rsicarelli.fakt' not found
```

**Cause**: Plugin not published to Maven Local
**Solution**:
```bash
./gradlew :compiler:publishToMavenLocal :gradle-plugin:publishToMavenLocal
```

---

## IR Generation Errors

### ERROR-101: Cannot Resolve Type
```
Error: e: Unresolved reference: User
```

**Cause**: Type not accessible in generated code scope
**Solution**: Check cross-module imports, ensure type is public

---

### ERROR-102: Generic Type Scoping
```
Error: Type parameter 'T' is not accessible at class level
```

**Cause**: Method-level generic (Phase 2A needed)
**Solution**:
1. Wait for Phase 2A
2. OR refactor to interface-level generic
3. OR use concrete types

---

### ERROR-103: IrGenerationExtension Exception
```
Error: java.lang.IllegalStateException: IR generation failed
```

**Cause**: Bug in IR generation code
**Solution**: Check logs for specific error, debug with kotlin-ir-debugger Skill

---

## Generated Code Errors

### ERROR-201: Syntax Error in Generated File
```
Error: Expecting a top level declaration
File: build/generated/fakt/test/kotlin/FakeUserServiceImpl.kt
```

**Cause**: Generated code has syntax error
**Solution**:
1. Read generated file: `Read build/generated/fakt/test/kotlin/FakeUserServiceImpl.kt`
2. Identify syntax issue
3. Fix generator logic in compiler plugin

---

### ERROR-202: Type Mismatch
```
Error: Type mismatch: Expected User, found Any
```

**Cause**: Generic type erasure (interface-level generic)
**Solution**: Phase 2B needed OR use concrete type

---

### ERROR-203: Unresolved Reference in Generated Code
```
Error: Unresolved reference: Result
```

**Cause**: Missing import in generated code
**Solution**: Update ImportResolver to include kotlin.Result

---

## Dependency Errors

### ERROR-301: ClassNotFoundException
```
Error: ClassNotFoundException: com.rsicarelli.fakt.runtime.Fake
```

**Cause**: Runtime dependency missing
**Solution**: Add to build.gradle.kts:
```kotlin
dependencies {
    implementation("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT")
}
```

---

### ERROR-302: Version Conflict
```
Error: Dependency resolution failed: conflicting versions
```

**Cause**: Mismatched versions between compiler and gradle plugin
**Solution**: Ensure same version for both:
```kotlin
id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
```

---

## Kotlin Compiler API Errors

### ERROR-401: IrFactory Method Not Found
```
Error: NoSuchMethodError: IrFactory.createClass
```

**Cause**: Using deprecated Kotlin compiler API
**Solution**: Use builder pattern:
```kotlin
irFactory.buildClass { ... }
```

---

### ERROR-402: Descriptor Access Deprecated
```
Warning: Accessing 'descriptor' is deprecated
```

**Cause**: Using K1 descriptor API in K2
**Solution**: Use IR-native APIs, avoid descriptor access

---

## Build Configuration Errors

### ERROR-501: Kotlin Version Mismatch
```
Error: This plugin requires Kotlin 2.0.0+
```

**Cause**: Project using Kotlin 1.x
**Solution**: Update to Kotlin 2.0.0+:
```kotlin
kotlin("jvm") version "2.2.20"
```

---

### ERROR-502: Source Set Not Found
```
Error: Source set 'commonTest' not found
```

**Cause**: Not a KMP project or misconfigured
**Solution**: For JVM-only:
```kotlin
fakt {
    outputDir.set(layout.buildDirectory.dir("generated/fakt/test/kotlin"))
}
```

---

## Phase-Specific Errors

### Phase 1 Limitations

**ERROR-601: Method-Level Generics Not Supported**
```
Error: Cannot generate behavior property for method-level generic <T>
```

**Status**: Phase 2A feature
**Workaround**: Use interface-level generics or concrete types

---

**ERROR-602: Complex Generic Constraints**
```
Warning: Generic constraint 'T : Comparable<T>' not fully preserved
```

**Status**: Partial Phase 1 support
**Impact**: Type safety reduced, manual verification needed

---

### Phase 2A In Progress

**ERROR-701: Dynamic Cast Warning**
```
Warning: Unchecked cast: Any? to T
```

**Status**: Expected in Phase 2A (using @Suppress)
**Impact**: Runtime type safety, safe if used correctly

---

## Troubleshooting Quick Reference

| Error Pattern | Category | Priority | Typical Fix |
|---------------|----------|----------|-------------|
| ServiceLoader | Plugin | BLOCKER | Rebuild JAR |
| Type mismatch | IR | CRITICAL | Check types |
| Unresolved ref | Generated | MAJOR | Add imports |
| ClassNotFound | Dependency | BLOCKER | publishToMavenLocal |
| NoSuchMethod | API | CRITICAL | Update API usage |

---

## Error Resolution Flowchart

```
Error Occurs
    |
    ├─ Plugin Loading? ──> Check JAR + META-INF/services
    ├─ IR Generation? ──> Check interface structure
    ├─ Generated Code? ──> Read generated file
    ├─ Dependencies? ──> Check Maven Local
    └─ Unknown? ──────> Clean rebuild + verbose logging
```

---

## Prevention Checklist

**Before reporting bug:**
- [ ] Clean rebuild attempted
- [ ] Published to Maven Local
- [ ] Verified Kotlin version (2.0.0+)
- [ ] Checked generated code exists
- [ ] Reviewed error catalog
- [ ] Tried with simple interface

**If still failing:**
- Create minimal reproduction
- Capture full logs (`--info` flag)
- Check known issues
- Consult troubleshooting-workflows.md
