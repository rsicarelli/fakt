# Testing Patterns

Best practices for using Fakt-generated fakes in your test suites.

---

## GIVEN-WHEN-THEN Pattern

Follow BDD-style test naming with Fakt fakes:

```kotlin
@Test
fun `GIVEN configured repository WHEN saving user THEN returns success`() = runTest {
    // GIVEN
    val fake = fakeUserRepository {
        saveUser { user -> Result.success(Unit) }
    }
    val service = UserService(fake)

    // WHEN
    val result = service.createUser("Alice")

    // THEN
    assertTrue(result.isSuccess)
    assertEquals(1, fake.saveUserCallCount.value)
}
```

---

## Isolated Fakes Per Test

Create fresh fakes for each test to avoid shared state:

```kotlin
class UserServiceTest {
    @Test
    fun `test case 1`() {
        val fake = fakeRepository { /* config */ }
        // Test with this fake
    }

    @Test
    fun `test case 2`() {
        val fake = fakeRepository { /* different config */ }
        // Fresh, independent fake
    }
}
```

---

## Verify Behavior, Not Implementation

Use call counters to verify interactions:

```kotlin
@Test
fun `GIVEN service WHEN processing user THEN calls repository once`() {
    val fakeRepo = fakeRepository()
    val service = UserService(fakeRepo)

    service.processUser("123")

    assertEquals(1, fakeRepo.getUserCallCount.value)
    assertEquals(1, fakeRepo.saveUserCallCount.value)
}
```

---

## Test Edge Cases

Configure fakes to test error handling:

```kotlin
@Test
fun `GIVEN repository failure WHEN saving user THEN handles error`() = runTest {
    val fake = fakeRepository {
        saveUser { user ->
            Result.failure(NetworkException())
        }
    }
    val service = UserService(fake)

    val result = service.createUser("Alice")

    assertTrue(result.isFailure)
}
```

---

## Use Turbine for Reactive Testing

Test StateFlow call counters reactively:

```kotlin
@Test
fun `GIVEN fake WHEN calling repeatedly THEN emits counts`() = runTest {
    val fake = fakeAnalytics()

    fake.trackCallCount.test {
        assertEquals(0, awaitItem())

        fake.track("event1")
        assertEquals(1, awaitItem())

        fake.track("event2")
        assertEquals(2, awaitItem())
    }
}
```

---

## Next Steps

- [Migration Guide](migration.md) - From MockK/Mockito
- [Performance](performance.md) - Build time impact
