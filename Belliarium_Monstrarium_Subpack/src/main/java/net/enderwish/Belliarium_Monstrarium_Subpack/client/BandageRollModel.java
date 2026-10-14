package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.medical.BandageItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * BandageRollModel + renderer — first-person in-hand bandage roll.
 *
 * The geo model replaces the flat item sprite in hand; the "spin" one-shot
 * animation makes the roll rotate while the player holds right-click
 * wrapping the bandage. Triggered from MedicalAnimClient while the
 * local player is using the item.
 */
public class BandageRollModel extends GeoModel<BandageItem> {

    @Override
    public ResourceLocation getModelResource(BandageItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                BelliariumMonstrariumSubpack.MODID, "geo/bandage_roll.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BandageItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                BelliariumMonstrariumSubpack.MODID, "textures/item/bandage.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BandageItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(
                BelliariumMonstrariumSubpack.MODID, "animations/bandage_roll.animation.json");
    }
}
