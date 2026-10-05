package net.enderwish.Belliarium_Monstrarium_Subpack.client.gui;

import net.enderwish.Belliarium_Monstrarium_Subpack.client.CombatLogClientState;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.CombatLogRowType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * CombatLogHUD
 *
 * BUGFIX (scoreboard sometimes not cleared) -- the old per-frame
 * true/false edge detection (wasInCombatLastFrame) is removed entirely.
 * Clearing now happens deterministically inside
 * CombatTimerClientState.update() the instant battleId changes -- see
 * that class.
 */
public class CombatLogHUD {

    public static final LayeredDraw.Layer LAYER = CombatLogHUD::render;

    private static final float BOX_LEFT_FRACTION = 0.67f;
    private static final float BOX_WIDTH_FRACTION = 0.18f;
    private static final float BOX_TOP_FRACTION = 0.06f;
    private static final float BOX_BOTTOM_FRACTION = 0.80f;
    private static final float KILL_FEED_HEIGHT_FRACTION = 0.25f;

    private static final long KILL_FEED_LINGER_MS = 4000;
    private static final long KILL_FEED_FADE_MS = 1000;
    private static final int KILL_FEED_MAX_ROWS = 6;
    private static final int ROW_HEIGHT = 11;

    private static final long ENTER_BATTLE_LINGER_MS = 3000;
    private static final long ENTER_BATTLE_FADE_MS = 1000;
    private static final long COMBO_BREAK_FADE_MS = 500;
    private static final float COUNT_UP_RATE = 40f;

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;

        CombatLogClientState.pruneKillFeed(KILL_FEED_LINGER_MS, KILL_FEED_FADE_MS);

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int boxX = (int) (screenW * BOX_LEFT_FRACTION);
        int boxTop = (int) (screenH * BOX_TOP_FRACTION);
        int boxBottom = (int) (screenH * BOX_BOTTOM_FRACTION);

        int killFeedHeight = (int) ((boxBottom - boxTop) * KILL_FEED_HEIGHT_FRACTION);
        int killFeedBottom = boxTop + killFeedHeight;

        renderKillFeed(graphics, mc, boxX, boxTop, killFeedHeight);
        renderExpLog(graphics, mc, boxX, killFeedBottom, boxBottom - killFeedBottom);
    }

    private static void renderKillFeed(GuiGraphics graphics, Minecraft mc, int x, int top, int height) {
        List<CombatLogClientState.KillFeedRow> rows = CombatLogClientState.getKillFeedRows();
        int shown = 0;
        for (int i = rows.size() - 1; i >= 0 && shown < KILL_FEED_MAX_ROWS; i--, shown++) {
            CombatLogClientState.KillFeedRow row = rows.get(i);
            long age = System.currentTimeMillis() - row.spawnedAtMillis();
            float alpha = age <= KILL_FEED_LINGER_MS
                    ? 1.0f
                    : 1.0f - Mth.clamp((age - KILL_FEED_LINGER_MS) / (float) KILL_FEED_FADE_MS, 0f, 1f);
            if (alpha <= 0f) continue;

            int y = top + shown * ROW_HEIGHT;
            if (y + ROW_HEIGHT > top + height) break;

            drawWeaponIconPlaceholder(graphics, x, y, alpha);
            String text = row.killerName() + "  >  " + row.targetName();
            drawFaded(graphics, mc, text, x + 12, y, 0xFFFFFFFF, alpha);
        }
    }

    private static void renderExpLog(GuiGraphics graphics, Minecraft mc, int x, int top, int height) {
        int rowIndex = 0;
        for (CombatLogRowType type : CombatLogRowType.values()) {
            CombatLogClientState.ExpRowState row = CombatLogClientState.getRow(type);
            if (!row.visible) continue;

            float alpha = computeAlpha(row);
            if (alpha <= 0f) {
                row.visible = false;
                continue;
            }

            row.displayedValue = approach(row.displayedValue, row.targetValue, COUNT_UP_RATE);

            int y = top + rowIndex * ROW_HEIGHT;
            if (y + ROW_HEIGHT > top + height) break;

            String text = row.label + "  +" + Math.round(row.displayedValue) + "xp";
            drawFaded(graphics, mc, text, x, y, 0xFFFFCC33, alpha);
            rowIndex++;
        }
    }

    private static float computeAlpha(CombatLogClientState.ExpRowState row) {
        long now = System.currentTimeMillis();
        if (row.fadingOut) {
            long fadeAge = now - row.fadeStartMillis;
            return 1.0f - Mth.clamp(fadeAge / (float) COMBO_BREAK_FADE_MS, 0f, 1f);
        }
        if (row.fadesOnItsOwn) {
            long age = now - row.shownAtMillis;
            if (age <= ENTER_BATTLE_LINGER_MS) return 1.0f;
            return 1.0f - Mth.clamp((age - ENTER_BATTLE_LINGER_MS) / (float) ENTER_BATTLE_FADE_MS, 0f, 1f);
        }
        return 1.0f;
    }

    private static float approach(float current, float target, float rate) {
        if (current < target) return Math.min(target, current + rate);
        if (current > target) return Math.max(target, current - rate);
        return current;
    }

    private static void drawWeaponIconPlaceholder(GuiGraphics graphics, int x, int y, float alpha) {
        int a = (int) (alpha * 255) << 24;
        graphics.fill(x, y, x + 9, y + 9, a | 0x888888);
    }

    private static void drawFaded(GuiGraphics graphics, Minecraft mc, String text, int x, int y, int rgb, float alpha) {
        int a = (int) (alpha * 255) << 24;
        graphics.drawString(mc.font, text, x, y, a | (rgb & 0xFFFFFF), false);
    }
}
