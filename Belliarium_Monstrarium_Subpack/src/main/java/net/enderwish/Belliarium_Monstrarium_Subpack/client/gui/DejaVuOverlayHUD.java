package net.enderwish.Belliarium_Monstrarium_Subpack.client.gui;

import net.enderwish.Belliarium_Monstrarium_Subpack.client.DejaVuClientState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;

/**
 * DejaVuOverlayHUD
 *
 * The teleport glitch cue: a red/violet vignette flash timed around the
 * "bad dream" jump, to sell "sleepwalking, can't remember how I got here."
 * (The ghost-loot visual itself is drawn in-slot, inside the actual
 * container screen, by DejaVuGhostSlotHandler -- not here.)
 */
public class DejaVuOverlayHUD {

    public static final LayeredDraw.Layer LAYER = DejaVuOverlayHUD::render;

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (DejaVuClientState.isTeleportGlitchActive()) renderTeleportGlitch(graphics, mc);
    }

    private static void renderTeleportGlitch(GuiGraphics graphics, Minecraft mc) {
        long age = DejaVuClientState.teleportGlitchAgeMs();
        long total = DejaVuClientState.TELEPORT_GLITCH_MS;
        float t = Mth.clamp(age / (float) total, 0f, 1f);
        float alpha = t < 0.5f ? (t / 0.5f) : (1f - (t - 0.5f) / 0.5f);
        int a = (int) (alpha * 160) << 24;
        int color = a | 0x5A1E3C;
        graphics.fill(0, 0, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), color);
    }
}
