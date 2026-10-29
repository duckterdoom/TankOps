param(
    [string]$JdkHome
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot

try {
    $jdk = & (Join-Path $projectRoot 'packaging\Find-Jdk.ps1') -JdkHome $JdkHome -RequiredTools @('javac.exe', 'jar.exe', 'jlink.exe')

    $buildRoot = Join-Path $projectRoot ('target\windows-package\' + [Guid]::NewGuid().ToString('N'))
    $classes = Join-Path $buildRoot 'classes'
    $inputDir = Join-Path $buildRoot 'input'
    $imageDir = Join-Path $buildRoot 'image'
    New-Item -ItemType Directory -Path $classes, $inputDir, $imageDir -Force | Out-Null

    Write-Host "Building TankOps with $jdk"
    # These two modules have no third-party runtime dependencies; compile only
    # production sources so database/server/test tooling is not required to play.
    $sourceRoots = @(
        (Join-Path $projectRoot 'tankops-common\src\main\java'),
        (Join-Path $projectRoot 'tankops-client\src\main\java')
    )
    $sources = Get-ChildItem -LiteralPath $sourceRoots -Filter '*.java' -Recurse |
        Sort-Object FullName | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }
    $sourceList = Join-Path $buildRoot 'sources.txt'
    [IO.File]::WriteAllLines($sourceList, [string[]]$sources, (New-Object Text.UTF8Encoding($false)))
    & (Join-Path $jdk 'bin\javac.exe') --release 21 -encoding UTF-8 -d $classes "@$sourceList"
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }

    foreach ($module in @('tankops-common', 'tankops-client')) {
        $resources = Join-Path $projectRoot "$module\src\main\resources"
        if (Test-Path -LiteralPath $resources) {
            Get-ChildItem -LiteralPath $resources | Copy-Item -Destination $classes -Recurse -Force
        }
    }
    $jarPath = Join-Path $inputDir 'TankOps.jar'
    & (Join-Path $jdk 'bin\jar.exe') --create --file $jarPath --main-class tankops.client.Main -C $classes .
    if ($LASTEXITCODE -ne 0) { throw 'Creating TankOps.jar failed.' }

    Write-Host 'Creating Windows executable and bundled Java runtime...'
    $appRoot = Join-Path $imageDir 'TankOps'
    $appDir = Join-Path $appRoot 'app'
    New-Item -ItemType Directory -Path $appDir -Force | Out-Null
    # Keep java.exe: the outer launcher starts it directly and captures JVM diagnostics.
    # jpackage's default runtime strips these commands and its native launcher only
    # reports the generic "Failed to launch JVM" on affected machines.
    & (Join-Path $jdk 'bin\jlink.exe') --module-path (Join-Path $jdk 'jmods') `
        --add-modules java.desktop --strip-debug --no-header-files --no-man-pages `
        --output (Join-Path $appRoot 'runtime')
    if ($LASTEXITCODE -ne 0) { throw 'Creating the bundled Java runtime failed.' }
    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $appDir 'TankOps.jar')

    Write-Host 'Embedding the game and Java runtime into one executable...'
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $payload = Join-Path $buildRoot 'TankOps.zip'
    [IO.Compression.ZipFile]::CreateFromDirectory(
        (Join-Path $imageDir 'TankOps'), $payload,
        [IO.Compression.CompressionLevel]::Optimal, $false)

    $compiler = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
    if (-not (Test-Path -LiteralPath $compiler)) {
        $compiler = Join-Path $env:WINDIR 'Microsoft.NET\Framework\v4.0.30319\csc.exe'
    }
    if (-not (Test-Path -LiteralPath $compiler)) {
        throw 'The Windows .NET Framework C# compiler was not found.'
    }
    $singleExe = Join-Path $buildRoot 'TankOps.exe'
    & $compiler /nologo /target:winexe /platform:x64 /optimize+ `
        /reference:System.IO.Compression.dll /reference:System.IO.Compression.FileSystem.dll `
        /reference:System.Windows.Forms.dll "/resource:$payload,TankOps.Payload.zip" `
        "/out:$singleExe" (Join-Path $projectRoot 'packaging\windows\Launcher.cs')
    if ($LASTEXITCODE -ne 0) { throw 'Creating the single-file launcher failed.' }
    $destination = Join-Path $projectRoot 'TankOps.exe'
    Copy-Item -LiteralPath $singleExe -Destination $destination -Force
    Write-Host "Ready: $destination (single file, bundled Java)"
} catch {
    Write-Error $_ -ErrorAction Continue
    exit 1
}
