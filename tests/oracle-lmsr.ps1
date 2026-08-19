# LMSR oracle / pulse-check.
#
# The lecturer's simulator (lmsr_simulation.html) prices shares with the softmax
# rule and charges the LMSR cost = C(after) - C(before), where
#     C(q0,q1) = b * ln(e^(q0/b) + e^(q1/b))
#     price_i  = e^(qi/b) / (e^(q0/b) + e^(q1/b))
#
# This script re-implements those formulas here, completely independently of the
# Java code, then runs our program on the same scenarios and checks that every
# number our program prints matches the reference to the cent. If they match,
# our engine agrees with the tool the lecturer told us to check against.

$ErrorActionPreference = "Stop"

$repo     = Split-Path $PSScriptRoot -Parent
$zip      = Join-Path $repo "submission\GuessMarket-EX1.zip"
$javaHome = "C:\Program Files\Java\jdk-25.0.4.1"
$sandbox  = Join-Path $env:TEMP "gm oracle"

Remove-Item -Recurse -Force $sandbox -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $sandbox | Out-Null
tar -x -f $zip -C $sandbox
$env:Path = "$javaHome\bin;$env:Path"

$script:pass = 0
$script:fail = 0

# --- Independent reference implementation (numerically stable log-sum-exp) ---

function Ref-Cost([double]$q0, [double]$q1, [double]$b) {
    $m = [math]::Max($q0, $q1)
    return $m + $b * [math]::Log([math]::Exp(($q0 - $m) / $b) + [math]::Exp(($q1 - $m) / $b))
}
function Ref-Price([double]$mine, [double]$other, [double]$b) {
    return 1.0 / (1.0 + [math]::Exp(($other - $mine) / $b))
}

function Make-EventFile([int]$b) {
    $path = Join-Path $sandbox "oracle-b$b.xml"
    $xml = @"
<?xml version="1.0" encoding="UTF-8"?>
<Guess-Market xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="GM-EX1-schema.xsd">
	<GM-events>
		<GM-event name="Oracle Event">
			<id>1</id>
			<description>Zero-commission event for exact LMSR comparison, b=$b.</description>
			<comision type="on-purchase">0</comision>
			<GM-options>
				<GM-option>Yes</GM-option>
				<GM-option>No</GM-option>
			</GM-options>
			<GM-method>
				<GM-LMSR>
					<b>$b</b>
				</GM-LMSR>
			</GM-method>
		</GM-event>
	</GM-events>
</Guess-Market>
"@
    [System.IO.File]::WriteAllText($path, $xml, (New-Object System.Text.UTF8Encoding $false))
    return $path
}

function Approximately([double]$a, [double]$b) {
    return [math]::Abs($a - $b) -le 0.01
}

# Runs one scenario: a list of buys @(@{opt=1;amt=100}, ...) on a b-event.
# Compares each buy's "Total paid" and the final two prices to the reference.
function Test-Scenario([int]$b, [array]$buys) {
    $xml = Make-EventFile $b

    # Build the program input and the reference answers in lock-step.
    $lines = @("1", $xml)
    $q = @(0.0, 0.0)
    $expectedCosts = @()
    foreach ($buy in $buys) {
        $before = Ref-Cost $q[0] $q[1] $b
        $q[$buy.opt - 1] += $buy.amt
        $after = Ref-Cost $q[0] $q[1] $b
        $expectedCosts += ($after - $before)
        $lines += @("4", "1", "$($buy.opt)", "$($buy.amt)")
    }
    $expPrice0 = Ref-Price $q[0] $q[1] $b
    $expPrice1 = Ref-Price $q[1] $q[0] $b
    $lines += @("3", "1", "6")

    Set-Content -Path (Join-Path $sandbox "_in.txt") -Value $lines -Encoding ASCII
    $out = cmd /c "cd /d ""$sandbox"" && run.bat < ""$(Join-Path $sandbox '_in.txt')"" 2>&1" | Out-String

    $label = "b=$b  buys=[" + (($buys | ForEach-Object { "$($_.opt):$($_.amt)" }) -join ",") + "]"

    if ($out -match "Exception in thread") {
        Write-Host "  [FAIL] $label  -> CRASHED" -ForegroundColor Red
        $script:fail++; return
    }

    $actualCosts = [regex]::Matches($out, "(?m)^Total paid: ([\d.]+)") | ForEach-Object { [double]$_.Groups[1].Value }
    $prices      = [regex]::Matches($out, "\| Price: ([\d.]+) \|") | ForEach-Object { [double]$_.Groups[1].Value }

    $problems = @()
    if ($actualCosts.Count -ne $expectedCosts.Count) {
        $problems += "expected $($expectedCosts.Count) buys, parsed $($actualCosts.Count)"
    } else {
        for ($i = 0; $i -lt $expectedCosts.Count; $i++) {
            $exp = [math]::Round($expectedCosts[$i], 2)
            if (-not (Approximately $actualCosts[$i] $exp)) {
                $problems += "buy $($i+1): program=$($actualCosts[$i]) reference=$exp"
            }
        }
    }
    if ($prices.Count -ge 2) {
        $lastTwo = $prices[($prices.Count - 2)..($prices.Count - 1)]
        if (-not (Approximately $lastTwo[0] ([math]::Round($expPrice0, 2)))) {
            $problems += "price[0]: program=$($lastTwo[0]) reference=$([math]::Round($expPrice0,2))"
        }
        if (-not (Approximately $lastTwo[1] ([math]::Round($expPrice1, 2)))) {
            $problems += "price[1]: program=$($lastTwo[1]) reference=$([math]::Round($expPrice1,2))"
        }
        if (-not (Approximately ($lastTwo[0] + $lastTwo[1]) 1.0)) {
            $problems += "prices do not sum to 1: $($lastTwo[0]) + $($lastTwo[1])"
        }
    } else {
        $problems += "could not parse final prices"
    }

    if ($problems.Count -eq 0) {
        Write-Host "  [PASS] $label" -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host "  [FAIL] $label" -ForegroundColor Red
        $problems | ForEach-Object { Write-Host "         $_" -ForegroundColor Red }
        $script:fail++
    }
}

Write-Host ""
Write-Host "=== LMSR oracle: our engine vs an independent softmax implementation ===" -ForegroundColor Cyan

foreach ($b in @(10, 50, 100, 400, 1000)) {
    Test-Scenario $b @( @{opt=1; amt=100} )
    Test-Scenario $b @( @{opt=1; amt=50}, @{opt=2; amt=30} )
    Test-Scenario $b @( @{opt=1; amt=200}, @{opt=1; amt=100} )
    Test-Scenario $b @( @{opt=2; amt=10}, @{opt=1; amt=10}, @{opt=2; amt=5} )
}

# The single documented example from the assignment: b=100, buy 100 -> 62.01, 0.73
Write-Host ""
Write-Host "=== The worked example from the assignment PDF ===" -ForegroundColor Cyan
Test-Scenario 100 @( @{opt=1; amt=100} )

Write-Host ""
Write-Host ("=" * 60)
if ($script:fail -eq 0) {
    Write-Host ("ORACLE AGREES ON ALL $($script:pass) SCENARIOS") -ForegroundColor Green
} else {
    Write-Host ("$($script:pass) passed, $($script:fail) FAILED") -ForegroundColor Red
}
Write-Host ("=" * 60)

Remove-Item -Recurse -Force $sandbox -ErrorAction SilentlyContinue
if ($script:fail -gt 0) { exit 1 }
