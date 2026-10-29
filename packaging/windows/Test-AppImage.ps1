param([Parameter(Mandatory=$true)][string]$AppRoot)
$ErrorActionPreference = 'Stop'
$runtimeJava = Join-Path $AppRoot 'runtime\bin\java.exe'
foreach ($required in @('runtime\bin\java.exe', 'runtime\bin\server\jvm.dll', 'app\TankOps.jar')) {
    if (-not (Test-Path -LiteralPath (Join-Path $AppRoot $required))) { throw "Missing package file: $required" }
}
foreach ($bridge in @($false, $true)) {
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName = $runtimeJava
    $info.WorkingDirectory = $AppRoot
    $info.Arguments = '-Dfile.encoding=UTF-8 -Djava.awt.headless=false '
    if ($bridge) { $info.Arguments += '-Djavax.accessibility.assistive_technologies=com.sun.java.accessibility.AccessBridge ' }
    $info.Arguments += '-jar app\TankOps.jar --self-test'
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    # Emulate a machine with no external Java; always start the exact runtime being shipped.
    $info.EnvironmentVariables.Remove('JAVA_HOME')
    foreach ($option in @('JAVA_TOOL_OPTIONS','_JAVA_OPTIONS','JDK_JAVA_OPTIONS')) { $info.EnvironmentVariables.Remove($option) }
    $info.EnvironmentVariables['PATH'] = (Join-Path $env:SystemRoot 'System32')
    $process = [Diagnostics.Process]::Start($info)
    try {
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit(30000)) { $process.Kill(); throw 'Package self-test timed out.' }
        Write-Output $stdout.Result
        if ($process.ExitCode -ne 0) { throw "Package self-test failed (AccessBridge=$bridge): $($stderr.Result)" }
        if ($stderr.Result) { Write-Output $stderr.Result }
    } finally { $process.Dispose() }
}
