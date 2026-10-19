package net.enderwish.Belliarium_Monstrarium_Subpack.event;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * BackpackSpeedHandler
 *
 * Applies each backpack's speedBonusPct as a flat multiplicative movement
 * bonus while the bag is equipped. Remove-then-add every tick (the same
 * pattern as BodyPartDebuffHandler's leg modifiers) so the modifier can
 * never linger after the bag is unequipped, and so config edits via
 * JSON reload apply on the next tick without a re-login.
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID)
public class BackpackSpeedHandler {

    private static final ResourceLocation SPEED_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "backpack_speed_bonus");

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (player.isCreative() || player.isSpectator()) return;

        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;

        speed.removeModifier(SPEED_BONUS_ID);

        ItemStack bp = BackpackHandler.getEquipped(player);
        float bonus = BackpackHandler.configOf(bp).speedBonusPct();
        if (bonus > 0f) {
            speed.addPermanentModifier(new AttributeModifier(
                    SPEED_BONUS_ID, bonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }
}
