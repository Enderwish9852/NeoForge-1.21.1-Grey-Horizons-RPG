package net.enderwish.Atmospheric_Overhaul_Subpack.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.enderwish.Atmospheric_Overhaul_Subpack.entity.FloatingIceEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

/**
 * FloatingIceRenderer
 *
 * Draws the floating ice as a CLUMP of ice blocks rather than one
 * cube: a sheet of ice that has broken up comes away in slabs, and a
 * single block would read as a floating cube.
 *
 * The blocks are laid out in a shallow raft, the way a sheet breaks
 * up, and the whole thing rocks with the swell.
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
        poseStack.pushPose();

        // it rocks on the swell
        float bob = (float) Math.sin(entity.tickCount * 0.08f) * 0.06f;
        poseStack.translate(0.0, bob, 0.0);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(
                (float) Math.sin(entity.tickCount * 0.04f) * 4.0f));

        int size = entity.getClumpSize();
        var ice = Blocks.ICE.defaultBlockState();

        // The raft grows with the size: a small piece is a tight
        // cluster, a medium one is a slab, and a large one is a raft
        // wide enough to stand on.
        float radius = 0.16f + size * 0.06f;
        // the bigger ones are thicker as well
        float thickness = size >= 12 ? 0.55f : size >= 6 ? 0.5f : 0.45f;

        // a raft of slabs: the middle row is full, the outer ones
        // only sometimes
        for (int i = 0; i < size; i++) {
            float angle = (float) (Math.PI * 2.0 * i / size);
            float r = radius * (i % 2 == 0 ? 1.0f : 0.75f);
            float x = (float) Math.cos(angle) * r;
            float z = (float) Math.sin(angle) * r;
            float y = (i % 2 == 0) ? 0.0f : -0.08f;

            poseStack.pushPose();
            poseStack.translate(x, y, z);
            poseStack.scale(0.85f, thickness, 0.85f);
            this.blockRenderer.renderSingleBlock(ice, poseStack, buffer, packedLight,
                    OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FloatingIceEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
