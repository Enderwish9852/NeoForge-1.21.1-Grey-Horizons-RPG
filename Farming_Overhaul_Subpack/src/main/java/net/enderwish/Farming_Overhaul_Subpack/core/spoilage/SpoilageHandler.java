package net.enderwish.Farming_Overhaul_Subpack.core.spoilage;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.item.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * SpoilageHandler
 *
 * Runs every server tick for each player.
 * Scans the player's entire inventory for spoilable items and:
 *
 *   1. Calculates the contamination multiplier for each slot
 *      based on neighbouring stale/spoiled items in the same inventory.
 *   2. Advances the spoilage bar by one tick × multiplier.
 *   3. Converts fully spoiled items to Rotten Scrap.
 *
 * Contamination rules (same inventory only):
 *   - Each stale item stack nearby  → ×1.2 multiplier (stacks)
 *   - Each spoiled item stack nearby → ×2.0 multiplier (stacks)
 *
 * This handler only runs server-side.
 * Client tooltip rendering reads the component directly — no extra sync needed
 * because NeoForge syncs data components automatically.
 */
@EventBusSubscriber(modid = FarmingOverhaulSubpack.MODID, bus = EventBusSubscriber.Bus.GAME)
public class SpoilageHandler {

    // ── Contamination multipliers ─────────────────────────────────────────────
    // These will move to a config system in the future

    private static final float STALE_MULTIPLIER  = 1.2f;
    private static final float SPOILED_MULTIPLIER = 2.0f;

    // ── Tick interval ─────────────────────────────────────────────────────────
    // Check every 20 ticks (1 second) to avoid running every single tick

    private static final int TICK_INTERVAL = 20;

    // ── Tick ──────────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        // Server side only
        if (player.level().isClientSide()) return;

        // Only run every TICK_INTERVAL ticks
        if (player.tickCount % TICK_INTERVAL != 0) return;

        processInventory(player);
    }

    // ── Inventory processing ──────────────────────────────────────────────────

    /**
     * Scans the entire player inventory.
     * Builds a snapshot of spoilage states first, then applies updates.
     * This prevents one item's conversion affecting another's multiplier
     * within the same tick.
     */
    private static void processInventory(Player player) {
        List<ItemStack> allStacks = getAllSpoilableStacks(player);

        // Step 1: count stale and spoiled stacks in this inventory
        int staleCount  = 0;
        int spoiledCount = 0;

        for (ItemStack stack : allStacks) {
            SpoilageComponent comp = stack.get(ModDataComponents.SPOILAGE.get());
            if (comp == null) continue;
            if (comp.isStale())   staleCount++;
            if (comp.isSpoiled()) spoiledCount++;
        }

        // Step 2: calculate the global multiplier for this inventory
        // Each stale stack adds ×1.2, each spoiled stack adds ×2.0
        // Multipliers stack additively on top of base 1.0
        float multiplier = 1.0f
                + (staleCount  * (STALE_MULTIPLIER  - 1.0f))
                + (spoiledCount * (SPOILED_MULTIPLIER - 1.0f));

        // Step 3: apply tick to every spoilable stack
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            SpoilageComponent comp = stack.get(ModDataComponents.SPOILAGE.get());
            if (comp == null) continue;

            // Advance spoilage — TICK_INTERVAL ticks worth at once
            // since we only run every TICK_INTERVAL ticks
            SpoilageComponent updated = comp;
            for (int t = 0; t < TICK_INTERVAL; t++) {
                updated = updated.tick(multiplier);
            }

            if (updated.isSpoiled()) {
                // Convert to Rotten Scrap — preserve stack size
                ItemStack rottenStack = new ItemStack(
                        ModItems.ROTTEN_SCRAP.get(),
                        stack.getCount()
                );
                player.getInventory().setItem(i, rottenStack);
            } else {
                // Write updated component back to the stack
                stack.set(ModDataComponents.SPOILAGE.get(), updated);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns all item stacks in the player's inventory that have
     * a SpoilageComponent attached.
     */
    private static List<ItemStack> getAllSpoilableStacks(Player player) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.has(ModDataComponents.SPOILAGE.get())) {
                result.add(stack);
            }
        }
        return result;
    }
}