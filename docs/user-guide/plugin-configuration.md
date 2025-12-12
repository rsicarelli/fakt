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

---

## Next Steps

- **[Multi-Module Setup](multi-module.md)** - Cross-module fakes architecture
- **[Usage Guide](usage.md)** - Comprehensive usage patterns and examples
- **[Troubleshooting](../help/troubleshooting.md)** - Common configuration issues
