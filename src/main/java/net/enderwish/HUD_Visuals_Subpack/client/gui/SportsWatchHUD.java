package net.enderwish.HUD_Visuals_Subpack.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.enderwish.HUD_Visuals_Subpack.core.WristCapability;
import net.enderwish.HUD_Visuals_Subpack.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * SPORTS WATCH HUD LOGIC
 * Positioned at the bottom right of the screen.
 */
public class SportsWatchHUD {

    private static final ResourceLocation HUD_TEXTURE = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/gui/sports_watch_hud.png");
    private static final ResourceLocation LIMBS_TEXTURE = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/gui/limbs.png");

    // HUD Dimensions for positioning logic
    private static final int HUD_WIDTH = 100;
    private static final int HUD_HEIGHT = 70;
    private static final int MARGIN = 10;

    public static final LayeredDraw.Layer SPORTS_WATCH_ELEMENT = (guiGraphics, partialTick) -> {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        if (CuriosApi.getCuriosHelper().findFirstCurio(mc.player, ModItems.SPORTS_WATCH.get()).isPresent()) {
            renderPaperDoll(guiGraphics, mc);
        }
    };

    private static void renderPaperDoll(GuiGraphics graphics, Minecraft mc) {
        Player player = mc.player;
        if (!player.hasData(ModAttachments.WRIST_CAP)) return;

        WristCapability cap = player.getData(ModAttachments.WRIST_CAP);

        // DYNAMIC POSITIONING: Calculate bottom-right corner
        int x = graphics.guiWidth() - HUD_WIDTH - MARGIN;
        int y = graphics.guiHeight() - HUD_HEIGHT - MARGIN;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // 1. Draw Background
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(HUD_TEXTURE, x, y, 0, 0, HUD_WIDTH, HUD_HEIGHT, 100, 70);

        // 2. Draw Limbs relative to the new X and Y
        // Head
        drawLimb(graphics, x + 44, y + 8, 0, 0, 12, 12, cap.getHeadHealth());

        // Torso
        drawLimb(graphics, x + 40, y + 21, 12, 0, 20, 24, cap.getTorsoHealth());

        // Left Arm
        drawLimb(graphics, x + 28, y + 21, 32, 0, 11, 22, cap.getLeftArmHealth());

        // Right Arm
        drawLimb(graphics, x + 61, y + 21, 43, 0, 11, 22, cap.getRightArmHealth());

        // Left Leg
        drawLimb(graphics, x + 40, y + 46, 54, 0, 9, 20, cap.getLeftLegHealth());

        // Right Leg
        drawLimb(graphics, x + 51, y + 46, 63, 0, 9, 20, cap.getRightLegHealth());

        // 3. Cleanup
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void drawLimb(GuiGraphics graphics, int x, int y, int u, int v, int w, int h, float health) {
        float hClamped = Math.max(0f, Math.min(1f, health));

        float red = hClamped > 0.5f ? (1.0f - hClamped) * 2.0f : 1.0f;
        float green = hClamped > 0.5f ? 1.0f : hClamped * 2.0f;
        float blue = 0.0f;

        RenderSystem.setShaderColor(red, green, blue, 1.0F);
        graphics.blit(LIMBS_TEXTURE, x, y, (float)u, (float)v, w, h, 128, 128);
    }
}