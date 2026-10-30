# Generate a valid 16x16 RGB PNG using stored (uncompressed) Deflate blocks
param($path, $width, $height, $pixels)  # pixels: flat array of bytes [R,G,B, R,G,B, ...]

function Clamp($v) { [Math]::Max(0, [Math]::Min(255, [int]$v)) }

function Write-Chunk {
    param($stream, $type, $data)
    
    # Length (4 bytes big-endian)
    $len = $data.Length
    $lenBytes = [BitConverter]::GetBytes([uint32]$len)
    if ([BitConverter]::IsLittleEndian) { [Array]::Reverse($lenBytes) }
    $stream.Write($lenBytes, 0, 4)
    
    # Type (4 bytes ASCII)
    $typeBytes = [System.Text.Encoding]::ASCII.GetBytes($type)
    $stream.Write($typeBytes, 0, 4)
    
    # Data
    if ($data.Length -gt 0) {
        $stream.Write($data, 0, $data.Length)
    }
    
    # CRC32 of type + data
    $crcData = New-Object System.IO.MemoryStream
    $crcWriter = New-Object System.IO.BinaryWriter($crcData)
    $crcWriter.Write($typeBytes, 0, 4)
    if ($data.Length -gt 0) {
        $crcWriter.Write($data, 0, $data.Length)
    }
    $crcBytes = $crcData.ToArray()
    $crcWriter.Close()
    $crcData.Close()
    
    $crc = 0xFFFFFFFF -bxor 0  # init
    foreach ($b in $crcBytes) {
        $crc = $crc -bxor $b
        for ($i = 0; $i -lt 8; $i++) {
            if (($crc -band 1) -eq 1) {
                $crc = ($crc -shr 1) -bxor 0xEDB88320
            } else {
                $crc = $crc -shr 1
            }
        }
    }
    $crc = $crc -bxor 0xFFFFFFFF
    
    $crcBytes = [BitConverter]::GetBytes([uint32]$crc)
    if ([BitConverter]::IsLittleEndian) { [Array]::Reverse($crcBytes) }
    $stream.Write($crcBytes, 0, 4)
}

function Build-DeflateStored {
    param($rawBytes)  # raw uncompressed data (scanlines with filter bytes)
    
    $ms = New-Object System.IO.MemoryStream
    $writer = New-Object System.IO.BinaryWriter($ms)
    
    $offset = 0
    $remaining = $rawBytes.Length
    
    while ($remaining -gt 0) {
        $blockLen = [Math]::Min(65535, $remaining)
        $isFinal = ($remaining -le 65535)
        
        # Byte 0: BFINAL(1 bit) + BTYPE(2 bits) + 5 padding bits
        $bFinal = if ($isFinal) { 1 } else { 0 }
        $byte0 = $bFinal -bor 0 -bor 0  # BTYPE=00 (stored)
        $writer.Write([byte]$byte0)
        
        # LEN (2 bytes LE)
        $lenBytes = [BitConverter]::GetBytes([uint16]$blockLen)
        if ([BitConverter]::IsLittleEndian) { }  # already LE
        $writer.Write($lenBytes, 0, 2)
        
        # NLEN (2 bytes LE)
        $nlen = [uint16](-bor $blockLen -bxor 0xFFFF)  # bitwise NOT
        $nlenBytes = [BitConverter]::GetBytes($nlen)
        $writer.Write($nlenBytes, 0, 2)
        
        # Data
        $blockData = New-Object byte[] $blockLen
        [Array]::Copy($rawBytes, $offset, $blockData, 0, $blockLen)
        $writer.Write($blockData, 0, $blockLen)
        
        $offset += $blockLen
        $remaining -= $blockLen
    }
    
    $writer.Close()
    return $ms.ToArray()
}

# Build scanlines: filter byte (0=none) + RGB pixels
$rawMs = New-Object System.IO.MemoryStream
$rawWriter = New-Object System.IO.BinaryWriter($rawMs)
for ($y = 0; $y -lt $height; $y++) {
    $rawWriter.Write([byte]0)  # filter: none
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

# Build deflate stream
$deflateBytes = Build-DeflateStored $rawBytes

# Write PNG file
$ms = New-Object System.IO.MemoryStream
$writer = New-Object System.IO.BinaryWriter($ms)

# PNG signature
$sig = [byte[]](0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
$writer.Write($sig, 0, 8)

# IHDR
$ihdrMs = New-Object System.IO.MemoryStream
$ihdrWriter = New-Object System.IO.BinaryWriter($ihdrMs)
$ihdrWriter.Write([uint32]$width)   # will fix endianness
$ihdrWriter.Write([uint32]$height)
$ihdrWriter.Write([byte]8)   # bit depth
$ihdrWriter.Write([byte]2)   # color type = RGB
$ihdrWriter.Write([byte]0)   # compression
$ihdrWriter.Write([byte]0)   # filter
$ihdrWriter.Write([byte]0)   # interlace
$ihdrBytes = $ihdrMs.ToArray()
# Fix endianness for width/height (first 8 bytes)
[Array]::Reverse($ihdrBytes, 0, 4)
[Array]::Reverse($ihdrBytes, 4, 4)
$ihdrWriter.Close()
$ihdrMs.Close()
Write-Chunk $writer "IHDR" $ihdrBytes

# IDAT
Write-Chunk $writer "IDAT" $deflateBytes

# IEND
Write-Chunk $writer "IEND" @()

$writer.Close()
$fileBytes = $ms.ToArray()
$ms.Close()

[System.IO.File]::WriteAllBytes($path, $fileBytes)