# Restarts the Grey Horizons dev dashboard (side task bar).
# Usage:  powershell -ExecutionPolicy Bypass -File devtools\dashboard\start.ps1
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$log  = Join-Path $PSScriptRoot 'server.log'
$err  = Join-Path $PSScriptRoot 'server.err.log'

Get-CimInstance Win32_Process -Filter "Name='node.exe'" | Where-Object {
    $_.CommandLine -like '*devtools*dashboard*server.js*'
} | ForEach-Object {
    Write-Output ("Stopping old dashboard process " + $_.ProcessId)
    Stop-Process -Id $_.ProcessId -Force
}

Start-Sleep -Milliseconds 700
Start-Process -FilePath 'node' `
    -ArgumentList 'devtools/dashboard/server.js' `
    -WorkingDirectory $root `
    -WindowStyle Hidden `
    -RedirectStandardOutput $log `
    -RedirectStandardError $err

Start-Sleep -Seconds 2
try {
    $r = Invoke-WebRequest -Uri 'http://localhost:7645/api/state' -UseBasicParsing -TimeoutSec 8 -Proxy $null
    Write-Output ("Dashboard running: HTTP " + $r.StatusCode + "  ->  http://localhost:7645")
} catch {
    Write-Output ("Dashboard failed to start: " + $_.Exception.Message)
    Get-Content $err -ErrorAction SilentlyContinue
}
