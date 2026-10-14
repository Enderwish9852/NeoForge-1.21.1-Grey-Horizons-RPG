package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackHandler;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.gear.BackpackItem;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * BackpackOpenPacket (C2S)
 *
 * Sent by the Open-Backpack keybind. The server verifies an equipped
 * backpack and replies with BackpackContentsPacket (which opens the
 * screen client-side with authoritative contents).
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
            ModMessages.sendToPlayer(new BackpackContentsPacket(item.getContents(backpack)), player);
        });
    }
}
