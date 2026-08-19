# Generates the edge-case XML files used by the acceptance suite.
#
# The assignment warns that the grader will feed files that are "valid
# schema-wise but not necessarily valid application-wise". Every file produced
# here is deliberately schema-valid, so the only thing that can reject it is our
# own application-level validation - which is exactly what we want to exercise.

$ErrorActionPreference = "Stop"

$repo    = Split-Path $PSScriptRoot -Parent
$edgeDir = Join-Path $repo "test-files\edge"

Remove-Item -Recurse -Force $edgeDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $edgeDir | Out-Null

function New-Event {
    param(
        [string] $Name, [int] $Id, [string] $Description,
        [string] $CommissionType, [int] $Commission,
        [string[]] $Options, [int] $B
    )
    $opts = ($Options | ForEach-Object { "`t`t`t`t<GM-option>$_</GM-option>" }) -join "`n"
    @"
		<GM-event name="$Name">
			<id>$Id</id>
			<description>$Description</description>
			<comision type="$CommissionType">$Commission</comision>
			<GM-options>
$opts
			</GM-options>
			<GM-method>
				<GM-LMSR>
					<b>$B</b>
				</GM-LMSR>
			</GM-method>
		</GM-event>
"@
}

function New-File {
    param([string] $FileName, [string[]] $Events)
    $body = $Events -join "`n"
    $xml = @"
<?xml version="1.0" encoding="UTF-8"?>
<Guess-Market xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="GM-EX1-schema.xsd">
	<GM-events>
$body
	</GM-events>
</Guess-Market>
"@
    $path = Join-Path $edgeDir $FileName
    [System.IO.File]::WriteAllText($path, $xml, (New-Object System.Text.UTF8Encoding $false))
}

$std = @{ Description = "A standard description for testing."; CommissionType = "on-purchase"; Options = @("Yes", "No"); B = 100 }

# --- Boundary values that MUST be accepted ---------------------------------

New-File "ok-commission-0.xml"  @( (New-Event @std -Name "Zero Commission" -Id 1 -Commission 0) )
New-File "ok-commission-90.xml" @( (New-Event @std -Name "Max Commission"  -Id 1 -Commission 90) )
New-File "ok-b-1.xml"           @( (New-Event -Name "Tiny Liquidity" -Id 1 -Description "b is at its smallest sensible value." -CommissionType "on-purchase" -Commission 5 -Options @("Yes","No") -B 1) )
New-File "ok-large-b.xml"       @( (New-Event -Name "Huge Liquidity" -Id 1 -Description "A very large liquidity parameter." -CommissionType "on-close" -Commission 10 -Options @("Yes","No") -B 1000000) )
New-File "ok-negative-id.xml"   @( (New-Event @std -Name "Negative Id" -Id -7 -Commission 5) )
New-File "ok-special-chars.xml" @( (New-Event -Name "Symbols" -Id 1 -Description "Quotes &apos;single&apos; and &quot;double&quot;, ampersand &amp;, math &lt; &gt;." -CommissionType "on-purchase" -Commission 5 -Options @("Yes &amp; maybe","No &lt; never") -B 100) )
New-File "ok-long-text.xml"     @( (New-Event -Name "Long Description" -Id 1 -Description ("word " * 300).Trim() -CommissionType "on-purchase" -Commission 5 -Options @("Yes","No") -B 100) )

$many = 1..50 | ForEach-Object { New-Event @std -Name "Event $_" -Id $_ -Commission 5 }
New-File "ok-50-events.xml" $many

# --- Schema-valid but application-invalid: must be rejected cleanly --------

New-File "bad-commission-91.xml"   @( (New-Event @std -Name "Over Limit" -Id 1 -Commission 91) )
New-File "bad-commission-neg.xml"  @( (New-Event @std -Name "Negative"   -Id 1 -Commission -1) )
New-File "bad-b-zero.xml"          @( (New-Event -Name "Zero B" -Id 1 -Description "Liquidity of zero would divide by zero." -CommissionType "on-purchase" -Commission 5 -Options @("Yes","No") -B 0) )
New-File "bad-b-negative.xml"      @( (New-Event -Name "Negative B" -Id 1 -Description "Liquidity cannot be negative." -CommissionType "on-purchase" -Commission 5 -Options @("Yes","No") -B -50) )

# GM-option is maxOccurs="2" with an implicit minOccurs of 1, so a single
# option passes the schema even though the exercise is about binary events.
New-File "bad-one-option.xml"      @( (New-Event -Name "Single Option" -Id 1 -Description "Only one outcome is offered." -CommissionType "on-purchase" -Commission 5 -Options @("Yes") -B 100) )

New-File "bad-identical-options.xml" @( (New-Event -Name "Same Twice" -Id 1 -Description "Both outcomes carry the same name." -CommissionType "on-purchase" -Commission 5 -Options @("Yes","Yes") -B 100) )
New-File "bad-empty-option.xml"      @( (New-Event -Name "Blank Option" -Id 1 -Description "One outcome has no name." -CommissionType "on-purchase" -Commission 5 -Options @("Yes","") -B 100) )
New-File "bad-empty-description.xml" @( (New-Event -Name "No Description" -Id 1 -Description "" -CommissionType "on-purchase" -Commission 5 -Options @("Yes","No") -B 100) )
New-File "bad-empty-name.xml"        @( (New-Event @std -Name "" -Id 1 -Commission 5) )

New-File "bad-duplicate-id.xml" @(
    (New-Event @std -Name "First Event"  -Id 4 -Commission 5),
    (New-Event @std -Name "Second Event" -Id 4 -Commission 5)
)

New-File "bad-duplicate-id-far-apart.xml" @(
    (New-Event @std -Name "Alpha"   -Id 1 -Commission 5),
    (New-Event @std -Name "Beta"    -Id 2 -Commission 5),
    (New-Event @std -Name "Gamma"   -Id 3 -Commission 5),
    (New-Event @std -Name "Delta"   -Id 1 -Commission 5)
)

# The first event is fine; the problem is in the last one. Nothing at all
# should be loaded - not even the events that came before the bad one.
New-File "bad-second-event-invalid.xml" @(
    (New-Event @std -Name "Perfectly Fine" -Id 1 -Commission 5),
    (New-Event @std -Name "Broken One"     -Id 2 -Commission 200)
)

Write-Host ("Generated {0} edge-case files in test-files\edge" -f (Get-ChildItem $edgeDir -Filter *.xml).Count)
