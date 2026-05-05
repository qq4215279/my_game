@echo off
@REM ========================================
@REM Luban Config Generator Script
@REM Generate Java code and JSON data from Excel
@REM ========================================

title Luban Config Generator

echo ========================================
echo   Luban Config Generator
echo ========================================
echo.

@REM Set working directory
set WORKSPACE=.
echo [1/5] Working directory: %CD%
echo.

@REM Luban tool path
set LUBAN_DLL=%WORKSPACE%\Tools\Luban\Luban.dll
echo [2/5] Checking Luban.dll...
if not exist "%LUBAN_DLL%" (
    echo [ERROR] Luban.dll not found: %LUBAN_DLL%
    echo Please confirm Luban.dll exists in Tools\Luban directory
    pause
    exit /b 1
)
echo [OK] Luban.dll exists
echo.

@REM Check dotnet installation
echo [3/5] Checking dotnet environment...
where dotnet >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] dotnet command not found!
    echo Please install .NET SDK 6.0 or higher
    echo Download: https://dotnet.microsoft.com/download
    pause
    exit /b 1
)
echo [OK] dotnet installed
dotnet --version
echo.

@REM Config file root
set CONF_ROOT=%WORKSPACE%
echo [4/5] Checking config file...
if not exist "%CONF_ROOT%\luban.conf" (
    echo [ERROR] luban.conf not found: %CONF_ROOT%\luban.conf
    pause
    exit /b 1
)
echo [OK] luban.conf exists
echo.

@REM Ensure output directories exist
echo [5/5] Creating output directories...
if not exist "%WORKSPACE%\output\json" (
    mkdir "%WORKSPACE%\output\json"
    echo [CREATE] output\json
) else (
    echo [EXISTS] output\json
)

if not exist "%WORKSPACE%\..\..\java\com\mumu\game\luban\cfg" (
    mkdir "%WORKSPACE%\..\..\java\com\mumu\game\luban\cfg"
    echo [CREATE] java\com\mumu\game\luban\cfg
) else (
    echo [EXISTS] java\com\mumu\game\luban\cfg
)
echo.

echo ========================================
echo   Start generating configs...
echo ========================================
echo.

@REM Execute Luban generation
dotnet "%LUBAN_DLL%" ^
    -t server ^
    -c java-json ^
    -d json ^
    --conf "%CONF_ROOT%\luban.conf" ^
    -x outputCodeDir="%WORKSPACE%\..\..\java\com\mumu\game\luban\cfg" ^
    -x outputDataDir="%WORKSPACE%\output\json"

@REM Check execution result
if %errorlevel% equ 0 (
    echo.
    echo ========================================
    echo   Generation SUCCESS!
    echo ========================================
    echo Code output: %WORKSPACE%\..\..\java\com\mumu\game\luban\cfg
    echo Data output: %WORKSPACE%\output\json
) else (
    echo.
    echo ========================================
    echo   Generation FAILED! Error code: %errorlevel%
    echo ========================================
    echo Please check error messages above
)

echo.
pause
