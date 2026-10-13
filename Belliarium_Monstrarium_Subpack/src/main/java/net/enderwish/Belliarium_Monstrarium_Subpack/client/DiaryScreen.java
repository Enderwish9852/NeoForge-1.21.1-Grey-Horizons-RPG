package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DayEntry;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DiaryBook;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.diary.DiaryContent;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.DiarySavePacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * DiaryScreen
 *
 * One page per day: title, printed server lines, printed end-of-day summary, then the
 * player's own writing. MultiLineEditBox (constructor and getValue/setValue confirmed for 1.21).
 * Notes are saved when changing page, on resize, and on close.
 */
public class DiaryScreen extends Screen {

    private static final int PARCHMENT = 0xFFE6D8B4;
    private static final int BORDER = 0xFF6B5330;
    private static final int INK = 0xFF3A2A14;
    private static final int SERVER_INK = 0xFF6A5A3A;
    private static final int SUMMARY_INK = 0xFF2F5A2F;

    private final int handOrdinal;
    private DiaryContent content;
    private int index;
    private MultiLineEditBox notesBox;

    public DiaryScreen(int handOrdinal, DiaryContent content) {
        super(Component.literal("Survivor's Diary"));
        this.handOrdinal = handOrdinal;
        this.content = content;
        this.index = Math.max(0, content.entries().size() - 1);
    }

    private DayEntry current() { return content.entries().get(index); }

    private int panelW() { return Math.min(300, this.width - 20); }
    private int panelX() { return (this.width - panelW()) / 2; }
    private int panelY() { return 12; }
    private int panelH() { return this.height - 24; }
    private int wrapWidth() { return panelW() - 20; }

    private int linesHeight(List<String> lines) {
        int count = 0;
        for (String line : lines) count += this.font.split(Component.literal(line), wrapWidth()).size();
        return count * (this.font.lineHeight + 1);
    }

    private int notesTop() {
        DayEntry entry = current();
        int top = panelY() + 24 + linesHeight(entry.serverLines());
        if (!entry.summaryLines().isEmpty()) top += 4 + linesHeight(entry.summaryLines());
        return top + 6;
    }

    @Override
    protected void init() {
        int x = panelX(), y = panelY(), w = panelW(), h = panelH();
        int top = notesTop();
        int boxHeight = Math.max(40, y + h - 34 - top);

        notesBox = new MultiLineEditBox(this.font, x + 8, top, w - 16, boxHeight,
                Component.literal("Write here..."), Component.empty());
        notesBox.setValue(current().notes());
        this.addRenderableWidget(notesBox);
        this.setInitialFocus(notesBox);

        int buttonY = y + h - 26;
        if (index > 0) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), b -> changePage(-1))
                    .bounds(x + 8, buttonY, 20, 20).build());
        }
        if (index < content.entries().size() - 1) {
            this.addRenderableWidget(Button.builder(Component.literal(">"), b -> changePage(1))
                    .bounds(x + w - 28, buttonY, 20, 20).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> onClose())
                .bounds(this.width / 2 - 40, buttonY, 80, 20).build());
    }

    private void changePage(int delta) {
        saveCurrent();
        index = Math.max(0, Math.min(content.entries().size() - 1, index + delta));
        this.clearWidgets();
        this.init();
    }

    private void saveCurrent() {
        if (notesBox == null) return;
        String text = notesBox.getValue();
        if (text.length() > DiaryBook.MAX_NOTES_CHARS) text = text.substring(0, DiaryBook.MAX_NOTES_CHARS);

        DayEntry entry = current();
        if (text.equals(entry.notes())) return;

        content = content.withEntryReplaced(index, entry.withNotes(text));
        ModMessages.sendToServer(new DiarySavePacket(handOrdinal, index, text));
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        saveCurrent();
        super.resize(minecraft, width, height);
    }

    @Override
    public void onClose() {
        saveCurrent();
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        int x = panelX(), y = panelY(), w = panelW(), h = panelH();
        graphics.fill(x - 2, y - 2, x + w + 2, y + h + 2, BORDER);
        graphics.fill(x, y, x + w, y + h, PARCHMENT);

        super.render(graphics, mouseX, mouseY, partialTick);

        DayEntry entry = current();
        graphics.drawString(this.font, entry.title(), x + 10, y + 8, INK, false);

        int ty = y + 24;
        ty = drawLines(graphics, entry.serverLines(), x + 10, ty, SERVER_INK);
        if (!entry.summaryLines().isEmpty()) {
            drawLines(graphics, entry.summaryLines(), x + 10, ty + 4, SUMMARY_INK);
        }

        graphics.drawCenteredString(this.font, "Entry " + (index + 1) + " / " + content.entries().size(),
                this.width / 2, y + h - 38, SERVER_INK);
    }

    private int drawLines(GuiGraphics graphics, List<String> lines, int x, int y, int color) {
        for (String line : lines) {
            for (FormattedCharSequence seq : this.font.split(Component.literal(line), wrapWidth())) {
                graphics.drawString(this.font, seq, x, y, color, false);
                y += this.font.lineHeight + 1;
            }
        }
        return y;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
