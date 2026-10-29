package net.enderwish.Farming_Overhaul_Subpack.entity;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.block.tree.FruitTreeLeavesBlock;
import net.enderwish.Farming_Overhaul_Subpack.block.tree.FruitTreeLogBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

/**
 * FallingTreeEntity
 *
 * Represents a felled tree falling through the air. Created when a player
 * cuts a log in the trunk column - the whole tree (logs + leaves above the
 * cut) becomes this entity and falls under gravity.
 *
 * Two entity types:
 *   - Trunk entity (isTrunk = true): falls with rotation/tilt toward fall
 *     direction (fall-over animation). On landing, places log blocks back
 *     into the world as a fallen trunk lying horizontal - player mines them
 *     one by one for wood.
 *   - Branch entity (isTrunk = false): falls straighter. On landing, branches
 *     snap into stick items scattered around.
 *
 * Leaves decay while falling (drop sticks/saplings in mid-air) and ALL
 * remaining leaves decay on landing.
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
    private static final double ROTATION_SPEED = 0.06;

    /** Maximum tilt angle while falling (radians) — trunk tilts toward fall direction. */
    private static final double MAX_TILT = 1.4; // ~80 degrees

    /** Minimum fall speed (blocks per tick) to cause damage on impact. */
    private static final double DAMAGE_THRESHOLD = 0.3;

    /** Chance (0-1) per leaf-decay tick for a leaf to drop a sapling. */
    private static final double SAPLING_DROP_CHANCE = 0.05;

    /** Chance (0-1) per leaf-decay tick for a leaf to drop a stick. */
    private static final double STICK_DROP_CHANCE = 0.5;

    /** How often (ticks) leaves decay while falling. */
    private static final int LEAF_DECAY_INTERVAL = 3;

    /** Chain reaction radius (blocks). Trees within this distance may fall. */
    private static final int CHAIN_REACTION_RADIUS = 8;

    /** Trees with fewer blocks than this are knocked over entirely. */
    private static final int SMALL_TREE_THRESHOLD = 12;

    /** Record of a single block in the falling tree. */
    public record BlockPosAndPos(BlockPos pos, BlockState state) {}

    private double fallX, fallY, fallZ;      // velocity
    private int ticksFalling = 0;
    private boolean hasLanded = false;
    private int landingTicks = 0;

    /** Yaw angle of the fall direction (radians). */
    private double fallYaw = 0.0;

    /** Current tilt angle while falling (0 = upright, MAX_TILT = horizontal). */
    private double tilt = 0.0;

    /** Height fallen since creation (blocks). */
    private double fallDistance = 0.0;

    /** Whether this tree has already dealt impact damage. */
    private boolean damageDealt = false;

    /** Whether this is the main trunk entity (true) or a branch entity (false). */
    private boolean isTrunk = false;

    /** Random for sound variation and item scatter. */
    private final Random random = new Random();

    /** Ticks since last leaf decay. */
    private int ticksSinceLeafDecay = 0;

    /** All block positions and states that make up this falling tree. */
    private final List<BlockPosAndPos> blocks = new ArrayList<>();

    /** Indices of blocks that are leaves (for decay while falling). */
    private final List<Integer> leafIndices = new ArrayList<>();

    public FallingTreeEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    /** Add a block to the falling tree. */
    public void addBlock(BlockPos pos, BlockState state) {
        if (blocks.size() >= MAX_BLOCKS) return;
        int idx = blocks.size();
        blocks.add(new BlockPosAndPos(pos, state));
        if (state.getBlock() instanceof LeavesBlock) {
            leafIndices.add(idx);
        }
    }

    /** Number of blocks in the tree. */
    public int blockCount() { return blocks.size(); }

    /** Set whether this is the main trunk entity (true) or a branch entity (false). */
    public void setTrunk(boolean trunk) { this.isTrunk = trunk; }

    /** Set the initial fall velocity and direction. */
    public void setFallDirection(double vx, double vy, double vz) {
        this.fallX = vx;
        this.fallY = vy;
        this.fallZ = vz;
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

        // Trunk entity tilts toward fall direction as it falls (fall-over animation)
        if (isTrunk) {
            double progress = Math.min(1.0, ticksFalling / 20.0); // reach full tilt over 20 ticks
            tilt = MAX_TILT * progress;
        }

        // Periodically decay leaves while falling (drop sticks/saplings in mid-air)
        ticksSinceLeafDecay++;
        if (ticksSinceLeafDecay >= LEAF_DECAY_INTERVAL) {
            ticksSinceLeafDecay = 0;
            decaySomeLeaves();
        }

        // Move the entity
        double nx = getX() + fallX;
        double ny = getY() + fallY;
        double nz = getZ() + fallZ;

        // Check collision at new position
        AABB box = getBoundingBox().move(fallX, fallY, fallZ);
        if (this.level().noCollision(this, box)) {
            setPos(nx, ny, nz);
            // Rotate entity to match fall direction while in the air
            setYRot((float) (fallYaw + Math.PI));
            if (isTrunk) {
                setXRot((float) (-tilt));
            }
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

        // Play crash sound (volume scales with fall speed)
        double speed = Math.sqrt(fallX * fallX + fallY * fallY + fallZ * fallZ);
        float volume = Math.min(1.0f, (float) (speed * 2.0));
        float pitch = 0.7f + random.nextFloat() * 0.3f;
        this.level().playSound(null, getX(), getY(), getZ(),
                SoundEvents.WOOD_BREAK,
                SoundSource.BLOCKS,
                volume, pitch);

        // Deal impact damage to nearby entities
        dealImpactDamage();

        // Shatter brittle blocks at impact
        shatterBrittleBlocks();

        // Decay ALL remaining leaves on landing (any that survived mid-fall decay)
        decayAllLeaves();

        // Trunk: place blocks back in world as fallen trunk.
        // Branches: snap into stick items scattered around.
        if (isTrunk) {
            placeBlocksOnGround();
        } else {
            dropItemsScattered();
        }

        // Chain reaction: nearby trees may fall
        triggerChainReaction();
    }

    /** Decay a few leaves while falling — drop sticks/saplings in mid-air. */
    private void decaySomeLeaves() {
        int toDecay = Math.min(3, leafIndices.size());
        for (int i = 0; i < toDecay && !leafIndices.isEmpty(); i++) {
            int idx = leafIndices.remove(leafIndices.size() - 1);
            BlockPosAndPos bsp = blocks.get(idx);
            BlockState state = bsp.state();
            Block block = state.getBlock();
            BlockPos pos = bsp.pos();

            // Remove the leaf block from the world while falling
            if (this.level().getBlockState(pos).getBlock() instanceof LeavesBlock) {
                this.level().removeBlock(pos, false);
            }

            // Drop a stick
            if (this.level().random.nextDouble() < STICK_DROP_CHANCE) {
                ItemStack stick = new ItemStack(Items.STICK);
                ItemEntity stickEntity = new ItemEntity(
                        this.level(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stick);
                stickEntity.setPickUpDelay(10);
                this.level().addFreshEntity(stickEntity);
            }

            // Drop a sapling
            if (this.level().random.nextDouble() < SAPLING_DROP_CHANCE) {
                Item sapling = getSaplingForLeaves(block);
                if (sapling != null) {
                    ItemStack saplingStack = new ItemStack(sapling);
                    ItemEntity saplingEntity = new ItemEntity(
                            this.level(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, saplingStack);
                    saplingEntity.setPickUpDelay(10);
                    this.level().addFreshEntity(saplingEntity);
                }
            }
        }
    }

    /** Decay ALL remaining leaves on landing. */
    private void decayAllLeaves() {
        for (int idx : new ArrayList<>(leafIndices)) {
            BlockPosAndPos bsp = blocks.get(idx);
            BlockState state = bsp.state();
            Block block = state.getBlock();
            BlockPos pos = bsp.pos();

            if (this.level().getBlockState(pos).getBlock() instanceof LeavesBlock) {
                this.level().removeBlock(pos, false);
            }

            // Drop a stick
            if (this.level().random.nextDouble() < STICK_DROP_CHANCE) {
                ItemStack stick = new ItemStack(Items.STICK);
                ItemEntity stickEntity = new ItemEntity(
                        this.level(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stick);
                stickEntity.setPickUpDelay(10);
                this.level().addFreshEntity(stickEntity);
            }

            // Drop a sapling
            if (this.level().random.nextDouble() < SAPLING_DROP_CHANCE) {
                Item sapling = getSaplingForLeaves(block);
                if (sapling != null) {
                    ItemStack saplingStack = new ItemStack(sapling);
                    ItemEntity saplingEntity = new ItemEntity(
                            this.level(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, saplingStack);
                    saplingEntity.setPickUpDelay(10);
                    this.level().addFreshEntity(saplingEntity);
                }
            }
        }
        leafIndices.clear();
    }

    private Item getSaplingForLeaves(Block leavesBlock) {
        String id = BuiltInRegistries.BLOCK.getKey(leavesBlock).toString();
        if (id.contains("oak")) return Items.OAK_SAPLING;
        if (id.contains("spruce")) return Items.SPRUCE_SAPLING;
        if (id.contains("birch")) return Items.BIRCH_SAPLING;
        if (id.contains("jungle")) return Items.JUNGLE_SAPLING;
        if (id.contains("acacia")) return Items.ACACIA_SAPLING;
        if (id.contains("dark_oak")) return Items.DARK_OAK_SAPLING;
        if (id.contains("mangrove")) return Items.MANGROVE_PROPAGULE;
        if (id.contains("cherry")) return Items.CHERRY_SAPLING;
        return null;
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
        BlockPos impact = BlockPos.containing(getX(), getY() - 0.5, getZ());
        int radius = 2;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = impact.offset(dx, dy, dz);
                    BlockState state = this.level().getBlockState(pos);
                    Block block = state.getBlock();

                    if (!isBrittle(block)) continue;

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

    /**
     * Trunk lands: place log blocks back into the world at rotated positions.
     * The trunk lies horizontal, pointing in the fall direction.
     * Player mines these blocks one by one to get wood.
     */
    private void placeBlocksOnGround() {
        BlockPos impact = BlockPos.containing(getX(), getY() - 0.5, getZ());
        double cx = impact.getX() + 0.5;
        double groundY = impact.getY(); // Entity sits ON ground, ground is at impact Y
        double cz = impact.getZ() + 0.5;

        // Sort log blocks by original Y so we place from bottom of trunk up
        List<Integer> logIndices = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            if (!leafIndices.contains(i)) {
                logIndices.add(i);
            }
        }
        logIndices.sort((a, b) -> Integer.compare(
                blocks.get(a).pos().getY(), blocks.get(b).pos().getY()));

        double cosYaw = Math.cos(fallYaw);
        double sinYaw = Math.sin(fallYaw);

        for (int idx : logIndices) {
            BlockPosAndPos bsp = blocks.get(idx);
            BlockState state = bsp.state();
            BlockPos origPos = bsp.pos();
            Block block = state.getBlock();

            // Offset from cut block (blocks.get(0) = the block player cut)
            double relX = origPos.getX() - blocks.get(0).pos().getX();
            double relY = origPos.getY() - blocks.get(0).pos().getY();
            double relZ = origPos.getZ() - blocks.get(0).pos().getZ();

            // Rotate: trunk lies horizontal pointing along fall direction
            // Blocks above the cut (positive relY) extend outward in fall direction
            double rotX = relY * cosYaw;
            double rotZ = relY * sinYaw;

            double finalX = cx + rotX + (relX - Math.round(relX)) * 0.5;
            double finalY = groundY; // sit ON the ground block
            double finalZ = cz + rotZ + (relZ - Math.round(relZ)) * 0.5;

            // Rotate block axis to match fall direction
            BlockState placeState = state;
            if (block instanceof FruitTreeLogBlock) {
                Direction.Axis axis = state.getValue(FruitTreeLogBlock.AXIS);
                Direction.Axis newAxis = axis == Direction.Axis.Y
                        ? (Math.abs(fallX) > Math.abs(fallZ) ? Direction.Axis.X : Direction.Axis.Z)
                        : axis;
                placeState = state.setValue(FruitTreeLogBlock.AXIS, newAxis);
            }

            BlockPos placePos = new BlockPos(
                    (int) Math.floor(finalX),
                    (int) Math.floor(finalY),
                    (int) Math.floor(finalZ)
            );

            BlockState existing = this.level().getBlockState(placePos);
            if (existing.isAir() || existing.canBeReplaced()) {
                this.level().setBlock(placePos, placeState, 3);
            } else {
                // Can't place — drop as item instead
                Item item = block.asItem();
                ItemStack itemStack = item.getDefaultInstance();
                if (!itemStack.isEmpty()) {
                    ItemStack drop = itemStack.copyWithCount(1);
                    ItemEntity itemEntity = new ItemEntity(
                            this.level(), finalX, finalY + 0.5, finalZ, drop);
                    itemEntity.setPickUpDelay(10);
                    this.level().addFreshEntity(itemEntity);
                }
            }
        }
    }

    /**
     * Branch lands: snap into stick items scattered around the landing point.
     * Thinner branches break apart on impact.
     */
    private void dropItemsScattered() {
        double cx = getX();
        double cy = getY() + 0.5;
        double cz = getZ();

        double speed = Math.sqrt(fallX * fallX + fallY * fallY + fallZ * fallZ);
        double scatterStrength = Math.min(1.0, speed * 0.8);

        for (int i = 0; i < blocks.size(); i++) {
            if (leafIndices.contains(i)) continue;

            BlockPosAndPos bsp = blocks.get(i);
            BlockState state = bsp.state();
            Block block = state.getBlock();
            Item item = block.asItem();
            ItemStack itemStack = item.getDefaultInstance();
            if (itemStack.isEmpty()) continue;

            ItemStack drop = itemStack.copyWithCount(1);

            double scatterX = (random.nextDouble() - 0.5) * 1.5 * scatterStrength;
            double scatterZ = (random.nextDouble() - 0.5) * 1.5 * scatterStrength;

            double dirBiasX = fallX * 1.5;
            double dirBiasZ = fallZ * 1.5;

            double px = cx + scatterX + dirBiasX;
            double py = cy + (random.nextDouble() * 0.8);
            double pz = cz + scatterZ + dirBiasZ;

            double vx = (random.nextDouble() - 0.5) * 0.4 + fallX * 0.3;
            double vy = random.nextDouble() * 0.4 + 0.2;
            double vz = (random.nextDouble() - 0.5) * 0.4 + fallZ * 0.3;

            ItemEntity itemEntity = new ItemEntity(this.level(), px, py, pz, drop);
            itemEntity.setPickUpDelay(10);
            itemEntity.setDeltaMovement(vx, vy, vz);
            this.level().addFreshEntity(itemEntity);
        }
    }

    /**
     * Chain reaction: scan for nearby trees and knock over small ones or damage large ones.
     * Uses TreeGrowthManager stored data when available for accurate detection.
     */
    private void triggerChainReaction() {
        BlockPos impact = BlockPos.containing(getX(), getY() - 0.5, getZ());

        Set<BlockPos> nearbyLogs = new HashSet<>();
        for (int dx = -CHAIN_REACTION_RADIUS; dx <= CHAIN_REACTION_RADIUS; dx++) {
            for (int dy = -4; dy <= 4; dy++) {
                for (int dz = -CHAIN_REACTION_RADIUS; dz <= CHAIN_REACTION_RADIUS; dz++) {
                    BlockPos pos = impact.offset(dx, dy, dz);
                    BlockState state = this.level().getBlockState(pos);
                    if (state.getBlock() instanceof FruitTreeLogBlock) {
                        nearbyLogs.add(pos);
                    }
                }
            }
        }

        if (nearbyLogs.isEmpty()) return;

        Set<BlockPos> visited = new HashSet<>();
        for (BlockPos start : nearbyLogs) {
            if (visited.contains(start)) continue;

            List<BlockPos> treeBlocks = new ArrayList<>();
            Queue<BlockPos> queue = new ArrayDeque<>();
            queue.add(start);
            visited.add(start);

            while (!queue.isEmpty()) {
                BlockPos current = queue.poll();
                treeBlocks.add(current);

                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.relative(dir);
                    if (visited.contains(neighbor)) continue;
                    if (!nearbyLogs.contains(neighbor)) continue;
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }

            if (treeBlocks.size() <= SMALL_TREE_THRESHOLD) {
                knockOverTree(treeBlocks);
            } else {
                damageLargeTree(treeBlocks);
            }
        }
    }

    private void knockOverTree(List<BlockPos> treeBlocks) {
        List<BlockPosAndPos> allBlocks = new ArrayList<>();
        Set<BlockPos> logPosSet = new HashSet<>(treeBlocks);

        for (BlockPos pos : treeBlocks) {
            BlockState state = this.level().getBlockState(pos);
            allBlocks.add(new BlockPosAndPos(pos, state));
        }

        for (BlockPos logPos : treeBlocks) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = logPos.relative(dir);
                if (logPosSet.contains(neighbor)) continue;
                BlockState state = this.level().getBlockState(neighbor);
                if (state.getBlock() instanceof FruitTreeLeavesBlock) {
                    allBlocks.add(new BlockPosAndPos(neighbor, state));
                    logPosSet.add(neighbor);
                }
            }
        }

        for (BlockPosAndPos bps : allBlocks) {
            this.level().removeBlock(bps.pos(), false);
        }

        double cx = 0, cy = 0, cz = 0;
        for (BlockPosAndPos bps : allBlocks) {
            cx += bps.pos().getX();
            cy += bps.pos().getY();
            cz += bps.pos().getZ();
        }
        int n = allBlocks.size();
        cx /= n; cy /= n; cz /= n;

        FallingTreeEntity entity = new FallingTreeEntity(
                net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack.FALLING_TREE.get(), this.level());
        entity.setPos(cx + 0.5, cy + 0.5, cz + 0.5);
        entity.setFallDirection(this.fallX * 0.6, this.fallY * 0.6, this.fallZ * 0.6);
        entity.setTrunk(true);

        for (BlockPosAndPos bps : allBlocks) {
            entity.addBlock(bps.pos(), bps.state());
        }

        this.level().addFreshEntity(entity);
    }

    private void damageLargeTree(List<BlockPos> treeBlocks) {
        int breakCount = Math.max(1, treeBlocks.size() / 5);
        int broken = 0;

        treeBlocks.sort((a, b) -> Integer.compare(b.getY(), a.getY()));

        for (BlockPos pos : treeBlocks) {
            if (broken >= breakCount) break;
            BlockState state = this.level().getBlockState(pos);
            if (!(state.getBlock() instanceof FruitTreeLogBlock)) continue;

            this.level().removeBlock(pos, false);
            state.getBlock().popResource(this.level(), pos, new ItemStack(state.getBlock()));
            broken++;
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
}
