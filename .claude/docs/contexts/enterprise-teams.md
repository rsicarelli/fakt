# Context: Enterprise Teams Using KtFakes

> **Target Audience**: Enterprise development teams and large-scale Kotlin projects
> **Scope**: Multi-module, multi-team, production-scale usage
> **Testing Standard**: [📋 Testing Guidelines](../validation/testing-guidelines.md)

## 🏢 **Enterprise Profile**

### **Who This Is For**
- **Enterprise development teams** (10+ developers)
- **Multi-module projects** with complex dependencies
- **Production applications** requiring high reliability
- **Teams** needing consistent testing standards across modules

### **Enterprise Challenges Addressed**
- **Standardized testing** across teams and modules
- **Type-safe mocking** at scale
- **Build performance** with large codebases
- **Team onboarding** and knowledge sharing
- **Quality gates** and CI/CD integration

## 🏗️ **Multi-Module Architecture Patterns**

### **Module Structure Example**
```
enterprise-app/
├── shared/
│   ├── domain/              # Core business entities
│   ├── network/             # API clients and DTOs
│   └── database/            # Data access interfaces
├── features/
│   ├── user-management/     # User feature module
│   ├── payment-processing/  # Payment feature module
│   └── analytics/           # Analytics feature module
├── platform/
│   ├── android/             # Android app
│   ├── ios/                 # iOS app (future)
│   └── backend/             # Backend services
└── testing/
    ├── test-fixtures/       # Shared test data
    ├── integration-tests/   # Cross-module tests
    └── performance-tests/   # Load testing
```

### **Shared Interface Definition**
```kotlin
// shared/network/src/commonMain/kotlin/UserApiClient.kt
@Fake
interface UserApiClient {
    suspend fun getUser(id: String): Result<UserDto>
    suspend fun updateUser(user: UserUpdateRequest): Result<UserDto>
    suspend fun searchUsers(query: String, limit: Int): Result<List<UserDto>>
}

// shared/database/src/commonMain/kotlin/UserRepository.kt
@Fake
interface UserRepository {
    suspend fun findById(id: String): User?
    suspend fun save(user: User): User
    suspend fun findByEmail(email: String): User?
    suspend fun delete(id: String): Boolean
}
```

### **Feature Module Testing**
```kotlin
// features/user-management/src/jvmTest/kotlin/UserServiceTest.kt
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserServiceTest {

    @Test
    fun `GIVEN user service WHEN creating user THEN should coordinate API and database`() = runTest {
        // Given - Enterprise-scale fake coordination
        val savedUsers = mutableListOf<User>()
        val apiCalls = mutableListOf<String>()

        val userRepository = fakeUserRepository {
            findByEmail { email -> savedUsers.find { it.email == email } }
            save { user -> savedUsers.add(user); user }
        }

        val userApiClient = fakeUserApiClient {
            updateUser { request ->
                apiCalls.add("updateUser(${request.id})")
                Result.success(UserDto(request.id, request.name, request.email))
            }
        }

        val userService = UserService(userRepository, userApiClient)

        // When - Complex business operation
        val result = userService.createUser(
            CreateUserRequest("john@enterprise.com", "John Doe")
        )

        // Then - Verify coordination
        assertTrue(result.isSuccess)
        assertEquals(1, savedUsers.size)
        assertEquals("john@enterprise.com", savedUsers[0].email)
        assertEquals(1, apiCalls.size)
    }
}
```

## 📊 **Enterprise Testing Standards**

### **Consistent Testing Patterns**
```kotlin
// Establish enterprise-wide testing base class
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class EnterpriseTestBase {

    // Standard test naming: GIVEN-WHEN-THEN
    protected fun testName(given: String, whenAction: String, then: String): String {
        return "GIVEN $given WHEN $whenAction THEN $then"
    }

    // Standard fake configuration patterns
    protected fun createStandardUserRepository(): UserRepository {
        val users = mutableMapOf<String, User>()
        return fakeUserRepository {
            findById { id -> users[id] }
            save { user -> users[user.id] = user; user }
            findByEmail { email -> users.values.find { it.email == email } }
            delete { id -> users.remove(id) != null }
        }
    }

    protected fun createStandardApiClient(): UserApiClient {
        return fakeUserApiClient {
            getUser { id -> Result.success(UserDto(id, "Test User", "test@enterprise.com")) }
            updateUser { request -> Result.success(UserDto(request.id, request.name, request.email)) }
            searchUsers { query, limit -> Result.success(emptyList()) }
        }
    }
}
```

