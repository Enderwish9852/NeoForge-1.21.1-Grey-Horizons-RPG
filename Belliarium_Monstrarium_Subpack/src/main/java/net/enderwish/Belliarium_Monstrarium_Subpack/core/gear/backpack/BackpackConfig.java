package net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack;

/**
 * BackpackConfig
 *
 * One backpack tier's tuning, loaded from
 * data/gh_belliarium_monstrarium/backpacks/&lt;id&gt;.json (JSON-driven like
 * everything else in this mod — no hardcoded tier enums).
 *
 * carryCapBonusKg is a FLAT bonus added to the 30 kg base carry cap (it
 * is not a percentage). specialLeft/specialRight are optional item
 * categories (ToolProfileRegistry categories) that may be placed in the
 * bag's two special slots — null means the bag has no slot on that side.
 */
public record BackpackConfig(
        String id,
        int slots,
        float selfWeightKg,
        float carryCapBonusKg,
        float speedBonusPct,
        float staminaEfficiencyPct,
        String specialLeft,
        String specialRight) {

    /** Weight fraction above which the stamina-efficiency relief kicks in. */
    public static final float HEAVY_THRESHOLD = 0.5f;
}
