package net.enderwish.Belliarium_Monstrarium_Subpack.network;

import net.enderwish.Belliarium_Monstrarium_Subpack.BelliariumMonstrariumSubpack;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.gear.backpack.BackpackHandler;
import net.enderwish.Belliarium_Monstrarium_Subpack.item.gear.BackpackItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * BackpackMovePacket (C2S)
 *
 * deposit=false: slotIndex is a backpack slot — move that stack out to the
 * hotbar (picking it out by hand). deposit=true: slotIndex is a HOTBAR slot
 * (0-8) — move that stack into the equipped backpack.
 * The server re-sends BackpackContentsPacket after the move so the open
 * screen refreshes with authoritative state.
 */
public record BackpackMovePacket(int slotIndex, boolean deposit) implements CustomPacketPayload {

    public static final Type<BackpackMovePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BelliariumMonstrariumSubpack.MODID, "backpack_move"));

    public static final StreamCodec<FriendlyByteBuf, BackpackMovePacket> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> { buf.writeVarInt(p.slotIndex); buf.writeBoolean(p.deposit); },
            buf -> new BackpackMovePacket(buf.readVarInt(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ItemStack backpack = BackpackHandler.getEquipped(player);
            if (backpack.isEmpty() || !(backpack.getItem() instanceof BackpackItem item)) return;

            if (deposit) {
                if (slotIndex < 0 || slotIndex >= 9) return;
                ItemStack from = player.getInventory().getItem(slotIndex);
                if (from.isEmpty()) return;
                int leftover = item.insert(backpack, from);
                if (leftover == 0) player.getInventory().setItem(slotIndex, ItemStack.EMPTY);
                else from.setCount(leftover);
            } else {
                var slots = item.getContents(backpack);
                if (slotIndex < 0 || slotIndex >= slots.size()) return;
                ItemStack stack = slots.get(slotIndex);
                if (stack.isEmpty()) return;
                if (!player.getInventory().add(stack)) {
                    player.drop(stack, false); // hotbar full: drop at feet
                }
                slots.set(slotIndex, ItemStack.EMPTY);
                item.writeContents(backpack, slots);
            }

            ModMessages.sendToPlayer(
                    new BackpackContentsPacket(item.getContents(backpack)), player);
        });
    }
}
