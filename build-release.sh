#!/bin/bash
# Script per rigenerare i pacchetti di distribuzione
# Esegue: build JAR → copia dipendenze → crea pacchetti → verifica

echo "🔨 Rigenerazione pacchetti di distribuzione..."
echo

# 1. Build completo del progetto
echo "📦 Building progetto..."
mvn clean package -DskipTests=true -q
if [ $? -ne 0 ]; then
    echo "❌ Build fallito!"
    exit 1
fi
echo "✅ Build completato"
echo

# 2. Preparazione directory distribuzione
echo "📁 Preparazione distribuzione..."
mkdir -p dist/lib

# Copia il JAR principale
cp src/launcher/target/launcher-1.0-SNAPSHOT-jar-with-dependencies.jar dist/BookRecommender.jar
echo "✅ JAR principale copiato"

# Copia dipendenze JavaFX (requisite per eseguire l'app)
JAVAFX_VERSION="17.0.12"
MAVEN_REPO="$HOME/.m2/repository"

cp "$MAVEN_REPO/org/openjfx/javafx-base/$JAVAFX_VERSION/javafx-base-$JAVAFX_VERSION.jar" dist/lib/
cp "$MAVEN_REPO/org/openjfx/javafx-base/$JAVAFX_VERSION/javafx-base-$JAVAFX_VERSION-linux.jar" dist/lib/ 2>/dev/null || true
cp "$MAVEN_REPO/org/openjfx/javafx-base/$JAVAFX_VERSION/javafx-base-$JAVAFX_VERSION-win.jar" dist/lib/ 2>/dev/null || true
cp "$MAVEN_REPO/org/openjfx/javafx-graphics/$JAVAFX_VERSION/javafx-graphics-$JAVAFX_VERSION.jar" dist/lib/
cp "$MAVEN_REPO/org/openjfx/javafx-graphics/$JAVAFX_VERSION/javafx-graphics-$JAVAFX_VERSION-linux.jar" dist/lib/ 2>/dev/null || true
cp "$MAVEN_REPO/org/openjfx/javafx-graphics/$JAVAFX_VERSION/javafx-graphics-$JAVAFX_VERSION-win.jar" dist/lib/ 2>/dev/null || true
cp "$MAVEN_REPO/org/openjfx/javafx-controls/$JAVAFX_VERSION/javafx-controls-$JAVAFX_VERSION.jar" dist/lib/
cp "$MAVEN_REPO/org/openjfx/javafx-controls/$JAVAFX_VERSION/javafx-controls-$JAVAFX_VERSION-linux.jar" dist/lib/ 2>/dev/null || true
cp "$MAVEN_REPO/org/openjfx/javafx-controls/$JAVAFX_VERSION/javafx-controls-$JAVAFX_VERSION-win.jar" dist/lib/ 2>/dev/null || true
cp "$MAVEN_REPO/org/openjfx/javafx-fxml/$JAVAFX_VERSION/javafx-fxml-$JAVAFX_VERSION.jar" dist/lib/
cp "$MAVEN_REPO/org/openjfx/javafx-fxml/$JAVAFX_VERSION/javafx-fxml-$JAVAFX_VERSION-linux.jar" dist/lib/ 2>/dev/null || true
cp "$MAVEN_REPO/org/openjfx/javafx-fxml/$JAVAFX_VERSION/javafx-fxml-$JAVAFX_VERSION-win.jar" dist/lib/ 2>/dev/null || true
cp "$MAVEN_REPO/org/openjfx/javafx-media/$JAVAFX_VERSION/javafx-media-$JAVAFX_VERSION.jar" dist/lib/
cp "$MAVEN_REPO/org/openjfx/javafx-media/$JAVAFX_VERSION/javafx-media-$JAVAFX_VERSION-linux.jar" dist/lib/ 2>/dev/null || true
cp "$MAVEN_REPO/org/openjfx/javafx-media/$JAVAFX_VERSION/javafx-media-$JAVAFX_VERSION-win.jar" dist/lib/ 2>/dev/null || true

# Copia ikonli
cp "$MAVEN_REPO/org/kordamp/ikonli/ikonli-javafx/12.3.1/ikonli-javafx-12.3.1.jar" dist/lib/
cp "$MAVEN_REPO/org/kordamp/ikonli/ikonli-fontawesome5-pack/12.3.1/ikonli-fontawesome5-pack-12.3.1.jar" dist/lib/
cp "$MAVEN_REPO/org/kordamp/ikonli/ikonli-materialdesign-pack/12.3.1/ikonli-materialdesign-pack-12.3.1.jar" dist/lib/

echo "✅ Dipendenze copiate in dist/lib"
echo

# 3. Aggiorna script di run (standalone - con lib bundleato)
echo "📝 Aggiornamento script di run standalone..."
cat > dist/run-linux.sh << 'RUNSCRIPT'
#!/bin/bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
java --module-path "$DIR/lib" --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media -jar "$DIR/BookRecommender.jar" "$@"
RUNSCRIPT
chmod +x dist/run-linux.sh

cat > dist/run-windows.bat << 'RUNSCRIPT'
@echo off
set DIR=%~dp0
java --module-path "%DIR%lib" --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media -jar "%DIR%BookRecommender.jar" %*
RUNSCRIPT

