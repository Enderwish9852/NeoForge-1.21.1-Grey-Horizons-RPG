package net.enderwish.Belliarium_Monstrarium_Subpack.client.gui;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.integration.CuriosHooks;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.survival.SurvivalCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.event.WeightEnforcementHandler;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
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

    private static final int BAR_WIDTH = 81;
    private static final int BAR_HEIGHT = 5;
    private static final int ROW_GAP = 7;

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) return;
        if (!CuriosHooks.hasWatchEquipped(player)) return;

        boolean creative = player.isCreative();
        SurvivalCapability survival = player.getData(ModAttachments.SURVIVAL);

        int x = mc.getWindow().getGuiScaledWidth() / 2 - 91;
        int baseY = mc.getWindow().getGuiScaledHeight() - 39;

        float staminaPct = creative ? 1.0f : survival.getStamina() / 100f;
        float energyPct = creative ? 1.0f : survival.getEnergy() / 100f;

        drawBar(graphics, x, baseY - ROW_GAP * 2, staminaPct, 0xFF3399FF);
        drawBar(graphics, x, baseY - ROW_GAP, energyPct, 0xFFFFAA33);

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

    private static void drawBar(GuiGraphics graphics, int x, int y, float pct, int color) {
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, 0x88000000);
        int fillWidth = (int) (Mth.clamp(pct, 0f, 1f) * (BAR_WIDTH - 2));
        if (fillWidth > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + BAR_HEIGHT - 1, color);
        }
    }
}
