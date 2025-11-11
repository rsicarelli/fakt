# Call Tracking

Every Fakt-generated fake includes automatic, thread-safe call tracking via Kotlin StateFlow.

---

## Basic Call Tracking

```kotlin
@Fake
interface Analytics {
    fun track(event: String)
}

val fake = fakeAnalytics {
    track { event -> println(event) }
}

fake.track("event1")
fake.track("event2")

assertEquals(2, fake.trackCallCount.value)
```

---

## StateFlow Integration

Call counters are `StateFlow<Int>`, enabling reactive testing:

```kotlin
import app.cash.turbine.test

@Test
fun `GIVEN fake WHEN calling methods THEN counter updates reactively`() = runTest {
    val fake = fakeRepository()

    fake.getUserCallCount.test {
        assertEquals(0, awaitItem())

        fake.getUser("123")
        assertEquals(1, awaitItem())

        fake.getUser("456")
        assertEquals(2, awaitItem())
    }
}
```

---

## Property Call Tracking

Properties track both getter and setter calls:

```kotlin
@Fake
interface Settings {
    var theme: String
}

val fake = fakeSettings {
    theme { "dark" }
}

val _ = fake.theme  // Getter
assertEquals(1, fake.getThemeCallCount.value)

fake.theme = "light"  // Setter
assertEquals(1, fake.setThemeCallCount.value)
```

---

## Thread Safety

All call counters are thread-safe via `MutableStateFlow.update`:

```kotlin
@Test
fun `GIVEN fake WHEN calling from multiple threads THEN counts correctly`() = runTest {
    val fake = fakeAnalytics()

    withContext(Dispatchers.Default) {
        repeat(1000) {
            launch {
                fake.track("event")
            }
        }
    }

    assertEquals(1000, fake.trackCallCount.value)
}
```

---

## Next Steps

- [Multi-Module](multi-module.md) - Cross-module fakes
- [Testing Patterns](../guides/testing-patterns.md) - Best practices
