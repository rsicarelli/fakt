# Fakt Development Commands
# Run from ktfake/ directory (cd ktfake && make <command>)

.PHONY: build test compile clean format shadowJar test-sample validate quick-test full-rebuild docs docs-serve docs-open docs-stop

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

format:
	@echo "✨ Formatting code..."
	./gradlew spotlessApply

# Compiler plugin specific
shadowJar:
	@echo "📦 Building compiler plugin JAR..."
	./gradlew :compiler:shadowJar

# Test samples (now composite builds - auto-rebuild plugin!)
test-sample:
	@echo "🎯 Testing kmp-single-module sample (composite build)..."
	cd samples/kmp-single-module && ./gradlew build

# KMP multi-module sample
test-kmp-multi-module:
	@echo "🏢 Testing kmp-multi-module sample (composite build)..."
	cd samples/kmp-multi-module && ./gradlew :app:build

# Comprehensive validation workflow
validate: shadowJar test-sample test
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
	./gradlew :compiler:shadowJar
	cd samples/kmp-single-module && rm -rf build/generated
	cd samples/kmp-single-module && ./gradlew build

# Debug compiler plugin
debug:
	@echo "🐛 Debugging compiler plugin (composite build)..."
	cd samples/kmp-single-module && ./gradlew compileKotlinJvm -i | grep -E "(Fakt|Generated|ERROR)"

# Documentation
docs:
	@echo "📚 Generating Dokka documentation..."
	./gradlew dokkaGenerate
	@echo "✅ Documentation generated at build/dokka/html/"
	@echo "   Serve with: make docs-serve"

docs-serve: docs
	@echo "🛑 Stopping any existing server on port 8000..."
	@lsof -ti:8000 | xargs kill -9 2>/dev/null || true
	@sleep 1
	@echo "🌐 Starting HTTP server at http://localhost:8000"
	@echo "   Press Ctrl+C to stop"
	@cd build/dokka/html && python3 -m http.server 8000

docs-open: docs
	@echo "🛑 Stopping any existing server on port 8000..."
	@lsof -ti:8000 | xargs kill -9 2>/dev/null || true
	@sleep 1
	@echo "🌐 Starting HTTP server in background..."
	@nohup python3 -m http.server 8000 --directory build/dokka/html > /tmp/docs-server.log 2>&1 & echo $$! > /tmp/docs-server.pid; \
		sleep 2; \
		echo "✅ Server started at http://localhost:8000 (PID: $$(cat /tmp/docs-server.pid))"; \
		open http://localhost:8000; \
		echo "   Logs: /tmp/docs-server.log"; \
		echo "   To stop server: make docs-stop"

docs-stop:
	@echo "🛑 Stopping documentation server..."
	@lsof -ti:8000 | xargs kill -9 2>/dev/null && echo "✅ Server stopped" || echo "⚠️  No server running"

# Help
help:
	@echo "📚 Fakt Development Commands:"
	@echo ""
	@echo "  build           - Build entire project (plugin only, no samples)"
	@echo "  test            - Run all tests"
	@echo "  compile         - Compile Kotlin sources"
	@echo "  clean           - Clean build artifacts"
	@echo "  format          - Format code with Spotless"
	@echo ""
	@echo "  shadowJar       - Build compiler plugin JAR"
	@echo "  test-sample     - Test kmp-single-module sample (composite build)"
	@echo "  test-kmp-multi-module - Test kmp-multi-module sample (composite build)"
	@echo ""
	@echo "  validate        - Full validation workflow"
	@echo "  quick-test      - Quick development cycle (auto-rebuilds plugin!)"
	@echo "  full-rebuild    - Nuclear rebuild option"
	@echo "  debug           - Debug compiler plugin output"
	@echo ""
	@echo "  docs            - Generate Dokka documentation"
	@echo "  docs-serve      - Generate and serve docs (Ctrl+C to stop)"
	@echo "  docs-open       - Generate, serve and open docs in browser"
	@echo "  docs-stop       - Stop documentation server"
	@echo ""
	@echo "  help            - Show this help"
	@echo ""
	@echo "🎯 Note: Samples are now composite builds! Plugin changes auto-rebuild."