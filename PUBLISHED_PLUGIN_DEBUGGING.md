# Published Plugin Runtime Debugging

**Date:** September 30, 2025
**Status:** Build-logic complete ✅ | Runtime integration blocked ❌

## Executive Summary

The build-logic infrastructure and Maven publishing are **fully functional**. All modules publish correctly to Maven Local. However, when samples try to use the **published plugin**, the compiler plugin doesn't generate fakes.

**Root Cause:** The Gradle plugin applies correctly to main compilations, but the **compiler plugin** (the FIR/IR code generator) is not executing or not finding `@Fake` annotations.

---

## What Works ✅

### 1. Build-Logic Infrastructure
- ✅ Created `build-logic/` composite build
- ✅ 4 convention plugins:
  - `FaktBasePlugin` - Common Kotlin configuration
  - `FaktPublishingPlugin` - Maven publishing (marker plugin)
  - `FaktKotlinJvmPlugin` - JVM module conventions
  - `FaktMultiplatformPlugin` - KMP module conventions
- ✅ All plugins compile and apply correctly

### 2. Maven Publishing
- ✅ vanniktech maven-publish plugin v0.34.0 configured
- ✅ Removed incompatible `signAllPublications()` call
- ✅ All 3 modules published to Maven Local:
  ```bash
  ~/.m2/repository/com/rsicarelli/fakt/
  ├── compiler/1.0.0-SNAPSHOT/
  ├── runtime/1.0.0-SNAPSHOT/
  └── gradle-plugin/1.0.0-SNAPSHOT/
  ```
- ✅ Artifact names corrected: `compiler` and `runtime` (not `ktfake-*`)

### 3. Runtime Annotations
- ✅ Added `public` visibility to all runtime annotations
- ✅ Explicit API mode working for runtime module
- ✅ Annotations: `@Fake`, `@FakeConfig`, `@CallTracking`

### 4. Gradle Plugin Configuration
- ✅ Fixed artifact names in gradle-plugin:
  ```kotlin
  const val PLUGIN_ARTIFACT_NAME = "compiler"  // was "ktfake-compiler"
  const val PLUGIN_GROUP_ID = "com.rsicarelli.fakt"
  ```
- ✅ Fixed runtime dependency injection:
  ```kotlin
  "${PLUGIN_GROUP_ID}:runtime:${PLUGIN_VERSION}"  // was "ktfake-runtime"
  ```
- ✅ Removed unsupported compiler options (`generateCallTracking`, etc.)
- ✅ Fixed `isApplicable` to target main compilations:
  ```kotlin
  val isMainCompilation = compilationName == "main" ||
                         compilationName.endsWith("main")
  ```

### 5. KMP Hierarchy Template
- ✅ Applied `applyDefaultHierarchyTemplate()` to all KMP samples
- ✅ Removed manual `dependsOn` calls
- ✅ No more hierarchy warnings
- ✅ Build succeeds without configuration errors

### 6. Compiler Plugin Structure
- ✅ Compiler JAR contains correct service files:
  ```
  META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
  → com.rsicarelli.fakt.compiler.FaktCompilerPluginRegistrar

  META-INF/services/org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
  → com.rsicarelli.fakt.compiler.FaktCommandLineProcessor
  ```
- ✅ Annotation detection code looks for correct FqName:
  ```kotlin
  ClassId.topLevel(FqName("com.rsicarelli.fakt.Fake"))
  ```
- ✅ Sample code uses matching import:
  ```kotlin
  import com.rsicarelli.fakt.Fake
  ```

---

## What Doesn't Work ❌

### Symptom
When samples use the published plugin:
```kotlin
plugins {
    kotlin("multiplatform") version "2.2.10"
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT")
            }
        }
    }
}
```

**Result:** No fakes are generated, tests fail with:
```
e: Unresolved reference 'fakeTestService'.
e: Unresolved reference 'fakeAnalyticsService'.
```

### Verified Facts

1. **Gradle Plugin Applies Correctly:**
   ```
   ✅ KtFakes: Applied Gradle plugin to project single-module
   ✅ KtFakes: Checking compilation 'main' - applicable: true
   ✅ KtFakes: Applying compiler plugin to compilation main
   ✅ KtFakes: Configured compiler plugin with 3 options
   ```

2. **No Generated Files:**
   ```bash
   $ find samples/single-module/build/generated -type f
   # No output - directory doesn't exist
   ```

3. **Test Compilation Fails:**
   ```bash
   $ ./gradlew :samples:single-module:compileTestKotlinJvm
   # Fails: Unresolved reference 'fakeTestService'
   ```

