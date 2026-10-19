package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackExtensionHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * BackpackExtensionSelectPacket (C2S)
 *
 * Sent by the client whenever its locally-predicted extension state
 * changes (select left / select right / exit). The server re-validates
 * against its own data -- equipped bag + config special for that side +
 * special slot non-empty -- before applying (and seeds its virtual held
 * stack from the validated slot), so a bogus side 1/2 never sticks.
 * Side 0 (exit) is always valid.
 */
public record BackpackExtensionSelectPacket(byte side) implements CustomPacketPayload {

    public static final Type<BackpackExtensionSelectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "backpack_extension_select"));

    public static final StreamCodec<FriendlyByteBuf, BackpackExtensionSelectPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeByte(p.side),
            buf -> new BackpackExtensionSelectPacket(buf.readByte())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            byte side = this.side;
            if (side == BackpackExtensionHelper.NONE) {
                BackpackExtensionHelper.clear(player);
            } else if (side == BackpackExtensionHelper.LEFT || side == BackpackExtensionHelper.RIGHT) {
                if (BackpackExtensionHelper.isAvailable(player, side)) {
                    BackpackExtensionHelper.select(player, side);
                } else {
                    BackpackExtensionHelper.clear(player);
                }
            }
        });
    }
}
