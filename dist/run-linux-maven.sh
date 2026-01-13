#!/bin/bash
REPO_URL="https://github.com/andreasib03/Progetto_interdisciplinare_B.git"
PROJECT_DIR="/tmp/BookRecommender"
LIB_DIR="/tmp/BookRecommender-lib"
MAVEN_REPO="$HOME/.m2/repository"
JAVAFX_VERSION="17.0.12"

if [ ! -f "$PROJECT_DIR/src/launcher/target/launcher-1.0-SNAPSHOT-jar-with-dependencies.jar" ]; then
    echo "📦 Cloning and building..."
    rm -rf "$PROJECT_DIR"
    git clone "$REPO_URL" "$PROJECT_DIR" --depth 1 -q
    if [ $? -ne 0 ]; then
        echo "❌ Clone failed! Make sure git is installed."
        exit 1
    fi
    echo "📦 Building..."
    cd "$PROJECT_DIR"
    mvn clean install -DskipTests=true -q
    if [ $? -ne 0 ]; then
        echo "❌ Build failed!"
        exit 1
    fi
fi

rm -rf "$LIB_DIR"
mkdir -p "$LIB_DIR"

cp "$MAVEN_REPO/org/openjfx/javafx-base/$JAVAFX_VERSION/javafx-base-${JAVAFX_VERSION}-linux.jar" "$LIB_DIR/"
cp "$MAVEN_REPO/org/openjfx/javafx-graphics/$JAVAFX_VERSION/javafx-graphics-${JAVAFX_VERSION}-linux.jar" "$LIB_DIR/"
cp "$MAVEN_REPO/org/openjfx/javafx-controls/$JAVAFX_VERSION/javafx-controls-${JAVAFX_VERSION}-linux.jar" "$LIB_DIR/"
cp "$MAVEN_REPO/org/openjfx/javafx-fxml/$JAVAFX_VERSION/javafx-fxml-${JAVAFX_VERSION}-linux.jar" "$LIB_DIR/"
cp "$MAVEN_REPO/org/openjfx/javafx-media/$JAVAFX_VERSION/javafx-media-${JAVAFX_VERSION}-linux.jar" "$LIB_DIR/"
cp "$MAVEN_REPO/org/kordamp/ikonli/ikonli-javafx/12.3.1/ikonli-javafx-12.3.1.jar" "$LIB_DIR/"
cp "$MAVEN_REPO/org/kordamp/ikonli/ikonli-fontawesome5-pack/12.3.1/ikonli-fontawesome5-pack-12.3.1.jar" "$LIB_DIR/"
cp "$MAVEN_REPO/org/kordamp/ikonli/ikonli-materialdesign-pack/12.3.1/ikonli-materialdesign-pack-12.3.1.jar" "$LIB_DIR/"

echo "▶️  Avvio BookRecommender..."
java --module-path "$LIB_DIR" --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media --enable-native-access=javafx.graphics,javafx.media -jar "$PROJECT_DIR/src/launcher/target/launcher-1.0-SNAPSHOT-jar-with-dependencies.jar" "$@"
