package net.enderwish.HUD_Visuals_Subpack.api;

import net.minecraft.resources.ResourceLocation;

/**
 * Updated Weather Definition.
 * Includes visual properties to fix sky rendering and particle behavior.
 */
public record WeatherType(
        ResourceLocation id,
        float tempModifier,     // Degree offset: e.g., -10.0 for Blizzard
        float wetnessRate,      // 0.0 to 1.0
        boolean isRare,         // Triggers the 24-hour "Rare Lock"
        WeatherRarity rarity,   // COMMON, UNCOMMON, or RARE

        // --- VISUAL PROPERTIES ---
        boolean hasPrecipitation, // True for Rain/Snow/Thunder, False for Clear/Fog
        int fogColor,             // Hex color for the horizon (e.g., 0xC0D8FF)
        float skyIntensity        // Multiplier for sky darkness (0.0 to 1.0)
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

    /**
     * Specifically checks if this weather should use the vanilla Thunder engine.
     */
    public boolean isThunder() {
        return id.getPath().contains("thunder");
    }
}