4. **@Fake Interfaces Exist:**
   ```
   samples/single-module/src/commonMain/kotlin/TestService.kt
   ├── @Fake interface TestService { ... }
   ├── @Fake interface AnalyticsService { ... }
   └── @Fake interface AuthenticationService { ... }
   ```

5. **Compiler Plugin JAR Is Valid:**
   ```bash
   $ jar tf ~/.m2/repository/com/rsicarelli/fakt/compiler/1.0.0-SNAPSHOT/compiler-1.0.0-SNAPSHOT.jar
   # Contains all expected classes and service files
   ```

---

## Debugging Trail

### Investigation Steps Completed

#### 1. Verified Plugin Application
```bash
$ ./gradlew :samples:single-module:compileKotlinJvm -i 2>&1 | grep "KtFakes"
✅ KtFakes: Applied Gradle plugin to project single-module
✅ KtFakes: Checking compilation 'main' - applicable: true
✅ KtFakes: Applying compiler plugin to compilation main
```
**Conclusion:** Gradle plugin is working correctly.

#### 2. Checked isApplicable Logic
**Before Fix:**
```kotlin
val isTestCompilation = compilationName.contains("test")
return isTestCompilation  // ❌ Wrong - @Fake is in main sources
```

**After Fix:**
```kotlin
val isMainCompilation = compilationName == "main" ||
                       compilationName.endsWith("main")
return isMainCompilation  // ✅ Correct for JVM and KMP
```

#### 3. Verified Annotation Detection
```kotlin
// Compiler looks for:
ClassId.topLevel(FqName("com.rsicarelli.fakt.Fake"))

// Sample uses:
import com.rsicarelli.fakt.Fake
@Fake interface TestService { ... }
```
**Conclusion:** Annotation matching is correct.

#### 4. Checked Compiler JAR Contents
```bash
$ unzip -p compiler-1.0.0-SNAPSHOT.jar META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
com.rsicarelli.fakt.compiler.FaktCompilerPluginRegistrar ✅

$ jar tf compiler-1.0.0-SNAPSHOT.jar | grep "FaktCompilerPluginRegistrar"
com/rsicarelli/fakt/compiler/FaktCompilerPluginRegistrar.class ✅
```
**Conclusion:** Service loader configuration is correct.

#### 5. Checked Runtime Dependencies
```bash
$ ./gradlew :samples:single-module:dependencies --configuration jvmCompileClasspath
# Shows: com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT ✅
```
**Conclusion:** Runtime dependency resolves correctly.

#### 6. Looked for Compiler Plugin Logs
```bash
$ ./gradlew :samples:single-module:compileKotlinJvm 2>&1 | grep -E "Fakt|Generated"
# No output from compiler plugin itself ❌
```
**Conclusion:** Compiler plugin might not be executing.

---

## Hypotheses for Root Cause

### Hypothesis 1: Compiler Plugin Not on Classpath
**Theory:** The Gradle plugin's `getPluginArtifact()` returns the correct artifact coordinates, but Kotlin compiler doesn't actually load it.

**Evidence:**
- Gradle plugin logs show "Applying compiler plugin"
- But no logs from compiler plugin itself
- No generated files created

**Next Steps:**
- Add debug logging to verify compiler classpath
- Check if `-Xplugin=` argument is actually passed to kotlinc
- Verify `SubpluginArtifact` resolution for published artifacts

### Hypothesis 2: Compiler Plugin Runs But Finds Nothing
**Theory:** The compiler plugin executes but doesn't find `@Fake` annotations during FIR phase.

**Evidence:**
- Service files are correct
- Annotation ClassId matches
- But no generation occurs

**Next Steps:**
- Add debug logging to `FakeAnnotationDetector`
- Verify FIR session has correct module descriptors
- Check if FIR checkers are actually registered

### Hypothesis 3: Output Directory Not Created/Accessible
**Theory:** The compiler plugin generates code but writes to wrong location or fails silently.

**Evidence:**
- `getGeneratedSourcesDirectory()` returns path
- But directory never gets created
- No error messages

**Next Steps:**
- Add debug logging to IR generation phase
- Verify file write permissions
- Check if output directory is on test compilation classpath

### Hypothesis 4: ClassLoader Issues
**Theory:** Published plugin uses different classloader than project dependencies, causing service loader or reflection issues.

**Evidence:**
- Works with project dependencies (`project(":compiler")`)
- Fails with published artifacts (`com.rsicarelli.fakt:compiler:1.0.0-SNAPSHOT`)

