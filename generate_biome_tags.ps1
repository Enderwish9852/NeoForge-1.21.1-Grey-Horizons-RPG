# Create biome tag JSONs so gh_terraforma biomes appear in /locate biome dropdown
# These tags categorize biomes so they show up in autocomplete

$baseDir = "C:\Intellij code\NeoForge-1.21.1-Grey-Horizons RPG\Farming_Overhaul_Subpack\src\main\resources\data"

# All gh_terraforma biomes
$allBiomes = @(
    "gh_terraforma:frozen_tundra",
    "gh_terraforma:glacial_peaks",
    "gh_terraforma:polar_ice_cap",
    "gh_terraforma:boreal_forest",
    "gh_terraforma:temperate_forest",
    "gh_terraforma:temperate_deciduous_forest",
    "gh_terraforma:pine_forest",
    "gh_terraforma:temperate_grassland",
    "gh_terraforma:cherry_grove",
    "gh_terraforma:highland_orchard",
    "gh_terraforma:rolling_meadows",
    "gh_terraforma:birch_highlands",
    "gh_terraforma:wetlands",
    "gh_terraforma:swamp",
    "gh_terraforma:mediterranean_scrubland",
    "gh_terraforma:olive_grove",
    "gh_terraforma:cypress_forest",
    "gh_terraforma:coastal_cliffs",
    "gh_terraforma:sea_cliffs",
    "gh_terraforma:lavender_fields",
    "gh_terraforma:tropical_rainforest",
    "gh_terraforma:jungle_hills",
    "gh_terraforma:mangrove_coast",
    "gh_terraforma:palm_oasis",
    "gh_terraforma:volcanic_lowlands",
    "gh_terraforma:volcanic_highlands",
    "gh_terraforma:tropical_savanna",
    "gh_terraforma:ash_plains",
    "gh_terraforma:salt_flats",
    "gh_terraforma:cracked_badlands",
    "gh_terraforma:red_wastes",
    "gh_terraforma:petrified_forest",
    "gh_terraforma:overgrown_ruins",
    "gh_terraforma:frozen_sea",
    "gh_terraforma:temperate_sea",
    "gh_terraforma:tropical_reef_waters",
    "gh_terraforma:kelp_forest",
    "gh_terraforma:abyssal_plain",
    "gh_terraforma:mangrove_swamp_coast"
)

# Categories for vanilla minecraft tags
$arctic = @("gh_terraforma:frozen_tundra", "gh_terraforma:glacial_peaks", "gh_terraforma:polar_ice_cap", "gh_terraforma:boreal_forest")
$desert = @("gh_terraforma:red_wastes", "gh_terraforma:cracked_badlands", "gh_terraforma:ash_plains", "gh_terraforma:salt_flats", "gh_terraforma:petrified_forest")
$jungle = @("gh_terraforma:tropical_rainforest", "gh_terraforma:jungle_hills", "gh_terraforma:mangrove_coast")
$forest = @("gh_terraforma:temperate_forest", "gh_terraforma:temperate_deciduous_forest", "gh_terraforma:pine_forest", "gh_terraforma:birch_highlands", "gh_terraforma:cherry_grove", "gh_terraforma:highland_orchard", "gh_terraforma:overgrown_ruins")
$plains = @("gh_terraforma:temperate_grassland", "gh_terraforma:rolling_meadows", "gh_terraforma:mediterranean_scrubland", "gh_terraforma:tropical_savanna")
$aquatic = @("gh_terraforma:frozen_sea", "gh_terraforma:temperate_sea", "gh_terraforma:tropical_reef_waters", "gh_terraforma:kelp_forest", "gh_terraforma:abyssal_plain", "gh_terraforma:mangrove_swamp_coast")
$wasteland = @("gh_terraforma:red_wastes", "gh_terraforma:cracked_badlands", "gh_terraforma:ash_plains", "gh_terraforma:salt_flats", "gh_terraforma:petrified_forest", "gh_terraforma:overgrown_ruins")

# Create gh_terraforma tags
$terraformaDir = "$baseDir/gh_terraforma/tags/biome/"
$terraformaTags = @{
    "all" = $allBiomes
    "is_snowy" = $arctic
    "is_desert" = $desert
    "is_jungle" = $jungle
    "is_forest" = $forest
    "is_plains" = $plains
    "is_aquatic" = $aquatic
    "is_wasteland" = $wasteland
}

$count = 0
foreach ($tagName in $terraformaTags.Keys) {
    $values = $terraformaTags[$tagName]
    $json = @{
        values = $values
        replace = $false
    }
    $json | ConvertTo-Json -Depth 3 | Set-Content -Path "$terraformaDir$tagName.json" -Encoding UTF8
    $count++
    Write-Host "Created gh_terraforma/tags/biome/$tagName.json"
}

# Add gh biomes to vanilla minecraft tags
$minecraftDir = "$baseDir/minecraft/tags/biome/"
$minecraftTags = @{
    "is_snowy" = $arctic
    "is_desert" = $desert
    "is_jungle" = $jungle
    "is_forest" = $forest
    "is_plains" = $plains
    "is_aquatic" = $aquatic
}

foreach ($tagName in $minecraftTags.Keys) {
    $values = $minecraftTags[$tagName]
    $json = @{
        values = $values
        replace = $false
    }
    $json | ConvertTo-Json -Depth 3 | Set-Content -Path "$minecraftDir$tagName.json" -Encoding UTF8
    $count++
    Write-Host "Created minecraft/tags/biome/$tagName.json"
}

Write-Host "Total biome tag files created: $count"