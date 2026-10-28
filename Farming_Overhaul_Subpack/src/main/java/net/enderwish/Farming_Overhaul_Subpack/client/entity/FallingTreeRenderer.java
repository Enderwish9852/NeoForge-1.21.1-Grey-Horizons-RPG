package net.enderwish.Farming_Overhaul_Subpack.client.entity;

import net.enderwish.Farming_Overhaul_Subpack.FarmingOverhaulSubpack;
import net.enderwish.Farming_Overhaul_Subpack.entity.FallingTreeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * FallingTreeRenderer — client-side renderer for FallingTreeEntity.
 *
 * The entity is a physics placeholder: all blocks were removed from the world
 * when the tree was cut, and items are dropped on land. The renderer exists
 * solely to prevent a NullPointerException in EntityRenderDispatcher when the
 * client tries to render the entity. It renders nothing visible.
 */
public class FallingTreeRenderer extends EntityRenderer<FallingTreeEntity> {

    private static final ResourceLocation MISSING_TEXTURE = ResourceLocation.parse("missingno");

    public FallingTreeRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
        this.shadowStrength = 1.0f;
    }

    @Override
    public void render(FallingTreeEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int light) {
        // Render nothing visible — the entity is a physics placeholder.
        // Blocks were removed from the world when the tree was cut.
    }

    @Override
    public ResourceLocation getTextureLocation(FallingTreeEntity entity) {
        return MISSING_TEXTURE;
    }

    @EventBusSubscriber(modid = FarmingOverhaulSubpack.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static class Register {
        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(
                    FarmingOverhaulSubpack.FALLING_TREE.get(),
                    FallingTreeRenderer::new
            );
        }
    }
}
