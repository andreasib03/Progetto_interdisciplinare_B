#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
java --module-path "$DIR/lib" --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media -jar "$DIR/BookRecommender.jar" "$@"
