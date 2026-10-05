package net.enderwish.Belliarium_Monstrarium_Subpack.client.gui;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyPartColors;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.combat.CombatLogEntry;
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
 * BUGFIX (diagram/scoreboard vanishing when the headline starts typing) --
 * render() used to `return` right after the scoreboard's own reveal window
 * ended, which meant NOTHING from renderBodyAndScoreboard() ever painted
 * again once the headline phase began. Per the original spec ("at the
 * BOTTOM of the screen" implies the diagram/scoreboard are still visible
 * above it), the diagram+scoreboard now always render once past the fade,
 * and the headline is drawn ON TOP of them once its own window arrives --
 * nothing disappears anymore.
 */
public class DeathReportScreen extends Screen {

    private static final long FADE_DURATION_MS = 1500;
    private static final long PER_ROW_REVEAL_MS = 400;
    private static final long MAX_SCOREBOARD_REVEAL_MS = 10_000;
    private static final long POST_SCOREBOARD_PAUSE_MS = 800;
    private static final long TYPEWRITER_CHAR_MS = 35;
    private static final long POST_TYPEWRITER_PAUSE_MS = 300;
    private static final long PULSE_PERIOD_MS = 900;
    private static final int DIAGRAM_SCALE = 4;

    private static final int SCOREBOARD_WIDTH = 200;
    private static final int SCOREBOARD_ROW_HEIGHT = 12;
    private static final int SCOREBOARD_ROWS_VISIBLE = 9;
    private static final int SCOREBOARD_HEIGHT = SCOREBOARD_ROW_HEIGHT * SCOREBOARD_ROWS_VISIBLE;
    private static final int DIAGRAM_TO_BOARD_GAP = 20;
    private static final int DIAGRAM_WIDTH = 35 * DIAGRAM_SCALE;
    private static final int DIAGRAM_HEIGHT = 59 * DIAGRAM_SCALE;

    private final DeathReportPacket data;
    private final long openedAtMillis;
    private boolean buttonAdded = false;
    private int manualScrollOffset = 0;

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

        if (!data.combatDeath()) {
            renderHeadline(graphics, postFade);
            return;
        }

        // THE FIX -- always render, never gated behind an early return.
        renderBodyAndScoreboard(graphics, mouseX, mouseY, postFade);

