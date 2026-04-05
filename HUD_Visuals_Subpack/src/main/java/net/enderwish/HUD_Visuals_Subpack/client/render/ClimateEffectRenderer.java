package net.enderwish.HUD_Visuals_Subpack.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.api.ClimateData;
import net.enderwish.HUD_Visuals_Subpack.client.ClientClimateCache;
import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.enderwish.HUD_Visuals_Subpack.core.PlayerCapability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = HUDVisualsSubpack.MOD_ID, value = Dist.CLIENT)
public class ClimateEffectRenderer {

    private static final ResourceLocation BLIZZARD_TEX = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/environment/blizzard.png");
    private static final ResourceLocation HEATWAVE_TEX = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/environment/heatwave.png");

    // Vanilla overlays for tinting and freezing effects
    private static final ResourceLocation VIGNETTE = ResourceLocation.withDefaultNamespace("textures/misc/vignette.png");
    private static final ResourceLocation FROST_OVERLAY = ResourceLocation.withDefaultNamespace("textures/misc/powder_snow_outline.png");

    @SubscribeEvent
    public static void onRenderGui(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int width = event.getGuiGraphics().guiWidth();
        int height = event.getGuiGraphics().guiHeight();
        GuiGraphics graphics = event.getGuiGraphics();

        // --- PART 1: WEATHER OVERLAYS (Based on World State) ---
        ClimateData data = ClientClimateCache.get();
        if (data != null && data.intensity() > 0.01f) {
            String weatherId = data.weather().toLowerCase();
            float intensity = data.intensity();

            if (weatherId.contains("blizzard")) {
                renderOverlay(graphics, BLIZZARD_TEX, 1.0f, 1.0f, 1.0f, intensity * 0.7f, width, height);
                renderOverlay(graphics, VIGNETTE, 0.4f, 0.7f, 1.0f, intensity * 0.4f, width, height);
            }
            else if (weatherId.contains("heatwave")) {
                renderOverlay(graphics, HEATWAVE_TEX, 1.0f, 0.9f, 0.7f, intensity * 0.4f, width, height);
                renderOverlay(graphics, VIGNETTE, 1.0f, 0.5f, 0.0f, intensity * 0.3f, width, height);
            }
        }

        // --- PART 2: EXPOSURE OVERLAYS (Based on Player Body Temp) ---
        PlayerCapability cap = mc.player.getData(ModAttachments.PLAYER_CAP);
        if (cap != null) {
            float temp = cap.getCoreTemp(); // 0.7 is neutral, < 0.5 is cold, > 1.0 is hot

            // COLD EXPOSURE (Blue/Frost)
            if (temp < 0.6f) {
                float coldAlpha = Math.min(1.0f, (0.6f - temp) * 2.0f);
                // Blue Vignette
                renderOverlay(graphics, VIGNETTE, 0.2f, 0.5f, 1.0f, coldAlpha * 0.6f, width, height);

                // Grade 3: Critical Freeze (Powder Snow effect)
                if (temp < 0.3f) {
                    float frostAlpha = Math.min(1.0f, (0.3f - temp) * 3.0f);
                    renderOverlay(graphics, FROST_OVERLAY, 1.0f, 1.0f, 1.0f, frostAlpha, width, height);
                }
            }
            // HEAT EXPOSURE (Red/Orange)
            else if (temp > 1.2f) {
                float heatAlpha = Math.min(1.0f, (temp - 1.2f) * 1.5f);
                // Orange Vignette
                renderOverlay(graphics, VIGNETTE, 1.0f, 0.3f, 0.0f, heatAlpha * 0.5f, width, height);

                // Grade 3: Boiling (Red-tinted Frost Overlay to simulate heat haze/pain)
                if (temp > 1.8f) {
                    float boilAlpha = Math.min(1.0f, (temp - 1.8f) * 2.0f);
                    renderOverlay(graphics, FROST_OVERLAY, 1.0f, 0.2f, 0.0f, boilAlpha, width, height);
                }
            }
        }
    }

    private static void renderOverlay(GuiGraphics graphics, ResourceLocation texture, float r, float g, float b, float alpha, int w, int h) {
        if (alpha <= 0.001f) return;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        graphics.setColor(r, g, b, alpha);
        graphics.blit(texture, 0, 0, -90, 0.0f, 0.0f, w, h, w, h);

        graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}