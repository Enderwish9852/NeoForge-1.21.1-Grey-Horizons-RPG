package net.enderwish.Belliarium_Monstrarium_Subpack.client.gui;

import net.enderwish.Belliarium_Monstrarium_Subpack.client.CombatTimerClientState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * CombatTimerHUD
 *
 * "IN COMBAT mm:ss" in red, centered above the hotbar, while a battle is
 * active. On battle end, the end-state message takes its place for
 * END_MESSAGE_HOLD_MS then fades over END_MESSAGE_FADE_MS -- both first-pass
 * numbers, adjust freely.
 */
public class CombatTimerHUD {

    public static final LayeredDraw.Layer LAYER = CombatTimerHUD::render;

    private static final int Y_ABOVE_HOTBAR = 58;
    private static final long END_MESSAGE_HOLD_MS = 2000;
    private static final long END_MESSAGE_FADE_MS = 1000;

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        int centerX = mc.getWindow().getGuiScaledWidth() / 2;
        int y = mc.getWindow().getGuiScaledHeight() - Y_ABOVE_HOTBAR;

        if (CombatTimerClientState.isInCombat()) {
            int seconds = CombatTimerClientState.getElapsedSeconds();
            String text = String.format("IN COMBAT %02d:%02d", seconds / 60, seconds % 60);
            drawCentered(graphics, mc, text, centerX, y, 0xFFFF3333, 1.0f);
            return;
        }

        String endMessage = CombatTimerClientState.getEndMessage();
        if (endMessage == null) return;

        long age = System.currentTimeMillis() - CombatTimerClientState.getEndMessageReceivedAtMillis();
        if (age >= END_MESSAGE_HOLD_MS + END_MESSAGE_FADE_MS) return;

        float alpha = age <= END_MESSAGE_HOLD_MS
                ? 1.0f
                : 1.0f - Mth.clamp((age - END_MESSAGE_HOLD_MS) / (float) END_MESSAGE_FADE_MS, 0f, 1f);

        drawCentered(graphics, mc, endMessage.toUpperCase(), centerX, y, 0xFFFFCC33, alpha);
    }

    private static void drawCentered(GuiGraphics graphics, Minecraft mc, String text,
                                     int centerX, int y, int rgb, float alpha) {
        int a = (int) (alpha * 255) << 24;
        int color = a | (rgb & 0xFFFFFF);
        graphics.drawCenteredString(mc.font, Component.literal(text), centerX, y, color);
    }
}
