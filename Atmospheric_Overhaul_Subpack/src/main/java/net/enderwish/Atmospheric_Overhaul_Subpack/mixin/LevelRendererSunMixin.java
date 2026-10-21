package net.enderwish.Atmospheric_Overhaul_Subpack.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.enderwish.Atmospheric_Overhaul_Subpack.client.sky.SunRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * LevelRendererSunMixin
 *
 * NEW -- swaps vanilla's square sun for the mod's realistic one.
 *
 * Vanilla's sun is a 60x60 world quad at distance 100, textured with a
 * 14px white square inside a 32px texture: a hard-edged WHITE SQUARE
 * about 15 degrees across. The mod draws a round, glowing sun instead
 * (SunRenderer), so the vanilla one has to go.
 *
 * Two hooks:
 *
 * 1. @Redirect on RenderSystem.setShaderTexture inside renderSky,
 *    ordinal 0 -- the FIRST setShaderTexture call in that method is
 *    the sun's (the second is the moon's), so the sun's texture is
 *    swapped for a fully transparent 1x1. The vanilla quad still gets
 *    drawn, but with alpha 0 and the sky's additive-ish blend it
 *    contributes nothing at all.
 *
 * 2. @Inject at TAIL of renderSky -- draws the mod's sun. TAIL is
 *    after the sky gradient, the sunrise glow, the moon and the stars,
 *    but still before the terrain, so the chunks simply cover the sun
 *    where it should be hidden.
 *
 * The TAIL inject is skipped on the End dimension (renderSky returns
 * early there) and SunRenderer re-checks the blindness / darkness /
 * powder-snow / fog guards vanilla uses.
 */
@Mixin(LevelRenderer.class)
public class LevelRendererSunMixin {

    /** A 1x1 fully transparent PNG: the vanilla sun's texture. */
    private static final ResourceLocation BLANK =
            ResourceLocation.fromNamespaceAndPath(AtmosphericOverhaulSubpack.MOD_ID, "textures/util/blank.png");

    @Redirect(
            method = "renderSky",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V",
                    ordinal = 0))
    private void gh_atmospheric$blankVanillaSun(int slot, ResourceLocation texture) {
        RenderSystem.setShaderTexture(slot, BLANK);
    }

    /**
     * The descriptor is written out in full, so the injection is
     * pinned to this exact overload of renderSky.
     *
     * Note the "FLnet/minecraft/client/Camera;": the float's own
     * descriptor F is followed by the object's L...; form. Writing
     * "Fnet/minecraft/client/Camera;" instead (the L missing) looks
     * plausible but is a malformed descriptor, and Mixin rejects it
     * with "has invalid target descriptor" before the game even
     * starts.
     */
    @Inject(
            method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V",
            at = @At("TAIL"))
    private void gh_atmospheric$drawRealisticSun(Matrix4f frustumMatrix, Matrix4f projectionMatrix,
                                                 float partialTick, Camera camera, boolean isFoggy,
                                                 Runnable skyFogSetup, CallbackInfo ci) {
        SunRenderer.render(frustumMatrix, projectionMatrix, partialTick, camera, isFoggy);
    }
}
