package net.enderwish.Atmospheric_Overhaul_Subpack.mixin;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.water.WaterReflectionRenderer;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MinecraftShaderTransparencyMixin
 *
 * The water-mask pass re-renders the translucent section layer into
 * the mod's OWN scratch target (WaterReflectionRenderer.maskTarget).
 *
 * Problem: RenderType.translucent() carries the TRANSLUCENT_TARGET
 * output state, whose setupRenderState() binds the GAME's translucent
 * target whenever shader transparency is on (Fabulous graphics). That
 * would steal the mask pass away from our target: the mask would stay
 * empty (no reflections at all) and the game's translucent target
 * would receive mask-mode geometry.
 *
 * The fix: report "no shader transparency" for the duration of the
 * mask pass only. The output state then does not bind anything, so the
 * already-bound mask target keeps the geometry -- exactly like the
 * Fast/Fancy path, where the output state is inert.
 *
 * The flag is only true inside the mask pass, so the normal render
 * path is untouched.
 */
@Mixin(Minecraft.class)
public class MinecraftShaderTransparencyMixin {

    @Inject(method = "useShaderTransparency", at = @At("HEAD"), cancellable = true)
    private static void ghWaterMaskPassKeepsOwnTarget(CallbackInfoReturnable<Boolean> cir) {
        if (WaterReflectionRenderer.isMaskPassActive()) {
            cir.setReturnValue(false);
        }
    }
}
