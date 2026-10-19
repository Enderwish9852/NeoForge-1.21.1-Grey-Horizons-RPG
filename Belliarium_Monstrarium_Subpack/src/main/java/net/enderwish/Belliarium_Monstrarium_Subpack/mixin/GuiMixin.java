package net.enderwish.Belliarium_Monstrarium_Subpack.mixin;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackExtensionHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * GuiMixin
 *
 * Suppresses the vanilla hotbar selection-highlight blit when the local
 * player is holding a backpack extension: the extension's own HUD
 * (BackpackExtensionHud) draws a selection highlight at either end of the
 * hotbar in that case. The redirect is scoped to the single 5-arg
 * blitSprite calls inside Gui#renderItemHotbar; non-selection sprites are
 * passed through untouched so the rest of the hotbar still renders.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {

    private static final ResourceLocation HOTBAR_SELECTION_SPRITE =
            ResourceLocation.withDefaultNamespace("hud/hotbar_selection");

    @Redirect(
            method = "renderItemHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"
            )
    )
    private void ghBelliarium$redirectHotbarBlit(GuiGraphics graphics, ResourceLocation sprite,
                                                 int x, int y, int width, int height) {
        Player player = Minecraft.getInstance().player;
        if (player != null && BackpackExtensionHelper.isActive(player)
                && sprite.equals(HOTBAR_SELECTION_SPRITE)) {
            return; // suppressed -- the extension HUD draws its own highlight
        }
        graphics.blitSprite(sprite, x, y, width, height);
    }
}
