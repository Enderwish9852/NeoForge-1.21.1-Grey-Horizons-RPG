package net.enderwish.Farming_Overhaul_Subpack.entity;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * FallingTreeEntity
 *
 * Represents a felled tree falling through the air. Created when a player
 * cuts a log in the trunk column - the whole tree (logs + leaves above the
 * cut) becomes this entity and falls under gravity.
 *
 * On impact with the ground or any solid block, the entity stops.
 */
public class FallingTreeEntity extends Entity {

    /** Maximum number of blocks a single falling tree can carry. */
    private static final int MAX_BLOCKS = 512;

    /** Gravity acceleration per tick. */
    private static final double GRAVITY = 0.08;

    /** Terminal velocity (blocks per tick). */
    private static final double TERMINAL_VELOCITY = 0.8;

    /** How many ticks the tree can fall before giving up (safety limit). */
    private static final int MAX_TICKS = 600;

    /** How many ticks after landing before the entity is discarded. */
    private static final int LANDING_TICKS = 5;

    /** Rotation speed while falling (radians per tick). */
    private static final double ROTATION_SPEED = 0.05;

    /** Minimum fall speed (blocks per tick) to cause damage on impact. */
    private static final double DAMAGE_THRESHOLD = 0.3;

    private double fallX, fallY, fallZ;      // velocity
    private int ticksFalling = 0;
    private boolean hasLanded = false;
    private int landingTicks = 0;

    /** Yaw angle of the fall direction (radians). */
    private double fallYaw = 0.0;

    /** Roll angle while falling (radians). */
    private double fallRoll = 0.0;

    /** Height fallen since creation (blocks). */
    private double fallDistance = 0.0;

    /** Whether this tree has already dealt impact damage. */
    private boolean damageDealt = false;

    /** All block positions and states that make up this falling tree. */
    private final List<BlockStateAndPos> blocks = new ArrayList<>();

    public FallingTreeEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    /** Add a block to the falling tree. */
    public void addBlock(BlockPos pos, BlockState state) {
        if (blocks.size() >= MAX_BLOCKS) return;
        blocks.add(new BlockStateAndPos(pos, state));
    }

    /** Number of blocks in the tree. */
    public int blockCount() { return blocks.size(); }

    /** Set the initial fall velocity and direction. */
    public void setFallDirection(double vx, double vy, double vz) {
        this.fallX = vx;
        this.fallY = vy;
        this.fallZ = vz;
        // Yaw points in the direction of horizontal movement
        double h = Math.sqrt(vx * vx + vz * vz);
        if (h > 0.001) {
            this.fallYaw = Math.atan2(vz, vx);
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (hasLanded) {
            landingTicks++;
            if (landingTicks > LANDING_TICKS) {
                discard();
            }
            return;
        }

        ticksFalling++;
        if (ticksFalling > MAX_TICKS) {
            discard();
            return;
        }

        // Apply gravity
        fallY -= GRAVITY;
        if (fallY < -TERMINAL_VELOCITY) fallY = -TERMINAL_VELOCITY;

        // Track fall distance for damage calculation
        fallDistance += Math.sqrt(fallX * fallX + fallY * fallY + fallZ * fallZ);

        // Slowly rotate while falling (tumbling)
        fallRoll += ROTATION_SPEED;

        // Move the entity
        double nx = getX() + fallX;
        double ny = getY() + fallY;
        double nz = getZ() + fallZ;

        // Check collision at new position
        AABB box = getBoundingBox().move(fallX, fallY, fallZ);
        if (this.level().noCollision(this, box)) {
            setPos(nx, ny, nz);
        } else {
            // Hit something - snap to surface and land
            double ny2 = Math.floor(ny) + 1.0;
            setPos(getX(), ny2, getZ());
            land();
        }
    }

    private void land() {
        hasLanded = true;
        landingTicks = 0;
        fallX = 0; fallY = 0; fallZ = 0;

        // Play landing sound
        this.level().playSound(null, getX(), getY(), getZ(),
                net.minecraft.sounds.SoundEvents.WOOD_BREAK,
                net.minecraft.sounds.SoundSource.BLOCKS,
                1.0f, 1.0f);

        // Deal impact damage to nearby entities
        dealImpactDamage();

        // Shatter brittle blocks at impact
        shatterBrittleBlocks();

        // Drop all items immediately for now
        dropAllItems();
    }

    private void dealImpactDamage() {
        if (damageDealt) return;
        double speed = Math.sqrt(fallX * fallX + fallY * fallY + fallZ * fallZ);
        if (speed < DAMAGE_THRESHOLD) return;

        damageDealt = true;

        int damage = (int) Math.round(speed * 10.0);
        damage = Math.max(1, Math.min(damage, 20));

        AABB box = getBoundingBox().inflate(2.0, 1.0, 2.0);
        List<Entity> entities = this.level().getEntitiesOfClass(Entity.class, box);
        for (Entity target : entities) {
            if (target == this) continue;
            if (target.isAlive()) {
                target.hurt(this.level().damageSources().fall(), damage);
            }
        }
    }

    private void shatterBrittleBlocks() {
        // Shatter breakable blocks at the impact position
        BlockPos impact = BlockPos.containing(getX(), getY() - 0.5, getZ());
        int radius = 2;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = impact.offset(dx, dy, dz);
                    BlockState state = this.level().getBlockState(pos);
                    Block block = state.getBlock();

                    // Brittle blocks: glass (by registry name), ice, leaves
                    boolean brittle = isBrittle(block);
                    if (!brittle) continue;

                    // Chance to shatter based on fall speed
                    double speed = Math.sqrt(fallX * fallX + fallY * fallY + fallZ * fallZ);
                    if (this.level().random.nextDouble() < speed * 0.5) {
                        this.level().removeBlock(pos, false);
                        block.popResource(this.level(), pos, new ItemStack(block));
                    }
                }
            }
        }
    }

    private boolean isBrittle(Block block) {
        String id = BuiltInRegistries.BLOCK.getKey(block).toString();
        return id.contains("glass")
            || id.contains("ice")
            || block instanceof IceBlock
            || block instanceof LeavesBlock;
    }

    private void dropAllItems() {
        // Drop items using vanilla mechanics
        for (BlockStateAndPos bsp : blocks) {
            BlockState state = bsp.state();
            Block block = state.getBlock();
            Item item = block.asItem();
            ItemStack itemStack = item.getDefaultInstance();
            if (!itemStack.isEmpty()) {
                ItemStack drop = itemStack.copyWithCount(1);
                ItemEntity itemEntity = new ItemEntity(
                        this.level(), getX(), getY() + 1.0, getZ(), drop);
                itemEntity.setPickUpDelay(10);
                this.level().addFreshEntity(itemEntity);
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No synced data needed - entities are spawned fresh
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // No saved data needed - entities are spawned fresh
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        // No saved data needed
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    /** Record of a single block in the falling tree. */
    public record BlockStateAndPos(BlockPos pos, BlockState state) {}
}
