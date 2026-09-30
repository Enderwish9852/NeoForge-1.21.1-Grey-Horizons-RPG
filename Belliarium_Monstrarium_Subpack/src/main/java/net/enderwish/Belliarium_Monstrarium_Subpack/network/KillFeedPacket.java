package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.CombatLogClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** One kill-feed row: "<killerName> [weapon icon] <targetName>". */
public record KillFeedPacket(String killerName, String weaponId, String targetName)
        implements CustomPacketPayload {

    public static final Type<KillFeedPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "kill_feed"));

    public static final StreamCodec<FriendlyByteBuf, KillFeedPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeUtf(p.killerName); buf.writeUtf(p.weaponId); buf.writeUtf(p.targetName); },
            buf -> new KillFeedPacket(buf.readUtf(), buf.readUtf(), buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;
            CombatLogClientState.addKillFeedRow(killerName, weaponId, targetName);
        });
    }
}
