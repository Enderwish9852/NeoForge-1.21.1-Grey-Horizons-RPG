package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.gui.BackpackMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * BackpackScreen
 *
 * ONE container screen for every backpack tier. The texture is picked by
 * the backpack id from the menu, and the per-bag image size is the menu's
 * own Layout record — client and server menus agree because the server
 * packed the config into the menu-open buffer.
 *
 * B (the Open-Backpack key) toggles the screen closed again, and item
 * tooltips are kept inside the GUI window instead of hanging off it.
 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {

    public BackpackScreen(BackpackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        BackpackMenu.Layout layout = menu.layout();
        this.imageWidth = layout.imageWidth();
        this.imageHeight = layout.imageHeight();
    }

    private ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath(
                BelliariumMonstrariumSubpack.MODID,
                "textures/gui/" + menu.backpackId() + ".png");
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // B again closes the GUI (onClose -> closeContainer keeps server in sync).
        if (ModKeyMappings.OPEN_BACKPACK.matches(keyCode, scanCode)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // vanilla container screens call renderTooltip from their own
        // render() (AbstractContainerScreen.render never does); same pattern.
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.menu.getCarried().isEmpty() && this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            ItemStack stack = this.hoveredSlot.getItem();
            List<Component> lines = this.getTooltipFromContainerItem(stack);
            // vanilla's positioner places the box at (mouseX + 12, mouseY - 12)
            // and only clamps it to the SCREEN edges; we pre-shift the anchor so
            // the box stays inside the bag window — and a NAME LONGER THAN THE
            // WINDOW can never fit anywhere, so it is ellipsized instead of
            // hanging outside the GUI. Box = text + 6px border; +12/+8 are safe
            // over-estimates.
            int maxBox = this.imageWidth - 8;
            int textWidth = 0;
            for (Component line : lines) {
                textWidth = Math.max(textWidth, this.font.width(line));
            }
            if (textWidth > maxBox && !lines.isEmpty()) {
                lines.set(0, fit(lines.get(0), maxBox - 12));
                textWidth = 0;
                for (Component line : lines) {
                    textWidth = Math.max(textWidth, this.font.width(line));
                }
            }
            int boxWidth = textWidth + 12;
            int boxHeight = lines.size() * 12 + 8;
            int left = Mth.clamp(mouseX + 12,
                    this.leftPos + 4,
                    Math.max(this.leftPos + 4, this.leftPos + this.imageWidth - boxWidth));
            int top = Mth.clamp(mouseY - 12,
                    this.topPos + 4,
                    Math.max(this.topPos + 4, this.topPos + this.imageHeight - boxHeight));
            graphics.renderTooltip(this.font, lines, stack.getTooltipImage(), stack, left - 12, top + 12);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(texture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Centered on the GUI window and ellipsized to its width, so the
        // name can never hang outside whatever its length. Vanilla only
        // left-anchors titles at x=8 (AbstractContainerScreen:111), which
        // reads off-center on the special-slot bags whose grid is inset
        // between the two flanking special slots.
        String text = this.title.getString();
        int maxWidth = this.imageWidth - 8;
        while (this.font.width(text) > maxWidth && text.length() > 4) {
            text = text.substring(0, text.length() - 2) + "...";
        }
        int x = Math.max(4, (this.imageWidth - this.font.width(text)) / 2);
        graphics.drawString(this.font, text, x, this.titleLabelY, 0x404040, false);
    }

    /** Truncates text with an ellipsis so it never runs past maxWidth. */
    private Component fit(Component text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) return text;
        String raw = text.getString();
        String suffix = "...";
        int keep = this.font.width(suffix);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            sb.append(raw.charAt(i));
            if (this.font.width(sb.toString()) + keep > maxWidth) {
                sb.deleteCharAt(sb.length() - 1);
                break;
            }
        }
        return Component.literal(sb + suffix);
    }
}
