@echo off
title PostgreSQL Installation
cd /d "%~dp0.."
echo ========================================
echo   Installing PostgreSQL...
echo ========================================
echo.

REM Instalar PostgreSQL en modo silencioso
echo [1/4] Installing PostgreSQL Server...
"%~dp0..\postgresql-installer.exe" ^
    --mode unattended ^
    --unattendedmodeui minimal ^
    --superpassword admin123 ^
    --servicename postgresql-x64-15 ^
    --serverport 5432 ^
    --datadir "C:\Program Files\PostgreSQL\15\data" ^
    --locale "English, United States"

if %errorlevel% neq 0 (
    echo [ERROR] PostgreSQL installation failed!
    pause
    exit /b 1
)

echo [2/4] Waiting for PostgreSQL to start...
timeout /t 15 /nobreak >nul

REM Agregar PostgreSQL al PATH temporalmente
set PGPATH=C:\Program Files\PostgreSQL\15\bin
set PATH=%PGPATH%;%PATH%
set PGPASSWORD=admin123

echo [3/4] Creating database...
"%PGPATH%\psql.exe" -U postgres -c "CREATE DATABASE inv_db;" 2>nul
if %errorlevel% neq 0 (
    echo Database already exists or error occurred
)

echo [4/4] Restoring database backup...
"%PGPATH%\psql.exe" -U postgres -d inv_db -f "%~dp0..\database\database-backup.sql"

if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo   PostgreSQL Setup Complete!
    echo ========================================
) else (
    echo [ERROR] Database restore failed!
)

echo.
pause