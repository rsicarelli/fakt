# Testing Patterns

Best practices for using Fakt-generated fakes in your test suites.

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

Use call history to verify interactions:

```kotlin
@Test
fun `GIVEN service WHEN processing user THEN calls repository once`() {
    val fakeRepo = fakeRepository()
    val service = UserService(fakeRepo)

    service.processUser("123")

    assertEquals(1, fakeRepo.getUserCalls.value.size)
    assertEquals(1, fakeRepo.saveUserCalls.value.size)
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

## Verification Patterns

Use the verification DSL to assert on call history:

### Basic Call History Verification

```kotlin
@Test
fun `GIVEN repository WHEN saving users THEN tracks calls`() {
    val fake = fakeRepository {
        saveUser { user -> Result.success(Unit) }
    }

    fake.saveUser(alice)
    fake.saveUser(bob)

    assertEquals(2, fake.saveUserCalls.value.size)
}
```

### Scoped Verification DSL

```kotlin
@Test
fun `GIVEN repository WHEN saving users THEN verifies arguments`() {
    val fake = fakeRepository {
        saveUser { user -> Result.success(Unit) }
    }

    fake.saveUser(alice)
    fake.saveUser(bob)

    fake.verifySaveUser {
        assertTrue(wasCalledTimes(2))
        assertTrue(wasCalledWith(alice))
        assertTrue(wasCalledWith(bob))
        assertEquals("Alice", first.user.name)
    }
}
```

### Call Order Verification

For single-parameter methods, verify call order:

```kotlin
@Test
fun `GIVEN analytics WHEN tracking events THEN verifies order`() {
    val fake = fakeAnalytics()

    fake.track("page_view")
    fake.track("button_click")
    fake.track("purchase")

    fake.verifyTrack {
        assertTrue(wasCalledInOrder("page_view", "button_click", "purchase"))
        assertTrue(neverCalledWith("error"))
    }
}
```

### Verifying No Calls

```kotlin
@Test
fun `GIVEN cached data WHEN loading THEN does not call API`() {
    val fake = fakeApiClient()
    val service = CachedService(fake, preloadedCache)

    service.getData("key")

    fake.verifyFetchData {
        assertTrue(wasNeverCalled())
    }
    // Or simply:
    assertEquals(0, fake.fetchDataCalls.value.size)
}
```

---

## Mid-Test Reconfiguration (Mutable Fakes)

For integration tests where a fake needs to simulate state changes during a single test, use [mutable fakes](immutable-vs-mutable.md):

### Simulating Failure After Success

```kotlin
@Test
fun `GIVEN repository succeeds WHEN database goes down THEN service handles failure`() {
    val fake = fakeMutableRepository {
        delete { true }
    }

    // Phase 1: Operations succeed
    assertTrue(fake.delete("item-1"))

    // Phase 2: Simulate database failure
    fake.modify {
        delete { throw IllegalStateException("Database unavailable") }
    }

    assertFailsWith<IllegalStateException> {
        fake.delete("item-2")
    }
}
```

### Property State Changes

```kotlin
@Test
fun `GIVEN service initializing WHEN ready THEN status updates`() {
    val fake = fakeMutableRepository {
        status { "initializing" }
    }
    assertEquals("initializing", fake.status)

    fake.modify {
        status { "ready" }
    }
    assertEquals("ready", fake.status)
}
```

### Mutable Fakes with Call History

Call history accumulates across reconfigurations — it is never reset:

```kotlin
@Test
fun `GIVEN mutable tracked fake WHEN reconfigured THEN call history persists`() {
    val fake = fakeMutableTrackedRepository {
        find { listOf(User("1", "Test")) }
    }

    fake.find("query1")
    fake.find("query2")
    assertEquals(2, fake.findCalls.value.size)

    fake.modify {
        find { emptyList() }
    }

    assertEquals(emptyList(), fake.find("query3"))
    assertEquals(3, fake.findCalls.value.size)  // Accumulated
}
```

!!! warning "Prefer Immutable Fakes for Unit Tests"
    Mutable fakes are designed for integration tests where the same injected instance must simulate different states. For unit tests, create separate immutable fake instances per scenario — this is safer and easier to reason about.

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
