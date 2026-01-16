@echo off
REM WealthVision - Run Local Development Server
REM Usage: run-local.bat

echo ============================================
echo   WealthVision - Local Development Mode
echo ============================================
echo.

cd /d "%~dp0.."

echo Starting MongoDB check...
where mongod >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo WARNING: MongoDB not found in PATH. Make sure MongoDB is running!
    echo.
)

echo Starting WealthVision with LOCAL profile...
echo.

call mvn spring-boot:run -Dspring-boot.run.profiles=local

pause