**Next Steps:**
- Compare classloader hierarchy
- Check for META-INF conflicts
- Verify Kotlin version compatibility

---

## Comparison: Project Dependencies vs Published Plugin

### Working: Project Dependencies
```kotlin
// In kmp-comprehensive-test (still using project deps)
tasks.withType<AbstractKotlinCompile<*>> {
    if (!name.contains("Test")) {
        val compilerJar = project.rootProject
            .project(":compiler")
            .tasks.named("shadowJar").get()
            .outputs.files.singleFile

        // Manually configure compiler plugin
    }
}
```
**Result:** ✅ Generates fakes successfully

### Not Working: Published Plugin
```kotlin
plugins {
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
}

dependencies {
    implementation("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT")
}
```
**Result:** ❌ No fakes generated

### Key Difference
- **Project dependencies:** Direct file path to shadow JAR
- **Published plugin:** Resolves via Gradle dependency mechanism through `SubpluginArtifact`

---

## Technical Deep Dive

### Gradle Plugin Flow

1. **Plugin Application** (`FaktGradleSubplugin.apply()`)
   ```kotlin
   override fun apply(target: Project) {
       target.extensions.create("ktfake", FaktPluginExtension::class.java)
       addRuntimeDependencies(target)      // ✅ Works
       configureSourceSets(target)         // ✅ Works
   }
   ```

2. **Compilation Check** (`isApplicable()`)
   ```kotlin
   override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
       val compilationName = kotlinCompilation.name.lowercase()
       return compilationName == "main" || compilationName.endsWith("main")
       // ✅ Returns true for "main", "jvmMain", "commonMain", etc.
   }
   ```

3. **Artifact Resolution** (`getPluginArtifact()`)
   ```kotlin
   override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
       groupId = "com.rsicarelli.fakt",      // ✅ Correct
       artifactId = "compiler",               // ✅ Correct (was "ktfake-compiler")
       version = "1.0.0-SNAPSHOT"            // ✅ Correct
   )
   ```
   **Question:** Does this actually add the JAR to kotlinc classpath?

4. **Compiler Options** (`applyToCompilation()`)
   ```kotlin
   override fun applyToCompilation(
       kotlinCompilation: KotlinCompilation<*>
   ): Provider<List<SubpluginOption>> {
       return project.provider {
           buildList {
               add(SubpluginOption(key = "enabled", value = "true"))
               add(SubpluginOption(key = "debug", value = "false"))
               add(SubpluginOption(key = "outputDir", value = "build/generated/..."))
           }
       }
   }
   ```
   **Question:** Are these options actually passed to the compiler plugin?

### Compiler Plugin Flow

1. **Service Loader Registration**
   ```
   META-INF/services/org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
   → com.rsicarelli.fakt.compiler.FaktCompilerPluginRegistrar
   ```
   ✅ File exists in JAR

2. **Plugin Registration** (`FaktCompilerPluginRegistrar.supportsK2 / registerExtensions`)
   ```kotlin
   override val supportsK2: Boolean = true

   override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
       if (!configuration.get(ENABLED_KEY, false)) return

       FirExtensionRegistrarAdapter.registerExtension(
           KtFakesFirExtensionRegistrar()
       )
       IrGenerationExtension.registerExtension(
           UnifiedKtFakesIrGenerationExtension(configuration)
       )
   }
   ```
   **Question:** Is this code ever executed?

3. **FIR Phase** (`FakeAnnotationDetector`)
   ```kotlin
   override fun checkClassifiersWithAnnotations(
       declaration: FirClassLikeDeclaration,
       context: CheckerContext,
       reporter: DiagnosticReporter
   ) {
       if (declaration !is FirRegularClass) return
       if (!declaration.isInterface()) return

       val hasFakeAnnotation = declaration.annotations.any {
           it.toAnnotationClassId(session) == FAKE_ANNOTATION_CLASS_ID
       }
       // ... register for IR generation
   }
   ```
   **Question:** Is this checker being called? Are annotations found?

4. **IR Phase** (`UnifiedKtFakesIrGenerationExtension`)
   ```kotlin
   override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
       // Analyze interfaces
       // Generate fakes
       // Write to output directory
   }
   ```
   **Question:** Is this phase reached? Are files written?

---

## Files to Investigate

### Gradle Plugin
```
gradle-plugin/src/main/kotlin/com/rsicarelli/fakt/gradle/
├── KtFakeGradleSubplugin.kt          # Main entry point
├── FaktPluginExtension.kt            # Configuration DSL
└── [Add debug logging here]
```

