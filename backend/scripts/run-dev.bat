@echo off
REM WealthVision - Run Development Server
REM Usage: run-dev.bat
REM Requires: MONGO_URI environment variable

echo ============================================
echo   WealthVision - Development Mode
echo ============================================
echo.

cd /d "%~dp0.."

if "%MONGO_URI%"=="" (
    echo ERROR: MONGO_URI environment variable not set!
    echo Set it with: set MONGO_URI=mongodb://your-dev-server:27017/wealthvision_dev
    pause
    exit /b 1
)

echo MongoDB URI: %MONGO_URI%
echo.

REM Build the JAR if not exists
if not exist target\wealthvision-1.0.0.jar (
    echo Building application...
    call mvn clean package -DskipTests
)

echo Starting WealthVision with DEV profile...
echo.

java -jar target\wealthvision-1.0.0.jar --spring.profiles.active=dev

pause
