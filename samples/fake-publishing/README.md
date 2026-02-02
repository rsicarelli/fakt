# Fake Publishing Sample

This sample validates that Fakt-generated fakes can be published as Maven artifacts and consumed by external projects.

## Scenario

A library author wants to:
1. Create interfaces with `@Fake` annotations
2. Generate fakes using collector mode
3. Publish both API and fakes as Maven artifacts
4. Allow consumers to use pre-generated fakes in their tests

## Structure

```
fake-publishing/
├── kmp-publisher/              # Library that publishes fakes
│   ├── api/                    # Interfaces with @Fake
│   └── api-fakes/              # Collector module (publishable)
│
└── kmp-consumer/               # External project consuming fakes
    └── app/                    # Uses published fakes in tests
```

## Validation Steps

### Prerequisites

Ensure Fakt plugin is published to Maven Local:

```bash
# From repository root
make publish-local
```

### Step 1: Publish the Library

```bash
cd samples/fake-publishing/kmp-publisher
./gradlew publishToMavenLocal
```

This publishes:
- `com.rsicarelli.fakt.samples.publisher:api:1.0.0-LOCAL`
- `com.rsicarelli.fakt.samples.publisher:api-fakes:1.0.0-LOCAL`

### Step 2: Verify Published Artifacts

```bash
# Check artifacts exist
ls ~/.m2/repository/com/rsicarelli/fakt/samples/publisher/api/1.0.0-LOCAL/
ls ~/.m2/repository/com/rsicarelli/fakt/samples/publisher/api-fakes/1.0.0-LOCAL/

# Verify fakes are in the JAR (JVM target)
jar tf ~/.m2/repository/com/rsicarelli/fakt/samples/publisher/api-fakes/1.0.0-LOCAL/api-fakes-jvm-1.0.0-LOCAL.jar | grep Fake
```

Expected output should include `FakeHttpClientImpl`, `FakeRepositoryImpl`, `FakeLoggerImpl`.

### Step 3: Build Consumer Project

```bash
cd samples/fake-publishing/kmp-consumer
./gradlew build
```

If this succeeds, the consumer can compile against published fakes.

### Step 4: Run Consumer Tests

```bash
cd samples/fake-publishing/kmp-consumer
./gradlew jvmTest --info
```

If tests pass, fakes work correctly at runtime.

## Success Criteria

| Criterion | Command | Expected Result |
|-----------|---------|-----------------|
| Publisher builds | `./gradlew build` | SUCCESS |
| Publisher publishes | `./gradlew publishToMavenLocal` | Artifacts in ~/.m2 |
| Consumer compiles | `./gradlew compileKotlinJvm` | SUCCESS |
| Consumer tests pass | `./gradlew jvmTest` | All tests pass |

## Types Being Tested

| Type | Kind | Features |
|------|------|----------|
| `HttpClient` | Interface | Suspend functions |
| `Repository` | Abstract class | Properties + suspend functions |
| `Logger` | Open class | Default implementations |

## Troubleshooting

### "Could not resolve" errors

Ensure publisher was published first:
```bash
cd kmp-publisher && ./gradlew publishToMavenLocal
```

### Fakes not found in JAR

Check collector task ran:
```bash
cd kmp-publisher && ./gradlew :api-fakes:collectFakes --info
```

### Import errors in consumer

Verify package matches: fakes should be in `com.rsicarelli.fakt.samples.publisher.api`.

## Related Documentation

- [Sample Projects](../../docs/examples/index.md) - All Fakt sample projects
- [Multi-Module Usage](../../docs/user-guide/multi-module.md) - Cross-module fakes
- [Testing Guidelines](../../.claude/docs/validation/testing-guidelines.md) - BDD test patterns
