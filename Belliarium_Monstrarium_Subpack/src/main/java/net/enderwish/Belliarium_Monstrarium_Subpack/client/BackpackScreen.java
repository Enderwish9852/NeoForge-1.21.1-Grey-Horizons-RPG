package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.network.BackpackMovePacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * BackpackScreen
 *
 * Plain Screen (the house style — no MenuType). Shows the equipped
 * backpack's grid on top and the player's hotbar below.
 * Click a backpack slot → move that stack OUT to the hotbar ("picking it
 * out by hand"). Click a hotbar slot → move that stack INTO the backpack.
 * The server re-sends contents after every move; the screen rebuilds.
 */
public class BackpackScreen extends net.minecraft.client.gui.screens.Screen {

    private static final int SLOT = 18, GAP = 2, COLS = 9;

    private List<ItemStack> slots;

    public BackpackScreen(List<ItemStack> slots) {
        super(Component.literal("Backpack"));
        this.slots = slots;
    }

    private int gridWidth() { return COLS * (SLOT + GAP) - GAP; }

    private int rows() { return (slots.size() + COLS - 1) / COLS; }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);

        int x0 = (width - gridWidth()) / 2;
        int y0 = Math.max(24, height / 2 - 60);

        g.drawCenteredString(font, title, width / 2, y0 - 12, 0xFFFFFF);

        for (int i = 0; i < slots.size(); i++) {
            int x = x0 + (i % COLS) * (SLOT + GAP);
            int y = y0 + (i / COLS) * (SLOT + GAP);
            g.fill(x, y, x + SLOT, y + SLOT, 0x88202028);
            g.renderItem(slots.get(i), x + 1, y + 1);
        }

        // hotbar mirror below (deposit targets)
        int hy = y0 + rows() * (SLOT + GAP) + 10;
        g.drawCenteredString(font, "Hotbar", width / 2, hy - 10, 0x00AAAAAA);
        var hotbar = minecraft.player.getInventory().items;
        for (int i = 0; i < 9; i++) {
            int x = x0 + i * (SLOT + GAP);
            g.fill(x, hy, x + SLOT, hy + SLOT, 0x88202820);
            g.renderItem(hotbar.get(i), x + 1, hy + 1);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x0 = (width - gridWidth()) / 2;
        int y0 = Math.max(24, height / 2 - 60);

        int gx = (int) mx - x0, gy = (int) my - y0;
        if (gx >= 0 && gy >= 0 && gx < gridWidth()
                && gy < rows() * (SLOT + GAP) - GAP) {
            int col = gx / (SLOT + GAP), row = gy / (SLOT + GAP);
            int idx = row * COLS + col;
            if (idx < slots.size() && !slots.get(idx).isEmpty()) {
                ModMessages.sendToServer(new BackpackMovePacket(idx, false));
                return true;
            }
        }

        int hy = y0 + rows() * (SLOT + GAP) + 10;
        int hx = (int) mx - x0, hyy = (int) my - hy;
        if (hx >= 0 && hyy >= 0 && hx < gridWidth() && hyy < SLOT) {
            int idx = hx / (SLOT + GAP);
            if (!minecraft.player.getInventory().getItem(idx).isEmpty()) {
                ModMessages.sendToServer(new BackpackMovePacket(idx, true));
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
