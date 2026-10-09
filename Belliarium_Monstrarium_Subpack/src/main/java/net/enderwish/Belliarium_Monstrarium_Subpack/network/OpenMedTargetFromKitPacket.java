package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.MedTargetScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenMedTargetFromKitPacket(int handOrdinal, int kitSlotIndex, ItemStack medStack)
        implements CustomPacketPayload {

    public static final Type<OpenMedTargetFromKitPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "open_med_target_from_kit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMedTargetFromKitPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.handOrdinal);
                buf.writeVarInt(p.kitSlotIndex);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, p.medStack);
            },
            buf -> new OpenMedTargetFromKitPacket(
                    buf.readVarInt(), buf.readVarInt(), ItemStack.OPTIONAL_STREAM_CODEC.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;
            Minecraft.getInstance().setScreen(new MedTargetScreen(handOrdinal, kitSlotIndex, medStack));
        });
    }
}
