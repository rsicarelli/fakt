# kmp-all-jvm

A Kotlin Multiplatform module where every target runs on the JVM:

```kotlin
kotlin {
    jvm("desktop")
    jvm("server")
}
```

## What it proves

Kotlin gives `commonMain` no compilation of its own when there is no non-JVM target. Fakt adds a
common producer for that case, so a fake declared in `commonMain` still reaches `commonTest`.

| Task                      | Owns                  | Writes to      |
|---------------------------|-----------------------|----------------|
| `faktGenerateCommonMain`  | `commonMain` fakes    | `commonTest`   |
| `faktGenerateDesktopMain` | `desktopMain` fakes   | `desktopTest`  |
| `faktGenerateServerMain`  | `serverMain` fakes    | `serverTest`   |

Before issue #160 there was no common producer, and `commonTest` failed with
`Unresolved reference 'fakeUserRepository'`.

## Layout

- `commonMain`: `UserRepository` (`@Fake`), `expect fun platformName()`, and `Greeter`.
- `desktopMain` / `serverMain`: the `actual fun platformName()` and one `@Fake` each
  (`WindowManager`, `RequestLog`).
- `commonTest`, `desktopTest`, `serverTest`: one test class each, using the fakes.

## Run

```bash
make test-kmp-all-jvm
# or
./gradlew allTests --continue
```
