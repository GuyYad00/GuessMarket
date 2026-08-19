# Validates the sample XML files against the official GM-EX1 schema.
#
# The point is not to test our program - it is to prove that the files we feed
# it are the kind of files the grader will feed it. A file that fails here is a
# bad test case, because the grader promised every input would be schema-valid.

$ErrorActionPreference = "Stop"

$repo    = Split-Path $PSScriptRoot -Parent
$xsd     = Join-Path $repo "test-files\GM-EX1-schema.xsd"
$targets = @(Get-ChildItem (Join-Path $repo "test-files") -Filter *.xml) +
           @(Get-ChildItem (Join-Path $repo "test-files\edge") -Filter *.xml)

$schemas = New-Object System.Xml.Schema.XmlSchemaSet
$schemas.Add($null, $xsd) | Out-Null

$valid = 0
$invalid = @()

foreach ($file in $targets) {
    $errors = New-Object System.Collections.ArrayList
    $settings = New-Object System.Xml.XmlReaderSettings
    $settings.ValidationType = [System.Xml.ValidationType]::Schema
    $settings.Schemas = $schemas
    $handler = [System.Xml.Schema.ValidationEventHandler] {
        param($sender, $e)
        [void]$errors.Add($e.Message)
    }
    $settings.add_ValidationEventHandler($handler)

    try {
        $reader = [System.Xml.XmlReader]::Create($file.FullName, $settings)
        while ($reader.Read()) { }
        $reader.Close()
    } catch {
        [void]$errors.Add($_.Exception.Message)
    }

    $label = if ($file.Directory.Name -eq "edge") { "edge\" + $file.Name } else { $file.Name }
    if ($errors.Count -eq 0) {
        Write-Host ("  [schema-valid] " + $label) -ForegroundColor Green
        $valid++
    } else {
        Write-Host ("  [SCHEMA-INVALID] " + $label) -ForegroundColor Red
        $errors | Select-Object -First 2 | ForEach-Object { Write-Host ("      " + $_) -ForegroundColor Red }
        $invalid += $label
    }
}

Write-Host ""
Write-Host ("{0} of {1} files conform to the official schema." -f $valid, $targets.Count)
if ($invalid.Count -gt 0) {
    Write-Host "These are not representative test inputs:" -ForegroundColor Yellow
    $invalid | ForEach-Object { Write-Host ("  - " + $_) -ForegroundColor Yellow }
    exit 1
}
