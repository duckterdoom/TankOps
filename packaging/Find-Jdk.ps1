param(
    [string]$JdkHome,
    [string[]]$RequiredTools = @('javac.exe', 'jar.exe', 'jpackage.exe')
)
$ErrorActionPreference = 'Stop'
$candidates = @($JdkHome, $env:JAVA_HOME)
foreach ($tool in @('javac.exe', 'jpackage.exe')) {
    $command = Get-Command $tool -ErrorAction SilentlyContinue
    if ($command) { $candidates += Split-Path (Split-Path $command.Source -Parent) -Parent }
}
foreach ($location in @(
    (Join-Path $env:USERPROFILE '.jdks'),
    (Join-Path $env:ProgramFiles 'Java'),
    (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
    (Join-Path $env:ProgramFiles 'Microsoft')
)) {
    if (Test-Path -LiteralPath $location) {
        $candidates += Get-ChildItem -LiteralPath $location -Directory |
            Sort-Object Name -Descending | Select-Object -ExpandProperty FullName
    }
}
foreach ($candidate in $candidates) {
    if (-not $candidate) { continue }
    $missing = @($RequiredTools | Where-Object { -not (Test-Path -LiteralPath (Join-Path $candidate "bin\$_")) })
    if ($missing.Count) { continue }
    $release = Join-Path $candidate 'release'
    if (-not (Test-Path -LiteralPath $release)) { continue }
    $version = Get-Content -LiteralPath $release | Select-String '^JAVA_VERSION="(\d+)'
    if ($version -and [int]$version.Matches[0].Groups[1].Value -ge 21) {
        return $candidate
    }
}
throw 'Build requires JDK 21 or newer. Set JAVA_HOME or pass -JdkHome.'
