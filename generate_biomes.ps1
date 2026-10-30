# Create all missing biome data JSONs for GH TerraForma
# Based on GHBiomeProvider.java definitions

$bioDir = "C:\Intellij code\NeoForge-1.21.1-Grey-Horizons RPG\Farming_Overhaul_Subpack\src\main\resources\data\gh_terraforma\worldgen\biome\"

# Biome definitions: name, temperature, downfall, precipitation, fog, water, waterFog, sky, grass?, foliage?, lavaLakes?
$biomes = @(
    @{ name="frozen_tundra"; temp=-0.7; down=0.3; precip=$true; fog=0xC0D8FF; water=0x3F76E4; wfog=0x050533; sky=0x7DA4FF; grass=0xA0B0A0; foliage=0x90A090; lava=$false }
    @{ name="glacial_peaks"; temp=-1.0; down=0.5; precip=$true; fog=0xC8E0FF; water=0x3D57D6; wfog=0x050533; sky=0x9AC0FF; grass=0x808080; foliage=0x707070; lava=$false }
    @{ name="polar_ice_cap"; temp=-0.9; down=0.1; precip=$false; fog=0xD8E8FF; water=0x3D57D6; wfog=0x030523; sky=0xA0C0FF; grass=0xE0E0E0; foliage=0xD0D0D0; lava=$false }
    @{ name="boreal_forest"; temp=-0.5; down=0.4; precip=$true; fog=0xB0C8E8; water=0x3F76E4; wfog=0x050533; sky=0x84A8E8; grass=0x5B7A2C; foliage=0x4A7A1C; lava=$false }
    @{ name="temperate_forest"; temp=0.6; down=0.7; precip=$true; fog=0xC0D8FF; water=0x3F76E4; wfog=0x050533; sky=0x78A7FF; grass=""; foliage=""; lava=$false }
    @{ name="temperate_deciduous_forest"; temp=0.5; down=0.6; precip=$true; fog=0xB0C8E8; water=0x3F76E4; wfog=0x050533; sky=0x78A7FF; grass=0x7AAB6D; foliage=0x6DAA3F; lava=$false }
    @{ name="pine_forest"; temp=0.3; down=0.5; precip=$true; fog=0xA0B8E8; water=0x3F76E4; wfog=0x050533; sky=0x84A8E8; grass=0x5B7A2C; foliage=0x4A7A1C; lava=$false }
    @{ name="temperate_grassland"; temp=0.8; down=0.4; precip=$true; fog=0xC0D8FF; water=0x3F76E4; wfog=0x050533; sky=0x76A7FF; grass=0xA0C050; foliage=""; lava=$false }
    @{ name="cherry_grove"; temp=0.6; down=0.5; precip=$true; fog=0xC0D8FF; water=0x3F76E4; wfog=0x050533; sky=0x78A7FF; grass=""; foliage=0xE090C0; lava=$false }
    @{ name="highland_orchard"; temp=0.4; down=0.6; precip=$true; fog=0xB0C8E8; water=0x3F76E4; wfog=0x050533; sky=0x84A8E8; grass=0x7AAB6D; foliage=0x6DAA3F; lava=$false }
    @{ name="rolling_meadows"; temp=0.7; down=0.5; precip=$true; fog=0xC0D8FF; water=0x3F76E4; wfog=0x050533; sky=0x76A7FF; grass=0x91BD59; foliage=""; lava=$false }
    @{ name="birch_highlands"; temp=0.5; down=0.6; precip=$true; fog=0xC0D8FF; water=0x3F76E4; wfog=0x050533; sky=0x78A7FF; grass=""; foliage=""; lava=$false }
    @{ name="wetlands"; temp=0.5; down=0.9; precip=$true; fog=0x9BBCB8; water=0x617B64; wfog=0x232317; sky=0x7BA5A0; grass=0x6A7039; foliage=0x6A7039; lava=$false }
    @{ name="swamp"; temp=0.4; down=0.8; precip=$true; fog=0x889060; water=0x4A6B3C; wfog=0x1A2A1A; sky=0x6B8A50; grass=0x5B7039; foliage=0x4B7029; lava=$false }
    @{ name="mediterranean_scrubland"; temp=1.2; down=0.2; precip=$false; fog=0xE8D8B0; water=0x3F76E4; wfog=0x050533; sky=0xE8C878; grass=0xA8A050; foliage=0x8C9A3C; lava=$false }
    @{ name="olive_grove"; temp=1.0; down=0.3; precip=$false; fog=0xD8C8A0; water=0x8C9A3C; wfog=0x403020; sky=0xD8C878; grass=0x7A9850; foliage=0x6A8A3C; lava=$false }
    @{ name="cypress_forest"; temp=0.8; down=0.4; precip=$true; fog=0xB8C8D8; water=0x3F76E4; wfog=0x050533; sky=0x88A8C8; grass=0x5A7A3C; foliage=0x4A6A2C; lava=$false }
    @{ name="coastal_cliffs"; temp=0.9; down=0.4; precip=$true; fog=0xC8D8E8; water=0x3A7FBF; wfog=0x050D20; sky=0x78B4E0; grass=0x8A9860; foliage=0x7A9050; lava=$false }
    @{ name="sea_cliffs"; temp=0.8; down=0.3; precip=$true; fog=0xB8C8D8; water=0x3A7FBF; wfog=0x050D20; sky=0x78B4E0; grass=0x7A8860; foliage=0x6A7850; lava=$false }
    @{ name="lavender_fields"; temp=1.1; down=0.2; precip=$true; fog=0xE8D8B0; water=0x3F76E4; wfog=0x050533; sky=0xE8C878; grass=0xA0A0E0; foliage=0x9080D0; lava=$false }
    @{ name="tropical_rainforest"; temp=1.8; down=0.9; precip=$true; fog=0x9DC0A0; water=0x3F76E4; wfog=0x050533; sky=0x77A89A; grass=0x59C93C; foliage=0x30BB0B; lava=$false }
    @{ name="jungle_hills"; temp=1.7; down=0.8; precip=$true; fog=0x8DC0A0; water=0x3F76E4; wfog=0x050533; sky=0x77A89A; grass=0x59C93C; foliage=0x30BB0B; lava=$false }
    @{ name="mangrove_coast"; temp=1.6; down=0.8; precip=$true; fog=0x8DB8A0; water=0x3D7A5E; wfog=0x1A3A2A; sky=0x77A89A; grass=0x4DC040; foliage=0x28A018; lava=$false }
    @{ name="palm_oasis"; temp=1.4; down=0.6; precip=$true; fog=0xC8D8B0; water=0x4D8A5E; wfog=0x1A3A2A; sky=0x77A89A; grass=0x6DC040; foliage=0x48A018; lava=$false }
    @{ name="volcanic_lowlands"; temp=1.5; down=0.4; precip=$false; fog=0xC8A060; water=0x3F76E4; wfog=0x050533; sky=0xC07840; grass=0x6B7044; foliage=0x5A6B30; lava=$true }
    @{ name="volcanic_highlands"; temp=1.6; down=0.3; precip=$false; fog=0xB8A060; water=0x4F76E4; wfog=0x050533; sky=0xC07840; grass=0x6B7044; foliage=0x5A6B30; lava=$false }
    @{ name="tropical_savanna"; temp=1.4; down=0.2; precip=$true; fog=0xD8C8A0; water=0x3F76E4; wfog=0x050533; sky=0xE8C878; grass=0xA8A050; foliage=0x8C9A3C; lava=$false }
    @{ name="ash_plains"; temp=0.5; down=0.0; precip=$false; fog=0x808070; water=0x606060; wfog=0x303030; sky=0x707060; grass=0x606050; foliage=0x585840; lava=$false }
    @{ name="salt_flats"; temp=0.4; down=0.1; precip=$false; fog=0xE8E8E0; water=0x606060; wfog=0x303030; sky=0xD0D0D0; grass=0xE0E0E0; foliage=0xD8D8D8; lava=$false }
    @{ name="cracked_badlands"; temp=1.8; down=0.0; precip=$false; fog=0xC0905A; water=0x905A30; wfog=0x401800; sky=0xC08040; grass=0x907050; foliage=0x807040; lava=$true }
    @{ name="red_wastes"; temp=1.9; down=0.0; precip=$false; fog=0xB0805A; water=0x805A30; wfog=0x401800; sky=0xB08040; grass=0x807050; foliage=0x706040; lava=$false }
    @{ name="petrified_forest"; temp=0.6; down=0.1; precip=$false; fog=0x888060; water=0x3F6B44; wfog=0x051505; sky=0x788060; grass=0x5B7A2C; foliage=0x4A7A1C; lava=$false }
    @{ name="overgrown_ruins"; temp=0.8; down=0.6; precip=$true; fog=0x889060; water=0x3F6B44; wfog=0x051505; sky=0x789060; grass=0x5B7A2C; foliage=0x4A7A1C; lava=$false }
    @{ name="frozen_sea"; temp=-0.8; down=0.5; precip=$true; fog=0xC0D8FF; water=0x2A4A9A; wfog=0x032040; sky=0x8AB4FF; grass=""; foliage=""; lava=$false }
    @{ name="temperate_sea"; temp=0.5; down=0.5; precip=$true; fog=0xC0D8FF; water=0x3F76E4; wfog=0x050533; sky=0x78A7FF; grass=""; foliage=""; lava=$false }
    @{ name="tropical_reef_waters"; temp=1.2; down=0.8; precip=$true; fog=0x9DE0D0; water=0x1FA8A0; wfog=0x0A4038; sky=0x77D8C8; grass=""; foliage=""; lava=$false }
    @{ name="kelp_forest"; temp=0.6; down=0.6; precip=$true; fog=0x8DC0A0; water=0x3F76E4; wfog=0x050533; sky=0x77A89A; grass=""; foliage=""; lava=$false }
    @{ name="abyssal_plain"; temp=0.3; down=0.4; precip=$true; fog=0x404040; water=0x2A3A4A; wfog=0x031020; sky=0x405060; grass=""; foliage=""; lava=$false }
    @{ name="mangrove_swamp_coast"; temp=1.5; down=0.9; precip=$true; fog=0x7DA8A0; water=0x3D7A5E; wfog=0x1A3A2A; sky=0x67A48A; grass=0x4DC040; foliage=0x28A018; lava=$false }
)

