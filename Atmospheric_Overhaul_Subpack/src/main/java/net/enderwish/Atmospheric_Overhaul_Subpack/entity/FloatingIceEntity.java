package net.enderwish.Atmospheric_Overhaul_Subpack.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

/**
 * FloatingIceEntity
 *
 * A clump of ice that has broken off a frozen river's surface and is
 * drifting away on the current.
 *
 * It is the other half of the ice: a river does not simply lose ice
 * to melting, it cracks, and the pieces float off. So when the ice
 * thins from below -- which is how a sheet of ice on water really
 * goes, because the water underneath is warmer than the air -- the
 * bottom goes first and what is left breaks off and drifts away as
 * one of these.
 *
 * There are three sizes, and they are not the same thing with
 * different numbers:
 *
 *   SMALL  -- the piece that comes off the edge. Light, quick to
 *             melt, and it shatters on the first thing it hits.
 *   MEDIUM -- heavier, slower to melt, slower to drift, and it takes
 *             a bite out of the sheet as it comes off.
 *   LARGE  -- a raft. It takes a great many blocks with it, drifts
 *             very slowly, and rather than shattering it breaks the
 *             ice it runs into -- and usually comes apart into a
 *             medium and a couple of smalls when it does.
 */
public class FloatingIceEntity extends Entity {

    /**
     * The three sizes.
     *
     * A size is a different kind of thing, not a bigger version of
     * the same thing: it melts at its own rate, drifts at its own
     * rate, survives a different number of collisions, and takes a
     * different amount of the sheet with it.
     */
    public enum Kind {
        SMALL(3, 5, 45 * 20, 1, 1.0f, 0),
        MEDIUM(6, 9, 90 * 20, 2, 0.55f, 4),
        LARGE(12, 18, 180 * 20, Integer.MAX_VALUE, 0.25f, 13);

        /** The fewest and most blocks a clump of this size holds. */
        public final int minBlocks;
        public final int maxBlocks;
        /** How long it takes to melt, in ticks. */
        public final int meltTicks;
        /** How many collisions it survives. The large one never shatters from a hit. */
        public final int hitsToShatter;
        /** How fast the current carries it, against the small one's. */
        public final float driftScale;
        /** How many blocks of ice it takes with it as it comes off. */
        public final int iceTaken;

        Kind(int minBlocks, int maxBlocks, int meltTicks, int hitsToShatter,
             float driftScale, int iceTaken) {
            this.minBlocks = minBlocks;
            this.maxBlocks = maxBlocks;
            this.meltTicks = meltTicks;
            this.hitsToShatter = hitsToShatter;
            this.driftScale = driftScale;
            this.iceTaken = iceTaken;
        }

        public static Kind byIndex(int index) {
            Kind[] all = values();
            return index >= 0 && index < all.length ? all[index] : SMALL;
        }
    }

    private static final EntityDataAccessor<Integer> DATA_CLUMP_SIZE =
            SynchedEntityData.defineId(FloatingIceEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_KIND =
            SynchedEntityData.defineId(FloatingIceEntity.class, EntityDataSerializers.INT);

    /**
     * How long a new floe is allowed to survive out of the water.
     *
     * Without it, a floe summoned into the air melts on its very first
     * tick and there is nothing at all to look at.
     */
    private static final int GRACE_TICKS = 60;
    /** The speed it has to be moving at to shatter on a bank. */
    private static final float SHATTER_SPEED = 0.28f;
    /** The chance a large floe comes apart when it breaks ice. */
    private static final float SPLIT_CHANCE = 0.6f;

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("GHAtmospheric/FloatingIce");

    private int life;
    private float bobPhase;
    private float driftX;
    private float driftZ;
    /** How many things it has hit and survived. */
    private int hits;

    public FloatingIceEntity(EntityType<? extends FloatingIceEntity> type, Level level) {
        super(type, level);
        this.bobPhase = (float) (this.random.nextDouble() * Math.PI * 2.0);
        Kind kind = Kind.values()[this.random.nextInt(Kind.values().length)];
        this.entityData.set(DATA_KIND, kind.ordinal());
        this.entityData.set(DATA_CLUMP_SIZE,
                kind.minBlocks + this.random.nextInt(kind.maxBlocks - kind.minBlocks + 1));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CLUMP_SIZE, 3);
        builder.define(DATA_KIND, Kind.SMALL.ordinal());
    }

    /** How many blocks the clump is made of. */
    public int getClumpSize() {
        return this.entityData.get(DATA_CLUMP_SIZE);
    }

    /** Which size this one is. */
    public Kind getKind() {
        return Kind.byIndex(this.entityData.get(DATA_KIND));
    }

