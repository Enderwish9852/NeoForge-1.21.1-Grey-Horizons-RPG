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
 * Fixes:
 * 1. Flashing/Blinking by using consistent Shader state and disabling depth testing.
 * 2. Proper limb alignment based on a central torso.
 */
public class SportsWatchHUD {

    private static final ResourceLocation LIMBS_TEXTURE = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/gui/limbs.png");

    private static final int BASE_WIDTH = 100;
    private static final int BASE_HEIGHT = 80;
    private static final int MARGIN = 10;

    public static final LayeredDraw.Layer SPORTS_WATCH_ELEMENT = (graphics, deltaTracker) -> {
        render(graphics, deltaTracker);
    };

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || player.isSpectator()) return;

        WristCapability cap = player.getData(ModAttachments.WRIST_CAP);

        // Ensure the HUD only renders if the watch is actually "equipped"
        // Note: If this is true while in hotbar, check WatchToggleHandler.java
        if (cap == null || !cap.hasWatchEquipped()) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // 1. STATUS BARS
        renderStatusBars(graphics, mc, cap, player, screenWidth, screenHeight);

        // 2. WATCH FACE (Bottom Right)
        int watchX = screenWidth - (BASE_WIDTH * 2) - MARGIN;
        int watchY = screenHeight - (BASE_HEIGHT * 2) - MARGIN;

        // Save state
        graphics.pose().pushPose();
        graphics.pose().translate(watchX, watchY, 0);
        graphics.pose().scale(2.0F, 2.0F, 1.0F);

        // STABILITY FIX: Set shader and blending explicitly to stop flashing
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest(); // Stops flickering with world/blocks

        // --- CORRECTED LIMB POSITIONS ---
        // Head
        drawLimb(graphics, 44, 5, 8, 8, 12, 12, cap.getHeadPct());

        // Torso (Center anchor)
        drawLimb(graphics, 40, 18, 48, 12, 20, 26, cap.getTorsoPct());

        // Arms (Symmetrical to Torso)
        drawLimb(graphics, 27, 18, 84, 12, 12, 24, cap.getLArmPct()); // Left
        drawLimb(graphics, 61, 18, 4, 52, 12, 24, cap.getRArmPct());  // Right

        // Legs (Symmetrical under Torso)
        drawLimb(graphics, 40, 45, 44, 50, 10, 22, cap.getLLegPct()); // Left
        drawLimb(graphics, 51, 45, 84, 50, 10, 22, cap.getRLegPct()); // Right

        // Feet
        drawLimb(graphics, 39, 67, 5, 82, 11, 7, cap.getLFootPct());   // Left
        drawLimb(graphics, 51, 67, 45, 82, 11, 7, cap.getRFootPct());  // Right

        // Heart Rate
        graphics.drawString(mc.font, cap.getBPM() + " BPM", 5, 5, 0xFFFFFF, true);

        RenderSystem.enableDepthTest();
        graphics.pose().popPose();

        // Global color reset
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
        if (pct >= 0.75f) {
            RenderSystem.setShaderColor(0.2F, 1.0F, 0.2F, 1.0F);
        } else if (pct >= 0.4f) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 0.0F, 1.0F);
        } else {
            RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 1.0F);
        }

        graphics.blit(LIMBS_TEXTURE, x, y, u, v, width, height, 128, 128);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}