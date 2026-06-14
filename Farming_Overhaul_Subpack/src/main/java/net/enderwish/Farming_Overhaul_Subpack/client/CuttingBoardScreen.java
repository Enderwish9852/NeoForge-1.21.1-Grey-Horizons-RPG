package net.enderwish.Farming_Overhaul_Subpack.client;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.core.cutting_board.CuttingBoardRecipe;
import net.enderwish.Farming_Overhaul_Subpack.gui.CuttingBoardMenu;
import net.enderwish.Farming_Overhaul_Subpack.item.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class CuttingBoardScreen
        extends AbstractContainerScreen<CuttingBoardMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    FarmingOverhaulSubpack.MODID,
                    "textures/gui/cutting_board.png");

    // ── GUI dimensions ────────────────────────────────────────────────────────
    private static final int GUI_WIDTH  = 176;
    private static final int GUI_HEIGHT = 222;

    // ── Progress arrow ────────────────────────────────────────────────────────
    private static final int ARROW_X     = 115;
    private static final int ARROW_Y     = 63;
    private static final int ARROW_TEX_X = 176;
    private static final int ARROW_TEX_Y = 3;
    private static final int ARROW_TEX_W = 24;
    private static final int ARROW_H     = 15;

    // ── Error icon ────────────────────────────────────────────────────────────
    private static final int ERROR_TEX_X = 176;
    private static final int ERROR_TEX_Y = 22;
    private static final int ERROR_TEX_W = 24;
    private static final int ERROR_TEX_H = 19;

    // ── Book button ───────────────────────────────────────────────────────────
    private static final int BOOK_X         = 30;
    private static final int BOOK_Y         = 43;
    private static final int BOOK_TEX_X     = 176;
    private static final int BOOK_TEX_Y     = 44;
    private static final int BOOK_TEX_W     = 20;
    private static final int BOOK_TEX_H     = 18;
    private static final int BOOK_SEL_TEX_X = 176;
    private static final int BOOK_SEL_TEX_Y = 63;
    private static final int BOOK_SEL_TEX_H = 18;

    // ── Cycling indicators ────────────────────────────────────────────────────
    // Texture sprites: bundle at (176,81) 18×18, knife at (176,99) 18×18
    private static final int INDICATOR_TEX_X        = 176;
    private static final int INDICATOR_BUNDLE_TEX_Y = 81;
    private static final int INDICATOR_KNIFE_TEX_Y  = 99;
    private static final int INDICATOR_W            = 18;
    private static final int INDICATOR_H            = 18;
    // Screen position — adjust to match your GUI layout
    private static final int INDICATOR_SCREEN_X          = 156;
    private static final int INDICATOR_TOOL_SCREEN_Y      = 50;
    private static final int INDICATOR_CONTAINER_SCREEN_Y = 70;
    // Cycle every 80 frames (~4 seconds at 20fps)
    private static final int CYCLE_FRAMES = 80;

    // ── Recipe book ───────────────────────────────────────────────────────────
    private final CuttingBoardRecipeBookComponent recipeBook =
            new CuttingBoardRecipeBookComponent();
    private CuttingBoardRecipe selectedRecipe = null;

    // ── Cycling state ─────────────────────────────────────────────────────────
    // Group 1: cleaver + bowl
    // Group 2: knife + bundle
    private int     indicatorTimer = 0;
    private boolean showGroup1     = true;

    public CuttingBoardScreen(CuttingBoardMenu menu,
                              Inventory playerInventory,
                              Component title) {
        super(menu, playerInventory, title);
        this.imageWidth      = GUI_WIDTH;
        this.imageHeight     = GUI_HEIGHT;
        this.inventoryLabelY = GUI_HEIGHT - 94;
    }

    @Override
    protected void init() {
        super.init();
        recipeBook.init(this.width, this.height,
                recipe -> selectedRecipe = recipe);
        this.leftPos = recipeBook.updateScreenPosition(
                this.width, this.imageWidth);
        this.topPos  = (this.height - this.imageHeight) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick,
                            int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0,
                imageWidth, imageHeight);
        renderProgress(graphics);
        renderBookButton(graphics, mouseX, mouseY);
        renderCyclingIndicators(graphics);
        renderGhostItems(graphics);
    }

    private void renderProgress(GuiGraphics graphics) {
        int progress  = menu.getChopProgress();
        int totalTime = menu.getChopTotalTime();

        if (totalTime > 0 && progress > 0) {
            int fill = (int) (ARROW_TEX_W * ((float) progress / totalTime));
            graphics.blit(TEXTURE,
                    leftPos + ARROW_X, topPos + ARROW_Y,
                    ARROW_TEX_X, ARROW_TEX_Y,
                    fill, ARROW_H);
        } else if (totalTime <= 0 && hasIngredients()) {
            graphics.blit(TEXTURE,
                    leftPos + ARROW_X, topPos + ARROW_Y,
                    ERROR_TEX_X, ERROR_TEX_Y,
                    ERROR_TEX_W, ERROR_TEX_H);
        }
    }

    private void renderBookButton(GuiGraphics graphics,
                                  int mouseX, int mouseY) {
        boolean hovered = mouseX >= leftPos + BOOK_X
                && mouseX <= leftPos + BOOK_X + BOOK_TEX_W
                && mouseY >= topPos  + BOOK_Y
                && mouseY <= topPos  + BOOK_Y + BOOK_TEX_H;
        boolean open = recipeBook.isVisible();

        graphics.blit(TEXTURE,
                leftPos + BOOK_X, topPos + BOOK_Y,
                (hovered || open) ? BOOK_SEL_TEX_X : BOOK_TEX_X,
                (hovered || open) ? BOOK_SEL_TEX_Y : BOOK_TEX_Y,
                BOOK_TEX_W,
                (hovered || open) ? BOOK_SEL_TEX_H : BOOK_TEX_H);
    }

    private void renderCyclingIndicators(GuiGraphics graphics) {
        boolean displayGroup1;

        if (selectedRecipe != null) {
            // Recipe selected — freeze on the correct group
            // "any" keeps cycling, "knife" shows group 2, "cleaver" shows group 1
            String tool = selectedRecipe.toolType();
            if (tool.equals("any")) {
                displayGroup1 = showGroup1; // keep cycling
            } else {
                displayGroup1 = tool.equals("cleaver");
            }
        } else {
            displayGroup1 = showGroup1;
        }

        if (displayGroup1) {
            // Group 1: cleaver + bowl — render as live item icons
            graphics.renderItem(
                    new ItemStack(ModItems.CLEAVER.get()),
                    leftPos + INDICATOR_SCREEN_X,
                    topPos  + INDICATOR_TOOL_SCREEN_Y);
            graphics.renderItem(
                    new ItemStack(Items.BOWL),
                    leftPos + INDICATOR_SCREEN_X,
                    topPos  + INDICATOR_CONTAINER_SCREEN_Y);
        } else {
            // Group 2: knife + bundle — use texture sprites
            graphics.blit(TEXTURE,
                    leftPos + INDICATOR_SCREEN_X,
                    topPos  + INDICATOR_TOOL_SCREEN_Y,
                    INDICATOR_TEX_X, INDICATOR_KNIFE_TEX_Y,
                    INDICATOR_W, INDICATOR_H);
            graphics.blit(TEXTURE,
                    leftPos + INDICATOR_SCREEN_X,
                    topPos  + INDICATOR_CONTAINER_SCREEN_Y,
                    INDICATOR_TEX_X, INDICATOR_BUNDLE_TEX_Y,
                    INDICATOR_W, INDICATOR_H);
        }
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
                    String itemId = selectedRecipe.keys()
                            .get(String.valueOf(key));
                    if (itemId == null) continue;
                    if (menu.getSlot(row * 3 + col).getItem().isEmpty()) {
                        renderGhostItem(graphics, itemId,
                                leftPos + CuttingBoardMenu.GRID_X + col * 18 + 1,
                                topPos  + CuttingBoardMenu.GRID_Y + row * 18 + 1);
                    }
                }
            }
        } else {
            List<String> ingredients = selectedRecipe.ingredients();
            for (int i = 0; i < ingredients.size() && i < 9; i++) {
                if (menu.getSlot(i).getItem().isEmpty()) {
                    renderGhostItem(graphics, ingredients.get(i),
                            leftPos + CuttingBoardMenu.GRID_X + (i % 3) * 18 + 1,
                            topPos  + CuttingBoardMenu.GRID_Y + (i / 3) * 18 + 1);
                }
            }
        }
    }

    private void renderGhostItem(GuiGraphics graphics,
                                 String itemId, int x, int y) {
        ItemStack ghost = new ItemStack(
                net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .get(net.minecraft.resources.ResourceLocation
                                .parse(itemId)));
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
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick) {
        // Advance cycling timer — always ticks, even when recipe selected
        // so it resumes smoothly on deselect
        indicatorTimer++;
        if (indicatorTimer >= CYCLE_FRAMES) {
            indicatorTimer = 0;
            showGroup1 = !showGroup1;
        }

        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        recipeBook.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= leftPos + BOOK_X
                && mouseX <= leftPos + BOOK_X + BOOK_TEX_W
                && mouseY >= topPos  + BOOK_Y
                && mouseY <= topPos  + BOOK_Y + BOOK_TEX_H) {
            recipeBook.toggleVisibility();
            this.leftPos = recipeBook.updateScreenPosition(
                    this.width, this.imageWidth);
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