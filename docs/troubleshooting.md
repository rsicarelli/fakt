# Troubleshooting

Common issues and solutions.

---

## Generated Fakes Not Appearing

**Symptoms**: IDE doesn't recognize `fakeXxx()` factory functions

**Solutions**:

1. **Rebuild the project**: `./gradlew clean build`
2. **Invalidate IDE caches**: File → Invalidate Caches → Invalidate and Restart
3. **Check build directory**: Fakes are in `build/generated/fakt/commonTest/kotlin/`
4. **Verify Gradle sync**: Ensure Gradle sync completed successfully

---

## Unresolved Reference: fakeXxx

**Common causes**:

1. **Missing build step**: Run `./gradlew build` first
2. **Wrong source set**: Import from test code (`src/commonTest/`), not main
3. **Package mismatch**: Generated fakes are in the same package as the interface
4. **Gradle sync issue**: Re-sync Gradle in your IDE

---

## Compilation Fails with "IrTypeAliasSymbol not found"

**Causes**:

1. **Kotlin version mismatch**: Ensure you're on Kotlin 2.2.10+
2. **Fakt version incompatibility**: Update Fakt to match your Kotlin version

**Solution**:

```kotlin
// gradle/libs.versions.toml
[versions]
kotlin = "2.2.10"
fakt = "1.0.0-SNAPSHOT"
```

---

## Build is Slow

**Solutions**:

1. **Use LogLevel.QUIET in CI/CD**:
   ```kotlin
   fakt {
       logLevel.set(LogLevel.QUIET)
   }
   ```

2. **Check cache hit rate** with `LogLevel.INFO`

3. **Verify incremental compilation** is enabled

---

## Still Having Issues?

- [FAQ](faq.md) - Common questions
- [GitHub Issues](https://github.com/rsicarelli/fakt/issues) - Report bugs
- [GitHub Discussions](https://github.com/rsicarelli/fakt/discussions) - Ask questions
