# Pattern: Suspend Function Handling

> **Purpose**: Comprehensive patterns for working with suspend functions and coroutines in KtFakes
> **Complexity**: Intermediate to Advanced
> **Testing Standard**: [📋 Testing Guidelines](../validation/testing-guidelines.md)

## 🎯 **Pattern Overview**

KtFakes provides full support for suspend functions with proper coroutine integration. This pattern covers:
- Basic suspend function interfaces
- Coroutine behavior configuration
- Flow and async patterns
- Error handling in coroutine contexts
- Performance testing with timing

## ⚡ **Basic Suspend Function Patterns**

### **Simple Suspend Interface**
```kotlin
@Fake
interface AsyncUserService {
    suspend fun getUser(id: String): User
    suspend fun updateUser(user: User): Boolean
    suspend fun deleteUser(id: String): Unit
}
```

**Generated Type-Safe API**:
```kotlin
class FakeAsyncUserServiceImpl : AsyncUserService {
    private var getUserBehavior: suspend (String) -> User = { User("", "", "") }
    private var updateUserBehavior: suspend (User) -> Boolean = { false }
    private var deleteUserBehavior: suspend (String) -> Unit = { Unit }

    override suspend fun getUser(id: String): User = getUserBehavior(id)
    override suspend fun updateUser(user: User): Boolean = updateUserBehavior(user)
    override suspend fun deleteUser(id: String): Unit = deleteUserBehavior(id)
}
```

