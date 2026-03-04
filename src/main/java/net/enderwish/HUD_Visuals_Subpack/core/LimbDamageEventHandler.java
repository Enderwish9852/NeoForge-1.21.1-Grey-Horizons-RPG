package net.enderwish.HUD_Visuals_Subpack.core;

import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.enderwish.HUD_Visuals_Subpack.network.LimbSyncPacket;
import net.enderwish.HUD_Visuals_Subpack.network.ModMessages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles logic for the permanent Limb Damage System.
 * Updates:
 * 1. Only detects the watch if equipped in the Curios "wrist" slot.
 * 2. Optimized syncing to prevent HUD flickering.
 */
@EventBusSubscriber(modid = HUDVisualsSubpack.MOD_ID)
public class LimbDamageEventHandler {
    private static final Random RANDOM = new Random();

    /**
     * Handles Health Locking and Curios Watch detection.
     */
    @SubscribeEvent
    public static void onPlayerTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Only check once every 10 ticks to save performance and reduce network jitter
            if (player.level().getGameTime() % 10 != 0) return;

            WristCapability cap = player.getData(ModAttachments.WRIST_CAP);
            if (cap == null) return;

            // 1. ALWAYS LOCK HEALTH (Limb system is the primary health)
            if (player.getHealth() < player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }

            // 2. DETECT WATCH (Curios "wrist" slot only)
            AtomicBoolean isWearingWatch = new AtomicBoolean(false);
            CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
                inventory.getStacksHandler("wrist").ifPresent(handler -> {
                    for (int i = 0; i < handler.getStacks().getSlots(); i++) {
                        ItemStack stack = handler.getStacks().getStackInSlot(i);
                        if (!stack.isEmpty() && stack.is(HUDVisualsSubpack.SPORTS_WATCH.get())) {
                            isWearingWatch.set(true);
                            break;
                        }
                    }
                });
            });

            // 3. Update state and sync only if it changed
            boolean hasWatch = isWearingWatch.get();
            if (cap.hasWatchEquipped() != hasWatch) {
                cap.setWatchEquipped(hasWatch);
                syncToClient(player, cap);
            }
        }
    }

    /**
     * Distributes damage to specific limbs based on damage source.
     */
    @SubscribeEvent
    public static void onPlayerDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        WristCapability cap = player.getData(ModAttachments.WRIST_CAP);
        if (cap == null) return;

        // Reduce total damage slightly since limbs have individual toughness
        float amount = event.getNewDamage() * 0.5f;

        if (event.getSource().is(DamageTypes.FALL)) {
            cap.damageLeftFoot(amount * 0.4f);
            cap.damageRightFoot(amount * 0.4f);
            cap.damageLeftLeg(amount * 0.2f);
            cap.damageRightLeg(amount * 0.2f);
        } else if (event.getSource().is(DamageTypes.EXPLOSION)) {
            cap.damageTorso(amount * 0.6f);
            cap.damageLeftLeg(amount * 0.2f);
            cap.damageRightLeg(amount * 0.2f);
        } else {
            // Random distribution based on "height" of the hit
            double hitY = RANDOM.nextDouble();
            if (hitY > 0.85) cap.damageHead(amount);
            else if (hitY > 0.45) cap.damageTorso(amount);
            else if (hitY > 0.25) {
                if (RANDOM.nextBoolean()) cap.damageLeftArm(amount);
                else cap.damageRightArm(amount);
            } else if (hitY > 0.10) {
                if (RANDOM.nextBoolean()) cap.damageLeftLeg(amount);
                else cap.damageRightLeg(amount);
            } else {
                if (RANDOM.nextBoolean()) cap.damageLeftFoot(amount);
                else cap.damageRightFoot(amount);
            }
        }

        // Immediate sync after taking damage so the HUD updates instantly
        syncToClient(player, cap);
    }

    private static void syncToClient(ServerPlayer player, WristCapability cap) {
        ModMessages.sendToPlayer(new LimbSyncPacket(
                cap.getBPM(), cap.getEnergy(), cap.hasWatchEquipped(),
                cap.getHeadHealth(), cap.getTorsoHealth(),
                cap.getLeftArmHealth(), cap.getRightArmHealth(),
                cap.getLeftLegHealth(), cap.getRightLegHealth(),
                cap.getLeftFootHealth(), cap.getRightFootHealth()
        ), player);
    }
}