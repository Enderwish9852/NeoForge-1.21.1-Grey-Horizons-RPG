package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.SurvivalCapability;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;


/**
 * EnergyFromFoodHandler
 *
 * VERIFY IF COMPILE FAILS -- ItemUseEvent isn't confirmed source in this
 * conversation and vanilla food's own "finish eating" hook has moved
 * between events across MC/NeoForge versions. The RELIABLE alternative if
 * this doesn't compile: check vanilla hunger level each PlayerTickEvent
 * and add energy proportional to any INCREASE since last tick -- less
 * elegant but needs no uncertain event class at all.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class EnergyFromFoodHandler {

    private static final float ENERGY_PER_HUNGER_POINT = 5.0f;

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;

        FoodProperties food = event.getItem().getItem()
                .getFoodProperties(event.getItem(), player);

        if (food == null) return;

        SurvivalCapability cap = player.getData(ModAttachments.SURVIVAL);
        cap.setEnergy(cap.getEnergy() + ENERGY_PER_HUNGER_POINT);
    }
}
