package net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack;

/**
 * BackpackConfig
 *
 * One backpack tier's tuning, loaded from
 * data/gh_belliarium_monstrarium/backpacks/&lt;id&gt;.json (JSON-driven like
 * everything else in this mod — no hardcoded tier enums).
 */
public record BackpackConfig(
        String id,
        int slots,
        float selfWeightKg,
        float carryCapBonusPct,
        float heavyLoadReliefPct) {

    /** Weight fraction above which the heavy-load stamina relief kicks in. */
    public static final float HEAVY_THRESHOLD = 0.5f;
}
