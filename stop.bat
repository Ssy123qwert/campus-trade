@echo off
chcp 65001 >nul
title 校园二手交易平台 - 停止服务

echo ============================================
echo   正在停止校园二手交易平台...
echo ============================================
echo.

echo 停止后端服务 (Java)...
taskkill /f /fi "WINDOWTITLE eq 校园二手-后端*" >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
    taskkill /f /pid %%a >nul 2>&1
)

echo 停止前端服务 (Node/Vite)...
taskkill /f /fi "WINDOWTITLE eq 校园二手-前端*" >nul 2>&1
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING"') do (
    taskkill /f /pid %%a >nul 2>&1
)

echo.
echo [√] 所有服务已停止
echo.
pause
