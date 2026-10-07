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

/**
 * OpenMedTargetFromKitPacket
 *
 * Carries the actual med ItemStack rather than just a slot index, so
 * MedTargetScreen always knows exactly what it's validating against with
 * no extra client-side state tracking needed. Uses RegistryFriendlyByteBuf
 * like FirstAidKitContentsPacket since ItemStack's stream codec needs
 * registry access.
 */
public record OpenMedTargetFromKitPacket(int handOrdinal, int kitSlotIndex, ItemStack medStack)
        implements CustomPacketPayload {

    public static final Type<OpenMedTargetFromKitPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "open_med_target_from_kit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMedTargetFromKitPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.handOrdinal);
                buf.writeVarInt(p.kitSlotIndex);
                ItemStack.STREAM_CODEC.encode(buf, p.medStack);
            },
            buf -> new OpenMedTargetFromKitPacket(buf.readVarInt(), buf.readVarInt(), ItemStack.STREAM_CODEC.decode(buf))
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
