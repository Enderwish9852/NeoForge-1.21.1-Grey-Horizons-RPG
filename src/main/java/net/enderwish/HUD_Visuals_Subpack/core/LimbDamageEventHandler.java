package net.enderwish.HUD_Visuals_Subpack.core;

import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.network.LimbSyncPacket;
import net.enderwish.HUD_Visuals_Subpack.network.ModMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.Random;

/**
 * Handles the logic for distributing incoming damage to specific limbs.
 * Updated to include Left and Right Foot support.
 */
@EventBusSubscriber(modid = HUDVisualsSubpack.MOD_ID)
public class LimbDamageEventHandler {
    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent.Post event) {
        // Only process for players on the server side to ensure data integrity
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        WristCapability cap = player.getData(ModAttachments.WRIST_CAP);

        // Scale damage: 0.05f means it takes roughly 20 full hearts of damage to break a limb
        float amount = event.getNewDamage() * 0.05f;

        // 1. DETERMINISTIC LOGIC (Environmental Damage)
        if (event.getSource().is(DamageTypes.FALL)) {
            // Fall damage hits feet hardest, then legs
            cap.damageLeftFoot(amount * 0.4f);
            cap.damageRightFoot(amount * 0.4f);
            cap.damageLeftLeg(amount * 0.1f);
            cap.damageRightLeg(amount * 0.1f);
        } else if (event.getSource().is(DamageTypes.EXPLOSION)) {
            // Explosions hit the lower body and torso
            cap.damageTorso(amount * 0.4f);
            cap.damageLeftLeg(amount * 0.2f);
            cap.damageRightLeg(amount * 0.2f);
            cap.damageLeftFoot(amount * 0.1f);
            cap.damageRightFoot(amount * 0.1f);
        } else {
            // 2. RANDOMIZED COMBAT LOGIC (Simulating hit height)
            double hitY = RANDOM.nextDouble();

            if (hitY > 0.85) { // Head
                cap.damageHead(amount);
            } else if (hitY > 0.45) { // Torso
                cap.damageTorso(amount);
            } else if (hitY > 0.25) { // Arms
                if (RANDOM.nextBoolean()) cap.damageLeftArm(amount);
                else cap.damageRightArm(amount);
            } else if (hitY > 0.10) { // Legs
                if (RANDOM.nextBoolean()) cap.damageLeftLeg(amount);
                else cap.damageRightLeg(amount);
            } else { // Feet (Hits very low to the ground)
                if (RANDOM.nextBoolean()) cap.damageLeftFoot(amount);
                else cap.damageRightFoot(amount);
            }
        }

        // 3. SYNC TO CLIENT
        // Must send all 11 values: bpm, energy, watchState, head, torso, lArm, rArm, lLeg, rLeg, lFoot, rFoot
        ModMessages.sendToPlayer(new LimbSyncPacket(
                cap.getBPM(),
                cap.getEnergy(),
                cap.hasWatchEquipped(),
                cap.getHeadHealth(),
                cap.getTorsoHealth(),
                cap.getLeftArmHealth(),
                cap.getRightArmHealth(),
                cap.getLeftLegHealth(),
                cap.getRightLegHealth(),
                cap.getLeftFootHealth(),
                cap.getRightFootHealth()
        ), player);
    }
}