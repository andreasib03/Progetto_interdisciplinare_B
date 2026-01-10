#!/bin/bash
# Universal Book Recommender System Launcher
# This script works on Linux, macOS, and can be adapted for Windows

echo "Book Recommender System Launcher"
echo "================================="

# Detect OS
case "$(uname -s)" in
    Linux*)     OS=linux;;
    Darwin*)    OS=mac;;
    CYGWIN*)    OS=windows;;
    MINGW*)     OS=windows;;
    *)          OS=unknown
esac

echo "Detected OS: $OS"

# Get script directory and navigate to project root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Calculate project root more robustly (handle different script locations)
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

# Check Maven
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven not found. Please install Maven 3.6+"
    echo "Download from: https://maven.apache.org/download.cgi"
    exit 1
fi

# Check Java
if ! command -v java &> /dev/null; then
    echo "ERROR: Java not found. Please install Java 17+"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "ERROR: Java 17+ required, found $JAVA_VERSION"
    exit 1
fi

echo "Java $JAVA_VERSION found ✓"
echo "Maven $(mvn -v | head -n 1 | cut -d' ' -f3) found ✓"
echo "Starting application..."

# Change to project root directory and launch with Maven
cd "$PROJECT_ROOT"
exec mvn javafx:run -pl src/launcher -q