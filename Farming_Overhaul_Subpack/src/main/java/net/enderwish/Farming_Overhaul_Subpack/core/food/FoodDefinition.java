package net.enderwish.Farming_Overhaul_Subpack.core.food;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * FoodDefinition
 *
 * Defines spoilage and weight rules for any food item.
 * Loaded from JSON by FoodRegistry.
 *
 * Intentionally simple — crop-specific fields like seasons
 * and growth type live in CropDefinition, not here.
 *
 * JSON example (cooked beef):
 * {
 *   "spoil_days": 5,
 *   "weight_kg": 0.25
 * }
 */
public record FoodDefinition(
        int spoilDays,
        float weightKg
) {

    // ── Codec ─────────────────────────────────────────────────────────────────

    public static final Codec<FoodDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("spoil_days").forGetter(FoodDefinition::spoilDays),
                    Codec.FLOAT.fieldOf("weight_kg").forGetter(FoodDefinition::weightKg)
            ).apply(instance, FoodDefinition::new)
    );

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns the spoil duration in ticks.
     * 1 day = 24000 ticks.
     */
    public int spoilTicks() {
        return spoilDays * 24000;
    }
}
