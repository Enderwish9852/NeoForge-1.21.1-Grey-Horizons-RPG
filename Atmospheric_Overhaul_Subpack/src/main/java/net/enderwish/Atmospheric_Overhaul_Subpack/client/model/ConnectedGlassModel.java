package net.enderwish.Atmospheric_Overhaul_Subpack.client.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * ConnectedGlassModel
 *
 * NEW -- connected-texture glass for the vanilla glass block, no
 * OptiFine needed. The vanilla bordered glass.png override is REVERTED;
 * instead every rendered face of a glass block uses the borderless
 * sprite, and a thin border strip (solid vanilla border color) is added
 * along each face edge whose NEIGHBOR in that edge direction is NOT
 * another glass block. Touch two glass blocks together: the border on
 * the touching side disappears. Isolated glass: borders on all edges
 * (vanilla look). Item form keeps the vanilla bordered sprite.
 *
 * Per-block connectivity is passed through ModelData (gathered in
 * getModelData, refreshed on chunk rebuilds -- setBlocksDirty marks
 * sections +-1 block around every change, so neighbor updates repaint
 * connections). Border strips share the base quad's vertex packing
 * (BLOCK format via IQuadTransformer offsets) so lighting/AO are
 * recomputed by the chunk renderer exactly like vanilla faces.
 */
public class ConnectedGlassModel extends BakedModelWrapper<BakedModel> {

    public static final ModelProperty<boolean[]> CONNECTIONS = new ModelProperty<>();

    private static final float EDGE_WIDTH = 1.0f / 16.0f;  // 1 texture pixel
    private static final float LIFT = 0.002f;              // anti z-fight lift off the face plane

    /** A border strip quad plus the perpendicular neighbor it hugs. */
    private record Strip(BakedQuad quad, Direction edge) {}

    private final TextureAtlasSprite borderlessSprite;
    private final TextureAtlasSprite edgeSprite;
    private final Map<BakedQuad, BakedQuad> borderlessQuads;   // identity, read-only after bake
    private final Map<BakedQuad, List<Strip>> edgeStrips;      // identity, read-only after bake

    public ConnectedGlassModel(BakedModel original, TextureAtlasSprite borderlessSprite, TextureAtlasSprite edgeSprite) {
        super(original);
        this.borderlessSprite = borderlessSprite;
        this.edgeSprite = edgeSprite;

        RandomSource rand = RandomSource.create();
        IdentityHashMap<BakedQuad, BakedQuad> base = new IdentityHashMap<>();
        IdentityHashMap<BakedQuad, List<Strip>> strips = new IdentityHashMap<>();

        for (Direction side : Direction.values()) {
            for (BakedQuad q : original.getQuads(null, side, rand)) {
                base.putIfAbsent(q, retex(q));
                strips.putIfAbsent(q, buildStrips(q));
            }
        }

        this.borderlessQuads = base;
        this.edgeStrips = strips;
    }

    /** Sprite lookup helper for the bake event. */
    public static TextureAtlasSprite sprite(ModelEvent.ModifyBakingResult event, String path) {
        return event.getTextureGetter().apply(new Material(
                net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS,
                ResourceLocation.fromNamespaceAndPath("gh_atmospheric", path)));
    }

    /** Swaps the quad's sprite for the borderless one, remapping UVs. */
    private BakedQuad retex(BakedQuad q) {
        int[] v = q.getVertices().clone();

        float su0 = q.getSprite().getU0(), su1 = q.getSprite().getU1();
        float sv0 = q.getSprite().getV0(), sv1 = q.getSprite().getV1();
        float du0 = borderlessSprite.getU0(), du1 = borderlessSprite.getU1();
        float dv0 = borderlessSprite.getV0(), dv1 = borderlessSprite.getV1();

        for (int i = 0; i < 4; i++) {
            int idx = i * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
            float un = (Float.intBitsToFloat(v[idx]) - su0) / (su1 - su0);
            float vn = (Float.intBitsToFloat(v[idx + 1]) - sv0) / (sv1 - sv0);
            v[idx] = Float.floatToIntBits(du0 + un * (du1 - du0));
            v[idx + 1] = Float.floatToIntBits(dv0 + vn * (dv1 - dv0));
        }
        return new BakedQuad(v, q.getTintIndex(), q.getDirection(),
                borderlessSprite, q.isShade(), q.hasAmbientOcclusion());
    }

    /** One 1px border strip per face edge (perpendicular neighbor direction). */
    private List<Strip> buildStrips(BakedQuad baseQuad) {
        List<Strip> out = new ArrayList<>(4);
        Direction face = baseQuad.getDirection();

        for (Direction edge : Direction.values()) {
            if (edge.getAxis() == face.getAxis()) continue;
            out.add(new Strip(buildStrip(baseQuad, face, edge), edge));
        }
        return out;
    }

