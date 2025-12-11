# Contributing

Contributions are welcome! Please follow these guidelines.

---

## Development Workflow

### Setup

```bash
git clone https://github.com/rsicarelli/fakt.git
cd fakt
make help  # Show available commands
```

### Build

```bash
make shadowJar  # Build compiler plugin
make test-sample  # Test working example
```

### Test

```bash
make quick-test  # Rebuild plugin + test fresh
make full-rebuild  # Clean + rebuild everything
```

---

## Testing Standards

**THE ABSOLUTE STANDARD**: All tests MUST follow GIVEN-WHEN-THEN pattern.

```kotlin
@Test
fun `GIVEN repository WHEN saving user THEN returns success`() = runTest {
    // GIVEN
    val fake = fakeRepository {
        saveUser { user -> Result.success(Unit) }
    }

    // WHEN
    val result = fake.saveUser(User("123", "Alice"))

    // THEN
    assertTrue(result.isSuccess)
}
```

**Required**:

- ✅ Vanilla JUnit5 + kotlin-test (NO custom matchers)
- ✅ `@TestInstance(TestInstance.Lifecycle.PER_CLASS)`
- ✅ GIVEN-WHEN-THEN naming (uppercase, BDD style)
- ✅ Isolated instances per test (no shared state)

**Prohibited**:

- ❌ "should" naming pattern
- ❌ Custom BDD frameworks
- ❌ Mocks (use fakes)
- ❌ @BeforeEach/@AfterEach

---

## Code Quality

### Formatting

```bash
make format  # Required before commits
```

### Validation

- Ensure all generated code compiles
- Test both single-platform and KMP scenarios
- Verify output in correct source set

---

## Pull Requests

1. Fork the repository
2. Create a feature branch
3. Follow testing standards
4. Run `make format`
5. Submit PR with clear description

---

## Next Steps

For additional development guidelines, see the testing best practices and architecture patterns in the project repository.
