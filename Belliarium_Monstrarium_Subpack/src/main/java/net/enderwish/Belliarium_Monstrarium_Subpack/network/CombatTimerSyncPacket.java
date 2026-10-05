package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.CombatTimerClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CombatTimerSyncPacket(boolean inCombat, int elapsedSeconds, String endMessage, long battleId)
        implements CustomPacketPayload {

    public static final Type<CombatTimerSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "combat_timer_sync"));

    public static final StreamCodec<FriendlyByteBuf, CombatTimerSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.inCombat);
                buf.writeVarInt(p.elapsedSeconds);
                buf.writeUtf(p.endMessage == null ? "" : p.endMessage);
                buf.writeVarLong(p.battleId);
            },
            buf -> {
                boolean inCombat = buf.readBoolean();
                int elapsed = buf.readVarInt();
                String msg = buf.readUtf();
                long battleId = buf.readVarLong();
                return new CombatTimerSyncPacket(inCombat, elapsed, msg.isEmpty() ? null : msg, battleId);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;
            CombatTimerClientState.update(inCombat, elapsedSeconds, endMessage, battleId);
        });
    }
}