        long scoreboardWindow = totalRevealDuration() + POST_SCOREBOARD_PAUSE_MS;
        if (postFade >= scoreboardWindow) {
            renderHeadline(graphics, postFade - scoreboardWindow);
        }
    }

    private long totalRevealDuration() {
        int count = data.log().size();
        return Math.min((long) count * PER_ROW_REVEAL_MS, MAX_SCOREBOARD_REVEAL_MS);
    }

    private int rowsRevealed(long postFade) {
        int count = data.log().size();
        if (count == 0) return 0;
        long duration = totalRevealDuration();
        float t = duration <= 0 ? 1f : Mth.clamp(postFade / (float) duration, 0f, 1f);
        float eased = 1f - (float) Math.pow(1f - t, 3);
        return Math.min(count, (int) Math.floor(eased * count));
    }

    private int diagramOriginX() {
        int totalBlockWidth = DIAGRAM_WIDTH + DIAGRAM_TO_BOARD_GAP + SCOREBOARD_WIDTH;
        return this.width / 2 - totalBlockWidth / 2;
    }

    private int diagramOriginY() {
        return this.height / 2 - DIAGRAM_HEIGHT / 2;
    }

    private int boardOriginX() {
        return diagramOriginX() + DIAGRAM_WIDTH + DIAGRAM_TO_BOARD_GAP;
    }

    private int boardOriginY() {
        return this.height / 2 - SCOREBOARD_HEIGHT / 2;
    }

    private void renderBodyAndScoreboard(GuiGraphics graphics, int mouseX, int mouseY, long postFade) {
        renderDiagram(graphics, diagramOriginX(), diagramOriginY());
        renderScoreboard(graphics, boardOriginX(), boardOriginY(), rowsRevealed(postFade));
    }

    private void renderDiagram(GuiGraphics graphics, int originX, int originY) {
        drawScaledPart(graphics, originX, originY, 12, 0, 10, 10, data.headPct(), hitPart("Head"));
        drawScaledPart(graphics, originX, originY, 8, 11, 18, 22, data.torsoPct(), hitPart("Torso"));
        drawScaledPart(graphics, originX, originY, 0, 11, 7, 20, data.leftArmPct(), hitPart("Left Arm"));
        drawScaledPart(graphics, originX, originY, 27, 11, 7, 20, data.rightArmPct(), hitPart("Right Arm"));
        drawScaledPart(graphics, originX, originY, 8, 34, 8, 18, data.leftLegPct(), hitPart("Left Leg"));
        drawScaledPart(graphics, originX, originY, 17, 34, 8, 18, data.rightLegPct(), hitPart("Right Leg"));
        drawScaledPart(graphics, originX, originY, 8, 53, 8, 6, data.leftFootPct(), hitPart("Left Foot"));
        drawScaledPart(graphics, originX, originY, 17, 53, 8, 6, data.rightFootPct(), hitPart("Right Foot"));
    }

    private boolean hitPart(String label) {
        for (CombatLogEntry entry : data.log()) {
            if (entry.bodyPart().toLowerCase().contains(label.toLowerCase())) return true;
        }
        return false;
    }

    private void drawScaledPart(GuiGraphics graphics, int originX, int originY, int dx, int dy, int w, int h,
                                float pct, boolean pulsing) {
        int sx = originX + dx * DIAGRAM_SCALE;
        int sy = originY + dy * DIAGRAM_SCALE;
        int sw = w * DIAGRAM_SCALE;
        int sh = h * DIAGRAM_SCALE;
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

    private void renderScoreboard(GuiGraphics graphics, int boxX, int boxY, int totalRevealed) {
        graphics.fill(boxX - 2, boxY - 2, boxX + SCOREBOARD_WIDTH + 2, boxY + SCOREBOARD_HEIGHT + 2, 0xAA000000);

        int maxScroll = Math.max(0, totalRevealed - SCOREBOARD_ROWS_VISIBLE);
        manualScrollOffset = Mth.clamp(manualScrollOffset, 0, maxScroll);
        int topIndex = maxScroll - manualScrollOffset;

        graphics.enableScissor(boxX, boxY, boxX + SCOREBOARD_WIDTH, boxY + SCOREBOARD_HEIGHT);
        List<CombatLogEntry> log = data.log();
        int visibleCount = Math.min(SCOREBOARD_ROWS_VISIBLE, totalRevealed - topIndex);
        for (int i = 0; i < visibleCount; i++) {
            CombatLogEntry entry = log.get(topIndex + i);
            String line = String.format("%ds  %s  %.1fm  %s  -%.1f",
                    entry.secondsIntoBattle(), entry.sourceName(),
                    entry.distanceBlocks(), entry.bodyPart(), entry.amount());
            graphics.drawString(this.font, line, boxX + 2, boxY + i * SCOREBOARD_ROW_HEIGHT, 0xFFFFFFFF);
        }
        graphics.disableScissor();
    }

    private boolean isMouseOverScoreboard(int mouseX, int mouseY) {
        int boxX = boardOriginX();
        int boxY = boardOriginY();
        return mouseX >= boxX && mouseX <= boxX + SCOREBOARD_WIDTH
                && mouseY >= boxY && mouseY <= boxY + SCOREBOARD_HEIGHT;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOverScoreboard((int) mouseX, (int) mouseY)) {
            int maxScroll = Math.max(0, data.log().size() - SCOREBOARD_ROWS_VISIBLE);
            manualScrollOffset = Mth.clamp(manualScrollOffset - (int) Math.signum(scrollY), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
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
