package net.enderwish.Atmospheric_Overhaul_Subpack.mixin;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class WeatherTickMixin {

    @Inject(method = "tickWeather", at = @At("HEAD"), cancellable = true)
    private void onTickWeather(CallbackInfo ci) {
        // Cancel vanilla weather ticking — GH seasons handles all weather
        ci.cancel();
    }
}
