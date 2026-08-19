# Acceptance tests for Guess Market EX1.
# Extracts the actual submission ZIP into a clean folder whose path contains a
# space, then drives the console app exactly the way the grader would.

$ErrorActionPreference = "Stop"

$repo     = Split-Path $PSScriptRoot -Parent
$zip      = Join-Path $repo "submission\GuessMarket-EX1.zip"
$xmlDir   = Join-Path $repo "test-files"
$sandbox  = Join-Path $env:TEMP "gm grader sandbox"
$javaHome = "C:\Program Files\Java\jdk-25.0.4.1"

Remove-Item -Recurse -Force $sandbox -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $sandbox | Out-Null
tar -x -f $zip -C $sandbox
$env:Path = "$javaHome\bin;$env:Path"

$script:pass = 0
$script:fail = 0
$script:failures = @()

function Invoke-App([string[]] $inputLines) {
    $inFile = Join-Path $sandbox "_in.txt"
    Set-Content -Path $inFile -Value $inputLines -Encoding ASCII
    # A crash writes to stderr; capture it as text instead of letting it abort the run.
    $old = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $out = cmd /c "cd /d ""$sandbox"" && run.bat < ""$inFile"" 2>&1"
    $ErrorActionPreference = $old
    return ($out | Out-String)
}

function Test-Case([string] $name, [string[]] $inputLines, [hashtable] $expect) {
    $out = Invoke-App $inputLines
    $problems = @()

    # A stack trace means the program crashed - an automatic Level 0 failure.
    if ($out -match "Exception in thread|\tat [a-z]+\.") {
        $problems += "CRASHED (stack trace in output)"
    }
    foreach ($needle in $expect.must) {
        if ($out -notlike "*$needle*") { $problems += "missing: '$needle'" }
    }
    foreach ($needle in $expect.mustNot) {
        if ($out -like "*$needle*") { $problems += "should not contain: '$needle'" }
    }

    if ($problems.Count -eq 0) {
        Write-Host ("  [PASS] " + $name) -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host ("  [FAIL] " + $name) -ForegroundColor Red
        $problems | ForEach-Object { Write-Host ("         " + $_) -ForegroundColor Red }
        $script:fail++
        $script:failures += $name
        Set-Content -Path (Join-Path $sandbox "_fail_$($script:fail).log") -Value $out
    }
}

$valid3  = Join-Path $xmlDir "valid-3-events.xml"
$valid1  = Join-Path $xmlDir "valid-1-event.xml"
$dupId   = Join-Path $xmlDir "invalid-dup-id.xml"
$badComm = Join-Path $xmlDir "invalid-commission.xml"
$negComm = Join-Path $xmlDir "invalid-negative-commission.xml"
$notXml  = Join-Path $xmlDir "not-an-xml.txt"
$stateFile = Join-Path $sandbox "saved-state"

Write-Host ""
Write-Host "=== A. Loading and file validation ===" -ForegroundColor Cyan

Test-Case "A1  valid file loads" @("1", $valid3, "6") @{
    must = @("valid and loaded successfully"); mustNot = @("Error:")
}

Test-Case "A2  duplicate event id rejected, says which id" @("1", $dupId, "6") @{
    must = @("Error:", "Duplicate event id"); mustNot = @("loaded successfully")
}

Test-Case "A3  commission above 90 rejected, says the value" @("1", $badComm, "6") @{
    must = @("Error:", "commission", "115"); mustNot = @("loaded successfully")
}

Test-Case "A4  negative commission rejected" @("1", $negComm, "6") @{
    must = @("Error:", "commission"); mustNot = @("loaded successfully")
}

Test-Case "A5  non-xml extension rejected" @("1", $notXml, "6") @{
    must = @("Error:", "not an XML file"); mustNot = @("loaded successfully")
}

Test-Case "A6  missing file rejected" @("1", "C:\no\such\file.xml", "6") @{
    must = @("Error:", "does not exist"); mustNot = @("loaded successfully")
}