**Key Methods:**
- `isApplicable()` ✅ Fixed
- `getPluginArtifact()` ❓ Needs verification
- `applyToCompilation()` ❓ Needs verification

### Compiler Plugin
```
compiler/src/main/kotlin/com/rsicarelli/fakt/compiler/
├── FaktCompilerPluginRegistrar.kt    # Plugin registration
├── FaktCommandLineProcessor.kt       # Option parsing
├── fir/
│   ├── KtFakesFirExtensionRegistrar.kt
│   └── FakeAnnotationDetector.kt     # Finds @Fake annotations
└── UnifiedKtFakesIrGenerationExtension.kt  # Generates code
```

**Key Questions:**
- Is `FaktCompilerPluginRegistrar.supportsK2` being called?
- Is `FakeAnnotationDetector.checkClassifiersWithAnnotations` being called?
- Are any files being written?

---

## Debugging Action Items

### Priority 1: Verify Compiler Plugin Loads

**Add Logging to `FaktCompilerPluginRegistrar`:**
```kotlin
override val supportsK2: Boolean get() {
    println("🔍 FaktCompilerPluginRegistrar.supportsK2 called")
    return true
}

override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    println("🔍 FaktCompilerPluginRegistrar.registerExtensions called")
    val enabled = configuration.get(ENABLED_KEY, false)
    println("🔍 Plugin enabled: $enabled")

    if (!enabled) {
        println("❌ Plugin disabled, skipping registration")
        return
    }

    println("✅ Registering FIR extension")
    // ... rest of code
}
```

**Republish and test:**
```bash
./gradlew :compiler:publishToMavenLocal --no-build-cache
./gradlew :gradle-plugin:publishToMavenLocal --no-build-cache
./gradlew :samples:single-module:clean :samples:single-module:compileKotlinJvm
```

**Expected Output:**
- If plugin loads: See "🔍 FaktCompilerPluginRegistrar..." messages
- If plugin doesn't load: No output → classpath issue

### Priority 2: Verify SubpluginArtifact Resolution

**Add Logging to `KtFakeGradleSubplugin`:**
```kotlin
override fun getPluginArtifact(): SubpluginArtifact {
    val artifact = SubpluginArtifact(
        groupId = PLUGIN_GROUP_ID,
        artifactId = PLUGIN_ARTIFACT_NAME,
        version = PLUGIN_VERSION
    )
    project.logger.lifecycle("🔍 KtFakes: Requesting compiler plugin artifact: $artifact")
    return artifact
}

override fun applyToCompilation(
    kotlinCompilation: KotlinCompilation<*>
): Provider<List<SubpluginOption>> {
    project.logger.lifecycle("🔍 KtFakes: Applying to compilation: ${kotlinCompilation.name}")

    return project.provider {
        buildList {
            add(SubpluginOption(key = "enabled", value = "true"))
            add(SubpluginOption(key = "debug", value = "true"))  // Enable debug!

            val outputDir = getGeneratedSourcesDirectory(project, kotlinCompilation)
            add(SubpluginOption(key = "outputDir", value = outputDir))

            project.logger.lifecycle("🔍 KtFakes: Plugin options: enabled=true, debug=true, outputDir=$outputDir")
        }
    }
}
```

### Priority 3: Verify FIR Phase Execution

**Add Logging to `FakeAnnotationDetector`:**
```kotlin
override fun checkClassifiersWithAnnotations(
    declaration: FirClassLikeDeclaration,
    context: CheckerContext,
    reporter: DiagnosticReporter
) {
    println("🔍 FakeAnnotationDetector: Checking ${declaration.symbol.classId}")

    if (declaration !is FirRegularClass) {
        println("  ↳ Not a regular class, skipping")
        return
    }

    if (!declaration.isInterface()) {
        println("  ↳ Not an interface, skipping")
        return
    }

    println("  ↳ Is interface! Checking for @Fake annotation")
    val hasFakeAnnotation = declaration.annotations.any { annotation ->
        val classId = annotation.toAnnotationClassId(context.session)
        println("    ↳ Found annotation: $classId")
        classId == FAKE_ANNOTATION_CLASS_ID
    }

    if (hasFakeAnnotation) {
        println("  ✅ Found @Fake annotation!")
        // ... rest of code
    } else {
        println("  ❌ No @Fake annotation found")
    }
}
```

### Priority 4: Check Classpath at Runtime

**Add task to dump compiler classpath:**
```kotlin
// In sample build.gradle.kts
tasks.register("dumpCompilerClasspath") {
    doLast {
        val compilation = kotlin.targets["jvm"].compilations["main"]
        println("Compiler classpath:")
        compilation.compileKotlinTaskProvider.get()
            .compilerOptions
            .freeCompilerArgs
            .get()
            .forEach { println("  $it") }
    }
}
```

