package net.enderwish.Belliarium_Monstrarium_Subpack.core.medical;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyHealthCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyPart;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyPartTier;

/**
 * TargetedMedItem
 *
 * Implemented by Bandage, Suture Kit, and Splint. MedTargetScreen /
 * MedApplyPacket / FirstAidTriage all go through this one interface
 * instead of knowing each item class by name -- adding a future tier-
 * item later just means implementing this, nothing else needs touching.
 */
public interface TargetedMedItem {

    BodyPartTier treatsTier();

    /** Splint overrides this to exclude Head/Torso -- see SplintItem. */
    default boolean canTreat(BodyPart part) {
        return true;
    }

    float healTargetFraction();

    default void applyTreatment(BodyHealthCapability cap, BodyPart part) {
        part.healToFraction(cap, healTargetFraction());
    }
}
