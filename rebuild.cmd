@echo off
cd /d "e:\Codebuddy-work space\campus-trade\campus-trade-server"
echo Deleting old target...
rmdir /s /q target 2>nul
echo Building...
call mvn clean package -DskipTests
echo Exit code: %ERRORLEVEL%
pause
