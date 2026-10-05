package net.enderwish.Belliarium_Monstrarium_Subpack.mixin;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ItemInHandRendererMixin
 *
 * NEW -- same fix, same target method, as Farming_Overhaul_Subpack's own
 * ItemInHandRendererMixin (confirmed working there for SpoilageComponent).
 * LEARNING_PROGRESS changes every tick while a skill book is being read,
 * which vanilla's re-equip check treats as "a different item" -- this
 * suppresses that specifically when the SKILL_ID is unchanged (same book,
 * only its progress ticked).
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

        String skillId1 = from.get(ModDataComponents.SKILL_ID.get());
        String skillId2 = to.get(ModDataComponents.SKILL_ID.get());

        if (skillId1 == null && skillId2 == null) return; // not a skill book
        if (skillId1 == null || skillId2 == null) return; // one side missing -- genuinely different
        if (!skillId1.equals(skillId2)) return; // different skill entirely

        cir.setReturnValue(false); // same book, only progress ticked -- suppress
    }
}
