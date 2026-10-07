package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.FirstAidKitContents;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.UseKitSlotPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class FirstAidKitScreen extends Screen {

    private static final int SLOT_SIZE = 24;
    private static final int SLOT_GAP = 6;

    private final int handOrdinal;
    private final List<ItemStack> slots;

    public FirstAidKitScreen(int handOrdinal, List<ItemStack> slots) {
        super(Component.literal("First Aid Kit"));
        this.handOrdinal = handOrdinal;
        this.slots = slots;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);

        int totalWidth = slots.size() * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP;
        int startX = this.width / 2 - totalWidth / 2;
        int y = this.height / 2 - SLOT_SIZE / 2;

        for (int i = 0; i < slots.size(); i++) {
            int x = startX + i * (SLOT_SIZE + SLOT_GAP);
            graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0x88000000);
            if (!slots.get(i).isEmpty()) {
                graphics.renderItem(slots.get(i), x + 4, y + 4);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int totalWidth = slots.size() * (SLOT_SIZE + SLOT_GAP) - SLOT_GAP;
        int startX = this.width / 2 - totalWidth / 2;
        int y = this.height / 2 - SLOT_SIZE / 2;

        for (int i = 0; i < slots.size(); i++) {
            int x = startX + i * (SLOT_SIZE + SLOT_GAP);
            if (mouseX >= x && mouseX <= x + SLOT_SIZE && mouseY >= y && mouseY <= y + SLOT_SIZE) {
                if (!slots.get(i).isEmpty()) {
                    ModMessages.sendToServer(new UseKitSlotPacket(handOrdinal, i));
                    onClose();
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}