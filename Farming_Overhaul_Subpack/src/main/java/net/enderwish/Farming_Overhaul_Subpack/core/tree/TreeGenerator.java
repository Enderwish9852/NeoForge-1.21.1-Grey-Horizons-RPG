package net.enderwish.Farming_Overhaul_Subpack.core.tree;

import net.enderwish.Farming_Overhaul_Subpack.block.tree.FruitBlock;
import net.enderwish.Farming_Overhaul_Subpack.block.tree.FruitTreeLeavesBlock;
import net.enderwish.Farming_Overhaul_Subpack.block.tree.FruitTreeLogBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TreeGenerator 閳?procedural growth engine (v4, "realistic growth" rework).
 *
 * Replaces the old endpoint/Manhattan-walk builder with a continuous
 * growth simulation built on real tree biology, aiming at the visual
 * language of the Dynamic Trees mod:
 *
 *  - Smooth 3D curves: every step nudges the growth direction with a
 *    tropism term (tips bend toward light; conifer branches sag under
 *    gravity) plus a small per-step wobble. No more staircase paths.
 *  - Taper everywhere: thickness lerps down each branch (trunk 6 ->
 *    limbs 5 -> branches 4 -> 3 -> 2 -> twigs 1) using the full pixel
 *    palette. JSON thickness is the tier's START thickness; the
 *    optional "taper" field sets tip thickness as a fraction of it.
 *  - Children attach ALONG the parent's outer two-thirds, not only at
 *    its tip, and are spaced with golden-angle phyllotaxis so limbs
 *    never clump into a flat X.
 *  - Apical dominance: one primary continues the trunk line (leader),
 *    the rest leave as laterals 閳?like a real deliquescent crown.
 *  - Noisy-edged leaf clusters: crowns read as organic clumps rather
 *    than math spheres, and never bury branches.
 *  - Optional root flare at the base (Dynamic-Trees buttressing look).
 */
public class TreeGenerator {

    private static final double GOLDEN_ANGLE = 2.399963229728653;

    // 閳光偓閳光偓 Generate (public API unchanged) 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓

    public static boolean generate(LevelAccessor level, BlockPos pos,
                                   String species, RandomSource random,
                                   boolean isWorldGen) {
        return generate(level, pos, species, random, isWorldGen, true);
    }

    public static boolean generate(LevelAccessor level, BlockPos pos,
                                   String species, RandomSource random,
                                   boolean isWorldGen, boolean withLeaves) {
        return TreeRegistry.INSTANCE.getBySpecies(species)
                .map(def -> generate(level, pos, def, random, withLeaves))
                .orElseGet(() -> {
                    System.err.println("[TreeGen] Unknown species: " + species);
                    return false;
                });
    }

    public static boolean generate(LevelAccessor level, BlockPos pos,
                                   TreeDefinition def, RandomSource random) {
        return generate(level, pos, def, random, true);
    }

    public static boolean generate(LevelAccessor level, BlockPos pos,
                                   TreeDefinition def, RandomSource random,
                                   boolean withLeaves) {
        return generate(level, pos, def, random, withLeaves,
                TreeStage.MATURE, null);
    }

    /** Full API: growth stage + optional collector of every placed voxel. */
    public static boolean generate(LevelAccessor level, BlockPos pos,
                                   TreeDefinition def, RandomSource random,
                                   boolean withLeaves, TreeStage stage,
                                   List<BlockPos> placedOut) {
        return switch (def.getArchitecture()) {
            case DELIQUESCENT -> generateDeliquescent(level, pos, def, random,
                    withLeaves, stage, placedOut);
            case EXCURRENT    -> generateExcurrent(level, pos, def, random,
                    withLeaves, stage, placedOut);
        };
    }

    // 閳光偓閳光偓 Placement buffer 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓
    // Logs win over leaves, leaves win over fruit. Nothing is written to the
    // world until the whole tree is laid out, so branches never fight leaves.

    private static final class Buffer {
        final Map<BlockPos, BlockState> logs = new LinkedHashMap<>();
        final Map<BlockPos, BlockState> leaves = new LinkedHashMap<>();
        final Map<BlockPos, BlockState> fruits = new LinkedHashMap<>();

