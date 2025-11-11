# Gemini Deep Research Prompt: Common Issues with Fake Testing Strategy

## Research Objective

Conduct a comprehensive, critical analysis of the challenges, pitfalls, and limitations of using fakes (instead of mocks) for testing. This research will ensure our documentation maintains **Metro-style transparency** by honestly addressing scenarios where fakes may not be the best choice, common mistakes developers make, and the maintenance burden of fake-based testing strategies.

**Philosophy**: We need to understand both sides of the argument. This research should provide ammunition for critics of our approach and help us address their concerns honestly in documentation.

---

## Core Research Questions

### 1. When Fakes Make Testing Harder

**Primary question**: In what specific scenarios does using fakes create more problems than using mocks?

**Sub-questions**:

#### 1.1 Complex Behavioral Logic
- When a fake requires implementing complex business logic, does it become a "mini production implementation"?
- At what point does maintaining a fake become more expensive than using a mock?
- Are there examples of fakes that became so complex they needed their own tests?
- What is the "fake implementation drift" problem? (When fakes diverge from real implementations)

**Scenarios to investigate**:
```kotlin
// Example: Complex stateful behavior
interface PaymentGateway {
    suspend fun charge(amount: Money): PaymentResult
    suspend fun refund(transactionId: String): RefundResult
    suspend fun getBalance(): Money
    suspend fun reconcile(): ReconciliationReport
    // 20+ more methods with complex state interactions
}
```

**Research questions**:
- How do teams handle faking complex, stateful APIs?
- What are the maintenance costs when the real API changes?
- When is it better to just mock the gateway?

#### 1.2 Third-Party APIs with Complex Contracts
- How do you fake a third-party API you don't control (Stripe, AWS, Firebase)?
- What happens when the third-party API has undocumented edge cases?
- Is it better to use "contract testing" (Pact) instead of fakes for third-party APIs?

**Sources to check**:
- Contract testing guides (Pact documentation)
- Articles on testing third-party integrations
- Developer discussions about faking vs mocking external APIs

#### 1.3 Legacy Code Without Clear Interfaces
- What if the production code doesn't have clean interfaces?
- Is refactoring to introduce interfaces always worth it?
- When is it pragmatic to just mock the concrete class?

#### 1.4 Time and Resource Constraints
- How long does it take to write a comprehensive fake vs configuring a mock?
- In startup/MVP scenarios, is the upfront cost of fakes justified?
- What is the "break-even point" where fake investment pays off?

---

### 2. The Maintenance Burden Problem

**Primary question**: What are the hidden costs of maintaining fake implementations over time?

**Sub-questions**:

#### 2.1 Fake Implementation Drift

**The problem**: Fakes can fall out of sync with real implementations.

**Research focus**:
- What is "fake drift"?
- How do teams detect when a fake no longer behaves like the real implementation?
- Are there tools or strategies to prevent fake drift?
- Should fakes be tested against the real implementation? (Integration test suites)

**Real-world scenarios**:
- A fake repository always returns success, but the real one can fail with 5 different error types
- A fake API doesn't validate input, but the real API throws exceptions
- A fake doesn't handle edge cases (nulls, empty strings, concurrent access)

#### 2.2 Update Cascade Problem

**The problem**: When an interface changes, ALL fakes need updating.

**Research questions**:
- How do large codebases handle updating 50+ fake implementations when an interface signature changes?
- Does automated fake generation solve this problem?
- What happens when you forget to update a fake?

#### 2.3 Test-Only Code Maintenance

**The problem**: Fakes live in test source sets, which may receive less scrutiny than production code.

**Research questions**:
- Do teams apply the same code review standards to fakes?
- Are fakes covered by linters, static analysis, and formatting tools?
- What happens when test-only code accumulates technical debt?

---

### 3. The "Fake Fidelity" Problem

**Primary question**: How realistic does a fake need to be, and how do you ensure fidelity?

**Sub-questions**:

#### 3.1 Realistic vs Simplified Fakes

**The spectrum**:
- **Overly simple**: Always returns `emptyList()` (useless for real scenarios)
- **Overly complex**: Reimplements 80% of production logic (high maintenance)
- **Just right**: Captures essential behavior without reimplementing internals

**Research questions**:
- What guidelines exist for determining fake complexity?
- How do you know if your fake is "realistic enough"?
- Should fakes validate inputs like production code?

**Example debate**:
```kotlin
// Option 1: Overly simple
class FakeUserRepository : UserRepository {
    override suspend fun getUser(id: String): User = User("test", "Test User")
}

// Option 2: Realistic
class FakeUserRepository : UserRepository {
    private val users = mutableMapOf<String, User>()

    override suspend fun getUser(id: String): User {
        return users[id] ?: throw UserNotFoundException(id)  // Realistic error
    }
}

// Option 3: Overly complex
class FakeUserRepository : UserRepository {
    private val db = InMemoryDatabase()
    private val cache = LRUCache<String, User>()
    private val validator = UserValidator()
    // ... 200 lines of fake implementation
}
```

