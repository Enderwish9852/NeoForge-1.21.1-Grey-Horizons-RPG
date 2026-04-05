package net.enderwish.HUD_Visuals_Subpack.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

public class HealthRegenEvents {

    @SubscribeEvent
    public void onLivingHeal(LivingHealEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCanceled(true);
        }
    }
}