Test-Case "A7  empty path rejected" @("1", "", "6") @{
    must = @("Error:"); mustNot = @("loaded successfully")
}

Test-Case "A8  a directory instead of a file is rejected" @("1", $xmlDir, "6") @{
    must = @("Error:"); mustNot = @("loaded successfully")
}

Test-Case "A9  bad file does NOT wipe the previously loaded good file" `
    @("1", $valid3, "1", $dupId, "2", "6") @{
    must = @("Mujtaba is Dead", "World Cap Winner", "Earth Quake on Dead Sea")
    mustNot = @()
}

# valid-1-event.xml holds only the Earth Quake event, so after it overrides
# valid-3-events.xml the other two events must be gone.
Test-Case "A10 a good file DOES override the previous one" `
    @("1", $valid3, "1", $valid1, "2", "6") @{
    must = @("Earth Quake on Dead Sea")
    mustNot = @("Mujtaba is Dead", "World Cap Winner")
}

Write-Host ""
Write-Host "=== A*. The lecturer's own four sample files (exact names) ===" -ForegroundColor Cyan

Test-Case "A*1 multiple.xml (lecturer valid, 3 events) loads" `
    @("1", (Join-Path $xmlDir "multiple.xml"), "2", "6") @{
    must = @("loaded successfully", "Mujtaba is Dead", "World Cap Winner", "Earth Quake on Dead Sea")
    mustNot = @("Error:")
}

Test-Case "A*2 single.xml (lecturer valid, 1 event) loads" `
    @("1", (Join-Path $xmlDir "single.xml"), "2", "6") @{
    must = @("loaded successfully", "Earth Quake on Dead Sea")
    mustNot = @("Error:")
}

Test-Case "A*3 error-2.xml (lecturer, commission 115) is refused" `
    @("1", (Join-Path $xmlDir "error-2.xml"), "6") @{
    must = @("Error:", "115"); mustNot = @("loaded successfully")
}

Test-Case "A*4 error-3.xml (lecturer, duplicate id) is refused" `
    @("1", (Join-Path $xmlDir "error-3.xml"), "6") @{
    must = @("Error:", "Duplicate event id"); mustNot = @("loaded successfully")
}

Write-Host ""
Write-Host "=== B. Commands with no file loaded ===" -ForegroundColor Cyan

foreach ($cmd in @("2", "3", "4", "5", "7")) {
    Test-Case "B   command $cmd before loading gives a clear error" @($cmd, "6") @{
        must = @("No system file is currently loaded"); mustNot = @()
    }
}

Write-Host ""
Write-Host "=== C. LMSR maths against the example in the assignment ===" -ForegroundColor Cyan

Test-Case "C1  fresh event shows both options at 0.50" `
    @("1", $valid3, "3", "1", "6") @{
    must = @("0.50"); mustNot = @()
}

Test-Case "C2  buying 100 shares at b=100 costs 62.01 (assignment example)" `
    @("1", $valid3, "4", "1", "1", "100", "6") @{
    must = @("62.01"); mustNot = @()
}

Test-Case "C3  5% on-purchase commission = 3.10, total paid 65.11" `
    @("1", $valid3, "4", "1", "1", "100", "6") @{
    must = @("3.10", "65.11"); mustNot = @()
}

Test-Case "C4  price moves to 0.73 / 0.27 after the buy" `
    @("1", $valid3, "4", "1", "1", "100", "3", "1", "6") @{
    must = @("0.73", "0.27"); mustNot = @()
}

Test-Case "C5  closing pays 100 to winners and leaves balance at -34.89" `
    @("1", $valid3, "4", "1", "1", "100", "5", "1", "1", "6") @{
    must = @("100.00", "-34.89"); mustNot = @()
}

Test-Case "C6  on-close commission: 50 shares at 15% pays 42.50, keeps 7.50" `
    @("1", $valid3, "4", "2", "1", "50", "5", "2", "1", "6") @{
    must = @("42.50", "7.50"); mustNot = @()
}

Write-Host ""
Write-Host "=== D. Display requirements ===" -ForegroundColor Cyan

