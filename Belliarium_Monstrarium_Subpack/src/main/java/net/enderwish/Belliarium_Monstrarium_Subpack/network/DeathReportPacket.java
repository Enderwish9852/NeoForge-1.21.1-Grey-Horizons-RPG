package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.DeathReportClientState;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.gui.DeathReportScreen;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.combat.CombatLogEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * DeathReportPacket
 *
 * NEW -- rewindAvailable. When true the player is NOT dead (they're in limbo), so the
 * report screen opens immediately instead of waiting for vanilla's DeathScreen.
 */
public record DeathReportPacket(
        boolean combatDeath,
        String headline,
        float headPct, float torsoPct, float leftArmPct, float rightArmPct,
        float leftLegPct, float rightLegPct, float leftFootPct, float rightFootPct,
        List<CombatLogEntry> log,
        boolean rewindAvailable
) implements CustomPacketPayload {

    public static final Type<DeathReportPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "death_report"));

    public static final StreamCodec<FriendlyByteBuf, DeathReportPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.combatDeath);
                buf.writeUtf(p.headline);
                buf.writeFloat(p.headPct); buf.writeFloat(p.torsoPct);
                buf.writeFloat(p.leftArmPct); buf.writeFloat(p.rightArmPct);
                buf.writeFloat(p.leftLegPct); buf.writeFloat(p.rightLegPct);
                buf.writeFloat(p.leftFootPct); buf.writeFloat(p.rightFootPct);
                buf.writeVarInt(p.log.size());
                for (CombatLogEntry entry : p.log) {
                    buf.writeVarInt(entry.secondsIntoBattle());
                    buf.writeUtf(entry.sourceName());
                    buf.writeDouble(entry.distanceBlocks());
                    buf.writeFloat(entry.amount());
                    buf.writeUtf(entry.bodyPart());
                }
                buf.writeBoolean(p.rewindAvailable);
            },
            buf -> {
                boolean combatDeath = buf.readBoolean();
                String headline = buf.readUtf();
                float head = buf.readFloat(), torso = buf.readFloat();
                float lArm = buf.readFloat(), rArm = buf.readFloat();
                float lLeg = buf.readFloat(), rLeg = buf.readFloat();
                float lFoot = buf.readFloat(), rFoot = buf.readFloat();
                int count = buf.readVarInt();
                List<CombatLogEntry> log = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    int seconds = buf.readVarInt();
                    String source = buf.readUtf();
                    double distance = buf.readDouble();
                    float amount = buf.readFloat();
                    String bodyPart = buf.readUtf();
                    log.add(new CombatLogEntry(seconds, source, distance, amount, bodyPart));
                }
                boolean rewindAvailable = buf.readBoolean();
                return new DeathReportPacket(combatDeath, headline, head, torso, lArm, rArm,
                        lLeg, rLeg, lFoot, rFoot, log, rewindAvailable);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;
            if (rewindAvailable) {
                Minecraft.getInstance().setScreen(new DeathReportScreen(this));
            } else {
                DeathReportClientState.setPending(this);
            }
        });
    }
}
