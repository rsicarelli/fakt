# Features

Quick reference of all Fakt capabilities.

---

## Supported Class Types

<table>
<tr><th>Case</th><th>Example</th></tr>
<tr>
<td><strong>Interfaces</strong></td>
<td>

```kotlin
@Fake
interface UserRepository {
    suspend fun getUser(id: String): User
}
```

</td>
</tr>
<tr>
<td><strong>Abstract Classes</strong></td>
<td>

```kotlin
@Fake
abstract class BaseService {
    abstract fun start(): Boolean
}
```

</td>
</tr>
<tr>
<td><strong>Open Classes</strong></td>
<td>

```kotlin
@Fake
open class NetworkClient {
    open suspend fun fetch(url: String): Result<String>
}
```

</td>
</tr>
<tr>
<td><strong>Data Classes</strong></td>
<td>

❌ <strong>Can't be faked</strong><br>
✅ <strong>Works as parameter/return types</strong>

</td>
</tr>
<tr>
<td><strong>Sealed Classes</strong></td>
<td>

❌ <strong>Can't be faked</strong><br>
✅ <strong>Works as parameter/return types</strong>

</td>
</tr>
</table>

---

## Type System

<table>
<tr><th>Case</th><th>Example</th></tr>
<tr>
<td><strong>Class-Level Generics</strong></td>
<td>

```kotlin
val fake = fakeRepository<User> {
    save { item -> Result.success(Unit) }
}
```

</td>
</tr>
<tr>
<td><strong>Method-Level Generics</strong></td>
<td>

```kotlin
fun <T, R> transform(input: T, mapper: (T) -> R): R
```

</td>
</tr>
<tr>
<td><strong>Generic Constraints</strong></td>
<td>

```kotlin
interface ComparableRepository<T : Comparable<T>> {
    fun findMax(items: List<T>): T?
}
```

</td>
</tr>
<tr>
<td><strong>Variance</strong></td>
<td>

```kotlin
interface Producer<out T>
interface Consumer<in T>
```

</td>
</tr>
<tr>
<td><strong>Nullable Types</strong></td>
<td>

```kotlin
fun findUser(id: String): User? // Default: null
```

</td>
</tr>
<tr>
<td><strong>Result Types</strong></td>
<td>

```kotlin
fun save(): Result<Unit> // Default: Result.failure(...)
```

</td>
</tr>
<tr>
<td><strong>Collections</strong></td>
<td>

```kotlin
fun getAll(): List<User> // Default: emptyList()
```

</td>
</tr>
</table>

---

## Language Features

<table>
<tr><th>Case</th><th>Example</th></tr>
<tr>
<td><strong>Suspend Functions</strong></td>
<td>

```kotlin
suspend fun fetch(): User // Works in runTest { }
```

</td>
</tr>
<tr>
<td><strong>Properties (val)</strong></td>
<td>

```kotlin
val apiUrl: String // fake.apiUrl + fake.apiUrlCalls.value.size
```

</td>
</tr>
<tr>
<td><strong>Properties (var)</strong></td>
<td>

```kotlin
var theme: String // getThemeCalls.value.size + setThemeCalls.value.size
```

</td>
</tr>
<tr>
<td><strong>Inheritance</strong></td>
<td>

```kotlin
interface UserRepo : BaseRepo { ... } // Inherits parent methods
```

</td>
</tr>
<tr>
<td><strong>Default Parameters</strong></td>
<td>

```kotlin
fun log(msg: String, level: LogLevel = INFO)
```

</td>
</tr>
</table>

---

## Call History & Verification

<table>
<tr><th>Case</th><th>Example</th></tr>
<tr>
<td><strong>Call Counts</strong></td>
<td>

```kotlin
fake.trackCalls.value.size // Thread-safe call count
```

</td>
</tr>
<tr>
<td><strong>Verification DSL</strong></td>
<td>

```kotlin
fake.verifyTrack {
    assertTrue(wasCalledTimes(2))
    assertTrue(wasCalledWith("page_view"))
}
```

