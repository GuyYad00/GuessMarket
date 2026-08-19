@echo off
rem Packages the submission: rebuilds readme.docx from readme-src and zips dist\.
rem Run build.bat first so that dist\ holds up-to-date jars.
setlocal

cd /d "%~dp0"

if not exist dist\ui.jar (
    echo dist\ui.jar is missing. Run build.bat first.
    exit /b 1
)

echo Building readme.docx from readme-src...
powershell -NoProfile -Command ^
  "Add-Type -AssemblyName System.IO.Compression.FileSystem;" ^
  "$t = Join-Path $PWD 'dist\readme.docx';" ^
  "Remove-Item $t -ErrorAction SilentlyContinue;" ^
  "$z = [System.IO.Compression.ZipFile]::Open($t, 'Create');" ^
  "foreach ($p in @('[Content_Types].xml','_rels/.rels','word/document.xml')) {" ^
  "  $src = Join-Path $PWD ('readme-src\' + $p.Replace('/','\'));" ^
  "  [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($z, $src, $p) | Out-Null }" ^
  "$z.Dispose()"
if errorlevel 1 goto :error

echo Creating submission zip...
if not exist submission mkdir submission
del /q submission\GuessMarket-EX1.zip 2>nul
tar -a -c -f submission\GuessMarket-EX1.zip -C dist .
if errorlevel 1 goto :error

echo.
echo Submission package ready: submission\GuessMarket-EX1.zip
exit /b 0

:error
echo.
echo PACKAGING FAILED.
exit /b 1