        void log(BlockPos pos, BlockState state) {
            BlockPos p = pos.immutable();
            BlockState prev = logs.get(p);
            if (prev != null
                    && prev.getValue(FruitTreeLogBlock.THICKNESS)
                            > state.getValue(FruitTreeLogBlock.THICKNESS)) {
                return; // never overwrite a thicker log 閳?no pinched forks
            }
            logs.put(p, state);
            leaves.remove(p);
            fruits.remove(p);
        }

        void leaf(BlockPos pos, BlockState state) {
            BlockPos p = pos.immutable();
            if (!logs.containsKey(p)) leaves.putIfAbsent(p, state);
        }

        void fruit(BlockPos pos, BlockState state) {
            BlockPos p = pos.immutable();
            if (!logs.containsKey(p) && !leaves.containsKey(p)) fruits.put(p, state);
        }
    }

    /** A grown branch: voxel path, per-voxel thickness, heading angles. */
    private static final class Branch {
        final BlockPos origin;
        final List<BlockPos> voxels = new ArrayList<>();
        final List<Integer> thickness = new ArrayList<>();
        /** State written per voxel; index 0 (the origin) is null 閳?the parent owns it. */
        final List<BlockState> states = new ArrayList<>();
        /** Face crossed to ENTER each voxel; index 0 is null (origin). */
        final List<Direction> entryFaces = new ArrayList<>();
        double startAngle;
        double tipAngle;
        BlockPos tip;
        int tipThickness;

        Branch(BlockPos origin) { this.origin = origin; }
    }

    // 閳光偓閳光偓 Growth core 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓

    /**
     * Grows one branch as a smooth curve. The origin voxel is NOT re-placed
     * (its parent already put a log there); only the stepped voxels are.
     * Thickness lerps from startThickness to endThickness along the path.
     */
    private static Branch growBranch(Buffer buf, Block logBlock,
                                     BlockPos origin,
                                     double dirX, double dirY, double dirZ,
                                     int length, int startThickness, int endThickness,
                                     double tropismY, double wobble,
                                     RandomSource random) {
        Branch branch = new Branch(origin);
        branch.startAngle = Math.atan2(dirZ, dirX);
        branch.voxels.add(origin);
        branch.thickness.add(clampThickness(startThickness, 1, 6));
        branch.states.add(null); // origin voxel belongs to the parent
        branch.entryFaces.add(null);

        double px = origin.getX() + 0.5;
        double py = origin.getY() + 0.5;
        double pz = origin.getZ() + 0.5;
        BlockPos lastVoxel = origin;
        int lastThickness = clampThickness(startThickness, 1, 6);

        double travelled = 0.0;
        int guard = 0;
        while (travelled < length && guard++ < length * 8 + 64) {
            dirY += tropismY;
            dirX += (random.nextDouble() - 0.5) * wobble;
            dirY += (random.nextDouble() - 0.5) * wobble;
            dirZ += (random.nextDouble() - 0.5) * wobble;
            double len = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
            if (len < 1.0E-6) { dirX = 0; dirY = 1; dirZ = 0; len = 1; }
            dirX /= len; dirY /= len; dirZ /= len;

            // Cross into the next voxel through its nearest boundary face 閳?
            // consecutive logs are always face-adjacent, never corner-to-corner
            double tMaxX = boundaryDistance(px, dirX);
            double tMaxY = boundaryDistance(py, dirY);
            double tMaxZ = boundaryDistance(pz, dirZ);
            Direction.Axis crossedAxis;
            Direction crossedFace;
            double tCross;
            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                crossedAxis = Direction.Axis.X;
                crossedFace = dirX >= 0 ? Direction.EAST : Direction.WEST;
                tCross = Math.max(tMaxX, 1.0E-9);
            } else if (tMaxY <= tMaxZ) {
                crossedAxis = Direction.Axis.Y;
                crossedFace = dirY >= 0 ? Direction.UP : Direction.DOWN;
                tCross = Math.max(tMaxY, 1.0E-9);
            } else {
                crossedAxis = Direction.Axis.Z;
                crossedFace = dirZ >= 0 ? Direction.SOUTH : Direction.NORTH;
                tCross = Math.max(tMaxZ, 1.0E-9);
            }

            travelled += tCross;
            px += dirX * tCross;
            py += dirY * tCross;
            pz += dirZ * tCross;

            BlockPos voxel = lastVoxel.relative(crossedFace);

            float t = length <= 1 ? 1f : (float) (travelled / length);
            int th = clampThickness(
                    (int) Math.round(startThickness + (endThickness - startThickness) * t),
                    1, 6);
            lastVoxel = voxel;
            lastThickness = th;
            BlockState written = logState(logBlock, th, crossedAxis,
                    FruitTreeLogBlock.sideOf(crossedFace.getOpposite(), crossedAxis),
                    false);
            buf.log(voxel, written);
            branch.voxels.add(voxel);
            branch.thickness.add(th);
            branch.states.add(written);
            branch.entryFaces.add(crossedFace);
        }

