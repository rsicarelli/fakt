# Performance

Fakt's compile-time impact and telemetry system.

---

## Build Time Impact

Fakt uses intelligent caching across KMP targets:

**First target compilation:**

```
DISCOVERY: 1ms (100 interfaces, 21 classes)
GENERATION: 39ms (121 new fakes, avg 333µs/fake)
TOTAL: 40ms
```

**Subsequent targets (cached):**

```
compileKotlinJvm:     1ms (121 from cache)
compileKotlinAndroid: 1ms (121 from cache)
compileKotlinIosX64:  1ms (121 from cache)
```

---

## Telemetry Configuration

Four log levels for debugging and performance analysis:

```kotlin
import com.rsicarelli.fakt.compiler.api.LogLevel

fakt {
    logLevel.set(LogLevel.INFO)   // Default: concise summary
    // LogLevel.QUIET  - Zero output (CI/CD)
    // LogLevel.DEBUG  - Detailed breakdown with FIR + IR details
}
```

### Log Level Outputs

**INFO (default):**

```
✅ 10 fakes generated in 1.2s (6 cached)
   Discovery: 120ms | Analysis: 340ms | Generation: 580ms
   Cache hit rate: 40% (6/15)
```

**DEBUG:**

```
[DISCOVERY] 120ms - 15 interfaces, 3 classes
[FILTERING] 85ms - Cache hits: 6/15 (40%)
[ANALYSIS] 340ms
  ├─ UserRepository (18ms)
  ├─ Analytics (42ms)
  ├─ FIR + IR node inspection, type resolution
```

Includes full FIR + IR details, type resolution, etc. (~5-10ms overhead)

---

## Cache Strategy

Fakt caches generated code across:

- KMP targets (jvm, ios, android, etc.)
- Incremental compilation runs
- Clean builds (invalidates cache)

---

## Best Practices

### ✅ Use QUIET in CI/CD

```kotlin
fakt {
    logLevel.set(LogLevel.QUIET)  // Zero overhead
}
```

### ✅ Use DEBUG for Troubleshooting

```kotlin
fakt {
    logLevel.set(LogLevel.DEBUG)  // ~5-10ms overhead
}
```

---

## Next Steps

- [Configuration](plugin-configuration.md) - Plugin options
- [Troubleshooting](../help/troubleshooting.md) - Common issues
