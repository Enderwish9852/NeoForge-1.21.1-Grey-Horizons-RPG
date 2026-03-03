package net.enderwish.HUD_Visuals_Subpack.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.enderwish.HUD_Visuals_Subpack.core.WristCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * SPORTS WATCH HUD LOGIC
 * Positioned at the bottom right.
 * No image background - uses a drawn semi-transparent rectangle for readability.
 */
public class SportsWatchHUD {

    private static final ResourceLocation LIMBS_TEXTURE = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/gui/limbs.png");

    // HUD Dimensions
    private static final int HUD_WIDTH = 100;
    private static final int HUD_HEIGHT = 70;
    private static final int MARGIN = 10;

    public static final LayeredDraw.Layer SPORTS_WATCH_ELEMENT = (graphics, delta) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || player.isSpectator()) return;

        WristCapability cap = player.getData(ModAttachments.WRIST_CAP);
        if (cap == null || !cap.hasWatchEquipped()) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Position: Bottom Right
        int x = screenWidth - HUD_WIDTH - MARGIN;
        int y = screenHeight - HUD_HEIGHT - MARGIN;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 1. Draw a "Glass" Background Rectangle
        // Instead of an image, we draw a dark box so we can see our white text/icons.
        // Color is (Black, 40% Transparency)
        graphics.fill(x, y, x + HUD_WIDTH, y + HUD_HEIGHT, 0x66000000);

        // Optional: Draw a thin border
        graphics.renderOutline(x, y, HUD_WIDTH, HUD_HEIGHT, 0xFFFFFFFF);

        // 2. Draw Limb Indicators
        // Coordinates and UVs stay the same.
        drawLimb(graphics, x + 44, y + 8,  0,  0, 12, 12, cap.getHeadPct());      // Head
        drawLimb(graphics, x + 40, y + 21, 12, 0, 20, 24, cap.getTorsoPct());     // Torso
        drawLimb(graphics, x + 28, y + 21, 32, 0, 11, 22, cap.getLArmPct());      // Left Arm
        drawLimb(graphics, x + 61, y + 21, 43, 0, 11, 22, cap.getRArmPct());      // Right Arm
        drawLimb(graphics, x + 40, y + 46, 54, 0, 9,  20, cap.getLLegPct());      // Left Leg
        drawLimb(graphics, x + 51, y + 46, 63, 0, 9,  20, cap.getRLegPct());      // Right Leg

        // Feet
        drawLimb(graphics, x + 38, y + 60, 72, 0, 10, 6, cap.getLFootPct());      // Left Foot
        drawLimb(graphics, x + 52, y + 60, 82, 0, 10, 6, cap.getRFootPct());      // Right Foot

        // 3. Draw Text Stats
        // We add shadows to text to make it stand out even more.
        graphics.drawString(mc.font, cap.getBPM() + " BPM", x + 5, y + 5, 0xFFFFFF, true);
        graphics.drawString(mc.font, (int)cap.getEnergy() + "% NRG", x + 5, y + HUD_HEIGHT - 15, 0x00AAFF, true);

        RenderSystem.disableBlend();
    };

    private static void drawLimb(GuiGraphics graphics, int x, int y, int u, int v, int width, int height, float pct) {
        if (pct > 0.7f) RenderSystem.setShaderColor(0.2F, 1.0F, 0.2F, 1.0F);
        else if (pct > 0.3f) RenderSystem.setShaderColor(1.0F, 1.0F, 0.2F, 1.0F);
        else RenderSystem.setShaderColor(1.0F, 0.2F, 0.2F, 1.0F);

        graphics.blit(LIMBS_TEXTURE, x, y, u, v, width, height, 128, 128);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}