@echo off
echo Stopping CFT Electronic backend...
taskkill /F /IM javaw.exe /T >nul 2>&1
if %errorlevel% == 0 (
    echo Server stopped.
) else (
    echo No running server found.
)
timeout /t 2 /nobreak >nul