$count = 0
foreach ($b in $biomes) {
    $name = $b.name
    
    # Build effects object
    $effects = @{
        fog_color       = $b.fog
        water_color     = $b.water
        water_fog_color = $b.wfog
        sky_color       = $b.sky
    }
    if ($b.grass -ne "") { $effects.grass_color = $b.grass }
    if ($b.foliage -ne "") { $effects.foliage_color = $b.foliage }
    
    # Build features array (11 steps: 0-10)
    $features = @()
    $features += @()  # step 0: empty
    
    # step 1: lava lakes for volcanic_lowlands and cracked_badlands
    if ($b.lava) {
        $features += @("minecraft:lake_lava_underground", "minecraft:lake_lava_surface")
    } else {
        $features += @()
    }
    
    # steps 2-10: empty
    for ($i = 2; $i -le 10; $i++) { $features += @() }
    
    # Build spawners
    $spawners = @{
        monster                   = @()
        creature                  = @()
        ambient                   = @()
        water_creature            = @()
        water_ambient             = @()
        underground_water_creature= @()
        misc                      = @()
    }
    
    # Build the full JSON
    $json = @{
        has_precipitation = $b.precip
        temperature       = $b.temp
        downfall          = $b.down
        effects           = $effects
        carvers           = @{ air = @("minecraft:cave", "minecraft:canyon") }
        features          = $features
        spawners          = $spawners
        spawn_costs       = @{}
    }
    
    $json | ConvertTo-Json -Depth 6 | Set-Content -Path "$bioDir$name.json" -Encoding UTF8
    $count++
    Write-Host "Created $name.json"
}

Write-Host "Total biome JSONs created: $count"