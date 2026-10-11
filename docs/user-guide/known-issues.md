# Limitations

Known limitations and workarounds.

---

## Current Limitations

### ❌ Data Classes as @Fake Targets

Data classes have compiler-generated implementations and can't be faked.

**Workaround**: Use builders or `copy()` for test data.

**Works as parameter/return types:**

```kotlin
data class User(val id: String, val name: String)

@Fake  // ✅ This works
interface UserRepository {
    fun getUser(id: String): User  // ✅ Data class as return type
}
```

---

### ❌ Sealed Classes as @Fake Targets

Sealed hierarchies can't be faked directly.

**Workaround**: Use exhaustive when-expressions or visitor patterns.

---

### ❌ Default Parameters in Interface Methods

Interfaces with default parameters are not yet supported.

**Workaround**: Use overloaded methods or remove defaults.

---

### Kotlin/Native

- Kotlin older than 2.4 cannot read the published annotations klibs, so Native projects need 2.4 or
  newer ([#171](https://github.com/rsicarelli/fakt/issues/171)).
- Windows hosts are not covered ([#167](https://github.com/rsicarelli/fakt/issues/167)).
- The fakes of a source set that only one target compiles (for example `mingwMain` with a single
  `mingwX64` target) are visible in that target's test compilation (`mingwX64Test`), not in the
  intermediate `mingwTest` above it, whose own generated directory stays empty.
- An `@OptIn` on the interface loses its import in the fake
  ([#172](https://github.com/rsicarelli/fakt/issues/172)). **Workaround**: opt in for the whole
  module, for example `optIn("kotlinx.cinterop.ExperimentalForeignApi")`.

See [Kotlin/Native](plugin-configuration.md#kotlinnative) for how the distribution is found.

---

## Reporting Issues

Found a limitation not listed here? [Report it on GitHub](https://github.com/rsicarelli/fakt/issues).

---

## Next Steps

- [FAQ](../help/faq.md) - Common questions
- [Troubleshooting](../help/troubleshooting.md) - Common issues
