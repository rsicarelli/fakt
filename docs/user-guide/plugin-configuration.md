# Plugin Configuration

Complete reference for configuring the Fakt Gradle plugin.

---

## Complete Configuration Reference

All available configuration options in your module's `build.gradle.kts`:

```kotlin
// build.gradle.kts
import com.rsicarelli.fakt.compiler.api.LogLevel

plugins {
    alias(libs.plugins.fakt)
}

fakt {
    // Enable or disable the plugin (default: true)
    enabled.set(true)

    // Control logging verbosity (default: INFO)
    logLevel.set(LogLevel.INFO)  // Options: QUIET, INFO, DEBUG

    // Control call history generation (default: true)
    enableCallHistory.set(true)  // Set to false for lightweight fakes

    // Control mutable fake generation (default: false)
    enableMutableFakes.set(false)  // Set to true for mutable fakes by default

    // Generate fakes to testFixtures source set (default: false)
    // JVM: requires `java-test-fixtures` plugin. Android: requires
    // `android { testFixtures { enable = true } }` (see the Test Fixtures guide).
    useGradleTestFixtures.set(false)

    // Multi-module: Collect fakes from another module (default: not set)
    @OptIn(com.rsicarelli.fakt.compiler.api.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

---

## Configuration Properties

<table>
<tr><th>Flag</th><th>Default</th><th>Example</th></tr>
<tr>
<td><strong>enabled</strong></td>
<td><code>true</code></td>
<td>

```kotlin
fakt {
    enabled.set(false)
}
```

</td>
</tr>
<tr>
<td><strong>logLevel</strong></td>
<td><code>INFO</code></td>
<td>

```kotlin
fakt {
    logLevel.set(LogLevel.DEBUG)
}
```

</td>
</tr>
<tr>
<td><strong>enableCallHistory</strong></td>
<td><code>true</code></td>
<td>

```kotlin
fakt {
    enableCallHistory.set(false)
}
```

</td>
</tr>
<tr>
<td><strong>enableMutableFakes</strong></td>
<td><code>false</code></td>
<td>

```kotlin
fakt {
    enableMutableFakes.set(true)
}
```

</td>
</tr>
<tr>
<td><strong>useGradleTestFixtures</strong></td>
<td><code>false</code></td>
<td>

```kotlin
fakt {
    useGradleTestFixtures.set(true)
}
```

</td>
</tr>
<tr>
<td><strong>collectFrom</strong></td>
<td>Not set</td>
<td>

```kotlin
fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

</td>
</tr>
</table>

---

## Log Level Details

<table>
<tr><th>Level</th><th>Description</th><th>Example</th></tr>
<tr>
<td><strong>INFO</strong><br>(default)</td>
<td>

Concise summary with key metrics. Use for local development and monitoring cache effectiveness.

</td>
<td>

```kotlin
fakt {
    logLevel.set(LogLevel.INFO)
}
```

<strong>Output:</strong>

```
Fakt: 101 fakes generated in 35ms (50 cached)
  Interfaces: 101 | Classes: 0
  FIR: 6ms | IR: 29ms
  Cache: 50/101 (49%)
```

</td>
</tr>
<tr>
<td><strong>DEBUG</strong></td>
<td>

Detailed FIR + IR phase timing. Use for troubleshooting, performance analysis, and bug reports.

</td>
<td>

```kotlin
fakt {
    logLevel.set(LogLevel.DEBUG)
}
```

<strong>Output:</strong>

```
Registering FIR extension
Registering IR extension with FIR metadata access
Built IR class map with 149 classes
FIR→IR Transformation (interfaces: 101/101, took 1ms)
FIR + IR trace
├─ Total FIR time: 6ms
├─ Total IR time: 58ms
│  ├─ FIR analysis: 1 type parameters, 6 members (55µs)
│  └─ IR generation: FakeDataCacheImpl 83 LOC (766µs)
│  ├─ FIR analysis: 2 type parameters, 1 members (23µs)
│  └─ IR generation: FakeMapTransformerImpl 23 LOC (335µs)
```

</td>
</tr>
<tr>
<td><strong>QUIET</strong></td>
<td>

No output except errors. Use for CI/CD pipelines and production builds.

</td>
<td>

```kotlin
fakt {
    logLevel.set(LogLevel.QUIET)
}
```

<strong>Output:</strong> None (silent)

</td>
</tr>
</table>

---

## Call History Configuration

Control whether generated fakes include call tracking and verification capabilities.

<table>
<tr><th>Setting</th><th>Description</th><th>Example</th></tr>
<tr>
<td><strong>true</strong><br>(default)</td>
<td>

Full call history with:

- `methodNameCalls` StateFlow properties
- `methodNameCallHistory` lists
- `verifyMethodName { }` DSL

</td>
<td>

```kotlin
fakt {
    enableCallHistory.set(true)
}
```

</td>
</tr>
<tr>
<td><strong>false</strong></td>
<td>

Lightweight fakes with only behavior configuration. No call history overhead.

</td>
<td>

```kotlin
fakt {
    enableCallHistory.set(false)
}
```

</td>
</tr>
</table>

### Per-Interface Override

Individual interfaces can override the project default:

```kotlin
import com.rsicarelli.fakt.CallHistoryMode

// Always generate call history (even if plugin default is false)
@Fake(callHistory = CallHistoryMode.ENABLED)
interface PaymentService { ... }

// Never generate call history (even if plugin default is true)
@Fake(callHistory = CallHistoryMode.DISABLED)
interface Logger { ... }

// Follow plugin default
@Fake  // or @Fake(callHistory = CallHistoryMode.DEFAULT)
interface UserService { ... }
```

