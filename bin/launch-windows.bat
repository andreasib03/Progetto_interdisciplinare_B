@echo off
setlocal enabledelayedexpansion
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
cd /d "%SCRIPT_DIR%.."
set "BIN_DIR=%cd%"

echo Java version check passed

echo Bin directory: %BIN_DIR%

REM Check if Maven is available
mvn -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven and try again
    pause
    exit /b 1
)

echo.
echo INFO: Direct JAR execution may fail due to JavaFX classloader issues.
echo INFO: Launching via Maven instead...
echo.

cd /d "%BIN_DIR%"
mvn javafx:run -pl src/launcher -q

echo Maven javafx:run finished

echo Launcher exited
pause