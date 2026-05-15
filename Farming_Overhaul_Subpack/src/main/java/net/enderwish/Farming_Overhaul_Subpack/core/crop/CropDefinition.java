package net.enderwish.Farming_Overhaul_Subpack.core.crop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * CropDefinition
 *
 * Defines the biological rules for a single crop type.
 * Loaded from JSON by CropRegistry.
 *
 * No imports from Atmospheric internals — season names are plain strings.
 * CropGrowthHandler compares against SeasonsAPI.getSeason(level).name().
 *
 * JSON example (wheat):
 * {
 *   "growth_type": "GROUND",
 *   "growth_days": 12,
 *   "spoil_days": 30,
 *   "weight_kg": 0.10,
 *   "stack_size": 64,
 *   "nutrition": 2,
 *   "hydration": 0,
 *   "year_round": false,
 *   "seasons": ["SPRING", "SUMMER"]
 * }
 */
public record CropDefinition(
        GrowthType growthType,
        int growthDays,
        int spoilDays,
        float weightKg,
        int stackSize,
        int nutrition,
        int hydration,
        boolean yearRound,
        List<String> seasons
) {

    // ── Codec ─────────────────────────────────────────────────────────────────

    public static final Codec<CropDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    GrowthType.CODEC.fieldOf("growth_type").forGetter(CropDefinition::growthType),
                    Codec.INT.fieldOf("growth_days").forGetter(CropDefinition::growthDays),
                    Codec.INT.fieldOf("spoil_days").forGetter(CropDefinition::spoilDays),
                    Codec.FLOAT.fieldOf("weight_kg").forGetter(CropDefinition::weightKg),
                    Codec.INT.fieldOf("stack_size").forGetter(CropDefinition::stackSize),
                    Codec.INT.fieldOf("nutrition").forGetter(CropDefinition::nutrition),
                    Codec.INT.fieldOf("hydration").forGetter(CropDefinition::hydration),
                    Codec.BOOL.fieldOf("year_round").forGetter(CropDefinition::yearRound),
                    Codec.STRING.listOf().fieldOf("seasons").forGetter(CropDefinition::seasons)
            ).apply(instance, CropDefinition::new)
    );

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns true if this crop can grow in the given season.
     * seasonName comes from SeasonsAPI.getSeason(level).name()
     * e.g. "SPRING", "SUMMER", "AUTUMN", "WINTER"
     */
    public boolean canGrowIn(String seasonName) {
        if (yearRound) return true;
        return seasons.contains(seasonName.toUpperCase());
    }

    /**
     * Returns the spoil duration in ticks.
     * 1 day = 24000 ticks.
     */
    public int spoilTicks() {
        return spoilDays * 24000;
    }

    /**
     * Returns the season tooltip string for item display.
     * e.g. "Spring, Summer" or "Year Round"
     * Used by FarmingAPI.getSeasonTooltip() for other subpacks.
     */
    public String getSeasonTooltip() {
        if (yearRound) return "Year Round";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < seasons.size(); i++) {
            String s = seasons.get(i);
            // Capitalise first letter only e.g. SPRING → Spring
            sb.append(s.charAt(0)).append(s.substring(1).toLowerCase());
            if (i < seasons.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    // ── GrowthType enum ───────────────────────────────────────────────────────

    /**
     * Defines how this crop physically grows.
     * Used by CropGrowthHandler to apply the correct growth rules.
     */
    public enum GrowthType {
        GROUND,       // standard flat crop (wheat, barley)
        ROOT,         // underground crop (carrot, potato, onion)
        VINE,         // climbs or sprawls (grapes, kiwi, cucumber)
        BUSH,         // small shrub (blueberries, bell pepper)
        TREE,         // fruit tree (apple, orange, mango)
        STALK,        // tall vertical crop (corn, asparagus)
        LOG,          // grows on wood blocks (oyster mushroom)
        UNDERGROUND,  // buried (truffle)
        PADDY;        // requires water above soil (rice)

        public static final Codec<GrowthType> CODEC =
                Codec.STRING.xmap(
                        s -> GrowthType.valueOf(s.toUpperCase()),
                        GrowthType::name
                );
    }
}
