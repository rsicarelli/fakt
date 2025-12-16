# Sample Projects

Fakt includes working sample projects demonstrating different use cases.

---

## jvm-single-module

**Location**: [`samples/jvm-single-module/`](https://github.com/rsicarelli/fakt/tree/main/samples/jvm-single-module)

**Demonstrates**: JVM-only projects with standard kotlin-jvm plugin

**Targets**: JVM only

**Key Examples**:

- UserRepository (CRUD with call tracking)
- AuthenticationService (suspend functions + Result types)
- PropertyAndMethodInterface (properties + methods)
- No platform-specific setup required

**Best for**: Learning Fakt basics without KMP complexity

---

## android-single-module

**Location**: [`samples/android-single-module/`](https://github.com/rsicarelli/fakt/tree/main/samples/android-single-module)

**Demonstrates**: Android Library projects with AGP integration

**Targets**: Android (compileSdk=35, minSdk=24)

**Key Examples**:

- Same scenarios as jvm-single-module for consistency
- Unit tests in `src/test/kotlin`
- Instrumented tests support in `src/androidTest/kotlin`
- Works with Android Gradle Plugin 8.12.3+

**Best for**: Android developers wanting test fakes without KMP

---

## kmp-single-module

**Location**: [`samples/kmp-single-module/`](https://github.com/rsicarelli/fakt/tree/main/samples/kmp-single-module)

**Demonstrates**: Basic KMP usage with single module

**Targets**: JVM, iOS, Android, JS, Native

**Key Examples**:

- Simple interfaces with suspend functions
- Property fakes (val/var)
- Generic interfaces
- Call tracking with StateFlow

**Best for**: Learning Fakt basics and KMP setup

---

## kmp-multi-module

**Location**: [`samples/kmp-multi-module/`](https://github.com/rsicarelli/fakt/tree/main/samples/kmp-multi-module)

**Demonstrates**: Advanced multi-module architecture with dedicated fake modules

**Structure**:

- 11 producer modules with `@Fake` interfaces
- 11 dedicated `-fakes` collector modules
- 1 consumer app module using all fakes

**Key Examples**:

- Cross-module fake consumption (experimental)
- Gradle project references with version catalogs
- Large-scale KMP project patterns
- Fake module organization

**Best for**: Understanding multi-module setups and scaling Fakt

---

## Running Samples

Clone the repository and build:

```bash
git clone https://github.com/rsicarelli/fakt.git

# JVM-only sample
cd fakt/samples/jvm-single-module
./gradlew build

# Android sample
cd fakt/samples/android-single-module
./gradlew build

# KMP samples
cd fakt/samples/kmp-single-module
./gradlew build
```

---

## Next Steps

- [Multi-Module Usage](../user-guide/multi-module.md) - Cross-module fakes
