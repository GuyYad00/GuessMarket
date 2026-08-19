@echo off
rem Builds the Guess Market system: compiles both modules and creates
rem dist\engine.jar + dist\ui.jar alongside the third-party JAXB jars.
setlocal enabledelayedexpansion

cd /d "%~dp0"

if "%JAVA_HOME%"=="" (
    set "JAVAC=javac"
    set "JAR=jar"
) else (
    set "JAVAC=%JAVA_HOME%\bin\javac"
    set "JAR=%JAVA_HOME%\bin\jar"
)

echo Cleaning previous build...
rmdir /s /q out 2>nul
rmdir /s /q dist 2>nul
mkdir out\engine-classes
mkdir out\ui-classes
mkdir dist\lib

echo Compiling engine module...
type nul > out\engine-sources.txt
for /f "delims=" %%f in ('dir /s /b engine\src\*.java') do (
    set "p=%%f"
    echo "!p:\=/!" >> out\engine-sources.txt
)
"%JAVAC%" -cp "lib\*" -d out\engine-classes @out\engine-sources.txt
if errorlevel 1 goto :error

echo Compiling ui module...
type nul > out\ui-sources.txt
for /f "delims=" %%f in ('dir /s /b ui\src\*.java') do (
    set "p=%%f"
    echo "!p:\=/!" >> out\ui-sources.txt
)
"%JAVAC%" -cp "out\engine-classes" -d out\ui-classes @out\ui-sources.txt
if errorlevel 1 goto :error

echo Creating engine.jar...
"%JAR%" --create --file dist\engine.jar -C out\engine-classes .
if errorlevel 1 goto :error

echo Creating ui.jar...
"%JAR%" --create --file dist\ui.jar --manifest ui\MANIFEST.MF -C out\ui-classes .
if errorlevel 1 goto :error

echo Copying third-party libraries and run script...
copy /y lib\*.jar dist\lib\ >nul
copy /y run.bat dist\ >nul

echo.
echo Build completed successfully. Run the system with: dist\run.bat
exit /b 0

:error
echo.
echo BUILD FAILED.
exit /b 1