    /**
     * A thin quad hugging the face plane along the edge side. The strip's
     * WIDTH runs along the edge's own axis, its LENGTH along the remaining
     * tangent axis. Winding is matched to the base quad's projected
     * orientation so backface culling agrees with vanilla faces.
     */
    private BakedQuad buildStrip(BakedQuad baseQuad, Direction face, Direction edge) {
        int[] src = baseQuad.getVertices().clone();

        Direction.Axis normalAxis = face.getAxis();
        Direction.Axis widthAxis = edge.getAxis();
        Direction.Axis lenAxis = Direction.Axis.X;
        for (Direction.Axis a : Direction.Axis.values()) {
            if (a != normalAxis && a != widthAxis) { lenAxis = a; break; }
        }

        // plane coordinate along the face normal (from the base quad), lifted outward
        float plane = Float.intBitsToFloat(src[IQuadTransformer.POSITION + axisInt(normalAxis)])
                + face.getAxisDirection().getStep() * LIFT;

        // base winding orientation projected onto (len, width)
        float orient = 0;
        for (int i = 0; i < 4; i++) {
            float[] a = readVertex(src, i);
            float[] b = readVertex(src, (i + 1) % 4);
            orient += (b[axisInt(lenAxis)] - a[axisInt(lenAxis)]) * (b[axisInt(widthAxis)] + a[axisInt(widthAxis)]);
        }

        // width interval along the edge's axis: strip hugs the face boundary
        boolean highSide = edge.getAxisDirection().getStep() > 0;
        float wOuter = highSide ? 1.0f : 0.0f;
        float wInner = highSide ? 1.0f - EDGE_WIDTH : EDGE_WIDTH;

        // 4 corners in (len, width)
        float[] lenP = {0f, 1f, 1f, 0f};
        float[] widthP = {wOuter, wOuter, wInner, wInner};
        float emitted = 0;
        for (int i = 0; i < 4; i++) {
            float aL = lenP[i], aW = widthP[i];
            float bL = lenP[(i + 1) % 4], bW = widthP[(i + 1) % 4];
            emitted += (bL - aL) * (bW + aW);
        }
        if ((emitted <= 0) != (orient <= 0)) {
            // reverse winding to match the base quad
            for (int i = 0; i < 2; i++) {
                float tl = lenP[i]; lenP[i] = lenP[3 - i]; lenP[3 - i] = tl;
                float tw = widthP[i]; widthP[i] = widthP[3 - i]; widthP[3 - i] = tw;
            }
        }

        for (int i = 0; i < 4; i++) {
            int idx = i * IQuadTransformer.STRIDE;
            float len = lenP[i];
            float width = widthP[i];
            float x = normalAxis == Direction.Axis.X ? plane : (lenAxis == Direction.Axis.X ? len : width);
            float y = normalAxis == Direction.Axis.Y ? plane : (lenAxis == Direction.Axis.Y ? len : width);
            float z = normalAxis == Direction.Axis.Z ? plane : (lenAxis == Direction.Axis.Z ? len : width);
            src[idx + IQuadTransformer.POSITION] = Float.floatToIntBits(x);
            src[idx + IQuadTransformer.POSITION + 1] = Float.floatToIntBits(y);
            src[idx + IQuadTransformer.POSITION + 2] = Float.floatToIntBits(z);
            // BakedQuad UVs are ATLAS coordinates, not sprite-local 0..1 --
            // raw width/len sampled whatever texture landed at that atlas
            // region (the user saw a random mod texture on the strips).
            // Sample a fixed point INSIDE the vanilla border ring instead:
            // half a pixel in from the sprite corner = opaque border color
            // everywhere on the strip.
            float uLocal = 0.5f / 16f, vLocal = 0.5f / 16f;
            src[idx + IQuadTransformer.UV0] = Float.floatToIntBits(edgeSprite.getU(uLocal));
            src[idx + IQuadTransformer.UV0 + 1] = Float.floatToIntBits(edgeSprite.getV(vLocal));
        }
        return new BakedQuad(src, baseQuad.getTintIndex(), face,
                edgeSprite, baseQuad.isShade(), baseQuad.hasAmbientOcclusion());
    }

    private float[] readVertex(int[] data, int vertex) {
        int idx = vertex * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
        return new float[]{
                Float.intBitsToFloat(data[idx]),
                Float.intBitsToFloat(data[idx + 1]),
                Float.intBitsToFloat(data[idx + 2])
        };
    }

    private static int axisInt(Direction.Axis axis) {
        return switch (axis) {
            case X -> 0;
            case Y -> 1;
            case Z -> 2;
        };
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        boolean[] conn = new boolean[6];
        for (Direction d : Direction.values()) {
            conn[d.ordinal()] = level.getBlockState(pos.relative(d)).is(Blocks.GLASS);
        }
        return ModelData.builder().with(CONNECTIONS, conn).build();
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand,
                                    ModelData data, RenderType renderType) {
        List<BakedQuad> base = originalModel.getQuads(state, side, rand, data, renderType);
        boolean[] conn = data == null ? null : data.get(CONNECTIONS);
        if (conn == null) return base;

        List<BakedQuad> out = null;
        for (int i = 0; i < base.size(); i++) {
            BakedQuad q = base.get(i);
            BakedQuad borderless = borderlessQuads.get(q);
            List<Strip> strips = edgeStrips.get(q);

            if (borderless == null && strips == null) continue;

            if (out == null) out = new ArrayList<>(base.size() + 8);
            out.add(borderless != null ? borderless : q);
            if (strips != null) {
                for (Strip strip : strips) {
                    if (!conn[strip.edge().ordinal()]) out.add(strip.quad());
                }
            }
        }
        return out != null ? out : base;
    }
}
