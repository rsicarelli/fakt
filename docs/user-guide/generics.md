# Generics

Fakt fully supports generic type parameters at both class and method levels.

---

## Class-Level Generics

```kotlin
@Fake
interface Repository<T> {
    fun save(item: T): Result<Unit>
    fun getAll(): List<T>
}

val fake = fakeRepository<User> {
    save { item -> Result.success(Unit) }
    getAll { emptyList() }
}
```

---

## Method-Level Generics

```kotlin
@Fake
interface Transformer {
    fun <T, R> transform(input: T, mapper: (T) -> R): R
}

val fake = fakeTransformer {
    transform { input, mapper -> mapper(input) }
}
```

---

## Generic Constraints

```kotlin
@Fake
interface ComparableRepository<T : Comparable<T>> {
    fun findMax(items: List<T>): T?
}

val fake = fakeComparableRepository<Int> {
    findMax { items -> items.maxOrNull() }
}
```

---

## Variance

Fakt supports variance modifiers (`out`, `in`):

```kotlin
@Fake
interface Producer<out T> {
    fun produce(): T
}

@Fake
interface Consumer<in T> {
    fun consume(item: T)
}
```

---

## Next Steps

- [Properties](properties.md) - val/var faking
- [Call Tracking](call-tracking.md) - StateFlow patterns