Test-Case "D1  show events lists all fields" @("1", $valid3, "2", "6") @{
    must = @("Mujtaba is Dead", "on-purchase", "on-close", "Argentina", "Active")
    mustNot = @()
}

Test-Case "D2  trade history is newest-first" `
    @("1", $valid3, "4", "1", "1", "10", "4", "1", "2", "20", "3", "1", "6") @{
    must = @("20", "10"); mustNot = @()
}

Test-Case "D3  closed event reports its winner" `
    @("1", $valid3, "5", "1", "2", "3", "1", "6") @{
    must = @("No way !", "Closed"); mustNot = @()
}

Test-Case "D4  a closed event is not offered for buying" `
    @("1", $valid1, "5", "1", "1", "4", "6") @{
    must = @("Error:"); mustNot = @()
}

Test-Case "D5  no ANSI colour codes anywhere in the output" `
    @("1", $valid3, "2", "3", "1", "6") @{
    must = @(); mustNot = @([char]27)
}

Write-Host ""
Write-Host "=== E. Garbage input must never crash ===" -ForegroundColor Cyan

Test-Case "E1  letters at the menu prompt" @("abc", "6") @{
    must = @("Error:"); mustNot = @()
}

Test-Case "E2  out-of-range menu number" @("99", "6") @{
    must = @("Error:"); mustNot = @()
}

Test-Case "E3  letters where a share amount is expected" `
    @("1", $valid3, "4", "1", "1", "abc", "10", "6") @{
    must = @("not a whole number"); mustNot = @()
}

Test-Case "E4  zero and negative share amounts" `
    @("1", $valid3, "4", "1", "1", "0", "-5", "10", "6") @{
    must = @("positive"); mustNot = @()
}

Test-Case "E5  non-existent event number" @("1", $valid3, "3", "99", "6") @{
    must = @("Error:"); mustNot = @()
}

Test-Case "E6  non-existent option number" `
    @("1", $valid3, "4", "1", "99", "6") @{
    must = @("Error:"); mustNot = @()
}

Test-Case "E7  huge share amount does not overflow into Infinity/NaN" `
    @("1", $valid3, "4", "1", "1", "999999999", "3", "1", "6") @{
    must = @(); mustNot = @("Infinity", "NaN")
}

Test-Case "E8  many small buys keep prices in the 0..1 range" `
    @("1", $valid3, "4", "1", "1", "500", "4", "1", "2", "500", "3", "1", "6") @{
    must = @("0.50"); mustNot = @("Infinity", "NaN")
}

Test-Case "E9  stream ends without an explicit exit (grader closes the window)" `
    @("1", $valid3, "2") @{
    must = @(); mustNot = @()
}

Write-Host ""
Write-Host "=== F. Bonus: save and load system state ===" -ForegroundColor Cyan

Test-Case "F1  state is saved to file" `
    @("1", $valid3, "4", "1", "1", "100", "7", $stateFile, "6") @{
    must = @("saved"); mustNot = @("Error:")
}

if (Test-Path "$stateFile.gm") {
    Write-Host "  [PASS] F2  saved file got its extension automatically (.gm)" -ForegroundColor Green
    $script:pass++
} else {
    Write-Host "  [FAIL] F2  expected '$stateFile.gm' to exist" -ForegroundColor Red
    $script:fail++; $script:failures += "F2"
}

Test-Case "F3  state reloads and restores events AND trade history" `
    @("8", $stateFile, "2", "3", "1", "6") @{
    must = @("Mujtaba is Dead", "Earth Quake on Dead Sea", "65.11")
    mustNot = @("Error:")
}

Test-Case "F4  loading state replaces a different loaded file" `
    @("1", $valid1, "8", $stateFile, "2", "6") @{
    must = @("Earth Quake on Dead Sea"); mustNot = @()
}

Test-Case "F5  loading a state file that does not exist is handled" `
    @("8", "C:\no\such\state", "6") @{
    must = @("Error:"); mustNot = @()
}

Write-Host ""
Write-Host "=== G. Schema-valid files the grader may throw at us ===" -ForegroundColor Cyan

