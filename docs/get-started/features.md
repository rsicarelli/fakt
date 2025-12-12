# Features

Quick reference of all Fakt capabilities.

---

## Supported Class Types

| Case | Example |
|------|---------|
| **Interfaces** | `@Fake interface UserRepository { suspend fun getUser(id: String): User }` |
| **Abstract Classes** | `@Fake abstract class BaseService { abstract fun start(): Boolean }` |
| **Open Classes** | `@Fake open class NetworkClient { open suspend fun fetch(url: String): Result<String> }` |
| **Data Classes** | ❌ Can't be faked. ✅ Works as parameter/return types. |
| **Sealed Classes** | ❌ Can't be faked. ✅ Works as parameter/return types. |

---

## Type System

| Case | Example |
|------|---------|
| **Class-Level Generics** | `val fake = fakeRepository<User> { save { item -> Result.success(Unit) } }` |
| **Method-Level Generics** | `fun <T, R> transform(input: T, mapper: (T) -> R): R` |
| **Generic Constraints** | `interface ComparableRepository<T : Comparable<T>> { fun findMax(items: List<T>): T? }` |
| **Variance** | `interface Producer<out T>`, `interface Consumer<in T>` |
| **Nullable Types** | `fun findUser(id: String): User? // Default: null` |
| **Result Types** | `fun save(): Result<Unit> // Default: Result.failure(...)` |
| **Collections** | `fun getAll(): List<User> // Default: emptyList()` |

---

## Language Features

| Case | Example |
|------|---------|
| **Suspend Functions** | `suspend fun fetch(): User // Works in runTest { }` |
| **Properties (val)** | `val apiUrl: String // fake.apiUrl + fake.apiUrlCallCount` |
| **Properties (var)** | `var theme: String // getThemeCallCount + setThemeCallCount` |
| **Inheritance** | `interface UserRepo : BaseRepo { ... } // Inherits parent methods` |
| **Default Parameters** | `fun log(msg: String, level: LogLevel = INFO)` |

---

## Call Tracking

| Case | Example |
|------|---------|
| **StateFlow Counters** | `fake.trackCallCount.value // Thread-safe Int counter` |
| **Reactive Testing** | `fake.trackCallCount.test { awaitItem() shouldBe 1 } // Turbine` |
| **Property Tracking** | `fake.getThemeCallCount`, `fake.setThemeCallCount` |
| **Thread Safety** | `// All counters use MutableStateFlow.update` |

---

## Code Generation

| Case | Example |
|------|---------|
| **Factory Functions** | `fakeUserRepository { getUser { id -> User(id, "Alice") } }` |
| **Type-Safe DSL** | `// Compiler catches type errors at build time` |
| **Smart Defaults** | `// String → "", Int → 0, Boolean → false, List → emptyList()` |

---

## Multi-Module (Experimental)

| Case | Example |
|------|---------|
| **Collector Module** | `fakt { collectFakesFrom(projects.core.analytics) }` |
| **Consumer Module** | `dependencies { commonTest { implementation(projects.core.analyticsFakes) } }` |

---

## Performance

| Case | Example |
|------|---------|
| **Intelligent Caching** | `// First target: ~40ms, subsequent: ~1ms` |
| **Log Levels** | `fakt { logLevel.set(LogLevel.INFO) } // QUIET, INFO, DEBUG` |

---

## Platform Support

| Case | Example |
|------|---------|
| **All KMP Targets** | `✅ JVM, Android, iOS, macOS, Linux, Windows, JS, WASM, watchOS, tvOS` |
| **Single Platform** | `✅ JVM-only, Android-only projects fully supported` |

---

## Next Steps

- [Usage Guide](../user-guide/usage.md) - Comprehensive usage reference with detailed examples
- [Why Fakt?](why-fakt.md) - Design philosophy and advantages
- [Multi-Module](../user-guide/multi-module.md) - Cross-module fakes