    /**
     * A large floe is a raft wide enough to stand on, so it needs a
     * box to match, or it will be culled out of the world while it
     * is still plainly in view.
     */
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        Kind kind = this.getKind();
        float width;
        float height;
        switch (kind) {
            case LARGE -> {
                width = 2.4f;
                height = 0.55f;
            }
            case MEDIUM -> {
                width = 1.5f;
                height = 0.45f;
            }
            default -> {
                width = 0.9f;
                height = 0.4f;
            }
        }
        return EntityDimensions.scalable(width, height);
    }

    @Override
    public void tick() {
        super.tick();

        this.life++;
        Kind kind = this.getKind();
        if (this.life > kind.meltTicks) {
            this.meltAway("lived out its time");
            return;
        }

        // The server moves it, and the server alone decides what
        // happens to it. If the client ran the same rules it could
        // shatter a floe the server still had, or drift one the other
        // way, and the two would argue about it every tick.
        if (this.level().isClientSide) return;

        BlockPos pos = this.blockPosition();
        FluidState fluid = this.level().getFluidState(pos);

        if (!fluid.is(Fluids.WATER)) {
            // it has drifted out of the water, or the water has frozen
            // over it: it melts. But not straight away, or a floe
            // summoned into the air is gone before it can be seen.
            if (this.life > GRACE_TICKS) {
                this.meltAway("out of the water");
            }
            return;
        }

        // the current carries it, and the heavier ones are slower
        Vec3 flow = fluid.getFlow(this.level(), pos);
        float scale = kind.driftScale;
        this.driftX += (float) (flow.x * scale - this.driftX) * 0.1f;
        this.driftZ += (float) (flow.z * scale - this.driftZ) * 0.1f;

        double speed = Math.sqrt(this.driftX * this.driftX + this.driftZ * this.driftZ);

        // it rides the surface of the water
        double surface = pos.getY() + fluid.getHeight(this.level(), pos);
        double y = surface + 0.06;

        double nx = this.getX() + this.driftX;
        double nz = this.getZ() + this.driftZ;

        BlockPos ahead = BlockPos.containing(nx, y, nz);
        if (this.level().getBlockState(ahead).blocksMotion()) {
            this.onCollision(ahead, speed, kind);
            return;
        }

        this.setPos(nx, y, nz);
        this.setDeltaMovement(Vec3.ZERO);
    }

    /**
     * It has run into something.
     *
     * The small one shatters at once. The medium takes two hits. The
     * large one does not shatter at all: it breaks the ice it runs
     * into, and usually comes apart in the process.
     */
    private void onCollision(BlockPos hit, double speed, Kind kind) {
        if (kind == Kind.LARGE) {
            if (this.level().getBlockState(hit).is(net.minecraft.world.level.block.Blocks.ICE)) {
                // it breaks the ice
                this.level().setBlockAndUpdate(hit,
                        net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
                if (this.random.nextFloat() < SPLIT_CHANCE) {
                    this.split();
                    return;
                }
            }
            // it cannot break anything else, so it just stops
            this.driftX = 0.0f;
            this.driftZ = 0.0f;
            return;
        }

        this.hits++;
        if (this.hits >= kind.hitsToShatter) {
            this.shatter();
        } else {
            // it holds together, but it stops
            this.driftX = 0.0f;
            this.driftZ = 0.0f;
        }
    }

    /** The large one comes apart into a medium and a couple of smalls. */
    private void split() {
        if (!(this.level() instanceof ServerLevel server)) {
            this.discard();
            return;
        }
        LOGGER.info("floating ice at {} broke down into a medium and two smalls", this.blockPosition());
        server.sendParticles(ParticleTypes.SNOWFLAKE,
                this.getX(), this.getY() + 0.3, this.getZ(), 14, 0.3, 0.2, 0.3, 0.03);
        FloatingIceEntity.spawn(server, this.getX(), this.getY(), this.getZ(), Kind.MEDIUM);
        FloatingIceEntity.spawn(server, this.getX() + 0.4, this.getY(), this.getZ(), Kind.SMALL);
        FloatingIceEntity.spawn(server, this.getX() - 0.4, this.getY(), this.getZ() + 0.3, Kind.SMALL);
        this.discard();
    }

    /** It melts: a puff of white and it is gone. */
    private void meltAway(String why) {
        LOGGER.info("floating ice at {} melted: {}", this.blockPosition(), why);
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SNOWFLAKE,
                    this.getX(), this.getY() + 0.2, this.getZ(), 6, 0.15, 0.1, 0.15, 0.02);
        }
        this.discard();
    }

    /** It hits a bank too hard and breaks apart. */
    private void shatter() {
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SNOWFLAKE,
                    this.getX(), this.getY() + 0.2, this.getZ(), 12, 0.2, 0.15, 0.2, 0.03);
            server.playSound(null, this.blockPosition(),
                    SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.6f, 0.8f);
        }
        this.discard();
    }

    /** Puts a floe of a given size into the world. */
    public static FloatingIceEntity spawn(ServerLevel level, double x, double y, double z, Kind kind) {
        FloatingIceEntity ice = new FloatingIceEntity(ModEntities.FLOATING_ICE.get(), level);
        ice.entityData.set(DATA_KIND, kind.ordinal());
        ice.entityData.set(DATA_CLUMP_SIZE,
                kind.minBlocks + level.getRandom().nextInt(kind.maxBlocks - kind.minBlocks + 1));
        ice.setPos(x, y, z);
        level.addFreshEntity(ice);
        return ice;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.life = tag.getInt("Life");
        if (tag.contains("Kind")) {
            this.entityData.set(DATA_KIND, tag.getInt("Kind"));
        }
        if (tag.contains("Blocks")) {
            this.entityData.set(DATA_CLUMP_SIZE, tag.getInt("Blocks"));
        }
        this.refreshDimensions();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", this.life);
        tag.putInt("Kind", this.entityData.get(DATA_KIND));
        tag.putInt("Blocks", this.entityData.get(DATA_CLUMP_SIZE));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0 * 128.0;
    }
}
