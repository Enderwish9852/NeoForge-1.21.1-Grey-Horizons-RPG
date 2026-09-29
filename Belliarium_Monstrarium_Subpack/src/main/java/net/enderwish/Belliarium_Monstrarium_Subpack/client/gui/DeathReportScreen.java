package net.enderwish.Belliarium_Monstrarium_Subpack.client.gui;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.BodyPartColors;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.CombatLogEntry;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.DeathReportPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * DeathReportScreen
 *
 * Combat death sequence: fade to black -> body diagram + scoreboard rolling
 * in row by row (parts that took damage pulse in their real damage-level
 * color) -> typewritten headline -> "Accept Fate" button -> vanilla DeathScreen.
 *
 * Non-combat death sequence: fade to black -> typewritten headline (vanilla's
 * own death message, e.g. "Player fell from a high place") -> button ->
 * vanilla DeathScreen. No diagram/scoreboard, since there's no combat log.
 *
 * All timing/layout constants below are first-pass placeholders, same status
 * as the tuning numbers already scattered through SurvivalTickHandler --
 * adjust freely once you can see it running.
 *
 * VERIFY IF COMPILE FAILS -- DeathScreen's (Component, boolean) constructor
 * and Level#getLevelData().isHardcore() are written from memory of long-stable
 * vanilla API, not confirmed against source pasted in this conversation.
 */
public class DeathReportScreen extends Screen {

    private static final long FADE_DURATION_MS = 1500;
    private static final long PER_ROW_REVEAL_MS = 400;
    private static final long POST_SCOREBOARD_PAUSE_MS = 800;
    private static final long TYPEWRITER_CHAR_MS = 35;
    private static final long POST_TYPEWRITER_PAUSE_MS = 300;
    private static final long PULSE_PERIOD_MS = 900;
    private static final int DIAGRAM_SCALE = 4;

    private final DeathReportPacket data;
    private final long openedAtMillis;
    private boolean buttonAdded = false;

    public DeathReportScreen(DeathReportPacket data) {
        super(Component.literal("Death Report"));
        this.data = data;
        this.openedAtMillis = System.currentTimeMillis();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        long age = System.currentTimeMillis() - openedAtMillis;
        float fadeAlpha = Mth.clamp(age / (float) FADE_DURATION_MS, 0f, 1f);
        int a = (int) (fadeAlpha * 255) << 24;
        graphics.fill(0, 0, this.width, this.height, a);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        long age = System.currentTimeMillis() - openedAtMillis;
        if (age < FADE_DURATION_MS) return;

        long postFade = age - FADE_DURATION_MS;
        long scoreboardWindow = data.combatDeath()
                ? data.log().size() * PER_ROW_REVEAL_MS + POST_SCOREBOARD_PAUSE_MS
                : 0L;

        if (data.combatDeath() && postFade < scoreboardWindow) {
            renderBodyAndScoreboard(graphics, postFade);
            return;
        }

        renderHeadline(graphics, postFade - scoreboardWindow);
    }

    private void renderBodyAndScoreboard(GuiGraphics graphics, long postFade) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        renderDiagram(graphics, centerX - 140, centerY - 60);

        int rowsRevealed = (int) Math.min(data.log().size(), postFade / PER_ROW_REVEAL_MS);
        List<CombatLogEntry> log = data.log();
        int rowX = centerX + 10;
        int rowY = centerY - 60;
        for (int i = 0; i < rowsRevealed; i++) {
            CombatLogEntry entry = log.get(i);
            String line = String.format("%ds  %s  %.1fm  %s  -%.1f",
                    entry.secondsIntoBattle(), entry.sourceName(),
                    entry.distanceBlocks(), entry.bodyPart(), entry.amount());
            graphics.drawString(this.font, line, rowX, rowY + i * 12, 0xFFFFFFFF);
        }
    }

    private void renderDiagram(GuiGraphics graphics, int x, int y) {
        drawScaledPart(graphics, x + 12, y, 10, 10, data.headPct(), hitPart("Head"));
        drawScaledPart(graphics, x + 8, y + 11, 18, 22, data.torsoPct(), hitPart("Torso"));
        drawScaledPart(graphics, x, y + 11, 7, 20, data.leftArmPct(), hitPart("Left Arm"));
        drawScaledPart(graphics, x + 27, y + 11, 7, 20, data.rightArmPct(), hitPart("Right Arm"));
        drawScaledPart(graphics, x + 8, y + 34, 8, 18, data.leftLegPct(), hitPart("Left Leg"));
        drawScaledPart(graphics, x + 17, y + 34, 8, 18, data.rightLegPct(), hitPart("Right Leg"));
        drawScaledPart(graphics, x + 8, y + 53, 8, 6, data.leftFootPct(), hitPart("Left Foot"));
        drawScaledPart(graphics, x + 17, y + 53, 8, 6, data.rightFootPct(), hitPart("Right Foot"));
    }

    /** Only parts that appear in the combat log pulse -- matches your confirmed spec. */
    private boolean hitPart(String label) {
        for (CombatLogEntry entry : data.log()) {
            if (entry.bodyPart().toLowerCase().contains(label.toLowerCase())) return true;
        }
        return false;
    }

    private void drawScaledPart(GuiGraphics graphics, int x, int y, int w, int h, float pct, boolean pulsing) {
        int sx = x * DIAGRAM_SCALE, sy = y * DIAGRAM_SCALE, sw = w * DIAGRAM_SCALE, sh = h * DIAGRAM_SCALE;
        int color = BodyPartColors.colorForPct(pct);

        float alpha = 1.0f;
        if (pulsing) {
            double phase = (System.currentTimeMillis() % PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS;
            alpha = 0.35f + 0.65f * (float) (0.5 - 0.5 * Math.cos(phase * Math.PI * 2));
        }
        int a = (int) (alpha * 255) << 24;
        int finalColor = a | (color & 0xFFFFFF);

        graphics.fill(sx, sy, sx + sw, sy + sh, 0xAA000000);
        graphics.fill(sx + 2, sy + 2, sx + sw - 2, sy + sh - 2, finalColor);
    }

    private void renderHeadline(GuiGraphics graphics, long typewriterAge) {
        String full = data.headline();
        int visibleChars = (int) Math.min(full.length(), Math.max(0, typewriterAge) / TYPEWRITER_CHAR_MS);
        String shown = full.substring(0, visibleChars);

        int centerX = this.width / 2;
        int y = this.height - 60;
        graphics.drawCenteredString(this.font, shown, centerX, y, 0xFFFFFFFF);

        boolean fullyTyped = visibleChars >= full.length();
        long sinceFullyTyped = typewriterAge - (long) full.length() * TYPEWRITER_CHAR_MS;

        if (fullyTyped && sinceFullyTyped >= POST_TYPEWRITER_PAUSE_MS && !buttonAdded) {
            addAcceptButton(centerX, y + 16);
        }
    }

    private void addAcceptButton(int centerX, int y) {
        buttonAdded = true;
        this.addRenderableWidget(Button.builder(Component.literal("Accept Fate"), b -> handOffToVanilla())
                .bounds(centerX - 60, y, 120, 20).build());
    }

    private void handOffToVanilla() {
        Minecraft mc = this.minecraft;
        if (mc == null) return;
        boolean hardcore = mc.level != null && mc.level.getLevelData().isHardcore();
        mc.setScreen(new DeathScreen(Component.literal(data.headline()), hardcore));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
