package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModMessages {
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(BelliariumMonstrariumSubpack.MODID).versioned("1.0");
        registrar.playToClient(BodyHealthSyncPacket.TYPE, BodyHealthSyncPacket.STREAM_CODEC, BodyHealthSyncPacket::handle);
        registrar.playToClient(SurvivalSyncPacket.TYPE, SurvivalSyncPacket.STREAM_CODEC, SurvivalSyncPacket::handle);
        registrar.playToClient(CombatTimerSyncPacket.TYPE, CombatTimerSyncPacket.STREAM_CODEC, CombatTimerSyncPacket::handle);
        registrar.playToClient(DeathReportPacket.TYPE, DeathReportPacket.STREAM_CODEC, DeathReportPacket::handle);
    }

    public static void sendToPlayer(CustomPacketPayload packet, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}