**Basic Usage**:
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AsyncUserServiceTest {

    @Test
    fun `GIVEN async user service WHEN calling suspend functions THEN should work correctly`() = runTest {
        // Given
        val userService = fakeAsyncUserService {
            getUser { id -> User(id, "$id@example.com", "User-$id") }
            updateUser { user -> user.name.isNotEmpty() }
            deleteUser { id -> Unit } // No-op for delete
        }

        // When
        val user = userService.getUser("123")
        val updateResult = userService.updateUser(User("123", "john@example.com", "John"))
        userService.deleteUser("123")

        // Then
        assertEquals("123", user.id)
        assertEquals("123@example.com", user.email)
        assertTrue(updateResult)
    }
}
```

## 🌊 **Flow Integration Patterns**

### **Flow-Based Interfaces**
```kotlin
@Fake
interface DataStreamService {
    suspend fun getDataStream(): Flow<String>
    suspend fun processStream(input: Flow<Int>): Flow<String>
    fun observeChanges(): Flow<ChangeEvent>
}
```

**Flow Configuration**:
```kotlin
@Test
fun `GIVEN data stream service WHEN working with flows THEN should handle streams correctly`() = runTest {
    // Given
    val streamService = fakeDataStreamService {
        getDataStream {
            flow {
                emit("data1")
                delay(10)
                emit("data2")
                delay(10)
                emit("data3")
            }
        }
        processStream { inputFlow ->
            inputFlow.map { "processed-$it" }
        }
        observeChanges {
            flow {
                repeat(3) { index ->
                    emit(ChangeEvent("change-$index"))
                    delay(5)
                }
            }
        }
    }

    // When
    val dataList = streamService.getDataStream().toList()
    val processedList = streamService.processStream(flowOf(1, 2, 3)).toList()
    val changes = streamService.observeChanges().take(2).toList()

    // Then
    assertEquals(listOf("data1", "data2", "data3"), dataList)
    assertEquals(listOf("processed-1", "processed-2", "processed-3"), processedList)
    assertEquals(2, changes.size)
    assertEquals("change-0", changes[0].description)
}
```

## 🎯 **Result and Error Handling Patterns**

### **Result-Based Suspend Functions**
```kotlin
@Fake
interface ApiClient {
    suspend fun fetchData(url: String): Result<String>
    suspend fun postData(url: String, data: String): Result<Unit>
    suspend fun uploadFile(file: ByteArray): Result<String>
}
```

**Error Scenario Testing**:
```kotlin
@Test
fun `GIVEN api client WHEN handling errors THEN should return appropriate results`() = runTest {
    // Given
    val apiClient = fakeApiClient {
        fetchData { url ->
            when {
                url.contains("timeout") -> {
                    delay(100)
                    Result.failure(Exception("Timeout"))
                }
                url.contains("404") -> Result.failure(Exception("Not Found"))
                url.contains("success") -> Result.success("""{"data": "success"}""")
                else -> Result.failure(Exception("Unknown error"))
            }
        }
        postData { url, data ->
            when {
                data.isEmpty() -> Result.failure(Exception("Empty data"))
                url.contains("fail") -> Result.failure(Exception("Server error"))
                else -> Result.success(Unit)
            }
        }
    }

    // When & Then - Test various scenarios
    val timeoutResult = apiClient.fetchData("https://api.timeout.com/data")
    assertTrue(timeoutResult.isFailure)
    assertEquals("Timeout", timeoutResult.exceptionOrNull()?.message)

    val successResult = apiClient.fetchData("https://api.success.com/data")
    assertTrue(successResult.isSuccess)
    assertEquals("""{"data": "success"}""", successResult.getOrNull())

    val postSuccess = apiClient.postData("https://api.example.com/data", "valid data")
    assertTrue(postSuccess.isSuccess)

    val postFailure = apiClient.postData("https://api.fail.com/data", "")
    assertTrue(postFailure.isFailure)
}
```

## ⏱️ **Timing and Performance Patterns**

### **Delayed Operations**
```kotlin
@Fake
interface SlowService {
    suspend fun quickOperation(): String
    suspend fun slowOperation(): String
    suspend fun variableOperation(complexity: Int): String
}
```

**Timing Behavior**:
```kotlin
@Test
fun `GIVEN slow service WHEN measuring performance THEN should have expected timing`() = runTest {
    // Given
    val slowService = fakeSlowService {
        quickOperation {
            delay(5)
            "quick-result"
        }
        slowOperation {
            delay(50)
            "slow-result"
        }
        variableOperation { complexity ->
            delay(complexity * 10L)
            "result-$complexity"
        }
    }

    // When & Then - Test timing behavior
    val quickStart = System.currentTimeMillis()
    val quickResult = slowService.quickOperation()
    val quickTime = System.currentTimeMillis() - quickStart

    val slowStart = System.currentTimeMillis()
    val slowResult = slowService.slowOperation()
    val slowTime = System.currentTimeMillis() - slowStart

    assertEquals("quick-result", quickResult)
    assertEquals("slow-result", slowResult)
    assertTrue(quickTime < slowTime)
    assertTrue(quickTime >= 5)  // At least the delay time
    assertTrue(slowTime >= 50)  // At least the delay time
}
```

### **Cancellation Support**
```kotlin
@Test
fun `GIVEN cancellable service WHEN cancelling operation THEN should handle cancellation`() = runTest {
    // Given
    val cancellableService = fakeCancellableService {
        longRunningOperation {
            try {
                delay(1000)  // Long operation
                "completed"
            } catch (e: CancellationException) {
                "cancelled"
            }
        }
    }

    // When
    val job = launch {
        cancellableService.longRunningOperation()
    }

    delay(50)  // Let it start
    job.cancel()  // Cancel early

    // Then
    assertTrue(job.isCancelled)
}
```

## 🔄 **Async Coordination Patterns**

### **Multiple Suspend Operations**
```kotlin
@Fake
interface CoordinatedService {
    suspend fun stepOne(): String
    suspend fun stepTwo(input: String): String
    suspend fun stepThree(input: String): Boolean
}
```

**Sequential and Parallel Execution**:
```kotlin
@Test
fun `GIVEN coordinated service WHEN running operations THEN should coordinate correctly`() = runTest {
    // Given
    val executionOrder = mutableListOf<String>()
    val coordinatedService = fakeCoordinatedService {
        stepOne {
            delay(10)
            executionOrder.add("step1")
            "step1-result"
        }
        stepTwo { input ->
            delay(5)
            executionOrder.add("step2")
            "step2-$input"
        }
        stepThree { input ->
            delay(15)
            executionOrder.add("step3")
            input.contains("step2")
        }
    }

    // When - Sequential execution
    val result1 = coordinatedService.stepOne()
    val result2 = coordinatedService.stepTwo(result1)
    val result3 = coordinatedService.stepThree(result2)

    // Then
    assertEquals(listOf("step1", "step2", "step3"), executionOrder)
    assertTrue(result3)

    // When - Parallel execution
    executionOrder.clear()
    val parallelResults = listOf(
        async { coordinatedService.stepOne() },
        async { coordinatedService.stepTwo("parallel-input") },
        async { coordinatedService.stepThree("parallel-input") }
    ).awaitAll()

    // Then - Order might vary in parallel execution
    assertEquals(3, parallelResults.size)
    assertEquals(3, executionOrder.size)
}
```

## 📊 **State Management in Suspend Context**

### **Stateful Async Operations**
```kotlin
@Test
fun `GIVEN stateful async service WHEN managing state THEN should maintain consistency`() = runTest {
    // Given - Thread-safe state management
    val connections = mutableSetOf<String>()
    val mutex = Mutex()

    val connectionService = fakeConnectionService {
        connect { serverId ->
            mutex.withLock {
                if (connections.add(serverId)) {
                    delay(10)  // Simulate connection time
                    true
                } else false  // Already connected
            }
        }
        disconnect { serverId ->
            mutex.withLock {
                connections.remove(serverId)
            }
        }
        getConnections {
            mutex.withLock {
                connections.toSet()
            }
        }
    }

    // When - Concurrent operations
    val connectResults = listOf("server1", "server2", "server1", "server3").map { server ->
        async { connectionService.connect(server) }
    }.awaitAll()

    val activeConnections = connectionService.getConnections()

    // Then
    assertEquals(listOf(true, true, false, true), connectResults)  // server1 duplicate fails
    assertEquals(setOf("server1", "server2", "server3"), activeConnections)

    // When - Disconnect
    connectionService.disconnect("server1")
    val finalConnections = connectionService.getConnections()

    // Then
    assertEquals(setOf("server2", "server3"), finalConnections)
}
```

## 🎮 **Channel and Actor Patterns**

### **Channel-Based Communication**
```kotlin
@Fake
interface MessageBroker {
    suspend fun sendMessage(channel: String, message: String): Boolean
    suspend fun receiveMessage(channel: String): String?
    fun createChannel(name: String): Channel<String>
}
```

**Channel Configuration**:
```kotlin
@Test
fun `GIVEN message broker WHEN using channels THEN should handle message flow`() = runTest {
    // Given
    val channels = mutableMapOf<String, Channel<String>>()
    val messageBroker = fakeMessageBroker {
        createChannel { name ->
            Channel<String>(Channel.UNLIMITED).also { channels[name] = it }
        }
        sendMessage { channel, message ->
            channels[channel]?.trySend(message)?.isSuccess ?: false
        }
        receiveMessage { channel ->
            channels[channel]?.tryReceive()?.getOrNull()
        }
    }

    // When
    val channel = messageBroker.createChannel("test-channel")
    val sendResult = messageBroker.sendMessage("test-channel", "Hello World")
    val receivedMessage = messageBroker.receiveMessage("test-channel")

    // Then
    assertTrue(sendResult)
    assertEquals("Hello World", receivedMessage)
}
```

## 🔗 **Best Practices for Suspend Function Testing**

### **Use runTest for Coroutine Tests**
```kotlin
@Test
fun `always use runTest for suspend function testing`() = runTest {
    // ✅ Correct - provides proper coroutine context
    val service = fakeAsyncService {
        operation { delay(10); "result" }
    }
    assertEquals("result", service.operation())
}
```

### **Test Cancellation Behavior**
```kotlin
@Test
fun `test cancellation handling`() = runTest {
    val service = fakeCancellableService {
        operation {
            delay(1000)
            "should-not-complete"
        }
    }

    val job = launch { service.operation() }
    delay(10)
    job.cancel()
    assertTrue(job.isCancelled)
}
```

### **Verify Timing Constraints**
```kotlin
@Test
fun `verify operation timing`() = runTest {
    val service = fakeTimedService {
        operation {
            delay(50)
            "timed-result"
        }
    }

    val startTime = System.currentTimeMillis()
    val result = service.operation()
    val elapsed = System.currentTimeMillis() - startTime

    assertEquals("timed-result", result)
    assertTrue(elapsed >= 50)
}
```

## 🔗 **Related Patterns**

- **[📋 Basic Fake Generation](basic-fake-generation.md)** - Foundation patterns
- **[📋 Complex Generics Strategy](complex-generics-strategy.md)** - Generic suspend functions
- **[📋 Working Examples](../examples/working-examples.md)** - Real-world async patterns
- **[📋 Type Safety Validation](../validation/type-safety-validation.md)** - Suspend function type safety

---

**Suspend function handling in KtFakes provides full coroutine support with type safety, making async testing natural and reliable.**