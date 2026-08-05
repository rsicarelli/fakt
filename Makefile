# Fakt Development Commands
# Run from fakt/ directory (or from project root)

.PHONY: build test compile clean format shadowJar test-sample test-fake-publishing validate quick-test full-rebuild test-compat-all test-compat-agp-all test-kmp-android-lint benchmark

# Core build commands
build:
	@echo "🏗️ Building Fakt..."
	./gradlew build

test:
	@echo "🧪 Running tests..."
	./gradlew test

compile:
	@echo "⚙️ Compiling Kotlin sources..."
	./gradlew compileKotlinJvm

clean:
	@echo "🧹 Cleaning build artifacts..."
	./gradlew clean
	@echo "🧹 Cleaning samples..."
	@find samples -maxdepth 2 -type d -name ".gradle" -exec rm -rf {} + 2>/dev/null || true
	@find samples -maxdepth 2 -type d -name "build" -exec rm -rf {} + 2>/dev/null || true

format:
	@echo "✨ Formatting code with ktfmt (via Spotless)..."
	./gradlew spotlessApply

# Compiler plugin specific
shadowJar:
	@echo "📦 Building compiler plugin JAR (debug only - use publish-local for actual usage)..."
	./gradlew :compiler:shadowJar

# Local publishing (use this for development!)
publish-local:
	@echo "📤 Publishing to Maven Local (no signing required locally)..."
	./gradlew publishToMavenLocal --no-daemon

# Test samples (now composite builds - auto-rebuild plugin!)
test-sample:
	@echo "🎯 Testing kmp-single-module sample (composite build)..."
	cd samples/kmp-single-module && ./gradlew build

# KMP multi-module sample
test-kmp-multi-module:
	@echo "🏢 Testing kmp-multi-module sample (composite build)..."
	cd samples/kmp-multi-module && ./gradlew :app:build

# KMP multi-target sample (hierarchy validation)
test-kmp-multi-target:
	@echo "🎯 Testing kmp-multi-target sample (hierarchy validation)..."
	cd samples/kmp-multi-target && ./gradlew allTests

# KMP sample without any JVM/Android target (metadata-driver producer)
test-kmp-no-jvm:
	@echo "🧭 Testing kmp-no-jvm sample (no drivable target)..."
	cd samples/kmp-no-jvm && ./gradlew allTests

# Fake publishing sample (two-project workflow)
test-fake-publishing:
	@echo "📤 Testing fake-publishing sample (two-step workflow)..."
	@echo "Step 1: Publishing kmp-publisher to Maven Local..."
	cd samples/fake-publishing/kmp-publisher && ./gradlew publishToMavenLocal
	@echo "Step 2: Building and testing kmp-consumer..."
	cd samples/fake-publishing/kmp-consumer && ./gradlew build

# Compat sample testing (multi-version Kotlin verification)
test-compat-all: publish-local
	@echo "Testing all compat samples..."
	@for dir in samples/compat/kotlin-*/; do \
		version=$$(basename "$$dir" | sed 's/kotlin-//'); \
		echo "Testing Kotlin $$version..."; \
		./gradlew -p "$$dir" jvmTest --no-daemon || exit 1; \
		echo "Kotlin $$version: PASS"; \
	done
	@echo "All compat samples passed!"

test-compat-%: publish-local
	./gradlew -p samples/compat/kotlin-$* jvmTest --no-daemon

# AGP compat sample testing (multi-version Android Gradle Plugin verification).
# Each agp-* sample ships its own Gradle wrapper (AGP 8.11 needs Gradle 8.13, which the repo-root
# Gradle 9 wrapper cannot provide), so these `cd` into the sample and use its own wrapper — not
# `./gradlew -p` from the root.
test-compat-agp-all: publish-local
	@echo "Testing all AGP compat samples..."
	@for dir in samples/compat-agp/agp-*/; do \
		version=$$(basename "$$dir" | sed 's/agp-//'); \
		echo "Testing AGP $$version..."; \
		(cd "$$dir" && ./gradlew testDebugUnitTest --no-daemon) || exit 1; \
		echo "AGP $$version: PASS"; \
	done
	@echo "All AGP compat samples passed!"

test-compat-agp-%: publish-local
	cd samples/compat-agp/agp-$* && ./gradlew testDebugUnitTest --no-daemon

# KMP + Android sample pinned to its own Gradle 9.6.1 wrapper. Runs AGP lint over the commonTest
# generated source dir as an end-to-end smoke test for #129 (the deterministic guard is the
# SimplifiedSourceSetConfigurationTest unit test).
test-kmp-android-lint: publish-local
	@echo "🤖 Testing kmp-android-lint sample (Gradle 9.6.1, AGP lint)..."
	cd samples/kmp-android-lint && ./gradlew lint --no-daemon

