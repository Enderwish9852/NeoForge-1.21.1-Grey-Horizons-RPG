package net.enderwish.Farming_Overhaul_Subpack.client;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.gui.ClayPotMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * ClayPotScreen
 *
 * Renders the clay pot GUI.
 * Uses the GUI texture from:
 *   assets/gh_farming_overhaul/textures/gui/clay_pot.png
 *
 * Layout (256x256 texture):
 *   - 3x3 ingredient grid at (62, 17)
 *   - Water slot at (26, 35)
 *   - Cook progress arrow at (116, 35)
 *   - Output slot at (152, 35)
 *   - Fire indicator at (62, 62)
 *   - Player inventory at (8, 84)
 *   - Player hotbar at (8, 142)
 */
public class ClayPotScreen extends AbstractContainerScreen<ClayPotMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            FarmingOverhaulSubpack.MODID, "textures/gui/clay_pot.png");

    // ── GUI dimensions ────────────────────────────────────────────────────────

    private static final int GUI_WIDTH  = 176;
    private static final int GUI_HEIGHT = 166;

    // ── Arrow animation ───────────────────────────────────────────────────────

    // Position of the arrow in the GUI
    private static final int ARROW_X      = 116;
    private static final int ARROW_Y      = 35;
    private static final int ARROW_WIDTH  = 24;
    private static final int ARROW_HEIGHT = 17;

    // Position of the arrow texture in the PNG (the filled arrow source)
    private static final int ARROW_TEX_X = 176;
    private static final int ARROW_TEX_Y = 14;

    // ── Fire indicator ────────────────────────────────────────────────────────

    private static final int FIRE_X      = 62;
    private static final int FIRE_Y      = 62;
    private static final int FIRE_WIDTH  = 14;
    private static final int FIRE_HEIGHT = 14;

    // Position of the lit fire texture in the PNG
    private static final int FIRE_TEX_X = 176;
    private static final int FIRE_TEX_Y = 0;

    // ── Water indicator ───────────────────────────────────────────────────────

    private static final int WATER_X      = 26;
    private static final int WATER_Y      = 35;
    private static final int WATER_WIDTH  = 16;
    private static final int WATER_HEIGHT = 16;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ClayPotScreen(ClayPotMenu menu, Inventory playerInventory,
                         Component title) {
        super(menu, playerInventory, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick,
                            int mouseX, int mouseY) {
        // Draw the main GUI background
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0,
                imageWidth, imageHeight);

        // Draw cook progress arrow
        renderArrow(graphics);

        // Draw fire indicator
        renderFire(graphics);
    }

    private void renderArrow(GuiGraphics graphics) {
        int progress   = menu.getCookProgress();
        int totalTime  = menu.getCookTotalTime();

        if (totalTime <= 0) return;

        // Scale arrow width to cook progress
        int arrowFill = (int) (ARROW_WIDTH * ((float) progress / totalTime));
        if (arrowFill <= 0) return;

        graphics.blit(TEXTURE,
                leftPos + ARROW_X,
                topPos  + ARROW_Y,
                ARROW_TEX_X,
                ARROW_TEX_Y,
                arrowFill,
                ARROW_HEIGHT);
    }

    private void renderFire(GuiGraphics graphics) {
        // Only show lit fire if campfire is lit
        // We detect this by checking if cook progress is advancing
        // (if campfire is out, progress won't move)
        // For now show fire indicator when water level > 0 or cooking
        boolean isLit = menu.getCookTotalTime() > 0;
        if (!isLit) return;

        graphics.blit(TEXTURE,
                leftPos + FIRE_X,
                topPos  + FIRE_Y,
                FIRE_TEX_X,
                FIRE_TEX_Y,
                FIRE_WIDTH,
                FIRE_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Draw title
        graphics.drawString(this.font, this.title,
                this.titleLabelX, this.titleLabelY, 0x404040, false);

        // Draw "Inventory" label
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);

        // Draw bowls remaining if output is ready
        int bowls = menu.getBowlsRemaining();
        if (bowls > 0) {
            graphics.drawString(this.font,
                    Component.literal(bowls + " bowls"),
                    ARROW_X + ARROW_WIDTH + 4,
                    ARROW_Y + 4,
                    0x404040, false);
        }

        // Draw water level indicator
        int water = menu.getWaterLevel();
        if (water > 0) {
            graphics.drawString(this.font,
                    Component.literal("~"),
                    WATER_X + 2,
                    WATER_Y + 4,
                    0x3366FF, false);
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
