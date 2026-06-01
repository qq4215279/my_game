@echo off
chcp 65001 >nul
setlocal EnableExtensions
title Luban Config Generator

:: ============================================================
::  Luban 配置一键生成脚本
::
::  流程：
::    1. gen_tables.py  扫描 Datas，自动生成 __tables__.xlsx
::    2. Luban          通过 customTemplates 生成 Java 代码 + JSON 数据
::
::  环境要求：
::    - .NET SDK 6+
::    - Python 3 + openpyxl
::
::  用法：在 luban 目录下双击运行，或 cmd 执行 gen.bat
:: ============================================================

:: 路径配置（假设脚本位于 luban 目录）
set "ROOT=."
set "LUBAN_DLL=%ROOT%\Tools\Luban\Luban.dll"
set "TEMPLATES=%ROOT%\customTemplates"
set "JAVA_OUT=%ROOT%\..\..\java\com\mumu\game\luban\config"
set "JSON_OUT=%ROOT%\output\json"

echo.
echo ========== Luban Config Generator ==========
echo 工作目录: %CD%
echo.

:: 前置检查
call :require_exist "%LUBAN_DLL%" "Luban.dll"
call :require_exist "%TEMPLATES%\java-json\bean.sbn" "customTemplates"
call :require_cmd dotnet ".NET SDK"
call :require_cmd python "Python"

:: [1/2] 扫描 Excel，自动生成 __tables__.xlsx
echo [1/2] 生成 __tables__.xlsx ...
python "%ROOT%\gen_tables.py"
if errorlevel 1 goto :fail

:: [2/2] Luban 生成
::   -t server              服务端目标
::   -c java-json            Gson 版 Java
::   -d json                 JSON 数据
::   tableImporter.name=none 跳过 # 自动导入，用 __tables__.xlsx
::   customTemplateDir       自定义 import 包路径
echo [2/2] Luban 生成 ...
if not exist "%JSON_OUT%" mkdir "%JSON_OUT%"

dotnet "%LUBAN_DLL%" ^
    -t server ^
    -c java-json ^
    -d json ^
    --conf "%ROOT%\luban.conf" ^
    --customTemplateDir "%TEMPLATES%" ^
    -x tableImporter.name=none ^
    -x outputCodeDir="%JAVA_OUT%" ^
    -x outputDataDir="%JSON_OUT%"
if errorlevel 1 goto :fail

echo       Java: %JAVA_OUT%
echo       JSON: %JSON_OUT%

:ok
echo.
echo ========== 全部完成 ==========
pause
exit /b 0

:: 检查文件是否存在
:require_exist
if exist "%~1" exit /b 0
echo [错误] 找不到 %~2: %~1
goto :fail

:: 检查命令是否在 PATH 中
:require_cmd
where %~1 >nul 2>&1
if not errorlevel 1 exit /b 0
echo [错误] 未找到 %~1，需要 %~2
goto :fail

:fail
echo.
echo ========== 执行失败 ==========
pause
exit /b 1
