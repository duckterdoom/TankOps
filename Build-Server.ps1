param([string]$JdkHome)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
try {
    $jdk = & (Join-Path $projectRoot 'packaging\Find-Jdk.ps1') -JdkHome $JdkHome -RequiredTools @('javac.exe', 'jar.exe')
    $buildRoot = Join-Path $projectRoot ('target\ubuntu-package\' + [Guid]::NewGuid().ToString('N'))
    $classes = Join-Path $buildRoot 'classes'
    $bundle = Join-Path $buildRoot 'bundle'
    New-Item -ItemType Directory -Path $classes, $bundle -Force | Out-Null
    $sourceRoots = @(
        (Join-Path $projectRoot 'tankops-common\src\main\java'),
        (Join-Path $projectRoot 'tankops-server\src\main\java')
    )
    $sources = Get-ChildItem -LiteralPath $sourceRoots -Filter '*.java' -Recurse |
        Sort-Object FullName | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }
    $sourceList = Join-Path $buildRoot 'sources.txt'
    $utf8 = New-Object Text.UTF8Encoding($false)
    [IO.File]::WriteAllLines($sourceList, [string[]]$sources, $utf8)
    & (Join-Path $jdk 'bin\javac.exe') --release 21 -encoding UTF-8 -d $classes "@$sourceList"
    if ($LASTEXITCODE -ne 0) { throw 'Server compilation failed.' }
    foreach ($module in @('tankops-common', 'tankops-server')) {
        $resources = Join-Path $projectRoot "$module\src\main\resources"
        Get-ChildItem -LiteralPath $resources | Copy-Item -Destination $classes -Recurse -Force
    }
    & (Join-Path $jdk 'bin\jar.exe') --create --file (Join-Path $bundle 'tankops-server.jar') `
        --main-class tankops.server.net.GameServer -C $classes .
    if ($LASTEXITCODE -ne 0) { throw 'Creating the server JAR failed.' }
    foreach ($file in Get-ChildItem -LiteralPath (Join-Path $projectRoot 'deploy\ubuntu') -File) {
        # The ZIP is prepared on Windows but shell scripts must use LF on Ubuntu.
        $content = [IO.File]::ReadAllText($file.FullName).Replace("`r`n", "`n")
        [IO.File]::WriteAllText((Join-Path $bundle $file.Name), $content, $utf8)
    }
    Copy-Item -LiteralPath (Join-Path $projectRoot 'docs\TAILSCALE_SETUP.md') -Destination (Join-Path $bundle 'HUONG-DAN.md')
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = Join-Path $buildRoot 'TankOps-Ubuntu.zip'
    [IO.Compression.ZipFile]::CreateFromDirectory($bundle, $archive)
    Copy-Item -LiteralPath $archive -Destination (Join-Path $projectRoot 'TankOps-Ubuntu.zip') -Force
    Write-Host "Ready: $projectRoot\TankOps-Ubuntu.zip (5 players per room)"
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}
