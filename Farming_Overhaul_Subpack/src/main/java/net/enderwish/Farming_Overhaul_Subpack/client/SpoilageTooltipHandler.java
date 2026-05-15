package net.enderwish.Farming_Overhaul_Subpack.client;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropDefinition;
import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.ModDataComponents;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.SpoilageComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * SpoilageTooltipHandler
 *
 * Adds spoilage bar and season info to item tooltips client-side.
 *
 * Tooltip layout example:
 *   [████████░░] Stale
 *   Seasons: Spring, Summer
 *   Weight: 0.10 kg
 *
 * Bar colours:
 *   Green  = Fresh  (0% - 75%)
 *   Orange = Stale  (75% - 99%)
 *   Red    = Spoiled (100%+)
 */
@EventBusSubscriber(
        modid = FarmingOverhaulSubpack.MODID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT
)
public class SpoilageTooltipHandler {

    // ── Bar settings ──────────────────────────────────────────────────────────

    private static final int BAR_LENGTH = 10;
    private static final String FILLED  = "█";
    private static final String EMPTY   = "░";

    // ── Tooltip event ─────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        // ── Spoilage bar ──────────────────────────────────────────────────────
        SpoilageComponent comp = stack.get(ModDataComponents.SPOILAGE.get());
        if (comp != null) {
            event.getToolTip().add(buildSpoilageBar(comp));
        }

        // ── Season and weight tooltip ─────────────────────────────────────────
        // Look up crop definition by item registry name
        String itemId = stack.getItem().builtInRegistryHolder()
                .key().location().getPath();

        if (CropRegistry.INSTANCE.isRegistered(itemId)) {
            CropDefinition def = CropRegistry.INSTANCE.getByName(itemId);

            // Season line — e.g. "Seasons: Spring, Summer" or "Year Round"
            event.getToolTip().add(Component.literal(
                    "Seasons: " + def.getSeasonTooltip()
            ).withStyle(ChatFormatting.DARK_GREEN));

            // Weight line — e.g. "Weight: 0.10 kg"
            event.getToolTip().add(Component.literal(
                    "Weight: " + def.weightKg() + " kg"
            ).withStyle(ChatFormatting.GRAY));
        }
    }

    // ── Bar builder ───────────────────────────────────────────────────────────

    /**
     * Builds a coloured spoilage bar component.
     * e.g. [████████░░] Stale
     */
    private static Component buildSpoilageBar(SpoilageComponent comp) {
        float progress = comp.getProgress();
        int filled = Math.min(BAR_LENGTH, Math.round(progress * BAR_LENGTH));
        int empty  = BAR_LENGTH - filled;

        // Choose colour based on spoilage state
        ChatFormatting barColour;
        String label;

        if (comp.isSpoiled()) {
            barColour = ChatFormatting.RED;
            label = "Spoiled";
        } else if (comp.isStale()) {
            barColour = ChatFormatting.GOLD;
            label = "Stale";
        } else {
            barColour = ChatFormatting.GREEN;
            label = "Fresh";
        }

        // Build bar string
        StringBuilder bar = new StringBuilder("[");
        bar.append(FILLED.repeat(filled));
        bar.append(EMPTY.repeat(empty));
        bar.append("] ");
        bar.append(label);

        return Component.literal(bar.toString()).withStyle(barColour);
    }
}
