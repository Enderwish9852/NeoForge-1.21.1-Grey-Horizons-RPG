package net.enderwish.Belliarium_Monstrarium_Subpack.core.body;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.AdrenalineManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.PainkillerManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;

/**
 * BodyPartDebuffs
 *
 * Pure query helpers, no stored state -- always computed fresh from the
 * real BodyHealthCapability + current handedness. PainkillerManager and
 * AdrenalineManager both override EVERYTHING here to false -- single choke
 * point for "all debuffs suppressed," per both items' specs.
 */
public final class BodyPartDebuffs {
    private BodyPartDebuffs() {}

    private static boolean overridden(Player player) {
        return PainkillerManager.INSTANCE.isActive(player.getUUID())
                || AdrenalineManager.INSTANCE.isActive(player.getUUID());
    }

    private static boolean isLeftHand(Player player, InteractionHand hand) {
        HumanoidArm mainArm = player.getMainArm();
        boolean isMainHand = hand == InteractionHand.MAIN_HAND;
        return isMainHand ? mainArm == HumanoidArm.LEFT : mainArm == HumanoidArm.RIGHT;
    }

    public static boolean isHandLocked(Player player, InteractionHand hand) {
        if (overridden(player)) return false;
        BodyHealthCapability cap = player.getData(ModAttachments.BODY_HEALTH);
        return isLeftHand(player, hand) ? cap.isLeftArmDisabled() : cap.isRightArmDisabled();
    }

    public static boolean isHandRedTier(Player player, InteractionHand hand) {
        if (overridden(player)) return false;
        BodyHealthCapability cap = player.getData(ModAttachments.BODY_HEALTH);
        return isLeftHand(player, hand) ? cap.isLeftArmRed() : cap.isRightArmRed();
    }

    public static boolean isEitherLegGrey(Player player) {
        if (overridden(player)) return false;
        BodyHealthCapability cap = player.getData(ModAttachments.BODY_HEALTH);
        return cap.isLeftLegDisabled() || cap.isRightLegDisabled();
    }

    public static boolean isEitherLegRed(Player player) {
        if (overridden(player)) return false;
        BodyHealthCapability cap = player.getData(ModAttachments.BODY_HEALTH);
        return cap.isLeftLegRed() || cap.isRightLegRed();
    }
}