        // End caps: a voxel renders its ring cap on its own tip side only
        // when that side actually faces air 閳?real tips, bends (the next
        // voxel is NOT straight ahead, so the open end would show the hollow
        // interior), and shoulders (next voxel thinner). Straight
        // equal-thickness runs stay open: the two bodies are flush, an
        // interior plane shows no hole, and a cap there would z-fight with
        // the neighbour's coplanar face (the original flashing bug).
        int n = branch.voxels.size();
        for (int i = 1; i < n; i++) {
            boolean tip = (i == n - 1);
            Direction.Axis axis = branch.states.get(i).getValue(FruitTreeLogBlock.AXIS);
            Direction ahead = switch (axis) {
                case X -> Direction.EAST;
                case Y -> Direction.UP;
                case Z -> Direction.SOUTH;
            };
            Direction nextEntry = tip ? null : branch.entryFaces.get(i + 1);
            boolean straightEqual = !tip
                    && nextEntry == ahead
                    && branch.thickness.get(i + 1) >= branch.thickness.get(i);
            if (!straightEqual) {
                buf.log(branch.voxels.get(i),
                        branch.states.get(i).setValue(FruitTreeLogBlock.CAPPED, true));
            }
        }

        branch.tip = lastVoxel;
        branch.tipThickness = lastThickness;
        branch.tipAngle = Math.atan2(dirZ, dirX);
        return branch;
    }

    // 閳光偓閳光偓 DELIQUESCENT generation 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓

    private static boolean generateDeliquescent(LevelAccessor level, BlockPos pos,
                                                TreeDefinition def, RandomSource random,
                                                boolean withLeaves,
                                                TreeStage stage, List<BlockPos> placedOut) {
        Block logBlock = getLogBlock(def.getLogBlock());
        if (logBlock == null) {
            System.err.println("[TreeGen] Log not found: " + def.getLogBlock());
            return false;
        }

        Buffer buf = new Buffer();

        // Stage scaling: young trees are proportionally smaller and their
        // wood is thinner 閳?the same biology, just earlier in its life.
        int trunkLevel = Math.min(def.getTrunkThicknessLevel(), stage.maxTrunk);
        int trunkHeight = Math.max(2,
                Math.round(def.randomTrunkHeight(random) * stage.sizeScale));
        int trunkEnd = clampThickness(
                Math.round(trunkLevel * def.getTrunkTaper()), 1, trunkLevel);

        if (def.hasRootFlare() && stage == TreeStage.MATURE)
            buildRootFlare(buf, logBlock, pos, trunkLevel, random);

        buf.log(pos, logState(logBlock, trunkLevel, Direction.Axis.Y,
                FruitTreeLogBlock.ParentSide.NEG, false));
        Branch trunk = growBranch(buf, logBlock, pos,
                0, 1, 0, trunkHeight, trunkLevel, trunkEnd, 0.0, 0.05, random);
        if (trunk.voxels.size() < 2) return false;

        // Fork point on the trunk
        float splitFrac = def.randomSplitFraction(random);
        int forkIdx = Math.round((trunk.voxels.size() - 1) * splitFrac);
        forkIdx = Math.min(Math.max(forkIdx, 1), trunk.voxels.size() - 1);
        BlockPos forkPos = trunk.voxels.get(forkIdx);
        int forkThickness = trunk.thickness.get(forkIdx);

        // 閳光偓閳光偓 Primary limbs: golden-angle phyllotaxis + one leader 閳光偓閳光偓
        int count = Math.max(1,
                Math.round(def.randomPrimaryCount(random) * stage.sizeScale));
        int primaryLevel = clampThickness(def.getPrimaryThicknessLevel(),
                1, Math.max(1, Math.min(forkThickness, stage.maxTrunk - 1)));
        int primaryEnd = clampThickness(
                Math.round(primaryLevel * def.getPrimaryTaper()), 1, primaryLevel);
        double baseAngle = random.nextDouble() * Math.PI * 2.0;

        List<Branch> primaries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            boolean leader = (i == count - 1);
            double angle;
            double horiz;
            double up;
            if (leader) {
                // apical dominance: one limb carries the trunk line upward
                angle = baseAngle + (random.nextDouble() - 0.5) * 0.6;
                horiz = 0.25 + random.nextDouble() * 0.2;
                up = 1.0;
            } else {
                angle = baseAngle + i * GOLDEN_ANGLE
                        + (random.nextDouble() - 0.5) * 0.5;
                horiz = def.randomPrimarySpread(random);
                up = def.randomPrimaryUpwardLift(random);
            }
            int len = Math.max(1, Math.round(
                    (def.randomPrimaryLength(random) + (leader ? 1 : 0))
                            * stage.sizeScale));
            // every branch starts STRICTLY thinner than its parent voxel so
            // joint extensions sit fully inside the parent's volume (flush
            // weld, no inset slits, no coplanar z-fighting)
            int startThick = leader
                    ? clampThickness(forkThickness - 1, 1, 6)
                    : clampThickness(primaryLevel, 1, Math.max(1, forkThickness - 1));
            primaries.add(growBranch(buf, logBlock, forkPos,
                    Math.cos(angle) * horiz, up, Math.sin(angle) * horiz,
                    len, startThick, primaryEnd, 0.03, 0.08, random));
        }

        // 閳光偓閳光偓 Secondary branches along each primary 閳光偓閳光偓
        int secLevel = clampThickness(def.getSecondaryThicknessLevel(),
                1, Math.max(1, stage.maxTrunk - 1));
        int secEnd = clampThickness(
                Math.round(secLevel * def.getSecondaryTaper()), 1, secLevel);
        double secWobble = 0.08 + def.randomSecondaryZigzag(random) * 0.2;

        List<Branch> secondaries = new ArrayList<>();
        for (Branch limb : primaries) {
            int n = Math.max(1, Math.round(
                    def.randomSecondaryCount(random) * stage.sizeScale));
            for (int i = 0; i < n; i++) {
                float fraction = 0.35f + (i + 0.5f) / n * 0.6f;
                BlockPos attach = voxelAt(limb, fraction);
                int parentThick = thicknessAt(limb, fraction);
                int startThick = clampThickness(
                        Math.min(secLevel, Math.max(1, parentThick - 1)), 1, 6);
                double angle = angleAt(limb, fraction)
                        + (random.nextDouble() - 0.5) * 1.1;
                float up = def.randomSecondaryUpwardLift(random);
                int len = Math.max(1, Math.round(
                        def.randomSecondaryLength(random) * stage.sizeScale));
                secondaries.add(growBranch(buf, logBlock, attach,
                        Math.cos(angle), up, Math.sin(angle),
                        len, startThick, secEnd, 0.02, secWobble, random));
            }
        }

        // 閳光偓閳光偓 Twigs along each secondary (outer two-thirds) 閳光偓閳光偓
        int twigLevel = clampThickness(def.getTwigThicknessLevel(),
                1, Math.max(1, stage.maxTrunk - 2));
        boolean droop = def.twigsDroop();

        List<Branch> twigs = new ArrayList<>();
        List<BlockPos> twigTips = new ArrayList<>();
        for (Branch sec : secondaries) {
            int n = Math.max(1, Math.round(
                    def.randomTwigCount(random) * stage.sizeScale));
            for (int i = 0; i < n; i++) {
                float fraction = 0.35f + (i + 0.5f) / n * 0.6f;
                BlockPos attach = voxelAt(sec, fraction);
                int parentThick = thicknessAt(sec, fraction);
                int startThick = clampThickness(
                        Math.min(twigLevel, Math.max(1, parentThick - 1)), 1, 6);
                double angle = angleAt(sec, fraction)
                        + (random.nextDouble() - 0.5) * 1.4;
                float vert = def.randomTwigVerticality(random);
                double up = droop ? -(0.3 + random.nextDouble() * 0.4)
                        : 0.2 + vert * 1.1;
                double horiz = droop ? 1.0 : Math.max(0.15, 1.1 - vert);
                int len = Math.max(1, Math.round(
                        def.randomTwigLength(random) * stage.sizeScale));
                Branch twig = growBranch(buf, logBlock, attach,
                        Math.cos(angle) * horiz, up, Math.sin(angle) * horiz,
                        len, startThick, 1, 0.0, 0.1, random);
                twigs.add(twig);
                twigTips.add(twig.tip);
            }
        }

        // 閳光偓閳光偓 Canopy 閳光偓閳光偓
        if (withLeaves) {
            Block leafBlock = getLeafBlock(def.getLeafBlock());
            if (leafBlock != null && !twigTips.isEmpty()) {
                float density = def.randomLeafDensity(random);
                int radius = Math.max(1, Math.round(
                        def.randomCanopyClusterRadius(random) * stage.sizeScale));
                float topBias = def.getCanopyTopBias();

                for (BlockPos tip : twigTips)
                    placeLeafCluster(buf, leafBlock, tip, radius, density, topBias, random);

                // leaves hugging the outer half of every twig
                for (Branch twig : twigs) {
                    int n = twig.voxels.size();
                    for (int i = n / 2; i < n; i++)
                        placeLeafCluster(buf, leafBlock, twig.voxels.get(i),
                                1, density * 0.55f, topBias, random);
                }

                // faint bleed on the outer third of secondaries
                for (Branch sec : secondaries) {
                    int n = sec.voxels.size();
                    for (int i = (int) (n * 0.7); i < n; i++)
                        placeLeafCluster(buf, leafBlock, sec.voxels.get(i),
                                1, density * 0.3f, topBias, random);
                }
            }
            // fruit only on fully mature trees 閳?young wood doesn't bear
            if (stage == TreeStage.MATURE)
                placeFruit(buf, twigTips, def, random);
        }

        placeAll(level, buf, placedOut);
        return true;
    }

    /** Dynamic-Trees-style buttressing: chunky rods gripping the ground. */
    private static void buildRootFlare(Buffer buf, Block logBlock,
                                       BlockPos base, int trunkLevel,
                                       RandomSource random) {
        int flares = 3 + random.nextInt(2);
        int thickness = clampThickness(trunkLevel - 1, 4, 6);
        for (int i = 0; i < flares; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int dx = (int) Math.round(Math.cos(angle));
            int dz = (int) Math.round(Math.sin(angle));
            if (dx == 0 && dz == 0) continue;
            Direction.Axis axis = Math.abs(dx) >= Math.abs(dz)
                    ? Direction.Axis.X : Direction.Axis.Z;
            if (axis == Direction.Axis.X) dz = 0; else dx = 0; // stay face-adjacent to the base
            Direction parentDir = axis == Direction.Axis.X
                    ? (dx > 0 ? Direction.WEST : Direction.EAST)
                    : (dz > 0 ? Direction.NORTH : Direction.SOUTH);
            buf.log(base.offset(dx, 0, dz),
                    logState(logBlock, thickness, axis,
                            FruitTreeLogBlock.sideOf(parentDir, axis), true));
        }
    }

    // 閳光偓閳光偓 EXCURRENT generation 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓

    /**
     * Excurrent architecture (spruce / conifer): full-height central leader
     * with whorls of branches that arc outward, then sag under their own
     * weight (gravity tropism) 閳?the classic conifer curve. Whorls shorten
     * with height for the conical silhouette.
     */
    private static boolean generateExcurrent(LevelAccessor level, BlockPos pos,
                                             TreeDefinition def, RandomSource random,
                                             boolean withLeaves,
                                             TreeStage stage, List<BlockPos> placedOut) {
        Block logBlock = getLogBlock(def.getLogBlock());
        if (logBlock == null) {
            System.err.println("[TreeGen] Log not found: " + def.getLogBlock());
            return false;
        }

        Buffer buf = new Buffer();

        // Stage scaling 閳?same biology, earlier in its life
        int trunkLevel = Math.min(def.getTrunkThicknessLevel(), stage.maxTrunk);
        int trunkHeight = Math.max(3,
                Math.round(def.randomTrunkHeight(random) * stage.sizeScale));
        int trunkEnd = clampThickness(
                Math.round(trunkLevel * def.getTrunkTaper()), 2, trunkLevel);

        buf.log(pos, logState(logBlock, trunkLevel, Direction.Axis.Y,
                FruitTreeLogBlock.ParentSide.NEG, false));
        Branch trunk = growBranch(buf, logBlock, pos,
                0, 1, 0, trunkHeight, trunkLevel, trunkEnd, 0.0, 0.04, random);

        // 閳光偓閳光偓 Whorls stepping up the trunk 閳光偓閳光偓
        int whorlLevel = clampThickness(def.getWhorlThicknessLevel(),
                1, Math.max(1, stage.maxTrunk - 1));
        int whorlEnd = clampThickness(
                Math.round(whorlLevel * def.getWhorlTaper()), 1, whorlLevel);
        int baseLen = Math.max(1, Math.round(
                def.randomWhorlBaseLength(random) * stage.sizeScale));
        double baseAngle = random.nextDouble() * Math.PI * 2.0;

        List<Branch> branches = new ArrayList<>();
        int y = 2; // keep a clear bole above the base
        int whorlIndex = 0;
        while (y < trunkHeight - 1) {
            int idx = Math.min(y, trunk.voxels.size() - 1);
            BlockPos at = trunk.voxels.get(idx);
            float lenFactor = (float) Math.pow(def.getWhorlLengthTaper(), whorlIndex);
            int len = Math.max(1, Math.round(baseLen * lenFactor));
            int branchCount = Math.max(2, Math.round(
                    def.randomWhorlBranchCount(random) * stage.sizeScale));
            double gravity = def.whorlDroop()
                    ? -(0.07 + random.nextDouble() * 0.05) : 0.0;

            for (int i = 0; i < branchCount; i++) {
                double angle = baseAngle + whorlIndex * 0.7
                        + i * (Math.PI * 2.0 / branchCount)
                        + (random.nextDouble() - 0.5) * 0.3;
                float spread = def.getWhorlSpread();
                double up = def.whorlDroop() ? 0.15 : 0.1;
                int startThick = Math.min(whorlLevel,
                        Math.max(1, trunk.thickness.get(idx) - 1));
                Branch branch = growBranch(buf, logBlock, at,
                        Math.cos(angle) * spread, up, Math.sin(angle) * spread,
                        len, startThick, whorlEnd, gravity, 0.05, random);
                branches.add(branch);

                // second-order feathering on the longer low whorls
                if (def.getWhorlSubBranchThickness() > 0 && len >= 4 && i % 2 == 0) {
                    float atFrac = 0.45f + random.nextFloat() * 0.25f;
                    BlockPos subAttach = voxelAt(branch, atFrac);
                    double subAngle = angle + (random.nextDouble() - 0.5) * 0.9;
                    int subLen = Math.max(1, len / 2);
                    int subStart = clampThickness(
                            Math.min(def.getWhorlSubBranchThickness(),
                                    Math.max(1, thicknessAt(branch, atFrac) - 1)),
                            1, 6);
                    branches.add(growBranch(buf, logBlock, subAttach,
                            Math.cos(subAngle) * spread, -0.05, Math.sin(subAngle) * spread,
                            subLen, subStart, 1,
                            gravity, 0.06, random));
                }
            }

            y += def.randomWhorlSpacing(random);
            whorlIndex++;
        }

        // 閳光偓閳光偓 Canopy 閳光偓閳光偓
        if (withLeaves) {
            Block leafBlock = getLeafBlock(def.getLeafBlock());
            if (leafBlock != null) {
                float density = def.randomLeafDensity(random);
                int radius = Math.max(1, Math.round(
                        def.randomCanopyClusterRadius(random) * stage.sizeScale));
                float topBias = def.getCanopyTopBias();

                if (def.canopyCoversFullBranch()) {
                    for (Branch branch : branches) {
                        int n = branch.voxels.size();
                        for (int i = 0; i < n; i++) {
                            float f = i / (float) Math.max(1, n - 1);
                            placeLeafCluster(buf, leafBlock, branch.voxels.get(i),
                                    Math.max(1, radius - 1),
                                    density * (0.45f + 0.55f * f), topBias, random);
                        }
                    }
                }
                for (Branch branch : branches)
                    placeLeafCluster(buf, leafBlock, branch.tip,
                            radius, density, topBias, random);

                // spire: foliage hugging the upper trunk + a tip tuft
                int spireStart = (int) (trunk.voxels.size() * 0.75);
                for (int i = spireStart; i < trunk.voxels.size(); i++)
                    placeLeafCluster(buf, leafBlock, trunk.voxels.get(i),
                            Math.max(1, radius - 1), density * 0.7f, topBias, random);
                placeLeafCluster(buf, leafBlock, trunk.tip.above(),
                        Math.max(1, radius - 1), density, topBias, random);
            }
        }

        placeAll(level, buf, placedOut);
        return true;
    }

    // 閳光偓閳光偓 Canopy 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓

    /**
     * Irregular leaf blob around a branch point: ellipse reach scaled by
     * topBias, probability falling toward the noisy edge. Never overwrites
     * logs, and every leaf sits within decay range of the branch that
     * seeded it (cluster radius < the 6-block decay search cube).
     */
    private static void placeLeafCluster(Buffer buf, Block leafBlock,
                                         BlockPos centre, int radius,
                                         float density, float topBias,
                                         RandomSource random) {
        if (radius <= 0 || density <= 0f) return;

        double upReach = radius * topBias * 1.3;
        double downReach = radius * (1.0 - topBias) * 1.3;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int upSteps = (int) Math.ceil(upReach);
                for (int y = 0; y <= upSteps; y++) {
                    double dist = Math.sqrt(x * x + z * z
                            + sq(y / Math.max(0.1, topBias * 1.3))) / radius;
                    if (dist > 1.0) continue;
                    if (random.nextFloat() < density * (1.25f - (float) dist))
                        buf.leaf(centre.offset(x, y, z), leafEntry(leafBlock));
                }
                int downSteps = (int) Math.ceil(downReach);
                for (int y = 1; y <= downSteps; y++) {
                    double dist = Math.sqrt(x * x + z * z
                            + sq(y / Math.max(0.1, (1.0 - topBias) * 1.3)))
                            / (radius * 0.85);
                    if (dist > 1.0) continue;
                    if (random.nextFloat() < density * (1.25f - (float) dist) * 0.55f)
                        buf.leaf(centre.offset(x, -y, z), leafEntry(leafBlock));
                }
            }
        }
    }

    private static BlockState leafEntry(Block leafBlock) {
        return leafBlock.defaultBlockState()
                .setValue(FruitTreeLeavesBlock.SEASON_STAGE,
                        FruitTreeLeavesBlock.SeasonStage.FULL);
    }

    // 閳光偓閳光偓 Fruit 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓

    private static void placeFruit(Buffer buf, List<BlockPos> twigTips,
                                   TreeDefinition def, RandomSource random) {
        if (def.getFruitBlock() == null) return;
        Block fruitBlock = BuiltInRegistries.BLOCK.get(
                ResourceLocation.parse(def.getFruitBlock()));
        if (!(fruitBlock instanceof FruitBlock)) return;
        for (BlockPos tip : twigTips) {
            if (random.nextFloat() < def.getFruitSpawnRate()) {
                buf.fruit(tip.below(),
                        fruitBlock.defaultBlockState()
                                .setValue(FruitBlock.AGE, 0)
                                .setValue(FruitBlock.FACING, Direction.DOWN));
            }
        }
    }

    // 閳光偓閳光偓 Placement 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓

    private static void placeAll(LevelAccessor level, Buffer buf,
                                 List<BlockPos> placedOut) {
        for (Map.Entry<BlockPos, BlockState> e : buf.logs.entrySet())
            placeInWorld(level, e.getKey(), e.getValue(), placedOut);
        for (Map.Entry<BlockPos, BlockState> e : buf.leaves.entrySet())
            placeInWorld(level, e.getKey(), e.getValue(), placedOut);
        for (Map.Entry<BlockPos, BlockState> e : buf.fruits.entrySet())
            placeInWorld(level, e.getKey(), e.getValue(), placedOut);
    }

    private static void placeInWorld(LevelAccessor level, BlockPos pos,
                                     BlockState state, List<BlockPos> placedOut) {
        // TreeGenerator replaces blocks as needed (wood on grass, leaves on air,
        // etc.). The feature placement system guarantees origin is a valid
        // surface position during chunk gen, so we always write.
        level.setBlock(pos, state, 3);
        if (placedOut != null) placedOut.add(pos.immutable());
    }

    // 閳光偓閳光偓 Helpers 閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓閳光偓

    private static BlockPos voxelAt(Branch branch, float fraction) {
        int idx = Math.round((branch.voxels.size() - 1)
                * Math.max(0f, Math.min(1f, fraction)));
        return branch.voxels.get(idx);
    }

    private static int thicknessAt(Branch branch, float fraction) {
        int idx = Math.round((branch.thickness.size() - 1)
                * Math.max(0f, Math.min(1f, fraction)));
        return branch.thickness.get(idx);
    }

    /** Heading of the parent branch at a fraction of its length. */
    private static double angleAt(Branch branch, float fraction) {
        double cx = Math.cos(branch.startAngle)
                + (Math.cos(branch.tipAngle) - Math.cos(branch.startAngle)) * fraction;
        double cz = Math.sin(branch.startAngle)
                + (Math.sin(branch.tipAngle) - Math.sin(branch.startAngle)) * fraction;
        return Math.atan2(cz, cx);
    }

    private static int clampThickness(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    /** Distance along the unit ray until p crosses into the next voxel on its axis. */
    private static double boundaryDistance(double p, double d) {
        if (d == 0.0) return Double.MAX_VALUE;
        double frac = p - Math.floor(p);
        double toBoundary = (d > 0.0) ? (1.0 - frac) : frac;
        return Math.max(toBoundary / Math.abs(d), 0.0);
    }

    private static double sq(double v) { return v * v; }

    private static BlockState logState(Block logBlock, int thickness,
                                       Direction.Axis axis,
                                       FruitTreeLogBlock.ParentSide parent,
                                       boolean capped) {
        return logBlock.defaultBlockState()
                .setValue(FruitTreeLogBlock.THICKNESS, clampThickness(thickness, 1, 6))
                .setValue(FruitTreeLogBlock.AXIS, axis)
                .setValue(FruitTreeLogBlock.PARENT, parent)
                .setValue(FruitTreeLogBlock.CAPPED, capped);
    }

    private static Block getLogBlock(String id) {
        if (id == null) return null;
        Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        return b instanceof FruitTreeLogBlock ? b : null;
    }

    private static Block getLeafBlock(String id) {
        if (id == null) return null;
        Block b = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(id));
        return b instanceof FruitTreeLeavesBlock ? b : null;
    }
}
