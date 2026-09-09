@echo off
rem LeetCode 热题 100 刷题助手 —— 开发模式一键运行（需本机安装 JDK 17+，推荐 JDK 21/26）
chcp 65001 >nul
cd /d %~dp0

if not exist out mkdir out

rem 收集源码列表
dir /s /b src\*.java > out\sources.txt

javac -encoding UTF-8 -cp "lib\flatlaf-3.4.1.jar" -d out @out\sources.txt
if errorlevel 1 (
    echo [错误] 编译失败，请检查 JDK 是否安装。
    del out\sources.txt
    pause
    exit /b 1
)
del out\sources.txt

java -cp "out;lib\flatlaf-3.4.1.jar" hot100.Hot100App
