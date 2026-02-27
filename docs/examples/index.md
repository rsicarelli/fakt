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

## jvm-test-fixtures

**Location**: [`samples/jvm-test-fixtures/`](https://github.com/rsicarelli/fakt/tree/main/samples/jvm-test-fixtures)

**Demonstrates**: Cross-module fake sharing via Gradle's `java-test-fixtures` plugin

**Structure**:

- `core/` — Producer with `@Fake` interface, abstract class, and open class
- `app/` — Consumer using fakes from core's testFixtures

**Key Examples**:

- `useGradleTestFixtures` plugin configuration
- Cross-module fake consumption via `testFixtures(projects.core)`
- No collector module or Fakt plugin needed in consumer
- Interface, abstract class, and open class support

**Best for**: JVM multi-module projects wanting simple cross-module fake sharing

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
- Mutable fakes with mid-test reconfiguration
- Mutable + call history composition

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

## kmp-multi-target

**Location**: [`samples/kmp-multi-target/`](https://github.com/rsicarelli/fakt/tree/main/samples/kmp-multi-target)

**Demonstrates**: KMP source set hierarchy validation and platform-specific fake generation

**Structure**:

- 6 platform-specific `@Fake` interfaces (one per source set)
- 11 test classes (6 basic + 5 hierarchy validation)
- 45+ test cases validating fake generation and hierarchy

**Key Validations**:

- ✅ Platform-specific fake generation (jvmMain → jvmTest, iosMain → iosTest)
- ✅ Vertical hierarchy (child source sets access parent fakes)
- ✅ Multi-level inheritance (iOS accesses commonMain + nativeMain + iosMain)
- ✅ Horizontal isolation (jvmTest cannot access iosFakes)

**Source Sets Tested**:

- `commonMain` → `commonTest` (base for all platforms)
- `jvmMain` → `jvmTest` (JVM-specific)
- `iosMain` → `iosTest` (iOS-specific, inherits from nativeMain)
- `jsMain` → `jsTest` (JavaScript-specific)
- `wasmJsMain` → `wasmJsTest` (WebAssembly-specific)
- `nativeMain` → `nativeTest` (shared across all native targets)

**Best for**: Validating Fakt's source set mapping and KMP hierarchy support

---

## fake-publishing

**Location**: [`samples/fake-publishing/`](https://github.com/rsicarelli/fakt/tree/main/samples/fake-publishing)

**Demonstrates**: Publishing Fakt-generated fakes as Maven artifacts for external consumption

**Structure**:

- `kmp-publisher/` - Library project that publishes fakes
- `kmp-consumer/` - External project that consumes published fakes

**Key Examples**:

- Publishing collector modules to Maven Local
- Consuming pre-generated fakes from Maven artifacts
- Cross-project fake usage without Fakt plugin
- Interface, abstract class, and open class support

**Best for**: Library authors who want to ship pre-generated fakes alongside their APIs

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

# JVM test fixtures multi-module
cd fakt/samples/jvm-test-fixtures
./gradlew build

# KMP samples
cd fakt/samples/kmp-single-module
./gradlew build

# KMP hierarchy validation
cd fakt/samples/kmp-multi-target
./gradlew allTests

# Fake publishing sample (requires sequential steps)
cd fakt/samples/fake-publishing/kmp-publisher
./gradlew publishToMavenLocal

cd fakt/samples/fake-publishing/kmp-consumer
./gradlew build
```

---

## Next Steps

- [Multi-Module Usage](../user-guide/multi-module.md) - Cross-module fakes
