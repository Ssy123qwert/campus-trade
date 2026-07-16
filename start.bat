@echo off
title Campus Trade Platform - Starting...

echo ====================================
echo   Campus Trade Platform v2.0
echo ====================================
echo.

:: Check Maven
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Maven not found. Please install Maven or check PATH.
    pause
    exit /b 1
)

:: Check Redis
echo [1/3] Checking Redis...
tasklist /FI "IMAGENAME eq redis-server.exe" 2>nul | find /I "redis-server.exe" >nul
if %errorlevel% neq 0 (
    echo   - Redis not running, starting...
    start /B "" "E:\Codebuddy-work space\redis-data\redis-server.exe" "E:\Codebuddy-work space\redis-data\redis.windows.conf"
    ping -n 4 127.0.0.1 >nul
    echo   [OK] Redis started
) else (
    echo   [OK] Redis is running
)

:: Check MySQL
echo.
echo [2/3] Checking MySQL...
sc query MYSQL80 2>nul | find "RUNNING" >nul
if %errorlevel% neq 0 (
    echo   - MySQL not running, starting...
    net start MYSQL80 >nul 2>nul
    if %errorlevel% equ 0 (
        echo   [OK] MySQL started
    ) else (
        echo   [!] MySQL start failed, please start manually
    )
) else (
    echo   [OK] MySQL is running
)

:: Kill old Java process (port 8080)
echo.
echo [3/4] Stopping old server...
taskkill /F /IM "java.exe" >nul 2>nul
ping -n 3 127.0.0.1 >nul
echo   [OK] Old process stopped

:: Start backend
echo.
echo [4/4] Starting backend...
cd /d "E:\Codebuddy-work space\campus-trade\campus-trade-server"
if %errorlevel% neq 0 (
    echo [ERROR] Cannot find project directory
    pause
    exit /b 1
)

start "CampusTrade-Backend" cmd /k "echo Building and starting... && mvn spring-boot:run"

:: Wait and open browser
echo.
echo Waiting for backend (15s)...
ping -n 16 127.0.0.1 >nul

echo Opening browser...
start http://localhost:8080

echo.
echo ====================================
echo   [OK] All done!
echo   URL: http://localhost:8080
echo   Admin: root / 123456
echo   User:  test / 123456
echo   API:   http://localhost:8080/doc.html
echo ====================================
echo.
echo Close the backend window to stop the server.
echo This window can be closed safely.
pause
