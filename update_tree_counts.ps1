# Increase tree spawn counts in flourishing biomes
# Dense types (oak_dense, birch_dense, jungle_dense): 8 → 12
# Normal types (apple_normal, pear_normal, etc.): 3 → 6
# Sparse types (spruce_sparse, etc.): keep at 1

$pfDir = "C:\Intellij code\NeoForge-1.21.1-Grey-Horizons RPG\Farming_Overhaul_Subpack\src\main\resources\data\gh_farming_overhaul\worldgen\placed_feature\"

$files = Get-ChildItem -Path $pfDir -Filter "*.json"

$count = 0
foreach ($file in $files) {
    $content = Get-Content -Path $file.FullName -Raw
    
    # Replace count values
    $newContent = $content -replace '"count":\s*8', '"count": 12'
    $newContent = $newContent -replace '"count":\s*3', '"count": 6'
    
    if ($content -ne $newContent) {
        $newContent | Set-Content -Path $file.FullName -Encoding UTF8
        $count++
        Write-Host "Updated $($file.Name)"
    }
}

Write-Host "Total placed feature files updated: $count"