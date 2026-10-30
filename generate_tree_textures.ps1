# Generate all 36 custom fruit tree textures as valid PNGs
# Fixed CRC computation for PowerShell compatibility

$texDir = "C:\Intellij code\NeoForge-1.21.1-Grey-Horizons RPG\Farming_Overhaul_Subpack\src\main\resources\assets\gh_farming_overhaul\textures\block\"

$trees = @(
    @{ name = "apple"; barkR=101; barkG=67; barkB=33; topR=139; topG=90; topB=43; leavesR=34; leavesG=139; leavesB=34 }
    @{ name = "pear"; barkR=139; barkG=119; barkB=101; topR=160; topG=140; topB=120; leavesR=50; leavesG=160; leavesB=50 }
    @{ name = "fig"; barkR=101; barkG=90; barkB=80; topR=120; topG=110; topB=100; leavesR=40; leavesG=100; leavesB=40 }
    @{ name = "guava"; barkR=160; barkG=120; barkB=80; topR=180; topG=140; topB=100; leavesR=60; leavesG=140; leavesB=60 }
    @{ name = "kiwi"; barkR=180; barkG=150; barkB=100; topR=200; topG=170; topB=120; leavesR=70; leavesG=150; leavesB=70 }
    @{ name = "mango"; barkR=80; barkG=50; barkB=30; topR=100; topG=70; topB=50; leavesR=20; leavesG=100; leavesB=20 }
    @{ name = "orange"; barkR=120; barkG=100; barkB=80; topR=140; topG=120; topB=100; leavesR=30; leavesG=120; leavesB=30 }
    @{ name = "papaya"; barkR=200; barkG=180; barkB=140; topR=220; topG=200; topB=160; leavesR=80; leavesG=160; leavesB=80 }
    @{ name = "peach"; barkR=140; barkG=80; barkB=60; topR=160; topG=100; topB=80; leavesR=90; leavesG=140; leavesB=90 }
    @{ name = "persimmon"; barkR=100; barkG=80; barkB=60; topR=120; topG=100; topB=80; leavesR=40; leavesG=120; leavesB=40 }
    @{ name = "plum"; barkR=70; barkG=50; barkB=80; topR=90; topG=70; topB=100; leavesR=60; leavesG=100; leavesB=60 }
    @{ name = "pomegranate"; barkR=110; barkG=80; barkB=60; topR=130; topG=100; topB=80; leavesR=50; leavesG=130; leavesB=50 }
)

function Clamp($v) { [Math]::Max(0, [Math]::Min(255, [int]$v)) }

function To-UInt32($v) { [uint32]($v -band 0xFFFFFFFF) }

function Write-Chunk($stream, $type, $data) {
    $len = $data.Length
    $lenBytes = [BitConverter]::GetBytes((To-UInt32 $len))
    if ([BitConverter]::IsLittleEndian) { [Array]::Reverse($lenBytes) }
    $stream.Write($lenBytes, 0, 4)

    $typeBytes = [System.Text.Encoding]::ASCII.GetBytes($type)
    $stream.Write($typeBytes, 0, 4)

    if ($data.Length -gt 0) { $stream.Write($data, 0, $data.Length) }

    # CRC32 of type + data
    $crcData = New-Object System.IO.MemoryStream
    $crcWriter = New-Object System.IO.BinaryWriter($crcData)
    $crcWriter.Write($typeBytes, 0, 4)
    if ($data.Length -gt 0) { $crcWriter.Write($data, 0, $data.Length) }
    $crcBytes = $crcData.ToArray()
    $crcWriter.Close(); $crcData.Close()

    $crc = 0xFFFFFFFF
    foreach ($b in $crcBytes) {
        $crc = $crc -bxor $b
        for ($i = 0; $i -lt 8; $i++) {
            if (($crc -band 1) -eq 1) { $crc = (($crc -shr 1) -bxor 0xEDB88320) -band 0xFFFFFFFF }
            else { $crc = ($crc -shr 1) -band 0xFFFFFFFF }
        }
    }
    $crc = ($crc -bxor 0xFFFFFFFF) -band 0xFFFFFFFF

    $crcBytes = [BitConverter]::GetBytes((To-UInt32 $crc))
    if ([BitConverter]::IsLittleEndian) { [Array]::Reverse($crcBytes) }
    $stream.Write($crcBytes, 0, 4)
}

function Build-DeflateStored($rawBytes) {
    $ms = New-Object System.IO.MemoryStream
    $writer = New-Object System.IO.BinaryWriter($ms)

    $offset = 0
    $remaining = $rawBytes.Length
    while ($remaining -gt 0) {
        $blockLen = [Math]::Min(65535, $remaining)
        $isFinal = ($remaining -le 65535)

        $byte0 = if ($isFinal) { 1 } else { 0 }
        $writer.Write([byte]$byte0)

        $lenBytes = [BitConverter]::GetBytes([uint16]$blockLen)
        $writer.Write($lenBytes, 0, 2)

        $nlen = [uint16]([uint16]$blockLen -bxor 0xFFFF)
        $nlenBytes = [BitConverter]::GetBytes($nlen)
        $writer.Write($nlenBytes, 0, 2)

        $blockData = New-Object byte[] $blockLen
        [Array]::Copy($rawBytes, $offset, $blockData, 0, $blockLen)
        $writer.Write($blockData, 0, $blockLen)

        $offset += $blockLen
        $remaining -= $blockLen
    }
    $writer.Close()
    return $ms.ToArray()
}

