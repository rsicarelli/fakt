#!/bin/bash
# Skill Auto-Activation Hook (Bash wrapper for TypeScript implementation)
#
# This script executes the TypeScript hook using tsx (TypeScript executor).
# It's called by Claude Code on UserPromptSubmit hook events.
#
# Environment:
# - CLAUDE_PROJECT_DIR: Automatically set by Claude Code to project root
# - stdin: JSON input from Claude Code with prompt and session info
# - stdout: Skill suggestions injected into Claude's context

set -euo pipefail

# Navigate to hooks directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Check if dependencies are installed
if [ ! -d "node_modules" ]; then
    echo "🔧 Installing hook dependencies..."
    npm install --silent > /dev/null 2>&1
fi

# Execute TypeScript hook using tsx
npx tsx skill-activation-prompt.ts
