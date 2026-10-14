package net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.integration.CuriosHooks;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.gear.BackpackItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * BackpackHandler
 *
 * Per-player helpers the rest of the mod calls for backpack effects.
 * All numbers live in the backpack JSONs; this class only reads them.
 */
public final class BackpackHandler {

    private BackpackHandler() {}

    /** The stack equipped in the Curios "back" slot, or ItemStack.EMPTY. */
    public static ItemStack getEquipped(Player player) {
        return CuriosHooks.getBackpack(player);
    }

    public static BackpackConfig configOf(ItemStack stack) {
        if (stack.getItem() instanceof BackpackItem bp) {
            return BackpackRegistry.INSTANCE.getOrFallback(bp.backpackId(stack));
        }
        return new BackpackConfig("none", 0, 0f, 0f, 0f);
    }

    /** Effective carry cap in kg (base 30 × pack bonus). */
    public static float getMaxCarryKg(Player player) {
        float base = net.enderwish.Belliarium_Monstrarium_Subpack.event.WeightEnforcementHandler.MAX_CARRY_KG;
        ItemStack bp = getEquipped(player);
        if (bp.isEmpty()) return base;
        return base * (1f + configOf(bp).carryCapBonusPct());
    }

    /**
     * Heavy-load stamina relief multiplier: 1.0 unless a backpack with a
     * heavyLoadReliefPct rating is equipped AND the player is carrying
     * above the heavy threshold — then (1 - relief), softening all
     * weight-driven stamina drains.
     */
    public static float getHeavyLoadRelief(Player player, float weightFraction) {
        ItemStack bp = getEquipped(player);
        if (bp.isEmpty()) return 1.0f;
        BackpackConfig cfg = configOf(bp);
        if (cfg.heavyLoadReliefPct() <= 0f) return 1.0f;
        if (weightFraction < BackpackConfig.HEAVY_THRESHOLD) return 1.0f;
        return 1.0f - cfg.heavyLoadReliefPct();
    }
}
