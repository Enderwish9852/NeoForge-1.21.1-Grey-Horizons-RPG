package net.enderwish.Farming_Overhaul_Subpack.entity;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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

    private double fallX, fallY, fallZ;      // velocity
    private int ticksFalling = 0;
    private boolean hasLanded = false;
    private int landingTicks = 0;

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

    @Override
    public void tick() {
        super.tick();

        if (hasLanded) {
            landingTicks++;
            if (landingTicks > 5) {
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
