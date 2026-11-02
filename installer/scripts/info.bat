@echo off
echo ==========================================
echo  CFT Electronic - Network Info
echo ==========================================
echo.
echo  Server PC (this machine):  http://localhost:8080
echo.
echo  Other PCs on the same WiFi/LAN use:
echo.
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /i "IPv4"') do (
    set ip=%%a
    setlocal enabledelayedexpansion
    set ip=!ip: =!
    echo     http://!ip!:8080
    endlocal
)
echo.
echo  The vendor logs in at that address with role: SELLER
echo ==========================================
echo.
pause
