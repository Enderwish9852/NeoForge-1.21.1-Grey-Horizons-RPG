package net.enderwish.Belliarium_Monstrarium_Subpack.client.gui;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.BodyHealthCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.BodyPartColors;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.entity.player.Player;

/**
 * BodyHealthHUD
 *
 * BUGFIX (creative didn't actually heal) -- the display-only "force green
 * in creative" override is gone. BodyDamageHandler.onGameModeChange now
 * genuinely heals the real capability on entering creative, so reading
 * cap.getXPct() directly is already correct -- no cosmetic stand-in needed.
 */
public class BodyHealthHUD {

    public static final LayeredDraw.Layer LAYER = BodyHealthHUD::render;

    private static final int BASE_X = 10;
    private static final int BASE_Y_FROM_BOTTOM = 90;

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) return;
        if (!net.enderwish.Belliarium_Monstrarium_Subpack.core.CuriosHooks.hasWatchEquipped(player)) return;

        BodyHealthCapability cap = player.getData(ModAttachments.BODY_HEALTH);
        int baseY = mc.getWindow().getGuiScaledHeight() - BASE_Y_FROM_BOTTOM;

        drawPart(graphics, BASE_X + 12, baseY, 10, 10, cap.getHeadPct());
        drawPart(graphics, BASE_X + 8, baseY + 11, 18, 22, cap.getTorsoPct());
        drawPart(graphics, BASE_X, baseY + 11, 7, 20, cap.getLeftArmPct());
        drawPart(graphics, BASE_X + 27, baseY + 11, 7, 20, cap.getRightArmPct());
        drawPart(graphics, BASE_X + 8, baseY + 34, 8, 18, cap.getLeftLegPct());
        drawPart(graphics, BASE_X + 17, baseY + 34, 8, 18, cap.getRightLegPct());
        drawPart(graphics, BASE_X + 8, baseY + 53, 8, 6, cap.getLeftFootPct());
        drawPart(graphics, BASE_X + 17, baseY + 53, 8, 6, cap.getRightFootPct());
    }

    private static void drawPart(GuiGraphics graphics, int x, int y, int w, int h, float pct) {
        graphics.fill(x, y, x + w, y + h, 0xAA000000);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, BodyPartColors.colorForPct(pct));
    }
}
