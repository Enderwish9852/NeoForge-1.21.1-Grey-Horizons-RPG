package net.enderwish.Farming_Overhaul_Subpack.core.spoilage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * SpoilageComponent
 *
 * A data component attached to every spoilable food item stack.
 * Stores the current spoilage state — how far along the item is
 * toward becoming Rotten Scrap.
 *
 * Attached to an item stack when the crop is harvested.
 * Updated every server tick by SpoilageHandler.
 * Read by tooltip renderer to show the spoilage bar.
 * Read by FarmingAPI for other subpacks.
 *
 * Spoilage stages (for tooltip colour only — no named states in code):
 *   0.00 - 0.75  = Fresh  (green bar)
 *   0.75 - 0.99  = Stale  (orange bar)
 *   1.00+        = Spoiled (red bar — item converts to Rotten Scrap next tick)
 *
 * spoiledTicks     — how many ticks of spoilage have accumulated
 * maxSpoilTicks    — total ticks before fully spoiled (from CropDefinition.spoilTicks())
 * spoilRatePerTick — how fast spoilage accumulates (1.0 = normal, 2.0 = double speed)
 */
public record SpoilageComponent(
        int spoiledTicks,
        int maxSpoilTicks,
        float spoilRatePerTick
) {

    // ── Codec ─────────────────────────────────────────────────────────────────

    public static final Codec<SpoilageComponent> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("spoiled_ticks").forGetter(SpoilageComponent::spoiledTicks),
                    Codec.INT.fieldOf("max_spoil_ticks").forGetter(SpoilageComponent::maxSpoilTicks),
                    Codec.FLOAT.fieldOf("spoil_rate_per_tick").forGetter(SpoilageComponent::spoilRatePerTick)
            ).apply(instance, SpoilageComponent::new)
    );

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Creates a fresh SpoilageComponent for a newly harvested crop.
     * spoiledTicks starts at 0 — fully fresh.
     * spoilRatePerTick starts at 1.0 — normal speed.
     */
    public static SpoilageComponent fresh(int maxSpoilTicks) {
        return new SpoilageComponent(0, maxSpoilTicks, 1.0f);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Returns spoilage progress as a 0.0 - 1.0+ fraction.
     * 0.0 = perfectly fresh, 1.0 = fully spoiled.
     */
    public float getProgress() {
        if (maxSpoilTicks <= 0) return 0.0f;
        return (float) spoiledTicks / maxSpoilTicks;
    }

    /** Returns true if the item is still fresh (progress below 75%). */
    public boolean isFresh() {
        return getProgress() < 0.75f;
    }

    /** Returns true if the item is stale (progress between 75% and 100%). */
    public boolean isStale() {
        float p = getProgress();
        return p >= 0.75f && p < 1.0f;
    }

    /** Returns true if the item is fully spoiled and should convert to Rotten Scrap. */
    public boolean isSpoiled() {
        return getProgress() >= 1.0f;
    }

    /**
     * Returns a new SpoilageComponent with spoiledTicks advanced by one tick.
     * spoilRatePerTick is the multiplier — 1.0 normal, 1.2 near stale, 2.0 near spoiled.
     * Records are immutable so we always return a new instance.
     */
    public SpoilageComponent tick(float rateMultiplier) {
        int newTicks = (int) Math.min(
                maxSpoilTicks,
                spoiledTicks + rateMultiplier * spoilRatePerTick
        );
        return new SpoilageComponent(newTicks, maxSpoilTicks, spoilRatePerTick);
    }

    /**
     * Returns a new SpoilageComponent with the spoil rate multiplier applied.
     * Used by SpoilageHandler when applying preservation effects.
     * e.g. pass 0.5f for a cellar slowing spoilage by 50%.
     */
    public SpoilageComponent withRate(float newRate) {
        return new SpoilageComponent(spoiledTicks, maxSpoilTicks, newRate);
    }
}