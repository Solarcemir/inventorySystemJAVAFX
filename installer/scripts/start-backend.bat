@echo off
title Inventory Backend Server
cd /d "%~dp0.."
echo ========================================
echo   Starting Backend Server...
echo ========================================
echo.

REM Usar el JRE incluido en el instalador
set JAVA_HOME=%~dp0..\jre
set PATH=%JAVA_HOME%\bin;%PATH%

echo Using Java from: %JAVA_HOME%
java -version
echo.

REM Iniciar el backend
echo Starting server on port 8080...
java -jar backend\backend.jar

pause