Which is the right level of fidelity?

#### 3.2 Edge Cases and Error Conditions

**Research questions**:
- Should fakes throw exceptions like real implementations?
- How do you fake network errors, timeouts, and retries?
- Do fakes need to handle concurrent access?

**Sources to check**:
- Testing guidelines from Google, Microsoft, Martin Fowler
- Discussions about fake implementation standards

---

### 4. Learning Curve and Developer Experience

**Primary question**: What are the cognitive and learning challenges of adopting a fake-based testing strategy?

**Sub-questions**:

#### 4.1 Mental Model Shift

**Research questions**:
- How difficult is it for developers coming from a mock-heavy background to adopt fakes?
- What misconceptions do developers have when starting with fakes?
- How long does it take for a team to become productive with fake-based testing?

#### 4.2 Upfront Effort vs Long-Term Benefits

**Research questions**:
- What is the typical upfront cost (time, lines of code) to establish a fake-based strategy?
- How do you convince stakeholders to invest in this upfront cost?
- Are there metrics showing the ROI of fakes vs mocks?

#### 4.3 Tooling and IDE Support

**Research questions**:
- Do IDEs provide good support for navigating between interfaces and fakes?
- Are there refactoring tools that automatically update fakes when interfaces change?
- What is the developer experience of manually creating fakes vs auto-generated fakes?

---

### 5. The "Over-Testing" Problem

**Primary question**: Can fakes lead to over-testing or testing the wrong things?

**Sub-questions**:

#### 5.1 Testing Fake Implementation Instead of Production Code

**The anti-pattern**:
```kotlin
@Test
fun `test that fake returns what I configured it to return`() {
    val fake = FakeRepository()
    fake.setReturnValue(user)

    val result = fake.getUser("123")

    assertEquals(user, result)  // ❌ Testing the fake, not the real code
}
```

**Research questions**:
- How common is this anti-pattern?
- What are signs that tests are testing fakes instead of business logic?
- How do teams prevent this mistake?

#### 5.2 False Confidence from Oversimplified Fakes

**The problem**: Tests pass with simple fakes but fail in production.

**Research questions**:
- How often do bugs slip through because fakes don't match real behavior?
- What strategies ensure fakes stay synchronized with production implementations?
- Should fakes be validated with integration tests against real implementations?

---

### 6. When Mocks Are Actually Better

**Primary question**: What are the concrete scenarios where behavior verification with mocks is superior to state-based testing with fakes?

**Sub-questions**:

#### 6.1 Side Effects Without Observable State

**Examples**:
- Logging (no state to inspect)
- Analytics events (fire-and-forget)
- Metrics collection
- Audit trails
- Notifications

**Research questions**:
- For these scenarios, is there any benefit to using fakes?
- What does the "fakes over mocks" community recommend for side effects?

#### 6.2 Interaction Timing and Ordering

**Examples**:
- Verifying a method is called exactly once
- Verifying methods are called in a specific order
- Verifying a method is NOT called under certain conditions

**Research questions**:
- Can fakes handle ordering verification effectively?
- Do fakes need to track call counts and order?
- Is this a valid use case for mocks even in a fake-heavy codebase?

#### 6.3 Rapid Prototyping and Exploratory Testing

**Research questions**:
- In early development when interfaces are unstable, are mocks faster?
- Is it better to start with mocks and refactor to fakes later?
- What is the cost of this "mock first, fake later" approach?

---

### 7. Organizational and Team Challenges

**Primary question**: What are the organizational challenges of adopting a fake-based testing strategy?

**Sub-questions**:

#### 7.1 Team Consensus and Buy-In

**Research questions**:
- How do you get an entire team to adopt fake-based testing?
- What resistance do developers have to moving away from mocks?
- Are there case studies of teams that tried and failed to adopt fakes?

#### 7.2 Onboarding New Team Members

**Research questions**:
- How long does it take to onboard developers unfamiliar with fakes?
- What documentation or training is needed?
- Do junior developers struggle more with fakes than mocks?

#### 7.3 Consistency Across Codebase

**Research questions**:
- How do teams enforce consistent fake implementation patterns?
- What happens when half the codebase uses fakes and half uses mocks?
- Are there linting rules or architecture tests to enforce fake usage?

---

### 8. Performance and Test Speed

**Primary question**: Do fakes introduce any performance problems in test suites?

**Sub-questions**:

#### 8.1 Fake Instantiation Overhead

**Research questions**:
- Do complex fakes with significant setup logic slow down tests?
- How does fake instantiation compare to mock instantiation performance?
- Are there scenarios where mocks are faster than fakes?

