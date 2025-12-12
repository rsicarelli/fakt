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
        val fake = fakeRepository {  // Fresh fake
            getUser { id -> User(id, "Alice") }
        }
        // Test with Alice
    }

    @Test
    fun `test case 2`() {
        val fake = fakeRepository {  // Fresh fake
            getUser { id -> User(id, "Bob") }
        }
        // Test with Bob
    }
}
```

**Why this matters:**
- Prevents test pollution (one test affecting another)
- Makes tests order-independent
- Easier to understand test setup

---

## Configure Only What You Need

Don't configure methods you don't use in the test:

```kotlin
@Test
fun `GIVEN repository WHEN getting user THEN returns user`() {
    val fake = fakeRepository {
        getUser { id -> User(id, "Alice") }
        // Don't configure saveUser, deleteUser, etc. if not used
    }

    val user = fake.getUser("123")
    assertEquals("Alice", user.name)
}
```

**Benefits:**
- Tests are easier to read (only relevant setup visible)
- Reduces noise in test code
- Smart defaults handle unconfigured methods

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

**State-based verification example:**

```kotlin
@Test
fun `GIVEN repository WHEN saving users THEN all users are saved`() {
    val savedUsers = mutableListOf<User>()
    val fake = fakeRepository {
        saveUser { user ->
            savedUsers.add(user)
            Result.success(Unit)
        }
    }

    val service = UserService(fake)
    service.batchSave(listOf(alice, bob))

    // Verify OUTCOME (state), not method calls
    assertEquals(2, savedUsers.size)
    assertTrue(savedUsers.contains(alice))
    assertTrue(savedUsers.contains(bob))
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

## Anti-Patterns to Avoid

### ❌ Shared Fakes Across Tests

```kotlin
// ❌ BAD: Shared fake
class UserServiceTest {
    private val sharedFake = fakeRepository()  // State leaks between tests

    @Test
    fun test1() { /* uses sharedFake */ }

    @Test
    fun test2() { /* uses same sharedFake - FLAKY! */ }
}
```

```kotlin
// ✅ GOOD: Fresh fake per test
class UserServiceTest {
    @Test
    fun test1() {
        val fake = fakeRepository()  // Isolated
    }

    @Test
    fun test2() {
        val fake = fakeRepository()  // Independent
    }
}
```

### ❌ Over-Configuring Fakes

```kotlin
// ❌ BAD: Configuring unused methods
@Test
fun `test getUser only`() {
    val fake = fakeRepository {
        getUser { id -> User(id, "Alice") }
        saveUser { /* not used in this test */ }
        deleteUser { /* not used in this test */ }
        listUsers { /* not used in this test */ }
    }

    val user = fake.getUser("123")  // Only this is tested
}
```

```kotlin
// ✅ GOOD: Configure only what's needed
@Test
fun `test getUser only`() {
    val fake = fakeRepository {
        getUser { id -> User(id, "Alice") }
        // Smart defaults handle the rest
    }

    val user = fake.getUser("123")
}
```

---

## Next Steps

- **[Migration Guide](migration-from-mocks.md)** - From MockK/Mockito to Fakt
- **[Performance](performance.md)** - Build time impact and optimization
- **[Usage Guide](usage.md)** - Core patterns and examples
