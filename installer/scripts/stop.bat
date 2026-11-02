@echo off
echo Stopping CFT Electronic backend...
REM Stops only the Java process running this installation's backend.jar, never other Java programs.
REM The path is matched in its long and short (8.3) forms, since Windows may report either one.
powershell -NoProfile -ExecutionPolicy Bypass -Command "$app = Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'javaw.exe' -and ($_.CommandLine -like '*%~dp0app\backend.jar*' -or $_.CommandLine -like '*%~sdp0app\backend.jar*') }; if ($app) { $app | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }; exit 0 } else { exit 1 }"
if %errorlevel% == 0 (
    echo Server stopped.
) else (
    echo No running server found.
)
timeout /t 2 /nobreak >nul
