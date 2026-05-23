package net.enderwish.Farming_Overhaul_Subpack.mixin;

import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.ModDataComponents;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.SpoilageComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ItemInHandRendererMixin
 *
 * Injects into ClientHooks.shouldCauseReequipAnimation() to suppress
 * the held item animation when only the SpoilageComponent changed.
 *
 * Without this fix, updating the spoilage data component on the
 * held item every tick causes the hand to "jump" as if a new
 * item was equipped.
 */
@Mixin(value = ClientHooks.class, remap = false)
public class ItemInHandRendererMixin {

    @Inject(
            method = "shouldCauseReequipAnimation",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void onShouldCauseReequipAnimation(ItemStack from, ItemStack to, int slot,
                                                      CallbackInfoReturnable<Boolean> cir) {
        if (from.isEmpty() || to.isEmpty()) return;
        if (!from.getItem().equals(to.getItem())) return;
        if (from.getCount() != to.getCount()) return;

        // Check if the only difference is the spoilage component
        SpoilageComponent comp1 = from.get(ModDataComponents.SPOILAGE.get());
        SpoilageComponent comp2 = to.get(ModDataComponents.SPOILAGE.get());

        // Neither has spoilage — not our concern
        if (comp1 == null && comp2 == null) return;

        // One has spoilage the other doesn't — genuinely different
        if (comp1 == null || comp2 == null) return;

        // Only spoil progress differs — suppress animation
        if (comp1.maxSpoilTicks() == comp2.maxSpoilTicks()) {
            cir.setReturnValue(false);
        }
    }
}