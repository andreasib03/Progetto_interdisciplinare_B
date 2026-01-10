@echo off
REM Book Recommender System Launcher (Windows)
REM This script launches the Book Recommender System Launcher

echo Starting Book Recommender System Launcher...

REM Check if Java is available
java -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17 or higher and try again
    pause
    exit /b 1
)

REM Check Java version (basic check)
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do set JAVA_VER=%%i
set JAVA_VER=%JAVA_VER:"=%
for /f "delims=." %%i in ("%JAVA_VER%") do set JAVA_MAJOR=%%i
if %JAVA_MAJOR% LSS 17 (
    echo ERROR: Java 17 or higher is required. Found Java %JAVA_MAJOR%
    pause
    exit /b 1
)

echo Java version check passed

REM Get the directory where this script is located
set "SCRIPT_DIR=%~dp0"
set "BIN_DIR=%SCRIPT_DIR%.."

echo Java version check passed

echo Bin directory: %BIN_DIR%

REM Launch the application with proper JavaFX options
java ^
    --add-opens javafx.controls/com.sun.javafx.application=ALL-UNNAMED ^
    --add-opens javafx.base/com.sun.javafx.reflect=ALL-UNNAMED ^
    --add-opens javafx.graphics/com.sun.javafx.application=ALL-UNNAMED ^
    -Djava.net.preferIPv4Stack=true ^
    -Dfile.encoding=UTF-8 ^
    -jar "%BIN_DIR%jar\launcher-1.0-SNAPSHOT-jar-with-dependencies.jar"

echo Launcher exited
pause