package net.enderwish.Farming_Overhaul_Subpack.client;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.block.clay_pot.ClayPotBlockEntity;
import net.enderwish.Farming_Overhaul_Subpack.core.claypot.ClayPotRecipe;
import net.enderwish.Farming_Overhaul_Subpack.gui.ClayPotMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ClayPotScreen extends AbstractContainerScreen<ClayPotMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            FarmingOverhaulSubpack.MODID, "textures/gui/clay_pot.png");

    private static final int GUI_WIDTH  = 176;
    private static final int GUI_HEIGHT = 222;

    private static final int ARROW_X      = 101;
    private static final int ARROW_Y      = 58;
    private static final int ARROW_W      = 22;
    private static final int ARROW_H      = 15;
    private static final int ARROW_TEX_X  = 176;
    private static final int ARROW_TEX_Y  = 14;
    private static final int ARROW_TEX_W  = 24;
    private static final int ARROW_TEX_H  = 16;

    private static final int ERROR_TEX_X  = 176;
    private static final int ERROR_TEX_Y  = 33;
    private static final int ERROR_TEX_W  = 24;
    private static final int ERROR_TEX_H  = 19;

    private static final int FIRE_X       = 61;
    private static final int FIRE_Y       = 101;
    private static final int FIRE_W       = 13;
    private static final int FIRE_H       = 13;
    private static final int FIRE_TEX_X   = 176;
    private static final int FIRE_TEX_Y   = 0;
    private static final int FIRE_TEX_W   = 14;
    private static final int FIRE_TEX_H   = 14;

    private static final int BOOK_X         = 16;
    private static final int BOOK_Y         = 40;
    private static final int BOOK_TEX_X     = 176;
    private static final int BOOK_TEX_Y     = 54;
    private static final int BOOK_TEX_W     = 20;
    private static final int BOOK_TEX_H     = 18;
    private static final int BOOK_SEL_TEX_X = 176;
    private static final int BOOK_SEL_TEX_Y = 73;
    private static final int BOOK_SEL_TEX_H = 18;

    private final ClayPotRecipeBookComponent recipeBook =
            new ClayPotRecipeBookComponent();
    private ClayPotRecipe selectedRecipe = null;

    public ClayPotScreen(ClayPotMenu menu, Inventory playerInventory,
                         Component title) {
        super(menu, playerInventory, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = GUI_HEIGHT - 94;
    }

    @Override
    protected void init() {
        super.init();
        // Init recipe book with screen dimensions — mirrors vanilla exactly
        recipeBook.init(this.width, this.height,
                recipe -> selectedRecipe = recipe);
        // Set leftPos using vanilla's formula via recipe book
        this.leftPos = recipeBook.updateScreenPosition(this.width, this.imageWidth);
        this.topPos  = (this.height - this.imageHeight) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick,
                            int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0,
                imageWidth, imageHeight);

        renderFire(graphics);
        renderProgressOrError(graphics);
        renderBookButton(graphics, mouseX, mouseY);
        renderGhostItems(graphics);
    }

    private void renderFire(GuiGraphics graphics) {
        boolean isLit = ClayPotBlockEntity.isCampfireLit(
                this.minecraft.level,
                menu.getBlockEntity().getBlockPos());
        if (!isLit) return;

        graphics.blit(TEXTURE,
                leftPos + FIRE_X, topPos + FIRE_Y,
                FIRE_TEX_X, FIRE_TEX_Y, FIRE_W, FIRE_H);
    }

    private void renderProgressOrError(GuiGraphics graphics) {
        int progress  = menu.getCookProgress();
        int totalTime = menu.getCookTotalTime();
        boolean hasRecipe = totalTime > 0;

        if (hasRecipe && progress > 0) {
            int arrowFill = (int) (ARROW_TEX_W * ((float) progress / totalTime));
            graphics.blit(TEXTURE,
                    leftPos + ARROW_X, topPos + ARROW_Y,
                    ARROW_TEX_X, ARROW_TEX_Y, arrowFill, ARROW_H);
        } else if (!hasRecipe && hasIngredients()) {
            graphics.blit(TEXTURE,
                    leftPos + ARROW_X, topPos + ARROW_Y,
                    ERROR_TEX_X, ERROR_TEX_Y, ERROR_TEX_W, ERROR_TEX_H);
        }
    }

    private void renderBookButton(GuiGraphics graphics, int mouseX, int mouseY) {
        boolean hovered = mouseX >= leftPos + BOOK_X
                && mouseX <= leftPos + BOOK_X + BOOK_TEX_W
                && mouseY >= topPos  + BOOK_Y
                && mouseY <= topPos  + BOOK_Y + BOOK_TEX_H;
        boolean selected = recipeBook.isVisible();

        graphics.blit(TEXTURE,
                leftPos + BOOK_X, topPos + BOOK_Y,
                (hovered || selected) ? BOOK_SEL_TEX_X : BOOK_TEX_X,
                (hovered || selected) ? BOOK_SEL_TEX_Y : BOOK_TEX_Y,
                BOOK_TEX_W,
                (hovered || selected) ? BOOK_SEL_TEX_H : BOOK_TEX_H);
    }

    private void renderGhostItems(GuiGraphics graphics) {
        if (selectedRecipe == null) return;

        if (selectedRecipe.shaped()) {
            for (int row = 0; row < 3; row++) {
                String patternRow = row < selectedRecipe.pattern().size()
                        ? selectedRecipe.pattern().get(row) : "   ";
                while (patternRow.length() < 3) patternRow += " ";
                for (int col = 0; col < 3; col++) {
                    char key = patternRow.charAt(col);
                    if (key == ' ') continue;
                    String itemId = selectedRecipe.keys().get(String.valueOf(key));
                    if (itemId == null) continue;
                    if (menu.getSlot(row * 3 + col).getItem().isEmpty()) {
                        renderGhostItem(graphics, itemId,
                                leftPos + ClayPotMenu.GRID_X + col * 18 + 1,
                                topPos  + ClayPotMenu.GRID_Y + row * 18 + 1);
                    }
                }
            }
        } else {
            List<String> ingredients = selectedRecipe.ingredients();
            for (int i = 0; i < ingredients.size() && i < 9; i++) {
                if (menu.getSlot(i).getItem().isEmpty()) {
                    renderGhostItem(graphics, ingredients.get(i),
                            leftPos + ClayPotMenu.GRID_X + (i % 3) * 18 + 1,
                            topPos  + ClayPotMenu.GRID_Y + (i / 3) * 18 + 1);
                }
            }
        }

        if (selectedRecipe.requiresWater() && menu.getSlot(9).getItem().isEmpty()) {
            renderGhostItem(graphics, "minecraft:water_bucket",
                    leftPos + ClayPotMenu.WATER_SLOT_X + 1,
                    topPos  + ClayPotMenu.WATER_SLOT_Y + 1);
        }
    }

    private void renderGhostItem(GuiGraphics graphics, String itemId, int x, int y) {
        ItemStack ghost = new ItemStack(
                net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .get(net.minecraft.resources.ResourceLocation.parse(itemId)));
        if (ghost.isEmpty()) return;

        graphics.fill(x, y, x + 16, y + 16, 822018048);
        graphics.renderFakeItem(ghost, x, y);
        graphics.fill(RenderType.guiGhostRecipeOverlay(),
                x, y, x + 16, y + 16, 822083583);
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
                    ARROW_X + ARROW_W + 2, ARROW_Y + 4,
                    0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        recipeBook.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Book button toggle — mirrors vanilla's ImageButton click
        if (mouseX >= leftPos + BOOK_X && mouseX <= leftPos + BOOK_X + BOOK_TEX_W
                && mouseY >= topPos + BOOK_Y && mouseY <= topPos + BOOK_Y + BOOK_TEX_H) {
            recipeBook.toggleVisibility();
            // Recalculate leftPos exactly like vanilla CraftingScreen does
            this.leftPos = recipeBook.updateScreenPosition(this.width, this.imageWidth);
            this.topPos  = (this.height - this.imageHeight) / 2;
            return true;
        }

        if (recipeBook.mouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (recipeBook.keyPressed(keyCode, scanCode, modifiers)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (recipeBook.charTyped(c, modifiers)) return true;
        return super.charTyped(c, modifiers);
    }
}
