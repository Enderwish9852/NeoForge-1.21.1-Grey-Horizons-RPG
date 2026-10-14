package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.BackpackScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * BackpackContentsPacket (S2C)
 *
 * Opens/refreshes the backpack screen with the authoritative contents of
 * the equipped backpack (mirrors FirstAidKitContentsPacket's pattern).
 */
public record BackpackContentsPacket(List<ItemStack> slots) implements CustomPacketPayload {

    public static final Type<BackpackContentsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "backpack_contents"));

    public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, BackpackContentsPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.slots.size());
                for (ItemStack stack : p.slots) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
            },
            buf -> {
                int count = buf.readVarInt();
                List<ItemStack> slots = new ArrayList<>(count);
                for (int i = 0; i < count; i++) slots.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                return new BackpackContentsPacket(slots);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;
            Minecraft.getInstance().setScreen(new BackpackScreen(slots));
        });
    }
}
