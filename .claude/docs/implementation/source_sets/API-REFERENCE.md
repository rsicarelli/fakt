# API Reference: Modern Source Set Mapping

**Date**: 2025-01-05
**Purpose**: Quick reference for Kotlin Gradle Plugin and Compiler APIs

---

## 🎯 Quick Links

- [Kotlin Gradle Plugin APIs](#kotlin-gradle-plugin-apis)
- [Source Set APIs](#source-set-apis)
- [Compilation APIs](#compilation-apis)
- [Serialization APIs](#serialization-apis)
- [Compiler Plugin APIs](#compiler-plugin-apis)
- [Gradle Configuration APIs](#gradle-configuration-apis)

---

## Kotlin Gradle Plugin APIs

### KotlinMultiplatformExtension

**Package**: `org.jetbrains.kotlin.gradle.dsl`

```kotlin
interface KotlinMultiplatformExtension {
    val targets: NamedDomainObjectContainer<KotlinTarget>
    val sourceSets: NamedDomainObjectContainer<KotlinSourceSet>

    fun jvm(): KotlinJvmTarget
    fun js(): KotlinJsTarget
    fun iosX64(): KotlinNativeTarget
    // ... other target creation methods

    fun applyDefaultHierarchyTemplate()
    fun applyHierarchyTemplate(template: KotlinHierarchyTemplate.() -> Unit)
}
```

**Usage**:
```kotlin
val kotlin = project.extensions.getByName("kotlin") as KotlinMultiplatformExtension

// Lazy access
kotlin.targets.configureEach { target ->
    // ...
}

kotlin.sourceSets.named("commonMain") {
    // ...
}
```

**Documentation**: [kotlinlang.org/api/kotlin-gradle-plugin](https://kotlinlang.org/api/kotlin-gradle-plugin/kotlin-gradle-plugin-api/org.jetbrains.kotlin.gradle.dsl/-kotlin-multiplatform-extension/)

---

### KotlinTarget

**Package**: `org.jetbrains.kotlin.gradle.plugin`

```kotlin
interface KotlinTarget {
    val name: String
    val platformType: KotlinPlatformType
    val compilations: NamedDomainObjectContainer<KotlinCompilation<*>>

    // Configuration names for variant publishing
    val apiElementsConfigurationName: String
    val runtimeElementsConfigurationName: String
}
```

**Properties**:
- `name`: Target name (e.g., `"jvm"`, `"iosX64"`, `"metadata"`)
- `platformType`: Enum value (`jvm`, `native`, `js`, `common`)
- `compilations`: All compilations for this target (usually `main` and `test`)

**Example**:
```kotlin
kotlin.targets.configureEach { target ->
    println("Target: ${target.name}, Platform: ${target.platformType}")

    target.compilations.configureEach { compilation ->
        println("  Compilation: ${compilation.name}")
    }
}
```

---

## Source Set APIs

### KotlinSourceSet

**Package**: `org.jetbrains.kotlin.gradle.plugin`

```kotlin
interface KotlinSourceSet {
    val name: String
    val kotlin: SourceDirectorySet
    val resources: SourceDirectorySet
    val languageSettings: LanguageSettingsBuilder
    val dependsOn: Set<KotlinSourceSet>  // ← KEY for hierarchy!

    fun dependsOn(other: KotlinSourceSet)
    fun dependencies(configure: KotlinDependencyHandler.() -> Unit)
}
```

**Key Properties**:

| Property | Type | Description |
|----------|------|-------------|
| `name` | `String` | Source set name (e.g., `"commonMain"`) |
| `kotlin` | `SourceDirectorySet` | Kotlin source directories |
| `dependsOn` | `Set<KotlinSourceSet>` | Direct parent source sets |
| `languageSettings` | `LanguageSettingsBuilder` | Compiler flags |

**Example**:
```kotlin
val commonMain = kotlin.sourceSets.getByName("commonMain")
val jvmMain = kotlin.sourceSets.getByName("jvmMain")

jvmMain.dependsOn(commonMain)  // jvmMain sees commonMain code

// Traverse hierarchy
fun getAllParents(sourceSet: KotlinSourceSet): Set<KotlinSourceSet> {
    val result = mutableSetOf(sourceSet)
    sourceSet.dependsOn.forEach { parent ->
        result.addAll(getAllParents(parent))
    }
    return result
}
```

**Documentation**: [kotlinlang.org/api/kotlin-gradle-plugin](https://kotlinlang.org/api/kotlin-gradle-plugin/kotlin-gradle-plugin-api/org.jetbrains.kotlin.gradle.plugin/-kotlin-source-set/)

---

### SourceDirectorySet

**Package**: `org.gradle.api.file`

```kotlin
interface SourceDirectorySet : FileTree {
    fun srcDir(srcPath: Any): SourceDirectorySet
    fun setSrcDirs(srcPaths: Iterable<*>): SourceDirectorySet
    val srcDirs: Set<File>
}
```

**Usage**:
```kotlin
// Add generated code directory
kotlin.sourceSets.named("jvmMain") {
    kotlin.srcDir("build/generated/fakt/main/jvm/kotlin")
}

// Use lazy Provider
kotlin.srcDir(project.layout.buildDirectory.dir("generated/fakt/main/jvm/kotlin"))
```

---

## Compilation APIs

### KotlinCompilation

**Package**: `org.jetbrains.kotlin.gradle.plugin`

```kotlin
interface KotlinCompilation<T : KotlinCommonOptions> {
    val name: String
    val target: KotlinTarget
    val platformType: KotlinPlatformType

    val defaultSourceSet: KotlinSourceSet  // ← Primary source set
    val allKotlinSourceSets: Set<KotlinSourceSet>  // ← Full hierarchy!

    val compileDependencyConfigurationName: String
    val runtimeDependencyConfigurationName: String

    val allAssociatedCompilations: Set<KotlinCompilation<*>>

    fun associateWith(other: KotlinCompilation<*>)
}
```

**Key Properties**:

| Property | Type | Description |
|----------|------|-------------|
| `name` | `String` | Compilation name (usually `"main"` or `"test"`) |
| `defaultSourceSet` | `KotlinSourceSet` | Primary source set (e.g., `jvmMain`) |
| `allKotlinSourceSets` | `Set<KotlinSourceSet>` | ALL source sets in hierarchy |
| `platformType` | `KotlinPlatformType` | Platform type enum |

**Constants**:
```kotlin
KotlinCompilation.MAIN_COMPILATION_NAME = "main"
KotlinCompilation.TEST_COMPILATION_NAME = "test"
```

**Example**:
```kotlin
target.compilations.configureEach { compilation ->
    println("Compilation: ${compilation.name}")
    println("Default source set: ${compilation.defaultSourceSet.name}")
    println("All source sets:")
    compilation.allKotlinSourceSets.forEach { sourceSet ->
        println("  - ${sourceSet.name}")
    }
}

// For jvmMain compilation, output might be:
// Compilation: main
// Default source set: jvmMain
// All source sets:
//   - jvmMain
//   - commonMain
```

**Documentation**: [kotlinlang.org/api/kotlin-gradle-plugin](https://kotlinlang.org/api/kotlin-gradle-plugin/kotlin-gradle-plugin-api/org.jetbrains.kotlin.gradle.plugin/-kotlin-compilation/)

---

### KotlinPlatformType

**Package**: `org.jetbrains.kotlin.gradle.plugin`

```kotlin
enum class KotlinPlatformType {
    jvm,
    androidJvm,
    js,
    wasm,
    native,
    common
}
```

**Usage**:
```kotlin
when (compilation.platformType) {
    KotlinPlatformType.jvm -> // JVM-specific logic
    KotlinPlatformType.native -> // Native-specific logic
    KotlinPlatformType.common -> // Common (metadata) logic
    else -> // Other platforms
}
```

---

## Serialization APIs

### kotlinx.serialization

**Package**: `kotlinx.serialization.json`

```kotlin
@Serializable
data class MyData(val name: String, val value: Int)

val json = Json {
    prettyPrint = false
    encodeDefaults = true
    ignoreUnknownKeys = true
}

// Serialize
val jsonString = json.encodeToString(myData)

// Deserialize
val decoded = json.decodeFromString<MyData>(jsonString)
```

**Key Configuration**:

| Option | Default | Recommended | Purpose |
|--------|---------|-------------|---------|
| `prettyPrint` | `true` | `false` | Compact output for command line |
| `encodeDefaults` | `true` | `true` | Include default values |
| `ignoreUnknownKeys` | `false` | `true` | Forward compatibility |

**Documentation**: [github.com/Kotlin/kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)

---

### Base64 Encoding

**Package**: `java.util.Base64`

```kotlin
import java.util.Base64

// Encode
val encoded = Base64.getEncoder().encodeToString(byteArray)

// Decode
val decoded = Base64.getDecoder().decode(encoded)
val string = String(decoded)
```

**Why Base64?**
- Command-line safe (no special characters)
- No escaping needed for shell arguments
- Standard library (no extra dependencies)

---

## Compiler Plugin APIs

### KotlinCompilerPluginSupportPlugin

**Package**: `org.jetbrains.kotlin.gradle.plugin`

```kotlin
interface KotlinCompilerPluginSupportPlugin : Plugin<Project> {
    fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean
    fun getCompilerPluginId(): String
    fun getPluginArtifact(): SubpluginArtifact
    fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>>
}
```

**Example Implementation**:
```kotlin
class MyGradleSubplugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        // Register extension, configure tasks, etc.
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        // Return true if plugin should be applied to this compilation
        return kotlinCompilation.name == "main"
    }

    override fun getCompilerPluginId() = "com.example.myplugin"

    override fun getPluginArtifact() = SubpluginArtifact(
        groupId = "com.example",
        artifactId = "compiler-plugin",
        version = "1.0.0"
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        return kotlinCompilation.project.provider {
            listOf(
                SubpluginOption(key = "enabled", value = "true")
            )
        }
    }
}
```

---

### SubpluginOption

**Package**: `org.jetbrains.kotlin.gradle.plugin`

```kotlin
class SubpluginOption(
    val key: String,
    val value: String
)

// Special case for file lists
class FilesSubpluginOption(
    key: String,
    files: Collection<File>
)
```

**Command-Line Format**:
```
-P plugin:<pluginId>:<key>=<value>
```

**Example**:
```kotlin
// In Gradle plugin
SubpluginOption(key = "outputDir", value = "/path/to/output")

// Becomes command-line argument
-P plugin:com.example.myplugin:outputDir=/path/to/output
```

---

### CommandLineProcessor

**Package**: `org.jetbrains.kotlin.compiler.plugin`

```kotlin
interface CommandLineProcessor {
    val pluginId: String
    val pluginOptions: Collection<CliOption>

    fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    )
}
```

**Example**:
```kotlin
@AutoService(CommandLineProcessor::class)
class MyCommandLineProcessor : CommandLineProcessor {

    override val pluginId = "com.example.myplugin"

    override val pluginOptions = listOf(
        CliOption(
            optionName = "outputDir",
            valueDescription = "<path>",
            description = "Output directory",
            required = false
        )
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration
    ) {
        when (option.optionName) {
            "outputDir" -> configuration.put(KEY_OUTPUT_DIR, value)
        }
    }
}
```

---

### CompilerConfiguration

**Package**: `org.jetbrains.kotlin.config`

```kotlin
// Define key
val KEY_MY_DATA = CompilerConfigurationKey<MyData>("my data")

// Store value (in CommandLineProcessor)
configuration.put(KEY_MY_DATA, myData)

// Retrieve value (in IrGenerationExtension)
val data = pluginContext.configuration.get(KEY_MY_DATA)
    ?: error("Data not found")
```

---

### IrGenerationExtension

**Package**: `org.jetbrains.kotlin.backend.common.extensions`

```kotlin
interface IrGenerationExtension {
    fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    )
}
```

**Example**:
```kotlin
class MyIrExtension(
    private val messageCollector: MessageCollector?
) : IrGenerationExtension {

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        // Access configuration
        val context = pluginContext.configuration.get(KEY_CONTEXT)
            ?: error("Context not provided")

        // Generate IR nodes...
    }
}
```

---

## Gradle Configuration APIs

### Provider API

**Package**: `org.gradle.api.provider`

```kotlin
interface Provider<T> {
    fun get(): T
    fun orNull(): T?
    fun <S> map(transformer: (T) -> S): Provider<S>
}
```

**Usage**:
```kotlin
// ✅ GOOD: Lazy Provider
val buildDir: Provider<Directory> = project.layout.buildDirectory
val outputDir = buildDir.map { it.dir("generated/fakt") }

// ❌ BAD: Eager get()
val buildDirFile = project.layout.buildDirectory.get().asFile
```

**Why Important?**
- Configuration cache compatibility
- Deferred evaluation
- Automatic dependency tracking

---

### Property API

**Package**: `org.gradle.api.provider`

```kotlin
interface Property<T> : Provider<T> {
    fun set(value: T)
    fun set(provider: Provider<T>)
    fun convention(value: T): Property<T>
}
```

**Usage**:
```kotlin
// In extension
abstract class MyExtension {
    abstract val enabled: Property<Boolean>
}

// In plugin
val extension = project.extensions.create("myPlugin", MyExtension::class.java)
extension.enabled.convention(true)

// In build script
myPlugin {
    enabled.set(false)
}
```

---

### NamedDomainObjectProvider

**Package**: `org.gradle.api.NamedDomainObjectProvider`

```kotlin
interface NamedDomainObjectProvider<T> : Provider<T> {
    fun configure(action: Action<in T>)
}
```

**Usage**:
```kotlin
// ✅ GOOD: Lazy named access
kotlin.sourceSets.named("commonMain") {
    dependencies {
        implementation("...")
    }
}

// ❌ BAD: Eager getByName
val commonMain = kotlin.sourceSets.getByName("commonMain")
commonMain.dependencies { ... }
```

---

### NamedDomainObjectContainer

**Package**: `org.gradle.api.NamedDomainObjectContainer`

```kotlin
interface NamedDomainObjectContainer<T> : NamedDomainObjectCollection<T> {
    fun create(name: String): T
    fun create(name: String, configureAction: Action<in T>): T
    fun named(name: String): NamedDomainObjectProvider<T>
    fun configureEach(action: Action<in T>)
}
```

**Usage**:
```kotlin
// ✅ GOOD: configureEach (lazy)
kotlin.sourceSets.configureEach { sourceSet ->
    // Applied to ALL source sets (existing and future)
}

// ❌ BAD: forEach (eager)
kotlin.sourceSets.forEach { sourceSet ->
    // Forces realization of ALL source sets immediately
}
```

---

## 📊 API Comparison Tables

### Source Set Access Patterns

| Pattern | Type | Lazy? | Configuration Cache Safe? | Use When |
|---------|------|-------|---------------------------|----------|
| `sourceSets.named("main")` | `NamedDomainObjectProvider<T>` | ✅ | ✅ | Accessing specific source set |
| `sourceSets.configureEach {}` | - | ✅ | ✅ | Configuring all source sets |
| `sourceSets.getByName("main")` | `T` | ❌ | ❌ | **NEVER - Deprecated** |
| `sourceSets.forEach {}` | - | ❌ | ❌ | **NEVER - Deprecated** |

---

### Compilation Detection Patterns

| Pattern | Reliability | Coverage | Performance |
|---------|-------------|----------|-------------|
| `compilation.name == "test"` | ✅ High | Standard test only | ⚡ Fast |
| `compilation.name.endsWith("Test")` | ✅ High | Custom test suites | ⚡ Fast |
| `compilation.allAssociatedCompilations.contains(main)` | ✅ High | Associated tests | ⚡ Fast |
| Pattern matching module name | ❌ Low | Brittle | ⚡ Fast |

---

## 🔧 Common Operations

### Get All Source Sets in Hierarchy

```kotlin
fun getAllSourceSets(compilation: KotlinCompilation<*>): Set<KotlinSourceSet> {
    // ✅ Use built-in API!
    return compilation.allKotlinSourceSets
}
```

---

### Map Source Set to Output Directory

```kotlin
fun getOutputDirectory(
    sourceSet: KotlinSourceSet,
    isTest: Boolean,
    project: Project
): File {
    val buildDir = project.layout.buildDirectory.get().asFile
    val type = if (isTest) "test" else "main"

    return buildDir.resolve("generated/myplugin/$type/${sourceSet.name}/kotlin")
}
```

---

### Check if Compilation is Test

```kotlin
fun isTestCompilation(compilation: KotlinCompilation<*>): Boolean {
    return compilation.name == KotlinCompilation.TEST_COMPILATION_NAME ||
           compilation.name.endsWith("Test", ignoreCase = true) ||
           compilation.allAssociatedCompilations.any {
               it.name == KotlinCompilation.MAIN_COMPILATION_NAME
           }
}
```

---

## 📚 External Documentation

### Official Kotlin Documentation

- **Multiplatform DSL Reference**: [kotlinlang.org/docs/multiplatform-dsl-reference](https://kotlinlang.org/docs/multiplatform-dsl-reference.html)
- **Compilation & Caches**: [kotlinlang.org/docs/gradle-compilation-and-caches](https://kotlinlang.org/docs/gradle-compilation-and-caches.html)
- **Hierarchical Structure**: [kotlinlang.org/docs/multiplatform-hierarchy](https://kotlinlang.org/docs/multiplatform-hierarchy.html)

### Gradle Documentation

- **Kotlin DSL Primer**: [docs.gradle.org/current/userguide/kotlin_dsl](https://docs.gradle.org/current/userguide/kotlin_dsl.html)
- **Lazy Configuration**: [docs.gradle.org/current/userguide/lazy_configuration](https://docs.gradle.org/current/userguide/lazy_configuration.html)
- **Configuration Cache**: [docs.gradle.org/current/userguide/configuration_cache](https://docs.gradle.org/current/userguide/configuration_cache.html)

### API Documentation

- **Kotlin Gradle Plugin API**: [kotlinlang.org/api/kotlin-gradle-plugin](https://kotlinlang.org/api/kotlin-gradle-plugin/kotlin-gradle-plugin-api/)
- **Gradle API**: [docs.gradle.org/current/javadoc](https://docs.gradle.org/current/javadoc/)

---

## 🎯 Quick Tips

### DO's ✅

- ✅ Use `named()` for specific source sets
- ✅ Use `configureEach {}` for all source sets
- ✅ Use `Provider<T>` for lazy evaluation
- ✅ Use `project.layout.buildDirectory` (not `buildDir`)
- ✅ Use `compilation.allKotlinSourceSets` for hierarchy
- ✅ Store data in `CompilerConfiguration` keys

### DON'Ts ❌

- ❌ Never use `getByName()` outside task execution
- ❌ Never use `forEach {}` on source sets/compilations
- ❌ Never use `project.buildDir` (deprecated)
- ❌ Never access `Provider.get()` during configuration
- ❌ Never hardcode source set names in patterns
- ❌ Never assume source set structure

---

**Last Updated**: 2025-01-05
**Kotlin Version**: 1.9.24+
**Gradle Version**: 8.5+
