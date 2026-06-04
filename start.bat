@echo off
chcp 65001 >nul
title 校园二手交易平台 - 一键启动

echo ============================================
echo   校园二手交易平台 - 正在启动...
echo ============================================
echo.

:: 设置项目根目录
set "PROJECT_DIR=%~dp0"
set "SERVER_DIR=%PROJECT_DIR%campus-trade-server"
set "UI_DIR=%PROJECT_DIR%campus-trade-ui"

:: 检查 MySQL 是否运行
echo [1/3] 检查 MySQL 数据库...
sc query MySQL | findstr "RUNNING" >nul
if %errorlevel% neq 0 (
    echo [警告] MySQL 服务未运行，正在尝试启动...
    net start MySQL >nul 2>&1
    if %errorlevel% neq 0 (
        echo [错误] MySQL 启动失败，请手动启动 MySQL 服务！
        pause
        exit /b 1
    )
)
echo [√] MySQL 数据库已就绪
echo.

:: 编译并启动后端
echo [2/3] 编译后端...
cd /d "%SERVER_DIR%"
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo [错误] 后端编译失败！
    pause
    exit /b 1
)
echo [√] 后端编译完成

echo 启动后端服务 (端口 8080)...
start "校园二手-后端" java -jar "%SERVER_DIR%\target\campus-trade-server-1.0.0.jar"
echo [√] 后端启动中，请稍候...
echo.

:: 等待后端就绪
echo 等待后端就绪...
:wait_backend
timeout /t 2 /nobreak >nul
curl -s -o NUL -w "%%{http_code}" http://localhost:8080/api/ai/chat -X POST -H "Content-Type: application/json" -d "{\"question\":\"ping\"}" 2>nul | findstr "200" >nul
if %errorlevel% neq 0 goto wait_backend
echo [√] 后端已就绪
echo.

:: 启动前端
echo [3/3] 启动前端 (端口 5173)...
cd /d "%UI_DIR%"
start "校园二手-前端" cmd /c "npm run dev"
echo [√] 前端启动中...
echo.

:: 等待前端就绪
echo 等待前端就绪...
:wait_frontend
timeout /t 2 /nobreak >nul
curl -s -o NUL -w "%%{http_code}" http://localhost:5173 2>nul | findstr "200" >nul
if %errorlevel% neq 0 goto wait_frontend
echo [√] 前端已就绪
echo.

:: 打开浏览器
echo 正在打开浏览器...
start http://localhost:5173

echo.
echo ============================================
echo   启动完成！
echo   前端地址: http://localhost:5173
echo   后端地址: http://localhost:8080
echo ============================================
echo.
echo 按任意键关闭此窗口（不会停止服务）
pause >nul
