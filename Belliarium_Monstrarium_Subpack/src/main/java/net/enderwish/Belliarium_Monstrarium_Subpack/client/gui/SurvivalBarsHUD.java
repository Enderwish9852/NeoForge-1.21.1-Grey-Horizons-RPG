package net.enderwish.Belliarium_Monstrarium_Subpack.client.gui;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.integration.CuriosHooks;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.survival.SurvivalCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.event.WeightEnforcementHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * SurvivalBarsHUD
 *
 * NEW -- creative mode pins stamina/energy to full, and the weight bar is
 * skipped entirely (not drawn at all) rather than shown full/empty, per your
 * call. Actual weight-limit ENFORCEMENT was already skipped for creative in
 * WeightEnforcementHandler -- this only fixes what the HUD displays.
 */
public class SurvivalBarsHUD {

    public static final LayeredDraw.Layer LAYER = SurvivalBarsHUD::render;

    private static final ResourceLocation BRAIN_ICON =
            ResourceLocation.fromNamespaceAndPath("gh_belliarium_monstrarium",
                    "textures/gui/brain_icon.png");

    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 5;
    private static final int ROW_GAP = 7;

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) return;

        int x = mc.getWindow().getGuiScaledWidth() / 2 - 91;
        int baseY = mc.getWindow().getGuiScaledHeight() - 39;

        // Wetness is NOT watch-gated — you physically feel getting wet.
        // The row only exists while actually wet, so a dry player never
        // sees it. DRENCHED (100%) reads as a deeper blue than plain damp.
        if (!player.isCreative()) {
            float wetness = player.getData(ModAttachments.SURVIVAL).getWetness();
            if (wetness >= 1.0f) {
                float wetPct = wetness / 100f;
                drawBar(graphics, x, baseY - ROW_GAP * 6, wetPct, wetnessColor(wetPct));
            }
        }

        if (!CuriosHooks.hasWatchEquipped(player)) return;

        boolean creative = player.isCreative();
        SurvivalCapability survival = player.getData(ModAttachments.SURVIVAL);

        float staminaPct = creative ? 1.0f : survival.getStamina() / 100f;
        float energyPct = creative ? 1.0f : survival.getEnergy() / 100f;

        drawBar(graphics, x, baseY - ROW_GAP * 2, staminaPct, 0xFF3399FF);
        drawBar(graphics, x, baseY - ROW_GAP, energyPct, 0xFFFFAA33);

        // Sanity meter (brain icon), watch-gated like the other bars
        var sanity = player.getData(ModAttachments.SANITY);
        float sanityPct = sanity.get() / 100f;
        graphics.blit(BRAIN_ICON, x - 11, baseY - ROW_GAP * 3 - 1,
                0f, 0f, 9, 9, 9, 9);
        drawBar(graphics, x, baseY - ROW_GAP * 3, sanityPct, sanityColor(sanityPct));

        // Thirst + body temp rows. Temp bar spans the 33-41C window with the
        // live reading (C) printed to its right; colors flag the two clinical
        // status zones so hypothermia/hyperthermia read at a glance.
        float thirstPct = creative ? 1.0f : survival.getThirst() / 100f;
        drawBar(graphics, x, baseY - ROW_GAP * 4, thirstPct, 0xFF3A7BD5);
        float tempC = creative ? 37.0f : survival.getBodyTemp();
        drawBar(graphics, x, baseY - ROW_GAP * 5,
                Mth.clamp((tempC - 33.0f) / 8.0f, 0f, 1f), tempColor(tempC));
        graphics.drawString(mc.font, String.format("%.1f\u00B0C", tempC),
                x + BAR_WIDTH + 3, baseY - ROW_GAP * 5, 0xFFC8C8C8, false);

        if (!creative) {
            float weightFraction = WeightEnforcementHandler.getWeightFraction(player);
            drawBar(graphics, x, baseY, weightFraction, weightColor(weightFraction));
        }
    }

    private static int weightColor(float fraction) {
        if (fraction >= 0.75f) return 0xFFCC3333;
        if (fraction >= 0.50f) return 0xFFCC7A33;
        if (fraction >= 0.25f) return 0xFFCCAA33;
        return 0xFF996633;
    }

    private static int sanityColor(float pct) {
        if (pct <= 0.25f) return 0xFFCC3366;   // critical: sickly red-pink
        if (pct <= 0.50f) return 0xFFB45BB0;   // frayed purple
        return 0xFF9F7FD6;                      // healthy lavender
    }

    private static int tempColor(float tempC) {
        if (tempC > 40.0f) return 0xFFCC4A33;   // hyperthermia
        if (tempC < 35.0f) return 0xFF4AA8CC;   // hypothermia
        return 0xFF9FB8BE;                      // neutral steel
    }

    /** Wetness: teal when merely damp, deeper blue when DRENCHED-full. */
    private static int wetnessColor(float pct) {
        if (pct >= 0.995f) return 0xFF2E86AB;   // drenched
        return 0xFF3AB5C4;                      // damp teal
    }

    private static void drawBar(GuiGraphics graphics, int x, int y, float pct, int color) {
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x88000000);
        int fillWidth = (int) (Mth.clamp(pct, 0f, 1f) * (BAR_WIDTH - 2));
        if (fillWidth > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + BAR_HEIGHT - 1, color);
        }
    }
}
