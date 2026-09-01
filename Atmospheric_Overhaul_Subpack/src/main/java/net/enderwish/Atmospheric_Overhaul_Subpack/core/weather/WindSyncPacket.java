package net.enderwish.Atmospheric_Overhaul_Subpack.network;

import net.enderwish.Atmospheric_Overhaul_Subpack.client.ClientSeasonState;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindDirection;
import net.enderwish.Atmospheric_Overhaul_Subpack.core.weather.WindState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * WindSyncPacket
 *
 * Sent server → all clients every 20 ticks.
 * Carries the current WindState so client-side systems
 * (particles, god rays, player animations) can read it.
 */
public record WindSyncPacket(
        byte direction,
        float speed,
        float gustFactor,
        float turbulence
) implements CustomPacketPayload {

    public static final Type<WindSyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    "gh_atmospheric", "wind_sync"));

    public static final StreamCodec<FriendlyByteBuf, WindSyncPacket> CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.BYTE,
                    WindSyncPacket::direction,
                    net.minecraft.network.codec.ByteBufCodecs.FLOAT,
                    WindSyncPacket::speed,
                    net.minecraft.network.codec.ByteBufCodecs.FLOAT,
                    WindSyncPacket::gustFactor,
                    net.minecraft.network.codec.ByteBufCodecs.FLOAT,
                    WindSyncPacket::turbulence,
                    WindSyncPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    public static WindSyncPacket from(WindState state) {
        return new WindSyncPacket(
                state.directionByte(),
                state.getSpeed(),
                state.getGustFactor(),
                state.getTurbulence());
    }

    // ── Send ──────────────────────────────────────────────────────────────────

    public static void sendToAll(ServerLevel level, WindState state) {
        WindSyncPacket packet = from(state);
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }

    // ── Handle (client side) ──────────────────────────────────────────────────

    public static void handle(WindSyncPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            WindState state = new WindState(
                    WindState.directionFromByte(packet.direction()),
                    packet.speed(),
                    packet.gustFactor(),
                    packet.turbulence());
            ClientSeasonState.INSTANCE.setWindState(state);
        });
    }
}
