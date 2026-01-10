#!/bin/bash
# Book Recommender System Launcher (Linux/macOS)
# This script launches the Book Recommender System Launcher using Maven

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Calculate project root more robustly
if [[ "$SCRIPT_DIR" == */src/launcher/src/main/resources ]]; then
    # If we're in resources, go up to project root
    PROJECT_ROOT="$(dirname "$SCRIPT_DIR/../../../../..")"
elif [[ "$SCRIPT_DIR" == */bin ]]; then
    # If we're in bin directory, go up one level
    PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
else
    # Fallback
    PROJECT_ROOT="$SCRIPT_DIR"
fi

echo "Script dir: $SCRIPT_DIR"
echo "Project root: $PROJECT_ROOT"

# Verify project root is correct
if [ ! -f "$PROJECT_ROOT/pom.xml" ]; then
    echo "ERROR: pom.xml not found in calculated project root: $PROJECT_ROOT"
    echo "Please check the script location and project structure"
    exit 1
fi

# Change to project root and launch with Maven JavaFX plugin
cd "$PROJECT_ROOT"
mvn javafx:run -pl src/launcher

echo "Launcher exited"