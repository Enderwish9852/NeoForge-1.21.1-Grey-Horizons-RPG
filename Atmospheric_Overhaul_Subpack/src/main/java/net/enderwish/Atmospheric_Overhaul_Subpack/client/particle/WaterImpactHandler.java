package net.enderwish.Atmospheric_Overhaul_Subpack.client.particle;

import net.enderwish.Atmospheric_Overhaul_Subpack.AtmosphericOverhaulSubpack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * WaterImpactHandler
 *
 * NEW -- water ripples are EVENT-DRIVEN: a ring appears on the surface
 * exactly when something crosses it, and never otherwise.
 *
 * The old SkyRenderHandler spawned one every 4 ticks at a random spot
 * within 14 blocks of the player, which is why ripples used to pop in
 * all over the water with nothing having touched it.
 *
 * There is no NeoForge event for "an entity entered a fluid" in 1.21.1
 * (checked the whole event package: the fluid events cover placing and
 * creating fluid blocks, not entities entering them), so the crossing
 * is detected on the client:
 *
 *   - every tick, each nearby entity is tested against the water
 *     surface at its own column;
 *   - the previous tick's answer is kept per entity id;
 *   - a false -> true flip means the entity has just crossed the
 *     surface going down, which is an impact, and a ring is spawned at
 *     that point.
 *
 * Doing it on the client (rather than on the server) means the ring
 * appears on the very tick of the impact, with no packet round trip.
 *
 * The strength of the ring comes from how fast the body was falling
 * and how big it is, so a dropped item makes a small ring and a cow
 * falling in makes a big one. The local player is tracked separately,
 * because the client does not render its own player in the entity
 * list.
 */
@EventBusSubscriber(modid = AtmosphericOverhaulSubpack.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class WaterImpactHandler {

    /** Only entities this close to the player can make a visible ring. */
    private static final double MAX_DISTANCE = 40.0;

    /** How far above the surface the ring is drawn, to beat z-fighting. */
    private static final double SURFACE_OFFSET = 0.02;

    /** entity id -> was its body below the water surface on the last tick. */
    private static final Map<Integer, Boolean> BELOW_SURFACE = new HashMap<>();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;

        Map<Integer, Boolean> next = new HashMap<>();

        for (Entity entity : level.entitiesForRendering()) {
            if (entity == mc.player) continue;
            if (entity.distanceToSqr(mc.player) > MAX_DISTANCE * MAX_DISTANCE) continue;
            track(level, entity, next);
        }
        // the client does not list its own player among the rendered entities
        track(level, mc.player, next);

        BELOW_SURFACE.clear();
        BELOW_SURFACE.putAll(next);
    }

    /** Records the entity's state and spawns a ring if it just hit the water. */
    private static void track(ClientLevel level, Entity entity, Map<Integer, Boolean> next) {
        double surface = waterSurfaceY(level, entity);
        boolean below = !Double.isNaN(surface);

        Boolean previous = BELOW_SURFACE.get(entity.getId());
        next.put(entity.getId(), below);

        // only a fresh crossing counts: first sighting, still inside, or
        // already inside last tick are all "nothing happened"
        if (previous == null || previous || !below) return;

        float fallSpeed = Math.max(0.0f, (float) -entity.getDeltaMovement().y);
        float size = entity.getBbHeight() > 0.05f ? entity.getBbHeight() : 1.0f;
        float strength = Mth.clamp(fallSpeed * 0.22f + size * 0.18f, 0.24f, 1.5f);

        spawn(level, entity.getX(), surface, entity.getZ(), strength);
    }

    /**
     * The water surface Y at the entity's own column, or NaN when the
     * entity's body is not below a water surface.
     */
    private static double waterSurfaceY(ClientLevel level, Entity entity) {
        int bx = Mth.floor(entity.getX());
        int bz = Mth.floor(entity.getZ());
        int by = Mth.floor(entity.getY());
        for (int dy = 0; dy >= -2; dy--) {
            BlockPos pos = new BlockPos(bx, by + dy, bz);
            FluidState fluid = level.getFluidState(pos);
            if (fluid.is(Fluids.WATER)) {
                double surface = pos.getY() + fluid.getHeight(level, pos);
                return entity.getY() < surface ? surface : Double.NaN;
            }
        }
        return Double.NaN;
    }

    /** Spawns the ring on the surface. The strength travels in dz. */
    public static void spawn(ClientLevel level, double x, double surfaceY, double z, float strength) {
        level.addParticle(ModParticles.WATER_RIPPLE.get(), x, surfaceY + SURFACE_OFFSET, z, 0.0, 0.0, strength);
    }
}
