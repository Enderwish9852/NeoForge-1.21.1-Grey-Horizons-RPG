package net.enderwish.HUD_Visuals_Subpack.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.api.ClimateData;
import net.enderwish.HUD_Visuals_Subpack.api.ClimateHooks;
import net.enderwish.HUD_Visuals_Subpack.client.ClientClimateCache;
import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.enderwish.HUD_Visuals_Subpack.core.PlayerCapability;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

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

        PlayerCapability cap = player.getData(ModAttachments.PLAYER_CAP);
        if (cap == null) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // 1. Render Starvation Timer
        renderStarvationTimer(graphics, mc, cap, sw, sh);

        // 2. Render Watch Elements
        if (cap.hasWatchEquipped()) {
            renderStatusBars(graphics, mc, cap, player, sw, sh);
            renderLimbDisplay(graphics, cap, sw, sh);
            renderSeasonInfo(graphics, mc, sw, sh);
        }
    }

    private static void renderSeasonInfo(GuiGraphics graphics, Minecraft mc, int sw, int sh) {
        ClimateData data = ClientClimateCache.get();
        if (data == null || mc.level == null) return;

        // FIX: Use the degree-based Hook we built for the Alpha Test
        float displayCelsius = ClimateHooks.getTemperatureInDegrees(mc.level, mc.player.blockPosition());
        boolean isFreezing = ClimateHooks.isColdToFreeze(mc.level);

        // Format Weather String (e.g., "gh_hud_visuals:blizzard" -> "BLIZZARD")
        String weatherLabel = data.weather().contains(":") ?
                data.weather().split(":")[1].toUpperCase() : data.weather().toUpperCase();

        int tempColor = isFreezing ? 0xFF55FFFF : 0xFFFFAA00;

        // Display: SEASON | WEATHER | 00.0°C | 00 BPM
        String displayLine = String.format("%s | %s | %.1f°C | %d BPM",
                data.season().getSerializedName().toUpperCase(),
                weatherLabel,
                displayCelsius,
                (int)mc.player.getData(ModAttachments.PLAYER_CAP).getBPM());

        graphics.pose().pushPose();
        graphics.pose().scale(0.8f, 0.8f, 0.8f);

        int scaledX = (int) ((sw / 2) / 0.8f);
        int scaledY = (int) ((sh - 62) / 0.8f);

        graphics.drawCenteredString(mc.font, displayLine, scaledX, scaledY, tempColor);
        graphics.pose().popPose();
    }

    private static void renderStarvationTimer(GuiGraphics graphics, Minecraft mc, PlayerCapability cap, int sw, int sh) {
        if (cap.getStarvationTimer() > 0) {
            int totalSecondsLeft = Math.max(0, (12000 - cap.getStarvationTimer()) / 20);
            int mins = totalSecondsLeft / 60;
            int secs = totalSecondsLeft % 60;
            String timeStr = String.format("%02d:%02d", mins, secs);

            int timerColor = (cap.getStarvationTimer() <= 2400) ? 0xFF00FF00 : 0xFFFF0000;
            graphics.drawCenteredString(mc.font, "STARVATION: " + timeStr, sw / 2, sh - 75, timerColor);
        }
    }

    private static void renderStatusBars(GuiGraphics graphics, Minecraft mc, PlayerCapability cap, Player player, int sw, int sh) {
        int leftX = sw / 2 - 91;
        int rightX = sw / 2 + 10;
        int row1Y = sh - 39;
        int row2Y = sh - 49;

        drawStatusBar(graphics, mc, leftX, row1Y, cap.getEnergy(), 0xFF00AAFF, "ENERGY");
        float hungerPct = (player.getFoodData().getFoodLevel() / 20.0f) * 100.0f;
        drawStatusBar(graphics, mc, rightX, row1Y, hungerPct, 0xFFFF9900, "HUNGER");
        drawStatusBar(graphics, mc, rightX, row2Y, cap.getThirst(), 0xFF00FFFF, "THIRST");
    }

    private static void renderLimbDisplay(GuiGraphics graphics, PlayerCapability cap, int sw, int sh) {
        int watchX = sw - (int)(WATCH_FACE_WIDTH * SCALE) - MARGIN;
        int watchY = sh - (int)(WATCH_FACE_HEIGHT * SCALE) - MARGIN;

        graphics.pose().pushPose();
        graphics.pose().translate(watchX, watchY, 0);
        graphics.pose().scale(SCALE, SCALE, 1.0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        drawLimb(graphics, 23, 22, 80, 1, 9, 25, cap.getLArmPct());
        drawLimb(graphics, 45, 22, 1, 40, 9, 25, cap.getRArmPct());
        drawLimb(graphics, 30, 47, 40, 40, 9, 21, cap.getLLegPct());
        drawLimb(graphics, 38, 47, 80, 40, 9, 21, cap.getRLegPct());
        drawLimb(graphics, 28, 68, 1, 80, 11, 5, cap.getLFootPct());
        drawLimb(graphics, 38, 68, 41, 80, 11, 5, cap.getRFootPct());
        drawLimb(graphics, 30, 5, 1, 1, 17, 17, cap.getHeadPct());
        drawLimb(graphics, 30, 22, 40, 1, 17, 25, cap.getTorsoPct());

        graphics.pose().popPose();
    }

    private static void drawStatusBar(GuiGraphics graphics, Minecraft mc, int x, int y, float percent, int color, String label) {
        int barWidth = 81;
        int barHeight = 7;
        graphics.fill(x, y, x + barWidth, y + barHeight, 0x44000000);

        int fillWidth = (int) (Mth.clamp(percent / 100.0f, 0, 1) * (barWidth - 2));
        if (fillWidth > 0) graphics.fill(x + 1, y + 1, x + 1 + fillWidth, y + barHeight - 1, color);

        graphics.pose().pushPose();
        graphics.pose().scale(0.5f, 0.5f, 0.5f);
        graphics.drawString(mc.font, label, (x) * 2, (y - 6) * 2, color, true);
        graphics.pose().popPose();
    }

    private static void drawLimb(GuiGraphics graphics, int x, int y, int u, int v, int width, int height, float pct) {
        if (pct <= 0.0f) RenderSystem.setShaderColor(0.1F, 0.1F, 0.1F, 1.0F);
        else if (pct >= 0.75f) RenderSystem.setShaderColor(0.2F, 1.0F, 0.2F, 1.0F);
        else if (pct >= 0.4f) RenderSystem.setShaderColor(1.0F, 1.0F, 0.0F, 1.0F);
        else RenderSystem.setShaderColor(1.0F, 0.0F, 0.0F, 1.0F);

        graphics.blit(LIMBS_TEXTURE, x, y, u, v, width, height, 128, 128);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}