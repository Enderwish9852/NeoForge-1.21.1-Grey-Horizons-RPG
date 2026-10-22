package net.enderwish.Atmospheric_Overhaul_Subpack.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.enderwish.Atmospheric_Overhaul_Subpack.entity.FloatingIceEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * FloatingIceRenderer
 *
 * Draws the floating ice as a clump of broken slabs rather than one
 * cube: a sheet of ice that has broken up comes away in pieces, and
 * a single block would read as a floating cube.
 *
 * The three sizes are drawn as three different things, because they
 * are three different things:
 *
 *   SMALL  -- a handful of jagged chips, tipped at every angle. It
 *             is a fragment, not a piece of sheet.
 *   MEDIUM -- a slab that has come apart, with a second course of
 *             pieces lying on top of it and one shard standing up.
 *   LARGE  -- a raft, broad and flat and stacked in three courses,
 *             with shards standing out of it the way a pressure
 *             ridge does.
 *
 * The shape of a floe is fixed for its whole life: the same seed
 * every frame, so the raft does not crawl about, and every floe is
 * different from every other one.
 */
public class FloatingIceRenderer extends EntityRenderer<FloatingIceEntity> {

    private final BlockRenderDispatcher blockRenderer;

    public FloatingIceRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(FloatingIceEntity entity, float yaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        FloatingIceEntity.Kind kind = entity.getKind();
        int size = entity.getClumpSize();

        // the same every frame, so the raft does not crawl about
        Rng rng = new Rng(entity.getUUID().hashCode());

        poseStack.pushPose();

        // it rocks on the swell -- but a big raft hardly notices
        float rock = switch (kind) {
            case LARGE -> 0.3f;
            case MEDIUM -> 0.6f;
            default -> 1.0f;
        };
        float bob = (float) Math.sin(entity.tickCount * 0.08f) * 0.06f * rock;
        poseStack.translate(0.0, bob, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                (float) Math.sin(entity.tickCount * 0.04f) * 4.0f * rock));

        var ice = Blocks.ICE.defaultBlockState();

        switch (kind) {
            case LARGE -> {
                // a broad flat raft in three courses, with shards
                // standing out of it
                int base = Math.max(5, size * 55 / 100);
                int middle = Math.max(3, size * 30 / 100);
                int top = Math.max(2, size - base - middle);
                this.raft(base, 1.25f, 0.0f, 0.55f, 5.0f, rng, poseStack, buffer, packedLight, ice);
                this.raft(middle, 0.85f, 0.30f, 0.50f, 7.0f, rng, poseStack, buffer, packedLight, ice);
                this.raft(top, 0.45f, 0.56f, 0.45f, 9.0f, rng, poseStack, buffer, packedLight, ice);
                this.shards(3, 1.15f, rng, poseStack, buffer, packedLight, ice);
            }
            case MEDIUM -> {
                // a slab that has come apart, with a second course
                // lying on it and one shard standing up
                int base = Math.max(3, size * 65 / 100);
                int top = Math.max(1, size - base);
                this.raft(base, 0.70f, 0.0f, 0.50f, 11.0f, rng, poseStack, buffer, packedLight, ice);
                this.raft(top, 0.38f, 0.27f, 0.45f, 14.0f, rng, poseStack, buffer, packedLight, ice);
                this.shards(1, 0.62f, rng, poseStack, buffer, packedLight, ice);
            }
            default -> {
                // a handful of jagged chips
                this.raft(size, 0.24f, 0.0f, 0.45f, 20.0f, rng, poseStack, buffer, packedLight, ice);
            }
        }

        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    /**
     * A course of slabs.
     *
     * They are spread evenly round a circle so the raft has an edge
     * and a middle, and every one of them is turned a little and
     * tipped a little, because a sheet that has broken up does not
     * break into a tidy grid.
     */
    private void raft(int count, float radius, float y, float thickness, float tiltMax,
                      Rng rng, PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                      BlockState ice) {
        if (count <= 0) return;
        for (int i = 0; i < count; i++) {
            float angle = (float) (Math.PI * 2.0 * i / count) + rng.range(-0.22f, 0.22f);
            float r = radius * (i % 2 == 0 ? 1.0f : 0.55f);
            float x = (float) Math.cos(angle) * r;
            float z = (float) Math.sin(angle) * r;
            float scale = rng.range(0.72f, 0.98f);

            poseStack.pushPose();
            poseStack.translate(x, y + rng.range(-0.04f, 0.04f), z);
            poseStack.mulPose(Axis.YP.rotationDegrees(rng.range(0.0f, 360.0f)));
            poseStack.mulPose(Axis.XP.rotationDegrees(rng.range(-tiltMax, tiltMax)));
            poseStack.mulPose(Axis.ZP.rotationDegrees(rng.range(-tiltMax * 0.6f, tiltMax * 0.6f)));
            poseStack.scale(0.85f * scale, thickness, 0.85f * scale);
            this.blockRenderer.renderSingleBlock(ice, poseStack, buffer, packedLight,
                    OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    /**
     * Shards standing up out of the raft.
     *
     * This is the thing that makes ice break read as ice break: the
     * edges of a sheet that is tearing apart turn up on end.
     */
    private void shards(int count, float radius, Rng rng, PoseStack poseStack,
                        MultiBufferSource buffer, int packedLight,
                        BlockState ice) {
        for (int i = 0; i < count; i++) {
            float angle = rng.range(0.0f, (float) (Math.PI * 2.0));
            float r = radius * rng.range(0.6f, 1.0f);
            float x = (float) Math.cos(angle) * r;
            float z = (float) Math.sin(angle) * r;

            poseStack.pushPose();
            poseStack.translate(x, 0.12f, z);
            poseStack.mulPose(Axis.YP.rotationDegrees(rng.range(0.0f, 360.0f)));
            poseStack.mulPose(Axis.XP.rotationDegrees(rng.range(8.0f, 24.0f)));
            poseStack.scale(rng.range(0.38f, 0.55f), rng.range(1.2f, 1.7f), rng.range(0.38f, 0.55f));
            this.blockRenderer.renderSingleBlock(ice, poseStack, buffer, packedLight,
                    OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(FloatingIceEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    /**
     * A very small random number generator.
     *
     * Seeded from the floe's own id, so a floe looks the same every
     * time it is drawn -- but no two floes look the same.
     */
    private static final class Rng {
        private long state;

        Rng(long seed) {
            this.state = seed * 6364136223846793005L + 1442695040888963407L;
        }

        float next() {
            long x = this.state;
            x ^= x << 13;
            x ^= x >>> 7;
            x ^= x << 17;
            this.state = x;
            return ((x >>> 11) & 0xFFFFFF) / (float) 0x1000000;
        }

        float range(float low, float high) {
            return low + (high - low) * this.next();
        }
    }
}
