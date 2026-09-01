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
import java.util.List;

public class TreeGenerator {

    // ── Generate ──────────────────────────────────────────────────────────────

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
        return switch (def.getArchitecture()) {
            case DELIQUESCENT -> generateDeliquescent(level, pos, def, random, withLeaves);
            case EXCURRENT    -> generateExcurrent(level, pos, def, random, withLeaves);
        };
    }

    // ── DELIQUESCENT generation ───────────────────────────────────────────────

    private static boolean generateDeliquescent(LevelAccessor level, BlockPos pos,
                                                TreeDefinition def, RandomSource random,
                                                boolean withLeaves) {
        List<PlacementEntry> placements = new ArrayList<>();

        int trunkHeight = def.randomTrunkHeight(random);
        System.out.println("[TreeGen] " + def.getSpecies() + " (deliquescent) trunk: " + trunkHeight);
        buildTrunk(placements, pos, trunkHeight, def);

        float splitFraction = def.randomSplitFraction(random);
        int splitY = (int) (trunkHeight * splitFraction);

        List<LimbEnd> limbEnds = new ArrayList<>();
        buildPrimaryLimbs(placements, pos, splitY, trunkHeight, def, random, limbEnds);

        List<LimbEnd> branchEnds  = new ArrayList<>();
        List<List<BlockPos>> branchPaths = new ArrayList<>();
        for (LimbEnd limb : limbEnds) {
            buildSecondaryBranches(placements, limb, def, random,
                    branchEnds, branchPaths, pos);
        }

        List<LimbEnd> twigEnds  = new ArrayList<>();
        List<List<BlockPos>> twigPaths = new ArrayList<>();
        for (LimbEnd branch : branchEnds) {
            buildTwigs(placements, branch, def, random, twigEnds, twigPaths);
        }

        if (withLeaves) {
            buildDeliquescentCanopy(placements, def, random, twigEnds, twigPaths, branchPaths);
        }
        if (withLeaves && def.hasFruit()) {
            placeFruit(placements, twigEnds, def, random);
        }

        placeAll(level, placements);
        return true;
    }

    // ── EXCURRENT generation ──────────────────────────────────────────────────

    /**
     * Excurrent architecture (spruce / conifer):
     *
     * 1. Full-height central leader trunk
     * 2. At every whorl interval: a set of branches radiating evenly
     *    outward around the trunk. Bottom whorls = longest branches.
     *    Each higher whorl tapers shorter by whorl_length_taper factor.
     * 3. If covers_full_branch=true, leaves cover each branch segment
     *    densely (spruce/jungle). If false, terminal clusters only.
     *
     * The combination of full-height trunk + horizontal branches that get
     * shorter toward the top naturally produces the conical silhouette.
     */
    private static boolean generateExcurrent(LevelAccessor level, BlockPos pos,
                                             TreeDefinition def, RandomSource random,
                                             boolean withLeaves) {
        List<PlacementEntry> placements = new ArrayList<>();

        int trunkHeight = def.randomTrunkHeight(random);
        System.out.println("[TreeGen] " + def.getSpecies() + " (excurrent) trunk: " + trunkHeight);
        buildTrunk(placements, pos, trunkHeight, def);

        // Collect all branch endpoints for canopy
        List<LimbEnd> allBranchEnds = new ArrayList<>();
        List<List<BlockPos>> allBranchPaths = new ArrayList<>();

        // Step up the trunk placing whorls
        int y = 1; // start 1 above base
        int whorlIndex = 0;
        int baseBranchLength = def.randomWhorlBaseLength(random);

        while (y < trunkHeight - 1) {
            int spacing = def.randomWhorlSpacing(random);

            // Branch length tapers: each whorl is shorter by taper factor
            float lengthFactor = (float) Math.pow(def.getWhorlLengthTaper(), whorlIndex);
            int branchLength = Math.max(1, Math.round(baseBranchLength * lengthFactor));

            buildWhorl(placements, pos.above(y), branchLength, def, random,
                    allBranchEnds, allBranchPaths);

            y += spacing;
            whorlIndex++;
        }

        if (withLeaves) {
            buildExcurrentCanopy(placements, def, random,
                    allBranchEnds, allBranchPaths);
        }

        placeAll(level, placements);
        return true;
    }

    /**
     * A single whorl — branches radiating evenly around the trunk at a
     * given height. Branch length is passed in (already tapered by caller).
     */
    private static void buildWhorl(List<PlacementEntry> placements,
                                   BlockPos origin, int branchLength,
                                   TreeDefinition def, RandomSource random,
                                   List<LimbEnd> branchEnds,
                                   List<List<BlockPos>> branchPaths) {
        Block logBlock = getLogBlock(def.getLogBlock());
        if (logBlock == null) return;

        int count = def.randomWhorlBranchCount(random);
        int thicknessLevel = def.getWhorlThicknessLevel();
        float spread = def.getWhorlSpread();

        double baseAngle = random.nextDouble() * Math.PI * 2.0;
        double angleStep = Math.PI * 2.0 / count;

        for (int i = 0; i < count; i++) {
            double angle = baseAngle + i * angleStep
                    + (random.nextDouble() - 0.5) * angleStep * 0.2;

            // spread=1.0 → purely horizontal, spread=0.0 → straight up
            // Most conifers are near-horizontal (0.85-0.95)
            int targetX = origin.getX()
                    + (int) Math.round(Math.cos(angle) * branchLength * spread);
            int targetY = origin.getY()
                    + (def.whorlDroop()
                    ? -Math.max(1, branchLength / 3)  // drooping tip
                    : Math.max(0, branchLength / 4)); // slight upward
            int targetZ = origin.getZ()
                    + (int) Math.round(Math.sin(angle) * branchLength * spread);

            BlockPos target = new BlockPos(targetX, targetY, targetZ);
            List<BlockPos> path = buildAdjacentPath(origin, target, random);

            List<BlockPos> fullPath = new ArrayList<>();
            fullPath.add(origin);
            fullPath.addAll(path);
            branchPaths.add(fullPath);

            BlockPos lastPlaced = origin;
            for (BlockPos step : path) {
                Direction.Axis axis = getAxis(
                        step.getX() - lastPlaced.getX(),
                        step.getY() - lastPlaced.getY(),
                        step.getZ() - lastPlaced.getZ());
                placements.add(new PlacementEntry(
                        step,
                        logBlock.defaultBlockState()
                                .setValue(FruitTreeLogBlock.THICKNESS, thicknessLevel)
                                .setValue(FruitTreeLogBlock.AXIS, axis)));
                lastPlaced = step;
            }

            // Sub-branches at tip (optional — adds detail for larger conifers)
            if (def.getWhorlSubBranchThickness() > 0 && branchLength > 2) {
                BlockPos tipPos = lastPlaced;
                for (int sb = 0; sb < 2; sb++) {
                    double subAngle = angle + (random.nextDouble() - 0.5) * Math.PI * 0.6;
                    int subLen = Math.max(1, branchLength / 2);
                    int sX = tipPos.getX() + (int) Math.round(Math.cos(subAngle) * subLen * spread);
                    int sY = tipPos.getY() + (def.whorlDroop() ? -1 : 0);
                    int sZ = tipPos.getZ() + (int) Math.round(Math.sin(subAngle) * subLen * spread);
                    List<BlockPos> subPath = buildAdjacentPath(tipPos,
                            new BlockPos(sX, sY, sZ), random);

                    List<BlockPos> subFull = new ArrayList<>();
                    subFull.add(tipPos);
                    subFull.addAll(subPath);
                    branchPaths.add(subFull);

                    BlockPos subLast = tipPos;
                    for (BlockPos sp : subPath) {
                        Direction.Axis ax = getAxis(
                                sp.getX() - subLast.getX(),
                                sp.getY() - subLast.getY(),
                                sp.getZ() - subLast.getZ());
                        placements.add(new PlacementEntry(
                                sp,
                                logBlock.defaultBlockState()
                                        .setValue(FruitTreeLogBlock.THICKNESS,
                                                def.getWhorlSubBranchThickness())
                                        .setValue(FruitTreeLogBlock.AXIS, ax)));
                        subLast = sp;
                    }
                    branchEnds.add(new LimbEnd(subLast, subAngle,
                            def.getWhorlSubBranchThickness()));
                }
            }

            branchEnds.add(new LimbEnd(lastPlaced, angle, thicknessLevel));
        }
    }

    /**
     * Excurrent canopy — covers the full branch path densely if
     * covers_full_branch=true (spruce, jungle), otherwise terminal only.
     */
    private static void buildExcurrentCanopy(List<PlacementEntry> placements,
                                             TreeDefinition def,
                                             RandomSource random,
                                             List<LimbEnd> branchEnds,
                                             List<List<BlockPos>> branchPaths) {
        Block leafBlock = getLeafBlock(def.getLeafBlock());
        if (leafBlock == null) return;

        float topBias = def.getCanopyTopBias();
        float density = def.randomLeafDensity(random);
        int radius    = def.randomCanopyClusterRadius(random);

        if (def.canopyCoversFullBranch()) {
            // Dense coverage along EVERY branch block — classic conifer look
            for (List<BlockPos> path : branchPaths) {
                int n = path.size();
                for (int i = 0; i < n; i++) {
                    float fraction = (float) i / Math.max(1, n - 1);
                    float localDensity = density * (0.5f + fraction * 0.5f);
                    placeLeafCluster(placements, leafBlock, path.get(i),
                            Math.max(1, radius - 1), localDensity, topBias, random);
                }
            }
            // Dense terminal pompoms at every tip
            for (LimbEnd end : branchEnds) {
                placeLeafCluster(placements, leafBlock, end.pos,
                        radius, density, topBias, random);
            }
        } else {
            // Terminal clusters only (for sparser excurrent shapes)
            for (LimbEnd end : branchEnds) {
                placeLeafCluster(placements, leafBlock, end.pos,
                        radius, density, topBias, random);
            }
        }
    }

    // ── Trunk ─────────────────────────────────────────────────────────────────

    private static void buildTrunk(List<PlacementEntry> placements,
                                   BlockPos base, int height,
                                   TreeDefinition def) {
        Block logBlock = getLogBlock(def.getLogBlock());
        if (logBlock == null) {
            System.err.println("[TreeGen] Log not found: " + def.getLogBlock());
            return;
        }
        int level = def.getTrunkThicknessLevel();
        for (int y = 0; y < height; y++) {
            placements.add(new PlacementEntry(
                    base.above(y),
                    logBlock.defaultBlockState()
                            .setValue(FruitTreeLogBlock.THICKNESS, level)
                            .setValue(FruitTreeLogBlock.AXIS, Direction.Axis.Y)));
        }
    }

    // ── Primary limbs ─────────────────────────────────────────────────────────

    private static void buildPrimaryLimbs(List<PlacementEntry> placements,
                                          BlockPos base, int splitY,
                                          int trunkHeight, TreeDefinition def,
                                          RandomSource random,
                                          List<LimbEnd> limbEnds) {
        Block logBlock = getLogBlock(def.getLogBlock());
        if (logBlock == null) return;

        int count = def.randomPrimaryCount(random);
        int thicknessLevel = def.getPrimaryThicknessLevel();
        double baseAngle = random.nextDouble() * Math.PI * 2.0;
        double angleStep = Math.PI * 2.0 / count;

        for (int i = 0; i < count; i++) {
            double angle = baseAngle + i * angleStep
                    + (random.nextDouble() - 0.5) * angleStep * 0.3;

            int length   = def.randomPrimaryLength(random);
            float spread = def.randomPrimarySpread(random);
            float upLift = def.randomPrimaryUpwardLift(random);

            int startY = splitY + random.nextInt(Math.max(1, trunkHeight - splitY));
            BlockPos start = base.above(startY);

            int targetX = start.getX() + (int) Math.round(Math.cos(angle) * length * spread);
            int targetY = start.getY() + Math.max(1, (int) (length * upLift));
            int targetZ = start.getZ() + (int) Math.round(Math.sin(angle) * length * spread);

            double perpAngle = angle + Math.PI / 2.0;
            double photoBend = (random.nextDouble() - 0.5) * length * 0.7;
            targetX += (int) Math.round(Math.cos(perpAngle) * photoBend);
            targetZ += (int) Math.round(Math.sin(perpAngle) * photoBend);

            BlockPos target = new BlockPos(targetX, targetY, targetZ);
            List<BlockPos> path = buildAdjacentPath(start, target, random);

            BlockPos lastPlaced = start;
            for (BlockPos step : path) {
                Direction.Axis axis = getAxis(
                        step.getX() - lastPlaced.getX(),
                        step.getY() - lastPlaced.getY(),
                        step.getZ() - lastPlaced.getZ());
                placements.add(new PlacementEntry(
                        step,
                        logBlock.defaultBlockState()
                                .setValue(FruitTreeLogBlock.THICKNESS, thicknessLevel)
                                .setValue(FruitTreeLogBlock.AXIS, axis)));
                lastPlaced = step;
            }

            limbEnds.add(new LimbEnd(lastPlaced, angle, thicknessLevel));
        }
    }

    // ── Secondary branches ────────────────────────────────────────────────────

    private static void buildSecondaryBranches(
            List<PlacementEntry> placements,
            LimbEnd limb, TreeDefinition def, RandomSource random,
            List<LimbEnd> branchEnds, List<List<BlockPos>> branchPaths,
            BlockPos trunkBase) {

        Block logBlock = getLogBlock(def.getLogBlock());
        if (logBlock == null) return;

        int count = def.randomSecondaryCount(random);
        int thicknessLevel = def.getSecondaryThicknessLevel();

        for (int i = 0; i < count; i++) {
            double angle  = limb.angle + (random.nextDouble() - 0.5) * Math.PI * 0.5;
            int length    = def.randomSecondaryLength(random);
            float inward  = def.randomSecondaryInwardPull(random);
            float upLift  = def.randomSecondaryUpwardLift(random);
            float zigzag  = def.randomSecondaryZigzag(random);

            BlockPos start = limb.pos;
            boolean curlInward = random.nextFloat() < 0.35f;

            int targetX, targetY, targetZ;

            if (curlInward) {
                int dx = trunkBase.getX() - start.getX();
                int dz = trunkBase.getZ() - start.getZ();
                targetX = start.getX() + (int) Math.round(dx * 0.5);
                targetZ = start.getZ() + (int) Math.round(dz * 0.5);
                targetY = start.getY() + Math.max(2, (int) (length * 1.2f));
                angle   = Math.atan2(targetZ - start.getZ(), targetX - start.getX());
            } else {
                targetX = start.getX() + (int) Math.round(Math.cos(angle) * length * 0.5f);
                targetY = start.getY() + Math.max(2, (int) (length * upLift));
                targetZ = start.getZ() + (int) Math.round(Math.sin(angle) * length * 0.5f);
                double perpAngle = angle + Math.PI / 2.0;
                double bend = (random.nextDouble() - 0.5) * length * zigzag;
                targetX += (int) Math.round(Math.cos(perpAngle) * bend);
                targetZ += (int) Math.round(Math.sin(perpAngle) * bend);
            }

            BlockPos target = new BlockPos(targetX, targetY, targetZ);
            List<BlockPos> path = buildAdjacentPath(start, target, random);

            List<BlockPos> fullPath = new ArrayList<>();
            fullPath.add(start);
            fullPath.addAll(path);
            branchPaths.add(fullPath);

            BlockPos lastPlaced = start;
            for (BlockPos step : path) {
                Direction.Axis axis = getAxis(
                        step.getX() - lastPlaced.getX(),
                        step.getY() - lastPlaced.getY(),
                        step.getZ() - lastPlaced.getZ());
                placements.add(new PlacementEntry(
                        step,
                        logBlock.defaultBlockState()
                                .setValue(FruitTreeLogBlock.THICKNESS, thicknessLevel)
                                .setValue(FruitTreeLogBlock.AXIS, axis)));
                lastPlaced = step;
            }

            double actualAngle = Math.atan2(
                    lastPlaced.getZ() - start.getZ(),
                    lastPlaced.getX() - start.getX());
            branchEnds.add(new LimbEnd(lastPlaced, actualAngle, thicknessLevel));
        }
    }

    // ── Twigs ─────────────────────────────────────────────────────────────────

    private static void buildTwigs(List<PlacementEntry> placements,
                                   LimbEnd branch, TreeDefinition def,
                                   RandomSource random,
                                   List<LimbEnd> twigEnds,
                                   List<List<BlockPos>> twigPaths) {
        Block logBlock = getLogBlock(def.getLogBlock());
        if (logBlock == null) return;

        int count = def.randomTwigCount(random);
        int thicknessLevel = def.getTwigThicknessLevel();

        for (int i = 0; i < count; i++) {
            double angle = branch.angle + (random.nextDouble() - 0.5) * Math.PI * 1.1;
            int length   = def.randomTwigLength(random);
            float vert   = def.randomTwigVerticality(random);

            BlockPos start = branch.pos;

            int targetX = start.getX()
                    + (int) Math.round(Math.cos(angle) * length * (1.0f - vert));
            int targetY = start.getY()
                    + (def.twigsDroop()
                    ? -Math.max(1, Math.round(length * 0.5f))
                    : Math.max(1, Math.round(length * (0.3f + vert * 0.9f))));
            int targetZ = start.getZ()
                    + (int) Math.round(Math.sin(angle) * length * (1.0f - vert));

            BlockPos target = new BlockPos(targetX, targetY, targetZ);
            List<BlockPos> path = buildAdjacentPath(start, target, random);

            List<BlockPos> fullPath = new ArrayList<>();
            fullPath.add(start);
            fullPath.addAll(path);
            twigPaths.add(fullPath);

            BlockPos lastPlaced = start;
            for (BlockPos step : path) {
                Direction.Axis axis = getAxis(
                        step.getX() - lastPlaced.getX(),
                        step.getY() - lastPlaced.getY(),
                        step.getZ() - lastPlaced.getZ());
                placements.add(new PlacementEntry(
                        step,
                        logBlock.defaultBlockState()
                                .setValue(FruitTreeLogBlock.THICKNESS, thicknessLevel)
                                .setValue(FruitTreeLogBlock.AXIS, axis)));
                lastPlaced = step;
            }

            twigEnds.add(new LimbEnd(lastPlaced, angle, thicknessLevel));
        }
    }

    // ── Deliquescent canopy ───────────────────────────────────────────────────

    private static void buildDeliquescentCanopy(List<PlacementEntry> placements,
                                                TreeDefinition def,
                                                RandomSource random,
                                                List<LimbEnd> twigEnds,
                                                List<List<BlockPos>> twigPaths,
                                                List<List<BlockPos>> branchPaths) {
        Block leafBlock = getLeafBlock(def.getLeafBlock());
        if (leafBlock == null) return;
        if (twigEnds.isEmpty()) return;

        float topBias = def.getCanopyTopBias();
        float density = def.randomLeafDensity(random);
        int clusterRadius = def.randomCanopyClusterRadius(random);

        // Terminal pompoms at twig tips
        for (LimbEnd twig : twigEnds) {
            placeLeafCluster(placements, leafBlock, twig.pos,
                    clusterRadius, density, topBias, random);
        }

        // Bleed taper along twig paths
        for (List<BlockPos> path : twigPaths) {
            int n = path.size();
            if (n < 2) continue;
            for (int i = 0; i < n; i++) {
                float fraction = (float) i / (n - 1);
                float localDensity = density * fraction * fraction * 0.6f;
                if (localDensity < 0.05f) continue;
                placeLeafCluster(placements, leafBlock, path.get(i),
                        1, localDensity, topBias, random);
            }
        }

        // Faint bleed on outer 40% of secondary branch paths
        for (List<BlockPos> path : branchPaths) {
            int n = path.size();
            if (n < 2) continue;
            int startIdx = (int) Math.ceil(n * 0.66);
            int span = Math.max(1, n - 1 - startIdx);
            for (int i = startIdx; i < n; i++) {
                float fraction = (float) (i - startIdx) / span;
                float localDensity = density * (0.08f + fraction * 0.12f);
                placeLeafCluster(placements, leafBlock, path.get(i),
                        1, localDensity, topBias, random);
            }
        }
    }

    // ── Leaf cluster ─────────────────────────────────────────────────────────

    private static void placeLeafCluster(List<PlacementEntry> placements,
                                         Block leafBlock, BlockPos centre,
                                         int radius, float density,
                                         float topBias, RandomSource random) {
        int upReach   = (int) Math.ceil(radius * topBias * 1.3f);
        int downReach = (int) Math.ceil(radius * (1.0f - topBias) * 1.3f);

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > (radius + 1) * (radius + 1)) continue;
                for (int y = 0; y <= upReach; y++) {
                    double dist = Math.sqrt(x * x
                            + Math.pow(y / Math.max(0.1, topBias * 1.3), 2) + z * z);
                    if (dist <= radius && random.nextFloat() < density)
                        placements.add(leafEntry(leafBlock, centre.offset(x, y, z)));
                }
                for (int y = 1; y <= downReach; y++) {
                    double dist = Math.sqrt(x * x
                            + Math.pow(y / Math.max(0.1, (1.0 - topBias) * 1.3), 2) + z * z);
                    if (dist <= radius * 0.8f && random.nextFloat() < density * 0.3f)
                        placements.add(leafEntry(leafBlock, centre.offset(x, -y, z)));
                }
            }
        }
    }

    private static PlacementEntry leafEntry(Block leafBlock, BlockPos pos) {
        return new PlacementEntry(pos, leafBlock.defaultBlockState()
                .setValue(FruitTreeLeavesBlock.SEASON_STAGE,
                        FruitTreeLeavesBlock.SeasonStage.FULL));
    }

    // ── Fruit ─────────────────────────────────────────────────────────────────

    private static void placeFruit(List<PlacementEntry> placements,
                                   List<LimbEnd> twigEnds,
                                   TreeDefinition def, RandomSource random) {
        if (def.getFruitBlock() == null) return;
        Block fruitBlock = BuiltInRegistries.BLOCK.get(
                ResourceLocation.parse(def.getFruitBlock()));
        if (!(fruitBlock instanceof FruitBlock)) return;
        for (LimbEnd twig : twigEnds) {
            if (random.nextFloat() < def.getFruitSpawnRate()) {
                placements.add(new PlacementEntry(
                        twig.pos.below(),
                        fruitBlock.defaultBlockState()
                                .setValue(FruitBlock.AGE, 0)
                                .setValue(FruitBlock.FACING, Direction.DOWN)));
            }
        }
    }

    // ── Placement ─────────────────────────────────────────────────────────────

    private static void placeAll(LevelAccessor level, List<PlacementEntry> placements) {
        System.out.println("[TreeGen] Placing " + placements.size() + " blocks");
        for (PlacementEntry entry : placements) {
            BlockState existing = level.getBlockState(entry.pos);
            if (existing.isAir() || existing.canBeReplaced())
                level.setBlock(entry.pos, entry.state, 3);
        }
    }

    // ── Adjacent path ─────────────────────────────────────────────────────────

    private static List<BlockPos> buildAdjacentPath(BlockPos start,
                                                    BlockPos target,
                                                    RandomSource random) {
        List<BlockPos> path = new ArrayList<>();
        BlockPos current = start;
        int maxSteps = Math.abs(target.getX() - start.getX())
                + Math.abs(target.getY() - start.getY())
                + Math.abs(target.getZ() - start.getZ()) + 5;

        for (int step = 0; step < maxSteps; step++) {
            if (current.equals(target)) break;
            int dx = target.getX() - current.getX();
            int dy = target.getY() - current.getY();
            int dz = target.getZ() - current.getZ();
            List<int[]> choices = new ArrayList<>();
            if (dx != 0) { choices.add(new int[]{Integer.signum(dx), 0, 0}); choices.add(new int[]{Integer.signum(dx), 0, 0}); }
            if (dz != 0) { choices.add(new int[]{0, 0, Integer.signum(dz)}); choices.add(new int[]{0, 0, Integer.signum(dz)}); }
            if (dy != 0) { choices.add(new int[]{0, Integer.signum(dy), 0}); }
            if (choices.isEmpty()) break;
            int[] chosen = choices.get(random.nextInt(choices.size()));
            current = current.offset(chosen[0], chosen[1], chosen[2]);
            path.add(current);
        }
        return path;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Direction.Axis getAxis(int dx, int dy, int dz) {
        int ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ay >= ax && ay >= az) return Direction.Axis.Y;
        if (ax >= az)             return Direction.Axis.X;
        return Direction.Axis.Z;
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

    private record PlacementEntry(BlockPos pos, BlockState state) {}
    private record LimbEnd(BlockPos pos, double angle, int thickness) {}
}