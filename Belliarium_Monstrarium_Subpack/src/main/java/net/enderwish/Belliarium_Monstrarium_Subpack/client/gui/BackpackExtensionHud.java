package net.enderwish.Belliarium_Monstrarium_Subpack.client.gui;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackConfig;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackExtensionHelper;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackHandler;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.gear.BackpackItem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * BackpackExtensionHud
 *
 * Draws the two virtual hotbar slots at either end of the vanilla hotbar
 * when the equipped backpack declares special slots and the slot holds an
 * item. The frames use the VANILLA hotbar texture itself — one 20x20 cell
 * of hud/hotbar.png (cell 0 on the left, cell 8 on the right) — drawn
 * flush against the hotbar ends. The cell row sits at guiHeight - 21,
 * one pixel below the sprite's 1px top border — exactly where the
 * vanilla hotbar's own cells sit inside the 22px sprite (1px border +
 * 20px cells + 1px border), so the extension reads as part of the same
 * hotbar. The item sits at +2, +2 (vanilla's hotbar_y + 3 item row) and
 * the selection sprite at -2, -2 (vanilla's hotbar_y - 1 row); the
 * GuiMixin suppresses the vanilla selection while active.
 */
public class BackpackExtensionHud {

    public static final LayeredDraw.Layer LAYER = BackpackExtensionHud::render;

    /** The vanilla hotbar sprite file: 182x22, nine 20x20 cells with 1px borders. */
    private static final ResourceLocation HOTBAR_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/sprites/hud/hotbar.png");
    private static final ResourceLocation HOTBAR_SELECTION_SPRITE =
            ResourceLocation.withDefaultNamespace("hud/hotbar_selection");

    private static final int CELL_SIZE = 20;
    private static final int HOTBAR_TEXTURE_W = 182;
    private static final int HOTBAR_TEXTURE_H = 22;
    private static final float CELL_U_LEFT = 1.0f;    // cell 0
    private static final float CELL_U_RIGHT = 161.0f; // cell 8 (1 + 8*20)
    private static final float CELL_V = 1.0f;
    private static final int CELL_X_LEFT = -111; // width/2 - 91 - 20 (flush left)
    private static final int CELL_X_RIGHT = 91;  // width/2 + 91 (flush right)
    private static final int CELL_Y_FROM_BOTTOM = 21;

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || !player.isAlive()) return;

        ItemStack backpack = BackpackHandler.getEquipped(player);
        if (backpack.isEmpty() || !(backpack.getItem() instanceof BackpackItem item)) return;

        BackpackConfig config = item.config(backpack);
        if (config.specialLeft() == null && config.specialRight() == null) return;

        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();
        int y = height - CELL_Y_FROM_BOTTOM;
        byte active = BackpackExtensionHelper.side(player);

        if (config.specialLeft() != null) {
            drawSlot(graphics, player, width / 2 + CELL_X_LEFT, y,
                    CELL_U_LEFT, BackpackExtensionHelper.LEFT, active);
        }
        if (config.specialRight() != null) {
            drawSlot(graphics, player, width / 2 + CELL_X_RIGHT, y,
                    CELL_U_RIGHT, BackpackExtensionHelper.RIGHT, active);
        }
    }

    /** Draws the slot frame + its item; the frame only when the slot holds an item. */
    private static void drawSlot(GuiGraphics graphics, Player player, int x, int y,
                                 float u, byte side, byte active) {
        ItemStack stack = BackpackExtensionHelper.getSpecialStack(player, side);
        if (stack.isEmpty()) return;

        graphics.blit(HOTBAR_TEXTURE, x, y, 0, u, CELL_V,
                CELL_SIZE, CELL_SIZE, HOTBAR_TEXTURE_W, HOTBAR_TEXTURE_H);
        graphics.renderItem(stack, x + 2, y + 2);
        if (active == side) {
            graphics.blitSprite(HOTBAR_SELECTION_SPRITE, x - 2, y - 2, 24, 23);
        }
    }
}
