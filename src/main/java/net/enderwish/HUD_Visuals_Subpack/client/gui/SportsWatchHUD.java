package net.enderwish.HUD_Visuals_Subpack.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.client.ClientSeasonHandler;
import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.enderwish.HUD_Visuals_Subpack.core.WristCapability;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * SPORTS WATCH HUD
 * Uses internal ClientSeasonHandler to display the custom season system.
 */
public class SportsWatchHUD {

    private static final ResourceLocation LIMBS_TEXTURE = ResourceLocation.fromNamespaceAndPath(HUDVisualsSubpack.MOD_ID, "textures/gui/limbs.png");

    private static final int WATCH_FACE_WIDTH = 80;
    private static final int WATCH_FACE_HEIGHT = 85;
    private static final int MARGIN = 10;
    private static final float SCALE = 1.5F;

    public static final LayeredDraw.Layer SPORTS_WATCH_ELEMENT = (graphics, deltaTracker) -> {
        render(graphics, deltaTracker);
    };

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        if (player == null || player.isSpectator() || !player.isAlive()) return;

        player.deathTime = 0;
        player.hurtTime = 0;

        WristCapability cap = player.getData(ModAttachments.WRIST_CAP);
        if (cap == null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        renderStarvationTimer(graphics, mc, cap, sw, sh);

        if (cap.hasWatchEquipped()) {
            renderStatusBars(graphics, mc, cap, player, sw, sh);
            renderLimbDisplay(graphics, cap, sw, sh);
            renderSeasonInfo(graphics, mc, sw, sh);
        }
    }

    private static void renderSeasonInfo(GuiGraphics graphics, Minecraft mc, int sw, int sh) {
        // Using your project's ClientSeasonHandler (from image_e57d84.jpg)
        Season currentSeason = ClientSeasonHandler.getClientSeason();
        int currentDay = ClientSeasonHandler.getClientDay();

        if (currentSeason == null) return;

        // Calculate Temperature
        float currentTemp = getCalculatedTemperature(mc);
        String tempText = String.format("%.1f°C", currentTemp);

        // Create display text: e.g., "SPRING - DAY 5 | 22.5°C"
        String seasonText = currentSeason.name() + " - DAY " + currentDay + " | " + tempText;
        int seasonColor = getSeasonColor(currentSeason);

        graphics.pose().pushPose();
        graphics.pose().scale(0.8f, 0.8f, 0.8f);

        // Positioned slightly above the status bars
        int scaledX = (int) ((sw / 2) / 0.8f);
        int scaledY = (int) ((sh - 62) / 0.8f);

        graphics.drawCenteredString(mc.font, seasonText, scaledX, scaledY, seasonColor);
        graphics.pose().popPose();
    }

    private static void renderStarvationTimer(GuiGraphics graphics, Minecraft mc, WristCapability cap, int sw, int sh) {
        if (cap.getStarvationTimer() > 0) {
            int totalSecondsLeft = (12000 - cap.getStarvationTimer()) / 20;
            int mins = totalSecondsLeft / 60;
            int secs = totalSecondsLeft % 60;
            String timeStr = String.format("%02d:%02d", mins, secs);
            int timerColor = (cap.getStarvationTimer() <= 2400) ? 0xFF00FF00 : 0xFFFF0000;
            graphics.drawCenteredString(mc.font, "STARVATION: " + timeStr, sw / 2, sh - 65, timerColor);
        }
    }

    private static void renderStatusBars(GuiGraphics graphics, Minecraft mc, WristCapability cap, Player player, int sw, int sh) {
        int leftX = sw / 2 - 91;
        int rightX = sw / 2 + 10;
        int row1Y = sh - 39;
        int row2Y = sh - 51;

        drawStatusBar(graphics, mc, leftX, row1Y, cap.getEnergy(), 0xFF00AAFF, "ENERGY");
        float hungerPct = (player.getFoodData().getFoodLevel() / 20.0f) * 100.0f;
        drawStatusBar(graphics, mc, rightX, row1Y, hungerPct, 0xFFFF9900, "HUNGER");
        drawStatusBar(graphics, mc, rightX, row2Y, cap.getThirst(), 0xFF00FFFF, "THIRST");
    }

    private static void renderLimbDisplay(GuiGraphics graphics, WristCapability cap, int sw, int sh) {
        int watchX = sw - (int)(WATCH_FACE_WIDTH * SCALE) - MARGIN;
        int watchY = sh - (int)(WATCH_FACE_HEIGHT * SCALE) - MARGIN;

        graphics.pose().pushPose();
        graphics.pose().translate(watchX, watchY, 0);
        graphics.pose().scale(SCALE, SCALE, 1.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, -0.01f);
        drawLimb(graphics, 23, 22, 80, 1, 9, 25, cap.getLArmPct());
        drawLimb(graphics, 45, 22, 1, 40, 9, 25, cap.getRArmPct());
        drawLimb(graphics, 30, 47, 40, 40, 9, 21, cap.getLLegPct());
        drawLimb(graphics, 38, 47, 80, 40, 9, 21, cap.getRLegPct());
        drawLimb(graphics, 28, 68, 1, 80, 11, 5, cap.getLFootPct());
        drawLimb(graphics, 38, 68, 41, 80, 11, 5, cap.getRFootPct());
        graphics.pose().popPose();

        drawLimb(graphics, 30, 5, 1, 1, 17, 17, cap.getHeadPct());
        drawLimb(graphics, 30, 22, 40, 1, 17, 25, cap.getTorsoPct());

        graphics.pose().popPose();
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
        if (pct <= 0.0f) {
            RenderSystem.setShaderColor(0.1F, 0.1F, 0.1F, 1.0F);
        } else if (pct >= 0.75f) {
            RenderSystem.setShaderColor(0.2F, 1.0F, 0.2F, 1.0F);
        } else if (pct >= 0.4f) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 0.0F, 1.0F);
        } else {
            RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 1.0F);
        }
        graphics.blit(LIMBS_TEXTURE, x, y, u, v, width, height, 128, 128);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static int getSeasonColor(Season season) {
        // Mapping colors to your specific Season enum
        return switch (season) {
            case SPRING -> 0xFF55FF55;
            case SUMMER -> 0xFFFFFF55;
            case AUTUMN -> 0xFFFFAA00;
            case WINTER -> 0xFF55FFFF;
            default -> 0xFFFFFFFF;
        };
    }

    private static float getCalculatedTemperature(Minecraft mc) {
        if (mc.level == null || mc.player == null) return 20.0f;

        BlockPos pos = mc.player.blockPosition();
        Biome biome = mc.level.getBiome(pos).value();

        // Base Minecraft biome temperature (usually 0.0 to 2.0)
        float baseTemp = biome.getBaseTemperature();

        // Convert to Celsius: Base 20C, scaled by Minecraft's internal temp
        float celsius = 20.0f + (baseTemp * 15.0f);

        // Time of day adjustment (Colder at night)
        long time = mc.level.getDayTime() % 24000;
        if (time > 13000 && time < 23000) { // Night time
            celsius -= 10.0f;
        } else if (time > 23000 || time < 1000) { // Dawn
            celsius -= 5.0f;
        }

        // Season adjustment
        Season season = ClientSeasonHandler.getClientSeason();
        if (season != null) {
            celsius += switch (season) {
                case SUMMER -> 10.0f;
                case WINTER -> -15.0f;
                case AUTUMN -> -5.0f;
                default -> 0.0f;
            };
        }

        // Altitude adjustment (Colder as you go up)
        if (pos.getY() > 80) {
            celsius -= (pos.getY() - 80) * 0.1f;
        }

        return celsius;
    }
}