### **Team-Specific Test Extensions**
```kotlin
// features/user-management/src/jvmTest/kotlin/UserManagementTestBase.kt
abstract class UserManagementTestBase : EnterpriseTestBase() {

    protected fun createUserManagementScenario(): UserManagementScenario {
        return UserManagementScenario(
            userRepository = createStandardUserRepository(),
            userApiClient = createStandardApiClient(),
            auditLogger = createStandardAuditLogger()
        )
    }

    data class UserManagementScenario(
        val userRepository: UserRepository,
        val userApiClient: UserApiClient,
        val auditLogger: AuditLogger
    )
}
```

## 🔄 **CI/CD Integration Patterns**

### **Build Pipeline Configuration**
```yaml
# .github/workflows/enterprise-ci.yml
name: Enterprise CI Pipeline

on:
  pull_request:
    branches: [main, develop]
  push:
    branches: [main]

jobs:
  compile-check:
    name: Compilation Validation
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Setup JDK
        uses: actions/setup-java@v3
        with:
          java-version: '21'
      - name: Validate KtFakes Generation
        run: |
          ./gradlew clean
          ./gradlew compileKotlin compileTestKotlin
          # Verify generated fakes compile
          find . -path "*/build/generated/ktfake/test/kotlin/*" -name "*.kt" | wc -l

  test-execution:
    name: Test Execution
    runs-on: ubuntu-latest
    needs: compile-check
    strategy:
      matrix:
        module: [shared, user-management, payment-processing, analytics]
    steps:
      - uses: actions/checkout@v3
      - name: Run Module Tests
        run: ./gradlew :${{ matrix.module }}:test

  integration-tests:
    name: Integration Tests
    runs-on: ubuntu-latest
    needs: test-execution
    steps:
      - uses: actions/checkout@v3
      - name: Run Integration Tests
        run: ./gradlew :integration-tests:test
```

### **Quality Gates**
```kotlin
// testing/quality-gates/src/test/kotlin/QualityGateTest.kt
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QualityGateTest {

    @Test
    fun `GIVEN all modules WHEN generating fakes THEN should maintain type safety`() = runTest {
        // Quality gate: All generated fakes must compile
        val generatedFiles = findAllGeneratedFakes()
        assertTrue(generatedFiles.isNotEmpty(), "No generated fakes found")

        generatedFiles.forEach { file ->
            assertTrue(file.exists(), "Generated file missing: ${file.path}")
            assertFalse(containsTodoStatements(file), "TODO statements found in ${file.path}")
        }
    }

    @Test
    fun `GIVEN enterprise test suite WHEN running tests THEN should follow naming conventions`() = runTest {
        // Quality gate: All tests must follow GIVEN-WHEN-THEN naming
        val testFiles = findAllTestFiles()
        testFiles.forEach { file ->
            val testMethods = extractTestMethods(file)
            testMethods.forEach { method ->
                assertTrue(
                    method.name.matches(Regex("GIVEN .* WHEN .* THEN .*")),
                    "Test method doesn't follow GIVEN-WHEN-THEN: ${method.name}"
                )
            }
        }
    }
}
```

## 📈 **Performance and Scale Considerations**

### **Build Performance Optimization**
```kotlin
// gradle.properties (enterprise configuration)
org.gradle.parallel=true
org.gradle.configureondemand=true
org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=1g -XX:+UseG1GC

# KtFakes-specific optimizations
ktfakes.parallel.generation=true
ktfakes.cache.enabled=true
ktfakes.incremental.compilation=true
```

### **Large-Scale Testing Strategy**
```kotlin
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LargeScaleIntegrationTest {

    @Test
    fun `GIVEN enterprise system WHEN processing high volume THEN should maintain performance`() = runTest {
        // Given - Large-scale fake coordination
        val userRepository = createHighCapacityUserRepository()
        val paymentProcessor = createHighVolumePaymentProcessor()
        val analyticsService = createHighThroughputAnalytics()

        val enterpriseService = EnterpriseService(
            userRepository, paymentProcessor, analyticsService
        )

        // When - High volume simulation
        val operations = (1..1000).map { index ->
            async {
                enterpriseService.processUserOperation(
                    userId = "user_$index",
                    operation = "process_payment",
                    amount = 100.0 + index
                )
            }
        }

        val results = operations.awaitAll()

        // Then - Performance verification
        assertTrue(results.all { it.isSuccess })
        assertTrue(measureExecutionTime() < 5000) // Under 5 seconds
    }

    private fun createHighCapacityUserRepository(): UserRepository {
        val users = ConcurrentHashMap<String, User>()
        return fakeUserRepository {
            findById { id -> users[id] }
            save { user -> users[user.id] = user; user }
            // Optimized for concurrent access
        }
    }
}
```

