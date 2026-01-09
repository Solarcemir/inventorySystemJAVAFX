@echo off
cd /d "%~dp0.."

REM Usar el JRE incluido en el instalador
set JAVA_HOME=%~dp0..\jre
set PATH=%JAVA_HOME%\bin;%PATH%

REM Esperar un momento por si el backend está iniciando
timeout /t 2 /nobreak >nul

REM Iniciar el frontend con JavaFX usando javaw (sin consola, en segundo plano)
start "Inventory System" "%JAVA_HOME%\bin\javaw.exe" -jar frontend\frontend.jar

REM Salir inmediatamente sin esperar
exit
