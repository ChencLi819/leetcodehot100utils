@echo off
rem 一键运行全部回归测试（合并 / import 补全 / 骨架生成 / 编译器 E2E）
chcp 65001 >nul
cd /d %~dp0

if not exist out mkdir out

echo [1/2] 编译源码与测试...
dir /s /b src\*.java test\*.java > out\sources.txt
javac -encoding UTF-8 -cp "lib\flatlaf-3.4.1.jar" -d out @out\sources.txt
if errorlevel 1 (echo [错误] 编译失败 & del out\sources.txt & pause & exit /b 1)
del out\sources.txt

echo [2/2] 运行测试...
java -cp "out;lib\flatlaf-3.4.1.jar" TestMerge
if errorlevel 1 goto :fail
java -cp "out;lib\flatlaf-3.4.1.jar" TestEnsureImports
if errorlevel 1 goto :fail
java -cp "out;lib\flatlaf-3.4.1.jar" TestStub
if errorlevel 1 goto :fail
java -Dfile.encoding=UTF-8 -cp "out;lib\flatlaf-3.4.1.jar" RunCompilerTest
if errorlevel 1 goto :fail

echo.
echo 全部测试通过 ✓
pause
exit /b 0

:fail
echo.
echo [错误] 存在失败的测试
pause
exit /b 1
