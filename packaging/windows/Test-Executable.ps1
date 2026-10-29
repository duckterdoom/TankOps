param([Parameter(Mandatory=$true)][string]$Executable)
$ErrorActionPreference = 'Stop'
$Executable = (Resolve-Path -LiteralPath $Executable).Path
# A fresh, disposable profile exercises extraction, not a previously populated cache.
$checkRoot = Join-Path $env:TEMP ('TankOps-release-check-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $checkRoot -Force | Out-Null
$unicodeDir = Join-Path $checkRoot ('Player ' + [char]0x1EC7 + ' ' + [char]0x4E2D)
New-Item -ItemType Directory -Path $unicodeDir -Force | Out-Null
$unicodeExe = Join-Path $unicodeDir 'TankOps game.exe'
Copy-Item -LiteralPath $Executable -Destination $unicodeExe
foreach ($scenario in @('fresh-no-java', 'cached-foreign-java', 'unicode-path')) {
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName = $Executable
    $info.Arguments = '--diagnose'
    $info.WorkingDirectory = $checkRoot
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.EnvironmentVariables['PATH'] = Join-Path $env:SystemRoot 'System32'
    $info.EnvironmentVariables['LOCALAPPDATA'] = Join-Path $checkRoot 'profile'
    $info.EnvironmentVariables.Remove('JAVA_HOME')
    foreach ($option in @('JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS')) {
        $info.EnvironmentVariables.Remove($option)
    }
    if ($scenario -eq 'cached-foreign-java') {
        $info.EnvironmentVariables['JAVA_HOME'] = Join-Path $checkRoot 'nonexistent-jdk'
        foreach ($option in @('JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS')) {
            $info.EnvironmentVariables[$option] = '-XX:TankOpsInvalidOptionForReleaseCheck=1'
        }
    }
    if ($scenario -eq 'unicode-path') {
        $info.FileName = $unicodeExe
        $info.EnvironmentVariables['LOCALAPPDATA'] = Join-Path $unicodeDir 'profile'
    }
    $process = [Diagnostics.Process]::Start($info)
    try {
        if (-not $process.WaitForExit(60000)) { $process.Kill(); throw "EXE check timed out: $scenario" }
        if ($process.ExitCode -ne 0) { throw "EXE check failed: $scenario. Inspect logs under $checkRoot or beside the EXE." }
        Write-Host "PASS EXE: $scenario"
    } finally { $process.Dispose() }
}
# Keep the evidence and logs; no user cache is removed by this check.
Write-Host "EXE check evidence: $checkRoot"