</td>
</tr>
<tr>
<td><strong>Call History</strong></td>
<td>

```kotlin
assertEquals("page_view", fake.trackCallHistory.first.event)
assertEquals(2, fake.trackCallHistory.all.size)
```

</td>
</tr>
<tr>
<td><strong>Property Tracking</strong></td>
<td>

```kotlin
fake.getThemeCalls.value.size
fake.setThemeCalls.value.size
```

</td>
</tr>
<tr>
<td><strong>Thread Safety</strong></td>
<td>

Call history is backed by thread-safe internal state.

</td>
</tr>
<tr>
<td><strong>Configurable</strong></td>
<td>

```kotlin
@Fake(callHistory = CallHistoryMode.DISABLED)
interface Logger { ... }  // Lightweight, no tracking
```

</td>
</tr>
</table>

---

## Behavior Mutability

<table>
<tr><th>Case</th><th>Example</th></tr>
<tr>
<td><strong>Immutable (Default)</strong></td>
<td>

```kotlin
@Fake  // Immutable by default
interface UserService { ... }
```

</td>
</tr>
<tr>
<td><strong>Mutable (Opt-In)</strong></td>
<td>

```kotlin
@Fake(mutability = MutabilityMode.MUTABLE)
interface UserRepository { ... }

val fake = fakeUserRepository { findById { User("1", "Alice") } }
fake.modify { findById { null } }  // Reconfigure mid-test
```

</td>
</tr>
<tr>
<td><strong>Explicit Immutable</strong></td>
<td>

```kotlin
@Fake(mutability = MutabilityMode.IMMUTABLE)
interface Logger { ... }  // Always immutable, overrides plugin default
```

</td>
</tr>
<tr>
<td><strong>Configurable</strong></td>
<td>

```kotlin
fakt {
    enableMutableFakes.set(true)  // All fakes mutable by default
}

@Fake(mutability = MutabilityMode.IMMUTABLE)
interface Logger { ... }  // Override: always immutable
```

</td>
</tr>
</table>

---

## Code Generation

<table>
<tr><th>Case</th><th>Example</th></tr>
<tr>
<td><strong>Factory Functions</strong></td>
<td>

```kotlin
fakeUserRepository { getUser { id -> User(id, "Alice") } }
```

</td>
</tr>
<tr>
<td><strong>Type-Safe DSL</strong></td>
<td>

Compiler catches type errors at build time.

</td>
</tr>
<tr>
<td><strong>Smart Defaults</strong></td>
<td>

<ul>
<li>String → <code>""</code></li>
<li>Int → <code>0</code></li>
<li>Boolean → <code>false</code></li>
<li>List → <code>emptyList()</code></li>
</ul>

</td>
</tr>
</table>

---

## Multi-Module (Experimental)

<table>
<tr><th>Case</th><th>Example</th></tr>
<tr>
<td><strong>Collector Module</strong></td>
<td>

```kotlin
fakt {
    @OptIn(ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
```

</td>
</tr>
<tr>
<td><strong>Consumer Module</strong></td>
<td>

```kotlin
dependencies {
    commonTest {
        implementation(projects.core.analyticsFakes)
    }
}
```

</td>
</tr>
</table>

---

## Platform Support

<table>
<tr><th>Case</th><th>Example</th></tr>
<tr>
<td><strong>All KMP Targets</strong></td>
<td>

✅ JVM, Android, iOS, macOS, Linux, Windows, JS, WASM, watchOS, tvOS

</td>
</tr>
<tr>
<td><strong>Single Platform</strong></td>
<td>

✅ JVM-only, Android-only projects fully supported

</td>
</tr>
</table>

---

## Next Steps

- [Usage Guide](../user-guide/usage.md) - Comprehensive usage reference with detailed examples
- [Why Fakt?](../why-fakt.md) - Design philosophy and advantages
- [Multi-Module](../user-guide/multi-module.md) - Cross-module fakes
