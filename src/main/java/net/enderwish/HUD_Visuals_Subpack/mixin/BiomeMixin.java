package net.enderwish.HUD_Visuals_Subpack.mixin;

import net.enderwish.HUD_Visuals_Subpack.client.ClientColorHandler;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to intercept biome color calculations.
 * This connects the Minecraft rendering engine to our seasonal color logic.
 * It works by intercepting the final calculated color of a biome and
 * passing it through our seasonal math filters.
 */
@Mixin(Biome.class)
public class BiomeMixin {

    /**
     * Intercepts grass color requests.
     * Grass colors are often calculated based on X/Z coordinates for smooth transitions.
     */
    @Inject(method = "getGrassColor", at = @At("RETURN"), cancellable = true)
    private void onGetGrassColor(double posX, double posZ, CallbackInfoReturnable<Integer> cir) {
        int originalColor = cir.getReturnValue();

        // Apply seasonal transformation to the grass color
        int seasonalColor = ClientColorHandler.modifyGrassColor(originalColor);

        // Overwrite the return value with our new color
        cir.setReturnValue(seasonalColor);
    }

    /**
     * Intercepts foliage (leaves) color requests.
     * Foliage usually handles oak and birch leaves specifically.
     */
    @Inject(method = "getFoliageColor", at = @At("RETURN"), cancellable = true)
    private void onGetFoliageColor(CallbackInfoReturnable<Integer> cir) {
        int originalColor = cir.getReturnValue();

        // Apply seasonal transformation to the foliage color
        int seasonalColor = ClientColorHandler.modifyFoliageColor(originalColor);

        // Overwrite the return value with our new color
        cir.setReturnValue(seasonalColor);
    }
}