#### 8.2 In-Memory State Management

**Research questions**:
- For fakes that maintain in-memory databases or complex state, what is the memory footprint?
- Can fake state leak between tests if not properly isolated?
- What are best practices for resetting fake state between tests?

---

### 9. Specific Kotlin/KMP Challenges with Fakes

**Primary question**: Are there Kotlin or KMP-specific challenges that make fakes more difficult to implement or maintain?

**Sub-questions**:

#### 9.1 Coroutine and Flow Complexity

**Research questions**:
- How difficult is it to write fakes that correctly implement Flow behavior?
- Do fakes need to handle `flowOn`, `catch`, `retry`, and other Flow operators?
- What are common mistakes when faking suspend functions?

**Example challenge**:
```kotlin
interface DataSource {
    fun observe(): Flow<List<Item>>  // Hot or cold flow? Shared or not?
    suspend fun refresh()           // Does this emit to the flow?
    fun stop()                       // Does this complete the flow?
}
```

How realistic does the fake need to be?

#### 9.2 Platform-Specific Behavior

**Research questions**:
- How do you fake platform-specific APIs (Android Context, iOS UIKit)?
- Do fakes need to behave differently on different KMP targets?
- What is the maintenance cost of platform-specific fake logic?

#### 9.3 Multiplatform Fake Distribution

**Research questions**:
- How do teams share fakes across modules in KMP projects?
- Should fakes live in `commonTest` or `jvmTest`/`iosTest`?
- What are the challenges of distributing test-only artifacts?

---

### 10. Alternative Approaches and Hybrid Strategies

**Primary question**: What alternative testing strategies exist, and when should they be used instead of or alongside fakes?

**Sub-questions**:

#### 10.1 Contract Testing (Pact, Spring Cloud Contract)

**Research questions**:
- How does contract testing compare to fakes for third-party APIs?
- Can contract testing and fakes be used together?
- What are the trade-offs?

#### 10.2 Stub Servers (WireMock, MockServer)

**Research questions**:
- When are stub servers (HTTP mocking) better than fakes?
- What is the performance cost of stub servers vs in-memory fakes?

#### 10.3 Hybrid Approach (Fakes + Mocks)

**Research questions**:
- Is it pragmatic to use fakes for some dependencies and mocks for others?
- What guidelines determine when to use which approach?
- How do teams document their testing strategy?

**Example hybrid pattern**:
```kotlin
@Test
fun `test checkout flow`() = runTest {
    // Fakes for state management
    val cart = fakeShoppingCart()
    val inventory = fakeInventory()

    // Mocks for side effects
    val analytics = mockk<Analytics>(relaxed = true)
    val emailService = mockk<EmailService>()

    // Test
    checkout(cart, inventory, analytics, emailService)

    // Assert state
    assertTrue(cart.isEmpty())

    // Verify interactions
    verify { analytics.track("checkout_complete") }
    verify { emailService.sendReceipt(any()) }
}
```

---

## Output Format Requirements

### Structure the research report with:

1. **Executive Summary** (500 words)
   - Key challenges and limitations of fake-based testing
   - Critical scenarios where mocks are better
   - Balanced assessment of trade-offs

2. **Problem Categories** (10 sections as above)
   - Each challenge explained with examples
   - Real-world developer testimonials
   - Mitigation strategies where available

3. **Critical Analysis Table**

| Challenge | Severity | Frequency | Mitigation |
|-----------|----------|-----------|------------|
| Fake drift | High | Common | Integration tests against real impl |
| ... | ... | ... | ... |

4. **When NOT to Use Fakes** (Decision Guide)
   - Clear criteria for choosing mocks over fakes
   - Flowchart or decision tree
   - Concrete examples

5. **Developer Testimonials: The Dark Side**
   - Quotes from developers who struggled with fakes
   - Stories of failed attempts to adopt fake-based testing
   - Lessons learned from production incidents

6. **Mitigation Strategies**
   - How to prevent fake drift
   - How to maintain fake quality
   - Tooling and automation recommendations

7. **Counterarguments to "Fakes Over Mocks"**
   - What do critics say?
   - Are there thought leaders who disagree?
   - What are their strongest arguments?

8. **Recommended Hybrid Strategies**
   - When to mix fakes and mocks
   - How to document the strategy
   - Team agreement templates

---

## Critical Success Factors

### This research MUST:

✅ **Find real failures**: Stories of teams that adopted fakes and struggled
✅ **Identify anti-patterns**: Common mistakes developers make with fakes
✅ **Provide balanced criticism**: Honest assessment of when fakes are worse than mocks
✅ **Offer mitigation strategies**: How to avoid the pitfalls
✅ **Include dissenting opinions**: Thought leaders who prefer mocks

### This research should AVOID:

