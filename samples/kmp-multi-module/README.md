# KMP Multi-Module Sample

> **Industry-standard DDD architecture with Vertical Slices pattern**
> Demonstrates Fakt compiler plugin in a real-world multi-module KMP project

## 🎯 Purpose

This sample demonstrates:
- ✅ **DDD (Domain-Driven Design)** with vertical slices
- ✅ **Clean separation** between core infrastructure and business features
- ✅ **Cross-module fake generation** and testing
- ✅ **Industry-standard package naming** (`com.rsicarelli.fakt.samples.kmpmultimodule.*`)
- ✅ **Real-world dependency patterns** (features depend on core, app coordinates features)

## 🏗️ Architecture

```
kmp-multi-module/
├── core/                     # Infrastructure layer (technical concerns)
│   ├── auth/                 # Authentication & authorization
│   ├── storage/              # Persistence (key-value, cache, secure)
│   ├── network/              # HTTP, WebSocket, API clients
│   ├── logger/               # Structured logging
│   └── analytics/            # Analytics & performance monitoring
│
├── features/                 # Business features (Vertical Slices)
│   ├── login/                # Login feature (domain + use cases + repository)
│   ├── order/                # Order management
│   ├── profile/              # User profile management
│   ├── dashboard/            # Analytics dashboard
│   ├── notifications/        # Push notifications
│   └── settings/             # App settings
│
└── app/                      # Application coordinator
    └── AppCoordinator.kt     # Lightweight orchestration layer
```

### Dependency Flow

```
app → features/* → core/*

features/login → core/{auth, logger, storage, analytics}
features/order → core/{network, logger, storage, analytics}
features/profile → core/{auth, storage, logger}
features/dashboard → core/{analytics, logger}
features/notifications → core/{network, storage, logger}
features/settings → core/{storage, logger}
```

## 📦 Package Structure

All modules follow consistent naming:

```
com.rsicarelli.fakt.samples.kmpmultimodule.core.<module>
com.rsicarelli.fakt.samples.kmpmultimodule.features.<feature>
com.rsicarelli.fakt.samples.kmpmultimodule.app
```

**Example:**
- `com.rsicarelli.fakt.samples.kmpmultimodule.core.auth.AuthProvider`
- `com.rsicarelli.fakt.samples.kmpmultimodule.features.login.LoginUseCase`
- `com.rsicarelli.fakt.samples.kmpmultimodule.app.AppCoordinator`

## 🎨 Design Principles

### 1. Vertical Slices (Feature-Based Architecture)

Each feature is **self-contained** with its own:
- **Domain models** (e.g., `Order`, `User`, `LoginCredentials`)
- **Use cases** (business logic, e.g., `LoginUseCase`, `OrderUseCase`)
- **Repositories** (data access, e.g., `LoginRepository`, `OrderRepository`)
- **Validators** (input validation, e.g., `LoginValidator`)

**Benefits:**
- Features are isolated and can evolve independently
- Easy to add/remove features
- Clear ownership boundaries
- Reduces coupling between business features

### 2. Core Infrastructure Layer

Core modules provide **technical capabilities**:
- **No business logic** - pure infrastructure services
- **Stable interfaces** - changes infrequently
- **Shared across features** - reusable building blocks

**All core interfaces are `@Fake` annotated:**
- Enables cross-module testing
- Features use core fakes in their tests
- App uses all fakes for integration tests

### 3. Lightweight App Coordinator

The `app` module is intentionally minimal:
- **No business logic** - just orchestration
- **Depends on all features** - integrates them together
- **Handles navigation and app lifecycle** (in real apps)

## 🧪 Testing Strategy

### Cross-Module Fake Usage

**Core modules generate fakes that features use in tests:**

```kotlin
// features/login/src/commonTest/kotlin/.../LoginTest.kt
class LoginUseCaseTest {
    @Test
    fun `GIVEN LoginUseCase WHEN login THEN should authenticate`() = runTest {
        // Use fakes from core modules
        val authProvider = fakeAuthProvider { /* configure */ }
        val tokenStorage = fakeTokenStorage { /* configure */ }
        val logger = fakeLogger { /* configure */ }
        val analytics = fakeAnalytics { /* configure */ }

        val useCase = fakeLoginUseCase { /* configure */ }

        val result = useCase.login(
            credentials,
            authProvider,  // core/auth fake
            tokenStorage,   // core/auth fake
            logger,         // core/logger fake
            analytics       // core/analytics fake
        )

        // Assertions...
    }
}
```

**App module uses fakes from ALL modules:**

```kotlin
// app/src/commonTest/kotlin/.../AppCoordinatorTest.kt
class AppCoordinatorTest {
    @Test
    fun `GIVEN AppCoordinator WHEN initializing THEN should orchestrate`() = runTest {
        // Core fakes
        val authProvider = fakeAuthProvider {}
        val apiClient = fakeApiClient {}
        val storage = fakeKeyValueStorage {}
        val logger = fakeLogger {}
        val analytics = fakeAnalytics {}

        // Feature fakes
        val loginUseCase = fakeLoginUseCase {}
        val orderUseCase = fakeOrderUseCase {}
        val profileUseCase = fakeProfileUseCase {}
        val dashboardUseCase = fakeDashboardUseCase {}

        val coordinator = AppCoordinator(/* all dependencies */)
        coordinator.initialize()

        // Assertions...
    }
}
```