# Runtime benchmark — measures test EXECUTION time of Fakt vs mock libraries and prints a comparison
# table. Runs every competitor in its own isolated module across FORKS fresh JVMs. --continue keeps
# one technology's failure from hiding the others. The authoritative run is CI (benchmark.yml).
FORKS ?= 3
benchmark: publish-local
	@echo "📊 Runtime benchmark ($(FORKS) forks): Fakt vs MockK / Mockito / Mokkery / hand-written..."
	@for i in $$(seq 1 $(FORKS)); do \
		echo "→ fork $$i/$(FORKS)"; \
		( cd samples/runtime-benchmark && ./gradlew benchmark --continue -Pfakt.benchmark.fork=$$i ) || true; \
	done
	@echo "📋 Aggregating results into a comparison table..."
	kotlin .github/scripts/benchmark-summary.main.kts "samples/runtime-benchmark" "Runtime Benchmark"

# Comprehensive validation workflow (runs all checks like CI)
validate:
	@echo "🔍 Running comprehensive validation..."
	@echo ""
	@echo "1️⃣ Formatting check..."
	./gradlew spotlessCheck
	@echo ""
	@echo "2️⃣ Static analysis..."
	./gradlew detekt
	@echo ""
	@echo "3️⃣ License audit..."
	./gradlew checkLicense
	@echo ""
	@echo "4️⃣ Running tests..."
	./gradlew test
	@echo ""
	@echo "5️⃣ Publishing plugin locally..."
	./gradlew publishToMavenLocal --no-daemon
	@echo ""
	@echo "6️⃣ API compatibility check..."
	./gradlew apiCheck
	@echo ""
	@echo "7️⃣ Testing samples..."
	cd samples/kmp-single-module && ../../gradlew build
	cd samples/kmp-multi-target && ../../gradlew allTests
	@echo ""
	@echo "✅ Full validation complete!"

# Quick development cycle (composite build auto-rebuilds plugin!)
quick-test:
	@echo "⚡ Quick test cycle (composite builds)..."
	cd samples/kmp-single-module && rm -rf build/generated
	cd samples/kmp-single-module && ./gradlew compileKotlinJvm --no-build-cache

# Full rebuild (nuclear option)
full-rebuild:
	@echo "💥 Full rebuild with clean slate..."
	./gradlew clean --no-build-cache
	./gradlew publishToMavenLocal --no-daemon
	cd samples/kmp-single-module && rm -rf build/generated
	cd samples/kmp-single-module && ./gradlew build

# Debug compiler plugin
debug:
	@echo "🐛 Debugging compiler plugin (composite build)..."
	cd samples/kmp-single-module && ./gradlew compileKotlinJvm -i | grep -E "(Fakt|Generated|ERROR)"

# Help
help:
	@echo "📚 Fakt Development Commands:"
	@echo ""
	@echo "  build           - Build entire project (plugin only, no samples)"
	@echo "  test            - Run all tests"
	@echo "  compile         - Compile Kotlin sources"
	@echo "  clean           - Clean build artifacts"
	@echo "  format          - Format code with ktfmt (via Spotless)"
	@echo ""
	@echo "  shadowJar       - Build compiler plugin JAR (debug only)"
	@echo "  publish-local   - Publish to Maven Local (⭐ use this for development!)"
	@echo "  test-sample     - Test kmp-single-module sample (composite build)"
	@echo "  test-kmp-multi-module - Test kmp-multi-module sample (composite build)"
	@echo "  test-kmp-multi-target - Test kmp-multi-target sample (hierarchy validation)"
	@echo "  test-kmp-no-jvm - Test kmp-no-jvm sample (no JVM/Android target)"
	@echo "  test-fake-publishing - Test fake-publishing sample (two-step workflow)"
	@echo "  test-compat-all     - Test all compat samples (Kotlin 2.2.0-2.4.10)"
	@echo "  test-compat-VERSION - Test specific compat sample (e.g., test-compat-2.2.0)"
	@echo "  test-compat-agp-all - Test all AGP compat samples (AGP 8.11, 8.12, 9.0)"
	@echo "  test-compat-agp-VERSION - Test specific AGP compat sample (e.g., test-compat-agp-8.11)"
	@echo "  test-kmp-android-lint - Test KMP+Android sample AGP lint on Gradle 9.6.1 (#129 guard)"
	@echo "  benchmark       - 📊 Runtime benchmark: Fakt vs mock libraries (comparison table)"
	@echo ""
	@echo "  validate        - ⭐ Run all validations (format, lint, tests, samples)"
	@echo "  quick-test      - Quick development cycle (auto-rebuilds plugin!)"
	@echo "  full-rebuild    - Nuclear rebuild option"
	@echo "  debug           - Debug compiler plugin output"
	@echo ""
	@echo "  help            - Show this help"
	@echo ""
	@echo "🎯 Note: Samples are now composite builds! Plugin changes auto-rebuild."
	@echo "💡 Tip: Use 'validate' before committing - runs all checks like CI!"