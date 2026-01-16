@echo off
REM WealthVision - Run Production Server
REM Usage: run-prod.bat
REM Requires: MONGO_URI, MONGO_DATABASE environment variables

echo ============================================
echo   WealthVision - Production Mode
echo ============================================
echo.

cd /d "%~dp0.."

if "%MONGO_URI%"=="" (
    echo ERROR: MONGO_URI environment variable not set!
    pause
    exit /b 1
)

echo MongoDB Database: %MONGO_DATABASE%
echo.

REM Build the JAR
echo Building production JAR...
call mvn clean package -DskipTests -Pprod

echo Starting WealthVision with PROD profile...
echo.

java -Xms512m -Xmx1024m ^
     -jar target\wealthvision-1.0.0.jar ^
     --spring.profiles.active=prod

pause