## 🔧 **Enterprise Development Workflow**

### **Team Onboarding Process**
```markdown
# Enterprise KtFakes Onboarding Checklist

## Week 1: Foundation
- [ ] Read [Quick Start Demo](../examples/quick-start-demo.md)
- [ ] Complete [Working Examples](../examples/working-examples.md)
- [ ] Study [Testing Guidelines](../validation/testing-guidelines.md)
- [ ] Review enterprise testing base classes

## Week 2: Module Integration
- [ ] Understand multi-module architecture
- [ ] Practice with shared interface faking
- [ ] Learn enterprise testing patterns
- [ ] Implement first feature tests

## Week 3: Advanced Patterns
- [ ] Complex integration scenarios
- [ ] Performance testing patterns
- [ ] CI/CD integration understanding
- [ ] Quality gate implementation

## Week 4: Team Contribution
- [ ] Mentor new team members
- [ ] Contribute to testing standards
- [ ] Optimize build performance
- [ ] Documentation improvements
```

### **Code Review Standards**
```kotlin
// Example of enterprise code review criteria
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodeReviewExampleTest {

    @Test
    fun `GIVEN feature implementation WHEN reviewing code THEN should meet enterprise standards`() = runTest {
        // ✅ GOOD: Clear test structure
        // ✅ GOOD: GIVEN-WHEN-THEN naming
        // ✅ GOOD: Specific assertions
        // ✅ GOOD: Type-safe fake usage

        val service = fakeEnterpriseService {
            processData { data ->
                // ✅ GOOD: Business logic in implementation, not fake
                ProcessedData(data.id, "processed")
            }
        }

        val result = service.processData(TestData("123"))
        assertEquals("processed", result.status)
    }

    @Test
    fun `GIVEN code review WHEN checking fake usage THEN should avoid anti-patterns`() = runTest {
        // ❌ BAD: Complex business logic in fake
        // ❌ BAD: Shared mutable state
        // ❌ BAD: Non-descriptive test names
        // ❌ BAD: Missing type safety

        // This test documents what NOT to do
        assertTrue(true) // Placeholder for anti-pattern examples
    }
}
```

## 📚 **Enterprise Training Materials**

### **Workshop Structure**
1. **Foundation Workshop** (4 hours)
   - KtFakes overview and benefits
   - Basic fake generation patterns
   - Testing standards adoption

2. **Integration Workshop** (4 hours)
   - Multi-module patterns
   - Enterprise testing base classes
   - CI/CD integration

3. **Advanced Workshop** (4 hours)
   - Performance optimization
   - Quality gates implementation
   - Team coordination patterns

### **Certification Criteria**
- Demonstrate GIVEN-WHEN-THEN test writing
- Implement multi-module fake coordination
- Contribute to enterprise testing standards
- Pass quality gate validations

## 🔗 **Enterprise Resources**

### **Internal Documentation Templates**
- **[📋 Module Testing Standards]** - Based on testing guidelines
- **[📋 Fake Generation Patterns]** - Enterprise-specific patterns
- **[📋 CI/CD Integration Guide]** - Build pipeline integration
- **[📋 Performance Benchmarks]** - Scale testing approaches

### **Related Documentation**
- **[📋 Testing Guidelines](../validation/testing-guidelines.md)** - THE ABSOLUTE STANDARD
- **[📋 Working Examples](../examples/working-examples.md)** - Practical patterns
- **[📋 Multi-Interface Projects](../patterns/multi-interface-projects.md)** - Enterprise scenarios
- **[📋 Common Issues](../troubleshooting/common-issues.md)** - Problem solving at scale

---

**Enterprise teams using KtFakes benefit from consistent testing standards, type-safe mocking at scale, and seamless CI/CD integration across large, multi-module Kotlin projects.**