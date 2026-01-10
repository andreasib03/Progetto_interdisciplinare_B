#!/bin/bash
# Book Recommender Launcher (Linux/macOS)
# Simple wrapper to run the JAR application

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
java -jar "$DIR/BookRecommender.jar" "$@"