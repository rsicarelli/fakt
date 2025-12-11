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

## Reporting Issues

Found a limitation not listed here? [Report it on GitHub](https://github.com/rsicarelli/fakt/issues).

---

## Next Steps

- [FAQ](../help/faq.md) - Common questions
- [Troubleshooting](../help/troubleshooting.md) - Common issues
