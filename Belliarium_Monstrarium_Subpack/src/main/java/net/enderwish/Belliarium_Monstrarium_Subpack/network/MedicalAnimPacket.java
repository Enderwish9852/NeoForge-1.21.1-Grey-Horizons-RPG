package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.MedicalAnimClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * MedicalAnimPacket (server -> all clients)
 *
 * Tells every client to play a named PlayerAnimator animation on the given
 * player's vanilla rig. Fired when a player starts wrapping a bandage or
 * inspects their first aid kit, so nearby players see the same motion.
 */
public record MedicalAnimPacket(UUID playerId, String animName) implements CustomPacketPayload {

    public static final Type<MedicalAnimPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "medical_anim"));

    public static final StreamCodec<FriendlyByteBuf, MedicalAnimPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUUID(p.playerId); buf.writeUtf(p.animName, 64); },
            buf -> new MedicalAnimPacket(buf.readUUID(), buf.readUtf(64))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().level() instanceof ClientLevel level)) return;
            Player target = level.getPlayerByUUID(playerId);
            if (target != null) {
                MedicalAnimClient.play(target, animName);
            }
        });
    }
}
