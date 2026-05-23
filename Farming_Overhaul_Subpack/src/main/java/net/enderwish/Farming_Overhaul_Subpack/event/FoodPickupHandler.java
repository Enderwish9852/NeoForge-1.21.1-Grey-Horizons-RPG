package net.enderwish.Farming_Overhaul_Subpack.event;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.core.crop.CropRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.food.FoodDefinition;
import net.enderwish.Farming_Overhaul_Subpack.core.food.FoodRegistry;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.ModDataComponents;
import net.enderwish.Farming_Overhaul_Subpack.core.spoilage.SpoilageComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = FarmingOverhaulSubpack.MODID, bus = EventBusSubscriber.Bus.GAME)
public class FoodPickupHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("GHFarming");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();

        if (player.level().isClientSide()) return;
        if (player.tickCount % 10 != 0) return;

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.has(ModDataComponents.SPOILAGE.get())) continue;

            String itemId = stack.getItem()
                    .builtInRegistryHolder()
                    .key().location().getPath();

            // Debug meat items
            if (itemId.contains("beef") || itemId.contains("pork") || itemId.contains("chicken")) {
                LOGGER.info("Checking meat: {} | loaded: {} | registered: {}",
                        itemId, FoodRegistry.INSTANCE.isLoaded(), FoodRegistry.INSTANCE.isRegistered(itemId));
            }

            if (FoodRegistry.INSTANCE.isRegistered(itemId)) {
                FoodDefinition def = FoodRegistry.INSTANCE.getByName(itemId);
                stack.set(
                        ModDataComponents.SPOILAGE.get(),
                        SpoilageComponent.fresh(def.spoilTicks())
                );
                continue;
            }

            if (CropRegistry.INSTANCE.isRegistered(itemId)) {
                int spoilTicks = CropRegistry.INSTANCE.getByName(itemId).spoilTicks();
                stack.set(
                        ModDataComponents.SPOILAGE.get(),
                        SpoilageComponent.fresh(spoilTicks)
                );
            }
        }
    }
}