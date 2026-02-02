# Fakt Development Commands
# Run from fakt/ directory (or from project root)

.PHONY: build test compile clean format shadowJar test-sample validate quick-test full-rebuild

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