```bash
./gradlew :samples:single-module:dumpCompilerClasspath
```

---

## Workaround: Continue Using Project Dependencies

Until the published plugin issue is resolved, samples can use project dependencies which work correctly:

```kotlin
// samples/single-module/build.gradle.kts
plugins {
    kotlin("multiplatform") version "2.2.10"
    // DON'T use: id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
}

repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":runtime"))  // Works!
            }
        }
    }
}

// Manual compiler plugin wiring (works):
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    if (!name.contains("Test")) {
        compilerOptions {
            val compilerJar = project.rootProject
                .project(":compiler")
                .tasks.named("shadowJar").get()
                .outputs.files.singleFile

            freeCompilerArgs.addAll(
                "-Xplugin=${compilerJar.absolutePath}",
                "-P", "plugin:com.rsicarelli.fakt:enabled=true",
                "-P", "plugin:com.rsicarelli.fakt:debug=true",
                "-P", "plugin:com.rsicarelli.fakt:outputDir=${project.buildDir}/generated/fakt/test"
            )
        }
    }
}
```

---

## Related Issues & References

### Similar Kotlin Compiler Plugin Issues
- KSP (Kotlin Symbol Processing) also uses `SubpluginArtifact`
- KAPT had similar classpath issues in early versions
- Kotlin serialization plugin has similar structure

### Gradle Plugin Development Resources
- [Kotlin Gradle Plugin API](https://github.com/JetBrains/kotlin/tree/master/libraries/tools/kotlin-gradle-plugin-api)
- [KotlinCompilerPluginSupportPlugin](https://github.com/JetBrains/kotlin/blob/master/libraries/tools/kotlin-gradle-plugin-api/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/KotlinCompilerPluginSupportPlugin.kt)
- [SubpluginArtifact](https://github.com/JetBrains/kotlin/blob/master/libraries/tools/kotlin-gradle-plugin-api/src/common/kotlin/org/jetbrains/kotlin/gradle/plugin/SubpluginArtifact.kt)

### Metro Plugin (Reference Implementation)
- Metro also publishes to Maven and uses `SubpluginArtifact`
- Check how Metro's published plugin resolves compiler artifact
- Location: `../../ktfakes-prototype/metro/gradle-plugin/`

---

## Success Criteria for Resolution

When the published plugin works correctly, we should see:

1. **Gradle Plugin Logs:**
   ```
   ✅ KtFakes: Applied Gradle plugin to project X
   ✅ KtFakes: Checking compilation 'main' - applicable: true
   ✅ KtFakes: Applying compiler plugin to compilation main
   ✅ KtFakes: Configured compiler plugin with 3 options
   ```

2. **Compiler Plugin Logs:**
   ```
   ✅ FaktCompilerPluginRegistrar.supportsK2 called
   ✅ FaktCompilerPluginRegistrar.registerExtensions called
   ✅ Plugin enabled: true
   ✅ Registering FIR extension
   ✅ FakeAnnotationDetector: Checking test.sample.TestService
   ✅ Found @Fake annotation!
   ```

3. **Generated Files:**
   ```bash
   $ find samples/single-module/build/generated -name "*.kt"
   samples/single-module/build/generated/fakt/jvmTest/kotlin/test/sample/TestServiceFakes.kt
   samples/single-module/build/generated/fakt/jvmTest/kotlin/test/sample/AnalyticsServiceFakes.kt
   ...
   ```

4. **Tests Pass:**
   ```bash
   $ ./gradlew :samples:single-module:jvmTest
   BUILD SUCCESSFUL
   ```

---

## Next Steps

1. **Add comprehensive debug logging** to compiler and gradle plugins
2. **Republish with logging** and capture full output
3. **Compare with Metro** published plugin implementation
4. **Test with minimal sample** (single interface, single target)
5. **Consider alternative approaches:**
   - Gradle task-based generation (pre-compilation)
   - Embedded plugin distribution (shadow everything)
   - Different SubpluginArtifact configuration

---

## Contact Points for Help

- **Kotlin Slack:** #compiler, #gradle-plugin
- **Stack Overflow:** kotlin-compiler-plugin tag
- **GitHub Issues:** JetBrains/kotlin (for Gradle plugin API questions)
- **Metro Project:** Reference for working published plugin

---

**Status:** Investigation paused. Build-logic and publishing infrastructure complete and working. Runtime integration needs focused debugging session with comprehensive logging.