function Write-Png($path, $width, $height, $pixels) {
    $rawMs = New-Object System.IO.MemoryStream
    $rawWriter = New-Object System.IO.BinaryWriter($rawMs)
    for ($y = 0; $y -lt $height; $y++) {
        $rawWriter.Write([byte]0)
        for ($x = 0; $x -lt $width; $x++) {
            $idx = ($y * $width + $x) * 3
            $rawWriter.Write($pixels[$idx])
            $rawWriter.Write($pixels[$idx + 1])
            $rawWriter.Write($pixels[$idx + 2])
        }
    }
    $rawWriter.Close()
    $rawBytes = $rawMs.ToArray()
    $rawMs.Close()

    $deflateBytes = Build-DeflateStored $rawBytes

    $ms = New-Object System.IO.MemoryStream
    $writer = New-Object System.IO.BinaryWriter($ms)

    $sig = [byte[]](0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    $writer.Write($sig, 0, 8)

    # IHDR
    $ihdrMs = New-Object System.IO.MemoryStream
    $ihdrWriter = New-Object System.IO.BinaryWriter($ihdrMs)
    $ihdrWriter.Write([uint32]$width)
    $ihdrWriter.Write([uint32]$height)
    $ihdrWriter.Write([byte]8)
    $ihdrWriter.Write([byte]2)
    $ihdrWriter.Write([byte]0)
    $ihdrWriter.Write([byte]0)
    $ihdrWriter.Write([byte]0)
    $ihdrBytes = $ihdrMs.ToArray()
    [Array]::Reverse($ihdrBytes, 0, 4)
    [Array]::Reverse($ihdrBytes, 4, 4)
    $ihdrWriter.Close(); $ihdrMs.Close()
    Write-Chunk $writer "IHDR" $ihdrBytes

    # IDAT
    Write-Chunk $writer "IDAT" $deflateBytes

    # IEND
    Write-Chunk $writer "IEND" @()

    $writer.Close()
    $fileBytes = $ms.ToArray()
    $ms.Close()

    [System.IO.File]::WriteAllBytes($path, $fileBytes)
}

$count = 0
foreach ($tree in $trees) {
    $name = $tree.name
    $w = 16; $h = 16

    # Bark
    $barkPixels = @()
    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $r = Clamp($tree.barkR + (Get-Random -Minimum -20 -Maximum 20))
            $g = Clamp($tree.barkG + (Get-Random -Minimum -20 -Maximum 20))
            $b = Clamp($tree.barkB + (Get-Random -Minimum -20 -Maximum 20))
            $barkPixels += $r; $barkPixels += $g; $barkPixels += $b
        }
    }
    Write-Png "$texDir${name}_log.png" $w $h $barkPixels
    $count++

    # Log top
    $topPixels = @()
    $centerX = 7.5; $centerY = 7.5; $radius = 7
    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $dx = $x - $centerX
            $dy = $y - $centerY
            $dist = [Math]::Sqrt($dx * $dx + $dy * $dy)
            if ($dist -le $radius) {
                $ring = [Math]::Floor($dist / 1.5) % 3
                switch ($ring) {
                    0 { $r=$tree.topR; $g=$tree.topG; $b=$tree.topB }
                    1 { $r=Clamp($tree.topR - 20); $g=Clamp($tree.topG - 20); $b=Clamp($tree.topB - 20) }
                    2 { $r=Clamp($tree.topR + 20); $g=Clamp($tree.topG + 20); $b=Clamp($tree.topB + 20) }
                }
            } else {
                $r=$tree.barkR; $g=$tree.barkG; $b=$tree.barkB
            }
            $topPixels += $r; $topPixels += $g; $topPixels += $b
        }
    }
    Write-Png "$texDir${name}_log_top.png" $w $h $topPixels
    $count++

    # Leaves
    $leavesPixels = @()
    for ($y = 0; $y -lt $h; $y++) {
        for ($x = 0; $x -lt $w; $x++) {
            $noise = Get-Random -Minimum -30 -Maximum 30
            $r = Clamp($tree.leavesR + $noise)
            $g = Clamp($tree.leavesG + $noise)
            $b = Clamp($tree.leavesB + $noise)
            $leavesPixels += $r; $leavesPixels += $g; $leavesPixels += $b
        }
    }
    Write-Png "$texDir${name}_leaves.png" $w $h $leavesPixels
    $count++

    Write-Host "Created textures for $name"
}

Write-Host "Total textures created: $count"