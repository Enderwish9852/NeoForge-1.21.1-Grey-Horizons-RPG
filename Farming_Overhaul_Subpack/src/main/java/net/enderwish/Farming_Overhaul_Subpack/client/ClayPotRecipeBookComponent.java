package net.enderwish.Farming_Overhaul_Subpack.client;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.core.claypot.ClayPotRecipe;
import net.enderwish.Farming_Overhaul_Subpack.core.claypot.ClayPotRecipeRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class ClayPotRecipeBookComponent {

    private static final ResourceLocation RECIPE_BOOK_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    FarmingOverhaulSubpack.MODID, "textures/gui/recipe_book.png");

    // ── Exact vanilla constants ───────────────────────────────────────────────
    public static final int IMAGE_WIDTH  = 147;
    public static final int IMAGE_HEIGHT = 166;
    private static final int OFFSET_X   = 86;

    // ── Persistent state ──────────────────────────────────────────────────────
    private static boolean savedVisible    = false;
    private static int     savedPage       = 0;
    private static ClayPotRecipe.ClayPotCategory savedCategory = null;
    private static String  savedSearchText = "";

    // ── Instance state ────────────────────────────────────────────────────────
    private boolean visible;
    private int currentPage;
    private ClayPotRecipe.ClayPotCategory currentCategory;
    private String searchText;
    private ClayPotRecipe selectedRecipe = null;

    private int screenWidth  = 0;
    private int screenHeight = 0;

    private List<ClayPotRecipe> filteredRecipes = new ArrayList<>();
    private RecipeSelectedCallback onRecipeSelected;

    public interface RecipeSelectedCallback {
        void onSelected(ClayPotRecipe recipe);
    }

    // ── Panel position — exact vanilla formula ────────────────────────────────
    private int getPanelX() {
        int xOffset = visible ? OFFSET_X : 0;
        return (screenWidth - IMAGE_WIDTH) / 2 - xOffset;
    }

    private int getPanelY() {
        return (screenHeight - IMAGE_HEIGHT) / 2;
    }

    // ── Tab position — exact vanilla formula ──────────────────────────────────
    private int getTabX() {
        int xOffset = visible ? OFFSET_X : 0;
        return (screenWidth - IMAGE_WIDTH) / 2 - xOffset - 30;
    }

    private int getTabY() {
        return (screenHeight - IMAGE_HEIGHT) / 2 + 3;
    }

    // ── Main GUI leftPos — exact vanilla formula ──────────────────────────────
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
        this.screenWidth  = screenWidth;
        this.screenHeight = screenHeight;
        this.onRecipeSelected = callback;

        this.visible         = savedVisible;
        this.currentPage     = savedPage;
        this.currentCategory = savedCategory;
        this.searchText      = savedSearchText;

        refreshRecipes();
    }

    // ── Visibility ────────────────────────────────────────────────────────────
    public void toggleVisibility() {
        visible = !visible;
        savedVisible = visible;
        if (visible) refreshRecipes();
    }

    public boolean isVisible() { return visible; }

    // ── Recipe filtering ──────────────────────────────────────────────────────
    private void refreshRecipes() {
        List<ClayPotRecipe> all = currentCategory == null
                ? ClayPotRecipeRegistry.INSTANCE.getAllRecipes()
                : ClayPotRecipeRegistry.INSTANCE.getByCategory(currentCategory);

        if (searchText.isEmpty()) {
            filteredRecipes = new ArrayList<>(all);
        } else {
            filteredRecipes = new ArrayList<>();
            for (ClayPotRecipe recipe : all) {
                String name = recipe.getOutput().getHoverName()
                        .getString().toLowerCase();
                if (name.contains(searchText.toLowerCase())) {
                    filteredRecipes.add(recipe);
                }
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

        // Panel background — vanilla: blit(texture, i, j, 1, 1, 147, 166)
        graphics.blit(RECIPE_BOOK_TEXTURE, x, y, 1, 1, IMAGE_WIDTH, IMAGE_HEIGHT);

        renderCategoryTabs(graphics, mouseX, mouseY);

        // Search bar — vanilla EditBox at (i+25, j+13), size 81×14
        graphics.blit(RECIPE_BOOK_TEXTURE, x + 25, y + 13, 0, 166, 81, 14);
        String display = searchText.isEmpty() ? "Search..." : searchText;
        int textColor = searchText.isEmpty() ? 0x808080 : 0xFFFFFF;
        graphics.drawString(mc.font, display, x + 28, y + 16, textColor, false);

        renderRecipeGrid(graphics, mc, x, y, mouseX, mouseY);
        renderPageNavigation(graphics, mc, x, y, mouseX, mouseY);

        graphics.pose().popPose();
    }

    private void renderCategoryTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        ClayPotRecipe.ClayPotCategory[] categories =
                ClayPotRecipe.ClayPotCategory.values();

        for (int i = 0; i < categories.length; i++) {
            boolean sel = categories[i] == currentCategory;
            int tx = getTabX();
            int ty = getTabY() + i * 27;

            if (sel) {
                // Selected tab — top-left (188,2), size 35×26
                graphics.blit(RECIPE_BOOK_TEXTURE, tx, ty, 188, 2, 35, 26);
            } else {
                // Normal tab — top-left (153,2), size 30×26
                graphics.blit(RECIPE_BOOK_TEXTURE, tx, ty, 153, 2, 30, 26);
            }

            // Category first letter centered in tab
            String label = categories[i].name().substring(0, 1);
            int tabW = sel ? 35 : 30;
            int labelX = tx + tabW / 2 - Minecraft.getInstance().font.width(label) / 2;
            int labelY = ty + 13 - 4;
            graphics.drawString(Minecraft.getInstance().font,
                    label, labelX, labelY, 0xFFFFFF, false);
        }
    }

    private void renderRecipeGrid(GuiGraphics graphics, Minecraft mc,
                                  int x, int y, int mouseX, int mouseY) {
        int startIndex = currentPage * 20;

        for (int i = 0; i < 20; i++) {
            int recipeIndex = startIndex + i;
            if (recipeIndex >= filteredRecipes.size()) break;

            ClayPotRecipe recipe = filteredRecipes.get(recipeIndex);

            // Vanilla exact position: x+11 + 25*(i%5), y+31 + 25*(i/5)
            int bx = x + 11 + 25 * (i % 5);
            int by = y + 31 + 25 * (i / 5);

            boolean hovered = mouseX >= bx && mouseX < bx + 25
                    && mouseY >= by && mouseY < by + 25;
            boolean selected = recipe == selectedRecipe;

            // Normal slot (29,206), hovered/selected slot (54,206) — size 25×25
            int texX = (selected || hovered) ? 54 : 29;
            graphics.blit(RECIPE_BOOK_TEXTURE, bx, by, texX, 206, 25, 25);

            // Item icon — 16×16 centered in 25×25 slot = offset 4
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
            // Page label — vanilla: x - labelWidth/2 + 73, y+141
            String label = (currentPage + 1) + "/" + totalPages;
            int labelWidth = mc.font.width(label);
            graphics.drawString(mc.font, label,
                    x - labelWidth / 2 + 73, y + 141,
                    0xFFFFFF, false);
        }

        // Left/back arrow — vanilla position x+38, y+137, size 11×17
        // Normal: (15,208), Selected: (15,227)
        if (currentPage > 0) {
            boolean hov = mouseX >= x + 38 && mouseX < x + 49
                    && mouseY >= y + 137 && mouseY < y + 154;
            graphics.blit(RECIPE_BOOK_TEXTURE,
                    x + 38, y + 137,
                    15, hov ? 227 : 208,
                    11, 17);
        }

        // Right/forward arrow — vanilla position x+93, y+137, size 10×17
        // Normal: (1,208), Selected: (1,226)
        if (currentPage < totalPages - 1) {
            boolean hov = mouseX >= x + 93 && mouseX < x + 103
                    && mouseY >= y + 137 && mouseY < y + 154;
            graphics.blit(RECIPE_BOOK_TEXTURE,
                    x + 93, y + 137,
                    1, hov ? 226 : 208,
                    10, 17);
        }
    }

    // ── Mouse handling ────────────────────────────────────────────────────────
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;

        int mx = (int) mouseX;
        int my = (int) mouseY;
        int x = getPanelX();
        int y = getPanelY();

        // Category tabs — size 35×27
        ClayPotRecipe.ClayPotCategory[] categories =
                ClayPotRecipe.ClayPotCategory.values();
        for (int i = 0; i < categories.length; i++) {
            int tx = getTabX();
            int ty = getTabY() + i * 27;
            if (mx >= tx && mx < tx + 35 && my >= ty && my < ty + 27) {
                currentCategory = categories[i] == currentCategory
                        ? null : categories[i];
                savedCategory = currentCategory;
                currentPage = 0;
                savedPage = 0;
                refreshRecipes();
                return true;
            }
        }

        // Recipe grid — vanilla: x+11 + 25*(i%5), y+31 + 25*(i/5)
        int startIndex = currentPage * 20;
        for (int i = 0; i < 20; i++) {
            int recipeIndex = startIndex + i;
            if (recipeIndex >= filteredRecipes.size()) break;

            int bx = x + 11 + 25 * (i % 5);
            int by = y + 31 + 25 * (i / 5);

            if (mx >= bx && mx < bx + 25 && my >= by && my < by + 25) {
                selectedRecipe = filteredRecipes.get(recipeIndex);
                if (onRecipeSelected != null) {
                    onRecipeSelected.onSelected(selectedRecipe);
                }
                return true;
            }
        }

        int totalPages = Math.max(1,
                (int) Math.ceil(filteredRecipes.size() / 20.0f));

        // Left arrow — x+38, y+137, size 11×17
        if (currentPage > 0
                && mx >= x + 38 && mx < x + 49
                && my >= y + 137 && my < y + 154) {
            currentPage--;
            savedPage = currentPage;
            return true;
        }

        // Right arrow — x+93, y+137, size 10×17
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

    public ClayPotRecipe getSelectedRecipe() { return selectedRecipe; }
}
