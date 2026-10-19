package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackConfig;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackHandler;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackRegistry;
import net.enderwish.Belliarium_Monstrarium_Subpack.gui.BackpackMenu;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.gear.BackpackItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * BackpackOpenPacket (C2S)
 *
 * Sent by the Open-Backpack keybind. The server validates an equipped
 * backpack, then opens the registered "backpack" menu via the vanilla
 * openMenu handshake: the config is packed into the extra-data buffer so
 * the client's BackpackMenu factory can rebuild an identical menu.
 */
public record BackpackOpenPacket() implements CustomPacketPayload {

    public static final Type<BackpackOpenPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "backpack_open"));

    public static final BackpackOpenPacket INSTANCE = new BackpackOpenPacket();

    public static final StreamCodec<FriendlyByteBuf, BackpackOpenPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ItemStack backpack = BackpackHandler.getEquipped(player);
            if (backpack.isEmpty() || !(backpack.getItem() instanceof BackpackItem item)) return;
            String id = item.backpackId(backpack);
            BackpackConfig config = BackpackRegistry.INSTANCE.getOrFallback(id);

            player.openMenu(new net.minecraft.world.MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("item.gh_belliarium_monstrarium." + id);
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, net.minecraft.world.entity.player.Inventory inv, Player p) {
                    return new BackpackMenu(windowId, inv, config, BackpackHandler.getEquipped(p));
                }
            }, buf -> BackpackMenu.writeConfig(buf, config));
        });
    }
}
