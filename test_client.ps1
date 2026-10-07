param(
    [string]$ServerHost = "localhost",
    [int]$Port = 5000
)

$client = New-Object System.Net.Sockets.TcpClient($ServerHost, $Port)
$stream = $client.GetStream()
$writer = New-Object System.IO.StreamWriter($stream)
$reader = New-Object System.IO.StreamReader($stream)
$writer.AutoFlush = $true

Write-Host "Da ket noi toi ${ServerHost}:${Port}. Go noi dung roi Enter de gui. Go 'exit' de thoat."

while ($true) {
    $line = Read-Host ">"
    if ($line -eq "exit") { break }
    $writer.WriteLine($line)
    $response = $reader.ReadLine()
    Write-Host "Server tra ve: $response"
}

$client.Close()
Write-Host "Da dong ket noi."