## 🚀 Building and Testing

### Build entire project
```bash
./gradlew build
```

### Test specific modules
```bash
./gradlew :core:logger:test
./gradlew :features:login:test
./gradlew :app:test
```

### Run all tests
```bash
./gradlew test
```

## 📚 Module Details

### Core Modules

| Module | Interfaces | Purpose |
|--------|-----------|---------|
| **core/auth** | `AuthProvider`, `TokenStorage`, `Authorizer` | Authentication, token management, permissions |
| **core/storage** | `KeyValueStorage`, `Cache`, `SecureStorage` | Data persistence |
| **core/network** | `HttpClient`, `ApiClient`, `WebSocketClient` | Network communication |
| **core/logger** | `Logger` | Structured logging |
| **core/analytics** | `Analytics`, `PerformanceMonitor` | Analytics and performance tracking |

### Feature Modules

| Module | Use Cases | Purpose |
|--------|-----------|---------|
| **features/login** | `LoginUseCase`, `LoginRepository`, `LoginValidator` | User authentication flow |
| **features/order** | `OrderUseCase`, `OrderRepository`, `OrderValidator` | E-commerce order management |
| **features/profile** | `ProfileUseCase` | User profile management |
| **features/dashboard** | `DashboardUseCase` | Analytics dashboard |
| **features/notifications** | `NotificationUseCase` | Push notifications |
| **features/settings** | `SettingsUseCase` | App settings and preferences |

## ⚙️ Build Configuration

### Convention Plugin (Zero Boilerplate!)

**Setup:** Add to root `build.gradle.kts`:
```kotlin
plugins {
    kotlin("multiplatform") version "2.3.20" apply false
    id("com.rsicarelli.fakt") version "1.0.0-alpha01" apply false
}
```

And in `settings.gradle.kts`:
```kotlin
pluginManagement {
    includeBuild("../..")
    includeBuild("../../build-logic")  // ← Include convention plugins
    // ...
}
```

All modules use the `fakt-sample` convention plugin, which automatically configures:
- ✅ **All KMP targets** (jvm, js, iOS, macOS, Linux, Windows, WASM)
- ✅ **JVM toolchain** (Java 21)
- ✅ **Test dependencies** (kotlin-test, coroutines-test)
- ✅ **KLIB duplicate handling** (centralized, no repetition!)
- ✅ **Runtime dependency** (automatic)

**Before (50+ lines per module):**
```kotlin
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt")
}

kotlin {
    jvmToolchain(21)
    applyDefaultHierarchyTemplate()
    jvm()
    js(IR) { browser(); nodejs() }
    // ... 10+ target declarations

    sourceSets {
        commonMain {
            dependencies {
                implementation("com.rsicarelli.fakt:annotations:1.0.0-alpha01")
            }
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.coroutines.test)
            }
        }
    }

    // KLIB duplicate handling (repeated in every module!)
    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xklib-duplicated-unique-name-strategy=allow-first-with-warning")
                }
            }
        }
    }
}

fakt {
    enabled.set(true)  // Default value, unnecessary
    debug.set(true)
}
```

**After (11 lines per module!):**
```kotlin
plugins {
    id("fakt-sample-kmp")           // ← Does everything!
    id("com.rsicarelli.fakt")
}

fakt {
    debug.set(true)
}
```

For modules with core dependencies, just add them:
```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.core.auth)
                implementation(projects.core.logger)
            }
        }
    }
}
```

## 🎓 Key Learnings

### ✅ What This Sample Demonstrates

1. **Real-world modularization** - Not a toy example
2. **Proper dependency management** - Unidirectional dependency flow
3. **Testable architecture** - Every layer can be tested in isolation
4. **Cross-module fake generation** - Core fakes used by features, feature fakes used by app
5. **Industry naming conventions** - Full package names with project namespace
6. **Convention plugins** - Eliminate boilerplate with shared configuration

### 🔄 Comparison with `multi-module` Sample

| Aspect | `multi-module` (old) | `kmp-multi-module` (new) |
|--------|---------------------|-------------------------|
| **Architecture** | Flat, unclear layers | DDD with vertical slices |
| **Packages** | `foundation`, `domain` | `com.rsicarelli.fakt.samples.kmpmultimodule.*` |
| **Organization** | Mixed concerns | Clear separation (core vs features) |
| **Features** | 3-4 simple examples | 6 realistic features |
| **Real-world** | Educational | Production-like |

## 📝 Notes

- All modules use KMP (JVM + JS targets)
- Fakt plugin enabled on all modules except `app`
- Tests follow GIVEN-WHEN-THEN pattern (BDD style)
- Uses vanilla JUnit5 + kotlin-test (no custom frameworks)

## 🎯 Next Steps

To extend this sample:

1. **Add more features** - Follow the vertical slice pattern
2. **Add feature-to-feature dependencies** - e.g., `features/order` depends on `features/login`
3. **Add -fakes modules** - Separate modules for generated fakes (see `multi-module` for pattern)
4. **Add shared models** - Create `shared/` module for cross-feature domain models
5. **Add platform-specific implementations** - Implement core interfaces for Android/iOS/JS

---

**Built with [Fakt](https://github.com/rsicarelli/fakt) - Type-safe test fakes for Kotlin**