# Script per utenti con Maven installato
echo "📝 Aggiornamento script di run Maven..."
cat > dist/run-linux-maven.sh << 'RUNSCRIPT'
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
RUNSCRIPT
chmod +x dist/run-linux-maven.sh

cat > dist/run-windows-maven.bat << 'RUNSCRIPT'
@echo off
setlocal EnableDelayedExpansion
set REPO_URL=https://github.com/andreasib03/Progetto_interdisciplinare_B.git
set PROJECT_DIR=%TEMP%\BookRecommender
set LIB_DIR=%TEMP%\BookRecommender-lib
set MAVEN_REPO=%USERPROFILE%\.m2\repository
set JAVAFX_VERSION=17.0.12

if not exist "%PROJECT_DIR%\src\launcher\target\launcher-1.0-SNAPSHOT-jar-with-dependencies.jar" (
    echo "📦 Cloning and building..."
    if exist "%PROJECT_DIR%" rmdir /s /q "%PROJECT_DIR%"
    git clone "%REPO_URL%" "%PROJECT_DIR%" --depth 1 -q
    if errorlevel 1 (
        echo "❌ Clone failed! Make sure git is installed."
        pause
        exit /b 1
    )
    echo "📦 Building..."
    cd /d "%PROJECT_DIR%"
    call mvn clean install -DskipTests=true -q
    if errorlevel 1 (
        echo "❌ Build failed!"
        pause
        exit /b 1
    )
    cd /d "%~dp0"
)

if exist "%LIB_DIR%" rmdir /s /q "%LIB_DIR%"
mkdir "%LIB_DIR%"

copy "%MAVEN_REPO%\org\openjfx\javafx-base\%JAVAFX_VERSION%\javafx-base-%JAVAFX_VERSION%-win.jar" "%LIB_DIR%\" >nul
copy "%MAVEN_REPO%\org\openjfx\javafx-graphics\%JAVAFX_VERSION%\javafx-graphics-%JAVAFX_VERSION%-win.jar" "%LIB_DIR%\" >nul
copy "%MAVEN_REPO%\org\openjfx\javafx-controls\%JAVAFX_VERSION%\javafx-controls-%JAVAFX_VERSION%-win.jar" "%LIB_DIR%\" >nul
copy "%MAVEN_REPO%\org\openjfx\javafx-fxml\%JAVAFX_VERSION%\javafx-fxml-%JAVAFX_VERSION%-win.jar" "%LIB_DIR%\" >nul
copy "%MAVEN_REPO%\org\openjfx\javafx-media\%JAVAFX_VERSION%\javafx-media-%JAVAFX_VERSION%-win.jar" "%LIB_DIR%\" >nul
copy "%MAVEN_REPO%\org\kordamp\ikonli\ikonli-javafx\12.3.1\ikonli-javafx-12.3.1.jar" "%LIB_DIR%\" >nul
copy "%MAVEN_REPO%\org\kordamp\ikonli\ikonli-fontawesome5-pack\12.3.1\ikonli-fontawesome5-pack-12.3.1.jar" "%LIB_DIR%\" >nul
copy "%MAVEN_REPO%\org\kordamp\ikonli\ikonli-materialdesign-pack\12.3.1\ikonli-materialdesign-pack-12.3.1.jar" "%LIB_DIR%\" >nul

echo "▶️  Avvio BookRecommender..."
java --module-path "%LIB_DIR%" --add-modules javafx.base,javafx.controls,javafx.fxml,javafx.graphics,javafx.media --enable-native-access=javafx.graphics,javafx.media -jar "%PROJECT_DIR%\src\launcher\target\launcher-1.0-SNAPSHOT-jar-with-dependencies.jar" %*
endlocal
RUNSCRIPT
echo "✅ Script di run aggiornati"
echo

# 4. Creazione pacchetti
echo "📦 Creazione pacchetti..."
cd dist

rm -f ../BookRecommender-*.tar.gz ../BookRecommender-*.zip

tar -czf ../BookRecommender-Unix.tar.gz *

if command -v zip &> /dev/null; then
    zip -r ../BookRecommender-Windows.zip *
elif command -v powershell &> /dev/null; then
    powershell -Command "Compress-Archive -Path '*' -DestinationPath '../BookRecommender-Windows.zip' -Force"
else
    echo "⚠️  zip non disponibile. Crea BookRecommender-Windows.zip manualmente."
fi

cd ..
echo "✅ Pacchetti creati:"
ls -lh BookRecommender-*.tar.gz BookRecommender-*.zip
echo

# 5. Test rapido
echo "🧪 Test rapido pacchetti..."
if [ -f "BookRecommender-Unix.tar.gz" ] && [ -f "BookRecommender-Windows.zip" ]; then
    echo "✅ Tutti i pacchetti sono stati creati correttamente!"
    echo
    echo "🎯 Pronti per GitHub Release!"
    echo "   - Carica: BookRecommender-Windows.zip"
    echo "   - Carica: BookRecommender-Unix.tar.gz"
else
    echo "❌ Errore nella creazione dei pacchetti!"
    exit 1
fi
