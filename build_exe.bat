@echo off
rem 一键重新打包 Hot100.exe（需要 JDK 17+ 自带 jpackage，推荐与本机 java 版本一致）
chcp 65001 >nul
cd /d %~dp0

echo [1/4] 编译源码...
if not exist out mkdir out
dir /s /b src\*.java > out\sources.txt
javac -encoding UTF-8 -cp "lib\flatlaf-3.4.1.jar" -d out @out\sources.txt
if errorlevel 1 (echo [错误] 编译失败 & del out\sources.txt & pause & exit /b 1)
del out\sources.txt

echo [2/4] 准备打包目录...
if exist input rmdir /s /q input
if exist dist rmdir /s /q dist
mkdir input\lib
copy /y lib\flatlaf-3.4.1.jar input\lib\ >nul
xcopy /e /i /y data input\data >nul
echo Class-Path: lib/flatlaf-3.4.1.jar> manifest.txt

echo [3/4] 打包 Hot100.jar...
jar --create --file input\Hot100.jar --manifest manifest.txt --main-class hot100.Hot100App -C out hot100
if errorlevel 1 (echo [错误] 打 jar 失败 & pause & exit /b 1)

echo [4/4] 生成 Hot100.exe（jpackage app-image）...
rem 注意：本机 PATH 里的 jpackage 可能是 JDK25，需与 javac(26) 同版本；--add-modules 必须带上
rem jdk.compiler（内置编译器答题区）与 jdk.zipfs（javac 读 jar 依赖）
set JP=jpackage
if exist "D:\javafiles\JDK26\bin\jpackage.exe" set JP=D:\javafiles\JDK26\bin\jpackage.exe
"%JP%" --type app-image --name Hot100 --input input --main-jar Hot100.jar --main-class hot100.Hot100App --app-version 1.3.0 --vendor "Hot100" --icon icon.ico --dest dist --java-options "-Dfile.encoding=UTF-8" --java-options "--enable-native-access=ALL-UNNAMED" --add-modules jdk.compiler,jdk.zipfs
if errorlevel 1 (echo [错误] jpackage 失败 & pause & exit /b 1)

del manifest.txt
echo.
echo 打包完成: dist\Hot100\Hot100.exe （可直接双击运行）
pause
