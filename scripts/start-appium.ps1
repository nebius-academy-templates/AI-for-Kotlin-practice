[CmdletBinding()]
param(
    [ValidateRange(1, 65535)]
    [int]$Port = 4723
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$workspace = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location -LiteralPath $workspace

$localAppium = Join-Path $workspace "node_modules\.bin\appium.cmd"
if (-not (Test-Path -LiteralPath $localAppium)) {
    throw "The pinned local Appium CLI is missing. Run npm.cmd ci from $workspace."
}

# Let Appium discover the driver pinned by this npm project, not a driver
# registry configured for another project on the same machine.
Remove-Item Env:APPIUM_HOME -ErrorAction SilentlyContinue

Write-Host "Starting the repository-pinned Appium server at http://127.0.0.1:$Port"
& npx.cmd --no-install appium --address 127.0.0.1 --port $Port
exit $LASTEXITCODE
