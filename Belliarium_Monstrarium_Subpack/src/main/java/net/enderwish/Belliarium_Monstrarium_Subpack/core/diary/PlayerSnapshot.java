package net.enderwish.Belliarium_Monstrarium_Subpack.core.diary;

import com.mojang.serialization.Codec;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.ModAttachments;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.body.BodyHealthCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.integration.CuriosHooks;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.AdrenalineManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.medical.PainkillerManager;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.skills.LearnedSkillsCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.core.survival.SurvivalCapability;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.BodyHealthSyncPacket;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.ModMessages;
import net.enderwish.Belliarium_Monstrarium_Subpack.network.SurvivalSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * PlayerSnapshot
 *
 * Everything about the player that the rewind puts back: position, inventory
 * (main, armor, off-hand, every item's data components), the Curios wrist slot,
 * food, XP, body health, survival bars and learned skills.
 *
 * Learned skills ARE rewound: a skill book is consumed when its key is bound, and
 * the inventory restore brings that book back, so keeping the skill would
 * duplicate it.
 *
 * NEW -- Curios wrist slot included via CuriosHooks. Snapshots saved by the
 * previous slice have no "curios" entry, so restore skips it for those.
 *
 * VERIFY IF COMPILE FAILS -- FoodData#addAdditionalSaveData/readAdditionalSaveData,
 * ServerPlayer#teleportTo(ServerLevel, x, y, z, yaw, pitch),
 * ClientboundSetExperiencePacket(float, int, int), Player#closeContainer(),
 * Entity#clearFire(), Entity#resetFallDistance().
 */
public final class PlayerSnapshot {
    private PlayerSnapshot() {}

    public static CompoundTag capture(ServerPlayer player) {
        CompoundTag tag = new CompoundTag();

        tag.putDouble("x", player.getX());
        tag.putDouble("y", player.getY());
        tag.putDouble("z", player.getZ());
        tag.putFloat("yaw", player.getYRot());
        tag.putFloat("pitch", player.getXRot());

        tag.put("inventory", player.getInventory().save(new ListTag()));
        tag.put("curios", CuriosHooks.capture(player));

        CompoundTag food = new CompoundTag();
        player.getFoodData().addAdditionalSaveData(food);
        tag.put("food", food);

        tag.putInt("xp_level", player.experienceLevel);
        tag.putFloat("xp_progress", player.experienceProgress);
        tag.putInt("xp_total", player.totalExperience);

        putCodec(tag, "body_health", BodyHealthCapability.CODEC, player.getData(ModAttachments.BODY_HEALTH));
        putCodec(tag, "survival", SurvivalCapability.CODEC, player.getData(ModAttachments.SURVIVAL));
        putCodec(tag, "learned_skills", LearnedSkillsCapability.CODEC, player.getData(ModAttachments.LEARNED_SKILLS));

        return tag;
    }

    /** Restores everything, then teleports LAST so the world is already rewound when the player lands. */
    public static void restore(ServerPlayer player, ServerLevel overworld, CompoundTag tag) {
        player.closeContainer(); // any open chest/menu would hold stale slots

        // Inventory + Curios
        player.getInventory().load(tag.getList("inventory", Tag.TAG_COMPOUND));
        player.inventoryMenu.slotsChanged(player.getInventory());
        player.inventoryMenu.broadcastChanges();
        if (tag.contains("curios")) {
            CuriosHooks.restore(player, tag.getCompound("curios"));
        }

        // Food
        player.getFoodData().readAdditionalSaveData(tag.getCompound("food"));

        // XP (the packet is needed because vanilla only re-sends XP when the total changes)
        player.experienceLevel = tag.getInt("xp_level");
        player.experienceProgress = tag.getFloat("xp_progress");
        player.totalExperience = tag.getInt("xp_total");
        player.connection.send(new ClientboundSetExperiencePacket(
                player.experienceProgress, player.totalExperience, player.experienceLevel));

        // Body health, survival bars, learned skills
        BodyHealthCapability body = getCodec(tag, "body_health", BodyHealthCapability.CODEC);
        if (body != null) {
            player.setData(ModAttachments.BODY_HEALTH, body);
            ModMessages.sendToPlayer(new BodyHealthSyncPacket(
                    body.getHead(), body.getTorso(), body.getLeftArm(), body.getRightArm(),
                    body.getLeftLeg(), body.getRightLeg(), body.getLeftFoot(), body.getRightFoot()), player);
        }

        SurvivalCapability survival = getCodec(tag, "survival", SurvivalCapability.CODEC);
        if (survival != null) {
            player.setData(ModAttachments.SURVIVAL, survival);
            ModMessages.sendToPlayer(new SurvivalSyncPacket(
                    survival.getEnergy(), survival.getStamina(), survival.isCollapsed()), player);
        }

        LearnedSkillsCapability skills = getCodec(tag, "learned_skills", LearnedSkillsCapability.CODEC);
        if (skills != null) {
            player.setData(ModAttachments.LEARNED_SKILLS, skills);
        }

        // Transient state that shouldn't survive a rewind
        player.removeAllEffects();
        AdrenalineManager.INSTANCE.clear(player.getUUID());
        PainkillerManager.INSTANCE.clear(player.getUUID());
        player.clearFire();
        player.setHealth(player.getMaxHealth());
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();

        player.teleportTo(overworld,
                tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"),
                tag.getFloat("yaw"), tag.getFloat("pitch"));
    }

    private static <T> void putCodec(CompoundTag tag, String key, Codec<T> codec, T value) {
        codec.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(encoded -> tag.put(key, encoded));
    }

    private static <T> T getCodec(CompoundTag tag, String key, Codec<T> codec) {
        if (!tag.contains(key)) return null;
        return codec.parse(NbtOps.INSTANCE, tag.get(key)).result().orElse(null);
    }
}