❌ Defending fakes at all costs
❌ Dismissing legitimate criticisms
❌ Presenting fakes as a silver bullet
❌ Ignoring the maintenance burden

---

## Sources to Consult

### Critical Perspectives
- Martin Fowler: "Mocks Aren't Stubs" (updated opinions)
- Uncle Bob: Testing strategies in Clean Architecture
- Kent Beck: TDD patterns (does he prefer fakes or mocks?)

### Developer Communities
- **Stack Overflow**: Questions about fake implementation challenges
- **Reddit**: r/Kotlin, r/androiddev, r/programming
  - Search: "fake vs mock problems"
  - Search: "fake implementation issues"
- **Medium**: Critical articles about fake-based testing

### Real-World Case Studies
- Post-mortems of production bugs that passed fake-based tests
- "Why we moved back to mocks" articles
- "Lessons learned from adopting fakes" retrospectives

### Testing Best Practices
- Google Testing Blog: Any articles critical of fakes?
- Microsoft Testing Guidelines
- ThoughtWorks Technology Radar: What do they say about test doubles?

---

## Verification and Citation Standards

For every challenge:

1. **Cite real examples** (GitHub issues, blog posts, conference talks)
2. **Quote developers** who experienced the problem
3. **Rate severity**:
   - 🔴 Critical (common and severe)
   - 🟡 Moderate (common but manageable)
   - 🟢 Minor (rare or easily mitigated)

4. **Distinguish between**:
   - Fundamental limitations (can't be fixed)
   - Implementation mistakes (can be avoided)
   - Tooling gaps (can be solved with better tools)

---

## Research Focus Areas (Priority Order)

### 1. CRITICAL (Must Research Thoroughly)
- Fake drift problem and mitigation strategies
- When mocks are objectively better
- Maintenance burden and update cascade
- Learning curve and adoption challenges

### 2. HIGH (Should Research)
- Over-testing and false confidence
- Performance and test speed concerns
- Platform-specific fake challenges (KMP)
- Hybrid strategies (fakes + mocks)

### 3. MEDIUM (Nice to Have)
- Organizational challenges
- Tooling and IDE support gaps
- Alternative approaches (contract testing, stub servers)
- Developer testimonials and war stories

---

## Questions to Answer Post-Research

After receiving Gemini's report, evaluate:

1. What is the single biggest challenge with fake-based testing?
2. In what percentage of scenarios are mocks actually better?
3. What are the 3-5 most common mistakes developers make with fakes?
4. How do we address these challenges in our documentation?
5. What features should Fakt have to mitigate these issues?
6. What honest disclaimers should we include in our marketing?

---

## How to Use This Research in Documentation

### For `docs/reference/fakes-over-mocks.md`:

Add a section: **"When Fakes Aren't the Answer"**
- List scenarios where mocks are better
- Provide clear decision criteria
- Show hybrid approach examples

Add a section: **"Common Pitfalls and How to Avoid Them"**
- Fake drift problem and solutions
- Over-testing anti-patterns
- Maintenance strategies

Add a section: **"FAQ: Challenges"**
- "What if my fake gets out of sync?"
- "How do I test side effects with fakes?"
- "When should I just use a mock?"

### For `docs/guides/testing-patterns.md`:

Add patterns for:
- Maintaining fake fidelity
- Preventing fake drift
- Hybrid fake/mock strategies
- Testing fakes themselves (meta-testing)

### For `docs/troubleshooting.md`:

Add entries for:
- "My fake implementation is too complex"
- "Fake drift causing production bugs"
- "Tests pass but production fails"

---

## Success Criteria

This research is successful if it:

✅ Provides honest, balanced criticism of fake-based testing
✅ Identifies real-world failures and challenges
✅ Offers practical mitigation strategies
✅ Helps us write transparent, trustworthy documentation
✅ Prepares us to answer skeptics' questions
✅ Identifies features Fakt needs to address these issues

**Goal**: Understand the weaknesses of our approach so we can address them honestly in documentation and product features. **Transparency builds trust.**

---

## Expected Output from Gemini

- **Report length**: 12,000-15,000 words
- **Sources**: 40-60 critical analyses, developer testimonials, case studies
- **Sections**: All 10 problem categories addressed
- **Tables**: Challenge severity/frequency matrix
- **Quotes**: 10-20 developer testimonials about struggles
- **Decision guides**: When to use fakes vs mocks vs hybrid

---

## Final Note: Metro-Inspired Transparency

This research embodies the Metro documentation philosophy:

> **"Be honest about limitations. Developers respect transparency more than marketing spin."**

By understanding and documenting the challenges of fake-based testing, we build credibility and trust. We're not claiming fakes are perfect—we're claiming they're the right choice for most Kotlin testing scenarios, while honestly acknowledging where they fall short.
