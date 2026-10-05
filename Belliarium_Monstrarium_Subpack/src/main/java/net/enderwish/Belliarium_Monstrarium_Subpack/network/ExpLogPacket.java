package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.client.CombatLogClientState;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.combat.CombatLogRowType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * ExpLogPacket
 *
 * Drives one row of the persistent exp-log section. action=SHOW carries the
 * row's label + the xp value it should count up to; fadesOnItsOwn=true is
 * only ever set for ENTER_BATTLE, matching "this is the only case this line
 * will fade out on its own" -- every other row's lifetime is tied to the
 * battle itself (cleared client-side when combat ends) or to an explicit
 * FADE_OUT/RESTART from a combo break.
 */
public record ExpLogPacket(CombatLogRowType rowType, byte action, String label, float targetXp, boolean fadesOnItsOwn)
        implements CustomPacketPayload {

    private static final byte ACTION_SHOW = 0;
    private static final byte ACTION_FADE_OUT = 1;
    private static final byte ACTION_RESTART = 2;

    public static ExpLogPacket show(CombatLogRowType rowType, String label, float targetXp, boolean fadesOnItsOwn) {
        return new ExpLogPacket(rowType, ACTION_SHOW, label, targetXp, fadesOnItsOwn);
    }

    public static ExpLogPacket fadeOut(CombatLogRowType rowType) {
        return new ExpLogPacket(rowType, ACTION_FADE_OUT, "", 0f, false);
    }

    public static ExpLogPacket restart(CombatLogRowType rowType, String label) {
        return new ExpLogPacket(rowType, ACTION_RESTART, label, 0f, false);
    }

    public static final Type<ExpLogPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "exp_log"));

    public static final StreamCodec<FriendlyByteBuf, ExpLogPacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeEnum(p.rowType);
                buf.writeByte(p.action);
                buf.writeUtf(p.label);
                buf.writeFloat(p.targetXp);
                buf.writeBoolean(p.fadesOnItsOwn);
            },
            buf -> new ExpLogPacket(buf.readEnum(CombatLogRowType.class), buf.readByte(),
                    buf.readUtf(), buf.readFloat(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().player == null) return;
            switch (action) {
                case ACTION_SHOW -> CombatLogClientState.showRow(rowType, label, targetXp, fadesOnItsOwn);
                case ACTION_FADE_OUT -> CombatLogClientState.fadeOutRow(rowType);
                case ACTION_RESTART -> CombatLogClientState.restartRow(rowType, label);
                default -> {}
            }
        });
    }
}
