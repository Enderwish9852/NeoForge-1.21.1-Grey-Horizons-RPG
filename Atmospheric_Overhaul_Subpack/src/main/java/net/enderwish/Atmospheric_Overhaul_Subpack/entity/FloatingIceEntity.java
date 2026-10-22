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
import net.minecraft.world.entity.EntityType;
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
 * It follows the water's flow, bobs on the swell, and shatters if it
 * is driven into a bank hard enough.
 */
public class FloatingIceEntity extends Entity {

    private static final EntityDataAccessor<Integer> DATA_CLUMP_SIZE =
            SynchedEntityData.defineId(FloatingIceEntity.class, EntityDataSerializers.INT);

    /** How long it lasts before it melts away, in ticks. */
    private static final int LIFETIME = 90 * 20;
    /** The speed it has to be moving at to shatter on a bank. */
    private static final float SHATTER_SPEED = 0.28f;

    private int life;
    private float bobPhase;
    private float driftX;
    private float driftZ;

    public FloatingIceEntity(EntityType<? extends FloatingIceEntity> type, Level level) {
        super(type, level);
        this.bobPhase = (float) (this.random.nextDouble() * Math.PI * 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CLUMP_SIZE, 2 + this.random.nextInt(4));
    }

    /** How many blocks the clump is made of. */
    public int getClumpSize() {
        return this.entityData.get(DATA_CLUMP_SIZE);
    }

    @Override
    public void tick() {
        super.tick();

        this.life++;
        if (this.life > LIFETIME) {
            this.meltAway();
            return;
        }

        BlockPos pos = this.blockPosition();
        FluidState fluid = this.level().getFluidState(pos);

        if (!fluid.is(Fluids.WATER)) {
            // it has drifted out of the water, or the water has frozen
            // over it: it melts
            this.meltAway();
            return;
        }

        // the current carries it
        Vec3 flow = fluid.getFlow(this.level(), pos);
        this.driftX += (float) (flow.x - this.driftX) * 0.1f;
        this.driftZ += (float) (flow.z - this.driftZ) * 0.1f;

        double speed = Math.sqrt(this.driftX * this.driftX + this.driftZ * this.driftZ);

        // it rides the surface of the water
        double surface = pos.getY() + fluid.getHeight(this.level(), pos);
        double y = surface + 0.06;

        double nx = this.getX() + this.driftX;
        double nz = this.getZ() + this.driftZ;

        // into a bank? at speed, it shatters
        if (speed > SHATTER_SPEED && this.level().getBlockState(
                BlockPos.containing(nx, y, nz)).blocksMotion()) {
            this.shatter();
            return;
        }

        this.setPos(nx, y, nz);
        this.setDeltaMovement(Vec3.ZERO);

        // the swell rocks it
        this.bobPhase += 0.08f;
        this.setPos(this.getX(), this.getY() + (float) Math.sin(this.bobPhase) * 0.01f, this.getZ());
    }

    /** It melts: a puff of white and it is gone. */
    private void meltAway() {
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

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.life = tag.getInt("Life");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", this.life);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0 * 128.0;
    }
}
