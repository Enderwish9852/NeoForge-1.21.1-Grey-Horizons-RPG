package net.enderwish.Farming_Overhaul_Subpack.client;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.block.clay_pot.ClayPotBlockEntity;
import net.enderwish.Farming_Overhaul_Subpack.gui.ClayPotMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ClayPotScreen extends AbstractContainerScreen<ClayPotMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            FarmingOverhaulSubpack.MODID, "textures/gui/clay_pot.png");

    // ── GUI dimensions ────────────────────────────────────────────────────────
    private static final int GUI_WIDTH  = 176;
    private static final int GUI_HEIGHT = 222;

    // ── Arrow (progress indicator) ────────────────────────────────────────────
    private static final int ARROW_X      = 101;
    private static final int ARROW_Y      = 58;
    private static final int ARROW_W      = 22;
    private static final int ARROW_H      = 15;
    private static final int ARROW_TEX_X  = 176;
    private static final int ARROW_TEX_Y  = 14;
    private static final int ARROW_TEX_W  = 24;
    private static final int ARROW_TEX_H  = 16;

    // ── Error icon ────────────────────────────────────────────────────────────
    private static final int ERROR_TEX_X  = 176;
    private static final int ERROR_TEX_Y  = 33;
    private static final int ERROR_TEX_W  = 24;
    private static final int ERROR_TEX_H  = 19;

    // ── Fire indicator ────────────────────────────────────────────────────────
    private static final int FIRE_X       = 61;
    private static final int FIRE_Y       = 101;
    private static final int FIRE_W       = 13;
    private static final int FIRE_H       = 13;
    private static final int FIRE_TEX_X   = 176;
    private static final int FIRE_TEX_Y   = 0;
    private static final int FIRE_TEX_W   = 14;
    private static final int FIRE_TEX_H   = 14;

    // ── Recipe book ───────────────────────────────────────────────────────────
    private static final int BOOK_X         = 16;
    private static final int BOOK_Y         = 40;
    private static final int BOOK_TEX_X     = 176;
    private static final int BOOK_TEX_Y     = 54;
    private static final int BOOK_TEX_W     = 20;  // 196 - 176
    private static final int BOOK_TEX_H     = 18;  // 71 - 54
    private static final int BOOK_SEL_TEX_X = 176;
    private static final int BOOK_SEL_TEX_Y = 73;
    private static final int BOOK_SEL_TEX_H = 18;  // 91 - 73

    // ── Constructor ───────────────────────────────────────────────────────────
    public ClayPotScreen(ClayPotMenu menu, Inventory playerInventory,
                         Component title) {
        super(menu, playerInventory, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = GUI_HEIGHT - 94;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick,
                            int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0,
                imageWidth, imageHeight);

        renderFire(graphics);
        renderProgressOrError(graphics);
        renderRecipeBook(graphics, mouseX, mouseY);
    }

    private void renderFire(GuiGraphics graphics) {
        boolean isLit = ClayPotBlockEntity.isCampfireLit(
                this.minecraft.level,
                menu.getBlockEntity().getBlockPos());
        if (!isLit) return;

        graphics.blit(TEXTURE,
                leftPos + FIRE_X,
                topPos  + FIRE_Y,
                FIRE_TEX_X, FIRE_TEX_Y,
                FIRE_W, FIRE_H);
    }

    private void renderProgressOrError(GuiGraphics graphics) {
        int progress  = menu.getCookProgress();
        int totalTime = menu.getCookTotalTime();
        boolean hasRecipe = totalTime > 0;

        if (hasRecipe && progress > 0) {
            int arrowFill = (int) (ARROW_TEX_W * ((float) progress / totalTime));
            graphics.blit(TEXTURE,
                    leftPos + ARROW_X,
                    topPos  + ARROW_Y,
                    ARROW_TEX_X, ARROW_TEX_Y,
                    arrowFill, ARROW_H);
        } else if (!hasRecipe && hasIngredients()) {
            graphics.blit(TEXTURE,
                    leftPos + ARROW_X,
                    topPos  + ARROW_Y,
                    ERROR_TEX_X, ERROR_TEX_Y,
                    ERROR_TEX_W, ERROR_TEX_H);
        }
    }

    private void renderRecipeBook(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean hovered = mouseX >= leftPos + BOOK_X
                && mouseX <= leftPos + BOOK_X + BOOK_TEX_W
                && mouseY >= topPos  + BOOK_Y
                && mouseY <= topPos  + BOOK_Y + BOOK_TEX_H;

        graphics.blit(TEXTURE,
                leftPos + BOOK_X,
                topPos  + BOOK_Y,
                hovered ? BOOK_SEL_TEX_X : BOOK_TEX_X,
                hovered ? BOOK_SEL_TEX_Y : BOOK_TEX_Y,
                BOOK_TEX_W,
                hovered ? BOOK_SEL_TEX_H : BOOK_TEX_H);
    }

    private boolean hasIngredients() {
        for (int i = 0; i < 9; i++) {
            if (!menu.getSlot(i).getItem().isEmpty()) return true;
        }
        return false;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title,
                this.titleLabelX, this.titleLabelY, 0x404040, false);

        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        int bowls = menu.getBowlsRemaining();
        if (bowls > 0) {
            graphics.drawString(this.font,
                    Component.literal(bowls + "x"),
                    ARROW_X + ARROW_W + 2,
                    ARROW_Y + 4,
                    0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
