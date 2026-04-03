package net.enderwish.HUD_Visuals_Subpack.api;

import net.minecraft.resources.ResourceLocation;

/**
 * A professional, tag-aware weather definition.
 * Matches Alpha Test Doc for temp modifiers (-20 for Diamond Dust, +10 for Thaw).
 */
public record WeatherType(
        ResourceLocation id,
        float tempModifier,     // Degree offset: e.g., -10.0 for Blizzard, +10.0 for Heatwave
        float wetnessRate,      // 0.0 to 1.0 (Thaw is 0.8, Clear is 0.0)
        boolean isRare,         // Triggers the 24-hour "Rare Lock"
        WeatherRarity rarity    // COMMON, UNCOMMON, or RARE
) {
    public enum WeatherRarity {
        COMMON, UNCOMMON, RARE
    }

    /**
     * Helper to return the string ID (e.g., "gh_hud_visuals:diamond_dust")
     */
    public String getIdString() {
        return id.toString();
    }
}