package net.enderwish.HUD_Visuals_Subpack.client;

import net.enderwish.HUD_Visuals_Subpack.core.Season;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

/**
 * Renders the Season and Day information on the player's HUD.
 * Updated for NeoForge 1.21.1 method signatures.
 */
public class SeasonHUDOverlay implements LayeredDraw.Layer {

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();

        // Don't render if the HUD is hidden (F1 mode) or player is null
        if (mc.options.hideGui || mc.player == null) {
            return;
        }

        Season currentSeason = ClientSeasonHandler.getClientSeason();
        int currentDay = ClientSeasonHandler.getClientDay();

        // Create the display text
        String text = currentSeason.name().charAt(0) + currentSeason.name().substring(1).toLowerCase() + " - Day " + currentDay;

        int color = getSeasonColor(currentSeason);

        // Render at top left
        guiGraphics.drawString(mc.font, Component.literal(text), 10, 10, color);
    }

    private int getSeasonColor(Season season) {
        return switch (season) {
            case SPRING -> 0x55FF55; // Green
            case SUMMER -> 0xFFFF55; // Yellow
            case AUTUMN -> 0xFFAA00; // Orange
            case WINTER -> 0x55FFFF; // Aqua
        };
    }
}