# Properties

Fakt generates fakes for both read-only (`val`) and mutable (`var`) properties with automatic call tracking.

---

## Read-Only Properties (val)

```kotlin
@Fake
interface Config {
    val apiUrl: String
    val timeout: Int
}

val fake = fakeConfig {
    apiUrl { "https://api.example.com" }
    timeout { 30 }
}

assertEquals("https://api.example.com", fake.apiUrl)
assertEquals(1, fake.apiUrlCallCount.value)
```

---

## Mutable Properties (var)

```kotlin
@Fake
interface Settings {
    var theme: String
    var fontSize: Int
}

val fake = fakeSettings {
    theme { "dark" }
    fontSize { 14 }
}

// Getter tracking
assertEquals("dark", fake.theme)
assertEquals(1, fake.getThemeCallCount.value)

// Setter tracking
fake.theme = "light"
assertEquals(1, fake.setThemeCallCount.value)
```

---

## Next Steps

- [Call Tracking](call-tracking.md) - Advanced StateFlow patterns
- [Multi-Module](../multi-module/index.md) - Cross-module fakes
