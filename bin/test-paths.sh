#!/bin/bash
# Test script to verify path calculations

echo "=== Path Calculation Test ==="
echo

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
echo "Script dir: $SCRIPT_DIR"

# Calculate project root more robustly
if [[ "$SCRIPT_DIR" == */bin ]]; then
    # If we're in a bin directory, go up one level
    PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
else
    # Fallback: assume we're in project root
    PROJECT_ROOT="$SCRIPT_DIR"
fi

echo "Project root: $PROJECT_ROOT"

# Check if pom.xml exists
if [ -f "$PROJECT_ROOT/pom.xml" ]; then
    echo "✅ pom.xml found - path calculation is correct"
else
    echo "❌ pom.xml not found - path calculation is wrong"
fi

echo
echo "Current working directory: $(pwd)"
echo "Files in project root:"
ls -la "$PROJECT_ROOT" | head -10

echo
echo "=== Test Complete ==="