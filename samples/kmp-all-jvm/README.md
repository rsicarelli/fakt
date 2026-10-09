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
| `faktGenerateCliMain`     | `cliMain` fakes       | `cliTest`      |
| `faktGenerateDesktopAndServerMain` | `desktopAndServerMain` fakes (#162) | `desktopAndServerTest` |

`desktopAndServerMain` is an intermediate source set: `desktopMain` and `serverMain` depend on it,
it depends on `commonMain`, and `cliMain` does not see it. Kotlin builds no compilation for it
either, so Fakt adds a synthetic producer on a JVM representative target.

Before issue #160 there was no common producer, and `commonTest` failed with
`Unresolved reference 'fakeUserRepository'`.

## Layout

- `commonMain`: `UserRepository` (`@Fake`), `expect fun platformName()`, and `Greeter`.
- `desktopMain` / `serverMain`: the `actual fun platformName()` and one `@Fake` each
  (`WindowManager`, `RequestLog`).
- `cliMain`: the `actual fun platformName()` and `actual fun transport()` for the `cli` target.
- `desktopAndServerMain`: `SessionStore` (`@Fake`) and the `actual fun transport()` for desktop
  and server.
- `commonTest`, `desktopTest`, `serverTest`, `cliTest`, `desktopAndServerTest`: tests using the fakes.
- `serverIntegrationTest`: a custom test compilation of the `server` target, associated with
  `main` late (`afterEvaluate`). It uses `fakeRequestLog()` because generated fakes reach a test
  compilation through its association, not through its name (#164b).

## Run

```bash
make test-kmp-all-jvm
# or
./gradlew allTests --continue
```
