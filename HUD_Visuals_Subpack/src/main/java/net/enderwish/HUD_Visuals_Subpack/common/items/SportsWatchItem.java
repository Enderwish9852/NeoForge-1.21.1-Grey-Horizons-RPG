package net.enderwish.HUD_Visuals_Subpack.common.items;

import net.enderwish.HUD_Visuals_Subpack.core.ModAttachments;
import net.enderwish.HUD_Visuals_Subpack.core.PlayerCapability;
import net.enderwish.HUD_Visuals_Subpack.network.ModMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Custom Item class for the Sports Watch.
 * Updated: Synchronizes the full 14-argument state including Hunger/Fuel and Thirst.
 */
public class SportsWatchItem extends Item {

    public SportsWatchItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {

            // Perform sync and check once a second (20 ticks) to optimize performance
            if (level.getGameTime() % 20 != 0) return;

            PlayerCapability cap = player.getData(ModAttachments.PLAYER_CAP);
            if (cap == null) return;

            // Curios check to see if the watch is actually equipped in the wrist slot
            AtomicBoolean isEquippedInWrist = new AtomicBoolean(false);

            CuriosApi.getCuriosInventory(player).ifPresent(inventory -> {
                inventory.getStacksHandler("wrist").ifPresent(handler -> {
                    for (int i = 0; i < handler.getStacks().getSlots(); i++) {
                        if (handler.getStacks().getStackInSlot(i) == stack) {
                            isEquippedInWrist.set(true);
                            break;
                        }
                    }
                });
            });

            // Update the capability state if the physical equipment status changed
            if (cap.hasWatchEquipped() != isEquippedInWrist.get()) {
                cap.setWatchEquipped(isEquippedInWrist.get());
            }

            // Always sync while in inventory so the HUD has the latest survival data
            syncToClient(player, cap);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MutableComponent epicPart = Component.translatable("item.gh_hud_visuals.sports_watch.epic")
                .withStyle(ChatFormatting.DARK_PURPLE);

        MutableComponent wearablePart = Component.translatable("item.gh_hud_visuals.sports_watch.key_wearable")
                .withStyle(ChatFormatting.GRAY);

        tooltip.add(epicPart.append(wearablePart));

        tooltip.add(Component.translatable("item.gh_hud_visuals.sports_watch.desc")
                .withStyle(ChatFormatting.GRAY));

        tooltip.add(Component.empty());

        tooltip.add(Component.translatable("item.gh_hud_visuals.sports_watch.hint")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
    }

    /**
     * Synchronizes all capability data to the client via LimbSyncPacket.
     * Updated: Now provides all 14 arguments required by the updated record.
     */
    private void syncToClient(ServerPlayer player, PlayerCapability cap) {
        ModMessages.sendToPlayer(new net.enderwish.HUD_Visuals_Subpack.network.LimbSyncPacket(
                cap.getBPM(),
                cap.getEnergy(),
                cap.getThirst(),
                cap.getHunger(),      // <--- ADDED: 4th Argument (Fuel Tank)
                cap.hasWatchEquipped(),
                cap.getHeadHealth(),
                cap.getTorsoHealth(),
                cap.getLeftArmHealth(),
                cap.getRightArmHealth(),
                cap.getLeftLegHealth(),
                cap.getRightLegHealth(),
                cap.getLeftFootHealth(),
                cap.getRightFootHealth(),
                cap.getStarvationTimer(),
                cap.getPollenExposure(),
                cap.getCoreTemp(),
                cap.getWetness()
        ), player);
    }
}