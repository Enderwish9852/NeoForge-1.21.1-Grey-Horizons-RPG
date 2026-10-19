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
        return new BackpackConfig("none", 0, 0f, 0f, 0f, 0f, null, null);
    }

    /** Effective carry cap in kg = base 30 kg + the bag's flat bonus. */
    public static float getMaxCarryKg(Player player) {
        float base = net.enderwish.Belliarium_Monstrarium_Subpack.event.WeightEnforcementHandler.MAX_CARRY_KG;
        ItemStack bp = getEquipped(player);
        if (bp.isEmpty()) return base;
        return base + configOf(bp).carryCapBonusKg();
    }

    /**
     * Weight-driven stamina drain multiplier: 1.0 unless a backpack with a
     * staminaEfficiencyPct rating is equipped AND the player is carrying
     * above the heavy threshold — then (1 - efficiency), softening all
     * weight-driven stamina drains. Bags without the stat return 1.0.
     */
    public static float getWeightDrainMultiplier(Player player, float weightFraction) {
        ItemStack bp = getEquipped(player);
        if (bp.isEmpty()) return 1.0f;
        BackpackConfig cfg = configOf(bp);
        if (cfg.staminaEfficiencyPct() <= 0f) return 1.0f;
        if (weightFraction < BackpackConfig.HEAVY_THRESHOLD) return 1.0f;
        return 1.0f - cfg.staminaEfficiencyPct();
    }
}
