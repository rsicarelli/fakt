# Migration from MockK/Mockito

Migrating from runtime mocking frameworks to Fakt compile-time fakes.

---

## Why Migrate?

| Aspect               | MockK/Mockito          | Fakt                   |
|----------------------|------------------------|------------------------|
| **Platform Support** | JVM/Android only       | All KMP targets        |
| **Runtime Cost**     | Reflection overhead    | Zero (compile-time)    |
| **Type Safety**      | Runtime errors         | Compile-time errors    |
| **Debugging**        | Proxy magic            | Generated code         |
| **Refactoring**      | Silent failures        | Breaks at compile-time |

---

## MockK → Fakt

**MockK:**

```kotlin
@Test
fun `test with MockK`() {
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

---

## Mockito → Fakt

**Mockito:**

```kotlin
@Test
fun `test with Mockito`() {
    val mock = mock(UserRepository::class.java)
    `when`(mock.getUser(anyString())).thenReturn(User("123", "Alice"))

    val service = UserService(mock)
    service.processUser("123")

    verify(mock, times(1)).getUser("123")
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

---

## Migration Checklist

- [ ] Add Fakt plugin to `build.gradle.kts`
- [ ] Annotate interfaces with `@Fake`
- [ ] Build project to generate fakes
- [ ] Replace `mockk<T>()` / `mock(T::class.java)` with `fakeT {}`
- [ ] Replace `every { }` / `when()` with DSL configuration
- [ ] Replace `verify { }` with call counter assertions
- [ ] Remove MockK/Mockito dependencies

---

## Next Steps

- [Testing Patterns](testing-patterns.md) - Best practices
- [Performance](performance.md) - Build time impact
