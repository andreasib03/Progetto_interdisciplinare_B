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
