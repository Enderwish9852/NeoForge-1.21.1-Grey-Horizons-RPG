package net.enderwish.Belliarium_Monstrarium_Subpack.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * ItemEntityRendererMixin
 *
 * BUGFIX (still spinning / still hovering) -- flattening confirmed correct
 * (no complaint about upside-down), so no sign-flip needed there.
 *
 * ghBelliarium$layFlatNoSpin now DISCARDS vanilla's spin rotation entirely
 * (only the flattening tip is applied) -- previously I deliberately kept
 * vanilla's spin on top, which is exactly the spinning you saw.
 *
 * ghBelliarium$noHover redirects the translate call that applies the idle
 * vertical bob, replacing vanilla's animated (bob + scale-based) offset
 * with one small fixed ground clearance -- static, no hover.
 */
@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V")
    )
    private void ghBelliarium$noHover(PoseStack poseStack, float x, float y, float z) {
        poseStack.translate(x, 0.03F, z); // THE FIX -- fixed clearance, no bob
    }

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V")
    )
    private void ghBelliarium$layFlatNoSpin(PoseStack poseStack, Quaternionf originalSpin) {
        // THE FIX -- originalSpin is intentionally never applied; flat and static.
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
    }
}
