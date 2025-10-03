# Testing

This section covers testing guidelines, patterns, and status for the Fakt compiler plugin.

## Overview

Fakt follows **strict BDD-style testing** with GIVEN-WHEN-THEN patterns using vanilla JUnit5. No custom matchers, no mocks - only fakes and clear test structure.

## Documentation in this Section

### Guidelines

- **[TESTING_GUIDELINES](TESTING_GUIDELINES.html)** - THE ABSOLUTE STANDARD ⭐
  - GIVEN-WHEN-THEN naming convention (mandatory)
  - Vanilla JUnit5 + kotlin-test only
  - Isolated instances per test
  - No @BeforeEach/@AfterEach hooks
  - Fakes instead of mocks

### Status & Coverage

- **[TESTING_STATUS_REPORT](TESTING_STATUS_REPORT.html)** - Current testing metrics
  - Test coverage statistics
  - Module-by-module test status
  - Integration test results

- **[TEST_COVERAGE_ANALYSIS](TEST_COVERAGE_ANALYSIS.html)** - Coverage deep-dive
  - Unit test coverage
  - Integration test scenarios
  - Missing test cases
  - Improvement recommendations

## Testing Principles

**BDD Style**: `GIVEN interface with generics WHEN generating fake THEN should handle correctly`
**Isolated**: Each test creates its own instances
**Compilation**: Generated code must compile without errors
**Professional**: Match MockK/Mockito-Kotlin quality standards

## Related Sections

- [Architecture](architecture.html) - Understand what to test
- [Specifications](specifications.html) - API contract validation
- [Implementation](implementation.html) - See implementation test requirements
