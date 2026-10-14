package net.enderwish.Belliarium_Monstrarium_Subpack.core.medical;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BandagePending — server-side memory of "this player has chosen a body
 * part and must now HOLD right-click for the wrap duration".
 *
 * Flow: player right-clicks bandage -> MedTargetScreen picks a part ->
 * MedApplyPacket stores the choice here instead of healing instantly ->
 * the player holds right-click -> BandageItem.use() starts vanilla's
 * hold-to-use tracking -> finishUsingItem() consumes the pending entry,
 * applies the treatment and consumes one bandage.
 * Releasing early just means the hold isn't finished — the pending entry
 * stays until used, so the player can re-hold and continue.
 */
public final class BandagePending {

    public record Pending(String bodyPartName, int handOrdinal, int kitSlotIndex) {}

    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    /** Client-side mirror: the SERVER map is invisible to the client, so
     * the client must be told a choice was made or use() would re-open
     * the picker screen on every right-click instead of holding. */
    private static volatile Pending CLIENT_PENDING = null;

    public static void set(Player player, Pending pending) {
        PENDING.put(player.getUUID(), pending);
    }

    public static Pending get(Player player) {
        return PENDING.get(player.getUUID());
    }

    public static Pending clear(Player player) {
        return PENDING.remove(player.getUUID());
    }

    /** CLIENT ONLY: mirror of the server's pending state for the local player. */
    public static void setClient(Pending pending) {
        CLIENT_PENDING = pending;
    }

    public static Pending getClient() {
        return CLIENT_PENDING;
    }

    public static void clearClient() {
        CLIENT_PENDING = null;
    }
}
