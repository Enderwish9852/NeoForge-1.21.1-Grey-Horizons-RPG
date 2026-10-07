package net.enderwish.Belliarium_Monstrarium_Subpack.core.medical;

import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModDataComponents;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyHealthCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyPart;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyPartTier;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.medical.FirstAidKitItem;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.medical.PainkillerItem;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.BodyHealthSyncPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * FirstAidTriage
 *
 * The "fast use" logic, per your spec:
 *   1. Painkillers applied ONCE, first, only if the player currently has
 *      a RED or GREY part AND isn't already under Painkiller/Adrenaline
 *      cover.
 *   2. Head/Torso treated first, worse of the two first.
 *   3. All 6 subsystem parts, worst tier across the whole group first.
 *
 * One press applies EVERY treatment the kit's contents currently allow in
 * that pass, not just one item -- a judgment call since your spec didn't
 * say "one item" vs "everything useful." Each part gets at most ONE
 * treatment per press even if a second tier-up item is also available
 * (e.g. a Grey arm with both Splint and Suture Kit in the kit only gets
 * Splinted this press, not Splinted-then-Sutured in one go) -- keeping
 * the tier chain meaningful rather than letting one press fully heal a
 * part instantly. Flag it if you want chained full-healing per press
 * instead; it's a small change.
 */
public final class FirstAidTriage {
    private FirstAidTriage() {}

    public static void performFastUse(ServerPlayer player, InteractionHand kitHand) {
        ItemStack kitStack = player.getItemInHand(kitHand);
        if (!(kitStack.getItem() instanceof FirstAidKitItem)) return;

        FirstAidKitContents contents = kitStack.getOrDefault(
                ModDataComponents.FIRST_AID_KIT_CONTENTS.get(), FirstAidKitContents.empty());
        BodyHealthCapability cap = player.getData(ModAttachments.BODY_HEALTH);

        int treatmentsApplied = 0;

        if (hasAnyDebuffCondition(cap) && !PainkillerManager.INSTANCE.isActive(player.getUUID())) {
            int painkillerSlot = findFirstSlotOf(contents, PainkillerItem.class);
            if (painkillerSlot >= 0) {
                PainkillerManager.INSTANCE.activate(player.getUUID(),
                        player.level().getGameTime() + PainkillerItem.DURATION_TICKS);
                contents = shrinkSlot(contents, painkillerSlot);
                treatmentsApplied++;
            }
        }

        List<BodyPart> mainSystems = new ArrayList<>(List.of(BodyPart.HEAD, BodyPart.TORSO));
        mainSystems.sort(Comparator.comparing(p -> p.getPct(cap)));
        for (BodyPart part : mainSystems) {
            FirstAidKitContents result = tryTreatPart(cap, contents, part);
            if (result != null) {
                contents = result;
                treatmentsApplied++;
            }
        }

        List<BodyPart> subsystems = new ArrayList<>(List.of(
                BodyPart.LEFT_ARM, BodyPart.RIGHT_ARM, BodyPart.LEFT_LEG,
                BodyPart.RIGHT_LEG, BodyPart.LEFT_FOOT, BodyPart.RIGHT_FOOT));
        subsystems.sort(Comparator.comparing(p -> p.getPct(cap)));
        for (BodyPart part : subsystems) {
            FirstAidKitContents result = tryTreatPart(cap, contents, part);
            if (result != null) {
                contents = result;
                treatmentsApplied++;
            }
        }

        kitStack.set(ModDataComponents.FIRST_AID_KIT_CONTENTS.get(), contents);

        player.displayClientMessage(Component.literal(
                treatmentsApplied > 0
                        ? "Applied " + treatmentsApplied + " treatment(s)."
                        : "Nothing in the kit can treat your current injuries."), true);

        ModMessages.sendToPlayer(new BodyHealthSyncPacket(
                cap.getHead(), cap.getTorso(), cap.getLeftArm(), cap.getRightArm(),
                cap.getLeftLeg(), cap.getRightLeg(), cap.getLeftFoot(), cap.getRightFoot()
        ), player);
    }

    private static FirstAidKitContents tryTreatPart(BodyHealthCapability cap, FirstAidKitContents contents, BodyPart part) {
        BodyPartTier tier = BodyPartTier.from(part.getPct(cap));
        if (tier == BodyPartTier.GREEN) return null;

        int slot = findFirstSlotTreating(contents, tier, part);
        if (slot < 0) return null;

        TargetedMedItem med = (TargetedMedItem) contents.getSlot(slot).getItem();
        med.applyTreatment(cap, part);
        return shrinkSlot(contents, slot);
    }

    private static int findFirstSlotTreating(FirstAidKitContents contents, BodyPartTier tier, BodyPart part) {
        for (int i = 0; i < FirstAidKitContents.MAX_SLOTS; i++) {
            ItemStack stack = contents.getSlot(i);
            if (stack.isEmpty()) continue;
            if (!(stack.getItem() instanceof TargetedMedItem med)) continue;
            if (med.treatsTier() == tier && med.canTreat(part)) return i;
        }
        return -1;
    }

    private static int findFirstSlotOf(FirstAidKitContents contents, Class<?> itemClass) {
        for (int i = 0; i < FirstAidKitContents.MAX_SLOTS; i++) {
            ItemStack stack = contents.getSlot(i);
            if (!stack.isEmpty() && itemClass.isInstance(stack.getItem())) return i;
        }
        return -1;
    }

    private static FirstAidKitContents shrinkSlot(FirstAidKitContents contents, int slot) {
        ItemStack stack = contents.getSlot(slot).copy();
        stack.shrink(1);
        return contents.withSlot(slot, stack);
    }

    private static boolean hasAnyDebuffCondition(BodyHealthCapability cap) {
        for (BodyPart part : BodyPart.values()) {
            BodyPartTier tier = BodyPartTier.from(part.getPct(cap));
            if (tier == BodyPartTier.RED || tier == BodyPartTier.GREY) return true;
        }
        return false;
    }
}