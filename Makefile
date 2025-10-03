# Copyright (C) 2025 Rodrigo Sicarelli
# SPDX-License-Identifier: Apache-2.0

.PHONY: help docs docs-serve docs-open clean

# Default target
help:
	@echo "Fakt Development Commands:"
	@echo ""
	@echo "Documentation:"
	@echo "  make docs        - Generate Dokka documentation"
	@echo "  make docs-serve  - Generate and serve docs at http://localhost:8000"
	@echo "  make docs-open   - Generate, serve, and open docs in browser"
	@echo ""
	@echo "Cleanup:"
	@echo "  make clean       - Clean all build artifacts"

# Generate Dokka documentation
docs:
	@echo "🔨 Generating Dokka documentation..."
	./gradlew dokkaGenerate
	@echo "✅ Documentation generated at build/dokka/html/"
	@echo "   Serve with: make docs-serve"

# Generate and serve documentation via HTTP
docs-serve: docs
	@echo "🌐 Starting HTTP server at http://localhost:8000"
	@echo "   Press Ctrl+C to stop"
	@cd build/dokka/html && python3 -m http.server 8000

# Generate, serve, and open in browser
docs-open: docs
	@echo "🌐 Starting HTTP server and opening browser..."
	@cd build/dokka/html && python3 -m http.server 8000 > /dev/null 2>&1 & \
		sleep 2 && open http://localhost:8000
	@echo "✅ Documentation opened at http://localhost:8000"
	@echo "   Server running in background (PID: $$(pgrep -f 'python3 -m http.server 8000'))"
	@echo "   To stop: kill $$(pgrep -f 'python3 -m http.server 8000')"

# Clean build artifacts
clean:
	@echo "🧹 Cleaning build artifacts..."
	./gradlew clean
	@echo "✅ Build artifacts cleaned"
