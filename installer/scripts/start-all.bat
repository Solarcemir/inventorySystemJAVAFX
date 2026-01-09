@echo off
title Inventory Management System
color 0A
cd /d "%~dp0.."

echo.
echo  ========================================
echo     INVENTORY MANAGEMENT SYSTEM
echo  ========================================
echo.

REM Verificar que PostgreSQL esté corriendo
echo [1/3] Checking PostgreSQL...
sc query postgresql-x64-15 | find "RUNNING" >nul
if %errorlevel% neq 0 (
    color 0C
    echo [ERROR] PostgreSQL is not running!
    echo Starting PostgreSQL service...
    net start postgresql-x64-15
    timeout /t 5 /nobreak >nul
)
echo        PostgreSQL OK

REM Iniciar Backend en ventana minimizada
echo [2/3] Starting Backend Server...
start "Backend Server" /min cmd /c "%~dp0start-backend.bat"
echo        Waiting for backend to start...
timeout /t 12 /nobreak >nul

REM Iniciar Frontend
echo [3/3] Launching Application...
call "%~dp0start-frontend.bat"

echo.
echo  ========================================
echo     System Started Successfully!
echo  ========================================
echo.
timeout /t 3 >nul
exit
