# 校园二手交易平台 - 一键启动脚本 (PowerShell)
# 使用方法: 右键 -> 使用 PowerShell 运行
# 或者在终端执行: powershell -ExecutionPolicy Bypass -File start.ps1

$ErrorActionPreference = "Stop"
$Host.UI.RawUI.WindowTitle = "校园二手交易平台 - 启动中..."

$PROJECT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$SERVER_DIR  = Join-Path $PROJECT_DIR "campus-trade-server"
$UI_DIR      = Join-Path $PROJECT_DIR "campus-trade-ui"
$BACKEND_PORT = 8080
$FRONTEND_PORT = 5173

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "   校园二手交易平台 - 正在启动..." -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ──── 1. 检查 MySQL ────
Write-Host "[1/3] 检查 MySQL 数据库..." -ForegroundColor Green
$mysqlService = Get-Service -Name "MySQL*" -ErrorAction SilentlyContinue
if (-not $mysqlService) {
    Write-Host "[错误] 未找到 MySQL 服务！请确认 MySQL 已安装。" -ForegroundColor Red
    Read-Host "按回车退出"
    exit 1
}
if ($mysqlService.Status -ne "Running") {
    Write-Host "[提示] MySQL 服务未运行，正在启动..." -ForegroundColor Yellow
    Start-Service -Name $mysqlService.Name
    Start-Sleep -Seconds 3
}
Write-Host "[√] MySQL 数据库已就绪" -ForegroundColor Green
Write-Host ""

# ──── 2. 编译并启动后端 ────
Write-Host "[2/3] 编译并启动后端 (端口 $BACKEND_PORT)..." -ForegroundColor Green

# 检查后端是否已在运行
$existingBackend = netstat -ano | Select-String ":$BACKEND_PORT " | Select-String "LISTENING"
if ($existingBackend) {
    Write-Host "[提示] 端口 $BACKEND_PORT 已被占用，尝试关闭旧进程..." -ForegroundColor Yellow
    $pidMatch = [regex]::Match($existingBackend, '\s+(\d+)\s*$')
    if ($pidMatch.Success) {
        Stop-Process -Id $pidMatch.Groups[1].Value -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }
}

Set-Location $SERVER_DIR
Write-Host "  正在编译后端 (可能需要 1-2 分钟)..." -ForegroundColor Gray
mvn package -DskipTests -q 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "[错误] 后端编译失败！请检查 Maven 配置。" -ForegroundColor Red
    Set-Location $PROJECT_DIR
    Read-Host "按回车退出"
    exit 1
}
Write-Host "[√] 后端编译完成" -ForegroundColor Green

$jarFile = Get-ChildItem "$SERVER_DIR\target\campus-trade-server-*.jar" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $jarFile) {
    Write-Host "[错误] 未找到 JAR 文件！" -ForegroundColor Red
    Set-Location $PROJECT_DIR
    Read-Host "按回车退出"
    exit 1
}

# 后台启动后端
$backendProcess = Start-Process -FilePath "java" -ArgumentList "-jar", $jarFile.FullName -NoNewWindow -PassThru
Write-Host "[√] 后端启动中 (PID: $($backendProcess.Id))，请稍候..." -ForegroundColor Green

# 等待后端就绪
Write-Host "  等待后端就绪..." -ForegroundColor Gray
$maxWait = 60
$waited = 0
do {
    Start-Sleep -Seconds 2
    $waited += 2
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$BACKEND_PORT/api/product/list" `
            -Method POST -ContentType "application/json" -Body '{"page":1,"size":1}' `
            -TimeoutSec 3 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) { break }
    } catch { }
    if ($waited -ge $maxWait) {
        Write-Host "[警告] 后端启动超时，请手动检查！" -ForegroundColor Yellow
        break
    }
} while ($true)
Write-Host "[√] 后端已就绪 (http://localhost:$BACKEND_PORT)" -ForegroundColor Green
Write-Host ""

# ──── 3. 启动前端 ────
Write-Host "[3/3] 启动前端 (端口 $FRONTEND_PORT)..." -ForegroundColor Green

# 检查前端是否已在运行
$existingFrontend = netstat -ano | Select-String ":$FRONTEND_PORT " | Select-String "LISTENING"
if ($existingFrontend) {
    Write-Host "[提示] 端口 $FRONTEND_PORT 已被占用，尝试关闭旧进程..." -ForegroundColor Yellow
    $pidMatch = [regex]::Match($existingFrontend, '\s+(\d+)\s*$')
    if ($pidMatch.Success) {
        Stop-Process -Id $pidMatch.Groups[1].Value -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 2
    }
}

# 检查 node_modules
if (-not (Test-Path (Join-Path $UI_DIR "node_modules"))) {
    Write-Host "  首次运行，正在安装前端依赖..." -ForegroundColor Gray
    Set-Location $UI_DIR
    npm install --silent 2>&1 | Out-Null
}

Set-Location $UI_DIR
$frontendProcess = Start-Process -FilePath "npm" -ArgumentList "run", "dev" -NoNewWindow -PassThru
Write-Host "[√] 前端启动中 (PID: $($frontendProcess.Id))，请稍候..." -ForegroundColor Green

# 等待前端就绪
Write-Host "  等待前端就绪..." -ForegroundColor Gray
$maxWait = 30
$waited = 0
do {
    Start-Sleep -Seconds 2
    $waited += 2
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$FRONTEND_PORT" -TimeoutSec 3 -ErrorAction SilentlyContinue
        if ($response.StatusCode -eq 200) { break }
    } catch { }
    if ($waited -ge $maxWait) {
        Write-Host "[警告] 前端启动超时，请手动检查！" -ForegroundColor Yellow
        break
    }
} while ($true)
Write-Host "[√] 前端已就绪 (http://localhost:$FRONTEND_PORT)" -ForegroundColor Green
Write-Host ""

# ──── 完成 ────
Set-Location $PROJECT_DIR

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "   启动完成！" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  前端地址 (本地):  http://localhost:$FRONTEND_PORT" -ForegroundColor White
Write-Host "  后端地址 (API):   http://localhost:$BACKEND_PORT" -ForegroundColor White
Write-Host ""
Write-Host "按回车键退出此窗口（服务不会停止）" -ForegroundColor Gray
Read-Host
