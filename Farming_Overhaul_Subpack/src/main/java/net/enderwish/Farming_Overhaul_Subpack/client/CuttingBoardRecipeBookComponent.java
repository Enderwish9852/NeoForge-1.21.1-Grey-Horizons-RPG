package net.enderwish.Farming_Overhaul_Subpack.client;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.core.cutting_board.CuttingBoardRecipe;
import net.enderwish.Farming_Overhaul_Subpack.core.cutting_board.CuttingBoardRecipeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class CuttingBoardRecipeBookComponent {

    private static final ResourceLocation RECIPE_BOOK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    FarmingOverhaulSubpack.MODID,
                    "textures/gui/recipe_book.png");

    public static final int IMAGE_WIDTH  = 147;
    public static final int IMAGE_HEIGHT = 166;
    private static final int OFFSET_X   = 86;

    // ── Persistent state ──────────────────────────────────────────────────────
    private static boolean savedVisible    = false;
    private static int     savedPage       = 0;
    private static String  savedSearchText = "";
    private static CuttingBoardRecipe.CBCategory savedCategory = null;

    // ── Instance state ────────────────────────────────────────────────────────
    private boolean visible;
    private int currentPage;
    private String searchText;
    private CuttingBoardRecipe.CBCategory currentCategory;
    private CuttingBoardRecipe selectedRecipe = null;

    private int screenWidth  = 0;
    private int screenHeight = 0;

    private List<CuttingBoardRecipe> filteredRecipes = new ArrayList<>();
    private RecipeSelectedCallback onRecipeSelected;

    public interface RecipeSelectedCallback {
        void onSelected(CuttingBoardRecipe recipe);
    }

    // ── Panel position ────────────────────────────────────────────────────────
    private int getPanelX() {
        int xOffset = visible ? OFFSET_X : 0;
        return (screenWidth - IMAGE_WIDTH) / 2 - xOffset;
    }

    private int getPanelY() {
        return (screenHeight - IMAGE_HEIGHT) / 2;
    }

    public int updateScreenPosition(int screenWidth, int imageWidth) {
        if (visible) {
            return 177 + (screenWidth - imageWidth - 200) / 2;
        } else {
            return (screenWidth - imageWidth) / 2;
        }
    }

    // ── Init ──────────────────────────────────────────────────────────────────
    public void init(int screenWidth, int screenHeight,
                     RecipeSelectedCallback callback) {
        this.screenWidth      = screenWidth;
        this.screenHeight     = screenHeight;
        this.onRecipeSelected = callback;
        this.visible          = savedVisible;
        this.currentPage      = savedPage;
        this.searchText       = savedSearchText;
        this.currentCategory  = savedCategory;
        refreshRecipes();
    }

    public void toggleVisibility() {
        visible = !visible;
        savedVisible = visible;
        if (visible) refreshRecipes();
    }

    public boolean isVisible() { return visible; }

    // ── Recipe filtering ──────────────────────────────────────────────────────
    private void refreshRecipes() {
        List<CuttingBoardRecipe> all = currentCategory == null
                ? CuttingBoardRecipeRegistry.INSTANCE.getAllRecipes()
                : CuttingBoardRecipeRegistry.INSTANCE.getByCategory(currentCategory);

        if (searchText.isEmpty()) {
            filteredRecipes = new ArrayList<>(all);
        } else {
            filteredRecipes = new ArrayList<>();
            for (CuttingBoardRecipe recipe : all) {
                String name = recipe.getOutput().getHoverName()
                        .getString().toLowerCase();
                if (name.contains(searchText.toLowerCase()))
                    filteredRecipes.add(recipe);
            }
        }

        int maxPage = Math.max(0, (filteredRecipes.size() - 1) / 20);
        if (currentPage > maxPage) currentPage = 0;
        savedPage = currentPage;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────
    public void render(GuiGraphics graphics, int mouseX, int mouseY,
                       float partialTick) {
        if (!visible) return;

        Minecraft mc = Minecraft.getInstance();
        int x = getPanelX();
        int y = getPanelY();

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 100.0F);

        // Panel background
        graphics.blit(RECIPE_BOOK_TEXTURE, x, y, 1, 1,
                IMAGE_WIDTH, IMAGE_HEIGHT);

        // Category tabs
        renderCategoryTabs(graphics, mc, x, y, mouseX, mouseY);

        // Search bar
        graphics.blit(RECIPE_BOOK_TEXTURE, x + 25, y + 13, 0, 166, 81, 14);
        String display = searchText.isEmpty() ? "Search..." : searchText;
        int textColor = searchText.isEmpty() ? 0x808080 : 0xFFFFFF;
        graphics.drawString(mc.font, display, x + 28, y + 16, textColor, false);

        renderRecipeGrid(graphics, mc, x, y, mouseX, mouseY);
        renderPageNavigation(graphics, mc, x, y, mouseX, mouseY);

        graphics.pose().popPose();
    }

    private void renderCategoryTabs(GuiGraphics graphics, Minecraft mc,
                                    int x, int y, int mouseX, int mouseY) {
        CuttingBoardRecipe.CBCategory[] cats =
                CuttingBoardRecipe.CBCategory.values();

        for (int i = 0; i < cats.length; i++) {
            boolean selected = cats[i] == currentCategory;
            int tx = x - 30;
            int ty = y + 3 + i * 27;

            // Tab background — use recipe_book.png tab sprites
            // Selected tab: (153,2)-(183,28), normal: same offset
            int tabTexX = selected ? 188 : 153;
            graphics.blit(RECIPE_BOOK_TEXTURE, tx, ty, tabTexX, 2, 30, 26);

            // Category label
            graphics.drawString(mc.font,
                    cats[i].displayName().substring(0, 1),
                    tx + 10, ty + 8,
                    selected ? 0xFFFFFF : 0xAAAAAA, false);

            // Tooltip on hover
            if (mouseX >= tx && mouseX < tx + 30
                    && mouseY >= ty && mouseY < ty + 26) {
                graphics.renderTooltip(mc.font,
                        Component.literal(cats[i].displayName()),
                        mouseX, mouseY);
            }
        }
    }

    private void renderRecipeGrid(GuiGraphics graphics, Minecraft mc,
                                  int x, int y, int mouseX, int mouseY) {
        int startIndex = currentPage * 20;

        for (int i = 0; i < 20; i++) {
            int recipeIndex = startIndex + i;
            if (recipeIndex >= filteredRecipes.size()) break;

            CuttingBoardRecipe recipe = filteredRecipes.get(recipeIndex);
            int bx = x + 11 + 25 * (i % 5);
            int by = y + 31 + 25 * (i / 5);

            boolean hovered  = mouseX >= bx && mouseX < bx + 25
                    && mouseY >= by && mouseY < by + 25;
            boolean selected = recipe == selectedRecipe;

            int texX = (selected || hovered) ? 54 : 29;
            graphics.blit(RECIPE_BOOK_TEXTURE, bx, by, texX, 206, 25, 25);
            graphics.renderItem(recipe.getOutput(), bx + 4, by + 4);

            if (hovered) {
                graphics.renderTooltip(mc.font,
                        Component.literal(recipe.getOutput()
                                .getHoverName().getString()),
                        mouseX, mouseY);
            }
        }
    }

    private void renderPageNavigation(GuiGraphics graphics, Minecraft mc,
                                      int x, int y, int mouseX, int mouseY) {
        int totalPages = Math.max(1,
                (int) Math.ceil(filteredRecipes.size() / 20.0f));

        if (totalPages > 1) {
            String label = (currentPage + 1) + "/" + totalPages;
            int labelWidth = mc.font.width(label);
            graphics.drawString(mc.font, label,
                    x - labelWidth / 2 + 73, y + 141,
                    0xFFFFFF, false);
        }

        if (currentPage > 0) {
            boolean hov = mouseX >= x + 38 && mouseX < x + 49
                    && mouseY >= y + 137 && mouseY < y + 154;
            graphics.blit(RECIPE_BOOK_TEXTURE,
                    x + 38, y + 137, 15, hov ? 227 : 208, 11, 17);
        }

        if (currentPage < totalPages - 1) {
            boolean hov = mouseX >= x + 93 && mouseX < x + 103
                    && mouseY >= y + 137 && mouseY < y + 154;
            graphics.blit(RECIPE_BOOK_TEXTURE,
                    x + 93, y + 137, 1, hov ? 226 : 208, 10, 17);
        }
    }

    // ── Mouse handling ────────────────────────────────────────────────────────
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        int mx = (int) mouseX;
        int my = (int) mouseY;
        int x = getPanelX();
        int y = getPanelY();

        // Category tabs
        CuttingBoardRecipe.CBCategory[] cats =
                CuttingBoardRecipe.CBCategory.values();
        for (int i = 0; i < cats.length; i++) {
            int tx = x - 30;
            int ty = y + 3 + i * 27;
            if (mx >= tx && mx < tx + 30 && my >= ty && my < ty + 26) {
                currentCategory = (cats[i] == currentCategory) ? null : cats[i];
                savedCategory   = currentCategory;
                currentPage     = 0;
                savedPage       = 0;
                refreshRecipes();
                return true;
            }
        }

        // Recipe grid
        int startIndex = currentPage * 20;
        for (int i = 0; i < 20; i++) {
            int recipeIndex = startIndex + i;
            if (recipeIndex >= filteredRecipes.size()) break;
            int bx = x + 11 + 25 * (i % 5);
            int by = y + 31 + 25 * (i / 5);
            if (mx >= bx && mx < bx + 25 && my >= by && my < by + 25) {
                selectedRecipe = filteredRecipes.get(recipeIndex);
                if (onRecipeSelected != null)
                    onRecipeSelected.onSelected(selectedRecipe);
                return true;
            }
        }

        int totalPages = Math.max(1,
                (int) Math.ceil(filteredRecipes.size() / 20.0f));

        if (currentPage > 0
                && mx >= x + 38 && mx < x + 49
                && my >= y + 137 && my < y + 154) {
            currentPage--;
            savedPage = currentPage;
            return true;
        }

        if (currentPage < totalPages - 1
                && mx >= x + 93 && mx < x + 103
                && my >= y + 137 && my < y + 154) {
            currentPage++;
            savedPage = currentPage;
            return true;
        }

        return false;
    }

    // ── Keyboard handling ─────────────────────────────────────────────────────
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;
        if (keyCode == 259 && !searchText.isEmpty()) {
            searchText = searchText.substring(0, searchText.length() - 1);
            savedSearchText = searchText;
            refreshRecipes();
            return true;
        }
        return false;
    }

    public boolean charTyped(char c, int modifiers) {
        if (!visible) return false;
        if (searchText.length() < 50) {
            searchText += c;
            savedSearchText = searchText;
            refreshRecipes();
            return true;
        }
        return false;
    }

    public CuttingBoardRecipe getSelectedRecipe() { return selectedRecipe; }
}