**Resolution order:** Annotation setting takes precedence over plugin default.

---

## Mutable Fakes Configuration

Control whether generated fakes are mutable (reconfigurable mid-test) or immutable (fixed at construction). For an in-depth exploration of when to use each mode, see **[Immutable vs Mutable](immutable-vs-mutable.md)**.

<table>
<tr><th>Setting</th><th>Description</th><th>Example</th></tr>
<tr>
<td><strong>false</strong><br>(default)</td>
<td>

Immutable fakes with:

- `private val` behavior properties
- Behavior fixed at construction time
- No `modify {}` method

</td>
<td>

```kotlin
fakt {
    enableMutableFakes.set(false)
}
```

</td>
</tr>
<tr>
<td><strong>true</strong></td>
<td>

Mutable fakes with:

- `@Volatile private var` behavior properties
- `modify {}` method for selective reconfiguration
- Mid-test behavior changes

</td>
<td>

```kotlin
fakt {
    enableMutableFakes.set(true)
}
```

</td>
</tr>
</table>

### Per-Interface Override

Individual interfaces can override the project default:

```kotlin
import com.rsicarelli.fakt.MutabilityMode

// Always generate mutable fake (even if plugin default is false)
@Fake(mutability = MutabilityMode.MUTABLE)
interface UserRepository { ... }

// Always generate immutable fake (even if plugin default is true)
@Fake(mutability = MutabilityMode.IMMUTABLE)
interface Logger { ... }

// Follow plugin default
@Fake  // or @Fake(mutability = MutabilityMode.DEFAULT)
interface UserService { ... }
```

**Resolution order:** Annotation setting takes precedence over plugin default.

---

## Multi-Module Configuration

<table>
<tr><th>Mode</th><th>Example</th></tr>
<tr>
<td><strong>Type-safe accessor</strong></td>
<td>

```kotlin
fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

</td>
</tr>
<tr>
<td><strong>String-based path</strong></td>
<td>

```kotlin
fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(project(":core:analytics"))
}
```

</td>
</tr>
</table>

For complete multi-module documentation, see **[Multi-Module Guide](multi-module.md)**.

---

## Cache-Correct Generation

Fakt generates fakes in dedicated, cacheable `faktGenerate*` Gradle tasks. The generated `.kt` files
are declared task outputs, so when Gradle's build cache restores a compilation the fakes come back
with it — no empty or missing fakes on a cache hit.

**Support matrix:**

| `@Fake` declared in        | Generated | Cache-correct |
|----------------------------|-----------|---------------|
| `commonMain`               | ✅        | ✅            |
| JVM / Android platform main| ✅        | ✅            |
| JS / Wasm platform main    | ✅        | Not yet       |
| Native platform main       | ✅        | No (permanent)|

Every `@Fake` is always generated — none are dropped. JS/Wasm are not cache-correct yet, and Native
cannot be (its compiler is not embeddable, so it can't run in a Gradle task); those platform fakes
are produced by the in-process plugin instead.

!!! note "Single-target multiplatform projects"
    A multiplatform project that declares exactly one target (`kotlin { jvm() }` and nothing else)
    keeps the in-process path. Kotlin does not give such a project a `commonMain` compilation to
    generate from, so there is nothing to make cache-correct. Fakes are still generated for every
    `@Fake`; they just aren't declared task outputs. Adding a second target moves the project onto
    the cache-correct path automatically.

**Default:** `true`.

### Opting out

The previous behaviour — generation running inside `compileKotlin*` as a side effect — is still
available. Turn it off via the extension or a Gradle property (the property wins over the extension,
so a command-line opt-out always applies):

```kotlin
fakt {
    useExperimentalGenerateTask.set(false)
}
```

```bash
gradle build -Pfakt.useExperimentalGenerateTask=false
```

!!! warning "Temporary escape hatch"
    On the in-process path the generated fakes are not declared task outputs, so a warm build cache
    can restore a compilation without them. The path is kept only as an escape hatch and will be
    removed in a future release — please [open an issue](https://github.com/rsicarelli/fakt/issues)
    if you need it.

---

## IDE Integration

### IntelliJ IDEA / Android Studio

Generated fakes appear in `build/generated/fakt/` and are automatically indexed.

**Enable K2 Mode for better autocomplete:**

1. **Settings** → **Languages & Frameworks** → **Kotlin**
2. Enable **K2 mode**
3. Restart IDE

K2 mode improves factory function autocomplete and type inference.

### Generated Sources Location

| Source Set | Generated Output |
|-----------|------------------|
| `commonTest/` | `build/generated/fakt/commonTest/kotlin/` |
| `jvmTest/` | `build/generated/fakt/jvmTest/kotlin/` |
| `iosTest/` | `build/generated/fakt/iosTest/kotlin/` |
| `androidUnitTest/` | `build/generated/fakt/androidUnitTest/kotlin/` |
| `testFixtures/` | `build/generated/fakt/testFixtures/kotlin/` (requires `useGradleTestFixtures`) |

---

## Next Steps

- **[Test Fixtures (JVM)](test-fixtures.md)** - Cross-module fakes for JVM projects
- **[Multi-Module (KMP)](multi-module.md)** - Cross-module fakes with collector modules
- **[Usage Guide](usage.md)** - Comprehensive usage patterns and examples
- **[Troubleshooting](../help/troubleshooting.md)** - Common configuration issues
