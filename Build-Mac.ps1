param([string]$JdkHome)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
try {
    $jdk = & (Join-Path $projectRoot 'packaging\Find-Jdk.ps1') -JdkHome $JdkHome -RequiredTools @('javac.exe', 'jar.exe')
    $buildRoot = Join-Path $projectRoot ('target\mac-package\' + [Guid]::NewGuid().ToString('N'))
    $classes = Join-Path $buildRoot 'classes'
    $bundle = Join-Path $buildRoot 'TankOps-Mac'
    New-Item -ItemType Directory -Path $classes, $bundle -Force | Out-Null
    $sourceRoots = @(
        (Join-Path $projectRoot 'tankops-common\src\main\java'),
        (Join-Path $projectRoot 'tankops-client\src\main\java')
    )
    $sources = Get-ChildItem -LiteralPath $sourceRoots -Filter '*.java' -Recurse |
        Sort-Object FullName | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }
    $sourceList = Join-Path $buildRoot 'sources.txt'
    $utf8 = New-Object Text.UTF8Encoding($false)
    [IO.File]::WriteAllLines($sourceList, [string[]]$sources, $utf8)
    & (Join-Path $jdk 'bin\javac.exe') --release 21 -encoding UTF-8 -d $classes "@$sourceList"
    if ($LASTEXITCODE -ne 0) { throw 'Mac client compilation failed.' }
    foreach ($module in @('tankops-common', 'tankops-client')) {
        $resources = Join-Path $projectRoot "$module\src\main\resources"
        Get-ChildItem -LiteralPath $resources | Copy-Item -Destination $classes -Recurse -Force
    }
    & (Join-Path $jdk 'bin\jar.exe') --create --file (Join-Path $bundle 'TankOps.jar') `
        --main-class tankops.client.Main -C $classes .
    if ($LASTEXITCODE -ne 0) { throw 'Creating the Mac client JAR failed.' }
    foreach ($file in Get-ChildItem -LiteralPath (Join-Path $projectRoot 'packaging\macos') -File) {
        $content = [IO.File]::ReadAllText($file.FullName).Replace("`r`n", "`n")
        [IO.File]::WriteAllText((Join-Path $bundle $file.Name), $content, $utf8)
    }
    Copy-Item -LiteralPath (Join-Path $projectRoot 'docs\MACOS_SETUP.md') -Destination (Join-Path $bundle 'HUONG-DAN.md')
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = Join-Path $buildRoot 'TankOps-Mac.zip'
    [IO.Compression.ZipFile]::CreateFromDirectory($bundle, $archive)
    # Store Unix executable mode in ZIP metadata, even though the package is built on Windows.
    $zip = [IO.Compression.ZipFile]::Open($archive, [IO.Compression.ZipArchiveMode]::Update)
    try {
        foreach ($entry in $zip.Entries) {
            if ($entry.FullName.EndsWith('.command')) { $entry.ExternalAttributes = -2115174400 } # 0100755 << 16
        }
    } finally { $zip.Dispose() }
    # Mark the .command central-directory entries as Unix; otherwise extractors may
    # ignore the executable bits when the ZIP was produced by Windows/.NET.
    $archiveBytes = [IO.File]::ReadAllBytes($archive)
    $endOffset = $archiveBytes.Length - 22 # ZipFile creates an archive without a ZIP comment.
    if ([BitConverter]::ToUInt32($archiveBytes, $endOffset) -ne 0x06054b50) { throw 'Unexpected ZIP footer.' }
    $centralOffset = [int][BitConverter]::ToUInt32($archiveBytes, $endOffset + 16)
    $entryCount = [BitConverter]::ToUInt16($archiveBytes, $endOffset + 10)
    for ($i = 0; $i -lt $entryCount; $i++) {
        if ([BitConverter]::ToUInt32($archiveBytes, $centralOffset) -ne 0x02014b50) { throw 'Invalid ZIP directory.' }
        $nameLength = [BitConverter]::ToUInt16($archiveBytes, $centralOffset + 28)
        $extraLength = [BitConverter]::ToUInt16($archiveBytes, $centralOffset + 30)
        $commentLength = [BitConverter]::ToUInt16($archiveBytes, $centralOffset + 32)
        $entryName = [Text.Encoding]::UTF8.GetString($archiveBytes, $centralOffset + 46, $nameLength)
        if ($entryName.EndsWith('.command')) { $archiveBytes[$centralOffset + 5] = 3 }
        $centralOffset += 46 + $nameLength + $extraLength + $commentLength
    }
    [IO.File]::WriteAllBytes($archive, $archiveBytes)
    Copy-Item -LiteralPath $archive -Destination (Join-Path $projectRoot 'TankOps-Mac.zip') -Force
    Write-Host "Ready: $projectRoot\TankOps-Mac.zip (macOS Java 21+ required; Intel and Apple Silicon)"
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}
