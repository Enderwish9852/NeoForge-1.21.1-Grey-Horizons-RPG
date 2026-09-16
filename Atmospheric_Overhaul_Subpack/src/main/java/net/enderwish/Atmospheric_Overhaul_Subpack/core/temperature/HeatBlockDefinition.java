package net.enderwish.Atmospheric_Overhaul_Subpack.core.temperature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * HeatBlockDefinition
 *
 * Defines the temperature contribution of a single block type for
 * LocalChunkTemperature's nearby-block scan. Replaces the old hardcoded
 * LAVA_TEMP/FIRE_TEMP/etc. constants that used to live directly in
 * LocalChunkTemperature.
 *
 * JSON example (lava):
 * {
 *   "block": "minecraft:lava",
 *   "temperature": 1.2
 * }
 *
 * requires_lit only applies to blocks with a LIT blockstate property
 * (campfires, furnaces, smokers) — when true, the block only contributes
 * its temperature value while lit.
 */
public record HeatBlockDefinition(
        String block,
        float temperature,
        boolean requiresLit
) {
    public static final Codec<HeatBlockDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("block").forGetter(HeatBlockDefinition::block),
                    Codec.FLOAT.fieldOf("temperature").forGetter(HeatBlockDefinition::temperature),
                    Codec.BOOL.optionalFieldOf("requires_lit", false).forGetter(HeatBlockDefinition::requiresLit)
            ).apply(instance, HeatBlockDefinition::new)
    );
}
