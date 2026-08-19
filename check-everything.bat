@echo off
REM One command that rebuilds the project, repackages the submission zip and
REM then checks it the way the grader will.
setlocal

if "%JAVA_HOME%"=="" set JAVA_HOME=C:\Program Files\Java\jdk-25.0.4.1

echo.
echo ============================================================
echo  STEP 1 of 4  -  Compiling and building the jars
echo ============================================================
call build.bat
if errorlevel 1 goto failed

echo.
echo ============================================================
echo  STEP 2 of 4  -  Rebuilding readme.docx and the submission zip
echo ============================================================
call package.bat
if errorlevel 1 goto failed

echo.
echo ============================================================
echo  STEP 3 of 4  -  Checking every sample file against the schema
echo ============================================================
powershell -ExecutionPolicy Bypass -NoProfile -File "tests\generate-edge-files.ps1"
if errorlevel 1 goto failed
powershell -ExecutionPolicy Bypass -NoProfile -File "tests\validate-against-schema.ps1"
if errorlevel 1 goto failed

echo.
echo ============================================================
echo  STEP 4 of 4  -  Running the submission zip like the grader
echo ============================================================
powershell -ExecutionPolicy Bypass -NoProfile -File "tests\run-tests.ps1"
if errorlevel 1 goto failed

echo.
echo Everything checked out. submission\GuessMarket-EX1.zip is ready to hand in.
goto end

:failed
echo.
echo Something failed above. Do not submit until it is fixed.
exit /b 1

:end
endlocal
