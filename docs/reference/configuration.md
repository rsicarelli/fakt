# Configuration

Gradle plugin configuration options.

---

## Log Level

Control compilation output verbosity:

```kotlin
import com.rsicarelli.fakt.compiler.api.LogLevel

fakt {
    logLevel.set(LogLevel.INFO)  // Default
}
```

Available levels:

- `LogLevel.QUIET` - Zero output (CI/CD)
- `LogLevel.INFO` - Concise summary (default)
- `LogLevel.DEBUG` - Detailed breakdown
- `LogLevel.TRACE` - Full IR details

---

## Multi-Module Support (Experimental)

Collect fakes from other modules:

```kotlin
fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

---

## Next Steps

- [Performance](../guides/performance.md) - Telemetry details
- [Multi-Module](../usage/multi-module.md) - Cross-module fakes
