package net.enderwish.HUD_Visuals_Subpack.network;

import net.enderwish.HUD_Visuals_Subpack.HUDVisualsSubpack;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModMessages {

    /**
     * Registers the networking channel and payloads.
     */
    public static void register(final RegisterPayloadHandlersEvent event) {
        // We use the MOD_ID as the namespace for the registrar
        final PayloadRegistrar registrar = event.registrar(HUDVisualsSubpack.MOD_ID)
                .versioned("1.0");

        // 1. Limb Sync (Server -> Client)
        registrar.playToClient(
                LimbSyncPacket.TYPE,
                LimbSyncPacket.STREAM_CODEC,
                LimbSyncPacket::handle
        );

        // 2. Wrist Sync (Server -> Client)
        registrar.playToClient(
                WristSyncPacket.TYPE,
                WristSyncPacket.STREAM_CODEC,
                WristSyncPacket::handle
        );

        // 3. Season Sync (Server -> Client) - ADDED THIS
        registrar.playToClient(
                SeasonSyncPacket.TYPE,
                SeasonSyncPacket.STREAM_CODEC,
                (payload, context) -> payload.handle(context)
        );
    }

    /**
     * Helper to send a packet to a specific player.
     */
    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }

    /**
     * Helper to send a packet to everyone on the server.
     * Required by WeatherManager to sync environmental states.
     */
    public static void sendToAllPlayers(CustomPacketPayload packet) {
        PacketDistributor.sendToAllPlayers(packet);
    }
}