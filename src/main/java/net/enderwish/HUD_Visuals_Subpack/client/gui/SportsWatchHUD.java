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
 * Handles the rendering logic for the Sports Watch HUD.
 */
public class SportsWatchHUD {

    private static final ResourceLocation HUD_TEXTURE = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/gui/sports_watch_hud.png");
    private static final ResourceLocation LIMBS_TEXTURE = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/gui/limbs.png");

    public static final LayeredDraw.Layer SPORTS_WATCH_ELEMENT = (guiGraphics, partialTick) -> {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }

        if (isWatchEquipped(mc.player)) {
            drawWatchHUD(guiGraphics, mc);
        }
    };

    private static boolean isWatchEquipped(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.SPORTS_WATCH.get()).isPresent();
    }

    private static void drawWatchHUD(GuiGraphics graphics, Minecraft mc) {
        Player player = mc.player;
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Position: Bottom Left
        int x = 10;
        int y = screenHeight - 80;

        // Safety check for Capability data
        if (!player.hasData(ModAttachments.WRIST_CAP)) return;
        WristCapability cap = player.getData(ModAttachments.WRIST_CAP);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        // Render the main HUD background (reset color to white first)
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(HUD_TEXTURE, x, y, 0, 0, 100, 70, 100, 70);

        // Render Limbs with Color Tinting

        renderLimb(graphics, x + 44, y + 8, 0, 0, 12, 12, cap.getHeadHealth());       // Head
        renderLimb(graphics, x + 40, y + 21, 12, 0, 20, 24, cap.getTorsoHealth());    // Torso
        renderLimb(graphics, x + 28, y + 21, 32, 0, 11, 22, cap.getLeftArmHealth());  // Left Arm
        renderLimb(graphics, x + 61, y + 21, 43, 0, 11, 22, cap.getRightArmHealth()); // Right Arm
        renderLimb(graphics, x + 40, y + 46, 54, 0, 9, 20, cap.getLeftLegHealth());   // Left Leg
        renderLimb(graphics, x + 51, y + 46, 63, 0, 9, 20, cap.getRightLegHealth());  // Right Leg

        // Reset color to white for text and other UI elements
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Draw Numerical Health Text
        String healthText = String.format("HP: %.0f/%.0f", player.getHealth(), player.getMaxHealth());
        graphics.drawString(mc.font, healthText, x + 25, y + 2, 0xFFFFFF, true);

        RenderSystem.disableBlend();
    }

    private static void renderLimb(GuiGraphics graphics, int x, int y, int u, int v, int w, int h, float healthPct) {
        float health = Math.max(0f, Math.min(1f, healthPct));

        float r, g, b = 0.0f;
        if (health > 0.5f) {
            // Transition from Yellow (1,1,0) to Green (0,1,0)
            r = (1f - health) * 2f;
            g = 1.0f;
        } else {
            // Transition from Red (1,0,0) to Yellow (1,1,0)
            r = 1.0f;
            g = health * 2f;
        }

        // Apply the calculated color to the shader
        RenderSystem.setShaderColor(r, g, b, 1.0F);

        // Render the texture part
        graphics.blit(LIMBS_TEXTURE, x, y, u, v, w, h, 128, 128);
    }
}