package net.enderwish.HUD_Visuals_Subpack.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.enderwish.HUD_Visuals_Subpack.core.WristCapability;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * SPORTS WATCH HUD
 * Updated: Moved Paper Doll to the bottom right of the screen.
 * UV = Texture Source | XY = Cut Dimensions
 */
public class SportsWatchHUD {

    private static final ResourceLocation LIMBS_TEXTURE = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/gui/limbs.png");

    // The dimensions of the area the paper doll occupies
    private static final int WATCH_FACE_WIDTH = 100;
    private static final int WATCH_FACE_HEIGHT = 80;
    private static final int MARGIN = 10;
    private static final float SCALE = 2.0F;

    public static final LayeredDraw.Layer SPORTS_WATCH_ELEMENT = (graphics, deltaTracker) -> {
        render(graphics, deltaTracker);
    };

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || player.isSpectator()) return;

        WristCapability cap = player.getData(ModAttachments.WRIST_CAP);
        if (cap == null || !cap.hasWatchEquipped()) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // 1. Render status bars (kept near center/bottom as per previous logic)
        renderStatusBars(graphics, mc, cap, player, sw, sh);

        // 2. Position for Bottom Right
        // We subtract the scaled width/height and a margin from the total screen width/height
        int watchX = sw - (int)(WATCH_FACE_WIDTH * SCALE) - MARGIN;
        int watchY = sh - (int)(WATCH_FACE_HEIGHT * SCALE) - MARGIN;

        graphics.pose().pushPose();
        graphics.pose().translate(watchX, watchY, 0);
        graphics.pose().scale(SCALE, SCALE, 1.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // --- DRAW LIMBS ---
        // Format: drawLimb(graphics, screenX, screenY, texU, texV, width, height, healthPct)

        // Head: UV 1,1 | XY 17,17
        drawLimb(graphics, 42, 5, 1, 1, 17, 17, cap.getHeadPct());

        // Torso: UV 40,1 | XY 17,25
        drawLimb(graphics, 42, 22, 40, 1, 17, 25, cap.getTorsoPct());

        // Left Arm: UV 80,1 | XY 9,25
        drawLimb(graphics, 32, 22, 80, 1, 9, 25, cap.getLArmPct());

        // Right Arm: UV 1,40 | XY 9,25
        drawLimb(graphics, 59, 22, 1, 40, 9, 25, cap.getRArmPct());

        // Left Leg: UV 40,40 | XY 9,21
        drawLimb(graphics, 42, 47, 40, 40, 9, 21, cap.getLLegPct());

        // Right Leg: UV 80,40 | XY 9,21
        drawLimb(graphics, 51, 47, 80, 40, 9, 21, cap.getRLegPct());

        // Left Foot: UV 1,80 | XY 11,5
        drawLimb(graphics, 40, 68, 1, 80, 11, 5, cap.getLFootPct());

        // Right Foot: UV 40,80 | XY 11,5
        drawLimb(graphics, 51, 68, 40, 80, 11, 5, cap.getRFootPct());

        // Draw BPM Text below the doll
        graphics.drawString(mc.font, cap.getBPM() + " BPM", 5, 5, 0xFFFFFF, true);

        graphics.pose().popPose();

        // Safety reset
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderStatusBars(GuiGraphics graphics, Minecraft mc, WristCapability cap, Player player, int sw, int sh) {
        int leftX = sw / 2 - 91;
        int rightX = sw / 2 + 10;
        int row1Y = sh - 39;
        int row2Y = sh - 51;

        drawStatusBar(graphics, mc, leftX, row1Y, cap.getEnergy(), 0xFF00AAFF, "ENERGY");
        float foodLevel = (player.getFoodData().getFoodLevel() / 20.0f) * 100.0f;
        drawStatusBar(graphics, mc, rightX, row1Y, foodLevel, 0xFFFF9900, "HUNGER");
        drawStatusBar(graphics, mc, rightX, row2Y, 100f, 0xFF00FFFF, "THIRST");

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawStatusBar(GuiGraphics graphics, Minecraft mc, int x, int y, float percent, int color, String label) {
        int barWidth = 81;
        int barHeight = 7;
        graphics.fill(x, y, x + barWidth, y + barHeight, 0x44000000);
        int fillWidth = (int) ((percent / 100.0f) * (barWidth - 2));
        if (fillWidth > 0) {
            graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + barHeight - 1, color);
        }
        graphics.pose().pushPose();
        graphics.pose().scale(0.5f, 0.5f, 0.5f);
        graphics.drawString(mc.font, label, (x) * 2, (y - 6) * 2, color, true);
        graphics.pose().popPose();
    }

    private static void drawLimb(GuiGraphics graphics, int x, int y, int u, int v, int width, int height, float pct) {
        if (pct >= 0.75f) RenderSystem.setShaderColor(0.2F, 1.0F, 0.2F, 1.0F);
        else if (pct >= 0.4f) RenderSystem.setShaderColor(1.0F, 1.0F, 0.0F, 1.0F);
        else RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 1.0F);

        graphics.blit(LIMBS_TEXTURE, x, y, u, v, width, height, 128, 128);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}