package net.enderwish.Belliarium_Monstrarium_Subpack.client;

import dev.kosmx.playerAnim.api.IPlayer;
import dev.kosmx.playerAnim.api.layered.AnimationStack;
import dev.kosmx.playerAnim.api.layered.KeyframeAnimationPlayer;
import dev.kosmx.playerAnim.minecraftApi.PlayerAnimationRegistry;
import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.medical.FirstAidKitItem;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.KitSelectNotifyPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * MedicalAnimClient — client-side PlayerAnimator driver.
 *
 * play(): plays a named animation (from assets/<modid>/player_animations/)
 * on the vanilla player rig in third person, replacing any previous one
 * for that player. Triggered by MedicalAnimPacket (bandage wrap, kit
 * inspect) so everyone nearby sees the same choreography.
 *
 * Hotbar watcher: selecting the first aid kit in the hotbar sends
 * KitSelectNotifyPacket so the server can broadcast the inspect animation
 * to everyone (the server cannot observe hotbar selection itself).
 */
@EventBusSubscriber(modid = BelliariumMonstrariumSubpack.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class MedicalAnimClient {

    private static final org.slf4j.Logger LOGGER =
            com.mojang.logging.LogUtils.getLogger();

    /** Currently playing layer per player, so a new animation replaces the old. */
    private static final Map<UUID, KeyframeAnimationPlayer> ACTIVE = new WeakHashMap<>();

    private static int lastSelectedSlot = -1;

    /** Plays <animName> on the given player's vanilla rig (third person). */
    public static void play(Player player, String animName) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                BelliariumMonstrariumSubpack.MODID, animName);
        dev.kosmx.playerAnim.api.IPlayable playable =
                PlayerAnimationRegistry.getAnimation(id);
        if (playable == null) {
            LOGGER.warn("Player animation not found: {}", id);
            return;
        }

        AnimationStack stack = ((IPlayer) player).getAnimationStack();
        KeyframeAnimationPlayer prev = ACTIVE.remove(player.getUUID());
        if (prev != null) stack.removeLayer(prev);

        KeyframeAnimationPlayer layer = new KeyframeAnimationPlayer(
                (dev.kosmx.playerAnim.core.data.KeyframeAnimation) playable);
        stack.addAnimLayer(0, layer);
        ACTIVE.put(player.getUUID(), layer);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            lastSelectedSlot = -1;
            return;
        }
        if (player.getInventory().selected != lastSelectedSlot) {
            lastSelectedSlot = player.getInventory().selected;
            ItemStack held = player.getMainHandItem();
            if (held.getItem() instanceof FirstAidKitItem) {
                ModMessages.sendToServer(KitSelectNotifyPacket.INSTANCE);
            }
        }
        // reap finished animations
        ACTIVE.entrySet().removeIf(e -> {
            if (!e.getValue().isActive()) {
                Player p = Minecraft.getInstance().level
                        .getPlayerByUUID(e.getKey());
                if (p != null) {
                    ((IPlayer) p).getAnimationStack().removeLayer(e.getValue());
                }
                return true;
            }
            return false;
        });
    }
}
