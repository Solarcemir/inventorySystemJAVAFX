@echo off
setlocal
title CFT Electronic - Build Installer

echo.
echo  =====================================================
echo   CFT Electronic - Full Build ^& Installer Creator
echo  =====================================================
echo.

set ROOT=%~dp0..
set INSTALLER=%~dp0

REM The real database connection lives outside git, in this file
set DB_CONFIG=%ROOT%\backend\config\application.properties
if not exist "%DB_CONFIG%" (
    echo.
    echo  ERROR: %DB_CONFIG% not found.
    echo  Copy application.properties.example from that folder and fill in the values.
    pause & exit /b 1
)

REM ── Step 1: Build React frontend into Spring Boot static folder ──────────────
echo [1/4] Building React frontend...
cd /d "%ROOT%\web-version\frontend"
call npm run build
if errorlevel 1 (
    echo.
    echo  ERROR: Frontend build failed. Check npm errors above.
    pause & exit /b 1
)
echo  Frontend built OK.
echo.

REM ── Step 2: Package Spring Boot as a fat JAR ────────────────────────────────
echo [2/4] Packaging Spring Boot backend...
cd /d "%ROOT%\backend"
call mvn clean package -DskipTests -q
if errorlevel 1 (
    echo.
    echo  ERROR: Maven build failed. Check errors above.
    pause & exit /b 1
)
echo  Backend packaged OK.
echo.

REM ── Step 3: Copy JAR + config to installer folder ───────────────────────────
echo [3/4] Preparing installer assets...
if not exist "%INSTALLER%app" mkdir "%INSTALLER%app"
if not exist "%INSTALLER%app\config" mkdir "%INSTALLER%app\config"

REM Copy the fat JAR (rename to backend.jar for simplicity)
copy /Y "%ROOT%\backend\target\backend-0.0.1-SNAPSHOT.jar" "%INSTALLER%app\backend.jar" >nul
if errorlevel 1 (
    echo  ERROR: Could not copy backend.jar. Did Maven build succeed?
    pause & exit /b 1
)

REM The JAR holds no credentials: the installed app reads them from config\application.properties
copy /Y "%DB_CONFIG%" "%INSTALLER%app\config\application.properties" >nul

echo  Assets ready.
echo.

REM ── Step 4: Compile Inno Setup installer ────────────────────────────────────
echo [4/4] Compiling Inno Setup installer...

REM Find ISCC.exe — checks system-wide, per-user, and PATH
set ISCC=
if exist "%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe" set "ISCC=%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe"
if exist "%ProgramFiles%\Inno Setup 6\ISCC.exe"       set "ISCC=%ProgramFiles%\Inno Setup 6\ISCC.exe"
if exist "%LOCALAPPDATA%\Programs\Inno Setup 6\ISCC.exe" set "ISCC=%LOCALAPPDATA%\Programs\Inno Setup 6\ISCC.exe"
if exist "%LOCALAPPDATA%\Inno Setup 6\ISCC.exe"          set "ISCC=%LOCALAPPDATA%\Inno Setup 6\ISCC.exe"
if exist "%APPDATA%\Inno Setup 6\ISCC.exe"               set "ISCC=%APPDATA%\Inno Setup 6\ISCC.exe"

REM Last resort: check if ISCC is on PATH
if "%ISCC%"=="" (
    for /f "delims=" %%i in ('where ISCC.exe 2^>nul') do set "ISCC=%%i"
)

if "%ISCC%"=="" (
    echo  ERROR: Inno Setup 6 not found.
    echo  Download from: https://jrsoftware.org/isdl.php
    pause & exit /b 1
)
echo  Using Inno Setup at: %ISCC%

if not exist "%INSTALLER%output" mkdir "%INSTALLER%output"

"%ISCC%" "%INSTALLER%setup.iss"
if errorlevel 1 (
    echo.
    echo  ERROR: Inno Setup compilation failed.
    pause & exit /b 1
)

echo.
echo  =====================================================
echo   SUCCESS! Installer created:
echo   %INSTALLER%output\CFT-Electronic-Setup-v1.1.0.exe
echo  =====================================================
echo.
pause
