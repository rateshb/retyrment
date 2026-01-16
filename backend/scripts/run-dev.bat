@echo off
REM Retyrment - Run Development Server
REM Usage: run-dev.bat
REM Requires: MONGO_URI environment variable

echo ============================================
echo   Retyrment - Development Mode
echo ============================================
echo.

cd /d "%~dp0.."

if "%MONGO_URI%"=="" (
    echo ERROR: MONGO_URI environment variable not set!
    echo Set it with: set MONGO_URI=mongodb://your-dev-server:27017/retyrment_dev
    pause
    exit /b 1
)

echo MongoDB URI: %MONGO_URI%
echo.

REM Build the JAR if not exists
if not exist target\retyrment-1.0.0.jar (
    echo Building application...
    call mvn clean package -DskipTests
)

echo Starting Retyrment with DEV profile...
echo.

java -jar target\retyrment-1.0.0.jar --spring.profiles.active=dev

pause
