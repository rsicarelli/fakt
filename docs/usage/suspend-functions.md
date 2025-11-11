# Suspend Functions

Fakt fully supports Kotlin coroutines and suspend functions without any special configuration.

---

## Basic Suspend Functions

Suspend functions work naturally in generated fakes:

```kotlin
@Fake
interface ApiClient {
    suspend fun fetchData(id: String): Result<Data>
    suspend fun upload(data: Data): Result<Unit>
}
```

**Usage in tests:**

```kotlin
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ApiClientTest {
    @Test
    fun `GIVEN API client fake WHEN fetching data THEN suspends correctly`() = runTest {
        val testData = Data("123", "content")

        val fake = fakeApiClient {
            fetchData { id ->
                delay(100)  // Suspends correctly
                Result.success(testData)
            }
            upload { data ->
                delay(50)
                Result.success(Unit)
            }
        }

        val result = fake.fetchData("123")

        assertTrue(result.isSuccess)
        assertEquals(testData, result.getOrNull())
        assertEquals(1, fake.fetchDataCallCount.value)
    }
}
```

---

## Suspend + Non-Suspend Mix

Interfaces can mix suspend and regular functions:

```kotlin
@Fake
interface UserRepository {
    fun getLocalUser(id: String): User?
    suspend fun fetchRemoteUser(id: String): Result<User>
    suspend fun syncUsers(): Result<Unit>
}
```

**Usage:**

```kotlin
@Test
fun `GIVEN repository WHEN calling mixed functions THEN works correctly`() = runTest {
    val fake = fakeUserRepository {
        getLocalUser { id -> User(id, "Local") }
        fetchRemoteUser { id ->
            delay(100)
            Result.success(User(id, "Remote"))
        }
        syncUsers {
            delay(200)
            Result.success(Unit)
        }
    }

    // Regular function (no suspend)
    val local = fake.getLocalUser("123")
    assertEquals("Local", local?.name)

    // Suspend functions
    val remote = fake.fetchRemoteUser("456")
    val syncResult = fake.syncUsers()

    assertTrue(remote.isSuccess)
    assertTrue(syncResult.isSuccess)
}
```

---

## Suspend Properties

Properties with suspend getters are supported:

```kotlin
@Fake
interface AsyncConfig {
    suspend fun loadConfig(): Map<String, String>
}
```

**Usage:**

```kotlin
@Test
fun `GIVEN async config WHEN loading THEN suspends correctly`() = runTest {
    val fake = fakeAsyncConfig {
        loadConfig {
            delay(50)
            mapOf("key" to "value")
        }
    }

    val config = fake.loadConfig()

    assertEquals("value", config["key"])
}
```

---

## Exception Handling

Suspend functions can throw exceptions naturally:

```kotlin
@Test
fun `GIVEN API client WHEN fetch fails THEN throws exception`() = runTest {
    val fake = fakeApiClient {
        fetchData { id ->
            delay(100)
            throw NetworkException("Connection failed")
        }
    }

    assertFailsWith<NetworkException> {
        fake.fetchData("123")
    }
}
```

---

## Coroutine Context

Fakt-generated fakes work with all coroutine contexts:

```kotlin
@Test
fun `GIVEN repository WHEN using different dispatchers THEN works correctly`() = runTest {
    val fake = fakeRepository {
        fetchData { id ->
            withContext(Dispatchers.Default) {
                // Computation
                Data(id)
            }
        }
    }

    val data = fake.fetchData("123")
    assertEquals("123", data.id)
}
```

---

## Best Practices

### ✅ Use runTest for Suspend Tests

Always wrap suspend function tests in `runTest`:

```kotlin
@Test
fun `test suspend function`() = runTest {  // ✅ Required
    val fake = fakeApiClient()
    fake.fetchData("123")
}
```

### ✅ Use delay() for Testing Timing

Test timing-sensitive code with `delay()`:

```kotlin
@Test
fun `GIVEN slow API WHEN fetching THEN handles timeout`() = runTest {
    val fake = fakeApiClient {
        fetchData { id ->
            delay(5000)  // Simulate slow response
            Result.success(Data(id))
        }
    }

    withTimeout(1000) {
        assertFailsWith<TimeoutCancellationException> {
            fake.fetchData("123")
        }
    }
}
```

---

## Next Steps

- [Generics](generics.md) - Generic type handling
- [Properties](properties.md) - val/var faking
- [Call Tracking](call-tracking.md) - StateFlow patterns
