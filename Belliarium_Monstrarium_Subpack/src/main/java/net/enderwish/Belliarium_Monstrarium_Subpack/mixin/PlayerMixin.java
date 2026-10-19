package net.enderwish.Belliarium_Monstrarium_Subpack.mixin;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackExtensionHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PlayerMixin
 *
 * Backpack special-slot hotbar extension (Unit C). Runs on BOTH sides
 * (common mixin list) -- the helper it calls is common code with no
 * client imports.
 *
 * getItemBySlot(MAINHAND): while the extension is active, the VIRTUAL HELD
 * stack (BackpackExtensionHelper.getExtensionStack -- one stable instance
 * seeded from the special slot; see that class) is handed out as the "held"
 * item instead of the vanilla selected hotbar slot's. Everything downstream
 * (use, attack, eat, first-person render, durability) then naturally
 * operates on that one instance because vanilla funnels it all through
 * this path -- including the item-use state machine, which silently stops
 * using the item if the hand item is not the same instance every call.
 *
 * setItemSlot(MAINHAND): while the extension is active, a mainhand write (a
 * drink finishing into a glass bottle, an item breaking) is redirected into
 * the virtual held stack, which write-throughs into the active special slot
 * on the live Curios stack. This is a raw write like vanilla's items.set --
 * onEquipItem semantics are deliberately bypassed.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(
            method = "getItemBySlot(Lnet/minecraft/world/entity/EquipmentSlot;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ghBelliarium$getItemBySlot(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        if (slot == EquipmentSlot.MAINHAND) {
            Player self = (Player) (Object) this;
            ItemStack extensionStack = BackpackExtensionHelper.getExtensionStack(self);
            if (!extensionStack.isEmpty()) {
                cir.setReturnValue(extensionStack);
            }
        }
    }

    @Inject(
            method = "setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ghBelliarium$setItemSlot(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        if (slot == EquipmentSlot.MAINHAND) {
            Player self = (Player) (Object) this;
            if (BackpackExtensionHelper.isActive(self)) {
                BackpackExtensionHelper.pushVirtual(self, stack);
                ci.cancel();
            }
        }
    }
}
