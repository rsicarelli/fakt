# Sample Projects

Fakt includes working sample projects demonstrating different use cases.

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
cd fakt/samples/kmp-single-module
./gradlew build
```

---

## Next Steps

- [Multi-Module Usage](../multi-module/index.md) - Cross-module fakes
- [Contributing](../contributing.md) - Add your own samples
