@echo off
@REM ========================================
@REM 鲁班配置表生成脚本
@REM 功能：从 Excel 表格生成 Java 代码和 JSON 数据
@REM ========================================

title 鲁班配置表生成工具

echo ========================================
echo   鲁班配置表生成工具
echo ========================================
echo.

@REM 设置工作目录（当前脚本所在目录）
set WORKSPACE=.
echo [1/5] 工作目录: %CD%
echo.

@REM 鲁班工具路径
set LUBAN_DLL=%WORKSPACE%\Tools\Luban\Luban.dll
echo [2/5] 检查 Luban.dll...
if not exist "%LUBAN_DLL%" (
    echo [错误] 找不到 Luban.dll: %LUBAN_DLL%
    echo 请确认 Tools\Luban 目录下存在 Luban.dll
    pause
    exit /b 1
)
echo [成功] Luban.dll 存在
echo.

@REM 检查 dotnet 是否安装
echo [3/5] 检查 dotnet 环境...
where dotnet >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 dotnet 命令！
    echo 请先安装 .NET SDK 6.0 或更高版本
    echo 下载地址: https://dotnet.microsoft.com/download
    pause
    exit /b 1
)
echo [成功] dotnet 已安装
dotnet --version
echo.

@REM 配置文件根目录
set CONF_ROOT=%WORKSPACE%
echo [4/5] 检查配置文件...
if not exist "%CONF_ROOT%\luban.conf" (
    echo [错误] 找不到 luban.conf: %CONF_ROOT%\luban.conf
    pause
    exit /b 1
)
echo [成功] luban.conf 存在
echo.

@REM 确保输出目录存在
echo [5/5] 创建输出目录...
if not exist "%WORKSPACE%\output\json" (
    mkdir "%WORKSPACE%\output\json"
    echo [创建] output\json
) else (
    echo [存在] output\json
)

if not exist "%WORKSPACE%\..\..\java\com\mumu\game\luban\cfg" (
    mkdir "%WORKSPACE%\..\..\java\com\mumu\game\luban\cfg"
    echo [创建] java\com\mumu\game\luban\cfg
) else (
    echo [存在] java\com\mumu\game\luban\cfg
)
echo.

echo ========================================
echo   开始生成配置...
echo ========================================
echo.

@REM 执行鲁班生成
@REM -t all: 生成所有组（client, server, editor）
@REM -c java-json: 生成 Java 代码，使用 JSON 数据格式
@REM -d json: 数据导出格式为 JSON
@REM --conf: 指定配置文件
@REM -x outputCodeDir: 代码输出目录
@REM -x outputDataDir: 数据输出目录
dotnet "%LUBAN_DLL%" ^
    -t all ^
    -c java-json ^
    -d json ^
    --conf "%CONF_ROOT%\luban.conf" ^
    -x outputCodeDir="%WORKSPACE%\..\..\java\com\mumu\game\luban\cfg" ^
    -x outputDataDir="%WORKSPACE%\output\json"

@REM 检查执行结果
if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo   生成成功！
    echo ========================================
    echo 代码输出: %WORKSPACE%\..\..\java\com\mumu\game\luban\cfg
    echo 数据输出: %WORKSPACE%\output\json
) else (
    echo.
    echo ========================================
    echo   生成失败！错误码: %errorlevel%
    echo ========================================
    echo 请检查上面的错误信息
)

echo.
pause
