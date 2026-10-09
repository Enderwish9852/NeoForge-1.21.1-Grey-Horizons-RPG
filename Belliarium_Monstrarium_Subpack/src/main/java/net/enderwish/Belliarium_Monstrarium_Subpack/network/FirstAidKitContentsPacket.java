package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.FirstAidKitScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record FirstAidKitContentsPacket(int handOrdinal, List<ItemStack> slots) implements CustomPacketPayload {

    public static final Type<FirstAidKitContentsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "first_aid_kit_contents"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FirstAidKitContentsPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.handOrdinal);
                buf.writeVarInt(p.slots.size());
                for (ItemStack stack : p.slots) ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
            },
            buf -> {
                int hand = buf.readVarInt();
                int count = buf.readVarInt();
                List<ItemStack> slots = new ArrayList<>(count);
                for (int i = 0; i < count; i++) slots.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
                return new FirstAidKitContentsPacket(hand, slots);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;
            Minecraft.getInstance().setScreen(new FirstAidKitScreen(handOrdinal, slots));
        });
    }
}