$edge = Join-Path $repo "test-files\edge"

# Files that are legal application-wise and must load.
$mustLoad = @{
    "ok-commission-0.xml"  = "commission of exactly 0 is allowed"
    "ok-commission-90.xml" = "commission of exactly 90 is allowed"
    "ok-b-1.xml"           = "the smallest liquidity value is allowed"
    "ok-large-b.xml"       = "a very large liquidity value is allowed"
    "ok-negative-id.xml"   = "a negative event id is still a unique id"
    "ok-special-chars.xml" = "quotes and ampersands in names survive"
    "ok-long-text.xml"     = "a very long description is handled"
    "ok-50-events.xml"     = "fifty events load and list"
}
foreach ($file in ($mustLoad.Keys | Sort-Object)) {
    Test-Case ("G+  " + $mustLoad[$file]) @("1", (Join-Path $edge $file), "2", "6") @{
        must = @("valid and loaded successfully"); mustNot = @("Error:", "Infinity", "NaN")
    }
}

# Files that break an application rule. Each must be refused with a message
# that names the offending event, not just "the file is invalid".
$mustReject = @{
    "bad-commission-91.xml"        = @("commission", "91")
    "bad-commission-neg.xml"       = @("commission", "-1")
    "bad-b-zero.xml"               = @("Zero B", "b=0")
    "bad-b-negative.xml"           = @("Negative B", "b=-50")
    "bad-one-option.xml"           = @("Single Option", "exactly 2 options")
    "bad-identical-options.xml"    = @("Same Twice", "twice")
    "bad-empty-option.xml"         = @("Blank Option", "empty option name")
    "bad-empty-description.xml"    = @("No Description", "no description")
    "bad-empty-name.xml"           = @("no name")
    "bad-duplicate-id.xml"         = @("Duplicate event id 4", "First Event", "Second Event")
    "bad-duplicate-id-far-apart.xml" = @("Duplicate event id 1", "Alpha", "Delta")
    "bad-second-event-invalid.xml" = @("Broken One", "200")
}
foreach ($file in ($mustReject.Keys | Sort-Object)) {
    Test-Case ("G-  " + $file + " is refused, and says why") `
        @("1", (Join-Path $edge $file), "6") @{
        must = @("Error:") + $mustReject[$file]
        mustNot = @("loaded successfully")
    }
}

# The bad event sits second in the file, so a partial load would still show the
# first event. Nothing at all may survive a rejected file.
Test-Case "G!  a file rejected on its second event loads nothing at all" `
    @("1", (Join-Path $edge "bad-second-event-invalid.xml"), "2", "6") @{
    must = @("No system file is currently loaded")
    mustNot = @("Perfectly Fine")
}

Test-Case "G!  b=1 with a large purchase stays finite" `
    @("1", (Join-Path $edge "ok-b-1.xml"), "4", "1", "1", "1000", "3", "1", "6") @{
    must = @("1049.27"); mustNot = @("Infinity", "NaN")
}

Test-Case "G!  90% commission is charged correctly" `
    @("1", (Join-Path $edge "ok-commission-90.xml"), "4", "1", "1", "100", "6") @{
    must = @("55.81", "117.82"); mustNot = @()
}

Test-Case "G!  0% commission charges nothing extra" `
    @("1", (Join-Path $edge "ok-commission-0.xml"), "4", "1", "1", "100", "6") @{
    must = @("62.01"); mustNot = @()
}

Write-Host ""
Write-Host ("=" * 60)
if ($script:fail -eq 0) {
    Write-Host ("ALL TESTS PASSED  ({0}/{0})" -f $script:pass) -ForegroundColor Green
} else {
    Write-Host ("{0} passed, {1} FAILED" -f $script:pass, $script:fail) -ForegroundColor Red
    $script:failures | ForEach-Object { Write-Host ("  - " + $_) -ForegroundColor Red }
    Write-Host "logs: $sandbox\_fail_*.log" -ForegroundColor Yellow
}
Write-Host ("=" * 60)
