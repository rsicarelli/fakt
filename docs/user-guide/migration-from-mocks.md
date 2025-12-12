# Migration from Mocks

Migrating from mocking frameworks (MockK, Mockito, Mokkery, Mockative) to Fakt compile-time fakes.

---

## Why Migrate?

Runtime mocking frameworks (MockK, Mockito) rely on reflection and bytecode manipulation, which **do not exist on Native or Wasm targets**—they cannot run in `commonTest` source sets for Kotlin Multiplatform. KSP-based alternatives (Mokkery, Mockative, MocKMP) attempt to solve this through compile-time generation but face critical limitations: Mokkery cannot mock `object` or `sealed` types, Mockative broke with Kotlin 2.0 forcing mass migrations, and all impose complex APIs with separate stubbing syntax.

Mock-based tests couple to implementation details (verifying *how* methods are called), breaking on valid refactorings. When you change *which* method is called but the *outcome* remains the same, mock tests report false failures. This creates brittle test suites that discourage refactoring and accumulate technical debt.

Fakt follows Google's "Now in Android" directive: **"Don't use mocking frameworks. Instead, use fakes."** Fakt generates clean, type-safe fakes that verify *outcomes*, work across all KMP targets with zero runtime cost, and use DSL lambdas matching your interface signatures—no separate stubbing APIs to learn.

[Learn more about Fakt's design philosophy →](../get-started/why-fakt.md)

---

## MockK → Fakt

### Basic Usage

**MockK:**

```kotlin
@Test
fun `GIVEN mock repository WHEN processing user THEN calls getUser`() {
    val mock = mockk<UserRepository>()
    every { mock.getUser(any()) } returns User("123", "Alice")

    val service = UserService(mock)
    service.processUser("123")

    verify(exactly = 1) { mock.getUser("123") }
}
```

**Fakt:**

```kotlin
@Test
fun `GIVEN fake repository WHEN processing user THEN calls getUser`() {
    val fake = fakeUserRepository {
        getUser { id -> User(id, "Alice") }
    }

    val service = UserService(fake)
    service.processUser("123")

    assertEquals(1, fake.getUserCallCount.value)
}
```

### Suspend Functions with Error Handling

**MockK:**

```kotlin
@Test
fun `GIVEN repository failure WHEN fetching data THEN handles error`() = runTest {
    val mock = mockk<DataService>()
    coEvery { mock.fetchData() } throws NetworkException("Connection timeout")

    val viewModel = DataViewModel(mock)
    viewModel.loadData()

    assertTrue(viewModel.errorState.value is Error.Network)
}
```

**Fakt:**

```kotlin
@Test
fun `GIVEN repository failure WHEN fetching data THEN handles error`() = runTest {
    val fake = fakeDataService {
        fetchData { throw NetworkException("Connection timeout") }
    }

    val viewModel = DataViewModel(fake)
    viewModel.loadData()

    assertTrue(viewModel.errorState.value is Error.Network)
}
```

**Key difference**: Fakt's DSL lambda has the same signature as the original interface method. No need to learn separate stubbing APIs (`coEvery`, `every`, `returns`).

---

## Mockito → Fakt

**Mockito:**

```kotlin
@Test
fun `GIVEN repository WHEN saving user THEN returns success`() {
    val mock = mock(UserRepository::class.java)
    `when`(mock.saveUser(any())).thenReturn(Result.success(Unit))

    val service = UserService(mock)
    val result = service.createUser("Alice")

    assertTrue(result.isSuccess)
    verify(mock, times(1)).saveUser(any())
}
```

**Fakt:**

```kotlin
@Test
fun `GIVEN repository WHEN saving user THEN returns success`() = runTest {
    val fake = fakeUserRepository {
        saveUser { user -> Result.success(Unit) }
    }

    val service = UserService(fake)
    val result = service.createUser("Alice")

    assertTrue(result.isSuccess)
    assertEquals(1, fake.saveUserCallCount.value)
}
```

---

## Mokkery → Fakt

**Mokkery:**

```kotlin
@Test
fun `GIVEN mock analytics WHEN tracking event THEN records event`() = runTest {
    val mock = mock<Analytics>()
    everySuspend { mock.track(any()) } returns Unit

    val service = AnalyticsService(mock)
    service.logUserAction("button_click")

    verifySuspend { mock.track("button_click") }
}
```

**Fakt:**

```kotlin
@Test
fun `GIVEN fake analytics WHEN tracking event THEN records event`() = runTest {
    val trackedEvents = mutableListOf<String>()
    val fake = fakeAnalytics {
        track { event -> trackedEvents.add(event) }
    }

    val service = AnalyticsService(fake)
    service.logUserAction("button_click")

    assertEquals(1, fake.trackCallCount.value)
    assertEquals("button_click", trackedEvents.first())
}
```

**Migration notes**:

- Replace `mock<T>()` with `fakeT {}`
- Replace `everySuspend { }` with DSL lambda configuration
- Replace `verifySuspend { }` with StateFlow call counters
- State-based verification (checking `trackedEvents`) is more resilient than interaction verification

---

## Mockative → Fakt

**Mockative:**

```kotlin
@Test
fun `GIVEN repository WHEN fetching user THEN returns user`() = runTest {
    val mock = mock<UserRepository>()
    given(mock).coroutine { getUser("123") }.thenReturn(User("123", "Alice"))

    val viewModel = UserViewModel(mock)
    viewModel.loadUser("123")

    assertEquals("Alice", viewModel.userName.value)
}
```

**Fakt:**

```kotlin
@Test
fun `GIVEN repository WHEN fetching user THEN returns user`() = runTest {
    val fake = fakeUserRepository {
        getUser { id -> User(id, "Alice") }
    }

    val viewModel = UserViewModel(fake)
    viewModel.loadUser("123")

    assertEquals("Alice", viewModel.userName.value)
    assertEquals(1, fake.getUserCallCount.value)
}
```

---

## Generic Repositories

**MockK:**

```kotlin
@Test
fun `GIVEN generic repository WHEN saving item THEN returns success`() {
    val mock = mockk<Repository<User>>()
    every { mock.save(any()) } returns Result.success(Unit)

    val service = CrudService(mock)
    service.createUser(User("123", "Alice"))

    verify { mock.save(any<User>()) }
}
```

**Fakt:**

```kotlin
@Test
fun `GIVEN generic repository WHEN saving item THEN returns success`() {
    val fake = fakeRepository<User> {
        save { item -> Result.success(Unit) }
    }

    val service = CrudService(fake)
    service.createUser(User("123", "Alice"))

    assertEquals(1, fake.saveCallCount.value)
}
```

**Type safety**: Fakt preserves generic type parameters. The DSL lambda receives `item: User`, not `item: Any`.

---

## Next Steps

- **[Testing Patterns](testing-patterns.md)** - Best practices for fake-based testing
- **[Usage Guide](usage.md)** - Core Fakt patterns and examples
- **[Performance](performance.md)** - Build time impact and optimization
